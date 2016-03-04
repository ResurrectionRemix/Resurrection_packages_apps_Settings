/*
 * Copyright (C) 2015 The Dirty Unicorns Project
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

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.internal.logging.MetricsLogger;
import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class LockscreenColors extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String TAG = "LockscreenColors";

    private static final String LOCKSCREEN_PHONE_ICON_COLOR = "lockscreen_phone_icon_color";
    private static final String LOCKSCREEN_LOCK_ICON_COLOR = "lockscreen_lock_icon_color";
    private static final String LOCKSCREEN_CAMERA_ICON_COLOR = "lockscreen_camera_icon_color";
    private static final String LOCKSCREEN_INDICATION_TEXT_COLOR = "lockscreen_indication_text_color";
    private static final String LOCKSCREEN_CLOCK_COLOR = "lockscreen_clock_color";
    private static final String LOCKSCREEN_CLOCK_DATE_COLOR = "lockscreen_clock_date_color";
    private static final String LOCKSCREEN_OWNER_INFO_COLOR = "lockscreen_owner_info_color";
    private static final String LOCKSCREEN_ALARM_COLOR = "lockscreen_alarm_color";
    private static final String PREF_TEXT_COLOR = "weather_text_color";
    private static final String PREF_ICON_COLOR = "weather_icon_color";	
    private static final String LOCKSCREEN_TEMP_COLOR = "lock_screen_weather_temp_color";
    private static final String LOCKSCREEN_CONDITION_COLOR = "lock_screen_weather_con_color";
    private static final String LOCKSCREEN_STAMP_COLOR = "lock_screen_weather_stamp_color";
    private static final String LOCKSCREEN_HUMID_COLOR = "lock_screen_weather_hum_color";
    private static final String LOCKSCREEN_CITY_COLOR = "lock_screen_weather_city_color";
    private static final String LOCKSCREEN_WIND_COLOR = "lock_screen_weather_wind_color";	

    private static final int MONOCHROME_ICON = 0;

    static final int DEFAULT = 0xffffffff;
    static final int TRANSPARENT = 0x0DFFFFFF;

    private static final int MENU_RESET = Menu.FIRST;

    private ColorPickerPreference mLockscreenPhoneColorPicker;
    private ColorPickerPreference mLockscreenLockColorPicker;
    private ColorPickerPreference mLockscreenCameraColorPicker;
    private ColorPickerPreference mLockscreenIndicationTextColorPicker;
    private ColorPickerPreference mLockscreenClockColorPicker;
    private ColorPickerPreference mLockscreenClockDateColorPicker;
    private ColorPickerPreference mLockscreenOwnerInfoColorPicker;
    private ColorPickerPreference mLockscreenAlarmColorPicker;
    private ColorPickerPreference mTextColor;
    private ColorPickerPreference mIconColor;
    private ColorPickerPreference mTempColor;
    private ColorPickerPreference mStampColor;
    private ColorPickerPreference mConditionColor;
    private ColorPickerPreference mHumidityColor;
    private ColorPickerPreference mCityColor;
    private ColorPickerPreference mWindColor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.lockscreen_colors);

        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();

        int intColor;
        String hexColor;

        mLockscreenPhoneColorPicker = (ColorPickerPreference) findPreference(LOCKSCREEN_PHONE_ICON_COLOR);
        mLockscreenPhoneColorPicker.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_PHONE_ICON_COLOR, TRANSPARENT);
        hexColor = String.format("#%08x", (0x0DFFFFFF & intColor));
        mLockscreenPhoneColorPicker.setSummary(hexColor);
        mLockscreenPhoneColorPicker.setNewPreviewColor(intColor);

        mLockscreenLockColorPicker = (ColorPickerPreference) findPreference(LOCKSCREEN_LOCK_ICON_COLOR);
        mLockscreenLockColorPicker.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_LOCK_ICON_COLOR, DEFAULT);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mLockscreenLockColorPicker.setSummary(hexColor);
        mLockscreenLockColorPicker.setNewPreviewColor(intColor);

        mLockscreenCameraColorPicker = (ColorPickerPreference) findPreference(LOCKSCREEN_CAMERA_ICON_COLOR);
        mLockscreenCameraColorPicker.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_CAMERA_ICON_COLOR, TRANSPARENT);
        hexColor = String.format("#%08x", (0x0DFFFFFF & intColor));
        mLockscreenCameraColorPicker.setSummary(hexColor);
        mLockscreenCameraColorPicker.setNewPreviewColor(intColor);

        mLockscreenIndicationTextColorPicker = (ColorPickerPreference) findPreference(LOCKSCREEN_INDICATION_TEXT_COLOR);
        mLockscreenIndicationTextColorPicker.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_INDICATION_TEXT_COLOR, DEFAULT);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mLockscreenIndicationTextColorPicker.setSummary(hexColor);
        mLockscreenIndicationTextColorPicker.setNewPreviewColor(intColor);

        mLockscreenClockColorPicker = (ColorPickerPreference) findPreference(LOCKSCREEN_CLOCK_COLOR);
        mLockscreenClockColorPicker.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_CLOCK_COLOR, DEFAULT);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mLockscreenClockColorPicker.setSummary(hexColor);
        mLockscreenClockColorPicker.setNewPreviewColor(intColor);

        mLockscreenClockDateColorPicker = (ColorPickerPreference) findPreference(LOCKSCREEN_CLOCK_DATE_COLOR);
        mLockscreenClockDateColorPicker.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_CLOCK_DATE_COLOR, DEFAULT);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mLockscreenClockDateColorPicker.setSummary(hexColor);
        mLockscreenClockDateColorPicker.setNewPreviewColor(intColor);

        mLockscreenOwnerInfoColorPicker = (ColorPickerPreference) findPreference(LOCKSCREEN_OWNER_INFO_COLOR);
       mLockscreenOwnerInfoColorPicker.setOnPreferenceChangeListener(this);
       intColor = Settings.System.getInt(resolver,
       		   Settings.System.LOCKSCREEN_OWNER_INFO_COLOR, DEFAULT);
       hexColor = String.format("#%08x", (0xffffffff & intColor));
       mLockscreenOwnerInfoColorPicker.setSummary(hexColor);
       mLockscreenOwnerInfoColorPicker.setNewPreviewColor(intColor);

       mLockscreenAlarmColorPicker = (ColorPickerPreference) findPreference(LOCKSCREEN_ALARM_COLOR);
       mLockscreenAlarmColorPicker.setOnPreferenceChangeListener(this);
       intColor = Settings.System.getInt(resolver,
                  Settings.System.LOCKSCREEN_ALARM_COLOR, DEFAULT);
       hexColor = String.format("#%08x", (0xffffffff & intColor));
       mLockscreenAlarmColorPicker.setSummary(hexColor);
       mLockscreenAlarmColorPicker.setNewPreviewColor(intColor);

       mTextColor = (ColorPickerPreference) findPreference(PREF_TEXT_COLOR);
       mIconColor = (ColorPickerPreference) findPreference(PREF_ICON_COLOR);

       intColor = Settings.System.getInt(resolver,
                 Settings.System.LOCK_SCREEN_WEATHER_TEXT_COLOR, -2);
       if (intColor == -2) {
                intColor = 0xffffffff;
                mTextColor.setSummary(getResources().getString(R.string.default_string));
            } else {
                hexColor = String.format("#%08x", (0xffffffff & intColor));
                mTextColor.setSummary(hexColor);
            }
       mTextColor.setNewPreviewColor(intColor);
       mTextColor.setOnPreferenceChangeListener(this);

       intColor = Settings.System.getInt(resolver,
                Settings.System.LOCK_SCREEN_WEATHER_ICON_COLOR, -2);
            if (intColor == -2) {
                intColor = 0xffffffff;
                mIconColor.setSummary(getResources().getString(R.string.default_string));
            } else {
                hexColor = String.format("#%08x", (0xffffffff & intColor));
                mIconColor.setSummary(hexColor);
            }
       mIconColor.setNewPreviewColor(intColor);
       mIconColor.setOnPreferenceChangeListener(this);

       mTempColor = (ColorPickerPreference) findPreference(LOCKSCREEN_TEMP_COLOR);
       mTempColor.setOnPreferenceChangeListener(this);
       intColor = Settings.System.getInt(resolver,
                  Settings.System.LOCK_SCREEN_WEATHER_TEMP_COLOR, DEFAULT);
       hexColor = String.format("#%08x", (0xffffffff & intColor));
       mTempColor.setSummary(hexColor);
       mTempColor.setNewPreviewColor(intColor);

       mConditionColor = (ColorPickerPreference) findPreference(LOCKSCREEN_CONDITION_COLOR);
       mConditionColor.setOnPreferenceChangeListener(this);
       intColor = Settings.System.getInt(resolver,
                  Settings.System.LOCK_SCREEN_WEATHER_CON_COLOR, DEFAULT);
       hexColor = String.format("#%08x", (0xffffffff & intColor));
       mConditionColor.setSummary(hexColor);
       mConditionColor.setNewPreviewColor(intColor);

       mStampColor = (ColorPickerPreference) findPreference(LOCKSCREEN_STAMP_COLOR);
       mStampColor.setOnPreferenceChangeListener(this);
       intColor = Settings.System.getInt(resolver,
                  Settings.System.LOCK_SCREEN_WEATHER_STAMP_COLOR, DEFAULT);
       hexColor = String.format("#%08x", (0xffffffff & intColor));
       mStampColor.setSummary(hexColor);
       mStampColor.setNewPreviewColor(intColor);

       mHumidityColor= (ColorPickerPreference) findPreference(LOCKSCREEN_HUMID_COLOR);
       mHumidityColor.setOnPreferenceChangeListener(this);
       intColor = Settings.System.getInt(resolver,
                  Settings.System.LOCK_SCREEN_WEATHER_HUM_COLOR, DEFAULT);
       hexColor = String.format("#%08x", (0xffffffff & intColor));
       mHumidityColor.setSummary(hexColor);
       mHumidityColor.setNewPreviewColor(intColor);

       mCityColor= (ColorPickerPreference) findPreference(LOCKSCREEN_CITY_COLOR);
       mCityColor.setOnPreferenceChangeListener(this);
       intColor = Settings.System.getInt(resolver,
                  Settings.System.LOCK_SCREEN_WEATHER_CITY_COLOR, DEFAULT);
       hexColor = String.format("#%08x", (0xffffffff & intColor));
       mCityColor.setSummary(hexColor);
       mCityColor.setNewPreviewColor(intColor);

       mWindColor= (ColorPickerPreference) findPreference(LOCKSCREEN_WIND_COLOR);
       mWindColor.setOnPreferenceChangeListener(this);
       intColor = Settings.System.getInt(resolver,
                  Settings.System.LOCK_SCREEN_WEATHER_WIND_COLOR, DEFAULT);
       hexColor = String.format("#%08x", (0xffffffff & intColor));
       mWindColor.setSummary(hexColor);
       mWindColor.setNewPreviewColor(intColor);

       setHasOptionsMenu(true);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
                ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mLockscreenCameraColorPicker) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LOCKSCREEN_CAMERA_ICON_COLOR, intHex);
            return true;
        } else if (preference == mLockscreenLockColorPicker) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LOCKSCREEN_LOCK_ICON_COLOR, intHex);
            return true;
        } else if (preference == mLockscreenPhoneColorPicker) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LOCKSCREEN_PHONE_ICON_COLOR, intHex);
            return true;
        } else if (preference == mLockscreenIndicationTextColorPicker) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LOCKSCREEN_INDICATION_TEXT_COLOR, intHex);
            return true;
        } else if (preference == mLockscreenClockColorPicker) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LOCKSCREEN_CLOCK_COLOR, intHex);
            return true;
        } else if (preference == mLockscreenClockDateColorPicker) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LOCKSCREEN_CLOCK_DATE_COLOR, intHex);
            return true;
         } else if (preference == mLockscreenOwnerInfoColorPicker) {
                String hex = ColorPickerPreference.convertToARGB(
                        Integer.valueOf(String.valueOf(newValue)));
                preference.setSummary(hex);
                int intHex = ColorPickerPreference.convertToColorInt(hex);
                Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                        Settings.System.LOCKSCREEN_OWNER_INFO_COLOR, intHex);
                return true;
            } else if (preference == mLockscreenAlarmColorPicker) {
                String hex = ColorPickerPreference.convertToARGB(
                        Integer.valueOf(String.valueOf(newValue)));
                preference.setSummary(hex);
                int intHex = ColorPickerPreference.convertToColorInt(hex);
                Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                        Settings.System.LOCKSCREEN_ALARM_COLOR, intHex);
                return true;
	} else if (preference == mTextColor) {
	    int intHex;
            String hex = ColorPickerPreference.convertToARGB(
                Integer.valueOf(String.valueOf(newValue)));
            intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(resolver,
                    Settings.System.LOCK_SCREEN_WEATHER_TEXT_COLOR, intHex);
            preference.setSummary(hex);
            return true;
        } else if (preference == mIconColor) {
	    int intHex;	
            String hex = ColorPickerPreference.convertToARGB(
                Integer.valueOf(String.valueOf(newValue)));
            intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(resolver,
                    Settings.System.LOCK_SCREEN_WEATHER_ICON_COLOR, intHex);
            preference.setSummary(hex);
            return true;
        } else if (preference == mTempColor) {
                String hex = ColorPickerPreference.convertToARGB(
                        Integer.valueOf(String.valueOf(newValue)));
                preference.setSummary(hex);
                int intHex = ColorPickerPreference.convertToColorInt(hex);
                Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                        Settings.System.LOCK_SCREEN_WEATHER_TEMP_COLOR, intHex);
                return true;
        } else if (preference ==  mConditionColor) {
                String hex = ColorPickerPreference.convertToARGB(
                        Integer.valueOf(String.valueOf(newValue)));
                preference.setSummary(hex);
                int intHex = ColorPickerPreference.convertToColorInt(hex);
                Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                        Settings.System.LOCK_SCREEN_WEATHER_CON_COLOR, intHex);
                return true;
	}  else if (preference == mStampColor) {
                String hex = ColorPickerPreference.convertToARGB(
                        Integer.valueOf(String.valueOf(newValue)));
                preference.setSummary(hex);
                int intHex = ColorPickerPreference.convertToColorInt(hex);
                Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                        Settings.System.LOCK_SCREEN_WEATHER_STAMP_COLOR, intHex);
                return true;
        } else if (preference ==  mHumidityColor) {
                String hex = ColorPickerPreference.convertToARGB(
                        Integer.valueOf(String.valueOf(newValue)));
                preference.setSummary(hex);
                int intHex = ColorPickerPreference.convertToColorInt(hex);
                Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                        Settings.System.LOCK_SCREEN_WEATHER_HUM_COLOR, intHex);
                return true;
	}   else if (preference == mCityColor) {
                String hex = ColorPickerPreference.convertToARGB(
                        Integer.valueOf(String.valueOf(newValue)));
                preference.setSummary(hex);
                int intHex = ColorPickerPreference.convertToColorInt(hex);
                Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                        Settings.System.LOCK_SCREEN_WEATHER_CITY_COLOR, intHex);
                return true;
        } else if (preference ==  mWindColor) {
                String hex = ColorPickerPreference.convertToARGB(
                        Integer.valueOf(String.valueOf(newValue)));
                preference.setSummary(hex);
                int intHex = ColorPickerPreference.convertToColorInt(hex);
                Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                        Settings.System.LOCK_SCREEN_WEATHER_WIND_COLOR, intHex);
                return true;
	}
         return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset)
                .setIcon(R.drawable.ic_settings_reset)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                resetToDefault();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void resetToDefault() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle(R.string.lockscreen_colors_reset_title);
        alertDialog.setMessage(R.string.lockscreen_colors_reset_message);
        alertDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                resetValues();
            }
        });
        alertDialog.setNegativeButton(R.string.cancel, null);
        alertDialog.create().show();
    }

    private void resetValues() {
	ContentResolver resolver = getActivity().getContentResolver();
        Settings.System.putInt(getContentResolver(),
                Settings.System.LOCKSCREEN_PHONE_ICON_COLOR, TRANSPARENT);
        mLockscreenPhoneColorPicker.setNewPreviewColor(TRANSPARENT);
        mLockscreenPhoneColorPicker.setSummary(R.string.default_string);
        Settings.System.putInt(getContentResolver(),
                Settings.System.LOCKSCREEN_LOCK_ICON_COLOR, DEFAULT);
        mLockscreenLockColorPicker.setNewPreviewColor(DEFAULT);
        mLockscreenLockColorPicker.setSummary(R.string.default_string);
        Settings.System.putInt(getContentResolver(),
                Settings.System.LOCKSCREEN_CAMERA_ICON_COLOR, TRANSPARENT);
        mLockscreenCameraColorPicker.setNewPreviewColor(TRANSPARENT);
        mLockscreenCameraColorPicker.setSummary(R.string.default_string);
        Settings.System.putInt(getContentResolver(),
                Settings.System.LOCKSCREEN_INDICATION_TEXT_COLOR, DEFAULT);
        mLockscreenIndicationTextColorPicker.setNewPreviewColor(DEFAULT);
        mLockscreenIndicationTextColorPicker.setSummary(R.string.default_string);
        Settings.System.putInt(getContentResolver(),
                Settings.System.LOCKSCREEN_CLOCK_COLOR, DEFAULT);
        mLockscreenClockColorPicker.setNewPreviewColor(DEFAULT);
        mLockscreenClockColorPicker.setSummary(R.string.default_string);
        Settings.System.putInt(getContentResolver(),
                Settings.System.LOCKSCREEN_CLOCK_DATE_COLOR, DEFAULT);
        mLockscreenClockDateColorPicker.setNewPreviewColor(DEFAULT);
        mLockscreenClockDateColorPicker.setSummary(R.string.default_string);
	Settings.System.putInt(resolver,
                 Settings.System.LOCKSCREEN_OWNER_INFO_COLOR, DEFAULT);
        mLockscreenOwnerInfoColorPicker.setNewPreviewColor(DEFAULT);
        mLockscreenOwnerInfoColorPicker.setSummary(R.string.default_string);
        Settings.System.putInt(resolver,
                 Settings.System.LOCKSCREEN_ALARM_COLOR, DEFAULT);
        mLockscreenAlarmColorPicker.setNewPreviewColor(DEFAULT);
        mLockscreenAlarmColorPicker.setSummary(R.string.default_string);    
	Settings.System.putInt(resolver,
                 Settings.System.LOCK_SCREEN_WEATHER_ICON_COLOR, -2);
        mIconColor.setNewPreviewColor(-2);
        mIconColor.setSummary(R.string.default_string);
        Settings.System.putInt(resolver,
                 Settings.System.LOCK_SCREEN_WEATHER_TEXT_COLOR, -2);
        mTextColor.setNewPreviewColor(-2);
        mTextColor.setSummary(R.string.default_string);

        Settings.System.putInt(resolver,
                 Settings.System.LOCK_SCREEN_WEATHER_TEMP_COLOR, DEFAULT);
        mTempColor.setNewPreviewColor(DEFAULT);
        mTempColor.setSummary(R.string.default_string);   
        Settings.System.putInt(resolver,
                 Settings.System.LOCK_SCREEN_WEATHER_CON_COLOR, DEFAULT);
        mConditionColor.setNewPreviewColor(DEFAULT);
        mConditionColor.setSummary(R.string.default_string);   
        Settings.System.putInt(resolver,
                 Settings.System.LOCK_SCREEN_WEATHER_STAMP_COLOR, DEFAULT);
        mStampColor.setNewPreviewColor(DEFAULT);
        mStampColor.setSummary(R.string.default_string);   
        Settings.System.putInt(resolver,
                 Settings.System.LOCK_SCREEN_WEATHER_HUM_COLOR, DEFAULT);
        mHumidityColor.setNewPreviewColor(DEFAULT);
        mHumidityColor.setSummary(R.string.default_string);   
        Settings.System.putInt(resolver,
                 Settings.System.LOCK_SCREEN_WEATHER_CITY_COLOR, DEFAULT);
        mCityColor.setNewPreviewColor(DEFAULT);
        mCityColor.setSummary(R.string.default_string);   
        Settings.System.putInt(resolver,
                 Settings.System.LOCK_SCREEN_WEATHER_WIND_COLOR, DEFAULT);
        mWindColor.setNewPreviewColor(DEFAULT);
        mWindColor.setSummary(R.string.default_string); 
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    protected int getMetricsCategory() {
        return MetricsLogger.APPLICATION;
    }
}
