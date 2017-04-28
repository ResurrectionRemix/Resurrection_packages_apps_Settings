/*Copyright (C) 2015 The ResurrectionRemix Project
     Licensed under the Apache License, Version 2.0 (the "License");
     you may not use this file except in compliance with the License.
     You may obtain a copy of the License at

          http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.
*/
package com.android.settings.rr;


import android.app.AlertDialog;
import android.content.Context;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v14.preference.SwitchPreference;
import android.provider.Settings;


import com.android.internal.logging.MetricsProto.MetricsEvent;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import android.util.Log;

import java.util.List;
import java.util.ArrayList;

import com.android.internal.util.rr.PackageUtils;
import cyanogenmod.providers.CMSettings;

public class QsAdvanced extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
        private static final String TAG = "QsAdvanced";
        private static final String BATTERY_TILE_STYLE = "battery_tile_style";
        private static final String CATEGORY_WEATHER = "weather_category";
        private static final String WEATHER_ICON_PACK = "weather_icon_pack";
        private static final String DEFAULT_WEATHER_ICON_PACKAGE = "org.omnirom.omnijaws";
        private static final String WEATHER_SERVICE_PACKAGE = "org.omnirom.omnijaws";
        private static final String CHRONUS_ICON_PACK_INTENT = "com.dvtonder.chronus.ICON_PACK";
        private PreferenceCategory mWeatherCategory;
        private ListPreference mWeatherIconPack;
        private ListPreference mBatteryTileStyle;
        private int mBatteryTileStyleValue;


    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

             addPreferencesFromResource(R.xml.rr_qs_advanced);

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

             mBatteryTileStyle = (ListPreference) findPreference(BATTERY_TILE_STYLE);
             mBatteryTileStyleValue = Settings.Secure.getInt(getActivity().getContentResolver(),
                     Settings.Secure.BATTERY_TILE_STYLE, 0);
             mBatteryTileStyle.setValue(Integer.toString(mBatteryTileStyleValue));
             mBatteryTileStyle.setSummary(mBatteryTileStyle.getEntry());
             mBatteryTileStyle.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
           if (preference == mWeatherIconPack) {
                String value = (String) newValue;
                Settings.System.putString(getContentResolver(),
                    Settings.System.OMNIJAWS_WEATHER_ICON_PACK, value);
                int valueIndex = mWeatherIconPack.findIndexOfValue(value);
                mWeatherIconPack.setSummary(mWeatherIconPack.getEntries()[valueIndex]);
               return true;
            }  else if (preference == mBatteryTileStyle) {
            mBatteryTileStyleValue = Integer.valueOf((String) newValue);
            int index = mBatteryTileStyle.findIndexOfValue((String) newValue);
            mBatteryTileStyle.setSummary(
                    mBatteryTileStyle.getEntries()[index]);
            Settings.Secure.putInt(getActivity().getContentResolver(),
                    Settings.Secure.BATTERY_TILE_STYLE, mBatteryTileStyleValue);
            return true;
           }
      return false;
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
