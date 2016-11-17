/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.security.screenlock;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.TwoStatePreference;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

import lineageos.providers.LineageSettings;

import org.lineageos.internal.util.LineageLockPatternUtils;

public class DirectlyShowLockPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String KEY_DIRECTLY_SHOW_LOCK = "directly_show_lock";

    private final int mUserId;
    private final LockPatternUtils mLockPatternUtils;
    private final LineageLockPatternUtils mLineageLockPatternUtils;

    public DirectlyShowLockPreferenceController(Context context, int userId,
            LockPatternUtils lockPatternUtils) {
        super(context);
        mUserId = userId;
        mLockPatternUtils = lockPatternUtils;
        mLineageLockPatternUtils = new LineageLockPatternUtils(context);
    }

    @Override
    public boolean isAvailable() {
        if (!mLockPatternUtils.isSecure(mUserId)) {
            return false;
        }
        switch (mLockPatternUtils.getKeyguardStoredPasswordQuality(mUserId)) {
            case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
            case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
            case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
            case DevicePolicyManager.PASSWORD_QUALITY_MANAGED:
                return true;
            default:
                return false;
        }
    }

    @Override
    public void updateState(Preference preference) {
        ((TwoStatePreference) preference).setChecked(
                mLineageLockPatternUtils.shouldPassToSecurityView(mUserId));
    }

    @Override
    public String getPreferenceKey() {
        return KEY_DIRECTLY_SHOW_LOCK;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        mLineageLockPatternUtils.setPassToSecurityView((Boolean) newValue, mUserId);
        return true;
    }
}
