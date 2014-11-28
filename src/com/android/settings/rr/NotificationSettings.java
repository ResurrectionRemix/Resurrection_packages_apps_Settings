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

import android.app.Activity;
import android.content.Context;
import android.content.ContentResolver;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.app.Fragment;
import android.preference.PreferenceFragment;

import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Intent;

import android.provider.Settings;
import android.content.res.Resources;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.rr.Preferences.SystemSettingSwitchPreference;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

public class NotificationSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String DISABLE_IMMERSIVE_MESSAGE = "disable_immersive_message";
    private SwitchPreference mDisableIM;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rr_notif_settings);
        final Activity activity = getActivity(); 
		ContentResolver resolver = getActivity().getContentResolver();

        mDisableIM = (SwitchPreference) findPreference(DISABLE_IMMERSIVE_MESSAGE);
        mDisableIM.setOnPreferenceChangeListener(this);
        int DisableIM = Settings.System.getInt(getContentResolver(),
                DISABLE_IMMERSIVE_MESSAGE, 0);
        mDisableIM.setChecked(DisableIM != 0);

       // Enable or disable mStatusBarImeSwitcher based on boolean: config_show_cmIMESwitcher
       boolean showCmImeSwitcher = getResources().getBoolean(
                   com.android.internal.R.bool.config_show_cmIMESwitcher);
           if (!showCmImeSwitcher) {
               getPreferenceScreen().removePreference(
                       findPreference(Settings.System.STATUS_BAR_IME_SWITCHER));
           }
    }


    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mDisableIM) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(getContentResolver(), DISABLE_IMMERSIVE_MESSAGE,
                    value ? 1 : 0);
            return true;
        } 
        return false;
	}



    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

}
