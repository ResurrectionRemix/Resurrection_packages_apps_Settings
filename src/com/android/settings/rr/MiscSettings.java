/*
 * Copyright (C) 2016 RR
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

package com.android.settings.rr;

import android.content.Context;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Build;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.provider.Settings;
import dalvik.system.VMRuntime;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.List;

import java.io.File;
import java.io.IOException;
import java.io.DataOutputStream;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

public class MiscSettings extends SettingsPreferenceFragment {

    private static final String APP_REMOVER = "system_app_remover";
    private static final String ROOT_ACCESS_PROPERTY = "persist.sys.root_access";



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rr_misc);
  	    final ContentResolver resolver = getActivity().getContentResolver();
        mAppRemover = (PreferenceScreen) findPreference(APP_REMOVER);

        // Magisk Manager
        boolean magiskSupported = false;
        // SuperSU
        boolean suSupported = false;
        try {
            magiskSupported = (getPackageManager().getPackageInfo("com.topjohnwu.magisk", 0).versionCode > 0);
        } catch (PackageManager.NameNotFoundException e) {
        }
        try {
            suSupported = (getPackageManager().getPackageInfo("eu.chainfire.supersu", 0).versionCode >= 185);
        } catch (PackageManager.NameNotFoundException e) {
        }
        if (magiskSupported || suSupported || isRootForAppsEnabled()) {
        } else {
            if (mAppRemover != null)
                getPreferenceScreen().removePreference(mAppRemover);
        }

    }


    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}

