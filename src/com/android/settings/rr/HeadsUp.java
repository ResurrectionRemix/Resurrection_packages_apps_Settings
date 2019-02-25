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
import android.content.ContentResolver;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v14.preference.SwitchPreference;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.settings.R;
import com.android.settings.rr.utils.RRUtils;
import com.android.settings.search.Indexable.SearchIndexProvider;
import com.android.settings.SettingsPreferenceFragment;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

public class HeadsUp extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String PREF_HEADS_UP_TIME_OUT = "heads_up_time_out";
    private static final String PREF_HEADS_UP_SNOOZE_TIME = "heads_up_snooze_time";

    private ListPreference mHeadsUpTimeOut;
    private ListPreference mHeadsUpSnoozeTime;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rr_headsup);
        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();
        Resources systemUiResources;
        try {
            systemUiResources = getPackageManager().getResourcesForApplication("com.android.systemui");
        } catch (Exception e) {
            return;
        }
        int defaultTimeOut = systemUiResources.getInteger(systemUiResources.getIdentifier(
                    "com.android.systemui:integer/heads_up_notification_decay", null, null));
        mHeadsUpTimeOut = (ListPreference) findPreference(PREF_HEADS_UP_TIME_OUT);
        mHeadsUpTimeOut.setOnPreferenceChangeListener(this);
        int headsUpTimeOut = Settings.System.getInt(getContentResolver(),
                Settings.System.HEADS_UP_TIMEOUT, defaultTimeOut);
        mHeadsUpTimeOut.setValue(String.valueOf(headsUpTimeOut));
        updateHeadsUpTimeOutSummary(headsUpTimeOut);

        int defaultSnooze = systemUiResources.getInteger(systemUiResources.getIdentifier(
                    "com.android.systemui:integer/heads_up_default_snooze_length_ms", null, null));
        mHeadsUpSnoozeTime = (ListPreference) findPreference(PREF_HEADS_UP_SNOOZE_TIME);
        mHeadsUpSnoozeTime.setOnPreferenceChangeListener(this);
        int headsUpSnooze = Settings.System.getInt(getContentResolver(),
                Settings.System.HEADS_UP_NOTIFICATION_SNOOZE, defaultSnooze);
        mHeadsUpSnoozeTime.setValue(String.valueOf(headsUpSnooze));
        updateHeadsUpSnoozeTimeSummary(headsUpSnooze);

    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mHeadsUpTimeOut) {
            int headsUpTimeOut = Integer.valueOf((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.HEADS_UP_TIMEOUT, headsUpTimeOut);
            updateHeadsUpTimeOutSummary(headsUpTimeOut);
            return true;
        } else if (preference == mHeadsUpSnoozeTime) {
            int headsUpSnooze = Integer.valueOf((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.HEADS_UP_NOTIFICATION_SNOOZE,
                    headsUpSnooze);
            updateHeadsUpSnoozeTimeSummary(headsUpSnooze);
            return true;
        }
        return false;
    }

    private void updateHeadsUpTimeOutSummary(int value) {
        String summary = getResources().getString(R.string.heads_up_time_out_summary,
                value / 1000);
        mHeadsUpTimeOut.setSummary(summary);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    private void updateHeadsUpSnoozeTimeSummary(int value) {
        if (value == 0) {
            mHeadsUpSnoozeTime.setSummary(getResources().getString(R.string.heads_up_snooze_disabled_summary));
        } else if (value == 60000) {
            mHeadsUpSnoozeTime.setSummary(getResources().getString(R.string.heads_up_snooze_summary_one_minute));
        } else {
            String summary = getResources().getString(R.string.heads_up_snooze_summary, value / 60 / 1000);
            mHeadsUpSnoozeTime.setSummary(summary);
        }
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        RRUtils.addSearchIndexProvider(R.xml.rr_headsup);
}
