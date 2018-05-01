/*
 * Copyright (C) 2018 CarbonROM
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

package com.android.settings.rr.fragments;

import android.content.Context;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.PowerManager;
import android.preference.ListPreference;
import android.preference.SwitchPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.rr.Preferences.SystemSettingSwitchPreference;

public class SmartPixels extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "SmartPixels";

    private static final String ON_POWER_SAVE = "smart_pixels_on_power_save";

    private SystemSettingSwitchPreference mSmartPixelsOnPowerSave;

    ContentResolver resolver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.smart_pixels);

        resolver = getActivity().getContentResolver();

        mSmartPixelsOnPowerSave = (SystemSettingSwitchPreference) findPreference(ON_POWER_SAVE);

        updateDependency();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        updateDependency();
        return true;
    }

    private void updateDependency() {
        boolean mUseOnPowerSave = (Settings.System.getIntForUser(
                resolver, Settings.System.SMART_PIXELS_ON_POWER_SAVE,
                0, UserHandle.USER_CURRENT) == 1);
        PowerManager pm = (PowerManager)getActivity().getSystemService(Context.POWER_SERVICE);
        if (pm.isPowerSaveMode() && mUseOnPowerSave) {
            mSmartPixelsOnPowerSave.setEnabled(false);
        } else {
            mSmartPixelsOnPowerSave.setEnabled(true);
        }
    }

}