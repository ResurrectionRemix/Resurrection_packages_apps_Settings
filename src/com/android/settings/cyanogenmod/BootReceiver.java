/*
 * Copyright (C) 2012 The CyanogenMod Project
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.settings.ButtonSettings;
import com.android.settings.DisplaySettings;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.hardware.VibratorIntensity;
import com.android.settings.inputmethod.InputMethodAndLanguageSettings;
import com.android.settings.livedisplay.DisplayColor;
import com.android.settings.livedisplay.DisplayGamma;

import java.util.Arrays;
import java.util.List;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";
    private static final String CPU_SETTINGS_PROP = "sys.cpufreq.restored";
    private static final String IOSCHED_SETTINGS_PROP = "sys.iosched.restored";

    private static final String ENCRYPTED_STATE = "1";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        final PowerManager pm = (PowerManager)ctx.getSystemService(Context.POWER_SERVICE);

        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)
                && !ENCRYPTED_STATE.equals(SystemProperties.get("vold.decrypt"))) {
            if (!pm.hasPowerProfiles()) {
                if (SystemProperties.getBoolean(CPU_SETTINGS_PROP, false) == false) {
                    SystemProperties.set(CPU_SETTINGS_PROP, "true");
                    configureCPU(ctx);
                } else {
                    SystemProperties.set(CPU_SETTINGS_PROP, "false");
                }
            }
            if (SystemProperties.getBoolean(IOSCHED_SETTINGS_PROP, false) == false) {
                SystemProperties.set(IOSCHED_SETTINGS_PROP, "true");
                configureIOSched(ctx);
            } else {
                SystemProperties.set(IOSCHED_SETTINGS_PROP, "false");
            }
        }

        /* Restore the hardware tunable values */
        DisplaySettings.restore(ctx);
        ButtonSettings.restoreKeyDisabler(ctx);
        DisplayGamma.restore(ctx);
        VibratorIntensity.restore(ctx);
        InputMethodAndLanguageSettings.restore(ctx);
    }

    private void initFreqCapFiles(Context ctx)
    {
        if (Processor.freqCapFilesInitialized) return;
        Processor.FREQ_MAX_FILE = ctx.getResources().getString(R.string.max_cpu_freq_file);
        Processor.FREQ_MIN_FILE = ctx.getResources().getString(R.string.min_cpu_freq_file);
        Processor.freqCapFilesInitialized = true;
    }

    private void configureCPU(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

        if (prefs.getBoolean(Processor.SOB_PREF, false) == false) {
            Log.i(TAG, "CPU restore disabled by user preference.");
            return;
        }

        String governor = prefs.getString(Processor.GOV_PREF, null);
        String minFrequency = prefs.getString(Processor.FREQ_MIN_PREF, null);
        String maxFrequency = prefs.getString(Processor.FREQ_MAX_PREF, null);
        String availableFrequenciesLine = Utils.fileReadOneLine(Processor.FREQ_LIST_FILE);
        String availableGovernorsLine = Utils.fileReadOneLine(Processor.GOV_LIST_FILE);
        boolean noSettings = ((availableGovernorsLine == null) || (governor == null)) &&
                             ((availableFrequenciesLine == null) || ((minFrequency == null) && (maxFrequency == null)));
        List<String> frequencies = null;
        List<String> governors = null;

        if (noSettings) {
            Log.d(TAG, "No CPU settings saved. Nothing to restore.");
        } else {
            initFreqCapFiles(ctx);
            if (availableGovernorsLine != null){
                governors = Arrays.asList(availableGovernorsLine.split(" "));
            }
            if (availableFrequenciesLine != null){
                frequencies = Arrays.asList(availableFrequenciesLine.split(" "));
            }
            if (maxFrequency != null && frequencies != null && frequencies.contains(maxFrequency)) {
                Utils.fileWriteOneLine(Processor.FREQ_MAX_FILE, maxFrequency);
            }
            if (minFrequency != null && frequencies != null && frequencies.contains(minFrequency)) {
                Utils.fileWriteOneLine(Processor.FREQ_MIN_FILE, minFrequency);
            }
            if (governor != null && governors != null && governors.contains(governor)) {
                Utils.fileWriteOneLine(Processor.GOV_FILE, governor);
            }
            Log.d(TAG, "CPU settings restored.");
        }
    }

    private void configureIOSched(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

        if (prefs.getBoolean(IOScheduler.SOB_PREF, false) == false) {
            Log.i(TAG, "IOSched restore disabled by user preference.");
            return;
        }

        String ioscheduler = prefs.getString(IOScheduler.IOSCHED_PREF, null);
        String availableIOSchedulersLine = Utils.fileReadOneLine(IOScheduler.IOSCHED_LIST_FILE);
        boolean noSettings = ((availableIOSchedulersLine == null) || (ioscheduler == null));
        List<String> ioschedulers = null;

        if (noSettings) {
            Log.d(TAG, "No I/O scheduler settings saved. Nothing to restore.");
        } else {
            if (availableIOSchedulersLine != null){
                ioschedulers = Arrays.asList(availableIOSchedulersLine.replace("[", "").replace("]", "").split(" "));
            }
            if (ioscheduler != null && ioschedulers != null && ioschedulers.contains(ioscheduler)) {
                Utils.fileWriteOneLine(IOScheduler.IOSCHED_LIST_FILE, ioscheduler);
            }
            Log.d(TAG, "I/O scheduler settings restored.");
        }
    }
}
