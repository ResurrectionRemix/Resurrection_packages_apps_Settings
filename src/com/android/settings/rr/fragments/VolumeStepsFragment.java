/*
 * Copyright (C) 2019 Android Open Source Project
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

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.SettingsPreferenceFragment;

import com.android.settings.R;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class VolumeStepsFragment extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "VolumeSteps";
    private static final String VOLUME_STEP_DEFAULTS = "volume_step_defaults";
    private static final String FIRST_RUN_KEY = "first_run";

    // base map of all preference keys and the associated stream
    private static final Map<String, Integer> volume_map = new HashMap<String, Integer>();
    static {
        volume_map.put("volume_steps_alarm", new Integer(AudioManager.STREAM_ALARM));
        volume_map.put("volume_steps_dtmf", new Integer(AudioManager.STREAM_DTMF));
        volume_map.put("volume_steps_music", new Integer(AudioManager.STREAM_MUSIC));
        volume_map.put("volume_steps_notification", new Integer(AudioManager.STREAM_NOTIFICATION));
        volume_map.put("volume_steps_ring", new Integer(AudioManager.STREAM_RING));
        volume_map.put("volume_steps_system", new Integer(AudioManager.STREAM_SYSTEM));
        volume_map.put("volume_steps_voice_call", new Integer(AudioManager.STREAM_VOICE_CALL));
    }

    // entries to remove on non-telephony devices
    private static final Set<String> telephony_set = new HashSet<String>();
    static {
        telephony_set.add("volume_steps_dtmf");
        telephony_set.add("volume_steps_ring");
        telephony_set.add("volume_steps_voice_call");
    }

    // set of available pref keys after device configuration filter
    private Set<String> mAvailableKeys = new HashSet<String>();
    private AudioManager mAudioManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.volume_steps_fragment);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        final PreferenceScreen prefScreen = getPreferenceScreen();
        mAvailableKeys = volume_map.keySet();

        // remove invalid audio stream prefs
        boolean isPhone = TelephonyManager.getDefault().getCurrentPhoneType() != TelephonyManager.PHONE_TYPE_NONE;

        if (!isPhone) {
            // remove telephony keys from available set
            mAvailableKeys.removeAll(telephony_set);
            for (String key : telephony_set) {
                Preference toRemove = prefScreen.findPreference(key);
                if (toRemove != null) {
                    prefScreen.removePreference(toRemove);
                }
            }
        }

        // check prefs for initial run of this fragment
        final boolean firstRun = checkForFirstRun();

        // entries array isn't translatable ugh
        final String defEntry = getString(R.string.volume_steps_reset);

        // initialize prefs: set defaults if first run, set listeners and update values
        for (String key : mAvailableKeys) {
            Preference pref = prefScreen.findPreference(key);
            if (pref == null || !(pref instanceof ListPreference)) {
                continue;
            }
            final ListPreference listPref = (ListPreference) pref;
            int steps = mAudioManager.getStreamMaxVolume(volume_map.get(key));
            if (firstRun) {
                saveDefaultSteps(listPref, steps);
            }
            final int defSteps = getDefaultSteps(listPref);
            CharSequence[] entries = new CharSequence[listPref.getEntries().length + 1];
            CharSequence[] values = new CharSequence[listPref.getEntryValues().length + 1];

            for (int i = 0; i < entries.length; i++) {
                if (i == 0) {
                    entries[i] = defEntry;
                    values[i] = String.valueOf(defSteps);
                    continue;
                }
                entries[i] = listPref.getEntries()[i - 1];
                values[i] = listPref.getEntryValues()[i - 1];
            }
            listPref.setEntries(entries);
            listPref.setEntryValues(values);
            updateVolumeStepPrefs(listPref, steps);
            listPref.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference.hasKey() && mAvailableKeys.contains(preference.getKey())) {
            commitVolumeSteps(preference, Integer.parseInt(objValue.toString()));
        }
        return true;
    }

    private SharedPreferences getDefaultStepsPrefs() {
        return getActivity().getSharedPreferences(VOLUME_STEP_DEFAULTS,
                Context.MODE_PRIVATE);
    }

    // test for initial run of this fragment
    private boolean checkForFirstRun() {
        String isFirstRun = getDefaultStepsPrefs().getString(FIRST_RUN_KEY, null);
        if (isFirstRun == null) {
            getDefaultStepsPrefs().edit().putString(FIRST_RUN_KEY, "first_run_initialized").commit();
            return true;
        }
        return false;
    }

    private int getDefaultSteps(Preference pref) {
        if (pref == null || !(pref instanceof ListPreference)) {
            // unlikely
            return -1;
        }
        String key = pref.getKey();
        String value = getDefaultStepsPrefs().getString(key, null);
        if (value == null) {
            // unlikely
            return -1;
        }
        return Integer.parseInt(value);
    }

    // on the initial run, let's store true device defaults in sharedPrefs
    private void saveDefaultSteps(Preference volPref, int defaultSteps) {
        String key = volPref.getKey();
        getDefaultStepsPrefs().edit().putString(key, String.valueOf(defaultSteps)).commit();
    }

    private void updateVolumeStepPrefs(Preference pref, int steps) {
        if (pref == null || !(pref instanceof ListPreference)) {
            return;
        }
        final ListPreference listPref = (ListPreference) pref;
        listPref.setSummary(String.valueOf(steps));
        listPref.setValue(String.valueOf(steps));
    }

    private void commitVolumeSteps(Preference pref, int steps) {
        Settings.System.putInt(getContentResolver(), pref.getKey(), steps);
        mAudioManager.setStreamMaxVolume(volume_map.get(pref.getKey()), steps);
        updateVolumeStepPrefs(pref, steps);
        Log.i(TAG, "Volume steps:" + pref.getKey() + "" + String.valueOf(steps));
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.RESURRECTED;
    }
}
