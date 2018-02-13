/*Copyright (C) 2015 The ResurrectionRemix Project
     Licensed under the Apache License, Version 2.0 (the "License");
     you may not use this file except in compliance with the License.
     You may obtain a copy of the License at

          http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.
*/
package com.android.settings.rr.navigation.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.accessibility.ToggleFontSizePreferenceFragment;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.android.settings.applications.LayoutPreference;


import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.rr.utils.TelephonyUtils;

public class UISettingsNav extends SettingsPreferenceFragment implements
    Preference.OnPreferenceChangeListener {
    private static final String TAG = "UI";
    private static final String RR_FP = "rr_fp";
    private static final String INCALL_VIB_OPTIONS = "rr_incall";

    private LayoutPreference mFpFragment;
    private FingerprintManager mFingerprintManager;
    private LayoutPreference mInCallFragment;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Activity activity = getActivity(); 
		ContentResolver resolver = getActivity().getContentResolver();
        mFingerprintManager = (FingerprintManager) getActivity().getSystemService(Context.FINGERPRINT_SERVICE);

        addPreferencesFromResource(R.xml.rr_ui_settings_navigation);

        mFpFragment = (LayoutPreference) findPreference(RR_FP);
        if (mFingerprintManager == null) {
            getPreferenceScreen().removePreference(mFpFragment);
        }


        mInCallFragment = (LayoutPreference) findPreference(INCALL_VIB_OPTIONS);
        if (!TelephonyUtils.isVoiceCapable(getActivity())) {
            getPreferenceScreen().removePreference(mInCallFragment);
        }

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }
}
