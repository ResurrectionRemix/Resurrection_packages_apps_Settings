/*
 * Copyright (C) 2016 Resurrection Remix 
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

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import android.provider.SearchIndexableResource;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import android.preference.SwitchPreference;
import com.android.settings.util.Helpers;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import com.android.internal.logging.MetricsLogger;
import android.preference.Preference.OnPreferenceChangeListener;

public class MultiFragment extends SettingsPreferenceFragment  implements OnPreferenceChangeListener, Indexable{
private static final String ENABLE_MULTI_WINDOW_KEY = "enable_multi_window";
private static final String MULTI_WINDOW_SYSTEM_PROPERTY = "persist.sys.debug.multi_window";
private static final String KEY_FW = "floating_windows";
private static final String KEY_PIE = "pa_pie_control";

    private PreferenceScreen mFloatingWindow;
    private PreferenceScreen mPie;


private SwitchPreference mEnableMultiWindow;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Activity activity = getActivity();
        ContentResolver resolver = getActivity().getContentResolver();
        addPreferencesFromResource(R.xml.rr_multitasking);
        PreferenceScreen prefSet = getPreferenceScreen();
        mEnableMultiWindow = (SwitchPreference) findPreference(ENABLE_MULTI_WINDOW_KEY);
        mFloatingWindow = (PreferenceScreen) findPreference(KEY_FW);
        mPie = (PreferenceScreen) findPreference(KEY_PIE);
    }

    protected int getMetricsCategory()
    {
	return MetricsLogger.APPLICATION;
    }
    
    private static boolean showEnableMultiWindowPreference() {
        return !"user".equals(Build.TYPE);
    }
    
    private void setEnableMultiWindow(boolean value) {
        SystemProperties.set(MULTI_WINDOW_SYSTEM_PROPERTY, String.valueOf(value));
    }
    
    @Override
    public void onResume() {
        super.onResume();
    }
           
    public boolean onPreferenceChange(Preference preference, Object newValue) {
    	ContentResolver resolver = getActivity().getContentResolver();
	Resources res = getResources();
	return false;
    } 
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mEnableMultiWindow) {
            if (mEnableMultiWindow.isChecked()) {
                setEnableMultiWindow(true);
            } else {
                setEnableMultiWindow(false);
            }
        }  else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }  
        return false;
    }
    
       public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                                                                            boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                   sir.xmlResId = R.xml.rr_multitasking;
                    result.add(sir);

                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    final List<String> keys = new ArrayList<String>();
                    return keys;
                }
        };

}
