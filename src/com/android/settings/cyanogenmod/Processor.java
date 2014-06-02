/*
 * Copyright (C) 2012-2014 The CyanogenMod Project
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

package com.android.settings.cyanogenmod;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.os.SystemService;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import java.lang.Runtime;

//
// CPU Related Settings
//
public class Processor extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    public static final String CPU_ONLINE = "/sys/devices/system/cpu/cpu0/online";
    public static final String FREQ_CUR_PREF = "pref_cpu_freq_cur";
    public static final String SCALE_CUR_FILE = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq";
    public static final String FREQINFO_CUR_FILE = "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_cur_freq";
    private static String FREQ_CUR_FILE = SCALE_CUR_FILE;
    public static final String GOV_PREF = "pref_cpu_gov";
    public static final String GOV_LIST_FILE = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors";
    public static final String GOV_FILE = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor";
    public static final String FREQ_MIN_PREF = "pref_cpu_freq_min";
    public static final String FREQ_MAX_PREF = "pref_cpu_freq_max";
    public static final String FREQ_LIST_FILE = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_frequencies";
    public static String FREQ_MAX_FILE = null;
    public static String FREQ_MIN_FILE = null;
    public static final String SOB_PREF = "pref_cpu_set_on_boot";

    protected static boolean freqCapFilesInitialized = false;

    private static final String TAG = "CPUSettings";

    private String mGovernorFormat;
    private String mMinFrequencyFormat;
    private String mMaxFrequencyFormat;

    private Preference mCurFrequencyPref;
    private ListPreference mGovernorPref;
    private ListPreference mMinFrequencyPref;
    private ListPreference mMaxFrequencyPref;

    private class CurCPUThread extends Thread {
        private boolean mInterrupt = false;

        public void interrupt() {
            mInterrupt = true;
        }

        @Override
        public void run() {
            try {
                while (!mInterrupt) {
                    sleep(500);
                    final String curFreq = Utils.fileReadOneLine(FREQ_CUR_FILE);
                    if (curFreq != null)
                        mCurCPUHandler.sendMessage(mCurCPUHandler.obtainMessage(0, curFreq));
                }
            } catch (InterruptedException e) {
            }
        }
    };

    private CurCPUThread mCurCPUThread = new CurCPUThread();

    private Handler mCurCPUHandler = new Handler() {
        public void handleMessage(Message msg) {
            mCurFrequencyPref.setSummary(toMHz((String) msg.obj));
            updateCpufreqValues();
        }
    };

    private void initFreqCapFiles()
    {
        if (freqCapFilesInitialized) return;
        FREQ_MAX_FILE = getString(R.string.max_cpu_freq_file);
        FREQ_MIN_FILE = getString(R.string.min_cpu_freq_file);
        freqCapFilesInitialized = true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initFreqCapFiles();

        mGovernorFormat = getString(R.string.cpu_governors_summary);
        mMinFrequencyFormat = getString(R.string.cpu_min_freq_summary);
        mMaxFrequencyFormat = getString(R.string.cpu_max_freq_summary);

        String[] availableFrequencies = new String[0];
        String[] availableGovernors = new String[0];
        String[] frequencies;
        String availableGovernorsLine;
        String availableFrequenciesLine;
        String temp;

        addPreferencesFromResource(R.xml.processor_settings);

        PreferenceScreen prefScreen = getPreferenceScreen();

        mGovernorPref = (ListPreference) prefScreen.findPreference(GOV_PREF);
        mCurFrequencyPref = (Preference) prefScreen.findPreference(FREQ_CUR_PREF);
        mMinFrequencyPref = (ListPreference) prefScreen.findPreference(FREQ_MIN_PREF);
        mMaxFrequencyPref = (ListPreference) prefScreen.findPreference(FREQ_MAX_PREF);

        /* Governor
        Some systems might not use governors */
        if (!Utils.fileExists(GOV_LIST_FILE) || !Utils.fileExists(GOV_FILE) || (temp = Utils.fileReadOneLine(GOV_FILE)) == null || (availableGovernorsLine = Utils.fileReadOneLine(GOV_LIST_FILE)) == null) {
            prefScreen.removePreference(mGovernorPref);

        } else {
            availableGovernors = availableGovernorsLine.split(" ");

            mGovernorPref.setEntryValues(availableGovernors);
            mGovernorPref.setEntries(availableGovernors);
            mGovernorPref.setValue(temp);
            mGovernorPref.setSummary(String.format(mGovernorFormat, temp));
            mGovernorPref.setOnPreferenceChangeListener(this);
        }

        // Disable the min/max list if we dont have a list file
        if (!Utils.fileExists(FREQ_LIST_FILE) || (availableFrequenciesLine = Utils.fileReadOneLine(FREQ_LIST_FILE)) == null) {
            mMinFrequencyPref.setEnabled(false);
            mMaxFrequencyPref.setEnabled(false);

        } else {
            availableFrequencies = availableFrequenciesLine.split(" ");

            frequencies = new String[availableFrequencies.length];
            for (int i = 0; i < frequencies.length; i++) {
                frequencies[i] = toMHz(availableFrequencies[i]);
            }

            // Min frequency
            if (!Utils.fileExists(FREQ_MIN_FILE) || (temp = Utils.fileReadOneLine(FREQ_MIN_FILE)) == null) {
                mMinFrequencyPref.setEnabled(false);

            } else {
                mMinFrequencyPref.setEntryValues(availableFrequencies);
                mMinFrequencyPref.setEntries(frequencies);
                mMinFrequencyPref.setValue(temp);
                mMinFrequencyPref.setSummary(String.format(mMinFrequencyFormat, toMHz(temp)));
                mMinFrequencyPref.setOnPreferenceChangeListener(this);
            }

            // Max frequency
            if (!Utils.fileExists(FREQ_MAX_FILE) || (temp = Utils.fileReadOneLine(FREQ_MAX_FILE)) == null) {
                mMaxFrequencyPref.setEnabled(false);

            } else {
                mMaxFrequencyPref.setEntryValues(availableFrequencies);
                mMaxFrequencyPref.setEntries(frequencies);
                mMaxFrequencyPref.setValue(temp);
                mMaxFrequencyPref.setSummary(String.format(mMaxFrequencyFormat, toMHz(temp)));
                mMaxFrequencyPref.setOnPreferenceChangeListener(this);
            }
        }

        // Cur frequency
        if (!Utils.fileExists(FREQ_CUR_FILE)) {
            FREQ_CUR_FILE = FREQINFO_CUR_FILE;
        }

        if (!Utils.fileExists(FREQ_CUR_FILE) || (temp = Utils.fileReadOneLine(FREQ_CUR_FILE)) == null) {
            mCurFrequencyPref.setEnabled(false);

        } else {
            mCurFrequencyPref.setSummary(toMHz(temp));

            mCurCPUThread.start();
        }
    }

    private void updateCpufreqValues() {
        String temp;

        if (Utils.fileExists(FREQ_MIN_FILE) && (temp = Utils.fileReadOneLine(FREQ_MIN_FILE)) != null) {
            mMinFrequencyPref.setValue(temp);
            mMinFrequencyPref.setSummary(String.format(mMinFrequencyFormat, toMHz(temp)));
        }

        if (Utils.fileExists(FREQ_MAX_FILE) && (temp = Utils.fileReadOneLine(FREQ_MAX_FILE)) != null) {
            mMaxFrequencyPref.setValue(temp);
            mMaxFrequencyPref.setSummary(String.format(mMaxFrequencyFormat, toMHz(temp)));
        }

        if (Utils.fileExists(GOV_FILE) && (temp = Utils.fileReadOneLine(GOV_FILE)) != null) {
            mGovernorPref.setSummary(String.format(mGovernorFormat, temp));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        initFreqCapFiles();
        updateCpufreqValues();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCurCPUThread.interrupt();
        try {
            mCurCPUThread.join();
        } catch (InterruptedException e) {
        }
    }

    public boolean onPreferenceChange(Preference preference, Object value) {
        initFreqCapFiles();

        final String newValue = (String) value;
        String fname = "";

        if (newValue != null) {
            if (preference == mGovernorPref) {
                fname = GOV_FILE;
            } else if (preference == mMinFrequencyPref) {
                fname = FREQ_MIN_FILE;
            } else if (preference == mMaxFrequencyPref) {
                fname = FREQ_MAX_FILE;
            }

            if (Utils.fileWriteOneLine(fname, newValue)) {
                final String file = fname;
                final int nrcpus = Runtime.getRuntime().availableProcessors();
                if (nrcpus > 1) {
                    new Thread() {
                        public void run() {
                            int count = 0;
                            int maxcount = 5;
                            String on = "1";
                            String off = "0";
                            String onfile = "";
                            String cpufile = "";
                            String savedstate = "";
                            String state = "";
                            String mpdec = "mpdecision";
                            SystemService.State mpdecstate = SystemService.getState(mpdec);
                            // Dumb down to a running mpdecision service
                            if (mpdecstate.equals(SystemService.State.RUNNING)) {
                                SystemService.stop(mpdec);
                            }
                            try {
                                for (int i = 1; i < nrcpus; i++) {
                                    onfile = CPU_ONLINE.replace("cpu0", "cpu" + i);
                                    cpufile = file.replace("cpu0", "cpu" + i);
                                    savedstate = Utils.fileReadOneLine(onfile);
                                    // Writing on to already online cpu throws EINVAL exception
                                    if (savedstate.equals(off)) {
                                        if (Utils.fileIsWritable(onfile)) {
                                            Utils.fileWriteOneLine(onfile, on);
                                        } else {
                                            String hw = SystemProperties.get("ro.hardware");
                                            Log.e(TAG, onfile +
                                            " not writable, did you set ownership in init." +
                                            hw + ".rc?");
                                        }
                                    }
                                    // Give ueventd a little time to set perms
                                    while (count < maxcount) {
                                        Thread.sleep(10);
                                        if (Utils.fileExists(cpufile)) {
                                            if (Utils.fileIsWritable(cpufile)) {
                                                Utils.fileWriteOneLine(cpufile, newValue);
                                                break;
                                            } else {
                                                Log.e(TAG, cpufile +
                                                " not writable, did you set ueventd rules?");
                                            }
                                        }
                                        count++;
                                        if (count == maxcount) {
                                            Log.e(TAG, "Failed setting new value to " + cpufile);
                                        }
                                    }
                                    count = 0;
                                    state = Utils.fileReadOneLine(onfile);
                                    // Restore prior state of onlined cpu
                                    if (state.equals(on) && !state.equals(savedstate)) {
                                        Utils.fileWriteOneLine(onfile, off);
                                    }
                                }
                            } catch (InterruptedException e) {
                            }
                            // Restart mpdec
                            if (mpdecstate.equals(SystemService.State.RUNNING)) {
                                SystemService.start(mpdec);
                            }
                        }
                    }.start();
                }

                if (preference == mGovernorPref) {
                    mGovernorPref.setSummary(String.format(mGovernorFormat, newValue));
                } else if (preference == mMinFrequencyPref) {
                    mMinFrequencyPref.setSummary(String.format(mMinFrequencyFormat,
                            toMHz(newValue)));
                } else if (preference == mMaxFrequencyPref) {
                    mMaxFrequencyPref.setSummary(String.format(mMaxFrequencyFormat,
                            toMHz(newValue)));
                }
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    private String toMHz(String mhzString) {
        return new StringBuilder().append(Integer.valueOf(mhzString) / 1000).append(" MHz")
                .toString();
    }
}
