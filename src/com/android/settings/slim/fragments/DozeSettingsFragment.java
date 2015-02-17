/*
 * Copyright (C) 2013 Slimroms
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

package com.android.settings.slim.fragments;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SlimSeekBarPreference;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class DozeSettingsFragment extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String KEY_DOZE_TIMEOUT = "doze_timeout";
    private static final String KEY_DOZE_TRIGGER_PICKUP = "doze_trigger_pickup";
    private static final String KEY_DOZE_TRIGGER_SIGMOTION = "doze_trigger_sigmotion";
    private static final String KEY_DOZE_TRIGGER_NOTIFICATION = "doze_trigger_notification";
    private static final String KEY_DOZE_SCHEDULE = "doze_schedule";

    private SlimSeekBarPreference mDozeTimeout;
    private SwitchPreference mDozeTriggerPickup;
    private SwitchPreference mDozeTriggerSigmotion;
    private SwitchPreference mDozeTriggerNotification;
    private SwitchPreference mDozeSchedule;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Activity activity = getActivity();
        PreferenceScreen prefSet = getPreferenceScreen();

        addPreferencesFromResource(R.xml.doze_settings);

        // Doze timeout seekbar
        mDozeTimeout = (SlimSeekBarPreference) findPreference(KEY_DOZE_TIMEOUT);
        mDozeTimeout.setDefault(3000);
        mDozeTimeout.isMilliseconds(true);
        mDozeTimeout.setInterval(1);
        mDozeTimeout.minimumValue(100);
        mDozeTimeout.multiplyValue(100);
        mDozeTimeout.setOnPreferenceChangeListener(this);

        // Doze triggers
        if (isPickupSensorAvailable(activity)) {
            mDozeTriggerPickup = (SwitchPreference) findPreference(KEY_DOZE_TRIGGER_PICKUP);
            mDozeTriggerPickup.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_DOZE_TRIGGER_PICKUP);
        }
        if (isSigmotionSensorAvailable(activity)) {
            mDozeTriggerSigmotion = (SwitchPreference) findPreference(KEY_DOZE_TRIGGER_SIGMOTION);
            mDozeTriggerSigmotion.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_DOZE_TRIGGER_SIGMOTION);
        }
        mDozeTriggerNotification = (SwitchPreference) findPreference(KEY_DOZE_TRIGGER_NOTIFICATION);
        mDozeTriggerNotification.setOnPreferenceChangeListener(this);

        // Doze schedule
        mDozeSchedule = (SwitchPreference) findPreference(KEY_DOZE_SCHEDULE);
        mDozeSchedule.setOnPreferenceChangeListener(this);

        setHasOptionsMenu(false);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mDozeTimeout) {
            int dozeTimeout = Integer.valueOf((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.DOZE_TIMEOUT, dozeTimeout);
        }
        if (preference == mDozeTriggerPickup) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.DOZE_TRIGGER_PICKUP, value ? 1 : 0);
        }
        if (preference == mDozeTriggerSigmotion) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.DOZE_TRIGGER_SIGMOTION, value ? 1 : 0);
        }
        if (preference == mDozeTriggerNotification) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.DOZE_TRIGGER_NOTIFICATION, value ? 1 : 0);
        }
        if (preference == mDozeSchedule) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.DOZE_SCHEDULE, value ? 1 : 0);
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateState();
    }

    private void updateState() {
        // Update doze preferences
        if (mDozeTimeout != null) {
            final int statusDozeTimeout = Settings.System.getInt(getContentResolver(),
                    Settings.System.DOZE_TIMEOUT, 3000);
            // minimum 100 is 1 interval of the 100 multiplier
            mDozeTimeout.setInitValue((statusDozeTimeout / 100) - 1);
        }
        if (mDozeTriggerPickup != null) {
            int value = Settings.System.getInt(getContentResolver(),
                    Settings.System.DOZE_TRIGGER_PICKUP, 1);
            mDozeTriggerPickup.setChecked(value != 0);
        }
        if (mDozeTriggerSigmotion != null) {
            int value = Settings.System.getInt(getContentResolver(),
                    Settings.System.DOZE_TRIGGER_SIGMOTION, 1);
            mDozeTriggerSigmotion.setChecked(value != 0);
        }
        if (mDozeTriggerNotification != null) {
            int value = Settings.System.getInt(getContentResolver(),
                    Settings.System.DOZE_TRIGGER_NOTIFICATION, 1);
            mDozeTriggerNotification.setChecked(value != 0);
        }
        if (mDozeSchedule != null) {
            int value = Settings.System.getInt(getContentResolver(),
                    Settings.System.DOZE_SCHEDULE, 1);
            mDozeSchedule.setChecked(value != 0);
        }
    }

    private static boolean isPickupSensorAvailable(Context context) {
        SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        return sensors != null && sensors.getDefaultSensor(Sensor.TYPE_PICK_UP_GESTURE) != null;
    }

    private static boolean isSigmotionSensorAvailable(Context context) {
        SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        return sensors != null && sensors.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION) != null;
    }
}
