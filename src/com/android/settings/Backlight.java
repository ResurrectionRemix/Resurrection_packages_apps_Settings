/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings;

import android.content.ContentResolver;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class Backlight extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String FILTER_ENABLED = "light_filter_enabled";
    private static final String FILTER_WINDOW = "light_filter_window";
    private static final String FILTER_RESET = "light_filter_reset";
    private static final String FILTER_INTERVAL = "light_filter_interval";
    private static final String LEVELS_ENABLED = "light_levels_enabled";
    private static final String SCREEN_DIM = "light_levels_dim";
    private static final String DECREASE_ENABLED = "light_decrease_enabled";
    private static final String DECREASE_HYSTERESIS = "light_decrease_hysteresis";

    private CheckBoxPreference mFilterEnabled;
    private ListPreference mFilterWindow;
    private ListPreference mFilterReset;
    private ListPreference mFilterInterval;
    private CheckBoxPreference mLevelsEnabled;
    private ListPreference mScreenDim;
    private CheckBoxPreference mDecreaseEnabled;
    private ListPreference mDecreaseHysteresis;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.backlight_settings);

        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver cr = getActivity().getApplicationContext().getContentResolver();

        mFilterEnabled = (CheckBoxPreference) prefSet.findPreference(FILTER_ENABLED);
        mFilterEnabled.setChecked(Settings.System.getInt(cr,
                Settings.System.LIGHT_FILTER, 0) != 0);
        mFilterEnabled.setOnPreferenceChangeListener(this);

        mFilterReset = (ListPreference) prefSet.findPreference(FILTER_RESET);
        mFilterReset.setValue(String.valueOf(Settings.System.getInt(cr,
                Settings.System.LIGHT_FILTER_RESET, -1)));
        mFilterReset.setOnPreferenceChangeListener(this);

        mFilterWindow = (ListPreference) prefSet.findPreference(FILTER_WINDOW);
        mFilterWindow.setValue(String.valueOf(Settings.System.getInt(cr,
                Settings.System.LIGHT_FILTER_WINDOW, 30000)));
        mFilterWindow.setOnPreferenceChangeListener(this);

        mFilterInterval = (ListPreference) prefSet.findPreference(FILTER_INTERVAL);
        mFilterInterval.setValue(String.valueOf(Settings.System.getInt(cr,
                Settings.System.LIGHT_FILTER_INTERVAL, 1000)));
        mFilterInterval.setOnPreferenceChangeListener(this);

        mLevelsEnabled = (CheckBoxPreference) prefSet.findPreference(LEVELS_ENABLED);
        mLevelsEnabled.setChecked(Settings.System.getInt(cr,
                Settings.System.LIGHT_SENSOR_CUSTOM, 0) != 0);
        mLevelsEnabled.setOnPreferenceChangeListener(this);

        mScreenDim = (ListPreference) prefSet.findPreference(SCREEN_DIM);
        mScreenDim.setValue(String.valueOf(Settings.System.getInt(cr,
                Settings.System.LIGHT_SCREEN_DIM,
                android.os.PowerManager.BRIGHTNESS_DIM)));
        mScreenDim.setOnPreferenceChangeListener(this);

        mDecreaseEnabled = (CheckBoxPreference) prefSet.findPreference(DECREASE_ENABLED);
        mDecreaseEnabled.setChecked(Settings.System.getInt(cr,
                Settings.System.LIGHT_DECREASE, 0) != 0);
        mDecreaseEnabled.setOnPreferenceChangeListener(this);

        mDecreaseHysteresis = (ListPreference) prefSet.findPreference(DECREASE_HYSTERESIS);
        mDecreaseHysteresis.setValue(String.valueOf(Settings.System.getInt(cr,
                Settings.System.LIGHT_HYSTERESIS, 50)));
        mDecreaseHysteresis.setOnPreferenceChangeListener(this);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean handled = false;

        if (preference == mFilterEnabled) {
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LIGHT_FILTER, getBoolean(newValue) ? 1 : 0);
            handled = true;
        } else if (preference == mFilterWindow) {
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LIGHT_FILTER_WINDOW, getInt(newValue));
            handled = true;
        } else if (preference == mFilterReset) {
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LIGHT_FILTER_RESET, getInt(newValue));
            handled = true;
        } else if (preference == mFilterInterval) {
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LIGHT_FILTER_INTERVAL, getInt(newValue));
            handled = true;
        } else if (preference == mScreenDim) {
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LIGHT_SCREEN_DIM, getInt(newValue));
            handled = true;
        } else if (preference == mLevelsEnabled) {
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LIGHT_SENSOR_CUSTOM, getBoolean(newValue) ? 1 : 0);
            handled = true;
        } else if (preference == mDecreaseEnabled) {
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LIGHT_DECREASE, getBoolean(newValue) ? 1 : 0);
            handled = true;
        } else if (preference == mDecreaseHysteresis) {
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LIGHT_HYSTERESIS, getInt(newValue));
            handled = true;
        }

        // Force the system to reload settings and reinit
        if (handled) {
            long tag = Settings.System.getLong(getActivity().getApplicationContext()
                    .getContentResolver(),
                    Settings.System.LIGHTS_CHANGED, 0) + 1;
            Settings.System.putLong(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LIGHTS_CHANGED, tag);
        }

        return handled;
    }

    private boolean getBoolean(Object o) {
        return Boolean.valueOf(o.toString());
    }

    private int getInt(Object o) {
        return Integer.valueOf(o.toString());
    }
}
