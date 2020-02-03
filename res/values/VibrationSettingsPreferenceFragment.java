/*
 * Copyright (C) 2020 crDroid Android Project
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

import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.widget.RadioButtonPreference;
import com.android.settingslib.core.AbstractPreferenceController;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VibrationSettingsPreferenceFragment extends DashboardFragment 
            implements Preference.OnPreferenceClickListener {

    private static final String TAG = "VibrationSettingsPreferenceFragment";

    private static final String RINGTONE_VIBRATION_PATTERN = "ringtone_vibration_pattern";
    private static final String INCALL_FEEDBACK_VIBRATE = "incall_feeedback_vibrate";

    private static final String[] mKeys = {"pattern_dzzz_dzzz", "pattern_dzzz_da", "pattern_mm_mm_mm",
        "pattern_da_da_dzzz", "pattern_da_dzzz_da"};

    private static final String RING_VIBRATION_INTENSITY = "ring_vibration_intensity";
    private static final String NOTIFICATION_VIBRATION_INTENSITY = "notification_vibration_intensity";

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

    private final Map<String, RadioButtonPreference> mStringToPreferenceMap = new HashMap<>();

    private RadioButtonPreference[] mRadioPreferences = new RadioButtonPreference[5];

    private Preference mRingerVibrationIntensity;
    private Preference mNotifVibrationIntensity;
    private SwitchPreference mIncallFeedback;

    private SettingsObserver mSettingObserver;
    private final Handler mH = new Handler();

    private boolean mHasOnePlusHaptics;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getContext();
        mContentResolver = mContext.getContentResolver();
        mSettingObserver = new SettingsObserver(mH);

        mHasOnePlusHaptics = getResources().getBoolean(
            com.android.internal.R.bool.config_hasOnePlusHapticMotor);

        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        if (mVibrator != null && !mVibrator.hasVibrator()) {
            mVibrator = null;
        }

        mRingerVibrationIntensity = (Preference) findPreference(RING_VIBRATION_INTENSITY);
        mNotifVibrationIntensity = (Preference) findPreference(NOTIFICATION_VIBRATION_INTENSITY);
        mIncallFeedback = (SwitchPreference) findPreference(INCALL_FEEDBACK_VIBRATE);

        for (int i = 0; i < 5; i++) {
            mRadioPreferences[i] = (RadioButtonPreference) findPreference(mKeys[i]);
            mStringToPreferenceMap.put(mKeys[i], mRadioPreferences[i]);
            mRadioPreferences[i].setOnPreferenceClickListener(this);
        }

        if (mHasOnePlusHaptics) {
            Log.i(TAG, "OnePlus vibrator format supported");
            mRingerVibrationIntensity.setOnPreferenceClickListener(this);
            mNotifVibrationIntensity.setOnPreferenceClickListener(this);
            updateIntensityText();
        } else {
            Log.i(TAG, "OnePlus vibrator format not supported");
            mRingerVibrationIntensity.setEnabled(false);
            mRingerVibrationIntensity.setVisible(false);
            mNotifVibrationIntensity.setEnabled(false);
            mNotifVibrationIntensity.setVisible(false);
        }

        mIncallFeedback.setOnPreferenceClickListener(this);

        mIncallFeedback.setChecked(Settings.System.getIntForUser(mContentResolver, INCALL_FEEDBACK_VIBRATE, 0, UserHandle.USER_CURRENT) == 1);

        final int currentPattern = Settings.System.getIntForUser(mContentResolver, RINGTONE_VIBRATION_PATTERN, 0, UserHandle.USER_CURRENT);

        updateVibrationPattern(currentPattern);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mHasOnePlusHaptics) {
            mContentResolver.unregisterContentObserver(mSettingObserver);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mHasOnePlusHaptics) {
            mContentResolver.registerContentObserver(
                Settings.System.getUriFor(Settings.System.RING_VIBRATION_INTENSITY),
                true, mSettingObserver, UserHandle.USER_CURRENT);

            mContentResolver.registerContentObserver(
                Settings.System.getUriFor(Settings.System.NOTIFICATION_VIBRATION_INTENSITY),
                true, mSettingObserver, UserHandle.USER_CURRENT);
        }
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        final VibrateOnTouchPreferenceController vibrateOnTouchPreferenceController =
                new VibrateOnTouchPreferenceController(context, this, getSettingsLifecycle());
        controllers.add(vibrateOnTouchPreferenceController);
        return controllers;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        final String key = preference.getKey();
        if (preference instanceof RadioButtonPreference) {
            int val = Arrays.asList(mKeys).indexOf(key);
            updateVibrationPattern(val);
            performVibrationDemo(val);
        } else if (preference instanceof SwitchPreference) {
            Settings.System.putIntForUser(mContentResolver, INCALL_FEEDBACK_VIBRATE,
                ((SwitchPreference) preference).isChecked() ? 1 : 0, UserHandle.USER_CURRENT);
        } else {
            final VibrationIntensityDialog dialog = new VibrationIntensityDialog();
            dialog.setParameters(mContext, key, preference);
            dialog.show(getFragmentManager(), TAG);
        }
        return true;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_VIBRATION;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.vibration_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    private void updateVibrationPattern(int val) {
        for (int i = 0; i < 5; i++) {
            ((RadioButtonPreference) mStringToPreferenceMap.get(mKeys[i])).setChecked((val == i) ? true:false);
        }
        Settings.System.putIntForUser(mContentResolver, RINGTONE_VIBRATION_PATTERN,
                val, UserHandle.USER_CURRENT);
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

    private final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (!selfChange) {
                if (uri.equals(Settings.System.getUriFor(Settings.System.RING_VIBRATION_INTENSITY))) {
                    performVibrationDemo(Settings.System.getIntForUser(mContentResolver, RINGTONE_VIBRATION_PATTERN, 0, UserHandle.USER_CURRENT));
                } else if (uri.equals(Settings.System.getUriFor(Settings.System.NOTIFICATION_VIBRATION_INTENSITY))) {
                    if (mVibrator != null && mVibrator.hasVibrator()) {
                        mVibrator.vibrate(250, mAudioAttributesNotif);
                    }
                }
            }
        }
    }
}
