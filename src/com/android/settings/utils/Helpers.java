
package com.android.settings.util;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.content.Context;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import com.android.settings.R;

public class Helpers {
    // avoids hardcoding the tag
    private static final String TAG = Thread.currentThread().getStackTrace()[1].getClassName();

    public Helpers() {
        // dummy constructor
    }

    public static void restartSystemUI() {
        CMDProcessor.startSuCommand("pkill -TERM -f com.android.systemui");
    }

    public static void showSystemUIrestartDialog(Activity a) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setTitle(R.string.systemui_restart_title);
        builder.setMessage(R.string.systemui_restart_message);
        builder.setPositiveButton(R.string.print_restart,
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected void onPreExecute() {
                        ProgressDialog dialog = new ProgressDialog(a);
                        dialog.setMessage(a.getResources().getString(R.string.restarting_ui));
                        dialog.setCancelable(false);
                        dialog.setIndeterminate(true);
                        dialog.show();
                    }
                    @Override
                    protected Void doInBackground(Void... params) {
                        // Give the user a second to see the dialog
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            // Ignore
                        }

                        // Restart the UI
                        CMDProcessor.startSuCommand("pkill -f com.android.systemui");
                        a.finish();
                        return null;
                    }
                };
                task.execute();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

	public static boolean getMount(String mount) {
        String[] mounts = getMounts("/system");
        if (mounts != null && mounts.length >= 3) {
            String device = mounts[0];
            String path = mounts[1];
            String point = mounts[2];
            String preferredMountCmd = new String("mount -o " + mount + ",remount -t " + point + ' ' + device + ' ' + path);
            if (CMDProcessor.runSuCommand(preferredMountCmd).success()) {
                return true;
            }
        }
        String fallbackMountCmd = new String("busybox mount -o remount," + mount + " /system");
        return CMDProcessor.runSuCommand(fallbackMountCmd).success();
    }


    public static String[] getMounts(CharSequence path) {
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader("/proc/mounts"), 256);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains(path)) {
                    return line.split(" ");
                }
            }
        } catch (FileNotFoundException ignored) {
            Log.d(TAG, "/proc/mounts does not exist");
        } catch (IOException ignored) {
            Log.d(TAG, "Error reading /proc/mounts");
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ignored) {
                    // ignored
                }
            }
        }
        return null;
    }
}

