/*
 * Copyright (C) 2020 Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.rr;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.media.AudioAttributes;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.widget.EditText;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.SettingsPreferenceFragment;

import com.android.settings.R;
import com.android.settings.rr.Preferences.SystemSettingListPreference;

public class VibrationSettingsPreferenceFragment extends SettingsPreferenceFragment
            implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    private static final String TAG = "VibrationSettingsPreferenceFragment";

    private static final String CATEGORY_VIBRATION_SETTINGS = "vibrate_when_ringing_settings";
    private static final String RING_VIBRATION_INTENSITY = "ring_vibration_intensity";
    private static final String NOTIFICATION_VIBRATION_INTENSITY = "notification_vibration_intensity";
    private static final String RINGTONE_VIBRATION_PATTERN = "ringtone_vibration_pattern";

    private static final long[] DZZZ_DZZZ_VIBRATION_PATTERN = {
        0, // No delay before starting
        800, // How long to vibrate
        800, // How long to wait before vibrating again
        800, // How long to vibrate
        800, // How long to wait before vibrating again
    };

    private static final long[] DZZZ_DA_VIBRATION_PATTERN = {
        0, // No delay before starting
        500, // How long to vibrate
        200, // Delay
        20, // How long to vibrate
        720, // How long to wait before vibrating again
    };

    private static final long[] MM_MM_MM_VIBRATION_PATTERN = {
        0, // No delay before starting
        300, // How long to vibrate
        400, // Delay
        300, // How long to vibrate
        400, // Delay
        300, // How long to vibrate
        1700, // How long to wait before vibrating again
    };

    private static final long[] DA_DA_DZZZ_VIBRATION_PATTERN = {
        0, // No delay before starting
        30, // How long to vibrate
        80, // Delay
        30, // How long to vibrate
        80, // Delay
        50,  // How long to vibrate
        180, // Delay
        600,  // How long to vibrate
        1050, // How long to wait before vibrating again
    };

    private static final long[] DA_DZZZ_DA_VIBRATION_PATTERN = {
        0, // No delay before starting
        80, // How long to vibrate
        200, // Delay
        600, // How long to vibrate
        150, // Delay
        20,  // How long to vibrate
        1050, // How long to wait before vibrating again
    };

    private static final int[] NINE_ELEMENTS_VIBRATION_AMPLITUDE = {
        0, // No delay before starting
        255, // Vibrate full amplitude
        0, // No amplitude while waiting
        255,
        0,
        255,
        0,
        255,
        0,
    };

    private static final int[] SEVEN_ELEMENTS_VIBRATION_AMPLITUDE = {
        0, // No delay before starting
        255, // Vibrate full amplitude
        0, // No amplitude while waiting
        255,
        0,
        255,
        0,
    };

    private static final int[] FIVE_ELEMENTS_VIBRATION_AMPLITUDE = {
        0, // No delay before starting
        255, // Vibrate full amplitude
        0, // No amplitude while waiting
        255,
        0,
    };

    private Context mContext;
    private ContentResolver mContentResolver;
    private Vibrator mVibrator;

    private final AudioAttributes mAudioAttributesRingtone = new AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
        .build();

    private final AudioAttributes mAudioAttributesNotif = new AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .build();

    private boolean mHasOnePlusHaptics;

    private Preference mRingerVibrationIntensity;
    private Preference mNotifVibrationIntensity;
    private PreferenceCategory mVibrationSettings;
    private ListPreference mVibrationPattern;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.vibration_settings);

        mContext = getContext();
        mContentResolver = mContext.getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();

        mHasOnePlusHaptics = getResources().getBoolean(
            com.android.internal.R.bool.config_hasOnePlusHapticMotor);

        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        if (mVibrator != null && !mVibrator.hasVibrator()) {
            mVibrator = null;
        }

        mVibrationSettings = (PreferenceCategory) findPreference(CATEGORY_VIBRATION_SETTINGS);
        mRingerVibrationIntensity = (Preference) findPreference(RING_VIBRATION_INTENSITY);
        mNotifVibrationIntensity = (Preference) findPreference(NOTIFICATION_VIBRATION_INTENSITY);
        mVibrationPattern = (ListPreference) findPreference(RINGTONE_VIBRATION_PATTERN);

        if (mHasOnePlusHaptics) {
            Log.i(TAG, "OnePlus vibrator format supported");
            mRingerVibrationIntensity.setOnPreferenceClickListener(this);
            mNotifVibrationIntensity.setOnPreferenceClickListener(this);
            mRingerVibrationIntensity.setOnPreferenceChangeListener(this);
            mNotifVibrationIntensity.setOnPreferenceChangeListener(this);
            updateIntensityText();
        } else {
            mVibrationSettings.removePreference(mRingerVibrationIntensity);
            prefScreen.removePreference(mNotifVibrationIntensity);
        }

        mVibrationPattern.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mRingerVibrationIntensity ||
                preference == mNotifVibrationIntensity) {
            final VibrationIntensityDialog dialog = new VibrationIntensityDialog();
            dialog.setParameters(mContext, preference.getKey(), preference);
            dialog.show(getFragmentManager(), TAG);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mVibrationPattern) {
            int val = Integer.valueOf((String) newValue);
            performVibrationDemo(val);
            return true;
        } else if (preference == mRingerVibrationIntensity ||
                preference == mNotifVibrationIntensity) {
            if (mVibrator != null && mVibrator.hasVibrator()) {
                mVibrator.vibrate(250, mAudioAttributesNotif);
            }
            return true;
        }
        return false;
    }

    public static void reset(Context mContext) {
        ContentResolver resolver = mContext.getContentResolver();

        Settings.System.putIntForUser(resolver,
                Settings.System.HAPTIC_FEEDBACK_ENABLED, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.INCALL_FEEDBACK_VIBRATE, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.NOTIFICATION_VIBRATION_INTENSITY, 2, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.RING_VIBRATION_INTENSITY, 2, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.RINGTONE_VIBRATION_PATTERN, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.VIBRATE_ON_NOTIFICATIONS, 1, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.VIBRATE_WHEN_RINGING, 0, UserHandle.USER_CURRENT);
    }

    private void updateIntensityText() {
        setText(mRingerVibrationIntensity, Settings.System.getIntForUser(mContext.getContentResolver(),
            Settings.System.RING_VIBRATION_INTENSITY, 2, UserHandle.USER_CURRENT));
        setText(mNotifVibrationIntensity, Settings.System.getIntForUser(mContext.getContentResolver(),
            Settings.System.NOTIFICATION_VIBRATION_INTENSITY, 2, UserHandle.USER_CURRENT));
    }

    private void setText(Preference pref, int intensity) {
        switch(intensity) {
            case 0:
                pref.setSummary(R.string.vibration_intensity_disabled);
                break;
            case 1:
                pref.setSummary(R.string.vibration_intensity_light);
                break;
            case 2:
                pref.setSummary(R.string.vibration_intensity_medium);
                break;
            case 3:
                pref.setSummary(R.string.vibration_intensity_strong);
                break;
            case 4:
                pref.setSummary(R.string.vibration_intensity_custom);
                break;
        }
    }

    private void performVibrationDemo(int val) {
        VibrationEffect mDefaultVibrationEffect;
        switch(val) {
            case 1:
                mDefaultVibrationEffect = VibrationEffect.createWaveform(DZZZ_DA_VIBRATION_PATTERN,
                    FIVE_ELEMENTS_VIBRATION_AMPLITUDE, -1);
                break;
            case 2:
                mDefaultVibrationEffect = VibrationEffect.createWaveform(MM_MM_MM_VIBRATION_PATTERN,
                    SEVEN_ELEMENTS_VIBRATION_AMPLITUDE, -1);
                break;
            case 3:
                mDefaultVibrationEffect = VibrationEffect.createWaveform(DA_DA_DZZZ_VIBRATION_PATTERN,
                    NINE_ELEMENTS_VIBRATION_AMPLITUDE, -1);
                break;
            case 4:
                mDefaultVibrationEffect = VibrationEffect.createWaveform(DA_DZZZ_DA_VIBRATION_PATTERN,
                    SEVEN_ELEMENTS_VIBRATION_AMPLITUDE, -1);
                break;
            default:
                mDefaultVibrationEffect = VibrationEffect.createWaveform(DZZZ_DZZZ_VIBRATION_PATTERN,
                    FIVE_ELEMENTS_VIBRATION_AMPLITUDE, -1);
                break;
        }
        if (mVibrator != null && mVibrator.hasVibrator()) {
            mVibrator.vibrate(mDefaultVibrationEffect, mAudioAttributesRingtone);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.RESURRECTED;
    }
}
