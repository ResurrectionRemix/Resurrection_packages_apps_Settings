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

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.app.admin.DevicePolicyManager;
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
import android.os.UserHandle;
import android.os.UserManager;
import android.os.ServiceManager;
import android.os.IBinder;
import android.os.Parcel;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.util.Log;
import android.widget.Switch;
import com.android.settings.DevelopmentSettings;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.rr.IOScheduler;
import com.android.settings.Utils;
import com.android.settings.SettingsActivity;
import com.android.settings.widget.SwitchBar;
import android.text.TextUtils;
import android.view.View;
import android.view.Menu;
import android.view.accessibility.AccessibilityManager;
import android.view.HardwareRenderer;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;


public class RRperformance extends SettingsPreferenceFragment  implements Preference.OnPreferenceChangeListener , SwitchBar.OnSwitchChangeListener  {

private static final String CATEGORY_KERNEL="kernel_aduitor";
private static final String CATEGORY_KERNEL_ADIUTOR = "kernel_tweaks";
private static final String CATEGORY_PROFILES = "pref_perf_profile";
private static final String FORCE_HIGHEND_GFX_PREF = "pref_force_highend_gfx";
private static final String CATEGORY_IOSCHEDUlER = "ioscheduler";
private static final String CATEGORY_PROCESSOR = "processor";
private static final String CATEGORY_GRAPHICS = "perf_graphics_prefs";
private static final String FORCE_HIGHEND_GFX_PERSIST_PROP = "persist.sys.force_highendgfx";
private static final String ENABLE_QUICKBOOT= "enable_quickboot";
private static final String QUICKBOOT_PACKAGE_NAME = "com.qapp.quickboot";
private static final String APP_PROCESS_LIMIT_KEY = "app_process_limit";
private static final String FORCE_HARDWARE_UI_KEY = "force_hw_ui";
private PerformanceProfileObserver mPerformanceProfileObserver = null;
private static final String SCROLLINGCACHE_PREF = "pref_scrollingcache";
private static final String SCROLLINGCACHE_PERSIST_PROP = "persist.sys.scrollingcache";
private static final String DISABLE_OVERLAYS_KEY = "disable_overlays";
private static final String SCROLLINGCACHE_DEFAULT = "1";
private static final String BOOT_OPTIONS = "rr_boot";
private static final String HARDWARE_UI_PROPERTY = "persist.sys.ui.hw";
public static final String PREF_SHOW = "show";
public static final String PREF_FILE = "development";
private String[] mPerfProfileEntries;
private String[] mPerfProfileValues;
private boolean mDontPokeProperties;
private boolean mHaveDebugSettings;
private boolean mLastEnabledState;
private boolean mDialogClicked;
private boolean mUnavailable;

private AlertDialog mAlertDialog;
private Dialog mEnableDialog;
private PowerManager mPowerManager;
private SharedPreferences mDevelopmentPreferences;
private SwitchPreference mQuickBoot;
private ListPreference mAppProcessLimit;
private PreferenceScreen mProcessor;
private PreferenceScreen mIOScheduler;
private PreferenceScreen mKernelAdiutor;
private ListPreference mSystemCategory;
private SwitchPreference mForceHighEndGfx;
private PreferenceCategory mKernel;
private ListPreference mScrollingCachePref;
private SwitchPreference mForceHardwareUi;
private SwitchPreference mDisableOverlays;
private PreferenceCategory mBoot;
private SwitchBar mSwitchBar;

private final ArrayList<Preference> mAllPrefs = new ArrayList<Preference>();
private final ArrayList<SwitchPreference> mResetSwitchPrefs = new ArrayList<SwitchPreference>();
private final HashSet<Preference> mDisabledPrefs = new HashSet<Preference>();

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
        mScrollingCachePref = (ListPreference) prefSet.findPreference(SCROLLINGCACHE_PREF);
        mScrollingCachePref.setValue(SystemProperties.get(SCROLLINGCACHE_PERSIST_PROP,
        SystemProperties.get(SCROLLINGCACHE_PERSIST_PROP, SCROLLINGCACHE_DEFAULT)));
	mBoot = (PreferenceCategory) prefSet.findPreference(BOOT_OPTIONS);
	
