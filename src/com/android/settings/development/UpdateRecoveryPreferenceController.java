/*
 * Copyright (C) 2020 The LineageOS Project
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
import android.os.SystemProperties;
import android.os.UserManager;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

import java.io.File;

public class UpdateRecoveryPreferenceController extends DeveloperOptionsPreferenceController
        implements PreferenceControllerMixin {

    private static final String TAG = "UpdateRecoveryPreferenceController";
    private static final String PREF_KEY = "update_recovery";

    private static final String UPDATE_RECOVERY_EXEC = "/system/bin/install-recovery.sh";
    private static final String UPDATE_RECOVERY_PROPERTY = "persist.sys.recovery_update";

    private final DevelopmentSettingsDashboardFragment mFragment;

    public UpdateRecoveryPreferenceController(Context context,
            DevelopmentSettingsDashboardFragment fragment) {
        super(context);

        mFragment = fragment;
    }

    @Override
    public boolean isAvailable() {
        return new File(UPDATE_RECOVERY_EXEC).exists();
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        boolean enabled = SystemProperties.getBoolean(UPDATE_RECOVERY_PROPERTY, false);
        ((SwitchPreference) mPreference).setChecked(enabled);

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
            UpdateRecoveryWarningDialog.show(mFragment);
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

    public void onUpdateRecoveryDialogConfirmed() {
        boolean enabled = SystemProperties.getBoolean(UPDATE_RECOVERY_PROPERTY, false);
        SystemProperties.set(UPDATE_RECOVERY_PROPERTY, String.valueOf(!enabled));
        ((SwitchPreference) mPreference).setChecked(!enabled);
    }

    public void onUpdateRecoveryDialogDismissed() {
        boolean enabled = SystemProperties.getBoolean(UPDATE_RECOVERY_PROPERTY, false);
        ((SwitchPreference) mPreference).setChecked(enabled);
    }

    @VisibleForTesting
    boolean isAdminUser() {
        return ((UserManager) mContext.getSystemService(Context.USER_SERVICE)).isAdminUser();
    }
}
