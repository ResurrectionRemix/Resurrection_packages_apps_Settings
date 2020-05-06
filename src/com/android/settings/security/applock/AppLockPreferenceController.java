/*
 * Copyright (C) 2019 Paranoid Android
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

package com.android.settings.security.applock;

import android.app.AppLockManager;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import com.android.settings.biometrics.BiometricStatusPreferenceController;

import com.android.settings.R;

public class AppLockPreferenceController extends BiometricStatusPreferenceController {

    private static final String TAG = "AppLockPrefController";
    private static final String KEY_APP_LOCK = "app_lock_settings";

    private AppLockManager mAppLockManager;
    private Resources mResources;

    public AppLockPreferenceController(Context context) {
        super(context, KEY_APP_LOCK);
        mAppLockManager = (AppLockManager) context.getSystemService(Context.APPLOCK_SERVICE);
        mResources = (Resources) context.getResources();
    }

    @Override
    protected boolean isDeviceSupported() {
        return mAppLockManager != null;
    }

    @Override
    protected boolean hasEnrolledBiometrics() {
        final int N = mAppLockManager.getLockedAppsCount();
        return (N > 0);
    }

    @Override
    protected String getSummaryTextEnrolled() {
        final int N = mAppLockManager.getLockedAppsCount();
        return mResources.getQuantityString(R.plurals.applock_summary, N, N);
    }

    @Override
    protected String getSummaryTextNoneEnrolled() {
        return mResources.getString(R.string.applock_setup);
    }

    @Override
    protected String getSettingsClassName() {
        return AppLockSettings.class.getName();
    }

    @Override
    protected String getEnrollClassName() {
        return AppLockSettings.class.getName();
    }
}
