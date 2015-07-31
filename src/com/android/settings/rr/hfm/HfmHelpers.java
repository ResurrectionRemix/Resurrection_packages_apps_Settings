/*
 * Copyright (C) 2014 Dirty Unicorns
 * Copyright (C) 2014 Android Ice Cold Project
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

package com.android.settings.rr.hfm;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.StrictMode;
import android.provider.Settings;
import android.widget.TextView;

import com.android.settings.rr.hfm.HfmSettings;
import com.android.settings.R;

public final class HfmHelpers {
    private static final String TAG = "HfmHelpers";

    private HfmHelpers() {
        throw new AssertionError();
    }

    public static int i = 0;

    public static void checkStatus(Context context) {
        File defHosts = new File("/etc/hosts.og");
        File altHosts = new File("/etc/hosts.alt");
        File hosts = new File("/etc/hosts");
        try {
            if (Settings.System.getInt(context.getContentResolver(), Settings.System.HFM_DISABLE_ADS, 0) == 1
                    && areFilesDifferent(hosts, altHosts)) {
                copyFiles(altHosts, hosts);
            } else if (Settings.System.getInt(context.getContentResolver(), Settings.System.HFM_DISABLE_ADS, 0) == 0
                    && areFilesDifferent(hosts, defHosts) && isOurHostsFile()) {
                copyFiles(defHosts, hosts);
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    public static boolean areFilesDifferent(File file1, File file2) throws IOException {
        String cr1, cr2;
        BufferedReader br1 = getBufferedReader(file1);
        BufferedReader br2 = getBufferedReader(file2);
        while ((cr1 = br1.readLine()) != null) {
                if((cr2 = br2.readLine()) != null) {
                        if(cr1.equals(cr2)) {
                            continue;
                        }
                }
                return true;
        }
        return br2.readLine() != null;
    }

    private static boolean isOurHostsFile() throws IOException {
        boolean ret = false;
        File hosts = new File("/etc/hosts");
        String line;
        BufferedReader rd1 = getBufferedReader(hosts);
        line = rd1.readLine();
        if (line.contains("#DirtyUnicorns")) {
            ret = true;
        }
        return ret;
    }

    private static BufferedReader getBufferedReader(File file) throws IOException {
            return new BufferedReader(new FileReader(file));
    }

    public static void RunAsRoot(String string) throws IOException {
        Process P = Runtime.getRuntime().exec("su");
        DataOutputStream os = new DataOutputStream(P.getOutputStream());
        os.writeBytes(string + "\n");
        os.writeBytes("exit\n");
        os.flush();
    }

    public static void copyFiles(File srcFile, File dstFile) throws IOException {
        if (srcFile.exists() && dstFile.exists()) {
            String cmd = "mount -o rw,remount /system"
                       + " && rm -f " + dstFile.getAbsolutePath()
                       + " && cp -f " + srcFile.getAbsolutePath() + " " + dstFile.getAbsolutePath()
                       + " && chmod 644 " + dstFile.getAbsolutePath()
                       + " ; mount -o ro,remount /system";
            RunAsRoot(cmd);
        }
    }

    public static boolean isScriptFinished() {
        File altHosts = new File("/etc/hosts.alt");
        File hosts0 = new File("/etc/hosts0");
        File hosts1 = new File("/etc/hosts1");
        File hosts2 = new File("/etc/hosts2");
        File hosts3 = new File("/etc/hosts3");
        File tmpHosts = new File("/etc/hosts.tmp");
        File started = new File("/etc/started.cfg");

        if (altHosts.exists() && !hosts0.exists() && !hosts1.exists()
            && !hosts2.exists() && !hosts3.exists() && !tmpHosts.exists() && !started.exists()) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isAvailable(String urlString) throws IOException {
        try {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);

            URL url = new URL(urlString);

            HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
            urlc.setRequestProperty("Connection", "close");
            urlc.setConnectTimeout(1000);
            urlc.connect();

            if (urlc.getResponseCode() == 200) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void showDialog(String message, Context c) {
        AlertDialog dialog = new AlertDialog.Builder(c)
        .setMessage(message)
        .setNeutralButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) { 
                //Nothing here
            }
        }).show();
        TextView tv = (TextView) dialog.findViewById(android.R.id.message);
        tv.setTextSize(14);
    }

    public static void checkConnectivity(Context c, ConnectivityManager man) throws IOException {
        NetworkInfo networkInfo = man.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            HfmSettings.pd.show();
            FetchHosts.fetch(); //Download and manage new hosts
            i = 0;
            applyNew(c);
        } else {
            showDialog(HfmSettings.res.getString(R.string.hfm_dialog_conn_error, c), c);
        }
    }

    public static void applyNew(final Context c) {

        final int delay = 500; //ms
        final Handler h = new Handler();
        h.postDelayed(new Runnable() {
            public void run() {
                if (isScriptFinished()) {
                    if (HfmSettings.mHfmDisableAds.isChecked()) {
                        checkStatus(c); //Hosts are downloaded. Apply if user has enabled blocking.
                    }
                    showDialog(FetchHosts.successfulSources + HfmSettings.res.getString(R.string.hfm_dialog_success), c);
                    HfmSettings.pd.dismiss();
                } else if (i < 40) {
                    i++;
                    h.postDelayed(this, delay);
                } else {
                    showDialog(HfmSettings.res.getString(R.string.hfm_dialog_failed), c);
                    try {
                        RunAsRoot("mount -o rw,remount /system"
                                     + " && rm -f /etc/hosts[0-9] /etc/hosts.tmp /etc/started.cfg"
                                     + " && mount -o ro,remount /system"); //Clean scraps after failing
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    HfmSettings.pd.dismiss();
                }
            }
        }, delay);
    }
}
