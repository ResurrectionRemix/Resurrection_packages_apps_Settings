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

import android.os.Bundle;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionManager;

import com.android.internal.logging.MetricsProto.MetricsEvent;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class StatusBarIcons extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "StatusBarIcons";
    private static final String FOUR_G = "show_fourg";
    private static final String THREE_G = "show_threeg";
    private static final String ROAMING_PREF = "roaming_indicator_icon";
    private static final String DATA_DISABLED_PREF = "data_disabled_icon";
    private static final String SIM_EMPTY_SWITCH = "no_sim_cluster_switch";
    private SubscriptionManager mSm;


    private SwitchPreference mNoSims;
    private SwitchPreference m4g;
    private SwitchPreference m3g;
    private SwitchPreference mRoaming;
    private SwitchPreference mNoData;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.rr_sb_icons);

        mNoSims = (SwitchPreference) findPreference(SIM_EMPTY_SWITCH);
        m4g = (SwitchPreference) findPreference(FOUR_G);
        m3g = (SwitchPreference) findPreference(THREE_G);
        mRoaming = (SwitchPreference) findPreference(ROAMING_PREF);
        mNoData = (SwitchPreference) findPreference(DATA_DISABLED_PREF);
        mSm = (SubscriptionManager) getSystemService(getContext().TELEPHONY_SUBSCRIPTION_SERVICE);

        if (mNoSims != null) { 
            if (!TelephonyManager.getDefault().isMultiSimEnabled() || mSm.getActiveSubscriptionInfoCount() <= 0){
                getPreferenceScreen().removePreference(mNoSims);
            }
        }

        if (mSm.getActiveSubscriptionInfoCount() <= 0) {
                getPreferenceScreen().removePreference(m4g);
                getPreferenceScreen().removePreference(m3g);
                getPreferenceScreen().removePreference(mRoaming);
                getPreferenceScreen().removePreference(mNoData);
        }
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) 		{
        return true;
    }
}
