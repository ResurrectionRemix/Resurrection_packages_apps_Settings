/*   Copyright (C) 2012 The CyanogenMod Project
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.android.settings.inputmethod;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.Collections;
import java.util.List;

public class StylusGestures extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    public static final String TAG = "Stylus Gestures";
    public static final String KEY_SPEN_LEFT = "gestures_left";
    public static final String KEY_SPEN_RIGHT = "gestures_right";
    public static final String KEY_SPEN_UP = "gestures_up";
    public static final String KEY_SPEN_DOWN = "gestures_down";
    public static final String KEY_SPEN_LONG = "gestures_long";
    public static final String KEY_SPEN_DOUBLE = "gestures_double";
    public static final int KEY_NO_ACTION = 1000;
    public static final String TEXT_NO_ACTION = "No Action";

    private ListPreference mSwipeLeft;
    private ListPreference mSwipeRight;
    private ListPreference mSwipeUp;
    private ListPreference mSwipeDown;
    private ListPreference mSwipeLong;
    private ListPreference mSwipeDouble;

    private Context mContext;
    private ContentResolver mResolver;

    private String[] mActionNames;
    private String[] mActionValues;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.gestures_prefs);
        mContext = getActivity();
        mResolver = getContentResolver();

        Resources resources = mContext.getResources();
        mActionNames = resources.getStringArray(R.array.gestures_entries);
        mActionValues = resources.getStringArray(R.array.gestures_values);

        // Setup the gestures
        mSwipeLeft   = setupGesturePref(KEY_SPEN_LEFT,   Settings.System.GESTURES_LEFT_SWIPE);
        mSwipeRight  = setupGesturePref(KEY_SPEN_RIGHT,  Settings.System.GESTURES_RIGHT_SWIPE);
        mSwipeUp     = setupGesturePref(KEY_SPEN_UP,     Settings.System.GESTURES_UP_SWIPE);
        mSwipeDown   = setupGesturePref(KEY_SPEN_DOWN,   Settings.System.GESTURES_DOWN_SWIPE);
        mSwipeLong   = setupGesturePref(KEY_SPEN_LONG,   Settings.System.GESTURES_LONG_PRESS);
        mSwipeDouble = setupGesturePref(KEY_SPEN_DOUBLE, Settings.System.GESTURES_DOUBLE_TAP);
    }

    private ListPreference setupGesturePref(String key, String settingName) {
        ListPreference pref = (ListPreference) findPreference(key);
        String setting = Settings.System.getString(mResolver, settingName);
        addApplicationEntries(pref, setting);
        pref.setOnPreferenceChangeListener(this);
        return pref;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String settingName = null;

        if (preference == mSwipeLeft) {
            settingName = Settings.System.GESTURES_LEFT_SWIPE;
        } else if (preference == mSwipeRight) {
            settingName = Settings.System.GESTURES_RIGHT_SWIPE;
        } else if (preference == mSwipeUp) {
            settingName = Settings.System.GESTURES_UP_SWIPE;
        } else if (preference == mSwipeDown) {
            settingName = Settings.System.GESTURES_DOWN_SWIPE;
        } else if (preference == mSwipeLong) {
            settingName = Settings.System.GESTURES_LONG_PRESS;
        } else if (preference == mSwipeDouble) {
            settingName = Settings.System.GESTURES_DOUBLE_TAP;
        } else {
            return false;
        }

        String packageName = newValue.toString();
        Settings.System.putString(mResolver, settingName, packageName);
        setPrefValue((ListPreference)preference, packageName);
        return true;
    }

    private String mapUpdateValue(String time) {
        for (int i = 0; i < mActionValues.length; i++) {
            if (mActionValues[i].equalsIgnoreCase(time)) {
                return mActionNames[i];
            }
        }
        return null;
    }

    private void setPrefValue(ListPreference pref, String packageName) {
        if (packageName == null) {
            packageName = String.valueOf(KEY_NO_ACTION);
        }

        String text = mapUpdateValue(packageName);
        if (text != null) {
            pref.setValue(packageName);
            pref.setSummary(text);
        } else {
            CharSequence appName = getAppName(packageName);
            if (appName != null) {
                pref.setValue(packageName);
                pref.setSummary(appName);
            } else {
                pref.setSummary(mContext.getString(R.string.stylus_app_not_installed,
                    packageName));
            }
        }

    }

    private void addApplicationEntries(ListPreference pref, String packageName) {
        PackageManager pm = getPackageManager();

        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> list = pm.queryIntentActivities(intent,
                PackageManager.PERMISSION_GRANTED);

        Collections.sort(list, new ResolveInfo.DisplayNameComparator(pm));

        int count = list.size() + mActionValues.length;
        CharSequence[] entries = new CharSequence[count];
        CharSequence[] values = new CharSequence[count];

        // Step 1: copy in predefined actions
        for (int i = 0; i < mActionValues.length; i++) {
            entries[i] = mActionNames[i];
            values[i] = mActionValues[i];
        }

        // Step 2: copy in resolved activities
        for (int i = mActionValues.length; i < count; i++) {
            ResolveInfo info = list.get(i - mActionValues.length);
            CharSequence label = info.loadLabel(pm);
            if (label == null) {
                label = info.activityInfo.name;
            }

            entries[i] = label;
            values[i] = info.activityInfo.applicationInfo.packageName;
        }

        pref.setEntries(entries);
        pref.setEntryValues(values);
        setPrefValue(pref, packageName);
    }

    private CharSequence getAppName(String packageName) {
        final PackageManager pm = mContext.getPackageManager();
        ApplicationInfo ai;

        try {
            ai = pm.getApplicationInfo(packageName, 0);
        } catch (final NameNotFoundException e) {
            ai = null;
        }

        if (ai != null) {
            return pm.getApplicationLabel(ai);
        }
        return null;
    }
}
