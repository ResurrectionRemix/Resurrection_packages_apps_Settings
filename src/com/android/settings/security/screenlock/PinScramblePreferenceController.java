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

package com.android.settings.security.screenlock;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

import lineageos.providers.LineageSettings;

public class PinScramblePreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    static final String KEY_LOCKSCREEN_SCRAMBLE_PIN_LAYOUT = "lockscreen_scramble_pin_layout";

    private final int mUserId;
    private final LockPatternUtils mLockPatternUtils;

    public PinScramblePreferenceController(Context context, int userId,
            LockPatternUtils lockPatternUtils) {
        super(context);
        mUserId = userId;
        mLockPatternUtils = lockPatternUtils;
    }

    @Override
    public boolean isAvailable() {
        return isPinLock();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_LOCKSCREEN_SCRAMBLE_PIN_LAYOUT;
    }

    @Override
    public void updateState(Preference preference) {
        ((TwoStatePreference) preference).setChecked(LineageSettings.System.getInt(
                mContext.getContentResolver(),
                LineageSettings.System.LOCKSCREEN_PIN_SCRAMBLE_LAYOUT,
                0) == 1);
    }

    private boolean isPinLock() {
        int quality = mLockPatternUtils.getKeyguardStoredPasswordQuality(mUserId);
        boolean hasPin = quality == DevicePolicyManager.PASSWORD_QUALITY_NUMERIC ||
                quality == DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX;
        return mLockPatternUtils.isSecure(mUserId) && hasPin;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        LineageSettings.System.putInt(
                mContext.getContentResolver(),
                LineageSettings.System.LOCKSCREEN_PIN_SCRAMBLE_LAYOUT,
                (Boolean) newValue ? 1 : 0);
        return true;
    }
}
