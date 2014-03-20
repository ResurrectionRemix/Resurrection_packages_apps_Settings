/*
 * Spawn.java
 * 
 * Copyright 2014 westcrip <westcrip@westcrip-altankrk>
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 * 
 * 
 */


package com.android.settings;

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


import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.settings.util.Helpers;

public class Spawn extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "Spawn";
								
    private Context mContext;

    private static final String BATTERY_AROUND_LOCKSCREEN_RING = "battery_around_lockscreen_ring";
    private static final String CATEGORY_HEADSETHOOK = "button_headsethook";
    private static final String BUTTON_HEADSETHOOK_LAUNCH_VOICE = "button_headsethook_launch_voice";
    private static final String STATUS_BAR_CUSTOM_HEADER = "custom_status_bar_header";
    private static final String PREF_NOTIFICATION_SHOW_WIFI_SSID = "notification_show_wifi_ssid";
    private static final String PREF_DISABLE_FC_NOTIFICATIONS = "disable_fc_notifications";
    
    private CheckBoxPreference mLockRingBattery;
    private CheckBoxPreference mHeadsetHookLaunchVoice;
    private CheckBoxPreference mStatusBarCustomHeader;
    private CheckBoxPreference mShowWifiName;
    CheckBoxPreference mDisableFC;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.spawn_settings);

        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();
        mContext = getActivity();
        
        mDisableFC = (CheckBoxPreference) findPreference(PREF_DISABLE_FC_NOTIFICATIONS);
        mDisableFC.setChecked(Settings.System.getBoolean(mContext.getContentResolver(),
                Settings.System.DISABLE_FC_NOTIFICATIONS, true));
                        
        mShowWifiName = (CheckBoxPreference) findPreference(PREF_NOTIFICATION_SHOW_WIFI_SSID);
        mShowWifiName.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.NOTIFICATION_SHOW_WIFI_SSID, 0) == 1);
        mShowWifiName.setOnPreferenceChangeListener(this);
                
        mStatusBarCustomHeader = (CheckBoxPreference) prefSet.findPreference(STATUS_BAR_CUSTOM_HEADER);
        mStatusBarCustomHeader.setChecked(Settings.System.getInt(resolver,
            Settings.System.STATUS_BAR_CUSTOM_HEADER, 0) == 1);
        mStatusBarCustomHeader.setOnPreferenceChangeListener(this);

            mLockRingBattery = (CheckBoxPreference) findPreference(BATTERY_AROUND_LOCKSCREEN_RING);
       if (mLockRingBattery != null) {
            mLockRingBattery.setChecked(Settings.System.getInt(getContentResolver(),
                     Settings.System.BATTERY_AROUND_LOCKSCREEN_RING, 0) == 1);
       }   
         
        mHeadsetHookLaunchVoice = (CheckBoxPreference) findPreference(BUTTON_HEADSETHOOK_LAUNCH_VOICE);
        mHeadsetHookLaunchVoice.setChecked(Settings.System.getInt(resolver,
                Settings.System.HEADSETHOOK_LAUNCH_VOICE, 1) == 1);   
    
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
                boolean value;
        if (preference == mLockRingBattery) {
             Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.BATTERY_AROUND_LOCKSCREEN_RING, mLockRingBattery.isChecked() ? 1 : 0);
        } else if (preference == mHeadsetHookLaunchVoice) {
            boolean checked = ((CheckBoxPreference)preference).isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.HEADSETHOOK_LAUNCH_VOICE, checked ? 1:0);
        } else if (preference == mDisableFC) {
            Settings.System.putBoolean(mContext.getContentResolver(),
                    Settings.System.DISABLE_FC_NOTIFICATIONS,
                    ((CheckBoxPreference) preference).isChecked());           
            }  else {
              // If not handled, let preferences handle it.
              return super.onPreferenceTreeClick(preferenceScreen, preference);
         }
        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
    final String key = preference.getKey();
        ContentResolver resolver = getActivity().getContentResolver();
       if (preference == mStatusBarCustomHeader) {
            boolean value = (Boolean) objValue;
             Settings.System.putInt(resolver,
                 Settings.System.STATUS_BAR_CUSTOM_HEADER, value ? 1 : 0);
            Helpers.restartSystemUI();
	        } else if (preference == mShowWifiName) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(getContentResolver(),
                Settings.System.NOTIFICATION_SHOW_WIFI_SSID, value ? 1 : 0);
            return true;
        } else {
            return false;
        }

        return true;
    }
}