	//Look For Power Profiles
        if (!mPowerManager.hasPowerProfiles()) {
	prefSet.removePreference(mSystemCategory);
	}
	else {
	prefSet.removePreference(mIOScheduler);
	   prefSet.addPreference(mSystemCategory);
	   prefSet.addPreference(category);
		mAppProcessLimit = addListPreference(APP_PROCESS_LIMIT_KEY);
	        mDisableOverlays = findAndInitSwitchPref(DISABLE_OVERLAYS_KEY);
		mForceHardwareUi = findAndInitSwitchPref(FORCE_HARDWARE_UI_KEY);
		mQuickBoot = findAndInitSwitchPref(ENABLE_QUICKBOOT);
 	if (!Utils.isPackageInstalled(getActivity(), QUICKBOOT_PACKAGE_NAME, false)) {   prefSet.removePreference(mBoot);
    removePreference1(mQuickBoot);		
	    		
	}
	   mSystemCategory.setOrder(-1);
           mSystemCategory.setEntries(mPerfProfileEntries);
           mSystemCategory.setEntryValues(mPerfProfileValues);
	   updatePerformanceValue();
	   mSystemCategory.setOnPreferenceChangeListener(this);
	   
		String forceHighendGfx = SystemProperties.get(FORCE_HIGHEND_GFX_PERSIST_PROP, "false");
        mForceHighEndGfx.setChecked("true".equals(forceHighendGfx));
	updateAppProcessLimitOptions();	
	}

        boolean supported = false;
	try {
            supported = (getPackageManager().getPackageInfo("com.grarak.kerneladiutor", 0).versionCode >= 0);
	} catch (PackageManager.NameNotFoundException e) {
        }
        if (!supported) {
           prefSet.removePreference(mKernelAdiutor);
		prefSet.removePreference(mKernel);
		mAppProcessLimit = addListPreference(APP_PROCESS_LIMIT_KEY);
	        mDisableOverlays = findAndInitSwitchPref(DISABLE_OVERLAYS_KEY);
		mForceHardwareUi = findAndInitSwitchPref(FORCE_HARDWARE_UI_KEY);
		mQuickBoot = findAndInitSwitchPref(ENABLE_QUICKBOOT);
	           if (!Utils.isPackageInstalled(getActivity(), QUICKBOOT_PACKAGE_NAME, false)) {
	    prefSet.removePreference(mBoot);
	    removePreference1(mQuickBoot);			    
	}
		 prefSet.addPreference(category);
	   	    mSystemCategory.setOrder(-1);
            mSystemCategory.setEntries(mPerfProfileEntries);
            mSystemCategory.setEntryValues(mPerfProfileValues);		
	   updatePerformanceValue();
	updateAppProcessLimitOptions();		
	mSystemCategory.setOnPreferenceChangeListener(this);	
	String forceHighendGfx = SystemProperties.get(FORCE_HIGHEND_GFX_PERSIST_PROP, "false");
        mForceHighEndGfx.setChecked("true".equals(forceHighendGfx));					   
        } else  {	
            prefSet.removePreference(mIOScheduler);
	}		
	mPerformanceProfileObserver = new PerformanceProfileObserver(new Handler());	

