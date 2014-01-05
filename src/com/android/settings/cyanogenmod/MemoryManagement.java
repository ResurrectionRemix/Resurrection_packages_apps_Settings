/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.android.settings.cyanogenmod;

import java.io.File;

import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class MemoryManagement extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    public static final String KSM_RUN_FILE = "/sys/kernel/mm/ksm/run";

    public static final String KSM_PREF = "pref_ksm";

    public static final String KSM_PREF_DISABLED = "0";

    public static final String KSM_PREF_ENABLED = "1";

    private static final String PURGEABLE_ASSETS_PREF = "pref_purgeable_assets";

    private static final String PURGEABLE_ASSETS_PERSIST_PROP = "persist.sys.purgeable_assets";

    private static final String PURGEABLE_ASSETS_DEFAULT = "0";

    private CheckBoxPreference mPurgeableAssetsPref;

    private CheckBoxPreference mKSMPref;

    private int swapAvailable = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.memory_management);

        PreferenceScreen prefSet = getPreferenceScreen();

        mPurgeableAssetsPref = (CheckBoxPreference) prefSet.findPreference(PURGEABLE_ASSETS_PREF);
        mKSMPref = (CheckBoxPreference) prefSet.findPreference(KSM_PREF);

        if (Utils.fileExists(KSM_RUN_FILE)) {
            mKSMPref.setChecked(KSM_PREF_ENABLED.equals(Utils.fileReadOneLine(KSM_RUN_FILE)));
        } else {
            prefSet.removePreference(mKSMPref);
        }

        String purgeableAssets = SystemProperties.get(PURGEABLE_ASSETS_PERSIST_PROP,
                PURGEABLE_ASSETS_DEFAULT);
        mPurgeableAssetsPref.setChecked("1".equals(purgeableAssets));
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        if (preference == mPurgeableAssetsPref) {
            SystemProperties.set(PURGEABLE_ASSETS_PERSIST_PROP,
                    mPurgeableAssetsPref.isChecked() ? "1" : "0");
            return true;
        }

        if (preference == mKSMPref) {
            Utils.fileWriteOneLine(KSM_RUN_FILE, mKSMPref.isChecked() ? "1" : "0");
            return true;
        }

        return false;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }
}
