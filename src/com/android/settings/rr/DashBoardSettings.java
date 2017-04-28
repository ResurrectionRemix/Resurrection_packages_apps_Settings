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
import android.content.Context;
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

import com.android.internal.logging.MetricsProto.MetricsEvent;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.dashboard.DashboardSummary;
import com.android.settings.Utils;

public class DashBoardSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "UI";
    private static final String RR_CONFIG = "rr_config_style";
    private static final String PREF_RR_SETTINGS_SUMMARY = "rr_settings_summary";
    private static final String KEY_DASHBOARD_PORTRAIT_COLUMNS = "dashboard_portrait_columns";
    private static final String KEY_DASHBOARD_LANDSCAPE_COLUMNS = "dashboard_landscape_columns";

    private ListPreference mConfig;
    private Preference mCustomSummary;
    private String mCustomSummaryText;
    private SeekBarPreference mDashboardPortraitColumns;
    private SeekBarPreference mDashboardLandscapeColumns;

    @Override
    protected int getMetricsCategory() {
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

        mDashboardPortraitColumns = (SeekBarPreference) findPreference(KEY_DASHBOARD_PORTRAIT_COLUMNS);
        int columnsPortrait = Settings.System.getInt(resolver,
                Settings.System.DASHBOARD_PORTRAIT_COLUMNS, DashboardSummary.mNumColumns);
        mDashboardPortraitColumns.setValue(columnsPortrait / 1);
        mDashboardPortraitColumns.setOnPreferenceChangeListener(this);

        mDashboardLandscapeColumns = (SeekBarPreference) findPreference(KEY_DASHBOARD_LANDSCAPE_COLUMNS);
        int columnsLandscape = Settings.System.getInt(resolver,
                Settings.System.DASHBOARD_LANDSCAPE_COLUMNS, 2);
        mDashboardLandscapeColumns.setValue(columnsLandscape / 1);
        mDashboardLandscapeColumns.setOnPreferenceChangeListener(this);

        mCustomSummary = (Preference) findPreference(PREF_RR_SETTINGS_SUMMARY);
        updateCustomSummaryTextString();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final ContentResolver resolver = getActivity().getContentResolver();
           if (preference == mConfig) {
            Settings.System.putInt(getContentResolver(), Settings.System.RR_CONFIG_STYLE,
            Integer.valueOf((String) objValue));
            mConfig.setValue(String.valueOf(objValue));
            mConfig.setSummary(mConfig.getEntry());
            finish();
            Intent fabIntent = new Intent();
            fabIntent.setClassName("com.android.settings", "com.android.settings.Settings$MainSettingsLayoutActivity");
            startActivity(fabIntent);
            return true;
        } else if (preference == mDashboardPortraitColumns) {
            int columnsPortrait = (Integer) objValue;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.DASHBOARD_PORTRAIT_COLUMNS, columnsPortrait * 1);
            return true;
        } else if (preference == mDashboardLandscapeColumns) {
            int columnsLandscape = (Integer) objValue;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.DASHBOARD_LANDSCAPE_COLUMNS, columnsLandscape * 1);
            return true;
        }
        return false;
    }

     @Override
     public boolean onPreferenceTreeClick(Preference preference) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mCustomSummary) {
            AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
            alert.setTitle(R.string.custom_summary_title);
            alert.setMessage(R.string.custom_summary_explain);

            // Set an EditText view to get user input
            final EditText input = new EditText(getActivity());
            input.setText(TextUtils.isEmpty(mCustomSummaryText) ? "" : mCustomSummaryText);
            input.setSelection(input.getText().length());
            alert.setView(input);
            alert.setPositiveButton(getString(android.R.string.ok),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String value = ((Spannable) input.getText()).toString().trim();
                            Settings.System.putString(resolver, Settings.System.RR_SETTINGS_SUMMARY, value);
                            updateCustomSummaryTextString();
                        }
                    });
            alert.setNegativeButton(getString(android.R.string.cancel), null);
            alert.show();
        } else {
            return super.onPreferenceTreeClick(preference);
        }
        return false;
    }

    private void updateCustomSummaryTextString() {
        mCustomSummaryText = Settings.System.getString(
                getActivity().getContentResolver(), Settings.System.RR_SETTINGS_SUMMARY);
 
        if (TextUtils.isEmpty(mCustomSummaryText)) {
            mCustomSummary.setSummary(R.string.rr_title_summary);
        } else {
            mCustomSummary.setSummary(mCustomSummaryText);
        }
    }
}
