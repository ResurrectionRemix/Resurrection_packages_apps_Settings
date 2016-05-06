/*
 * Copyright (C) 2015 The Resurrection Remix  Project
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


import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.Build;
import com.android.settings.util.AbstractAsyncSuCMDProcessor;
import com.android.settings.util.CMDProcessor;
import com.android.settings.util.Helpers;
import android.preference.Preference;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.PreferenceScreen;
import android.preference.Preference;
import android.preference.ListPreference;
import android.preference.PreferenceCategory;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.provider.SearchIndexableResource;
import android.util.Log;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import android.preference.SwitchPreference;
import com.android.settings.util.Helpers;
import dalvik.system.VMRuntime;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.DevelopmentSettings;

import com.android.settings.Utils;

import java.io.File;
import java.io.IOException;
import java.io.DataOutputStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import com.android.internal.logging.MetricsLogger;

public class MiscSettings extends SettingsPreferenceFragment  implements OnPreferenceChangeListener, Indexable {

private static final String TAG = "MiscSettings";
private static final String RESTART_SYSTEMUI = "restart_systemui";
private static final String SELINUX = "selinux";
private static final String CATEGORY_VIB = "misc_4";
private static final String DOZE_POWERSAVE_PROPERTY = "persist.sys.doze_powersave";
private static final String DOZE_POWERSAVE_KEY = "doze_powersave";
private final ArrayList<Preference> mAllPrefs = new ArrayList<Preference>();
private final ArrayList<SwitchPreference> mResetSwitchPrefs  = new ArrayList<SwitchPreference>();

private Preference mRestartSystemUI;
private SwitchPreference mSelinux;
private FingerprintManager mFingerprintManager;
private SwitchPreference mFingerprintVib;
private SwitchPreference mDozePowersave;
private boolean mDontPokeProperties;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.rr_misc);
	final ContentResolver resolver = getActivity().getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();
	mRestartSystemUI = findPreference(RESTART_SYSTEMUI);

        //SELinux
        mSelinux = (SwitchPreference) findPreference(SELINUX);
        mSelinux.setOnPreferenceChangeListener(this);

 	 if (CMDProcessor.runShellCommand("getenforce").getStdout().contains("Enforcing")) {
            mSelinux.setChecked(true);
            mSelinux.setSummary(R.string.selinux_enforcing_title);
        } else {
            mSelinux.setChecked(false);
            mSelinux.setSummary(R.string.selinux_permissive_title);
         }
         
        mDozePowersave = findAndInitSwitchPref(DOZE_POWERSAVE_KEY);
        updateDozePowersaveOptions();

        mFingerprintManager = (FingerprintManager) getActivity().getSystemService(Context.FINGERPRINT_SERVICE);
	PreferenceCategory mVibratepref = (PreferenceCategory)
        getPreferenceScreen().findPreference(CATEGORY_VIB);         
        mFingerprintVib = (SwitchPreference) prefScreen.findPreference("fingerprint_success_vib");
        if (!mFingerprintManager.isHardwareDetected()){
            getPreferenceScreen().removePreference(mVibratepref);
        }
     }
     
     
    private SwitchPreference findAndInitSwitchPref(String key) {
        SwitchPreference pref = (SwitchPreference) findPreference(key);
        if (pref == null) {
            throw new IllegalArgumentException("Cannot find preference with key = " + key);
        }
        mAllPrefs.add(pref);
        mResetSwitchPrefs.add(pref);
        return pref;
    }
        

    protected int getMetricsCategory()
    {
	return MetricsLogger.DEVELOPMENT;
    }

	@Override
    public void onResume() {
        super.onResume();
    }
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
 	if (preference == mRestartSystemUI) {
           Helpers.restartSystemUI();  
	}  else if (preference == mDozePowersave) {
            writeDozePowersaveOptions();
	} else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }  
        return false;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
     ContentResolver resolver = getActivity().getContentResolver();
            if (preference == mSelinux) {
            if (newValue.toString().equals("true")) {
                CMDProcessor.runSuCommand("setenforce 1");
                mSelinux.setSummary(R.string.selinux_enforcing_title);
            } else if (newValue.toString().equals("false")) {
                CMDProcessor.runSuCommand("setenforce 0");
                mSelinux.setSummary(R.string.selinux_permissive_title);
            }
            return true;
         } 
        return false;
     } 
     
             
        
    void pokeSystemProperties() {
        if (!mDontPokeProperties) {
            //noinspection unchecked
            (new SystemPropPoker()).execute();
        }
    }
     
   private void updateDozePowersaveOptions() {
        updateSwitchPreference(mDozePowersave, SystemProperties.getBoolean(DOZE_POWERSAVE_PROPERTY, false));
    }
    
    void updateSwitchPreference(SwitchPreference switchPreference, boolean value) {
        switchPreference.setChecked(value);
    }

    private void writeDozePowersaveOptions() {
        SystemProperties.set(DOZE_POWERSAVE_PROPERTY, mDozePowersave.isChecked() ? "true" : "false");
        pokeSystemProperties();
    }

   public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                                                                            boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                   sir.xmlResId = R.xml.rr_misc;
                    result.add(sir);

                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    final List<String> keys = new ArrayList<String>();
                    return keys;
                }
        };
        
        
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
                    } catch (Exception e) {
                        Log.i(TAG, "Someone wrote a bad service '" + service
                                + "' that doesn't like to be poked: " + e);
                    }
                    data.recycle();
                }
            }
            return null;
        }
    }
}

