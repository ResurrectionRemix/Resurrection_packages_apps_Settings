/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.settings.fuelgauge;

import android.os.IDeviceIdleController;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.ArraySet;
import android.util.Log;


/**
 * Handles getting/changing the whitelist for the exceptions to battery saving features.
 */
public class PowerWhitelistBackend {

    private static final String TAG = "PowerWhitelistBackend";

    private static final String DEVICE_IDLE_SERVICE = "deviceidle";

    private static final PowerWhitelistBackend INSTANCE = new PowerWhitelistBackend();

    private final IDeviceIdleController mDeviceIdleService;
    private final ArraySet<String> mWhitelistedApps = new ArraySet<>();
    private final ArraySet<String> mSysWhitelistedApps = new ArraySet<>();

    public PowerWhitelistBackend() {
        mDeviceIdleService = IDeviceIdleController.Stub.asInterface(
                ServiceManager.getService(DEVICE_IDLE_SERVICE));
        refreshList();
    }

    public int getWhitelistSize() {
        return mWhitelistedApps.size();
    }

    public boolean isSysWhitelisted(String pkg) {
        return mSysWhitelistedApps.contains(pkg);
    }

    public boolean isWhitelisted(String pkg) {
        return mWhitelistedApps.contains(pkg);
    }

    public void addApp(String pkg) {
        try {
            if (isSysWhitelisted(pkg)) {
                mDeviceIdleService.addSystemPowerSaveWhitelistApp(pkg);
            } else {
                mDeviceIdleService.addPowerSaveWhitelistApp(pkg);
            }
            mWhitelistedApps.add(pkg);
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to reach IDeviceIdleController", e);
        }
    }

    public void removeApp(String pkg) {
        try {
            if (isSysWhitelisted(pkg)) {
                mDeviceIdleService.removeSystemPowerSaveWhitelistApp(pkg);
            } else {
                mDeviceIdleService.removePowerSaveWhitelistApp(pkg);
            }
            mWhitelistedApps.remove(pkg);
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to reach IDeviceIdleController", e);
        }
    }

    private void refreshList() {
        mSysWhitelistedApps.clear();
        mWhitelistedApps.clear();
        try {
            String[] whitelistedApps = mDeviceIdleService.getFullPowerWhitelist();
            for (String app : whitelistedApps) {
                mWhitelistedApps.add(app);
            }
            String[] sysWhitelistedApps = mDeviceIdleService.getSystemPowerWhitelist();
            for (String app : sysWhitelistedApps) {
                mSysWhitelistedApps.add(app);
            }
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to reach IDeviceIdleController", e);
        }
    }

    public static PowerWhitelistBackend getInstance() {
        return INSTANCE;
    }

}
