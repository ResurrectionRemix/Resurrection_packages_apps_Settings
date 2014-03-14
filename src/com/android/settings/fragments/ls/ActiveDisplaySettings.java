/*
 * Copyright (C) 2013 The ChameleonOS Project
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

package com.android.settings.fragments.ls;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.format.DateFormat;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.chameleonos.AppMultiSelectListPreference;
import com.android.settings.chameleonos.SeekBarPreference;

import com.android.internal.util.cm.DeviceUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ActiveDisplaySettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {
    private static final String TAG = "ActiveDisplaySettings";

    private static final String KEY_ENABLED = "ad_enable";
    private static final String KEY_ANNOYING = "ad_annoying";
    private static final String KEY_BYPASS_CONTENT = "ad_bypass";
    private static final String KEY_POCKET_MODE = "ad_pocket_mode";
    private static final String KEY_SUNLIGHT_MODE = "ad_sunlight_mode";
    private static final String KEY_REDISPLAY = "ad_redisplay";
    private static final String KEY_EXCLUDED_APPS = "ad_excluded_apps";
    private static final String KEY_PRIVACY_APPS = "ad_privacy_apps";
    private static final String KEY_SHOW_AMPM = "ad_show_ampm";
    private static final String KEY_BRIGHTNESS = "ad_brightness";
    private static final String KEY_TIMEOUT = "ad_timeout";
    private static final String KEY_THRESHOLD = "ad_threshold";
    private static final String KEY_TURNOFF_MODE = "ad_turnoff_mode";
    private static final String KEY_SHAKE_THRESHOLD = "ad_shake_threshold";
    private static final String KEY_SHAKE_LONGTHRESHOLD = "ad_shake_long_threshold";
    private static final String KEY_SHAKE_TIMEOUT = "ad_shake_timeout";

    private ContentResolver mResolver;
    private Context mContext;

    private SwitchPreference mEnabledPref;
    private AppMultiSelectListPreference mExcludedAppsPref;
    private AppMultiSelectListPreference mPrivacyAppsPref;
    private CheckBoxPreference mBypassPref;
    private CheckBoxPreference mShowAmPmPref;
    private CheckBoxPreference mSunlightModePref;
    private CheckBoxPreference mTurnOffModePref;
    private SeekBarPreference mBrightnessLevel;
    private SeekBarPreference mAnnoyingNotification;
    private SeekBarPreference mShakeThreshold;
    private SeekBarPreference mShakeLongThreshold;
    private SeekBarPreference mShakeTimeout;
    private ListPreference mDisplayTimeout;
    private ListPreference mPocketModePref;
    private ListPreference mProximityThreshold;
    private ListPreference mRedisplayPref;
    private int mMinimumBacklight;
    private int mMaximumBacklight;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.active_display_settings);

        mContext = getActivity().getApplicationContext();
        mResolver = mContext.getContentResolver();
        PreferenceScreen prefSet = getPreferenceScreen();

        mEnabledPref = (SwitchPreference) prefSet.findPreference(KEY_ENABLED);
        mEnabledPref.setChecked((Settings.System.getInt(mResolver,
                Settings.System.ENABLE_ACTIVE_DISPLAY, 0) == 1));
        mEnabledPref.setOnPreferenceChangeListener(this);

        mBypassPref = (CheckBoxPreference) prefSet.findPreference(KEY_BYPASS_CONTENT);
        mPocketModePref = (ListPreference) prefSet.findPreference(KEY_POCKET_MODE);
        mProximityThreshold = (ListPreference) prefSet.findPreference(KEY_THRESHOLD);
        mTurnOffModePref = (CheckBoxPreference) prefSet.findPreference(KEY_TURNOFF_MODE);

        if (!DeviceUtils.deviceSupportsProximitySensor(mContext)) {
            prefSet.removePreference(mPocketModePref);
            prefSet.removePreference(mBypassPref);
            prefSet.removePreference(mProximityThreshold);
            prefSet.removePreference(mTurnOffModePref);
        } else {
            mBypassPref.setChecked((Settings.System.getInt(mResolver,
                Settings.System.ACTIVE_DISPLAY_BYPASS, 1) != 0));

            int mode = Settings.System.getInt(mResolver,
                    Settings.System.ACTIVE_DISPLAY_POCKET_MODE, 0);
            mPocketModePref.setValue(String.valueOf(mode));
            mPocketModePref.setSummary(mPocketModePref.getEntry());
            mPocketModePref.setOnPreferenceChangeListener(this);

            long threshold = Settings.System.getLong(mResolver,
                Settings.System.ACTIVE_DISPLAY_THRESHOLD, 5000L);
            mProximityThreshold.setValue(String.valueOf(threshold));
            mProximityThreshold.setSummary(mProximityThreshold.getEntry());
            mProximityThreshold.setOnPreferenceChangeListener(this);

            mTurnOffModePref.setChecked((Settings.System.getInt(mResolver,
                Settings.System.ACTIVE_DISPLAY_TURNOFF_MODE, 0) == 1));
        }

        mSunlightModePref = (CheckBoxPreference) prefSet.findPreference(KEY_SUNLIGHT_MODE);

        if (!DeviceUtils.deviceSupportsLightSensor(mContext)) {
            prefSet.removePreference(mSunlightModePref);
        } else {
            mSunlightModePref.setChecked((Settings.System.getInt(mResolver,
                Settings.System.ACTIVE_DISPLAY_SUNLIGHT_MODE, 0) == 1));
        }

        mRedisplayPref = (ListPreference) prefSet.findPreference(KEY_REDISPLAY);
        long timeout = Settings.System.getLong(mResolver,
                Settings.System.ACTIVE_DISPLAY_REDISPLAY, 0);
        mRedisplayPref.setValue(String.valueOf(timeout));
        mRedisplayPref.setSummary(mRedisplayPref.getEntry());
        mRedisplayPref.setOnPreferenceChangeListener(this);

        mAnnoyingNotification = (SeekBarPreference) prefSet.findPreference(KEY_ANNOYING);
        mAnnoyingNotification.setValue(Settings.System.getInt(mResolver,
                Settings.System.ACTIVE_DISPLAY_ANNOYING, 0));
        mAnnoyingNotification.setOnPreferenceChangeListener(this);

        mShakeThreshold = (SeekBarPreference) prefSet.findPreference(KEY_SHAKE_THRESHOLD);
        mShakeThreshold.setValue(Settings.System.getInt(mResolver,
                Settings.System.ACTIVE_DISPLAY_SHAKE_THRESHOLD, 10));
        mShakeThreshold.setOnPreferenceChangeListener(this);

        mShakeLongThreshold = (SeekBarPreference) prefSet.findPreference(KEY_SHAKE_LONGTHRESHOLD);
        mShakeLongThreshold.setValue(Settings.System.getInt(mResolver,
                Settings.System.ACTIVE_DISPLAY_SHAKE_LONGTHRESHOLD, 2));
        mShakeLongThreshold.setOnPreferenceChangeListener(this);

        mShakeTimeout = (SeekBarPreference) prefSet.findPreference(KEY_SHAKE_TIMEOUT);
        mShakeTimeout.setValue(Settings.System.getInt(mResolver,
                Settings.System.ACTIVE_DISPLAY_SHAKE_TIMEOUT, 10));
        mShakeTimeout.setOnPreferenceChangeListener(this);

        mExcludedAppsPref = (AppMultiSelectListPreference) prefSet.findPreference(KEY_EXCLUDED_APPS);
        Set<String> excludedApps = getExcludedApps();
        if (excludedApps != null) {
            mExcludedAppsPref.setValues(excludedApps);
        }
        mExcludedAppsPref.setOnPreferenceChangeListener(this);

        mPrivacyAppsPref = (AppMultiSelectListPreference) prefSet.findPreference(KEY_PRIVACY_APPS);
        Set<String> privacyApps = getPrivacyApps();
        if (privacyApps != null) {
            mPrivacyAppsPref.setValues(privacyApps);
        }
        mPrivacyAppsPref.setOnPreferenceChangeListener(this);

        mShowAmPmPref = (CheckBoxPreference) prefSet.findPreference(KEY_SHOW_AMPM);
        mShowAmPmPref.setChecked((Settings.System.getInt(mResolver,
                Settings.System.ACTIVE_DISPLAY_SHOW_AMPM, 0) == 1));
        mShowAmPmPref.setEnabled(!is24Hour());

        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mMinimumBacklight = pm.getMinimumScreenBrightnessSetting();
        mMaximumBacklight = pm.getMaximumScreenBrightnessSetting();

        mBrightnessLevel = (SeekBarPreference) prefSet.findPreference(KEY_BRIGHTNESS);
        int brightness = Settings.System.getInt(mResolver,
                Settings.System.ACTIVE_DISPLAY_BRIGHTNESS, mMaximumBacklight);
        int realBrightness =  (int)(((float)brightness / (float)mMaximumBacklight) * 100);
        mBrightnessLevel.setValue(realBrightness);
        mBrightnessLevel.setOnPreferenceChangeListener(this);

        try {
            if (Settings.System.getInt(mResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                mBrightnessLevel.setEnabled(false);
                mBrightnessLevel.setSummary(R.string.status_bar_toggle_info);
            }
        } catch (SettingNotFoundException e) {
        }

        mDisplayTimeout = (ListPreference) prefSet.findPreference(KEY_TIMEOUT);
        timeout = Settings.System.getLong(mResolver,
                Settings.System.ACTIVE_DISPLAY_TIMEOUT, 8000L);
        mDisplayTimeout.setValue(String.valueOf(timeout));
        mDisplayTimeout.setSummary(mDisplayTimeout.getEntry());
        mDisplayTimeout.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mRedisplayPref) {
            int val = Integer.parseInt((String) newValue);
            int index = mRedisplayPref.findIndexOfValue((String) newValue);
            Settings.System.putInt(mResolver,
                Settings.System.ACTIVE_DISPLAY_REDISPLAY, val);
            mRedisplayPref.setSummary(mRedisplayPref.getEntries()[index]);
            return true;
        } else if (preference == mEnabledPref) {
            Settings.System.putInt(mResolver,
                    Settings.System.ENABLE_ACTIVE_DISPLAY,
                    ((Boolean) newValue).booleanValue() ? 1 : 0);
            return true;
        } else if (preference == mPocketModePref) {
            int val = Integer.parseInt((String) newValue);
            int index = mPocketModePref.findIndexOfValue((String) newValue);
            Settings.System.putInt(mResolver,
                Settings.System.ACTIVE_DISPLAY_POCKET_MODE, val);
            mPocketModePref.setSummary(mPocketModePref.getEntries()[index]);
            return true;
        } else if (preference == mExcludedAppsPref) {
            storeExcludedApps((Set<String>) newValue);
            return true;
        } else if (preference == mPrivacyAppsPref) {
            storePrivacyApps((Set<String>) newValue);
            return true;
        } else if (preference == mAnnoyingNotification) {
            int annoying = ((Integer)newValue).intValue();
            Settings.System.putInt(mResolver,
                    Settings.System.ACTIVE_DISPLAY_ANNOYING, annoying);
            return true;
        } else if (preference == mShakeThreshold) {
            int threshold = ((Integer)newValue).intValue();
            Settings.System.putInt(mResolver,
                    Settings.System.ACTIVE_DISPLAY_SHAKE_THRESHOLD, threshold);
            return true;
        } else if (preference == mShakeLongThreshold) {
            int longThreshold = ((Integer)newValue).intValue();
            Settings.System.putInt(mResolver,
                    Settings.System.ACTIVE_DISPLAY_SHAKE_LONGTHRESHOLD, longThreshold);
            return true;
        } else if (preference == mShakeTimeout) {
            int timeout = ((Integer)newValue).intValue();
            Settings.System.putInt(mResolver,
                    Settings.System.ACTIVE_DISPLAY_SHAKE_TIMEOUT, timeout);
            return true;
        } else if (preference == mBrightnessLevel) {
            int brightness = ((Integer)newValue).intValue();
            int realBrightness =  Math.max(mMinimumBacklight, (int)(((float)brightness / (float)100) * mMaximumBacklight));                   
            Settings.System.putInt(mResolver, Settings.System.ACTIVE_DISPLAY_BRIGHTNESS, realBrightness);
            return true;
        } else if (preference == mDisplayTimeout) {
            int val = Integer.parseInt((String) newValue);
            int index = mDisplayTimeout.findIndexOfValue((String) newValue);
            Settings.System.putInt(mResolver,
                Settings.System.ACTIVE_DISPLAY_TIMEOUT, val);
            mDisplayTimeout.setSummary(mDisplayTimeout.getEntries()[index]);
            return true;
        } else if (preference == mProximityThreshold) {
            int val = Integer.parseInt((String) newValue);
            int index = mProximityThreshold.findIndexOfValue((String) newValue);
            Settings.System.putInt(mResolver,
                Settings.System.ACTIVE_DISPLAY_THRESHOLD, val);
            mProximityThreshold.setSummary(mProximityThreshold.getEntries()[index]);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;
        if (preference == mBypassPref) {
            value = mBypassPref.isChecked();
            Settings.System.putInt(mResolver,
                    Settings.System.ACTIVE_DISPLAY_BYPASS,
                    value ? 1 : 0);
        } else if (preference == mSunlightModePref) {
            value = mSunlightModePref.isChecked();
            Settings.System.putInt(mResolver,
                    Settings.System.ACTIVE_DISPLAY_SUNLIGHT_MODE,
                    value ? 1 : 0);
        } else if (preference == mShowAmPmPref) {
            value = mShowAmPmPref.isChecked();
            Settings.System.putInt(mResolver,
                    Settings.System.ACTIVE_DISPLAY_SHOW_AMPM,
                    value ? 1 : 0);
        } else if (preference == mTurnOffModePref) {
            value = mTurnOffModePref.isChecked();
            Settings.System.putInt(mResolver,
                    Settings.System.ACTIVE_DISPLAY_TURNOFF_MODE,
                    value ? 1 : 0);
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }

    private Set<String> getExcludedApps() {
        String excluded = Settings.System.getString(mResolver,
                Settings.System.ACTIVE_DISPLAY_EXCLUDED_APPS);
        if (TextUtils.isEmpty(excluded)) {
            return null;
        }
        return new HashSet<String>(Arrays.asList(excluded.split("\\|")));
    }

    private void storeExcludedApps(Set<String> values) {
        StringBuilder builder = new StringBuilder();
        String delimiter = "";
        for (String value : values) {
            builder.append(delimiter);
            builder.append(value);
            delimiter = "|";
        }
        Settings.System.putString(mResolver,
                Settings.System.ACTIVE_DISPLAY_EXCLUDED_APPS, builder.toString());
    }

    private Set<String> getPrivacyApps() {
        String privacies = Settings.System.getString(mResolver,
                Settings.System.ACTIVE_DISPLAY_PRIVACY_APPS);
        if (TextUtils.isEmpty(privacies)) {
            return null;
        }
        return new HashSet<String>(Arrays.asList(privacies.split("\\|")));
    }

    private void storePrivacyApps(Set<String> values) {
        StringBuilder builder = new StringBuilder();
        String delimiter = "";
        for (String value : values) {
            builder.append(delimiter);
            builder.append(value);
            delimiter = "|";
        }
        Settings.System.putString(mResolver,
                Settings.System.ACTIVE_DISPLAY_PRIVACY_APPS, builder.toString());
    }

    private boolean is24Hour() {
        return DateFormat.is24HourFormat(mContext);
    }
}
