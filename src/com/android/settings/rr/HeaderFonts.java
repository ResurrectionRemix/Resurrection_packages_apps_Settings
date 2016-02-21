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

public class HeaderFonts extends SettingsPreferenceFragment  implements Preference.OnPreferenceChangeListener {
 private static final String PREF_STATUS_BAR_CLOCK_FONT_STYLE = "header_clock_font_style";
 private static final String PREF_STATUS_BAR_WEATHER_FONT_STYLE = "header_weather_font_style";	
 private static final String PREF_STATUS_BAR_HEADER_FONT_STYLE = "status_bar_header_font_style";
 private static final String PREF_STATUS_BAR_DETAIL_FONT_STYLE = "header_detail_font_style";
 private static final String PREF_STATUS_BAR_DATE_FONT_STYLE = "header_date_font_style";	
 private static final String PREF_STATUS_BAR_ALARM_FONT_STYLE = "header_alarm_font_style";	
	
    private ListPreference mStatusBarClockFontStyle;	
    private ListPreference mStatusBarWeatherFontStyle;
    private SwitchPreference mEnableTaskManager;
    private ListPreference mStatusBarHeaderFontStyle;	
    private ListPreference mStatusBarDateFontStyle;	
    private ListPreference mStatusBarDetailFontStyle;
    private ListPreference mStatusBarAlarmFontStyle;	

 @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.rr_header_fonts);
        PreferenceScreen prefSet = getPreferenceScreen();
        final ContentResolver resolver = getActivity().getContentResolver();

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
	 }
	return false;
	}
}
