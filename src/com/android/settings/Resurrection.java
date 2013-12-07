/*
 * Resurrection Remix ROM Settings
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

package com.android.settings;

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

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class Resurrection extends SettingsPreferenceFragment implements
    Preference.OnPreferenceChangeListener {
    private static final String TAG = "Resurrection";
  
    private CheckBoxPreference mSeeThrough;
    private CheckBoxPreference mQuickUnlockScreen;
    private CheckBoxPreference mLockRingBattery;
    private CheckBoxPreference mStatusBarTraffic;
    private CheckBoxPreference mStatusBarNotifCount;
    
    private static final String STATUS_BAR_NOTIF_COUNT = "status_bar_notif_count";
    private static final String KEY_LOCK_CLOCK = "lock_clock";
    private static final String KEY_SEE_TRHOUGH = "see_through";
    private static final String LOCKSCREEN_QUICK_UNLOCK_CONTROL = "lockscreen_quick_unlock_control";
    private static final String BATTERY_AROUND_LOCKSCREEN_RING = "battery_around_lockscreen_ring";
    private static final String STATUS_BAR_TRAFFIC = "status_bar_traffic";
    
    private final Configuration mCurConfig = new Configuration();
    private Context mContext;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.resurrection_settings);
       PreferenceScreen prefSet = getPreferenceScreen();
       mContext = getActivity();
       
                
        mQuickUnlockScreen = (CheckBoxPreference) findPreference(LOCKSCREEN_QUICK_UNLOCK_CONTROL);
        if (mQuickUnlockScreen  != null) {
            mQuickUnlockScreen.setChecked(Settings.System.getInt(getContentResolver(), 
                    Settings.System.LOCKSCREEN_QUICK_UNLOCK_CONTROL, 0) == 1);
            mQuickUnlockScreen.setOnPreferenceChangeListener(this);
}
              mLockRingBattery = (CheckBoxPreference) findPreference(BATTERY_AROUND_LOCKSCREEN_RING);
        if (mLockRingBattery != null) {
            mLockRingBattery.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.BATTERY_AROUND_LOCKSCREEN_RING, 0) == 1);
}
        mSeeThrough = (CheckBoxPreference) prefSet.findPreference(KEY_SEE_TRHOUGH);
        
        mStatusBarTraffic = (CheckBoxPreference) prefSet.findPreference(STATUS_BAR_TRAFFIC);
        mStatusBarTraffic.setChecked(Settings.System.getInt(getContentResolver(),
            Settings.System.STATUS_BAR_TRAFFIC, 0) == 1);
        mStatusBarTraffic.setOnPreferenceChangeListener(this);
        
	    mStatusBarNotifCount = (CheckBoxPreference) prefSet.findPreference(STATUS_BAR_NOTIF_COUNT);
        mStatusBarNotifCount.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.STATUS_BAR_NOTIF_COUNT, 0) == 1);
        mStatusBarNotifCount.setOnPreferenceChangeListener(this);

    }

    @Override
    public void onResume() {
        super.onResume();
    }
    
    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
                boolean value;
          if (preference == mSeeThrough) {
                    Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.LOCKSCREEN_SEE_THROUGH, mSeeThrough.isChecked() ? 1 : 0);
        } else if (preference == mLockRingBattery) {
                    Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.BATTERY_AROUND_LOCKSCREEN_RING, mLockRingBattery.isChecked() ? 1 : 0);
        } else if (preference == mQuickUnlockScreen) {
                    Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LOCKSCREEN_QUICK_UNLOCK_CONTROL, mQuickUnlockScreen.isChecked() ? 1 : 0);
            }  else {
         }
         return true; 
    }
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
	  if (preference == mStatusBarTraffic) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(getContentResolver(),
                Settings.System.STATUS_BAR_TRAFFIC, value ? 1 : 0);
	        } else if (preference == mStatusBarNotifCount) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(getContentResolver(), Settings.System.STATUS_BAR_NOTIF_COUNT, value ? 1 : 0);
            }  else {
	            return false;
        }
			        return true;
    }
}
