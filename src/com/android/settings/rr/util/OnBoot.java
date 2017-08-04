package com.android.settings.rr.util;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.os.SystemProperties;
import android.util.Log;
import java.util.List;

import com.android.settings.util.CMDProcessor;

public class OnBoot extends BroadcastReceiver {

    private static final String TAG = "RR_onboot";
    private static final String ROOT_ACCESS_PROPERTY = "persist.sys.root_access";
    private boolean mSetupRunning = false;
    private Context mContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();
        for(int i = 0; i < procInfos.size(); i++) {
            if(procInfos.get(i).processName.equals("com.google.android.setupwizard")) {
                mSetupRunning = true;
            }
        }
        // Magisk Manager
        boolean magiskSupported = false;
        // SuperSU
        boolean suSupported = false;
        try {
            magiskSupported = (context.getPackageManager().getPackageInfo("com.topjohnwu.magisk", 0).versionCode > 0);
        } catch (PackageManager.NameNotFoundException e) {
        }
        try {
            suSupported = (context.getPackageManager().getPackageInfo("eu.chainfire.supersu", 0).versionCode >= 185);
        } catch (PackageManager.NameNotFoundException e) {
        }
        if (magiskSupported || suSupported || isRootForAppsEnabled()) {
            if(!mSetupRunning) {
               SharedPreferences sharedpreferences = context.getSharedPreferences("selinux_pref", Context.MODE_PRIVATE);
               String isSelinuxEnforcing = sharedpreferences.getString("selinux", null);
               if (isSelinuxEnforcing != null) {
                   if (isSelinuxEnforcing.equals("true")) {
                       CMDProcessor.runShellCommand("setenforce 1");
                   } else if (isSelinuxEnforcing.equals("false")) {
                       CMDProcessor.runSuCommand("setenforce 0");
                   }
               } else {
                  if (CMDProcessor.runShellCommand("getenforce").getStdout().contains("Enforcing")) {
                      setSelinuxEnabled("true");
                  } else {
                      setSelinuxEnabled("false");
                  }
              }
          }
       }     
    }

    public static boolean isRootForAppsEnabled() {
        int value = SystemProperties.getInt(ROOT_ACCESS_PROPERTY, 0);
        boolean daemonState =
                SystemProperties.get("init.svc.su_daemon", "absent").equals("running");
        return daemonState && (value == 1 || value == 3);
    }

    private void setSelinuxEnabled(String status) {
        SharedPreferences.Editor editor = mContext.getSharedPreferences("selinux_pref", Context.MODE_PRIVATE).edit();
        editor.putString("selinux", status);
        editor.apply();
    }
}
