/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings.wifi;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WpsInfo;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;
import android.security.Credentials;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;

import android.util.Log;
import android.widget.Toast;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.RestrictedSettingsFragment;
import com.android.settingslib.RestrictedLockUtils;

public class AdvancedWifiSettings extends RestrictedSettingsFragment
        implements OnPreferenceChangeListener {
    private static final String TAG = "AdvancedWifiSettings";

    private static final String KEY_INSTALL_CREDENTIALS = "install_credentials";
    private static final String KEY_WIFI_DIRECT = "wifi_direct";
    private static final String KEY_WPS_PUSH = "wps_push_button";
    private static final String KEY_WPS_PIN = "wps_pin_entry";

    // Wifi extension requirement
    private static final String KEY_CURRENT_GATEWAY = "current_gateway";
    private static final String KEY_CURRENT_NETMASK = "current_netmask";
    private static final int WIFI_HS2_ENABLED = 1;
    private static final int WIFI_HS2_DISABLED = 0;

    private static final String KEY_PRIORITY_SETTINGS = "wifi_priority_settings";

    private static final String KEY_AUTO_CONNECT_ENABLE = "auto_connect_type";
    private static final String WIFI_AUTO_CONNECT_TYPE = "wifi_auto_connect_type";
    private static final int AUTO_CONNECT_ENABLED = 0;
    private static final int AUTO_CONNECT_DISABLE = 1;
    private static final int AUTO_CONNECT_DEFAULT_VALUE = AUTO_CONNECT_ENABLED;

    private static final String KEY_CELLULAR_TO_WLAN = "cellular_to_wlan";
    private static final String CELLULAR_TO_WLAN_CONNECT_TYPE = "cellular_to_wlan_type";
    private static final int CELLULAR_TO_WLAN_CONNECT_TYPE_AUTO = 0;
    private static final int CELLULAR_TO_WLAN_CONNECT_TYPE_MANUAL = 1;
    private static final int CELLULAR_TO_WLAN_CONNECT_TYPE_ASK = 2;
    private static final int CELLULAR_WLAN_DEFAULT_VALUE = CELLULAR_TO_WLAN_CONNECT_TYPE_AUTO;

    private static final String KEY_CELLULAR_TO_WLAN_HINT = "cellular_to_wlan_hint";
    private static final String CELLULAR_TO_WLAN_HINT = "cellular_to_wlan_hint";

    private static final String KEY_WLAN_TO_CELLULAR_HINT = "wlan_to_cellular_hint";
    private static final String WLAN_TO_CELLULAR_HINT = "wlan_to_cellular_hint";

    private static final String KEY_CONNECT_NOTIFY = "notify_ap_connected";
    private static final String NOTIFY_USER_CONNECT = "notify_user_when_connect_cmcc";
    private static final String KEY_ENABLE_HS2 = "enable_hs2";
    private static final int NOTIFY_USER = 0;
    private static final int DO_NOT_NOTIFY_USER = -1;

    private CheckBoxPreference mAutoConnectEnablePref;
    private CheckBoxPreference mCellularToWlanHintPref;
    private ListPreference mCellularToWlanPref;

    private boolean mUnavailable;

    public AdvancedWifiSettings() {
        super(UserManager.DISALLOW_CONFIG_WIFI);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.WIFI_ADVANCED;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isUiRestricted()) {
            mUnavailable = true;
            setPreferenceScreen(new PreferenceScreen(getPrefContext(), null));
        } else {
            addPreferencesFromResource(R.xml.wifi_advanced_settings);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getEmptyTextView().setText(R.string.wifi_advanced_not_available);
        if (mUnavailable) {
            getPreferenceScreen().removeAll();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mUnavailable) {
            initPreferences();
        }
    }

    private void initPreferences() {
        final Context context = getActivity();
        Intent intent = new Intent(Credentials.INSTALL_AS_USER_ACTION);
        intent.setClassName("com.android.certinstaller",
                "com.android.certinstaller.CertInstallerMain");
        intent.putExtra(Credentials.EXTRA_INSTALL_AS_UID, android.os.Process.WIFI_UID);
        Preference pref = findPreference(KEY_INSTALL_CREDENTIALS);
        pref.setIntent(intent);
        SwitchPreference enableHs2 =
                (SwitchPreference) findPreference(KEY_ENABLE_HS2);
        if (enableHs2 != null && getResources().getBoolean(
                com.android.internal.R.bool.config_passpoint_setting_on)) {
            enableHs2.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    String key = preference.getKey();
                    if (KEY_ENABLE_HS2.equals(key)) {
                        Settings.Global.putInt(getContentResolver(),
                                Settings.Global.WIFI_HOTSPOT2_ENABLED,
                                ((SwitchPreference) preference).isChecked() ? 1 : 0);
                    }
                return true;
                }
            });

            enableHs2.setChecked(Settings.Global.getInt(getContentResolver(),
                    Settings.Global.WIFI_HOTSPOT2_ENABLED, WIFI_HS2_DISABLED) == WIFI_HS2_ENABLED);
        } else {
            getPreferenceScreen().removePreference(enableHs2);
        }

        Intent wifiDirectIntent = new Intent(context,
                com.android.settings.Settings.WifiP2pSettingsActivity.class);
        Preference wifiDirectPref = findPreference(KEY_WIFI_DIRECT);
        wifiDirectPref.setIntent(wifiDirectIntent);

        // WpsDialog: Create the dialog like WifiSettings does.
        Preference wpsPushPref = findPreference(KEY_WPS_PUSH);
        wpsPushPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference arg0) {
                    WpsFragment wpsFragment = new WpsFragment(WpsInfo.PBC);
                    wpsFragment.show(getFragmentManager(), KEY_WPS_PUSH);
                    return true;
                }
        });

        // WpsDialog: Create the dialog like WifiSettings does.
        Preference wpsPinPref = findPreference(KEY_WPS_PIN);
        wpsPinPref.setOnPreferenceClickListener(new OnPreferenceClickListener(){
                public boolean onPreferenceClick(Preference arg0) {
                    WpsFragment wpsFragment = new WpsFragment(WpsInfo.DISPLAY);
                    wpsFragment.show(getFragmentManager(), KEY_WPS_PIN);
                    return true;
                }
        });

        // Wifi extension requirement
        Preference prioritySettingPref = findPreference(KEY_PRIORITY_SETTINGS);
        if (prioritySettingPref != null) {
            if (!getResources().getBoolean(R.bool.set_wifi_priority)) {
                getPreferenceScreen().removePreference(prioritySettingPref);
            }
        } else {
            Log.d(TAG, "Fail to get priority pref...");
        }

        mAutoConnectEnablePref =
                (CheckBoxPreference) findPreference(KEY_AUTO_CONNECT_ENABLE);
        if (mAutoConnectEnablePref != null) {
            if (getResources().getBoolean(R.bool.config_auto_connect_wifi_enabled)) {
                mAutoConnectEnablePref.setChecked(isAutoConnectEnabled());
                mAutoConnectEnablePref.setOnPreferenceChangeListener(this);
            } else {
                getPreferenceScreen().removePreference(mAutoConnectEnablePref);
            }
        }

        mCellularToWlanPref =
                (ListPreference) findPreference(KEY_CELLULAR_TO_WLAN);
        if (mCellularToWlanPref != null) {
            if (getResources().getBoolean(R.bool.cell_to_wifi)) {
                int value = getCellularToWlanValue();
                mCellularToWlanPref.setValue(String.valueOf(value));
                updateCellToWlanSummary(mCellularToWlanPref, value);
                mCellularToWlanPref.setOnPreferenceChangeListener(this);
            } else {
                getPreferenceScreen().removePreference(mCellularToWlanPref);
            }
        }

        CheckBoxPreference wlanToCellularHintPref =
                (CheckBoxPreference) findPreference(KEY_WLAN_TO_CELLULAR_HINT);
        if (wlanToCellularHintPref != null) {
            if (getResources().getBoolean(R.bool.wifi_to_cell)) {
                wlanToCellularHintPref.setChecked(isWlanToCellHintEnable());
                wlanToCellularHintPref.setOnPreferenceChangeListener(this);
            } else {
                getPreferenceScreen().removePreference(wlanToCellularHintPref);
            }
        }

        CheckBoxPreference notifyConnectedApPref =
                (CheckBoxPreference) findPreference(KEY_CONNECT_NOTIFY);
        if (notifyConnectedApPref != null) {
            if (getResources().getBoolean(R.bool.connect_to_cmcc_notify)) {
                notifyConnectedApPref.setChecked(ifNotifyConnect());
                notifyConnectedApPref.setOnPreferenceChangeListener(this);
            } else {
                getPreferenceScreen().removePreference(notifyConnectedApPref);
            }
        }

        mCellularToWlanHintPref = (CheckBoxPreference) findPreference(KEY_CELLULAR_TO_WLAN_HINT);
        if (mCellularToWlanHintPref != null) {
            if (getResources().getBoolean(R.bool.cellular_to_wlan_hint)) {
                mCellularToWlanHintPref.setChecked(isCellularToWlanHintEnable());
                mCellularToWlanHintPref.setOnPreferenceChangeListener(this);
            } else {
                getPreferenceScreen().removePreference(mCellularToWlanHintPref);
            }
        }
    }

    /* Wrapper class for the WPS dialog to properly handle life cycle events like rotation. */
    public static class WpsFragment extends DialogFragment {
        private static int mWpsSetup;

        // Public default constructor is required for rotation.
        public WpsFragment() {
            super();
        }

        public WpsFragment(int wpsSetup) {
            super();
            mWpsSetup = wpsSetup;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new WpsDialog(getActivity(), mWpsSetup);
        }
    }

    // Wifi extension requirement
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final Context context = getActivity();
        String key = preference.getKey();

        if (KEY_WLAN_TO_CELLULAR_HINT.equals(key)) {
            boolean checked = ((Boolean) newValue).booleanValue();
            setWlanToCellularHintEnable(checked);
        }

        if (KEY_AUTO_CONNECT_ENABLE.equals(key)) {
            boolean checked = ((Boolean) newValue).booleanValue();
            setAutoConnectTypeEnabled(checked);
            updateCellularToWifiPrefs(checked);
            if (!checked) {
                updateCellularToWlanHintPref(true);
            }
        }

        if (KEY_CELLULAR_TO_WLAN.equals(key)) {
            int value = Integer.parseInt(((String) newValue));
            setCellToWlanType(value);
            mCellularToWlanPref.setValue(String.valueOf(value));
            updateCellToWlanSummary(mCellularToWlanPref, value);
            updateAutoConnectPref(value == CELLULAR_TO_WLAN_CONNECT_TYPE_AUTO);
            if (CELLULAR_TO_WLAN_CONNECT_TYPE_AUTO != value) {
                updateCellularToWlanHintPref(true);
            }
        }

        if (KEY_CONNECT_NOTIFY.equals(key)) {
            boolean checked = ((Boolean) newValue).booleanValue();
            setApConnectedNotify(checked);
        }

        if (KEY_CELLULAR_TO_WLAN_HINT.equals(key)) {
            boolean checked = ((Boolean) newValue).booleanValue();
            setCellularToWlanHintEnable(checked);
            if(!checked) {
                Toast.makeText(getActivity(),
                        getResources().getString(R.string.cellular_to_wlan_hint_toast),
                        Toast.LENGTH_LONG).show();
            }
        }

        return true;
    }


    private boolean isCellularToWlanHintEnable() {
        return Settings.System.getInt(getActivity().getContentResolver(),
                CELLULAR_TO_WLAN_HINT, NOTIFY_USER) == NOTIFY_USER;
    }

    private boolean isWlanToCellHintEnable() {
        return Settings.System.getInt(getActivity().getContentResolver(),
                WLAN_TO_CELLULAR_HINT, NOTIFY_USER) == NOTIFY_USER;
    }

    private void setWlanToCellularHintEnable(boolean enable) {
        final int defaultValue = enable ? NOTIFY_USER : DO_NOT_NOTIFY_USER;
        Settings.System.putInt(getActivity().getContentResolver(),
                WLAN_TO_CELLULAR_HINT, defaultValue);
    }

    private boolean ifNotifyConnect() {
        return Settings.System.getInt(getActivity().getContentResolver(),
                NOTIFY_USER_CONNECT, NOTIFY_USER) == NOTIFY_USER;
    }

    private boolean isAutoConnectEnabled() {
        return Settings.System.getInt(getActivity().getContentResolver(),
                WIFI_AUTO_CONNECT_TYPE, AUTO_CONNECT_ENABLED) == AUTO_CONNECT_ENABLED;
    }

    private void setAutoConnectTypeEnabled(boolean enable) {
        final int defaultValue = enable ? AUTO_CONNECT_ENABLED : AUTO_CONNECT_DISABLE;
        Settings.System.putInt(getActivity().getContentResolver(),
                WIFI_AUTO_CONNECT_TYPE, defaultValue);
    }

    private int getCellularToWlanValue() {
        if (isAutoConnectEnabled()) {
            return CELLULAR_TO_WLAN_CONNECT_TYPE_AUTO;
        } else {
            return Settings.System.getInt(getContentResolver(), CELLULAR_TO_WLAN_CONNECT_TYPE,
                    CELLULAR_TO_WLAN_CONNECT_TYPE_AUTO);
        }
    }

    private void updateCellToWlanSummary(Preference preference, int index) {
        String[] summaries = getResources().getStringArray(R.array.cellcular2wifi_entries);
        preference.setSummary(summaries[index]);
    }

    private void updateCellularToWlanHintPref(boolean enable) {
        mCellularToWlanHintPref.setChecked(enable);
        setCellularToWlanHintEnable(enable);
    }

    private void setCellularToWlanHintEnable(boolean needNotify) {
        final int defaultValue = needNotify ? NOTIFY_USER : DO_NOT_NOTIFY_USER;
        Settings.System.putInt(getActivity().getContentResolver(),
                CELLULAR_TO_WLAN_HINT, defaultValue);
    }

    private void setApConnectedNotify(boolean needNotify) {
        final int defaultValue = needNotify ? NOTIFY_USER : DO_NOT_NOTIFY_USER;
        Settings.System.putInt(getActivity().getContentResolver(),
                NOTIFY_USER_CONNECT, defaultValue);
    }

    private void setCellToWlanType(int value) {
        try {
            Settings.System.putInt(getContentResolver(), CELLULAR_TO_WLAN_CONNECT_TYPE,
                    value);
        } catch (NumberFormatException e) {
            Toast.makeText(getActivity(), R.string.wifi_setting_connect_type_error,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void updateCellularToWifiPrefs(boolean isAutoEnabled) {
        if (!isAutoEnabled) {
            updateCellularToWlanHintPref(true);
        }
        int defaultValue = isAutoEnabled ? CELLULAR_TO_WLAN_CONNECT_TYPE_AUTO
                : CELLULAR_TO_WLAN_CONNECT_TYPE_MANUAL;
        Settings.System.putInt(getContentResolver(), CELLULAR_TO_WLAN_CONNECT_TYPE, defaultValue);
        mCellularToWlanPref.setValue(String.valueOf(defaultValue));
        updateCellToWlanSummary(mCellularToWlanPref, defaultValue);
    }

    private void updateAutoConnectPref(boolean isAutoMode) {
        setAutoConnectTypeEnabled(isAutoMode);
        mAutoConnectEnablePref.setChecked(isAutoMode);
    }
}
