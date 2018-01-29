
package com.android.settings.rr.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Date;

import com.android.settings.R;

public class Helpers {
    // avoids hardcoding the tag
    private static final String TAG = Thread.currentThread().getStackTrace()[1].getClassName();

    public Helpers() {
        // dummy constructor
    }

    public static void restartSystemUI(Context context) {
        new RestartSystemUITask().execute(context);
    }

    public static void showSystemUIrestartDialog(Activity a) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setTitle(R.string.systemui_restart_title);
        builder.setMessage(R.string.systemui_restart_message);
        builder.setPositiveButton(R.string.print_restart,
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                RestartSystemUITask task = new RestartSystemUITask() {
                    private ProgressDialog dialog;
                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        dialog = new ProgressDialog(a);
                        dialog.setMessage(a.getResources().getString(R.string.restarting_ui));
                        dialog.setCancelable(false);
                        dialog.setIndeterminate(true);
                        dialog.show();
                    }
                    @Override
                    protected Void doInBackground(Context... params) {
                        // Give the user a second to see the dialog
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            // Ignore
                        }

                        // Restart the UI
                        super.doInBackground(params);
                        return null;
                    }
                    @Override
                    protected void onPostExecute(Void param) {
                        super.onPostExecute(param);
                        dialog.dismiss();
                    }
                };
                task.execute(a);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    private static class RestartSystemUITask extends AsyncTask<Context, Void, Void> {
        private Context mContext;
        @Override
        protected Void doInBackground(Context... params) {
            try {
                if (params.length > 0) {
                    mContext = params[0].getApplicationContext();
                } else {
                    throw new Exception("Called RestartSystemUITask without context");
                }
                ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
                Class ActivityManagerNative = Class.forName("android.app.ActivityManagerNative");
                Method getDefault = ActivityManagerNative.getDeclaredMethod("getDefault", null);
                Object amn = getDefault.invoke(null, null);
                Method killApplicationProcess = amn.getClass().getDeclaredMethod("killApplicationProcess", String.class, int.class);
                mContext.stopService(new Intent().setComponent(new ComponentName("com.android.systemui", "com.android.systemui.SystemUIService")));
                am.killBackgroundProcesses("com.android.systemui");
                for (ActivityManager.RunningAppProcessInfo app : am.getRunningAppProcesses()) {
                    if ("com.android.systemui".equals(app.processName)) {
                        killApplicationProcess.invoke(amn, app.processName, app.uid);
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
