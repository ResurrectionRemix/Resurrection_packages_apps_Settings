/*
 * Copyright (C) 2012 ParanoidAndroid Project
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

package com.android.settings.paranoid;

import android.content.Context;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

/**
 * Lockscreen Settings
 */
public class Lockscreen extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    public static final String KEY_ALLOW_ROTATION = "allow_rotation";
    public static final String KEY_SEE_TRHOUGH = "see_through";

    private CheckBoxPreference mAllowRotation;
    private CheckBoxPreference mSeeThrough;

    private Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.lock_screen_settings);
        PreferenceScreen prefSet = getPreferenceScreen();
        mContext = getActivity();

        mAllowRotation = (CheckBoxPreference) prefSet.findPreference(KEY_ALLOW_ROTATION);
        mAllowRotation.setChecked(Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.LOCKSCREEN_ALLOW_ROTATION, 0) == 1);

        mSeeThrough = (CheckBoxPreference) prefSet.findPreference(KEY_SEE_TRHOUGH);
        mSeeThrough.setChecked(Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.LOCKSCREEN_SEE_THROUGH, 0) == 1);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mAllowRotation) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.LOCKSCREEN_ALLOW_ROTATION, mAllowRotation.isChecked()
                    ? 1 : 0);
        } else if (preference == mSeeThrough) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.LOCKSCREEN_SEE_THROUGH, mSeeThrough.isChecked()
                    ? 1 : 0);
        } 
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return true;
    }
}
