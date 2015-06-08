/*
 * Copyright (C) 2013-2015 Slimroms
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.rr;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.preference.SwitchPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.SeekBarPreferenceCham;

public class PieTriggerSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    // This equals EdgeGesturePosition.LEFT.FLAG
    private static final int DEFAULT_POSITION = 1 << 0;

    private static final String PREF_PIE_DISABLE_IME_TRIGGERS = "pie_disable_ime_triggers";
    private static final String PIE_TRIGGER_SENSITIVITY = "pie_trigger_sensitivity";

    private static final String[] TRIGGER = {
        "pie_control_trigger_left",
        "pie_control_trigger_bottom",
        "pie_control_trigger_right",
        "pie_control_trigger_top"
    };

    private SwitchPreference[] mTrigger = new SwitchPreference[4];
    private SwitchPreference mDisableImeTriggers;
    private SeekBarPreferenceCham mSensitivity;

    private ContentObserver mPieTriggerObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            updatePieTriggers();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pie_trigger);

        PreferenceScreen prefSet = getPreferenceScreen();

        for (int i = 0; i < TRIGGER.length; i++) {
            mTrigger[i] = (SwitchPreference) prefSet.findPreference(TRIGGER[i]);
            mTrigger[i].setOnPreferenceChangeListener(this);
        }

        mDisableImeTriggers = (SwitchPreference) findPreference(PREF_PIE_DISABLE_IME_TRIGGERS);
        mDisableImeTriggers.setOnPreferenceChangeListener(this);
        
        mSensitivity = (SeekBarPreferenceCham) findPreference(PIE_TRIGGER_SENSITIVITY);
        mSensitivity.setValue(Settings.System.getInt(getContentResolver(),
                Settings.System.PIE_TRIGGER_SENSITIVITY, 5));
        mSensitivity.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int triggerSlots = 0;
        if (preference == mDisableImeTriggers) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.PIE_IME_CONTROL,
                    (Boolean) newValue ? 1 : 0);
        } else if (preference == mSensitivity) {
            int sensitivity = ((Integer)newValue).intValue();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.PIE_TRIGGER_SENSITIVITY, sensitivity);
        } else {
            for (int i = 0; i < mTrigger.length; i++) {
                boolean checked = preference == mTrigger[i]
                        ? (Boolean) newValue : mTrigger[i].isChecked();
                if (checked) {
                    triggerSlots |= 1 << i;
                }
            }
            Settings.System.putInt(getContentResolver(),
                    Settings.System.PIE_GRAVITY, triggerSlots);
        }
        updatePieTriggers();
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();

        getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.PIE_GRAVITY), true,
                mPieTriggerObserver);

        updatePieTriggers();
    }

    @Override
    public void onPause() {
        super.onPause();
        getContentResolver().unregisterContentObserver(mPieTriggerObserver);
    }

    private void updatePieTriggers() {
        int triggerSlots = Settings.System.getInt(getContentResolver(),
                Settings.System.PIE_GRAVITY, DEFAULT_POSITION);

        for (int i = 0; i < mTrigger.length; i++) {
            if ((triggerSlots & (0x01 << i)) != 0) {
                mTrigger[i].setChecked(true);
            } else {
                mTrigger[i].setChecked(false);
            }
        }

        mDisableImeTriggers.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.PIE_IME_CONTROL, 1) == 1);
    }
}
