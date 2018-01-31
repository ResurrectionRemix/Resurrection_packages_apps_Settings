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

import android.os.Bundle;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

import android.provider.Settings;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import com.android.internal.utils.du.DUActionUtils;
import com.android.settings.applications.LayoutPreference;

public class ButtonSettingsNav extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String SMARTBAR = "smartbar_settings";
    private static final String PULSE = "pulse_settings";
    private static final String FLING = "fling_settings";
    private static final String STOCK = "stock";

    private LayoutPreference mPulseFrag;
    private LayoutPreference mSmartbarFrag;
    private LayoutPreference mFlingFrag;
    private LayoutPreference mStockFrag;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rr_buttons_navigation);

        boolean mNavbarEnabled = Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.NAVIGATION_BAR_VISIBLE,
                DUActionUtils.hasNavbarByDefault(getActivity()) ? 1 : 0) != 0;
        int mNavbarType = Settings.Secure.getInt(getContentResolver(), Settings.Secure.NAVIGATION_BAR_MODE,
                0);
        mPulseFrag = (LayoutPreference) findPreference(PULSE);
        mSmartbarFrag = (LayoutPreference) findPreference(SMARTBAR);
        mFlingFrag = (LayoutPreference) findPreference(FLING);
        mStockFrag = (LayoutPreference) findPreference(STOCK);
        updateEnableMent(mNavbarEnabled, mNavbarType);
 


    }

    public void updateEnableMent(boolean enable, int mNavbarType) {
        if (!enable) {
            getPreferenceScreen().removePreference(mPulseFrag);
            getPreferenceScreen().removePreference(mFlingFrag);
            getPreferenceScreen().removePreference(mStockFrag);
            return;
        } else {
            if (mNavbarType == 0) {
                getPreferenceScreen().removePreference(mPulseFrag);
                getPreferenceScreen().removePreference(mFlingFrag);
                getPreferenceScreen().removePreference(mSmartbarFrag);
            } else if (mNavbarType == 1) {
                getPreferenceScreen().removePreference(mFlingFrag);
                getPreferenceScreen().removePreference(mStockFrag);
            } else if (mNavbarType == 2) {
                getPreferenceScreen().removePreference(mStockFrag);
                getPreferenceScreen().removePreference(mSmartbarFrag);
            }
        }
   }

    @Override    
    public boolean onPreferenceChange(Preference preference, Object objValue) 		{
        return false;
    }
}
