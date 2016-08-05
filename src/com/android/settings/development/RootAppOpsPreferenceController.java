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
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.ServiceManager;
import android.os.UserManager;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.Settings.AppOpsSummaryActivity;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

import static com.android.settings.SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS;

public class RootAppOpsPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceClickListener, PreferenceControllerMixin {

    private static final String TAG = "RootAppOpsPreferenceController";
    private static final String PREF_KEY = "root_appops";

    public RootAppOpsPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        // User builds don't get root, and eng always gets root
        return Build.IS_DEBUGGABLE || "eng".equals(Build.TYPE);
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreference.setOnPreferenceClickListener(this);

        if (!RootAccessPreferenceController.isRootForAppsEnabled() || !isAdminUser()) {
            mPreference.setEnabled(false);
        }
    }

    @Override
    protected void onDeveloperOptionsSwitchEnabled() {
        if (isAdminUser()) {
            mPreference.setEnabled(true);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        Bundle args = new Bundle();
        args.putString("appops_tab",
                mContext.getResources().getString(R.string.app_ops_categories_su));
        intent.putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, args);
        intent.setClass(mContext, AppOpsSummaryActivity.class);
        mContext.startActivity(intent);
        return true;
    }

    @VisibleForTesting
    boolean isAdminUser() {
        return ((UserManager) mContext.getSystemService(Context.USER_SERVICE)).isAdminUser();
    }
}
