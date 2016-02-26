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
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.PreferenceScreen;
import android.preference.Preference;
import android.preference.ListPreference;
import android.preference.PreferenceCategory;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.provider.SearchIndexableResource;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import android.preference.SwitchPreference;
import com.android.settings.util.Helpers;
import dalvik.system.VMRuntime;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

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
private static final String ENABLE_MULTI_WINDOW_KEY = "enable_multi_window";
private static final String MULTI_WINDOW_SYSTEM_PROPERTY = "persist.sys.debug.multi_window";
private static final String RESTART_SYSTEMUI = "restart_systemui";
private static final String SELINUX = "selinux";
private static final String MEDIA_SCANNER_ON_BOOT = "media_scanner_on_boot";
private static final String CATEGORY_VIB = "misc_4";


private SwitchPreference mEnableMultiWindow;
private Preference mRestartSystemUI;
private SwitchPreference mSelinux;
private ListPreference mMSOB;
private FingerprintManager mFingerprintManager;
private SwitchPreference mFingerprintVib;

private final ArrayList<Preference> mAllPrefs = new ArrayList<Preference>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.rr_misc);
	  final ContentResolver resolver = getActivity().getContentResolver();
          final PreferenceScreen prefScreen = getPreferenceScreen();
        mEnableMultiWindow = (SwitchPreference) findPreference(ENABLE_MULTI_WINDOW_KEY);
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

        mMSOB = (ListPreference) findPreference(MEDIA_SCANNER_ON_BOOT);
        mAllPrefs.add(mMSOB);
        mMSOB.setOnPreferenceChangeListener(this);
        updateMSOBOptions();
	
        mFingerprintManager = (FingerprintManager) getActivity().getSystemService(Context.FINGERPRINT_SERVICE);
	PreferenceCategory mVibratepref = (PreferenceCategory)
        getPreferenceScreen().findPreference(CATEGORY_VIB);         
        mFingerprintVib = (SwitchPreference) prefScreen.findPreference("fingerprint_success_vib");
        if (!mFingerprintManager.isHardwareDetected()){
            getPreferenceScreen().removePreference(mVibratepref);
        }
     }
        
  private static boolean showEnableMultiWindowPreference() {
        return !"user".equals(Build.TYPE);
    }

    private void setEnableMultiWindow(boolean value) {
        SystemProperties.set(MULTI_WINDOW_SYSTEM_PROPERTY, String.valueOf(value));
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
        if (preference == mEnableMultiWindow) {
            if (mEnableMultiWindow.isChecked()) {
                setEnableMultiWindow(true);
            } else {
                setEnableMultiWindow(false);
            }
        }
 	else if (preference == mRestartSystemUI) {
           Helpers.restartSystemUI();  
	}   else {
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
         } else if (preference == mMSOB) {
            writeMSOBOptions(newValue);
            return true;
	}
        return false;
     } 

   private void resetMSOBOptions() {
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.MEDIA_SCANNER_ON_BOOT, 0);
    }

    private void writeMSOBOptions(Object newValue) {
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.MEDIA_SCANNER_ON_BOOT,
                Integer.valueOf((String) newValue));
        updateMSOBOptions();
    }

    private void updateMSOBOptions() {
        int value = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.MEDIA_SCANNER_ON_BOOT, 0);
        mMSOB.setValue(String.valueOf(value));
        mMSOB.setSummary(mMSOB.getEntry());
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
}

