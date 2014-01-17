/*
 * Copyright (C) 2013 The CyanogenMod Project
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

package com.android.settings.cyanogenmod;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

/**
 * Performance Settings
 */
public class PerformanceSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "PerformanceSettings";

    private static final String PERF_PROFILE_PREF = "pref_perf_profile";
    private static final String USE_16BPP_ALPHA_PREF = "pref_use_16bpp_alpha";

    private static final String USE_16BPP_ALPHA_PROP = "persist.sys.use_16bpp_alpha";

    private ListPreference mPerfProfilePref;
    private CheckBoxPreference mUse16bppAlphaPref;

    private String[] mPerfProfileEntries;
    private String[] mPerfProfileValues;
    private String mPerfProfileDefaultEntry;

    private ContentObserver mPerformanceProfileObserver = null;

    private AlertDialog alertDialog;

    private class PerformanceProfileObserver extends ContentObserver {
        public PerformanceProfileObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            setCurrentValue();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPerfProfileDefaultEntry = getString(
                com.android.internal.R.string.config_perf_profile_default_entry);
        mPerfProfileEntries = getResources().getStringArray(
                com.android.internal.R.array.perf_profile_entries);
        mPerfProfileValues = getResources().getStringArray(
                com.android.internal.R.array.perf_profile_values);

        addPreferencesFromResource(R.xml.performance_settings);

        PreferenceScreen prefSet = getPreferenceScreen();

        mPerfProfilePref = (ListPreference)prefSet.findPreference(PERF_PROFILE_PREF);
        String perfProfileProp = getString(com.android.internal.R.string.config_perf_profile_prop);
        if (mPerfProfilePref != null && TextUtils.isEmpty(perfProfileProp)) {
            prefSet.removePreference(mPerfProfilePref);
            mPerfProfilePref = null;
        } else {
            mPerformanceProfileObserver = new PerformanceProfileObserver(new Handler());

            mPerfProfilePref.setEntries(mPerfProfileEntries);
            mPerfProfilePref.setEntryValues(mPerfProfileValues);
            setCurrentValue();
            mPerfProfilePref.setOnPreferenceChangeListener(this);
        }

        mUse16bppAlphaPref = (CheckBoxPreference) prefSet.findPreference(USE_16BPP_ALPHA_PREF);
        String use16bppAlpha = SystemProperties.get(USE_16BPP_ALPHA_PROP, "0");
        mUse16bppAlphaPref.setChecked("1".equals(use16bppAlpha));

        /* Display the warning dialog */
        alertDialog = new AlertDialog.Builder(getActivity()).create();
        alertDialog.setTitle(R.string.performance_settings_warning_title);
        alertDialog.setMessage(getResources().getString(R.string.performance_settings_warning));
        alertDialog.setCancelable(false);
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE,
                getResources().getString(com.android.internal.R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        return;
                    }
                });
        alertDialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mPerfProfilePref != null) {
            setCurrentValue();
            ContentResolver resolver = getActivity().getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.PERFORMANCE_PROFILE), false, mPerformanceProfileObserver);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mPerfProfilePref != null) {
            ContentResolver resolver = getActivity().getContentResolver();
            resolver.unregisterContentObserver(mPerformanceProfileObserver);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mUse16bppAlphaPref) {
            SystemProperties.set(USE_16BPP_ALPHA_PROP,
                    mUse16bppAlphaPref.isChecked() ? "1" : "0");
        } else {
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (newValue != null) {
            if (preference == mPerfProfilePref) {
                Settings.System.putString(getActivity().getContentResolver(),
                        Settings.System.PERFORMANCE_PROFILE, String.valueOf(newValue));
                setCurrentPerfProfileSummary();
                return true;
            }
        }
        return false;
    }

    private void setCurrentPerfProfileSummary() {
        String value = getCurrentPerformanceProfile();
        String summary = "";
        int count = mPerfProfileValues.length;
        for (int i = 0; i < count; i++) {
            try {
                if (mPerfProfileValues[i].equals(value)) {
                    summary = mPerfProfileEntries[i];
                }
            } catch (IndexOutOfBoundsException ex) {
                // Ignore
            }
        }
        mPerfProfilePref.setSummary(String.format("%s", summary));
    }

    private void setCurrentValue() {
        String value = getCurrentPerformanceProfile();
        mPerfProfilePref.setValue(value);
        setCurrentPerfProfileSummary();
    }

    private String getCurrentPerformanceProfile() {
        String value = Settings.System.getString(getActivity().getContentResolver(),
                Settings.System.PERFORMANCE_PROFILE);
        if (TextUtils.isEmpty(value)) {
            value = mPerfProfileDefaultEntry;
        }
        return value;
    }
}
