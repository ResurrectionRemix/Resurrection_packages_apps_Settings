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
import android.provider.Settings;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.logging.MetricsProto.MetricsEvent;

import com.android.settings.rr.Preferences.SystemSettingSwitchPreference;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;


import cyanogenmod.providers.CMSettings;

public class FingerPrintSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "FingerPrintSettings";
    private static final String PREF_QUICK_PULLDOWN_FP = "quick_pulldown_fp";
    private static final String FP_SWIPE_CALL_ACTIONS = "fp_swipe_call_actions";
    private static final String FP_UNLOCK_KEYSTORE = "fp_unlock_keystore";

    private SystemSettingSwitchPreference mQuickPulldownFp;
    private ListPreference mFpSwipeCallActions;
    private int mFpSwipeCallActionsValue;
    private SwitchPreference mFpKeystore;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.rr_fp_settings);

        mQuickPulldownFp = (SystemSettingSwitchPreference) findPreference(PREF_QUICK_PULLDOWN_FP);
        mFpSwipeCallActions = (ListPreference) findPreference(FP_SWIPE_CALL_ACTIONS);
        if (!getResources().getBoolean(com.android.internal.R.bool.config_supportSystemNavigationKeys)) {
             getPreferenceScreen().removePreference(mQuickPulldownFp);
             getPreferenceScreen().removePreference(mFpSwipeCallActions);
        } else {
             mQuickPulldownFp.setChecked((Settings.System.getInt(getContentResolver(),
                   Settings.System.STATUS_BAR_QUICK_QS_PULLDOWN_FP, 0) == 1));
             mQuickPulldownFp.setOnPreferenceChangeListener(this);

             mFpSwipeCallActionsValue = Settings.System.getIntForUser(getContentResolver(),
                    Settings.System.FP_SWIPE_CALL_ACTIONS, 0, UserHandle.USER_CURRENT);
             mFpSwipeCallActions.setValue(Integer.toString(mFpSwipeCallActionsValue));
             mFpSwipeCallActions.setSummary(mFpSwipeCallActions.getEntry());
             mFpSwipeCallActions.setOnPreferenceChangeListener(this);
        }

        mFpKeystore = (SwitchPreference) findPreference(FP_UNLOCK_KEYSTORE);
        mFpKeystore.setChecked((Settings.System.getInt(getContentResolver(),
            Settings.System.FP_UNLOCK_KEYSTORE, 0) == 1));
        mFpKeystore.setOnPreferenceChangeListener(this);

    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
		ContentResolver resolver = getActivity().getContentResolver();
		if (preference == mQuickPulldownFp) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(resolver,
                    Settings.System.STATUS_BAR_QUICK_QS_PULLDOWN_FP, value ? 1 : 0);
            return true;
        } else if (preference == mFpSwipeCallActions) {
            mFpSwipeCallActionsValue = Integer.valueOf((String) newValue);
            int index = mFpSwipeCallActions.findIndexOfValue((String) newValue);
            mFpSwipeCallActions.setSummary(
                    mFpSwipeCallActions.getEntries()[index]);
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.FP_SWIPE_CALL_ACTIONS, mFpSwipeCallActionsValue,
                    UserHandle.USER_CURRENT);
            return true;
        }  else if (preference == mFpKeystore) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(resolver,
                    Settings.System.FP_UNLOCK_KEYSTORE, value ? 1 : 0);
            return true;
        }
        return false;
    }
}
