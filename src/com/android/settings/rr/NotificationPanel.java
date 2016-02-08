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


    private ListPreference mStatusBarClockFontStyle;	
    private ListPreference mStatusBarWeatherFontStyle;

 @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.notification_panel_customizations);
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
		}
	return false;
	}

}
