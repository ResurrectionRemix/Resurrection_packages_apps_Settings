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
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.Spannable;
import android.text.TextUtils;
import android.widget.EditText;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

import com.android.settings.R;
import com.android.settings.rr.utils.RRUtils;
import com.android.settings.search.Indexable.SearchIndexProvider;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.dashboard.DashboardSummary;

public class DashBoardSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "UI";
    private static final String RR_CONFIG = "rr_config_style";

    private ListPreference mConfig;
    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.rr_dashboard_settings);
        final ContentResolver resolver = getActivity().getContentResolver();

        mConfig = (ListPreference) findPreference(RR_CONFIG);
        mConfig.setValue(String.valueOf(Settings.System.getInt(
                getContentResolver(), Settings.System.RR_CONFIG_STYLE, 0)));
        mConfig.setSummary(mConfig.getEntry());
        mConfig.setOnPreferenceChangeListener(this);
       /* mDashboardPortraitColumns = (SeekBarPreference) findPreference(KEY_DASHBOARD_PORTRAIT_COLUMNS);
        int columnsPortrait = Settings.System.getInt(resolver,
                Settings.SystemCustomSummary = (Preference) findPreference(PREF_RR_SETTINGS_SUMMARY);m.DASHBOARD_PORTRAIT_COLUMNS, DashboardSummary.mNumColumns);
        mDashboardPortraitColumns.setValue(columnsPortrait / 1);
        mDashboardPortraitColumns.setOnPreferenceChangeListener(this);

        mDashboardLandscapeColumns = (SeekBarPreference) findPreference(KEY_DASHBOARD_LANDSCAPE_COLUMNS);
        int columnsLandscape = Settings.System.getInt(resolver,
                Settings.System.DASHBOARD_LANDSCAPE_COLUMNS, 2);
        mDashboardLandscapeColumns.setValue(columnsLandscape / 1);
        mDashboardLandscapeColumns.setOnPreferenceChangeListener(this);
        mCustomSummary = (Preference) findPreference(PREF_RR_SETTINGS_SUMMARY); */

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final ContentResolver resolver = getActivity().getContentResolver();
           if (preference == mConfig) {
            int style = Integer.parseInt((String) objValue);
            Settings.System.putInt(getContentResolver(), Settings.System.RR_CONFIG_STYLE,
            Integer.valueOf((String) objValue));
            mConfig.setValue(String.valueOf(objValue));
            mConfig.setSummary(mConfig.getEntry());
            Intent fabIntent = new Intent();
            fabIntent.setClassName("com.android.settings", "com.android.settings.Settings$MainSettingsLayoutActivity");
            startActivity(fabIntent);
            return true;
        } /*else if (preference == mDashboardPortraitColumns) {
            int columnsPortrait = (Integer) objValue;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.DASHBOARD_PORTRAIT_COLUMNS, columnsPortrait * 1);
            return true;
        } else if (preference == mDashboardLandscapeColumns) {
            int columnsLandscape = (Integer) objValue;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.DASHBOARD_LANDSCAPE_COLUMNS, columnsLandscape * 1);
            return true;
        }*/
        return false;
    }

     @Override
     public boolean onPreferenceTreeClick(Preference preference) {
        return false;

    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        RRUtils.addSearchIndexProvider(R.xml.rr_dashboard_settings);
}
