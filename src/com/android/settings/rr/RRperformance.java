/*
 * Copyright (C) 2014 The LiquidSmooth Project
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
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;

import com.android.settings.DevelopmentSettings;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.rr.IOScheduler;

public class RRperformance extends SettingsPreferenceFragment {
private PreferenceScreen mProcessor;
private PreferenceScreen mIOScheduler;
private PreferenceScreen mKernelAdiutor;
private PreferenceCategory mSystemCategory;
private static final String CATEGORY_KERNEL_ADIUTOR = "kernel_tweaks";
private static final String CATEGORY_PROFILES = "pref_perf_profile";
private static final String FORCE_HIGHEND_GFX_PREF = "pref_force_highend_gfx";
private static final String CATEGORY_IOSCHEDUlER = "ioscheduler";
private static final String CATEGORY_PROCESSOR = "processor";

private AlertDialog mAlertDialog;
private PowerManager mPowerManager;
private SharedPreferences mDevelopmentPreferences;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	

        addPreferencesFromResource(R.xml.rr_performance_settings);
	PreferenceScreen prefSet = getPreferenceScreen();
	mProcessor = (PreferenceScreen) prefSet.findPreference(CATEGORY_PROCESSOR);
       mIOScheduler = (PreferenceScreen) prefSet.findPreference(CATEGORY_IOSCHEDUlER);
	mKernelAdiutor = (PreferenceScreen) prefSet.findPreference(CATEGORY_KERNEL_ADIUTOR);
	mSystemCategory = (PreferenceCategory) prefSet.findPreference(CATEGORY_PROFILES);
	mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);


        PreferenceCategory category = (PreferenceCategory) prefSet.findPreference(CATEGORY_PROFILES);
        if (!mPowerManager.hasPowerProfiles()) {
	prefSet.removePreference(category);
	}
	else {
	prefSet.removePreference(mIOScheduler);
	prefSet.addPreference(mSystemCategory);
	}

        boolean supported = false;
	try {
            supported = (getPackageManager().getPackageInfo("com.grarak.kerneladiutor", 0).versionCode >= 0);
} catch (PackageManager.NameNotFoundException e) {
        }
        if (!supported) {
           prefSet.removePreference(mKernelAdiutor);
	   prefSet.addPreference(mSystemCategory);				   
        } else  {
            prefSet.removePreference(mIOScheduler);	
  }		
}	
    @Override
    public void onResume() {
        super.onResume();
	}

    @Override
    public void onPause() {
        super.onPause();
    }
}
