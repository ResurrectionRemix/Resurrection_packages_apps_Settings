/*
 * Copyright (C) 2014 The CyanogenMod Project
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

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;

import com.android.settings.DevelopmentSettings;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

/**
 * Performance Settings
 */
public class PerformanceSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "PerformanceSettings";

    private static final String CATEGORY_PROFILES = "perf_profile_prefs";
    private static final String CATEGORY_SYSTEM = "perf_system_prefs";
    private static final String CATEGORY_GRAPHICS = "perf_graphics_prefs";

    private static final String CATEGORY_PROCESSOR = "processor";

    private static final String PERF_PROFILE_PREF = "pref_perf_profile";

    private static final String FORCE_HIGHEND_GFX_PREF = "pref_force_highend_gfx";
    private static final String FORCE_HIGHEND_GFX_PERSIST_PROP = "persist.sys.force_highendgfx";

    // Don't nag the user with the dragons ahead warning every time
    private static final String KEY_DRAGONS_ARE_AWESOME = "pref_dragons_awesome";

    private ListPreference mPerfProfilePref;
    private SwitchPreference mForceHighEndGfx;

    private String[] mPerfProfileEntries;
    private String[] mPerfProfileValues;
    private String mPerfProfileDefaultEntry;

    private AlertDialog mAlertDialog;

    private PowerManager mPowerManager;

    private ContentObserver mPerformanceProfileObserver = null;

    private class PerformanceProfileObserver extends ContentObserver {
        public PerformanceProfileObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            setCurrentValue();
        }
    }

    private SharedPreferences mDevelopmentPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

        mDevelopmentPreferences = getActivity().getSharedPreferences(
                DevelopmentSettings.PREF_FILE, Context.MODE_PRIVATE);

        mPerfProfileDefaultEntry = getString(
                com.android.internal.R.string.config_perf_profile_default_entry);
        mPerfProfileEntries = getResources().getStringArray(
                com.android.internal.R.array.perf_profile_entries);
        mPerfProfileValues = getResources().getStringArray(
                com.android.internal.R.array.perf_profile_values);

        addPreferencesFromResource(R.xml.performance_settings);

        PreferenceScreen prefSet = getPreferenceScreen();

        // 1. always show performance profiles, if available on this device
        // 2. only show system / graphics options if dev mode enabled
        // 3. never show individual processor control if profiles enabled

        PreferenceCategory category = (PreferenceCategory) prefSet.findPreference(CATEGORY_PROFILES);

        mPerfProfilePref = (ListPreference)prefSet.findPreference(PERF_PROFILE_PREF);
        if (mPerfProfilePref != null && !mPowerManager.hasPowerProfiles()) {
            prefSet.removePreference(category);
            mPerfProfilePref = null;
        } else {
            mPerfProfilePref.setEntries(mPerfProfileEntries);
            mPerfProfilePref.setEntryValues(mPerfProfileValues);
            setCurrentValue();
            mPerfProfilePref.setOnPreferenceChangeListener(this);
            ((PreferenceCategory)prefSet.findPreference(CATEGORY_SYSTEM)).removePreference(
                    prefSet.findPreference(CATEGORY_PROCESSOR));
        }

        category = (PreferenceCategory) prefSet.findPreference(CATEGORY_SYSTEM);
        if (!showAdvancedPerfSettings()) {
            prefSet.removePreference(category);
        }

        category = (PreferenceCategory) prefSet.findPreference(CATEGORY_GRAPHICS);
        if (ActivityManager.isLowRamDeviceStatic() && showAdvancedPerfSettings()) {
            mForceHighEndGfx = (SwitchPreference) prefSet.findPreference(FORCE_HIGHEND_GFX_PREF);
            String forceHighendGfx = SystemProperties.get(FORCE_HIGHEND_GFX_PERSIST_PROP, "false");
            mForceHighEndGfx.setChecked("true".equals(forceHighendGfx));
        } else {
            prefSet.removePreference(category);
        }

        mPerformanceProfileObserver = new PerformanceProfileObserver(new Handler());
    }

    private boolean showAdvancedPerfSettings() {
        return !android.os.Build.TYPE.equals("user") &&
                mDevelopmentPreferences.getBoolean(
                DevelopmentSettings.PREF_SHOW,
                android.os.Build.TYPE.equals("eng"));
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mPerfProfilePref != null) {
            setCurrentValue();
            ContentResolver resolver = getActivity().getContentResolver();
            resolver.registerContentObserver(Settings.Secure.getUriFor(
                    Settings.Secure.PERFORMANCE_PROFILE), false, mPerformanceProfileObserver);
        }

        boolean warned = mDevelopmentPreferences.getBoolean(KEY_DRAGONS_ARE_AWESOME, false);

        if (showAdvancedPerfSettings() && !warned) {
            /* Display the warning dialog */
            mAlertDialog = new AlertDialog.Builder(getActivity()).create();
            mAlertDialog.setTitle(R.string.performance_settings_warning_title);
            mAlertDialog.setMessage(getResources().getString(R.string.performance_settings_warning));
            mAlertDialog.setCancelable(false);
            mAlertDialog.setButton(DialogInterface.BUTTON_POSITIVE,
                    getResources().getString(com.android.internal.R.string.ok),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // only warn about the dragons once
                            mDevelopmentPreferences.edit().putBoolean(
                                    KEY_DRAGONS_ARE_AWESOME, true).commit();
                        }
                    });
            mAlertDialog.show();
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
        if (preference == mForceHighEndGfx) {
            SystemProperties.set(FORCE_HIGHEND_GFX_PERSIST_PROP,
                    mForceHighEndGfx.isChecked() ? "true" : "false");
        } else {
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (newValue != null) {
            if (preference == mPerfProfilePref) {
                mPowerManager.setPowerProfile(String.valueOf(newValue));
                setCurrentPerfProfileSummary();
                return true;
            }
        }
        return false;
    }

    private void setCurrentPerfProfileSummary() {
        String value = mPowerManager.getPowerProfile();
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
        if (mPerfProfilePref == null) {
            return;
        }
        mPerfProfilePref.setValue(mPowerManager.getPowerProfile());
        setCurrentPerfProfileSummary();
    }
}
