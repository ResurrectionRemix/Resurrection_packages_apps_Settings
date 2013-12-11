/*
 *  Copyright (C) 2013 The Resurrection Remix Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.android.settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.IWindowManager;
import android.view.Display;
import android.view.Window;
import android.widget.Toast;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;
import java.io.IOException;

import java.util.List;

public class Spawn extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "Spawn";
								
    private Context mContext;

    private static final String STATUS_BAR_NOTIF_COUNT = "status_bar_notif_count";
    private static final String KEY_SEE_TRHOUGH = "see_through";
    private static final String STATUS_BAR_TRAFFIC = "status_bar_traffic";
    private static final String BATTERY_AROUND_LOCKSCREEN_RING = "battery_around_lockscreen_ring";

    private CheckBoxPreference mStatusBarNotifCount;
    private CheckBoxPreference mSeeThrough;
    private CheckBoxPreference mStatusBarTraffic;
    private CheckBoxPreference mLockRingBattery;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.spawn_settings);

        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();
        mContext = getActivity();

        mSeeThrough = (CheckBoxPreference) prefSet.findPreference(KEY_SEE_TRHOUGH);

        mStatusBarNotifCount = (CheckBoxPreference) prefSet.findPreference(STATUS_BAR_NOTIF_COUNT);
        mStatusBarNotifCount.setChecked(Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_NOTIF_COUNT, 0) == 1);
        mStatusBarNotifCount.setOnPreferenceChangeListener(this);

	mStatusBarTraffic = (CheckBoxPreference) prefSet.findPreference(STATUS_BAR_TRAFFIC);
        mStatusBarTraffic.setChecked(Settings.System.getInt(resolver,
            Settings.System.STATUS_BAR_TRAFFIC, 0) == 1);
        mStatusBarTraffic.setOnPreferenceChangeListener(this);

            mLockRingBattery = (CheckBoxPreference) findPreference(BATTERY_AROUND_LOCKSCREEN_RING);
       if (mLockRingBattery != null) {
            mLockRingBattery.setChecked(Settings.System.getInt(getContentResolver(),
                     Settings.System.BATTERY_AROUND_LOCKSCREEN_RING, 0) == 1);
       }   
    
    
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
                boolean value;
        if (preference == mSeeThrough) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.LOCKSCREEN_SEE_THROUGH, mSeeThrough.isChecked()
                    ? 1 : 0);
        } else if (preference == mLockRingBattery) {
             Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.BATTERY_AROUND_LOCKSCREEN_RING, mLockRingBattery.isChecked() ? 1 : 0);                    
            }  else {
              // If not handled, let preferences handle it.
              return super.onPreferenceTreeClick(preferenceScreen, preference);
         }
        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
    final String key = preference.getKey();
        ContentResolver resolver = getActivity().getContentResolver();
           if (preference == mStatusBarNotifCount) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(resolver, Settings.System.STATUS_BAR_NOTIF_COUNT, value ? 1 : 0);
        } else if (preference == mStatusBarTraffic) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(resolver,
                Settings.System.STATUS_BAR_TRAFFIC, value ? 1 : 0);
        } else {
            return false;
        }

        return true;
    }
}

