/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.app.ActivityManagerNative;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.Camera;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.preference.RingtonePreference;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManagerGlobal;

import java.util.ArrayList;

import com.android.settings.R;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.SeekBarPreference;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.preference.AppMultiSelectListPreference;

import java.io.File;
import java.lang.Thread;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays;

public class AppCircleBar extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, OnPreferenceClickListener {

    private static final String TAG = "AppCircleSidebar";

    private static final String PREF_INCLUDE_APP_CIRCLE_BAR_KEY = "app_circle_bar_included_apps";
    private static final String KEY_TRIGGER_WIDTH = "trigger_width";
    private static final String KEY_TRIGGER_TOP = "trigger_top";
    private static final String KEY_TRIGGER_BOTTOM = "trigger_bottom";

    private AppMultiSelectListPreference mIncludedAppCircleBar;

    private SeekBarPreference mTriggerWidthPref;
    private SeekBarPreference mTriggerTopPref;
    private SeekBarPreference mTriggerBottomPref;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentResolver resolver = getActivity().getContentResolver();
        Resources res = getResources();

        addPreferencesFromResource(R.xml.app_circlebar);

        mIncludedAppCircleBar = (AppMultiSelectListPreference) findPreference(PREF_INCLUDE_APP_CIRCLE_BAR_KEY);
        Set<String> includedApps = getIncludedApps();
        if (includedApps != null) mIncludedAppCircleBar.setValues(includedApps);
        mIncludedAppCircleBar.setOnPreferenceChangeListener(this);

        mTriggerWidthPref = (SeekBarPreference) findPreference(KEY_TRIGGER_WIDTH);
        mTriggerWidthPref.setOnPreferenceChangeListener(this);

        mTriggerTopPref = (SeekBarPreference) findPreference(KEY_TRIGGER_TOP);
        mTriggerTopPref.setOnPreferenceChangeListener(this);

        mTriggerBottomPref = (SeekBarPreference) findPreference(KEY_TRIGGER_BOTTOM);
        mTriggerBottomPref.setOnPreferenceChangeListener(this);

    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }
    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        final String key = preference.getKey();
        if (preference == mIncludedAppCircleBar) {
            storeIncludedApps((Set<String>) newValue);
        } else if (preference == mTriggerWidthPref) {
            int width = ((Integer)newValue).intValue();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.APP_CIRCLE_BAR_TRIGGER_WIDTH, width);
            return true;
        } else if (preference == mTriggerTopPref) {
            int top = ((Integer)newValue).intValue();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.APP_CIRCLE_BAR_TRIGGER_TOP, top);
            return true;
        } else if (preference == mTriggerBottomPref) {
            int bottom = ((Integer)newValue).intValue();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.APP_CIRCLE_BAR_TRIGGER_HEIGHT, bottom);
            return true;
        }

        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return false;
    }

    private Set<String> getIncludedApps() {
        String included = Settings.System.getString(getActivity().getContentResolver(),
                Settings.System.WHITELIST_APP_CIRCLE_BAR);
        if (TextUtils.isEmpty(included)) {
            return null;
        }
        return new HashSet<String>(Arrays.asList(included.split("\\|")));
    }

    private void storeIncludedApps(Set<String> values) {
        StringBuilder builder = new StringBuilder();
        String delimiter = "";
        for (String value : values) {
            builder.append(delimiter);
            builder.append(value);
            delimiter = "|";
        }
        Settings.System.putString(getActivity().getContentResolver(),
                Settings.System.WHITELIST_APP_CIRCLE_BAR, builder.toString());
    }

    @Override
    public void onPause() {
        super.onPause();
        Settings.System.putInt(getContentResolver(),
                Settings.System.APP_CIRCLE_BAR_SHOW_TRIGGER, 0);
    }

    @Override
    public void onResume() {
        super.onResume();
        Settings.System.putInt(getContentResolver(),
                Settings.System.APP_CIRCLE_BAR_SHOW_TRIGGER, 1);
    }
}
