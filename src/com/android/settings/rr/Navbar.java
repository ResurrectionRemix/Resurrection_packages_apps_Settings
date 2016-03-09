/*

 * Copyright (C) 2014 Screw'd Android
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

import android.provider.SearchIndexableResource;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.SystemProperties;
import android.provider.Settings;
import android.preference.ListPreference;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.PreferenceCategory;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SlimSeekBarPreference;
import android.preference.SwitchPreference;

import com.android.internal.utils.du.ActionConstants;
import com.android.internal.utils.du.Config;
import com.android.internal.utils.du.DUActionUtils;
import com.android.settings.cyanogenmod.SecureSettingSwitchPreference;
import com.android.internal.utils.du.Config.ButtonConfig;
import android.text.TextUtils;
import android.widget.Toast;


import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.internal.util.slim.Action;
import java.util.ArrayList;
import java.util.List;

import com.android.internal.logging.MetricsLogger;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class Navbar extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {
	
	private static final String NAVBAR_VISIBILITY = "navbar_visibility";
    private static final String KEY_NAVBAR_MODE = "navbar_mode";
    private static final String KEY_AOSP_NAVBAR_SETTINGS = "aosp_navbar_settings";
    private static final String KEY_FLING_NAVBAR_SETTINGS = "fling_settings";
    private static final String KEY_CATEGORY_NAVIGATION_INTERFACE = "category_navbar_interface";
    private static final String KEY_CATEGORY_NAVIGATION_GENERAL = "category_navbar_general";
    private static final String KEY_SMARTBAR_SETTINGS = "smartbar_settings";
    private static final String KEY_NAVIGATION_BAR_SIZE = "navigation_bar_size";

    private SwitchPreference mNavbarVisibility;
    private ListPreference mNavbarMode;
    private PreferenceScreen mFlingSettings;
    private PreferenceCategory mNavInterface;
    private PreferenceCategory mNavGeneral;
    private PreferenceScreen mSmartbarSettings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.rr_navbar_settings);
		
		Activity activity = getActivity();
		
		ContentResolver resolver = getActivity().getContentResolver();
		PreferenceScreen prefSet = getPreferenceScreen();
		
		mNavInterface = (PreferenceCategory) findPreference(KEY_CATEGORY_NAVIGATION_INTERFACE);
        mNavGeneral = (PreferenceCategory) findPreference(KEY_CATEGORY_NAVIGATION_GENERAL);
        mNavbarVisibility = (SwitchPreference) findPreference(NAVBAR_VISIBILITY);
        mNavbarMode = (ListPreference) findPreference(KEY_NAVBAR_MODE);
        mFlingSettings = (PreferenceScreen) findPreference(KEY_FLING_NAVBAR_SETTINGS);
        mSmartbarSettings = (PreferenceScreen) findPreference(KEY_SMARTBAR_SETTINGS);

        boolean showing = Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.NAVIGATION_BAR_VISIBLE,
                DUActionUtils.hasNavbarByDefault(getActivity()) ? 1 : 0) != 0;
        updateBarVisibleAndUpdatePrefs(showing);
        mNavbarVisibility.setOnPreferenceChangeListener(this);

        int mode = Settings.Secure.getInt(getContentResolver(), Settings.Secure.NAVIGATION_BAR_MODE,
                0);

        // Smartbar moved from 2 to 0, deprecating old navbar
        if (mode == 2) {
            mode = 0;
        }
        updateBarModeSettings(mode);
        mNavbarMode.setOnPreferenceChangeListener(this);

			

    }
	

    private void updateBarModeSettings(int mode) {
        mNavbarMode.setValue(String.valueOf(mode));
        mSmartbarSettings.setEnabled(mode == 0);
        mSmartbarSettings.setSelectable(mode == 0);
        mFlingSettings.setEnabled(mode == 1);
        mFlingSettings.setSelectable(mode == 1);
    }

    private void updateBarVisibleAndUpdatePrefs(boolean showing) {
        mNavbarVisibility.setChecked(showing);
        mNavInterface.setEnabled(mNavbarVisibility.isChecked());
        mNavGeneral.setEnabled(mNavbarVisibility.isChecked());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (preference.equals(mNavbarMode)) {
            int mode = Integer.parseInt(((String) newValue).toString());
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.NAVIGATION_BAR_MODE, mode);
            updateBarModeSettings(mode);
            return true;
        } else if (preference.equals(mNavbarVisibility)) {
            boolean showing = ((Boolean)newValue);
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.NAVIGATION_BAR_VISIBLE,
                    showing ? 1 : 0);
            updateBarVisibleAndUpdatePrefs(showing);
            return true;
        }
        return false;
    }		
	@Override
	protected int getMetricsCategory() {
        return MetricsLogger.APPLICATION;
    }

}
