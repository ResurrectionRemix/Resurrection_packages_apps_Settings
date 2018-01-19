/*
 * Copyright (C) 2017 AICP
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


package com.android.settings.rr.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.IActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.SystemProperties;
import android.os.Vibrator;
import android.text.TextUtils;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v14.preference.PreferenceFragment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.android.settings.R;
import com.android.settings.SettingsActivity;

public abstract class Util {

    private static final String TAG = Util.class.getSimpleName();

    public static final String PROPERTY_DEVICE = "ro.rr.device";
    public static final String PROPERTY_DEVICE_EXT = "ro.product.device";


    public static void setSummaryToValue(ListPreference pref) {
        pref.setSummary(pref.getEntry());
    }

    public static void setSummaryToValue(ListPreference pref, Object newValue) {
        try {
            int index = pref.findIndexOfValue((String) newValue);
            pref.setSummary(pref.getEntries()[index]);
        } catch (Exception e) {
            Log.w(TAG, "setSummaryToValue: " + newValue + " caused exception " + e);
        }
    }

    public static boolean isPackageInstalled(String packageName, PackageManager pm) {
        try {
            String mVersion = pm.getPackageInfo(packageName, 0).versionName;
            return mVersion != null;
        } catch (PackageManager.NameNotFoundException notFound) {
            return false;
        }
    }

    public static boolean isPackageEnabled(String packageName, PackageManager pm) {
        try {
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            return ai.enabled;
        } catch (PackageManager.NameNotFoundException notFound) {
            return false;
        }
    }

    public static String getDevice(Context context) {
        String device = SystemProperties.get(PROPERTY_DEVICE);
        if (TextUtils.isEmpty(device)) {
            device = SystemProperties.get(PROPERTY_DEVICE_EXT);
            //device = translateDeviceName(context, device);
        }
        return device == null ? "" : device.toLowerCase();
   }

   public static boolean hasVibrator(Context context) {
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        return v.hasVibrator();
   }

   public static String readStringFromFile(File inputFile) throws IOException {
        FileReader fileReader = new FileReader(inputFile);
        StringBuffer stringBuffer = new StringBuffer();
        int numCharsRead;
        char[] charArray = new char[1024];
        while ((numCharsRead = fileReader.read(charArray)) > 0) {
            stringBuffer.append(charArray, 0, numCharsRead);
        }
        fileReader.close();
        return stringBuffer.toString();
    }

    /**
     * This can not reliably detect whether the user has root access,
     * but it can detect some cases when the user hasn't.
     */
    public static boolean hasSu() {
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(new String[] { "which", "su" });
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            return br.readLine() != null;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (p != null) p.destroy();
        }
        return false;
    }

    public static void requireRoot(Preference preference) {
        if (!hasSu()) {
            preference.getParent().removePreference(preference);
        }
    }

    public static void restartSystemUi(Context context) {
        new RestartSystemUiTask(context).execute();
    }

    public static void showSystemUiRestartDialog(Context context) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.restart_systemui)
                .setMessage(R.string.restart_systemui)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        restartSystemUi(context);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static class RestartSystemUiTask extends AsyncTask<Void, Void, Void> {
        private Context mContext;

        public RestartSystemUiTask(Context context) {
            super();
            mContext = context;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                ActivityManager am =
                        (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
                IActivityManager ams = ActivityManager.getService();
                for (ActivityManager.RunningAppProcessInfo app: am.getRunningAppProcesses()) {
                    if ("com.android.systemui".equals(app.processName)) {
                        ams.killApplicationProcess(app.processName, app.uid);
                        break;
                    }
                }
                //Class ActivityManagerNative = Class.forName("android.app.ActivityManagerNative");
                //Method getDefault = ActivityManagerNative.getDeclaredMethod("getDefault", null);
                //Object amn = getDefault.invoke(null, null);
                //Method killApplicationProcess = amn.getClass().getDeclaredMethod("killApplicationProcess", String.class, int.class);
                //mContext.stopService(new Intent().setComponent(new ComponentName("com.android.systemui", "com.android.systemui.SystemUIService")));
                //am.killBackgroundProcesses("com.android.systemui");
                //for (ActivityManager.RunningAppProcessInfo app : am.getRunningAppProcesses()) {
                //    if ("com.android.systemui".equals(app.processName)) {
                //        killApplicationProcess.invoke(amn, app.processName, app.uid);
                //        break;
                //    }
                //}
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
