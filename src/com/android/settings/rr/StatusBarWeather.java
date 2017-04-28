/*
 * Copyright (C) 2014-2015 RR
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
package com.android.settings.rr;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;

import com.android.settings.R;
import com.android.settings.rr.SeekBarPreference;
import com.android.settings.SettingsPreferenceFragment;
import com.android.internal.logging.MetricsProto.MetricsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.util.Log;

import java.util.List;
import java.util.ArrayList;

import com.android.internal.util.rr.PackageUtils;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class StatusBarWeather extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener {

    private static final String TAG = "StatusBarWeather";

    private LinearLayout mView;

    private static final String STATUS_BAR_TEMPERATURE = "status_bar_temperature";
    private static final String STATUS_BAR_TEMPERATURE_STYLE = "status_bar_temperature_style";
    private static final String PREF_STATUS_BAR_WEATHER_SIZE = "status_bar_weather_size";
    private static final String PREF_STATUS_BAR_WEATHER_FONT_STYLE = "status_bar_weather_font_style";
	private static final String PREF_STATUS_BAR_WEATHER_COLOR = "status_bar_weather_color";
	private static final String PREF_STATUS_BAR_WEATHER_IMAGE_COLOR = "status_bar_weather_image_color";

	private static final String CATEGORY_WEATHER = "weather_category";
	private static final String WEATHER_ICON_PACK = "weather_icon_pack";
	private static final String DEFAULT_WEATHER_ICON_PACKAGE = "org.omnirom.omnijaws";
	private static final String WEATHER_SERVICE_PACKAGE = "org.omnirom.omnijaws";
	private static final String CHRONUS_ICON_PACK_INTENT = "com.dvtonder.chronus.ICON_PACK";

    private ListPreference mStatusBarTemperature;
    private ListPreference mStatusBarTemperatureStyle;
    private SeekBarPreference mStatusBarTemperatureSize;
    private ListPreference mStatusBarTemperatureFontStyle;
    private ColorPickerPreference mStatusBarTemperatureColor;
    private ColorPickerPreference mStatusBarTemperatureImageColor;

    private PreferenceCategory mWeatherCategory;
    private ListPreference mWeatherIconPack;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.status_bar_weather);

        int intColor;
        String hexColor;

        ContentResolver resolver = getActivity().getContentResolver();
        mStatusBarTemperature = (ListPreference) findPreference(STATUS_BAR_TEMPERATURE);
        int temperatureShow = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_SHOW_WEATHER_TEMP, 0,
                UserHandle.USER_CURRENT);
        mStatusBarTemperature.setValue(String.valueOf(temperatureShow));
        mStatusBarTemperature.setSummary(mStatusBarTemperature.getEntry());
        mStatusBarTemperature.setOnPreferenceChangeListener(this);

        mStatusBarTemperatureStyle = (ListPreference) findPreference(STATUS_BAR_TEMPERATURE_STYLE);
        int temperatureStyle = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_WEATHER_TEMP_STYLE, 0,
                UserHandle.USER_CURRENT);
        mStatusBarTemperatureStyle.setValue(String.valueOf(temperatureStyle));
        mStatusBarTemperatureStyle.setSummary(mStatusBarTemperatureStyle.getEntry());
        mStatusBarTemperatureStyle.setOnPreferenceChangeListener(this);

        mStatusBarTemperatureSize = (SeekBarPreference) findPreference(PREF_STATUS_BAR_WEATHER_SIZE);
        mStatusBarTemperatureSize.setValue(Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_WEATHER_SIZE, 14));
        mStatusBarTemperatureSize.setOnPreferenceChangeListener(this);

        mStatusBarTemperatureFontStyle = (ListPreference) findPreference(PREF_STATUS_BAR_WEATHER_FONT_STYLE);
        mStatusBarTemperatureFontStyle.setOnPreferenceChangeListener(this);
        mStatusBarTemperatureFontStyle.setValue(Integer.toString(Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_WEATHER_FONT_STYLE, 0)));
        mStatusBarTemperatureFontStyle.setSummary(mStatusBarTemperatureFontStyle.getEntry());

        mStatusBarTemperatureColor =
            (ColorPickerPreference) findPreference(PREF_STATUS_BAR_WEATHER_COLOR);
        mStatusBarTemperatureColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_WEATHER_COLOR, 0xffffffff);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
            mStatusBarTemperatureColor.setSummary(hexColor);
            mStatusBarTemperatureColor.setNewPreviewColor(intColor);

        mStatusBarTemperatureImageColor =
            (ColorPickerPreference) findPreference(PREF_STATUS_BAR_WEATHER_IMAGE_COLOR);
        mStatusBarTemperatureImageColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_WEATHER_IMAGE_COLOR, 0xffffffff);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mStatusBarTemperatureImageColor.setSummary(hexColor);
        mStatusBarTemperatureImageColor.setNewPreviewColor(intColor);

        initweathercat();
        updateWeatherOptions();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mStatusBarTemperature) {
            int temperatureShow = Integer.valueOf((String) newValue);
            int index = mStatusBarTemperature.findIndexOfValue((String) newValue);
            Settings.System.putIntForUser(
                    resolver, Settings.System.STATUS_BAR_SHOW_WEATHER_TEMP, temperatureShow,
                    UserHandle.USER_CURRENT);
            mStatusBarTemperature.setSummary(
                    mStatusBarTemperature.getEntries()[index]);
            updateWeatherOptions();
            return true;
        } else if (preference == mStatusBarTemperatureStyle) {
            int temperatureStyle = Integer.valueOf((String) newValue);
            int index = mStatusBarTemperatureStyle.findIndexOfValue((String) newValue);
            Settings.System.putIntForUser(
                    resolver, Settings.System.STATUS_BAR_WEATHER_TEMP_STYLE, temperatureStyle,
                    UserHandle.USER_CURRENT);
            mStatusBarTemperatureStyle.setSummary(
                    mStatusBarTemperatureStyle.getEntries()[index]);
            return true;
        } else if (preference == mStatusBarTemperatureSize) {
            int width = ((Integer)newValue).intValue();
            Settings.System.putInt(resolver,
                    Settings.System.STATUS_BAR_WEATHER_SIZE, width);
            return true;
        } else if (preference == mStatusBarTemperatureFontStyle) {
            int val = Integer.parseInt((String) newValue);
            int index = mStatusBarTemperatureFontStyle.findIndexOfValue((String) newValue);
            Settings.System.putInt(resolver,
                    Settings.System.STATUS_BAR_WEATHER_FONT_STYLE, val);
            mStatusBarTemperatureFontStyle.setSummary(mStatusBarTemperatureFontStyle.getEntries()[index]);
            return true;
        } else if (preference == mStatusBarTemperatureColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUS_BAR_WEATHER_COLOR, intHex);
            return true;
        } else if (preference == mStatusBarTemperatureImageColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUS_BAR_WEATHER_IMAGE_COLOR, intHex);
            return true;
        } else if (preference == mWeatherIconPack) {
                String value = (String) newValue;
                Settings.System.putString(getContentResolver(),
                    Settings.System.OMNIJAWS_WEATHER_ICON_PACK, value);
                int valueIndex = mWeatherIconPack.findIndexOfValue(value);
                mWeatherIconPack.setSummary(mWeatherIconPack.getEntries()[valueIndex]);
               return true;
        } 
        return false;
    }

    private void updateWeatherOptions() {
        ContentResolver resolver = getActivity().getContentResolver();
        int status = Settings.System.getInt(
                resolver, Settings.System.STATUS_BAR_SHOW_WEATHER_TEMP, 0);
        if (status == 0) {
            mStatusBarTemperatureStyle.setEnabled(false);
            mStatusBarTemperatureColor.setEnabled(false);
            mStatusBarTemperatureSize.setEnabled(false);
            mStatusBarTemperatureFontStyle.setEnabled(false);
            mStatusBarTemperatureImageColor.setEnabled(false);
        } else if (status == 1 || status == 2){
            mStatusBarTemperatureStyle.setEnabled(true);
            mStatusBarTemperatureColor.setEnabled(true);
            mStatusBarTemperatureSize.setEnabled(true);
            mStatusBarTemperatureFontStyle.setEnabled(true);
            mStatusBarTemperatureImageColor.setEnabled(true);
        } else if (status == 3 || status == 4) {
            mStatusBarTemperatureStyle.setEnabled(true);
            mStatusBarTemperatureColor.setEnabled(true);
            mStatusBarTemperatureSize.setEnabled(true);
            mStatusBarTemperatureFontStyle.setEnabled(true);
            mStatusBarTemperatureImageColor.setEnabled(false);
        } else if (status == 5) {
            mStatusBarTemperatureStyle.setEnabled(true);
            mStatusBarTemperatureColor.setEnabled(false);
            mStatusBarTemperatureSize.setEnabled(false);
            mStatusBarTemperatureFontStyle.setEnabled(false);
            mStatusBarTemperatureImageColor.setEnabled(true);
        }
    }

   public void initweathercat() {
        mWeatherCategory = (PreferenceCategory) getPreferenceScreen().findPreference(CATEGORY_WEATHER);
             if (mWeatherCategory != null && !isOmniJawsServiceInstalled()) {
             getPreferenceScreen().removePreference(mWeatherCategory);
             } else {
             String settingsJaws = Settings.System.getString(getContentResolver(),
                     Settings.System.OMNIJAWS_WEATHER_ICON_PACK);
             if (settingsJaws == null) {
                 settingsJaws = DEFAULT_WEATHER_ICON_PACKAGE;
             }
             mWeatherIconPack = (ListPreference) findPreference(WEATHER_ICON_PACK);
 
             List<String> entriesJaws = new ArrayList<String>();
             List<String> valuesJaws = new ArrayList<String>();
             getAvailableWeatherIconPacks(entriesJaws, valuesJaws);
             mWeatherIconPack.setEntries(entriesJaws.toArray(new String[entriesJaws.size()]));
             mWeatherIconPack.setEntryValues(valuesJaws.toArray(new String[valuesJaws.size()]));
 
             int valueJawsIndex = mWeatherIconPack.findIndexOfValue(settingsJaws);
             if (valueJawsIndex == -1) {
                 // no longer found
                 settingsJaws = DEFAULT_WEATHER_ICON_PACKAGE;
                 Settings.System.putString(getContentResolver(),
                         Settings.System.OMNIJAWS_WEATHER_ICON_PACK, settingsJaws);
                 valueJawsIndex = mWeatherIconPack.findIndexOfValue(settingsJaws);
             }
             mWeatherIconPack.setValueIndex(valueJawsIndex >= 0 ? valueJawsIndex : 0);
             mWeatherIconPack.setSummary(mWeatherIconPack.getEntry());
             mWeatherIconPack.setOnPreferenceChangeListener(this);
          }
   }

     @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

     private boolean isOmniJawsServiceInstalled() {
         return PackageUtils.isAvailableApp(WEATHER_SERVICE_PACKAGE, getActivity());
     }
 
     private void getAvailableWeatherIconPacks(List<String> entries, List<String> values) {
         Intent i = new Intent();
         PackageManager packageManager = getPackageManager();
         i.setAction("org.omnirom.WeatherIconPack");
         for (ResolveInfo r : packageManager.queryIntentActivities(i, 0)) {
             String packageName = r.activityInfo.packageName;
             Log.d("maxwen", packageName);
             if (packageName.equals(DEFAULT_WEATHER_ICON_PACKAGE)) {
                 values.add(0, r.activityInfo.name);
             } else {
                 values.add(r.activityInfo.name);
             }
             String label = r.activityInfo.loadLabel(getPackageManager()).toString();
             if (label == null) {
                 label = r.activityInfo.packageName;
             }
             if (packageName.equals(DEFAULT_WEATHER_ICON_PACKAGE)) {
                 entries.add(0, label);
             } else {
                 entries.add(label);
             }
         }
         i = new Intent(Intent.ACTION_MAIN);
         i.addCategory(CHRONUS_ICON_PACK_INTENT);
         for (ResolveInfo r : packageManager.queryIntentActivities(i, 0)) {
             String packageName = r.activityInfo.packageName;
             values.add(packageName + ".weather");
             String label = r.activityInfo.loadLabel(getPackageManager()).toString();
             if (label == null) {
                 label = r.activityInfo.packageName;
             }
             entries.add(label);
         }
     }
 
     private boolean isOmniJawsEnabled() {
         final Uri SETTINGS_URI
             = Uri.parse("content://org.omnirom.omnijaws.provider/settings");
 
         final String[] SETTINGS_PROJECTION = new String[] {
             "enabled"
         };
 
         final Cursor c = getContentResolver().query(SETTINGS_URI, SETTINGS_PROJECTION,
                 null, null, null);
         if (c != null) {
             int count = c.getCount();
             if (count == 1) {
                 c.moveToPosition(0);
                 boolean enabled = c.getInt(0) == 1;
                 return enabled;
             }
         }
         return true;
     }



}

