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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.hardware.fingerprint.FingerprintManager;
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
import android.telephony.TelephonyManager;
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
    private static final String RR_OTA = "rr_ota_fab";
    private static final String RR_INCALL = "rr_incall";

    private SwitchPreference mConfig;
    private SwitchPreference mSelinux;
    private FingerprintManager mFingerprintManager;
    private SwitchPreference mFingerprintVib;
    private PreferenceScreen mIncall;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.rr_misc);
  	    final ContentResolver resolver = getActivity().getContentResolver();

	    //SELinux
        mSelinux = (SwitchPreference) findPreference(SELINUX);
        mSelinux.setOnPreferenceChangeListener(this);

        if (CMDProcessor.runShellCommand("getenforce").getStdout().contains("Enforcing")) {
            mSelinux.setChecked(true);
            mSelinux.setSummary(R.string.selinux_enforcing_title);
        } else {
            mSelinux.setChecked(false);
            mSelinux.setSummary(R.string.selinux_permissive_title);
        }

        mConfig = (SwitchPreference) findPreference(RR_OTA);
        mConfig.setChecked((Settings.System.getInt(getContentResolver(),
                            Settings.System.RR_OTA_FAB, 0) == 1));
        mConfig.setOnPreferenceChangeListener(this);

        mFingerprintManager = (FingerprintManager) getActivity().getSystemService(Context.FINGERPRINT_SERVICE);        
        mFingerprintVib = (SwitchPreference) findPreference("fingerprint_success_vib");
        if (!mFingerprintManager.isHardwareDetected()){
            getPreferenceScreen().removePreference(mFingerprintVib);
        }

        PreferenceScreen mIncall = (PreferenceScreen) findPreference(RR_INCALL);
        if (!isVoiceCapable(getActivity())) {
            getPreferenceScreen().removePreference(mIncall);
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

    /**
     * Returns whether the device is voice-capable (meaning, it is also a phone).
     */
    public static boolean isVoiceCapable(Context context) {
        TelephonyManager telephony =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return telephony != null && telephony.isVoiceCapable();
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
        } else if (preference == mConfig) {
            boolean newvalue = (Boolean) value;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.RR_OTA_FAB, newvalue ? 1 : 0);
            finish();
            Intent fabIntent = new Intent();
            fabIntent.setClassName("com.android.settings", "com.android.settings.Settings$MainSettingsLayoutActivity");
            startActivity(fabIntent);
            return true;
        }
        return false;
    }
}

