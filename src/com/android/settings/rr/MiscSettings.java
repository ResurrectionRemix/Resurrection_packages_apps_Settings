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
import com.android.settings.util.AbstractAsyncSuCMDProcessor;
import com.android.settings.util.CMDProcessor;
import com.android.settings.util.Helpers;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;
import android.provider.Settings;
import com.android.settings.util.Helpers;
import dalvik.system.VMRuntime;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.List;
import com.android.settings.Utils;

import java.io.File;
import java.io.IOException;
import java.io.DataOutputStream;

import com.android.internal.logging.MetricsProto.MetricsEvent;

public class MiscSettings extends SettingsPreferenceFragment  implements OnPreferenceChangeListener{

    private static final String SELINUX = "selinux";
    private static final String SELINX_PREF ="selinux_switch";
    private static final String APP_REMOVER = "system_app_remover";
    private SwitchPreference mSelinux;
    private Preference mSelinuxPref;
    private PreferenceScreen mAppRemover;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.rr_misc);
  	    final ContentResolver resolver = getActivity().getContentResolver();

	    //SELinux
        mSelinux = (SwitchPreference) findPreference(SELINUX);
        mAppRemover = (PreferenceScreen) findPreference(APP_REMOVER);
        mSelinuxPref = (Preference) findPreference(SELINX_PREF);

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
        if (magiskSupported || suSupported) {
            mSelinux.setOnPreferenceChangeListener(this);
            if (CMDProcessor.runShellCommand("getenforce").getStdout().contains("Enforcing")) {
                mSelinux.setChecked(true);
                mSelinux.setSummary(R.string.selinux_enforcing_title);
            } else {
                mSelinux.setChecked(false);
                mSelinux.setSummary(R.string.selinux_permissive_title);
            }
        } else {
            if (mSelinux != null) 
                getPreferenceScreen().removePreference(mSelinux);
            if (mAppRemover != null)
                getPreferenceScreen().removePreference(mAppRemover);
            if (mSelinuxPref != null) 
                getPreferenceScreen().removePreference(mSelinuxPref);
        }

    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void setSelinuxEnabled(String status) {
        SharedPreferences.Editor editor = getContext().getSharedPreferences("selinux_pref", Context.MODE_PRIVATE).edit();
        editor.putString("selinux", status);
        editor.apply();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mSelinux) {
            if (value.toString().equals("true")) {
                CMDProcessor.runSuCommand("setenforce 1");
                setSelinuxEnabled("true");
                mSelinux.setSummary(R.string.selinux_enforcing_title);
            } else if (value.toString().equals("false")) {
                CMDProcessor.runSuCommand("setenforce 0");
                setSelinuxEnabled("false");
                mSelinux.setSummary(R.string.selinux_permissive_title);
            }
            return true;
        }
        return false;
    }
}

