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
package com.android.settings.rr;

import android.app.Activity;
import android.content.Context;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.logging.MetricsProto.MetricsEvent;

import com.android.settings.rr.SeekBarPreference;
import com.android.settings.util.CMDProcessor;
import com.android.settings.util.Helpers;
import com.android.settings.Utils;

import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import android.hardware.fingerprint.FingerprintManager;
import com.android.settings.Utils;
import com.android.internal.widget.LockPatternUtils;


public class LockScreenSecurity extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "LockScreenSecurity";


	private static final String LOCKSCREEN_MAX_NOTIF_CONFIG = "lockscreen_max_notif_cofig";
    private static final String FP_UNLOCK_KEYSTORE = "fp_unlock_keystore";
    private static final String PREF_SHOW_EMERGENCY_BUTTON = "show_emergency_button";

    private SwitchPreference mEmergencyButton;

    private static final int MY_USER_ID = UserHandle.myUserId();

	private SeekBarPreference mMaxKeyguardNotifConfig;
    private SwitchPreference mFpKeystore;
    private FingerprintManager mFingerprintManager;


    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
		}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final LockPatternUtils lockPatternUtils = new LockPatternUtils(getActivity());
        final ContentResolver resolver = getActivity().getContentResolver();


        addPreferencesFromResource(R.xml.rr_ls_security);
        mFingerprintManager = (FingerprintManager) getActivity().getSystemService(Context.FINGERPRINT_SERVICE);


        mFpKeystore = (SwitchPreference) findPreference(FP_UNLOCK_KEYSTORE);
        if (mFpKeystore !=null) {
            if (!mFingerprintManager.isHardwareDetected()){
            getPreferenceScreen().removePreference(mFpKeystore);
            } else {
            mFpKeystore.setChecked((Settings.System.getInt(getContentResolver(),
                        Settings.System.FP_UNLOCK_KEYSTORE, 0) == 1));
            mFpKeystore.setOnPreferenceChangeListener(this);
            }
        }


        mEmergencyButton = (SwitchPreference) findPreference(PREF_SHOW_EMERGENCY_BUTTON);
        mEmergencyButton.setChecked((Settings.System.getInt(resolver,
                Settings.System.SHOW_EMERGENCY_BUTTON, 1) == 1));
        mEmergencyButton.setOnPreferenceChangeListener(this);
		
        mMaxKeyguardNotifConfig = (SeekBarPreference) findPreference(LOCKSCREEN_MAX_NOTIF_CONFIG);
        int kgconf = Settings.System.getInt(getContentResolver(),
                Settings.System.LOCKSCREEN_MAX_NOTIF_CONFIG, 5);
        mMaxKeyguardNotifConfig.setValue(kgconf);
        mMaxKeyguardNotifConfig.setOnPreferenceChangeListener(this);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue){
        	ContentResolver resolver = getActivity().getContentResolver();
 			if (preference == mMaxKeyguardNotifConfig) {
            int kgconf = (Integer) objValue;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.LOCKSCREEN_MAX_NOTIF_CONFIG, kgconf);
            return true;
        	} else if (preference == mFpKeystore) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.FP_UNLOCK_KEYSTORE, value ? 1 : 0);
            return true;
            } else if  (preference == mEmergencyButton) {
            boolean checked = ((SwitchPreference)preference).isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SHOW_EMERGENCY_BUTTON, checked ? 1:0);
            Helpers.showSystemUIrestartDialog(getActivity());
            return true;
            }
	return false;
    }
}
