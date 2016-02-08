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


import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SwitchPreference;
import android.provider.Settings;
import com.android.settings.util.Helpers;
import org.cyanogenmod.internal.util.CmLockPatternUtils;
import com.android.settings.Utils;
import android.provider.SearchIndexableResource;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import com.android.internal.logging.MetricsLogger;
import cyanogenmod.providers.CMSettings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.List;
import java.util.ArrayList;

public class NotificationPanel extends SettingsPreferenceFragment  implements Preference.OnPreferenceChangeListener, Indexable{
 private static final String PREF_STATUS_BAR_CLOCK_FONT_STYLE = "header_clock_font_style";
 private static final String PREF_STATUS_BAR_WEATHER_FONT_STYLE = "header_weather_font_style";	
 private static final String PREF_STATUS_BAR_HEADER_FONT_STYLE = "status_bar_header_font_style";
 private static final String PREF_CUSTOM_HEADER = "status_bar_custom_header";
 private static final String PREF_CUSTOM_HEADER_DEFAULT = "status_bar_custom_header_default";
 private static final String PREF_ENABLE_TASK_MANAGER = "enable_task_manager";
	


    private ListPreference mStatusBarClockFontStyle;	
    private ListPreference mStatusBarWeatherFontStyle;
    private SwitchPreference mCustomHeader;	
    private ListPreference mCustomHeaderDefault;
    private SwitchPreference mEnableTaskManager;
    private ListPreference mStatusBarHeaderFontStyle;		

 @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.notification_panel_customizations);
        PreferenceScreen prefSet = getPreferenceScreen();
        final ContentResolver resolver = getActivity().getContentResolver();

 
        // Status bar custom header
        mCustomHeader = (SwitchPreference) prefSet.findPreference(PREF_CUSTOM_HEADER);
        mCustomHeader.setChecked((Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_CUSTOM_HEADER, 0) == 1));
        mCustomHeader.setOnPreferenceChangeListener(this);

         // Status bar custom header hd
        mCustomHeaderDefault = (ListPreference) findPreference(PREF_CUSTOM_HEADER_DEFAULT);
        mCustomHeaderDefault.setOnPreferenceChangeListener(this);
           int customHeaderDefault = Settings.System.getInt(getContentResolver(),
                Settings.System.STATUS_BAR_CUSTOM_HEADER_DEFAULT, 0);
        mCustomHeaderDefault.setValue(String.valueOf(customHeaderDefault));

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
	} else  if (preference == mCustomHeader) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUS_BAR_CUSTOM_HEADER,
                    (Boolean) newValue ? 1 : 0);
            return true;
        } else if (preference == mCustomHeaderDefault) {
           int customHeaderDefault = Integer.valueOf((String) newValue);
            Settings.System.putIntForUser(getContentResolver(), 
                    Settings.System.STATUS_BAR_CUSTOM_HEADER_DEFAULT,
                    customHeaderDefault, UserHandle.USER_CURRENT);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUS_BAR_CUSTOM_HEADER,
                    0);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUS_BAR_CUSTOM_HEADER,
                    1);
            return true;
	} else if (preference == mStatusBarHeaderFontStyle) {
                int val = Integer.parseInt((String) newValue);
                int index = mStatusBarHeaderFontStyle.findIndexOfValue((String) newValue);
                Settings.System.putIntForUser(resolver,
                        Settings.System.STATUS_BAR_HEADER_FONT_STYLE, val, UserHandle.USER_CURRENT);
                mStatusBarHeaderFontStyle.setSummary(mStatusBarHeaderFontStyle.getEntries()[index]);
                return true;
	}
	return false;
	}

	@Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
	 if  (preference == mEnableTaskManager) {
            boolean enabled = ((SwitchPreference)preference).isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.ENABLE_TASK_MANAGER, enabled ? 1:0);  
	}    
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

}
