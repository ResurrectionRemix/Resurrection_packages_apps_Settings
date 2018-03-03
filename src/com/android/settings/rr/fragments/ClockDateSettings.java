/*
 * Copyright (C) 2017 RR
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.rr.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v14.preference.SwitchPreference;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

import java.util.Date;

public class ClockDateSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener { 

    private static final String PREF_CAT_DATE =
            "clock_date_cat_date";
    private static final String PREF_CLOCK_DATE_POSITION =
            "clock_date_position";
    private static final String PREF_SHOW_SECONDS =
            "clock_date_show_seconds";
    private static final String PREF_SHOW_DATE =
            "clock_date_show_date";
    private static final String PREF_DATE_FORMAT =
            "clock_date_date_format";
    private static final String PREF_DATE_STYLE =
            "clock_date_date_style";
    private static final String PREF_DATE_SIZE_SMALL =
            "clock_date_date_size_small";

    private static final int DATE_STYLE_LOWERCASE     = 1;
    private static final int DATE_STYLE_UPPERCASE     = 2;
    private static final int CUSTOM_DATE_FORMAT_INDEX = 18;

    private static final int MENU_RESET = Menu.FIRST;
    private static final int DLG_RESET  = 0;

    private ListPreference mClockDatePosition;
    private SwitchPreference mShowSeconds;
    private SwitchPreference mShowDate;
    private ListPreference mDateFormat;
    private ListPreference mDateStyle;
    private SwitchPreference mDateSizeSmall;

    private ContentResolver mResolver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        refreshSettings();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    public void refreshSettings() {
        PreferenceScreen prefs = getPreferenceScreen();
        if (prefs != null) {
            prefs.removeAll();
        }

        addPreferencesFromResource(R.xml.status_bar_clock_date_settings);

        mResolver = getContentResolver();

        mClockDatePosition =
                (ListPreference) findPreference(PREF_CLOCK_DATE_POSITION);
        int clockDatePosition = Settings.System.getInt(mResolver,
                Settings.System.STATUS_BAR_CLOCK_DATE_POSITION, 0);
        mClockDatePosition.setValue(String.valueOf(clockDatePosition));
        mClockDatePosition.setSummary(mClockDatePosition.getEntry());
        mClockDatePosition.setOnPreferenceChangeListener(this);

        boolean isClockEnabled = clockDatePosition != 2;
        boolean isDateEnabled = isClockEnabled && Settings.System.getInt(mResolver,
               Settings.System.STATUS_BAR_CLOCK_SHOW_DATE, 0) == 1;

        PreferenceCategory catDate =
                (PreferenceCategory) findPreference(PREF_CAT_DATE);

        if (isClockEnabled) {
            mShowSeconds = (SwitchPreference) findPreference(PREF_SHOW_SECONDS);
            mShowSeconds.setChecked(Settings.System.getInt(mResolver,
                    Settings.System.STATUS_BAR_CLOCK_SHOW_SECONDS, 0) == 1);
            mShowSeconds.setOnPreferenceChangeListener(this);

            mShowDate = (SwitchPreference) findPreference(PREF_SHOW_DATE);
            mShowDate.setChecked(isDateEnabled);
            mShowDate.setOnPreferenceChangeListener(this);

        } else {
            removePreference(PREF_SHOW_DATE);
            removePreference(PREF_SHOW_SECONDS);
        }

        if (isClockEnabled && isDateEnabled) {
            mDateFormat = (ListPreference) findPreference(PREF_DATE_FORMAT);
            parseDateFormats();
            String dateFormat = Settings.System.getString(mResolver,
                    Settings.System.STATUS_BAR_CLOCK_DATE_FORMAT);
            if (dateFormat == null) {
                dateFormat = "EEE";
            }
            mDateFormat.setValue(dateFormat);
            mDateFormat.setOnPreferenceChangeListener(this);

            mDateStyle = (ListPreference) findPreference(PREF_DATE_STYLE);
            int dateStyle = Settings.System.getInt(mResolver,
                    Settings.System.STATUS_BAR_CLOCK_DATE_STYLE, 0);
            mDateStyle.setValue(String.valueOf(dateStyle));
            mDateStyle.setSummary(mDateStyle.getEntry());
            mDateStyle.setOnPreferenceChangeListener(this);

            mDateSizeSmall = (SwitchPreference) findPreference(PREF_DATE_SIZE_SMALL);
            mDateSizeSmall.setChecked(Settings.System.getInt(mResolver,
                    Settings.System.STATUS_BAR_CLOCK_DATE_SIZE_SMALL, 0) == 1);
            mDateSizeSmall.setOnPreferenceChangeListener(this);

        } else {
            catDate.removePreference(findPreference(PREF_DATE_FORMAT));
            catDate.removePreference(findPreference(PREF_DATE_STYLE));
            catDate.removePreference(findPreference(PREF_DATE_SIZE_SMALL));
            removePreference(PREF_CAT_DATE);
        }

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset)
                .setIcon(R.drawable.ic_action_reset)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                showDialogInner(DLG_RESET);
                return true;
             default:
                return super.onContextItemSelected(item);
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean result = false;
        AlertDialog dialog;

        if (preference == mClockDatePosition) {
            int clockDatePosition = Integer.valueOf((String) newValue);
            int index = mClockDatePosition.findIndexOfValue((String) newValue);
            Settings.System.putInt(mResolver,
                    Settings.System.STATUS_BAR_CLOCK_DATE_POSITION, clockDatePosition);
            preference.setSummary(mClockDatePosition.getEntries()[index]);
            refreshSettings();
            return true;
        } else if (preference == mShowSeconds) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(mResolver,
                    Settings.System.STATUS_BAR_CLOCK_SHOW_SECONDS, value ? 1 : 0);
            return true;
        } else if (preference == mShowDate) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(mResolver,
                    Settings.System.STATUS_BAR_CLOCK_SHOW_DATE, value ? 1 : 0);
            refreshSettings();
            return true;
        }  else if (preference == mDateFormat) {
            int index = mDateFormat.findIndexOfValue((String) newValue);

            if (index == CUSTOM_DATE_FORMAT_INDEX) {
                AlertDialog.Builder alert =
                        new AlertDialog.Builder(getActivity());
                alert.setTitle(
                        R.string.clock_date_date_string_edittext_title);
                alert.setMessage(
                        R.string.clock_date_date_string_edittext_summary);
                final EditText input = new EditText(getActivity());
                String oldText = Settings.System.getString(mResolver,
                        Settings.System.STATUS_BAR_CLOCK_DATE_FORMAT);
                if (oldText != null) {
                    input.setText(oldText);
                }
                alert.setView(input);
                alert.setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface,
                            int whichButton) {
                        String value = input.getText().toString();
                        if (value.equals("")) {
                            return;
                        }
                        Settings.System.putString(mResolver,
                                Settings.System.STATUS_BAR_CLOCK_DATE_FORMAT, value);

                        return;
                    }
                });

                alert.setNegativeButton(R.string.dlg_cancel,
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface,
                            int which) {
                        return;
                    }
                });
                dialog = alert.create();
                dialog.show();
            } else {
                if ((String) newValue != null) {
                    Settings.System.putString(mResolver,
                            Settings.System.STATUS_BAR_CLOCK_DATE_FORMAT, (String) newValue);
                }
            }
            return true;
        } else if (preference == mDateStyle) {
            int dateStyle = Integer.valueOf((String) newValue);
            int index = mDateStyle.findIndexOfValue((String) newValue);
            Settings.System.putInt(mResolver,
                    Settings.System.STATUS_BAR_CLOCK_DATE_STYLE, dateStyle);
            preference.setSummary(mDateStyle.getEntries()[index]);
            return true;
        } else if (preference == mDateSizeSmall) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(mResolver,
                    Settings.System.STATUS_BAR_CLOCK_DATE_SIZE_SMALL, value ? 1 : 0);
            return true;
        }
        return false;
    }

    private void parseDateFormats() {
        // Parse and repopulate mStatusBarDateFormat's entries based on current date.
        String[] dateEntries = getResources().getStringArray(
                R.array.clock_date_date_format_entries);
        CharSequence parsedDateEntries[];
        parsedDateEntries = new String[dateEntries.length];
        Date now = new Date();

        int lastEntry = dateEntries.length - 1;
        int dateStyle = Settings.System.getInt(mResolver,
                Settings.System.STATUS_BAR_CLOCK_DATE_STYLE, 2);
        for (int i = 0; i < dateEntries.length; i++) {
            if (i == lastEntry) {
                parsedDateEntries[i] = dateEntries[i];
            } else {
                String newDate;
                CharSequence dateString = DateFormat.format(dateEntries[i], now);
                if (dateStyle == DATE_STYLE_LOWERCASE) {
                    newDate = dateString.toString().toLowerCase();
                } else if (dateStyle == DATE_STYLE_UPPERCASE) {
                    newDate = dateString.toString().toUpperCase();
                } else {
                    newDate = dateString.toString();
                }

                parsedDateEntries[i] = newDate;
            }
        }
        mDateFormat.setEntries(parsedDateEntries);
    }

    private void showDialogInner(int id) {
        DialogFragment newFragment = MyAlertDialogFragment.newInstance(id);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(int id) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            frag.setArguments(args);
            return frag;
        }

        ClockDateSettings getOwner() {
            return (ClockDateSettings) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_RESET:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.reset)
                    .setMessage(R.string.reset_message)
                    .setNegativeButton(R.string.dlg_cancel, null)
                    .setNeutralButton(R.string.dlg_reset_android,
                            new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.STATUS_BAR_CLOCK_DATE_POSITION, 0);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.STATUS_BAR_CLOCK_SHOW_SECONDS, 0);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.STATUS_BAR_CLOCK_SHOW_DATE, 0);
                            Settings.System.putString(getOwner().mResolver,
                                    Settings.System.STATUS_BAR_CLOCK_DATE_FORMAT, "EEE");
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.STATUS_BAR_CLOCK_DATE_STYLE, 0);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.STATUS_BAR_CLOCK_DATE_SIZE_SMALL, 0);
                            getOwner().refreshSettings();
                        }
                    })
                    .setPositiveButton(R.string.dlg_reset_rr,
                            new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.STATUS_BAR_CLOCK_DATE_POSITION, 1);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.STATUS_BAR_CLOCK_SHOW_SECONDS, 0);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.STATUS_BAR_CLOCK_SHOW_DATE, 1);
                            Settings.System.putString(getOwner().mResolver,
                                    Settings.System.STATUS_BAR_CLOCK_DATE_FORMAT, "EEE");
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.STATUS_BAR_CLOCK_DATE_STYLE, 0);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.STATUS_BAR_CLOCK_DATE_SIZE_SMALL, 0);
                            getOwner().refreshSettings();
                        }
                    })
                    .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }

        @Override
        public void onCancel(DialogInterface dialog) {

        }
    }
}
