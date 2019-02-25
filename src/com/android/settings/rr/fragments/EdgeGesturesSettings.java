/*
 * Copyright (C) 2018 CarbonROM
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

package com.android.settings.rr.fragments;

import android.app.ActionBar; 
import android.os.Bundle; 
import android.os.UserHandle; 
import android.provider.Settings; 
import android.support.v14.preference.SwitchPreference; 
import android.support.v7.preference.Preference; 
 
import com.android.internal.logging.nano.MetricsProto; 
import com.android.internal.utils.ActionUtils; 
import com.android.settings.R;
import com.android.settings.rr.utils.RRUtils;
import com.android.settings.search.Indexable.SearchIndexProvider;
import com.android.settings.SettingsPreferenceFragment; 
import com.android.settings.rr.Preferences.SecureSettingSeekBarPreference; 
 
public class EdgeGesturesSettings extends SettingsPreferenceFragment implements 
        Preference.OnPreferenceChangeListener { 
 
    public static final String EDGE_GESTURES_ENABLED = "edge_gestures_enabled"; 
    public static final String EDGE_GESTURES_SCREEN_PERCENT = "edge_gestures_back_screen_percent"; 
 
    private String previousTitle; 
 
    private SwitchPreference enabledPreference; 
    private SecureSettingSeekBarPreference screenPercentPreference; 
 
    @Override 
    public void onCreate(Bundle savedInstanceState) { 
        super.onCreate(savedInstanceState); 
 
        addPreferencesFromResource(R.xml.rr_edge_gestures); 
 
        enabledPreference = (SwitchPreference) findPreference(EDGE_GESTURES_ENABLED); 
        /*enabledPreference.setChecked((Settings.System.getInt(getContentResolver(), 
                Settings.Secure.EDGE_GESTURES_ENABLED, 0) == 1));*/ 
        enabledPreference.setOnPreferenceChangeListener(this); 
 
        screenPercentPreference = (SecureSettingSeekBarPreference) findPreference(EDGE_GESTURES_SCREEN_PERCENT); 
        int percent = Settings.Secure.getIntForUser(getContentResolver(), Settings.Secure.EDGE_GESTURES_BACK_SCREEN_PERCENT, 60, UserHandle.USER_CURRENT); 
        screenPercentPreference.setValue(percent); 
    } 
 
    @Override 
    public void onStart() { 
        super.onStart(); 
 
        ActionBar actionBar = getActivity().getActionBar(); 
        previousTitle = actionBar.getTitle().toString(); 
        actionBar.setTitle(R.string.edge_gestures_title); 
    } 
 
    @Override 
    public void onStop() { 
        super.onStop(); 
 
        ActionBar actionBar = getActivity().getActionBar(); 
        actionBar.setTitle(previousTitle); 
    } 
 
    @Override 
    public int getMetricsCategory() { 
        return MetricsProto.MetricsEvent.RESURRECTED; 
    } 
 
    @Override 
    public boolean onPreferenceChange(Preference preference, Object newValue) { 
        if (preference == enabledPreference) { 
            int enabled = ((boolean) newValue) ? 1 : 0; 
            //Settings.Secure.putIntForUser(getContentResolver(), Settings.Secure.EDGE_GESTURES_ENABLED, enabled, UserHandle.USER_CURRENT); 
 
            if (enabled == 1) { 
                Settings.Secure.putInt(getContentResolver(), 
                        Settings.Secure.NAVIGATION_BAR_VISIBLE, 
                        0); 
            } else { 
                if (ActionUtils.hasNavbarByDefault(getPrefContext())) { 
                    Settings.Secure.putInt(getContentResolver(), 
                            Settings.Secure.NAVIGATION_BAR_VISIBLE, 
                            1); 
                } 
            } 
            return true; 
        } /*else if (preference == hapticFeedbackDurationPreference) { 
            int hapticFeedbackValue = Integer.valueOf((String) newValue); 
            Settings.Secure.putIntForUser(getContentResolver(), Settings.Secure.EDGE_GESTURES_FEEDBACK_DURATION, hapticFeedbackValue, UserHandle.USER_CURRENT); 
            return true; 
        } else if (preference == longPressDurationPreference) { 
            int longPressValue = Integer.valueOf((String) newValue); 
            Settings.Secure.putIntForUser(getContentResolver(), Settings.Secure.EDGE_GESTURES_LONG_PRESS_DURATION, longPressValue, UserHandle.USER_CURRENT); 
            return true; 
        }*/ 
 
        return false; 
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        RRUtils.addSearchIndexProvider(R.xml.rr_edge_gestures);
}
