/*
 *<!-- Copyright (C) 2012-2014 Resurrection Remix ROM Project
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
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.UserHandle;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.preference.PreferenceScreen;
import android.preference.SeekBarPreference;
import com.android.settings.SettingsPreferenceFragment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.Display;
import android.view.Window;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.util.Helpers;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.cyanogenmod.SystemSettingSwitchPreference;


public class RecentsSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "RecentsSettings";
    private static final String SHOW_CLEAR_ALL_RECENTS = "show_clear_all_recents";
    private static final String RECENTS_CLEAR_ALL_LOCATION = "recents_clear_all_location";
    private SwitchPreference mRecentsClearAll;
    private ListPreference mRecentsClearAllLocation;

    private static final String KEY_OMNISWITCH = "omniswitch";
    public static final String OMNISWITCH_PACKAGE_NAME = "org.omnirom.omniswitch";


    private Context mContext;

    private Preference mOmniSwitch;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rr_recents_settings);

        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getContentResolver();
	PackageManager pm = getPackageManager();

        mContext = getActivity();

        mRecentsClearAll = (SwitchPreference) prefSet.findPreference(SHOW_CLEAR_ALL_RECENTS);
        mRecentsClearAll.setChecked(Settings.System.getIntForUser(resolver,
            Settings.System.SHOW_CLEAR_ALL_RECENTS, 1, UserHandle.USER_CURRENT) == 1);
        mRecentsClearAll.setOnPreferenceChangeListener(this);

        mRecentsClearAllLocation = (ListPreference) prefSet.findPreference(RECENTS_CLEAR_ALL_LOCATION);
        int location = Settings.System.getIntForUser(resolver,
                Settings.System.RECENTS_CLEAR_ALL_LOCATION, 0, UserHandle.USER_CURRENT);
        mRecentsClearAllLocation.setValue(String.valueOf(location));
        mRecentsClearAllLocation.setOnPreferenceChangeListener(this);
        updateRecentsLocation(location);

        mOmniSwitch = (Preference)
                prefSet.findPreference(KEY_OMNISWITCH);
        if (!Helpers.isPackageInstalled(OMNISWITCH_PACKAGE_NAME, pm)) {
            prefSet.removePreference(mOmniSwitch);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
       ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mRecentsClearAll) {
            boolean show = (Boolean) newValue;
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    Settings.System.SHOW_CLEAR_ALL_RECENTS, show ? 1 : 0, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mRecentsClearAllLocation) {
            int location = Integer.valueOf((String) newValue);
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    Settings.System.RECENTS_CLEAR_ALL_LOCATION, location, UserHandle.USER_CURRENT);
            updateRecentsLocation(location);
		return true;
	}
	return false;
     }

    private void updateRecentsLocation(int value) {
        ContentResolver resolver = getContentResolver();
        Resources res = getResources();
        int summary = -5;

        Settings.System.putInt(resolver, Settings.System.RECENTS_CLEAR_ALL_LOCATION, value);

        if (value == 0) {
            Settings.System.putInt(resolver, Settings.System.RECENTS_CLEAR_ALL_LOCATION, 0);
            summary = R.string.recents_clear_all_location_top_right;
        } else if (value == 1) {
            Settings.System.putInt(resolver, Settings.System.RECENTS_CLEAR_ALL_LOCATION, 1);
            summary = R.string.recents_clear_all_location_top_left;
        } else if (value == 2) {
            Settings.System.putInt(resolver, Settings.System.RECENTS_CLEAR_ALL_LOCATION, 2);
            summary = R.string.recents_clear_all_location_top_center;
        } else if (value == 3) {
            Settings.System.putInt(resolver, Settings.System.RECENTS_CLEAR_ALL_LOCATION, 3);
            summary = R.string.recents_clear_all_location_bottom_right;
        } else if (value == 4) {
            Settings.System.putInt(resolver, Settings.System.RECENTS_CLEAR_ALL_LOCATION, 4);
            summary = R.string.recents_clear_all_location_bottom_left;
        } else if (value == 5) {
            Settings.System.putInt(resolver, Settings.System.RECENTS_CLEAR_ALL_LOCATION, 5);
            summary = R.string.recents_clear_all_location_bottom_center;
        }
        if (mRecentsClearAllLocation != null && summary != -5) {
            mRecentsClearAllLocation.setSummary(res.getString(summary));

    }

}

 }
