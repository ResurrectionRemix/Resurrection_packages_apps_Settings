/*
 * Copyright (C) 2018 The LineageOS Project
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

package com.android.settings.development;

import android.content.Context;
import android.net.NetworkUtils;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserManager;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v14.preference.SwitchPreference;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

import lineageos.providers.LineageSettings;

public class WirelessAdbPreferenceController extends DeveloperOptionsPreferenceController
        implements PreferenceControllerMixin {

    private static final String TAG = "WirelessAdbPreferenceController";
    private static final String PREF_KEY = "adb_over_network";

    private final DevelopmentSettingsDashboardFragment mFragment;
    private final WifiManager mWifiManager;

    public WirelessAdbPreferenceController(Context context,
            DevelopmentSettingsDashboardFragment fragment) {
        super(context);

        mFragment = fragment;
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        updatePreference();

        if (!isAdminUser()) {
            mPreference.setEnabled(false);
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (Utils.isMonkeyRunning()) {
            return false;
        }

        if (TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            if (((SwitchPreference) mPreference).isChecked()) {
                WirelessAdbWarningDialog.show(mFragment);
            } else {
                LineageSettings.Secure.putInt(mContext.getContentResolver(),
                       LineageSettings.Secure.ADB_PORT, -1);
                updatePreference();
            }
            return true;
        }
        return false;
    }

    @Override
    protected void onDeveloperOptionsSwitchEnabled() {
        if (isAdminUser()) {
            mPreference.setEnabled(true);
        }
    }

    public void onWirelessAdbDialogConfirmed() {
        LineageSettings.Secure.putInt(mContext.getContentResolver(),
                LineageSettings.Secure.ADB_PORT, 5555);
        updatePreference();
    }

    public void onWirelessAdbDialogDismissed() {
        updatePreference();
    }

    private void updatePreference() {
        int port = LineageSettings.Secure.getInt(mContext.getContentResolver(),
                LineageSettings.Secure.ADB_PORT, -1);
        boolean enabled = port > 0;
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();

        if (enabled && wifiInfo != null) {
            String hostAddress = NetworkUtils.intToInetAddress(
                    wifiInfo.getIpAddress()).getHostAddress();
            mPreference.setSummary(hostAddress + ":" + String.valueOf(port));
        } else {
            mPreference.setSummary(R.string.adb_over_network_summary);
        }

        ((SwitchPreference) mPreference).setChecked(enabled);
    }

    @VisibleForTesting
    boolean isAdminUser() {
        return ((UserManager) mContext.getSystemService(Context.USER_SERVICE)).isAdminUser();
    }
}
