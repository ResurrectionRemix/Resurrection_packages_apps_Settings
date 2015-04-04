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
import android.support.v7.preference.Preference;
import android.support.v7.preference.TwoStatePreference;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public abstract class AbstractPatternSwitchPreferenceController
        extends AbstractPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private final String mKey;
    private final int mUserId;
    private final LockPatternUtils mLockPatternUtils;

    public AbstractPatternSwitchPreferenceController(Context context, String key,
            int userId, LockPatternUtils lockPatternUtils) {
        super(context);
        mKey = key;
        mUserId = userId;
        mLockPatternUtils = lockPatternUtils;
    }

    @Override
    public boolean isAvailable() {
        return isPatternLock();
    }

    @Override
    public String getPreferenceKey() {
        return mKey;
    }

    @Override
    public void updateState(Preference preference) {
        ((TwoStatePreference) preference).setChecked(isEnabled(mLockPatternUtils, mUserId));
    }

    private boolean isPatternLock() {
        return mLockPatternUtils.isSecure(mUserId)
                && mLockPatternUtils.getKeyguardStoredPasswordQuality(mUserId)
                == DevicePolicyManager.PASSWORD_QUALITY_SOMETHING;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        setEnabled(mLockPatternUtils, mUserId, (Boolean) newValue);
        return true;
    }

    protected abstract boolean isEnabled(LockPatternUtils utils, int userId);
    protected abstract void setEnabled(LockPatternUtils utils, int userId, boolean enabled);
}
