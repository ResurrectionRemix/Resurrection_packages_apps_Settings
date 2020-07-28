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
import android.content.Context;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceCategory;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import lineageos.preference.LineageSystemSettingListPreference;


import java.util.Date;
import android.content.Context;
import android.provider.SearchIndexableResource;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

import com.android.settings.rr.Preferences.*;
import com.android.settings.rr.utils.RRUtils;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;
import com.android.settings.rr.Preferences.CustomSeekBarPreference;


import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
@SearchIndexable
public class ClockSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {
    private static final String TAG = "ClockSettings";
    private static final String CLOCK_DATE_AUTO_HIDE_HDUR = "status_bar_clock_auto_hide_hduration";
    private static final String CLOCK_DATE_AUTO_HIDE_SDUR = "status_bar_clock_auto_hide_sduration";
    private static final String CLOCK_DATE_FORMAT = "statusbar_clock_date_format";
    private static final String CLOCK_DATE = "statusbar_clock_date_display";
    private static final String CLOCK_DATE_CAT = "date_cat";
    private static final int CLOCK_DATE_STYLE_LOWERCASE = 1;
    private static final int CLOCK_DATE_STYLE_UPPERCASE = 2;
    private static final int CUSTOM_CLOCK_DATE_FORMAT_INDEX = 18;
    private CustomSeekBarPreference mHideDuration, mShowDuration;
    private SecureSettingListPreference mClockDateFormat;
    private SecureSettingListPreference mClockDate;
    private PreferenceCategory mClockDateCat;


    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rr_clock_settings);

        ContentResolver resolver = getActivity().getContentResolver();

        mHideDuration = (CustomSeekBarPreference) findPreference(CLOCK_DATE_AUTO_HIDE_HDUR);
        int hideVal = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_CLOCK_AUTO_HIDE_HDURATION, 60, UserHandle.USER_CURRENT);
        mHideDuration.setValue(hideVal);
        mHideDuration.setOnPreferenceChangeListener(this);
        int showdate = Settings.Secure.getInt(resolver,
                Settings.Secure.STATUSBAR_CLOCK_DATE_DISPLAY, 0);
        mShowDuration = (CustomSeekBarPreference) findPreference(CLOCK_DATE_AUTO_HIDE_SDUR);
        mClockDate = (SecureSettingListPreference) findPreference(CLOCK_DATE);
        mClockDate.setOnPreferenceChangeListener(this);
        mClockDateCat = (PreferenceCategory) findPreference(CLOCK_DATE_CAT);
        int showVal = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_CLOCK_AUTO_HIDE_SDURATION, 5, UserHandle.USER_CURRENT);
        mShowDuration.setValue(showVal);
        mShowDuration.setOnPreferenceChangeListener(this);
        mClockDateFormat = (SecureSettingListPreference) findPreference(CLOCK_DATE_FORMAT);
        mClockDateFormat.setOnPreferenceChangeListener(this);
        if (mClockDateFormat.getValue() == null) {
            mClockDateFormat.setValue("EEE");
        }

        parseClockDateFormats();
        updateDatePrefs(showdate);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        AlertDialog dialog;
        ContentResolver resolver = getActivity().getContentResolver();
       if (preference == mHideDuration) {
            int value = (Integer) newValue;
            Settings.System.putIntForUser(resolver,
                    Settings.System.STATUS_BAR_CLOCK_AUTO_HIDE_HDURATION, value, UserHandle.USER_CURRENT);
            return true;
      } else if (preference == mShowDuration) {
            int value = (Integer) newValue;
            Settings.System.putIntForUser(resolver,
                    Settings.System.STATUS_BAR_CLOCK_AUTO_HIDE_SDURATION, value, UserHandle.USER_CURRENT);
            return true;
      } else if (preference == mClockDate) {
            int value = Integer.parseInt((String) newValue);
            updateDatePrefs(value);
            return true;
      } else if (preference == mClockDateFormat) {
            int index = mClockDateFormat.findIndexOfValue((String) newValue);

            if (index == CUSTOM_CLOCK_DATE_FORMAT_INDEX) {
                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                alert.setTitle(R.string.clock_date_string_edittext_title);
                alert.setMessage(R.string.clock_date_string_edittext_summary);

                final EditText input = new EditText(getActivity());
                String oldText = Settings.Secure.getString(
                    resolver,
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
                        Settings.Secure.putString(resolver,
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
                    Settings.Secure.putString(resolver,
                        Settings.Secure.STATUSBAR_CLOCK_DATE_FORMAT, (String) newValue);
                }
            }
            return true;
        }
        return false;
    }


    private void updateDatePrefs(int mode) {
        if (mode == 0) {
            mClockDateCat.setEnabled(false);
        } else {
            mClockDateCat.setEnabled(true);
        }
    }

    private void parseClockDateFormats() {
        String[] dateEntries = getResources().getStringArray(
                R.array.clock_date_format_entries_values);
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

    /**
     * For Search.
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean enabled) {
                ArrayList<SearchIndexableResource> result =
                    new ArrayList<SearchIndexableResource>();
                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.rr_clock_settings;
                    result.add(sir);
                    return result;
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                List<String> keys = super.getNonIndexableKeys(context);
                return keys;
            }
        };
}
