/*
* Copyright (C) 2016 RR
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SwitchPreference;
import com.android.settings.rr.SeekBarPreference;
import android.provider.Settings;
import com.android.settings.util.Helpers;
import org.cyanogenmod.internal.util.CmLockPatternUtils;
import com.android.settings.Utils;
import android.provider.SearchIndexableResource;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.util.Log;

import com.android.internal.logging.MetricsLogger;
import cyanogenmod.providers.CMSettings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import net.margaritov.preference.colorpicker.ColorPickerPreference;

import java.util.List;
import java.util.ArrayList;

public class HeaderColors extends SettingsPreferenceFragment  implements Preference.OnPreferenceChangeListener {

 private static final String HEADER_CLOCK_COLOR = "header_clock_color";
 private static final String HEADER_DETAIL_COLOR = "header_detail_color";
 private static final String HEADER_WEATHERONE_COLOR = "header_weatherone_color";
 private static final String HEADER_WEATHERTWO_COLOR = "header_weathertwo_color";
 private static final String HEADER_BATTERY_COLOR = "header_battery_text_color";
 private static final String HEADER_ALARM_COLOR = "header_alarm_text_color";

    static final int DEFAULT = 0xffffffff;
    private static final int MENU_RESET = Menu.FIRST;
	

    private ColorPickerPreference mHeaderCLockColor;
    private ColorPickerPreference mHeaderDetailColor;
    private ColorPickerPreference mHeaderWeatheroneColor;
    private ColorPickerPreference mHeaderWeathertwoColor;	
    private ColorPickerPreference mBatteryColor;
    private ColorPickerPreference mAlarmColor;	
 

 @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.rr_header_colors);
        PreferenceScreen prefSet = getPreferenceScreen();
        final ContentResolver resolver = getActivity().getContentResolver();

   	int intColor;
        String hexColor;

        mHeaderCLockColor = (ColorPickerPreference) findPreference(HEADER_CLOCK_COLOR);
        mHeaderCLockColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.HEADER_CLOCK_COLOR, DEFAULT);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mHeaderCLockColor.setSummary(hexColor);
        mHeaderCLockColor.setNewPreviewColor(intColor);

        mHeaderDetailColor = (ColorPickerPreference) findPreference(HEADER_DETAIL_COLOR);
        mHeaderDetailColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.HEADER_DETAIL_COLOR, DEFAULT);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mHeaderDetailColor.setSummary(hexColor);
        mHeaderDetailColor.setNewPreviewColor(intColor);

        mHeaderWeatheroneColor = (ColorPickerPreference) findPreference(HEADER_WEATHERONE_COLOR);
        mHeaderWeatheroneColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.HEADER_WEATHERONE_COLOR, DEFAULT);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mHeaderWeatheroneColor.setSummary(hexColor);
        mHeaderWeatheroneColor.setNewPreviewColor(intColor);

        mHeaderWeathertwoColor = (ColorPickerPreference) findPreference(HEADER_WEATHERTWO_COLOR);
        mHeaderWeathertwoColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.HEADER_WEATHERTWO_COLOR, DEFAULT);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mHeaderWeathertwoColor.setSummary(hexColor);
        mHeaderWeathertwoColor.setNewPreviewColor(intColor);

       	mBatteryColor = (ColorPickerPreference) findPreference(HEADER_BATTERY_COLOR);
        mBatteryColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.HEADER_BATTERY_TEXT_COLOR, DEFAULT);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mBatteryColor.setSummary(hexColor);
        mBatteryColor.setNewPreviewColor(intColor);

        mAlarmColor = (ColorPickerPreference) findPreference(HEADER_ALARM_COLOR);
        mAlarmColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.HEADER_ALARM_TEXT_COLOR , DEFAULT);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mAlarmColor.setSummary(hexColor);
        mAlarmColor.setNewPreviewColor(intColor);
	
	setHasOptionsMenu(true);

}


    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.NOTIFICATION_DRAWER_SETTINGS;
    }

    @Override
    public void onResume() {
        super.onResume();
    }


	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
	ContentResolver resolver = getActivity().getContentResolver();
	Resources res = getResources();
	  if (preference == mHeaderCLockColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.HEADER_CLOCK_COLOR, intHex);
            return true;
         } else if (preference == mHeaderDetailColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.HEADER_DETAIL_COLOR, intHex);
            return true;
         } else if (preference == mHeaderWeatheroneColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.HEADER_WEATHERONE_COLOR, intHex);
            return true;
         } else if (preference == mHeaderWeathertwoColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.HEADER_WEATHERTWO_COLOR, intHex);
            return true;
         }  else if (preference == mBatteryColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.HEADER_BATTERY_TEXT_COLOR, intHex);
            return true;
         }  else if (preference == mAlarmColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.HEADER_ALARM_TEXT_COLOR, intHex);
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
        alertDialog.setTitle(R.string.header_colors_reset_title);
        alertDialog.setMessage(R.string.header_colors_reset_message);
        alertDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                resetValues();
            }
        });
        alertDialog.setNegativeButton(R.string.cancel, null);
        alertDialog.create().show();
    }

    private void resetValues() {
        Settings.System.putInt(getContentResolver(),
                Settings.System.HEADER_CLOCK_COLOR, DEFAULT);
        mHeaderCLockColor.setNewPreviewColor(DEFAULT);
        mHeaderCLockColor.setSummary(R.string.default_string);
        Settings.System.putInt(getContentResolver(),
                Settings.System.HEADER_DETAIL_COLOR, DEFAULT);
        mHeaderDetailColor.setNewPreviewColor(DEFAULT);
        mHeaderDetailColor.setSummary(R.string.default_string);
        Settings.System.putInt(getContentResolver(),
                Settings.System.HEADER_WEATHERONE_COLOR, DEFAULT);
        mHeaderWeatheroneColor.setNewPreviewColor(DEFAULT);
        mHeaderWeatheroneColor.setSummary(R.string.default_string);
        Settings.System.putInt(getContentResolver(),
                Settings.System.HEADER_WEATHERTWO_COLOR, DEFAULT);
	mHeaderWeathertwoColor.setNewPreviewColor(DEFAULT);
        mHeaderWeathertwoColor.setSummary(R.string.default_string);
        Settings.System.putInt(getContentResolver(),
                Settings.System.HEADER_BATTERY_TEXT_COLOR, DEFAULT);
        mBatteryColor.setNewPreviewColor(DEFAULT);
        mBatteryColor.setSummary(R.string.default_string);
        Settings.System.putInt(getContentResolver(),
                Settings.System.HEADER_ALARM_TEXT_COLOR, DEFAULT);
	        mAlarmColor.setNewPreviewColor(DEFAULT);
        mAlarmColor.setSummary(R.string.default_string);

    }

}
