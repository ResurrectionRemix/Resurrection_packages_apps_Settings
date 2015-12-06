/*
* Copyright (C) 2014 The CyanogenMod Project
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
package com.android.settings.cyanogenmod;


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

import com.android.internal.logging.MetricsLogger;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class NotificationDrawerSettings extends SettingsPreferenceFragment  implements Preference.OnPreferenceChangeListener{
    private static final String FORCE_EXPANDED_NOTIFICATIONS = "force_expanded_notifications";
private static final String PREF_CUSTOM_HEADER = "status_bar_custom_header";
    private static final String PREF_CUSTOM_HEADER_DEFAULT = "status_bar_custom_header_default";
 private static final String PREF_ENABLE_TASK_MANAGER = "enable_task_manager";

    private SwitchPreference mForceExpanded;
    private SwitchPreference mCustomHeader;	
    private ListPreference mCustomHeaderDefault;
    private SwitchPreference mEnableTaskManager;
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.notification_drawer_settings);
        PreferenceScreen prefSet = getPreferenceScreen();
        final ContentResolver resolver = getActivity().getContentResolver();

	mForceExpanded = (SwitchPreference) findPreference(FORCE_EXPANDED_NOTIFICATIONS);
        mForceExpanded.setChecked((Settings.System.getInt(resolver, Settings.System.FORCE_EXPANDED_NOTIFICATIONS, 0) == 1));

 
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


        // Task manager
        mEnableTaskManager = (SwitchPreference) prefSet.findPreference(PREF_ENABLE_TASK_MANAGER);
        mEnableTaskManager.setChecked((Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.ENABLE_TASK_MANAGER, 0) == 1));

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
	 if (preference == mCustomHeader) {
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
         }
         return false;
	}


    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if  (preference == mForceExpanded) {
            boolean checked = ((SwitchPreference)preference).isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.FORCE_EXPANDED_NOTIFICATIONS, checked ? 1:0);
            return true;
        } else  if  (preference == mEnableTaskManager) {
            boolean enabled = ((SwitchPreference)preference).isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.ENABLE_TASK_MANAGER, enabled ? 1:0);  
	}    
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }


}

