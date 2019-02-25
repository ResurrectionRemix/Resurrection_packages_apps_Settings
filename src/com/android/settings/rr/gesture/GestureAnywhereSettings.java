/*
 * Copyright (C) 2013 The ChameleonOS Project
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

package com.android.settings.rr.gesture;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;
import android.provider.Settings;
import android.view.Gravity;
import com.android.settings.R;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.SettingsPreferenceFragment;

import com.android.settings.rr.Preferences.CustomSeekBarPreference;
import com.android.settings.rr.utils.RRUtils;
import com.android.settings.search.Indexable.SearchIndexProvider;

public class GestureAnywhereSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {
    private static final String TAG = "GestureAnywhereSettings";

    private static final String KEY_ENABLED = "gesture_anywhere_enabled";
    private static final String KEY_POSITION = "gesture_anywhere_position";
    private static final String KEY_GESTURES = "gesture_anywhere_gestures";
    private static final String KEY_TRIGGER_WIDTH = "gesture_anywhere_trigger_width";
    private static final String KEY_TRIGGER_TOP = "gesture_anywhere_trigger_top";
    private static final String KEY_TRIGGER_BOTTOM = "gesture_anywhere_trigger_bottom";

    private SwitchPreference mEnabledPref;
    private ListPreference mPositionPref;
    private CustomSeekBarPreference mTriggerWidthPref;
    private CustomSeekBarPreference mTriggerTopPref;
    private CustomSeekBarPreference mTriggerBottomPref;

    private CharSequence mPreviousTitle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.gesture_anywhere);

        mEnabledPref = (SwitchPreference) findPreference(KEY_ENABLED);
        mEnabledPref.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.System.GESTURE_ANYWHERE_ENABLED, 0) == 1));
        mEnabledPref.setOnPreferenceChangeListener(this);

        PreferenceScreen prefSet = getPreferenceScreen();
        mPositionPref = (ListPreference) prefSet.findPreference(KEY_POSITION);
        mPositionPref.setOnPreferenceChangeListener(this);
        int position = Settings.System.getInt(getContentResolver(),
                Settings.System.GESTURE_ANYWHERE_POSITION, Gravity.LEFT);
                mPositionPref.setValue(String.valueOf(position));

        mTriggerWidthPref = (CustomSeekBarPreference) findPreference(KEY_TRIGGER_WIDTH);
        mTriggerWidthPref.setValue(Settings.System.getInt(getContentResolver(),
                Settings.System.GESTURE_ANYWHERE_TRIGGER_WIDTH, 40));
        mTriggerWidthPref.setOnPreferenceChangeListener(this);

        mTriggerTopPref = (CustomSeekBarPreference) findPreference(KEY_TRIGGER_TOP);
        mTriggerTopPref.setValue(Settings.System.getInt(getContentResolver(),
                Settings.System.GESTURE_ANYWHERE_TRIGGER_TOP, 0));
        mTriggerTopPref.setOnPreferenceChangeListener(this);

        mTriggerBottomPref = (CustomSeekBarPreference) findPreference(KEY_TRIGGER_BOTTOM);
        mTriggerBottomPref.setValue(Settings.System.getInt(getContentResolver(),
                Settings.System.GESTURE_ANYWHERE_TRIGGER_HEIGHT, 100));
        mTriggerBottomPref.setOnPreferenceChangeListener(this);

        Preference pref = findPreference(KEY_GESTURES);
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(getActivity(), GestureAnywhereBuilderActivity.class));
                return true;
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        final ActionBar bar = getActivity().getActionBar();
        mPreviousTitle = bar.getTitle();
        bar.setTitle(R.string.gesture_anywhere_title);
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().getActionBar().setTitle(mPreviousTitle);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mPositionPref) {
            int position = Integer.valueOf((String) newValue);
            updatePositionSummary(position);
            return true;
        } else if (preference == mEnabledPref) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.GESTURE_ANYWHERE_ENABLED,
                    ((Boolean) newValue).booleanValue() ? 1 : 0);
            return true;
        } else if (preference == mTriggerWidthPref) {
            int width = ((Integer)newValue).intValue();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.GESTURE_ANYWHERE_TRIGGER_WIDTH, width);
            return true;
        } else if (preference == mTriggerTopPref) {
            int top = ((Integer)newValue).intValue();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.GESTURE_ANYWHERE_TRIGGER_TOP, top);
            return true;
        } else if (preference == mTriggerBottomPref) {
            int bottom = ((Integer)newValue).intValue();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.GESTURE_ANYWHERE_TRIGGER_HEIGHT, bottom);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        boolean value;
        return true;
    }

    private void updatePositionSummary(int value) {
        mPositionPref.setSummary(mPositionPref.getEntries()[mPositionPref.findIndexOfValue("" + value)]);
        Settings.System.putInt(getContentResolver(),
                Settings.System.GESTURE_ANYWHERE_POSITION, value);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.RESURRECTED;
    }

    @Override
    public void onPause() {
        super.onPause();
        Settings.System.putInt(getContentResolver(),
                Settings.System.GESTURE_ANYWHERE_SHOW_TRIGGER, 0);
    }

    @Override
    public void onResume() {
        super.onResume();
        Settings.System.putInt(getContentResolver(),
                Settings.System.GESTURE_ANYWHERE_SHOW_TRIGGER, 1);
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        RRUtils.addSearchIndexProvider(R.xml.gesture_anywhere);
}
