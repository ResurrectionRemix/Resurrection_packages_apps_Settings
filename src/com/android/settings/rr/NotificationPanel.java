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

public class NotificationPanel extends SettingsPreferenceFragment  implements Preference.OnPreferenceChangeListener, Indexable{
 private static final String PREF_STATUS_BAR_CLOCK_FONT_STYLE = "header_clock_font_style";
 private static final String PREF_STATUS_BAR_WEATHER_FONT_STYLE = "header_weather_font_style";	
 private static final String PREF_STATUS_BAR_HEADER_FONT_STYLE = "status_bar_header_font_style";
 private static final String PREF_STATUS_BAR_DETAIL_FONT_STYLE = "header_detail_font_style";
 private static final String PREF_STATUS_BAR_DATE_FONT_STYLE = "header_date_font_style";	
 private static final String PREF_STATUS_BAR_ALARM_FONT_STYLE = "header_alarm_font_style";	
 private static final String PREF_ENABLE_TASK_MANAGER = "enable_task_manager";

 private static final String HEADER_CLOCK_COLOR = "header_clock_color";
 private static final String HEADER_DETAIL_COLOR = "header_detail_color";
 private static final String HEADER_WEATHERONE_COLOR = "header_weatherone_color";
 private static final String HEADER_WEATHERTWO_COLOR = "header_weathertwo_color";
 private static final String HEADER_BATTERY_COLOR = "header_battery_text_color";
 private static final String HEADER_ALARM_COLOR = "header_alarm_text_color";
 private static final String CUSTOM_HEADER_IMAGE = "status_bar_custom_header";
 private static final String DAYLIGHT_HEADER_PACK = "daylight_header_pack";
 private static final String DEFAULT_HEADER_PACKAGE = "com.android.systemui";
 private static final String CUSTOM_HEADER_IMAGE_SHADOW = "status_bar_custom_header_shadow";

    static final int DEFAULT = 0xffffffff;
    private static final int MENU_RESET = Menu.FIRST;
	
    private ListPreference mStatusBarClockFontStyle;	
    private ListPreference mStatusBarWeatherFontStyle;
    private SwitchPreference mEnableTaskManager;
    private ListPreference mStatusBarHeaderFontStyle;	
    private ListPreference mStatusBarDateFontStyle;	
    private ListPreference mStatusBarDetailFontStyle;
    private ListPreference mStatusBarAlarmFontStyle;	

    private ColorPickerPreference mHeaderCLockColor;
    private ColorPickerPreference mHeaderDetailColor;
    private ColorPickerPreference mHeaderWeatheroneColor;
    private ColorPickerPreference mHeaderWeathertwoColor;	
    private ColorPickerPreference mBatteryColor;
    private ColorPickerPreference mAlarmColor;	
    private ListPreference mDaylightHeaderPack;
    private SwitchPreference mCustomHeaderImage;	
    private SeekBarPreference mHeaderShadow; 

