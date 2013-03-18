/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.cyanogenmod;

import static com.android.internal.util.cm.QSConstants.TILE_BLUETOOTH;
import static com.android.internal.util.cm.QSConstants.TILE_MOBILEDATA;
import static com.android.internal.util.cm.QSConstants.TILE_NETWORKMODE;
import static com.android.internal.util.cm.QSConstants.TILE_NFC;
import static com.android.internal.util.cm.QSConstants.TILE_WIFIAP;
import static com.android.internal.util.cm.QSConstants.TILE_LTE;
import static com.android.internal.util.cm.QSUtils.deviceSupportsBluetooth;
import static com.android.internal.util.cm.QSUtils.deviceSupportsNfc;
import static com.android.internal.util.cm.QSUtils.deviceSupportsUsbTether;
import static com.android.internal.util.cm.QSUtils.deviceSupportsWifiDisplay;

import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class QuickSettings extends SettingsPreferenceFragment implements OnPreferenceChangeListener {
    private static final String TAG = "QuickSettings";

    private static final String SEPARATOR = "OV=I=XseparatorX=I=VO";
    private static final String EXP_RING_MODE = "pref_ring_mode";
    private static final String EXP_NETWORK_MODE = "pref_network_mode";
    private static final String EXP_SCREENTIMEOUT_MODE = "pref_screentimeout_mode";
    private static final String DYNAMIC_ALARM = "dynamic_alarm";
    private static final String DYNAMIC_BUGREPORT = "dynamic_bugreport";
    private static final String DYNAMIC_IME = "dynamic_ime";
    private static final String DYNAMIC_USBTETHER = "dynamic_usbtether";
    private static final String DYNAMIC_WIFI = "dynamic_wifi";
    private static final String COLLAPSE_PANEL = "collapse_panel";
    private static final String GENERAL_SETTINGS = "pref_general_settings";
    private static final String STATIC_TILES = "static_tiles";
    private static final String DYNAMIC_TILES = "pref_dynamic_tiles";

    MultiSelectListPreference mRingMode;
    ListPreference mNetworkMode;
    ListPreference mScreenTimeoutMode;
    CheckBoxPreference mDynamicAlarm;
    CheckBoxPreference mDynamicBugReport;
    CheckBoxPreference mDynamicWifi;
    CheckBoxPreference mDynamicIme;
    CheckBoxPreference mDynamicUsbTether;
    CheckBoxPreference mCollapsePanel;
    PreferenceCategory mGeneralSettings;
    PreferenceCategory mStaticTiles;
    PreferenceCategory mDynamicTiles;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.quick_settings_panel_settings);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        PreferenceScreen prefSet = getPreferenceScreen();
        PackageManager pm = getPackageManager();
        ContentResolver resolver = getActivity().getContentResolver();
        mGeneralSettings = (PreferenceCategory) prefSet.findPreference(GENERAL_SETTINGS);
        mStaticTiles = (PreferenceCategory) prefSet.findPreference(STATIC_TILES);
        mDynamicTiles = (PreferenceCategory) prefSet.findPreference(DYNAMIC_TILES);

        mCollapsePanel = (CheckBoxPreference) prefSet.findPreference(COLLAPSE_PANEL);
        mCollapsePanel.setChecked(Settings.System.getInt(resolver, Settings.System.QS_COLLAPSE_PANEL, 0) == 1);

        // Add the sound mode
        mRingMode = (MultiSelectListPreference) prefSet.findPreference(EXP_RING_MODE);
        String storedRingMode = Settings.System.getString(resolver,
                Settings.System.EXPANDED_RING_MODE);
        if (storedRingMode != null) {
            String[] ringModeArray = TextUtils.split(storedRingMode, SEPARATOR);
            mRingMode.setValues(new HashSet<String>(Arrays.asList(ringModeArray)));
            updateSummary(storedRingMode, mRingMode, R.string.pref_ring_mode_summary);
        }
        mRingMode.setOnPreferenceChangeListener(this);

        // Screen timeout mode
        mScreenTimeoutMode = (ListPreference) prefSet.findPreference(EXP_SCREENTIMEOUT_MODE);
        mScreenTimeoutMode.setSummary(mScreenTimeoutMode.getEntry());
        mScreenTimeoutMode.setOnPreferenceChangeListener(this);

        // Add the dynamic tiles checkboxes
        mDynamicAlarm = (CheckBoxPreference) prefSet.findPreference(DYNAMIC_ALARM);
        mDynamicAlarm.setChecked(Settings.System.getInt(resolver, Settings.System.QS_DYNAMIC_ALARM, 1) == 1);
        mDynamicBugReport = (CheckBoxPreference) prefSet.findPreference(DYNAMIC_BUGREPORT);
        mDynamicBugReport.setChecked(Settings.System.getInt(resolver, Settings.System.QS_DYNAMIC_BUGREPORT, 1) == 1);
        mDynamicIme = (CheckBoxPreference) prefSet.findPreference(DYNAMIC_IME);
        if (mDynamicIme != null) {
            mDynamicIme.setChecked(Settings.System.getInt(resolver, Settings.System.QS_DYNAMIC_IME, 1) == 1);
        }
        mDynamicUsbTether = (CheckBoxPreference) prefSet.findPreference(DYNAMIC_USBTETHER);
        if (mDynamicUsbTether != null) {
            if (deviceSupportsUsbTether(getActivity())) {
                mDynamicUsbTether.setChecked(Settings.System.getInt(resolver, Settings.System.QS_DYNAMIC_USBTETHER, 1) == 1);
            } else {
                mDynamicTiles.removePreference(mDynamicUsbTether);
                mDynamicUsbTether = null;
            }
        }
        mDynamicWifi = (CheckBoxPreference) prefSet.findPreference(DYNAMIC_WIFI);
        if (mDynamicWifi != null) {
            if (deviceSupportsWifiDisplay(getActivity())) {
                mDynamicWifi.setChecked(Settings.System.getInt(resolver, Settings.System.QS_DYNAMIC_WIFI, 1) == 1);
            } else {
                mDynamicTiles.removePreference(mDynamicWifi);
                mDynamicWifi = null;
            }
        }

        // Don't show mobile data options if not supported
        boolean isMobileData = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
        if (!isMobileData) {
            QuickSettingsUtil.TILES.remove(TILE_MOBILEDATA);
            QuickSettingsUtil.TILES.remove(TILE_WIFIAP);
        } else {
            // We have telephony support however, some phones run on networks not supported
            // by the networkmode tile so remove both it and the associated options list
            int network_state = -99;
            try {
                network_state = Settings.Global.getInt(resolver,
                        Settings.Global.PREFERRED_NETWORK_MODE);
            } catch (Settings.SettingNotFoundException e) {
                Log.e(TAG, "Unable to retrieve PREFERRED_NETWORK_MODE", e);
            }

            switch (network_state) {
                // list of supported network modes
                case Phone.NT_MODE_WCDMA_PREF:
                case Phone.NT_MODE_WCDMA_ONLY:
                case Phone.NT_MODE_GSM_UMTS:
                case Phone.NT_MODE_GSM_ONLY:
                    break;
            }
        }

        // Don't show the bluetooth options if not supported
        if (!deviceSupportsBluetooth()) {
            QuickSettingsUtil.TILES.remove(TILE_BLUETOOTH);
        }

        // Dont show the NFC tile if not supported
        if (!deviceSupportsNfc(getActivity())) {
            QuickSettingsUtil.TILES.remove(TILE_NFC);
        }
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mDynamicAlarm) {
            Settings.System.putInt(resolver, Settings.System.QS_DYNAMIC_ALARM,
                    mDynamicAlarm.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mDynamicBugReport) {
            Settings.System.putInt(resolver, Settings.System.QS_DYNAMIC_BUGREPORT,
                    mDynamicBugReport.isChecked() ? 1 : 0);
            return true;
        } else if (mDynamicIme != null && preference == mDynamicIme) {
            Settings.System.putInt(resolver, Settings.System.QS_DYNAMIC_IME,
                    mDynamicIme.isChecked() ? 1 : 0);
            return true;
        } else if (mDynamicUsbTether != null && preference == mDynamicUsbTether) {
            Settings.System.putInt(resolver, Settings.System.QS_DYNAMIC_USBTETHER,
                    mDynamicUsbTether.isChecked() ? 1 : 0);
            return true;
        } else if (mDynamicWifi != null && preference == mDynamicWifi) {
            Settings.System.putInt(resolver, Settings.System.QS_DYNAMIC_WIFI,
                    mDynamicWifi.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mCollapsePanel) {
            Settings.System.putInt(resolver, Settings.System.QS_COLLAPSE_PANEL,
                    mCollapsePanel.isChecked() ? 1 : 0);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private class MultiSelectListPreferenceComparator implements Comparator<String> {
        private MultiSelectListPreference pref;

        MultiSelectListPreferenceComparator(MultiSelectListPreference p) {
            pref = p;
        }

        @Override
        public int compare(String lhs, String rhs) {
            return Integer.compare(pref.findIndexOfValue(lhs),
                    pref.findIndexOfValue(rhs));
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mRingMode) {
            ArrayList<String> arrValue = new ArrayList<String>((Set<String>) newValue);
            Collections.sort(arrValue, new MultiSelectListPreferenceComparator(mRingMode));
            Settings.System.putString(resolver, Settings.System.EXPANDED_RING_MODE,
                    TextUtils.join(SEPARATOR, arrValue));
            updateSummary(TextUtils.join(SEPARATOR, arrValue), mRingMode, R.string.pref_ring_mode_summary);
            return true;
        } else if (preference == mScreenTimeoutMode) {
            int value = Integer.valueOf((String) newValue);
            int index = mScreenTimeoutMode.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.EXPANDED_SCREENTIMEOUT_MODE, value);
            mScreenTimeoutMode.setSummary(mScreenTimeoutMode.getEntries()[index]);
            return true;
        }
        return false;
    }

    private void updateSummary(String val, MultiSelectListPreference pref, int defSummary) {
        // Update summary message with current values
        final String[] values = parseStoredValue(val);
        if (values != null) {
            final int length = values.length;
            final CharSequence[] entries = pref.getEntries();
            StringBuilder summary = new StringBuilder();
            for (int i = 0; i < (length); i++) {
                CharSequence entry = entries[Integer.parseInt(values[i])];
                if ((length - i) > 1) {
                    summary.append(entry).append(" | ");
                } else {
                    summary.append(entry);
                }
            }
            pref.setSummary(summary);
        } else {
            pref.setSummary(defSummary);
        }
    }

    public static String[] parseStoredValue(CharSequence val) {
        if (TextUtils.isEmpty(val)) {
            return null;
        } else {
            return val.toString().split(SEPARATOR);
        }
    }
}
