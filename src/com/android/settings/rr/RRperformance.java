/*
 * Copyright (C) 2014 The Resurrection Remix Project
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

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import com.android.internal.os.PowerProfile;
import android.os.SystemProperties;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.content.res.Resources;
import android.os.BatteryManager;
import android.os.BatteryStats;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

import com.android.settings.DevelopmentSettings;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.rr.IOScheduler;
import com.android.settings.Utils;
import com.android.settings.SettingsActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class RRperformance extends SettingsPreferenceFragment  
	implements Preference.OnPreferenceChangeListener  {
private PreferenceScreen mProcessor;
private PreferenceScreen mIOScheduler;
private PreferenceScreen mKernelAdiutor;
private ListPreference mSystemCategory;
private SwitchPreference mForceHighEndGfx;
private PreferenceCategory mKernel;
private ListPreference mScrollingCachePref;
private static final String CATEGORY_KERNEL="kernel_aduitor";
private static final String CATEGORY_KERNEL_ADIUTOR = "kernel_tweaks";
private static final String CATEGORY_PROFILES = "pref_perf_profile";
private static final String FORCE_HIGHEND_GFX_PREF = "pref_force_highend_gfx";
private static final String CATEGORY_IOSCHEDUlER = "ioscheduler";
private static final String CATEGORY_PROCESSOR = "processor";
private static final String CATEGORY_GRAPHICS = "perf_graphics_prefs";
private static final String FORCE_HIGHEND_GFX_PERSIST_PROP = "persist.sys.force_highendgfx";
private String[] mPerfProfileEntries;
private String[] mPerfProfileValues;

private AlertDialog mAlertDialog;
private PowerManager mPowerManager;
private SharedPreferences mDevelopmentPreferences;
private PerformanceProfileObserver mPerformanceProfileObserver = null;
private static final String SCROLLINGCACHE_PREF = "pref_scrollingcache";
private static final String SCROLLINGCACHE_PERSIST_PROP = "persist.sys.scrollingcache";
private static final String SCROLLINGCACHE_DEFAULT = "1";



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	
        mPerfProfileEntries= getResources().getStringArray(
                com.android.internal.R.array.perf_profile_entries);
        mPerfProfileValues = getResources().getStringArray(
                com.android.internal.R.array.perf_profile_values);

        addPreferencesFromResource(R.xml.rr_performance_settings);
	PreferenceScreen prefSet = getPreferenceScreen();
	mProcessor = (PreferenceScreen) prefSet.findPreference(CATEGORY_PROCESSOR);
       mIOScheduler = (PreferenceScreen) prefSet.findPreference(CATEGORY_IOSCHEDUlER);
	mKernelAdiutor = (PreferenceScreen) prefSet.findPreference(CATEGORY_KERNEL_ADIUTOR);
	mKernel = (PreferenceCategory) prefSet.findPreference(CATEGORY_KERNEL);
	mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
	mSystemCategory = (ListPreference) findPreference(CATEGORY_PROFILES);
	mForceHighEndGfx = (SwitchPreference) prefSet.findPreference(FORCE_HIGHEND_GFX_PREF);
	 PreferenceCategory category = (PreferenceCategory) prefSet.findPreference(CATEGORY_GRAPHICS);

	//Look For Powe Profiles
        if (!mPowerManager.hasPowerProfiles()) {
	prefSet.removePreference(mSystemCategory);
	}
	else {
	prefSet.removePreference(mIOScheduler);
	    prefSet.addPreference(mSystemCategory);
	   prefSet.addPreference(category);	
	    mSystemCategory.setOrder(-1);
            mSystemCategory.setEntries(mPerfProfileEntries);
            mSystemCategory.setEntryValues(mPerfProfileValues);
	   updatePerformanceValue();
	mSystemCategory.setOnPreferenceChangeListener(this);
	   
		String forceHighendGfx = SystemProperties.get(FORCE_HIGHEND_GFX_PERSIST_PROP, "false");
        mForceHighEndGfx.setChecked("true".equals(forceHighendGfx));	
	}

        boolean supported = false;
	try {
            supported = (getPackageManager().getPackageInfo("com.grarak.kerneladiutor", 0).versionCode >= 0);
	} catch (PackageManager.NameNotFoundException e) {
        }
        if (!supported) {
           prefSet.removePreference(mKernelAdiutor);
		prefSet.removePreference(mKernel);
		prefSet.addPreference(mSystemCategory);
		 prefSet.addPreference(category);
	   	    mSystemCategory.setOrder(-1);
            mSystemCategory.setEntries(mPerfProfileEntries);
            mSystemCategory.setEntryValues(mPerfProfileValues);
	   updatePerformanceValue();	
	mSystemCategory.setOnPreferenceChangeListener(this);	
	String forceHighendGfx = SystemProperties.get(FORCE_HIGHEND_GFX_PERSIST_PROP, "false");
        mForceHighEndGfx.setChecked("true".equals(forceHighendGfx));				   
        } else  {
            prefSet.removePreference(mIOScheduler);	
 	 }
	mPerformanceProfileObserver = new PerformanceProfileObserver(new Handler());	
	
	
        // Scrolling cache
        mScrollingCachePref = (ListPreference) prefSet.findPreference(SCROLLINGCACHE_PREF);
        mScrollingCachePref.setValue(SystemProperties.get(SCROLLINGCACHE_PERSIST_PROP,
                SystemProperties.get(SCROLLINGCACHE_PERSIST_PROP, SCROLLINGCACHE_DEFAULT)));
        mScrollingCachePref.setOnPreferenceChangeListener(this);	
	}	
    
	@Override
    public void onResume() {
        super.onResume();
        if (mSystemCategory != null) {
            updatePerformanceValue();
            ContentResolver resolver = getActivity().getContentResolver();
            resolver.registerContentObserver(Settings.Secure.getUriFor(
                    Settings.Secure.PERFORMANCE_PROFILE), false, mPerformanceProfileObserver);
       				 }
	}

    @Override
    public void onPause() {
        super.onPause();
        if (mSystemCategory != null) {
            ContentResolver resolver = getActivity().getContentResolver();
            resolver.unregisterContentObserver(mPerformanceProfileObserver);
        }
    }

    private void updatePerformanceValue() {
        if (mSystemCategory == null) {
            return;
        }
        mSystemCategory.setValue(mPowerManager.getPowerProfile());
        updatePerformanceSummary();
	}	

        public void onChange(boolean selfChange, Uri uri) {
            updatePerformanceValue();
        }

    private void updatePerformanceSummary() {
        String value = mPowerManager.getPowerProfile();
        String summary = "";
        int count = mPerfProfileValues.length;
        for (int i = 0; i < count; i++) {
            try {
                if (mPerfProfileValues[i].equals(value)) {
                    summary = mPerfProfileEntries[i];
                }
            } catch (IndexOutOfBoundsException ex) {
                // Ignore
            }
        }
        mSystemCategory.setSummary(String.format("%s", summary));
    }
    private class PerformanceProfileObserver extends ContentObserver {
        public PerformanceProfileObserver(Handler handler) {
            super(handler);
        	}
	}
 	@Override
    	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mForceHighEndGfx) {
            SystemProperties.set(FORCE_HIGHEND_GFX_PERSIST_PROP,
                    mForceHighEndGfx.isChecked() ? "true" : "false");
        } else {
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        	}

        return true;
    	}
	
	@Override
    	public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (newValue != null) {
            if (preference == mSystemCategory) {
                mPowerManager.setPowerProfile(String.valueOf(newValue));
                updatePerformanceSummary();
                return true;
       } else if (preference == mScrollingCachePref) {
            if (newValue != null) {
                SystemProperties.set(SCROLLINGCACHE_PERSIST_PROP, (String)newValue);
            return true;
	            }
	        }
	    }
	        return false;
	 }		
    
   @Override
    public void onStop() {
        super.onStop();
    }

}	
