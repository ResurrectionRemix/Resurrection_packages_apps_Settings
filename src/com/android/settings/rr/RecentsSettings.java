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
import android.content.Intent;
import android.content.Context;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.app.Fragment;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.rr.Preferences.MasterSwitchPreference;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

public class RecentsSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String PREF_STOCK_RECENTS_CATEGORY = "stock_recents_category";
    private static final String PREF_ALTERNATIVE_RECENTS_CATEGORY = "alternative_recents_category";

    private PreferenceCategory mStockRecentsCategory;
    private PreferenceCategory mAlternativeRecentsCategory;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rr_recents);

 
         mStockRecentsCategory = (PreferenceCategory) findPreference(PREF_STOCK_RECENTS_CATEGORY);
         mAlternativeRecentsCategory =
                 (PreferenceCategory) findPreference(PREF_ALTERNATIVE_RECENTS_CATEGORY);
 
         // Alternative recents en-/disabling
         Preference.OnPreferenceChangeListener alternativeRecentsChangeListener =
                 new Preference.OnPreferenceChangeListener() {
             @Override
             public boolean onPreferenceChange(Preference preference, Object newValue) {
                 updateDependencies((Boolean) newValue ? preference : null);
                 return true;
             }
         };
         for (int i = 0; i < mAlternativeRecentsCategory.getPreferenceCount(); i++) {
             Preference preference = mAlternativeRecentsCategory.getPreference(i);
             if (preference instanceof MasterSwitchPreference) {
                 preference.setOnPreferenceChangeListener(alternativeRecentsChangeListener);
             }
         }
    }

     @Override
     public void onResume() {
         super.onResume();
 
         for (int i = 0; i < mAlternativeRecentsCategory.getPreferenceCount(); i++) {
             Preference preference = mAlternativeRecentsCategory.getPreference(i);
             if (preference instanceof MasterSwitchPreference) {
                 ((MasterSwitchPreference) preference).reloadValue();
             }
         }
         updateDependencies(null);
     }
 
     private void updateDependencies(Preference enabledAlternativeRecentsPreference) {
         boolean alternativeRecentsEnabled = false;
         for (int i = 0; i < mAlternativeRecentsCategory.getPreferenceCount(); i++) {
             Preference preference = mAlternativeRecentsCategory.getPreference(i);
             if (enabledAlternativeRecentsPreference != null
                     && enabledAlternativeRecentsPreference != preference
                     && preference instanceof MasterSwitchPreference
                     && ((MasterSwitchPreference) preference).isChecked()) {
                 // Only one alternative recents at the time!
                 ((MasterSwitchPreference) preference).setCheckedPersisting(false);
             } else if (preference instanceof MasterSwitchPreference
                     && ((MasterSwitchPreference) preference).isChecked()) {
                 alternativeRecentsEnabled = true;
             }
         }
         mStockRecentsCategory.setEnabled(!alternativeRecentsEnabled);
     }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
		return false;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

}