        // Scrolling cache
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
	else if (preference == mQuickBoot) {
            Settings.System.putInt(getActivity().getContentResolver(), ENABLE_QUICKBOOT,
                    mQuickBoot.isChecked() ? 1 : 0);
       		 }
	else if (preference == mAppProcessLimit) {
            writeAppProcessLimitOptions(newValue);
            return true;
	    }
	 else if (preference == mForceHardwareUi) {
            writeHardwareUiOptions();
        } 
	else if (preference == mDisableOverlays) {
            writeDisableOverlaysOption();
        	}
	}	
	        return false;		
    }

   @Override
    public void onStop() {
        super.onStop();
    }


    private void updateAppProcessLimitOptions() {
        try {
            int limit = ActivityManagerNative.getDefault().getProcessLimit();
            CharSequence[] values = mAppProcessLimit.getEntryValues();
            for (int i=0; i<values.length; i++) {
                int val = Integer.parseInt(values[i].toString());
                if (val >= limit) {
                    if (i != 0) {
                        mHaveDebugSettings = true;
                    }
                    mAppProcessLimit.setValueIndex(i);
                    mAppProcessLimit.setSummary(mAppProcessLimit.getEntries()[i]);
                    return;
                }
            }
            mAppProcessLimit.setValueIndex(0);
            mAppProcessLimit.setSummary(mAppProcessLimit.getEntries()[0]);
        } catch (RemoteException e) {
        }
    }

 private void writeAppProcessLimitOptions(Object newValue) {
        try {
            int limit = newValue != null ? Integer.parseInt(newValue.toString()) : -1;
            ActivityManagerNative.getDefault().setProcessLimit(limit);
            updateAppProcessLimitOptions();
        } catch (RemoteException e) {
        }
    }

    private void updateHardwareUiOptions() {
        updateSwitchPreference(mForceHardwareUi, SystemProperties.getBoolean(HARDWARE_UI_PROPERTY, true));
    }

    private void writeHardwareUiOptions() {
	
        SystemProperties.set(HARDWARE_UI_PROPERTY, mForceHardwareUi.isChecked() ? "true" : "false");
	 pokeSystemProperties();
    }

       void pokeSystemProperties() {
        if (!mDontPokeProperties) {
            //noinspection unchecked
            (new SystemPropPoker()).execute();
        }
    }

    private void writeDisableOverlaysOption() {
        try {
            IBinder flinger = ServiceManager.getService("SurfaceFlinger");
            if (flinger != null) {
                Parcel data = Parcel.obtain();
                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                final int disableOverlays = mDisableOverlays.isChecked() ? 1 : 0;
                data.writeInt(disableOverlays);
                flinger.transact(1008, data, null, 0);
                data.recycle();

                updateFlingerOptions();
            }
        } catch (RemoteException ex) {
        }
    }

 private void updateFlingerOptions()  {
        try {
            IBinder flinger = ServiceManager.getService("SurfaceFlinger");
            if (flinger != null) {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                flinger.transact(1010, data, reply, 0);
                @SuppressWarnings("unused")
                int showCpu = reply.readInt();
                @SuppressWarnings("unused")
                int enableGL = reply.readInt();
                int showUpdates = reply.readInt();
                @SuppressWarnings("unused")
                int showBackground = reply.readInt();
                int disableOverlays = reply.readInt();
                updateSwitchPreference(mDisableOverlays, disableOverlays != 0);
                reply.recycle();
                data.recycle();
            }
        } catch (RemoteException ex) {
        }
    }

    void updateSwitchPreference(SwitchPreference switchPreference, boolean value) {
        switchPreference.setChecked(value);
        mHaveDebugSettings |= value;
    }
    static class SystemPropPoker extends AsyncTask<Void, Void, Void> {
	@Override	
        protected Void doInBackground(Void... params) {
            String[] services;
            try {
                services = ServiceManager.listServices();
            } catch (RemoteException e) {
                return null;
            }
            for (String service : services) {
                IBinder obj = ServiceManager.checkService(service);
                if (obj != null) {
                    Parcel data = Parcel.obtain();
                    try {
                        obj.transact(IBinder.SYSPROPS_TRANSACTION, data, null, 0);
                    } catch (RemoteException e) {
                    } 
                    data.recycle();
                }
            }
            return null;
        }
   }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        if (switchView != mSwitchBar.getSwitch()) {
            return;
        }
        if (isChecked != mLastEnabledState) {
            if (isChecked) {
                mDialogClicked = false;
            } else {
		mDialogClicked = true;
            }
        }
    }

    private void setPrefsEnabledState(boolean enabled) {
        for (int i = 0; i < mAllPrefs.size(); i++) {
            Preference pref = mAllPrefs.get(i);
            pref.setEnabled(enabled && !mDisabledPrefs.contains(pref));
        }
        updateAllOptions();
    }

	private void updateAllOptions()
	{
	final Context context = getActivity();
        final ContentResolver cr = context.getContentResolver();
	updateSwitchPreference(mQuickBoot, Settings.System.getInt(cr, ENABLE_QUICKBOOT, 0) != 0);
	updateHardwareUiOptions();
	updateFlingerOptions();
	updateAppProcessLimitOptions();
	}

 private SwitchPreference findAndInitSwitchPref(String key) {
        SwitchPreference pref = (SwitchPreference) findPreference(key);
        if (pref == null) {
            throw new IllegalArgumentException("Cannot find preference with key = " + key);
        }
        mAllPrefs.add(pref);
        return pref;
    }

    private ListPreference addListPreference(String prefKey) {
        ListPreference pref = (ListPreference) findPreference(prefKey);
        mAllPrefs.add(pref);
        pref.setOnPreferenceChangeListener(this);
        return pref;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final SettingsActivity activity = (SettingsActivity) getActivity();

        mSwitchBar = activity.getSwitchBar();
       if (mUnavailable) {
            mSwitchBar.setEnabled(false);
            return;
        }

        mSwitchBar.addOnSwitchChangeListener(this);
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mUnavailable) {
            return;
        }
        mSwitchBar.removeOnSwitchChangeListener(this);
        mSwitchBar.hide();
    }

    private void removePreference1(Preference preference) {
        getPreferenceScreen().removePreference(preference);
        mAllPrefs.remove(preference);
    }

    private void resetDangerousOptions() {
        mDontPokeProperties = true;
        for (int i=0; i< mResetSwitchPrefs.size(); i++) {
            SwitchPreference cb = mResetSwitchPrefs.get(i);
            if (cb.isChecked()) {
                cb.setChecked(false);
                onPreferenceTreeClick(null, cb);
            }
        }
	updateAllOptions();
        mDontPokeProperties = false;
        pokeSystemProperties();
   }		
}	
