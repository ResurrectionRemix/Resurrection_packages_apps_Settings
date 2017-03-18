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
import android.os.ServiceManager;
import android.content.Context;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.support.v14.preference.SwitchPreference;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import com.android.internal.logging.MetricsProto.MetricsEvent;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.ITelephonyRegistry;

import java.util.List;

public class StatusBarIcons extends SettingsPreferenceFragment {
    private static final String TAG = "StatusBarIcons";
    private static final String VOLTE_SWITCH = "volte_icon_enabled";
    private SwitchPreference mVolteSwitch;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.rr_sb_icons);

        mVolteSwitch = (SwitchPreference) findPreference(VOLTE_SWITCH);

        if (mVolteSwitch != null) {
            if (!isMobileIms()) {
                getPreferenceScreen().removePreference(mVolteSwitch);
            }
        }
    }

   public boolean isImsRegisteredForSubscriber(int subId) {
       try {
           ITelephony telephony = getITelephony();
           if (telephony == null)
               return false;
           return telephony.isImsRegisteredForSubscriber(subId);
       } catch (Exception ex) {
           return false;
       }
   }

    private boolean isMobileIms() {

        List<SubscriptionInfo> subInfos = SubscriptionManager.from(getContext())
                        .getActiveSubscriptionInfoList();
        if (subInfos != null) {
            for (SubscriptionInfo subInfo: subInfos) {
                int subId = subInfo.getSubscriptionId();
                if (isImsRegisteredForSubscriber(subId)) {
                    return true;
                }
            }
        } else {
            Log.e(TAG, "Invalid SubscriptionInfo");
        }
        return false;
     }

    private ITelephony getITelephony() {
        return ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
    }
}