 @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.notification_panel_customizations);
        PreferenceScreen prefSet = getPreferenceScreen();
        final ContentResolver resolver = getActivity().getContentResolver();

   	int intColor;
        String hexColor;

 

 	// Status bar header Clock font style
            mStatusBarClockFontStyle = (ListPreference) findPreference(PREF_STATUS_BAR_CLOCK_FONT_STYLE);
            mStatusBarClockFontStyle.setOnPreferenceChangeListener(this);
            mStatusBarClockFontStyle.setValue(Integer.toString(Settings.System.getIntForUser(resolver,
                    Settings.System.HEADER_CLOCK_FONT_STYLE , 0, UserHandle.USER_CURRENT)));
            mStatusBarClockFontStyle.setSummary(mStatusBarClockFontStyle.getEntry());
  	// Status bar header Weather font style
            mStatusBarWeatherFontStyle = (ListPreference) findPreference(PREF_STATUS_BAR_WEATHER_FONT_STYLE);
            mStatusBarWeatherFontStyle .setOnPreferenceChangeListener(this);
            mStatusBarWeatherFontStyle.setValue(Integer.toString(Settings.System.getIntForUser(resolver,
                    Settings.System.HEADER_WEATHER_FONT_STYLE, 0, UserHandle.USER_CURRENT)));
            mStatusBarWeatherFontStyle .setSummary(mStatusBarWeatherFontStyle.getEntry());

        // Task manager
        mEnableTaskManager = (SwitchPreference) prefSet.findPreference(PREF_ENABLE_TASK_MANAGER);
        mEnableTaskManager.setChecked((Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.ENABLE_TASK_MANAGER, 0) == 1));

 	 // Status bar header font style
            mStatusBarHeaderFontStyle = (ListPreference) findPreference(PREF_STATUS_BAR_HEADER_FONT_STYLE);
            mStatusBarHeaderFontStyle.setOnPreferenceChangeListener(this);
            mStatusBarHeaderFontStyle.setValue(Integer.toString(Settings.System.getIntForUser(resolver,
                    Settings.System.STATUS_BAR_HEADER_FONT_STYLE, 0, UserHandle.USER_CURRENT)));
            mStatusBarHeaderFontStyle.setSummary(mStatusBarHeaderFontStyle.getEntry());

  	// Status bar Detail font style
            mStatusBarDetailFontStyle = (ListPreference) findPreference(PREF_STATUS_BAR_DETAIL_FONT_STYLE);
            mStatusBarDetailFontStyle.setOnPreferenceChangeListener(this);
            mStatusBarDetailFontStyle.setValue(Integer.toString(Settings.System.getIntForUser(resolver,
                    Settings.System.HEADER_DETAIL_FONT_STYLE, 0, UserHandle.USER_CURRENT)));
            mStatusBarDetailFontStyle.setSummary(mStatusBarDetailFontStyle.getEntry());

 	 // Status bar header Date  font style
            mStatusBarDateFontStyle = (ListPreference) findPreference(PREF_STATUS_BAR_DATE_FONT_STYLE);
            mStatusBarDateFontStyle .setOnPreferenceChangeListener(this);
            mStatusBarDateFontStyle .setValue(Integer.toString(Settings.System.getIntForUser(resolver,
                    Settings.System.HEADER_DATE_FONT_STYLE, 0, UserHandle.USER_CURRENT)));
            mStatusBarDateFontStyle .setSummary(mStatusBarDateFontStyle .getEntry());

           // Status bar header Alarm font style
            mStatusBarAlarmFontStyle = (ListPreference) findPreference(PREF_STATUS_BAR_ALARM_FONT_STYLE);
            mStatusBarAlarmFontStyle.setOnPreferenceChangeListener(this);
            mStatusBarAlarmFontStyle.setValue(Integer.toString(Settings.System.getIntForUser(resolver,
                    Settings.System.HEADER_ALARM_FONT_STYLE, 0, UserHandle.USER_CURRENT)));
            mStatusBarAlarmFontStyle.setSummary(mStatusBarAlarmFontStyle.getEntry());

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

	// header image packs
        final boolean customHeaderImage = Settings.System.getInt(getContentResolver(),
                Settings.System.STATUS_BAR_CUSTOM_HEADER, 0) == 1;
        mCustomHeaderImage = (SwitchPreference) findPreference(CUSTOM_HEADER_IMAGE);
        mCustomHeaderImage.setChecked(customHeaderImage);

         String imageHeaderPackage = Settings.System.getString(getContentResolver(),
                 Settings.System.STATUS_BAR_DAYLIGHT_HEADER_PACK);
         if (imageHeaderPackage == null) {
             imageHeaderPackage = DEFAULT_HEADER_PACKAGE;
         }
         mDaylightHeaderPack = (ListPreference) findPreference(DAYLIGHT_HEADER_PACK);
         List<String> entries = new ArrayList<String>();
         List<String> values = new ArrayList<String>();
         getAvailableHeaderPacks(entries, values);
         mDaylightHeaderPack.setEntries(entries.toArray(new String[entries.size()]));
         mDaylightHeaderPack.setEntryValues(values.toArray(new String[values.size()]));
 
         int valueIndexHeader = mDaylightHeaderPack.findIndexOfValue(imageHeaderPackage);
         if (valueIndexHeader == -1) {
             // no longer found
             imageHeaderPackage = DEFAULT_HEADER_PACKAGE;
             Settings.System.putString(getContentResolver(),
                     Settings.System.STATUS_BAR_DAYLIGHT_HEADER_PACK, imageHeaderPackage);
             valueIndexHeader = mDaylightHeaderPack.findIndexOfValue(imageHeaderPackage);
         }
         mDaylightHeaderPack.setValueIndex(valueIndexHeader >= 0 ? valueIndexHeader : 0);
         mDaylightHeaderPack.setSummary(mDaylightHeaderPack.getEntry());
         mDaylightHeaderPack.setOnPreferenceChangeListener(this);

        mHeaderShadow = (SeekBarPreference) findPreference(CUSTOM_HEADER_IMAGE_SHADOW);
        final int headerShadow = Settings.System.getInt(getContentResolver(),
                Settings.System.STATUS_BAR_CUSTOM_HEADER_SHADOW, 0);
        mHeaderShadow.setValue((int)((headerShadow / 255) * 100));
        mHeaderShadow.setOnPreferenceChangeListener(this);
	
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
	if (preference == mStatusBarClockFontStyle) {
                int val = Integer.parseInt((String) newValue);
                int index = mStatusBarClockFontStyle.findIndexOfValue((String) newValue);
                Settings.System.putIntForUser(resolver,
                        Settings.System.HEADER_CLOCK_FONT_STYLE, val, UserHandle.USER_CURRENT);
                mStatusBarClockFontStyle.setSummary(mStatusBarClockFontStyle.getEntries()[index]);
                return true;
	} else if (preference == mStatusBarWeatherFontStyle) {
                int val = Integer.parseInt((String) newValue);
                int index = mStatusBarWeatherFontStyle.findIndexOfValue((String) newValue);
                Settings.System.putIntForUser(resolver,
                        Settings.System.HEADER_WEATHER_FONT_STYLE, val, UserHandle.USER_CURRENT);
                mStatusBarWeatherFontStyle.setSummary(mStatusBarWeatherFontStyle.getEntries()[index]);
                return true;
	} else if (preference == mStatusBarHeaderFontStyle) {
                int val = Integer.parseInt((String) newValue);
                int index = mStatusBarHeaderFontStyle.findIndexOfValue((String) newValue);
                Settings.System.putIntForUser(resolver,
                        Settings.System.STATUS_BAR_HEADER_FONT_STYLE, val, UserHandle.USER_CURRENT);
                mStatusBarHeaderFontStyle.setSummary(mStatusBarHeaderFontStyle.getEntries()[index]);
                return true;
	} else if (preference == mStatusBarDateFontStyle) {
                int val = Integer.parseInt((String) newValue);
                int index = mStatusBarDateFontStyle.findIndexOfValue((String) newValue);
                Settings.System.putIntForUser(resolver,
                        Settings.System.HEADER_DATE_FONT_STYLE, val, UserHandle.USER_CURRENT);
                mStatusBarDateFontStyle.setSummary(mStatusBarDateFontStyle.getEntries()[index]);
                return true;
	} else if (preference == mStatusBarDetailFontStyle) {
                int val = Integer.parseInt((String) newValue);
                int index = mStatusBarDetailFontStyle.findIndexOfValue((String) newValue);
                Settings.System.putIntForUser(resolver,
                        Settings.System.HEADER_DETAIL_FONT_STYLE, val, UserHandle.USER_CURRENT);
                mStatusBarDetailFontStyle.setSummary(mStatusBarDetailFontStyle.getEntries()[index]);
                return true;
	} else if (preference == mStatusBarAlarmFontStyle) {
                int val = Integer.parseInt((String) newValue);
                int index = mStatusBarAlarmFontStyle.findIndexOfValue((String) newValue);
                Settings.System.putIntForUser(resolver,
                        Settings.System.HEADER_ALARM_FONT_STYLE, val, UserHandle.USER_CURRENT);
                mStatusBarAlarmFontStyle.setSummary(mStatusBarAlarmFontStyle.getEntries()[index]);
                return true;
	} else if (preference == mHeaderCLockColor) {
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
         } else if (preference == mDaylightHeaderPack) {
             String value = (String) newValue;
             Settings.System.putString(getContentResolver(),
                     Settings.System.STATUS_BAR_DAYLIGHT_HEADER_PACK, value);
             int valueIndex = mDaylightHeaderPack.findIndexOfValue(value);
             mDaylightHeaderPack.setSummary(mDaylightHeaderPack.getEntries()[valueIndex]);
             return true;
         } else if (preference == mHeaderShadow) {
            Integer headerShadow = (Integer) newValue;
            int realHeaderValue = (int) (((double) headerShadow / 100) * 255);
            Settings.System.putInt(getContentResolver(),
              Settings.System.STATUS_BAR_CUSTOM_HEADER_SHADOW, realHeaderValue);
	    return true;
	 }
	return false;
	}

  private void getAvailableHeaderPacks(List<String> entries, List<String> values) {
        Intent i = new Intent();
        PackageManager packageManager = getPackageManager();
        i.setAction("org.omnirom.DaylightHeaderPack");
        for (ResolveInfo r : packageManager.queryIntentActivities(i, 0)) {
            String packageName = r.activityInfo.packageName;
            if (packageName.equals(DEFAULT_HEADER_PACKAGE)) {
                values.add(0, packageName);
            } else {
                values.add(packageName);
            }
            String label = r.activityInfo.loadLabel(getPackageManager()).toString();
            if (label == null) {
                label = r.activityInfo.packageName;
            }
            if (packageName.equals(DEFAULT_HEADER_PACKAGE)) {
                entries.add(0, label);
            } else {
                entries.add(label);
            }
        }
        i.setAction("org.omnirom.DaylightHeaderPack1");
        for (ResolveInfo r : packageManager.queryIntentActivities(i, 0)) {
            String packageName = r.activityInfo.packageName;
            values.add(packageName  + "/" + r.activityInfo.name);
            String label = r.activityInfo.loadLabel(getPackageManager()).toString();
            if (label == null) {
                label = packageName;
            }
            entries.add(label);
        }
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


	@Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
	 if  (preference == mEnableTaskManager) {
            boolean enabled = ((SwitchPreference)preference).isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.ENABLE_TASK_MANAGER, enabled ? 1:0);  
	} else  if (preference == mCustomHeaderImage) {
                final boolean value = ((SwitchPreference)preference).isChecked();
                Settings.System.putInt(getContentResolver(),
                        Settings.System.STATUS_BAR_CUSTOM_HEADER, value ? 1 : 0);
                return true;
            }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

}
