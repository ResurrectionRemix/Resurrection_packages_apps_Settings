/*
 * Copyright (C) 2017 The Android Open Source Project
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
import android.provider.Settings;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;
import androidx.preference.TwoStatePreference;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.security.trustagent.TrustAgentManager;
import com.android.settingslib.core.AbstractPreferenceController;

public class QuickUnlockPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String KEY_LOCKSCREEN_QUICK_UNLOCK_CONTROL = "lockscreen_quick_unlock_control";

    private final int mUserId;
    private final LockPatternUtils mLockPatternUtils;
    private final TrustAgentManager mTrustAgentManager;

    public QuickUnlockPreferenceController(Context context, int userId,
            LockPatternUtils lockPatternUtils) {
        super(context);
        mUserId = userId;
        mLockPatternUtils = lockPatternUtils;
        mTrustAgentManager = FeatureFactory.getFactory(context)
                .getSecurityFeatureProvider().getTrustAgentManager();
    }

    @Override
    public boolean isAvailable() {
        if (!mLockPatternUtils.isSecure(mUserId)) {
            return false;
        }
        switch (mLockPatternUtils.getKeyguardStoredPasswordQuality(mUserId)) {
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
            case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
            case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
                return true;
            default:
                return false;
        }
    }

    @Override
    public void updateState(Preference preference) {
        int value = Settings.System.getInt(
                mContext.getContentResolver(), Settings.System.LOCKSCREEN_QUICK_UNLOCK_CONTROL, 0);
        ((SwitchPreference) preference).setChecked(value != 0);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean value = (Boolean) newValue;
        Settings.System.putInt(
                mContext.getContentResolver(), Settings.System.LOCKSCREEN_QUICK_UNLOCK_CONTROL, value ? 1 : 0);
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_LOCKSCREEN_QUICK_UNLOCK_CONTROL;
    }
}
