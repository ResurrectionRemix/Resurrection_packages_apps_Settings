/* 
 * Copyright (C) 2014 DarkKat
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
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.internal.logging.MetricsLogger;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.benzo.SeekBarPreference;

public class LockScreenWeatherSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String PREF_CONDITION_ICON =
            "weather_condition_icon";
    private static final String PREF_HIDE_WEATHER =
            "weather_hide_panel";
    private static final String PREF_NUMBER_OF_NOTIFICATIONS =
            "weather_number_of_notifications";

    private static final int MONOCHROME_ICON = 0;

    private ListPreference mConditionIcon;
    private ListPreference mHideWeather;
    private SeekBarPreference mNumberOfNotifications;

    private ContentResolver mResolver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.lock_screen_weather_settings);
        mResolver = getActivity().getContentResolver();
        PreferenceScreen prefs = getPreferenceScreen();

        mConditionIcon =
                (ListPreference) findPreference(PREF_CONDITION_ICON);
        int conditionIcon = Settings.System.getInt(mResolver,
               Settings.System.LOCK_SCREEN_WEATHER_CONDITION_ICON, MONOCHROME_ICON);
        mConditionIcon.setValue(String.valueOf(conditionIcon));
        mConditionIcon.setSummary(mConditionIcon.getEntry());
        mConditionIcon.setOnPreferenceChangeListener(this);

        mHideWeather =
                (ListPreference) findPreference(PREF_HIDE_WEATHER);
        int hideWeather = Settings.System.getInt(mResolver,
               Settings.System.LOCK_SCREEN_WEATHER_HIDE_PANEL, 0);
        mHideWeather.setValue(String.valueOf(hideWeather));
        mHideWeather.setOnPreferenceChangeListener(this);

        mNumberOfNotifications =
                (SeekBarPreference) findPreference(PREF_NUMBER_OF_NOTIFICATIONS);
        int numberOfNotifications = Settings.System.getInt(mResolver,
                Settings.System.LOCK_SCREEN_WEATHER_NUMBER_OF_NOTIFICATIONS, 4);
        mNumberOfNotifications.setValue(numberOfNotifications);
        mNumberOfNotifications.setOnPreferenceChangeListener(this);

        updatePreference();
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.LOCKSCREEN_WEATHER_SETTINGS;
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreference();
    }

    private void updatePreference() {
        int hideWeather = Settings.System.getInt(mResolver,
                Settings.System.LOCK_SCREEN_WEATHER_HIDE_PANEL, 0);
        if (hideWeather == 0) {
            mNumberOfNotifications.setEnabled(false);
            mHideWeather.setSummary(R.string.weather_hide_panel_auto_summary);
        } else if (hideWeather == 1) {
            mNumberOfNotifications.setEnabled(true);
            mHideWeather.setSummary(R.string.weather_hide_panel_custom_summary);
        } else {
            mNumberOfNotifications.setEnabled(false);
            mHideWeather.setSummary(R.string.weather_hide_panel_never_summary);
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean value;
        if (preference == mConditionIcon) {
            int intValue = Integer.valueOf((String) newValue);
            int index = mConditionIcon.findIndexOfValue((String) newValue);
            Settings.System.putInt(mResolver,
                    Settings.System.LOCK_SCREEN_WEATHER_CONDITION_ICON, intValue);
            mConditionIcon.setSummary(mConditionIcon.getEntries()[index]);
            return true;
        } else if (preference == mHideWeather) {
            int intValue = Integer.valueOf((String) newValue);
            int index = mHideWeather.findIndexOfValue((String) newValue);
            Settings.System.putInt(mResolver,
                    Settings.System.LOCK_SCREEN_WEATHER_HIDE_PANEL, intValue);
            updatePreference();
            return true;
        } else if (preference == mNumberOfNotifications) {
            int numberOfNotifications = (Integer) newValue;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.LOCK_SCREEN_WEATHER_NUMBER_OF_NOTIFICATIONS,
            numberOfNotifications);
            return true;
        }
        return false;
    }
}
