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
import android.os.Bundle;
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

import android.provider.Settings;


import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.util.Helpers;

import com.android.internal.logging.MetricsProto.MetricsEvent;

public class NotificationSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

	private static final String DISABLE_IMMERSIVE_MESSAGE = "disable_immersive_message";
	private static final String NOTIFICATION_GUTS_KILL_APP_BUTTON = "notification_guts_kill_app_button";

	private SwitchPreference mDisableIM;
    private SwitchPreference mNotificationKill;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rr_notif_settings);

		mDisableIM = (SwitchPreference) findPreference(DISABLE_IMMERSIVE_MESSAGE);
        mDisableIM.setOnPreferenceChangeListener(this);
        int DisableIM = Settings.System.getInt(getContentResolver(),
                DISABLE_IMMERSIVE_MESSAGE, 0);
        mDisableIM.setChecked(DisableIM != 0);

        mNotificationKill = (SwitchPreference) findPreference(NOTIFICATION_GUTS_KILL_APP_BUTTON);
        mNotificationKill.setOnPreferenceChangeListener(this);

    }


    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mDisableIM) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(getContentResolver(), DISABLE_IMMERSIVE_MESSAGE,
                    value ? 1 : 0);
            return true;
        }  else if (preference == mNotificationKill) {
                // Setting will only apply to new created notifications.
                // By restarting SystemUI, we can re-create all notifications
                Helpers.showSystemUIrestartDialog(getActivity());
                return true;
        }
        return false;
	}



    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

}
