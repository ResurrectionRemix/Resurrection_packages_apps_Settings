
package com.android.settings.util;

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

    public static boolean isPackageInstalled(String packageName, PackageManager pm) {
        try {
            String mVersion = pm.getPackageInfo(packageName, 0).versionName;
            if (mVersion == null) {
                return false;
            }
        } catch (NameNotFoundException notFound) {
            Log.i(TAG, "Package could not be found!", notFound);
            return false;
        }
        return true;
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

    public static String readStringFromFile(File inputFile ) throws IOException {
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
    // DelayCallback usage
    int secs = 2; // Delay in seconds
    Helpers.delay(secs, new Utils.DelayCallback() {
        @Override
        public void afterDelay() {
            // Do something after delay
        }
    });
    */

    public interface DelayCallback{
        void afterDelay();
    }

    public static void delay(int secs, final DelayCallback delayCallback){
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                delayCallback.afterDelay();
            }
        }, secs * 1000);
    }
}
