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

import android.os.Bundle;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.provider.Settings;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v14.preference.SwitchPreference;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import lineageos.preference.LineageSystemSettingListPreference;

import com.android.settings.R;
import com.android.settings.rr.utils.RRUtils;
import com.android.settings.search.Indexable.SearchIndexProvider;
import com.android.settings.SettingsPreferenceFragment;

import java.util.Date;

public class ClockSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "ClockSettings";
    private static final String CATEGORY_CLOCK = "status_bar_clock_key";
    private static final String STATUS_BAR_CLOCK_STYLE = "status_bar_clock";
    private static final String PREF_AM_PM_STYLE = "status_bar_am_pm";
    private static final String PREF_CLOCK_DATE_DISPLAY = "clock_date_display";
    private static final String PREF_CLOCK_DATE_STYLE = "clock_date_style";
    private static final String PREF_CLOCK_DATE_FORMAT = "clock_date_format";
    private static final String PREF_STATUS_BAR_CLOCK = "status_bar_show_clock";
    private static final String PREF_CLOCK_DATE_POSITION = "clock_date_position";
    public static final int CLOCK_DATE_STYLE_LOWERCASE = 1;
    public static final int CLOCK_DATE_STYLE_UPPERCASE = 2;
    private static final int CUSTOM_CLOCK_DATE_FORMAT_INDEX = 18;
    private ListPreference mClockAmPmStyle;
    private ListPreference mClockDateDisplay;
    private ListPreference mClockDateStyle;
    private ListPreference mClockDateFormat;
    private LineageSystemSettingListPreference mStatusBarClock;
    private ListPreference mClockDatePosition;


    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.rr_clock_settings);
        mClockAmPmStyle = (ListPreference) findPreference(PREF_AM_PM_STYLE);
        mClockAmPmStyle.setOnPreferenceChangeListener(this);
        mClockAmPmStyle.setValue(Integer.toString(Settings.Secure.getInt(getActivity()
                .getContentResolver(), Settings.Secure.STATUSBAR_CLOCK_AM_PM_STYLE,
                0)));
        boolean is24hour = DateFormat.is24HourFormat(getActivity());
        if (is24hour) {
            mClockAmPmStyle.setSummary(R.string.status_bar_am_pm_info);
        } else {
            mClockAmPmStyle.setSummary(mClockAmPmStyle.getEntry());
        }
        mClockAmPmStyle.setEnabled(!is24hour);
        mClockDateDisplay = (ListPreference) findPreference(PREF_CLOCK_DATE_DISPLAY);
        mClockDateDisplay.setOnPreferenceChangeListener(this);
        mClockDateDisplay.setValue(Integer.toString(Settings.Secure.getInt(getActivity()
                .getContentResolver(), Settings.Secure.STATUSBAR_CLOCK_DATE_DISPLAY,
                0)));
        mClockDateDisplay.setSummary(mClockDateDisplay.getEntry());
        mClockDateStyle = (ListPreference) findPreference(PREF_CLOCK_DATE_STYLE);
        mClockDateStyle.setOnPreferenceChangeListener(this);
        mClockDateStyle.setValue(Integer.toString(Settings.Secure.getInt(getActivity()
                .getContentResolver(), Settings.Secure.STATUSBAR_CLOCK_DATE_STYLE,
                0)));
        mClockDateStyle.setSummary(mClockDateStyle.getEntry());
        mClockDateFormat = (ListPreference) findPreference(PREF_CLOCK_DATE_FORMAT);
        mClockDateFormat.setOnPreferenceChangeListener(this);
        String value = Settings.Secure.getString(getActivity().getContentResolver(),
                Settings.Secure.STATUSBAR_CLOCK_DATE_FORMAT);
        if (value == null || value.isEmpty()) {
            value = "EEE";
        }
        int index = mClockDateFormat.findIndexOfValue((String) value);
        if (index == -1) {
            mClockDateFormat.setValueIndex(CUSTOM_CLOCK_DATE_FORMAT_INDEX);
        } else {
            mClockDateFormat.setValue(value);
        }
        parseClockDateFormats();
        mClockDatePosition = (ListPreference) findPreference(PREF_CLOCK_DATE_POSITION);
        mClockDatePosition.setOnPreferenceChangeListener(this);
        mClockDatePosition.setValue(Integer.toString(Settings.Secure.getInt(getActivity()
                .getContentResolver(), Settings.Secure.STATUSBAR_CLOCK_DATE_POSITION,
                0)));
        mClockDatePosition.setSummary(mClockDatePosition.getEntry());
         boolean mClockDateToggle = Settings.Secure.getInt(getActivity().getContentResolver(),
                    Settings.Secure.STATUSBAR_CLOCK_DATE_DISPLAY, 0) != 0;
        if (!mClockDateToggle) {
            mClockDateStyle.setEnabled(false);
            mClockDateFormat.setEnabled(false);
            mClockDatePosition.setEnabled(false);
        }
        mStatusBarClock =
                (LineageSystemSettingListPreference) findPreference(STATUS_BAR_CLOCK_STYLE);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) 		{
 AlertDialog dialog;
        if (preference == mClockAmPmStyle) {
            int val = Integer.parseInt((String) newValue);
            int index = mClockAmPmStyle.findIndexOfValue((String) newValue);
            Settings.Secure.putInt(getActivity().getContentResolver(),
                    Settings.Secure.STATUSBAR_CLOCK_AM_PM_STYLE, val);
            mClockAmPmStyle.setSummary(mClockAmPmStyle.getEntries()[index]);
            return true;
        } else if (preference == mClockDateDisplay) {
            int val = Integer.parseInt((String) newValue);
            int index = mClockDateDisplay.findIndexOfValue((String) newValue);
            Settings.Secure.putInt(getActivity().getContentResolver(),
                    Settings.Secure.STATUSBAR_CLOCK_DATE_DISPLAY, val);
            mClockDateDisplay.setSummary(mClockDateDisplay.getEntries()[index]);
            if (val == 0) {
                mClockDateStyle.setEnabled(false);
                mClockDateFormat.setEnabled(false);
                mClockDatePosition.setEnabled(false);
            } else {
                mClockDateStyle.setEnabled(true);
                mClockDateFormat.setEnabled(true);
                mClockDatePosition.setEnabled(true);
            }
            return true;
        } else if (preference == mClockDateStyle) {
            int val = Integer.parseInt((String) newValue);
            int index = mClockDateStyle.findIndexOfValue((String) newValue);
            Settings.Secure.putInt(getActivity().getContentResolver(),
                    Settings.Secure.STATUSBAR_CLOCK_DATE_STYLE, val);
            mClockDateStyle.setSummary(mClockDateStyle.getEntries()[index]);
            parseClockDateFormats();
            return true;
        } else if (preference == mClockDateFormat) {
            int index = mClockDateFormat.findIndexOfValue((String) newValue);
             if (index == CUSTOM_CLOCK_DATE_FORMAT_INDEX) {
                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                alert.setTitle(R.string.clock_date_string_edittext_title);
                alert.setMessage(R.string.clock_date_string_edittext_summary);
                final EditText input = new EditText(getActivity());
                String oldText = Settings.Secure.getString(
                    getActivity().getContentResolver(),
                    Settings.Secure.STATUSBAR_CLOCK_DATE_FORMAT);
                if (oldText != null) {
                    input.setText(oldText);
                }
                alert.setView(input);
                alert.setPositiveButton(R.string.menu_save, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int whichButton) {
                        String value = input.getText().toString();
                        if (value.equals("")) {
                            return;
                        }
                        Settings.Secure.putString(getActivity().getContentResolver(),
                            Settings.Secure.STATUSBAR_CLOCK_DATE_FORMAT, value);
                         return;
                    }
                });
                alert.setNegativeButton(R.string.menu_cancel,
                    new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int which) {
                        return;
                    }
                });
                dialog = alert.create();
                dialog.show();
            } else {
                if ((String) newValue != null) {
                    Settings.Secure.putString(getActivity().getContentResolver(),
                        Settings.Secure.STATUSBAR_CLOCK_DATE_FORMAT, (String) newValue);
                }
            }
            return true;
        } else if (preference == mClockDatePosition) {
            int val = Integer.parseInt((String) newValue);
            int index = mClockDatePosition.findIndexOfValue((String) newValue);
            Settings.Secure.putInt(getActivity().getContentResolver(),
                    Settings.Secure.STATUSBAR_CLOCK_DATE_POSITION, val);
            mClockDatePosition.setSummary(mClockDatePosition.getEntries()[index]);
            parseClockDateFormats();
            return true;
        }
        return false;
    }

    private void parseClockDateFormats() {
        String[] dateEntries = getResources().getStringArray(R.array.clock_date_format_entries_values);
        CharSequence parsedDateEntries[];
        parsedDateEntries = new String[dateEntries.length];
        Date now = new Date();
         int lastEntry = dateEntries.length - 1;
        int dateFormat = Settings.Secure.getInt(getActivity()
                .getContentResolver(), Settings.Secure.STATUSBAR_CLOCK_DATE_STYLE, 0);
        for (int i = 0; i < dateEntries.length; i++) {
            if (i == lastEntry) {
                parsedDateEntries[i] = dateEntries[i];
            } else {
                String newDate;
                CharSequence dateString = DateFormat.format(dateEntries[i], now);
                if (dateFormat == CLOCK_DATE_STYLE_LOWERCASE) {
                    newDate = dateString.toString().toLowerCase();
                } else if (dateFormat == CLOCK_DATE_STYLE_UPPERCASE) {
                    newDate = dateString.toString().toUpperCase();
                } else {
                    newDate = dateString.toString();
                }
                 parsedDateEntries[i] = newDate;
            }
        }
        mClockDateFormat.setEntries(parsedDateEntries);
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        RRUtils.addSearchIndexProvider(R.xml.rr_clock_settings);
}
