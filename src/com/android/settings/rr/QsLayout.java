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
import android.os.UserHandle;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.logging.MetricsProto.MetricsEvent;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class QsLayout extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String PREF_ROWS_PORTRAIT = "qs_rows_portrait";
    private static final String PREF_COLUMNS_PORTRAIT = "qs_columns_portrait";
    private static final String PREF_ROWS_LANDSCAPE = "qs_rows_landscape";
    private static final String PREF_COLUMNS_LANDSCAPE = "qs_columns_landscape";
	private static final String PREF_CAT_LAYOUT = "qs_layout";
    private static final String KEY_SYSUI_QQS_COUNT = "sysui_qqs_count_key";

    private ListPreference mRowsPortrait;
    private ListPreference mColumnsPortrait;
    private ListPreference mRowsLandscape;
    private ListPreference mColumnsLandscape;
    private ListPreference mSysuiQqsCount;
	protected Context mContext;
	protected ContentResolver mContentRes;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
          addPreferencesFromResource(R.xml.rr_qs_layout);

		  mContext = getActivity().getApplicationContext();

          mContentRes = getActivity().getContentResolver();
		  final Resources res = getResources();
		  int defaultValue;

		  PreferenceScreen prefs = getPreferenceScreen();
		  ContentResolver resolver = getActivity().getContentResolver();
		  PreferenceCategory catLayout = (PreferenceCategory) findPreference(PREF_CAT_LAYOUT);

          mRowsPortrait =
                  (ListPreference) findPreference(PREF_ROWS_PORTRAIT);
          int rowsPortrait = Settings.System.getInt(resolver,
                  Settings.System.QS_ROWS_PORTRAIT, 3);
          mRowsPortrait.setValue(String.valueOf(rowsPortrait));
          mRowsPortrait.setSummary(mRowsPortrait.getEntry());
          mRowsPortrait.setOnPreferenceChangeListener(this);
  
          mColumnsPortrait =
                  (ListPreference) findPreference(PREF_COLUMNS_PORTRAIT);
          int columnsPortrait = Settings.System.getInt(resolver,
                  Settings.System.QS_COLUMNS_PORTRAIT, 3);
          mColumnsPortrait.setValue(String.valueOf(columnsPortrait));
          mColumnsPortrait.setSummary(mColumnsPortrait.getEntry());
          mColumnsPortrait.setOnPreferenceChangeListener(this);

        mSysuiQqsCount = (ListPreference) findPreference(KEY_SYSUI_QQS_COUNT);
        int SysuiQqsCount = Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.QQS_COUNT, 5);
        mSysuiQqsCount.setValue(Integer.toString(SysuiQqsCount));
        mSysuiQqsCount.setSummary(mSysuiQqsCount.getEntry());
        mSysuiQqsCount.setOnPreferenceChangeListener(this);
  
          defaultValue = res.getInteger(R.integer.config_qs_num_rows_landscape_default);
          if (defaultValue != 1) {
              mRowsLandscape =
                      (ListPreference) findPreference(PREF_ROWS_LANDSCAPE);
              int rowsLandscape = Settings.System.getInt(resolver,
                      Settings.System.QS_ROWS_LANDSCAPE, defaultValue);
              mRowsLandscape.setValue(String.valueOf(rowsLandscape));
              mRowsLandscape.setSummary(mRowsLandscape.getEntry());
              mRowsLandscape.setOnPreferenceChangeListener(this);
          } else {
              catLayout.removePreference(findPreference(PREF_ROWS_LANDSCAPE));
          }
  
          mColumnsLandscape =
                  (ListPreference) findPreference(PREF_COLUMNS_LANDSCAPE);
          defaultValue = res.getInteger(R.integer.config_qs_num_columns_landscape_default);
          int columnsLandscape = Settings.System.getInt(resolver,
                  Settings.System.QS_COLUMNS_LANDSCAPE, defaultValue);
          mColumnsLandscape.setValue(String.valueOf(columnsLandscape));
          mColumnsLandscape.setSummary(mColumnsLandscape.getEntry());
          mColumnsLandscape.setOnPreferenceChangeListener(this);

    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
		int intValue;
        int index;
		ContentResolver resolver = getActivity().getContentResolver();
		final Resources res = getResources();
		 if (preference == mRowsPortrait) {
              intValue = Integer.valueOf((String) newValue);
              index = mRowsPortrait.findIndexOfValue((String) newValue);
              Settings.System.putInt(resolver,
                      Settings.System.QS_ROWS_PORTRAIT, intValue);
              preference.setSummary(mRowsPortrait.getEntries()[index]);
              return true;
          } else if (preference == mColumnsPortrait) {
              intValue = Integer.valueOf((String) newValue);
              index = mColumnsPortrait.findIndexOfValue((String) newValue);
              Settings.System.putInt(resolver,
                      Settings.System.QS_COLUMNS_PORTRAIT, intValue);
              preference.setSummary(mColumnsPortrait.getEntries()[index]);
              return true;
          } else if (preference == mRowsLandscape) {
              intValue = Integer.valueOf((String) newValue);
              index = mRowsLandscape.findIndexOfValue((String) newValue);
              Settings.System.putInt(resolver,
                      Settings.System.QS_ROWS_LANDSCAPE, intValue);
              preference.setSummary(mRowsLandscape.getEntries()[index]);
              return true;
          } else if (preference == mColumnsLandscape) {
              intValue = Integer.valueOf((String) newValue);
              index = mColumnsLandscape.findIndexOfValue((String) newValue);
              Settings.System.putInt(resolver,
                      Settings.System.QS_COLUMNS_LANDSCAPE, intValue);
              preference.setSummary(mColumnsLandscape.getEntries()[index]);
              return true;
          } else if (preference == mSysuiQqsCount) {
              String SysuiQqsCount = (String) newValue;
              int SysuiQqsCountValue = Integer.parseInt(SysuiQqsCount);
              Settings.Secure.putInt(getContentResolver(), Settings.Secure.QQS_COUNT, SysuiQqsCountValue);
              int SysuiQqsCountIndex = mSysuiQqsCount.findIndexOfValue(SysuiQqsCount);
              mSysuiQqsCount.setSummary(mSysuiQqsCount.getEntries()[SysuiQqsCountIndex]);
              return true;
          }
        return false;
    }
}
