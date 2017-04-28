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
import android.content.res.Resources;
import android.provider.Settings;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.logging.MetricsProto.MetricsEvent;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class TaskManagerSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "RecentsUI";

    private static final String TM_KILL_BUTTON = "task_manager_kill_button";


    private ListPreference mKillButton;


    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.rr_tm_ui);
     	final ContentResolver resolver = getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();
        final Resources res = getResources();

        mKillButton = (ListPreference) findPreference(TM_KILL_BUTTON);
        mKillButton.setValue(String.valueOf(Settings.System.getInt(
                getContentResolver(), Settings.System.TASK_MANAGER_KILL_BUTTON, 0)));
        mKillButton.setSummary(mKillButton.getEntry());
        mKillButton.setOnPreferenceChangeListener(this);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue){
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mKillButton) {
            Settings.System.putInt(getContentResolver(), Settings.System.TASK_MANAGER_KILL_BUTTON,
                    Integer.valueOf((String) newValue));
            mKillButton.setValue(String.valueOf(newValue));
            mKillButton.setSummary(mKillButton.getEntry());
            return true;
        }
        return false;
    }
}
