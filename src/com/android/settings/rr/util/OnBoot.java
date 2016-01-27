package com.android.settings.rr.util;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import com.android.internal.logging.MetricsLogger;
import android.widget.Toast;
import java.io.IOException;
import java.util.List;

import com.android.settings.util.CMDProcessor;

public class OnBoot extends BroadcastReceiver {

    Context settingsContext = null;
    private static final String TAG = "RR_onboot";
    Boolean mSetupRunning = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();
        for(int i = 0; i < procInfos.size(); i++)
        {
            if(procInfos.get(i).processName.equals("com.google.android.setupwizard")) {
                mSetupRunning = true;
            }
  if(procInfos.get(i).processName.equals("com.cyanogenmod.setupwizard")) {
                mSetupRunning = true;
            }
        }
        if(!mSetupRunning) {
             SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
             if(sharedpreferences.getBoolean("selinux", true)) {
                 CMDProcessor.runSuCommand("setenforce 1");
             } else if (!sharedpreferences.getBoolean("selinux", true)) {
                 CMDProcessor.runSuCommand("setenforce 0");
             }
        }
    }

    protected int getMetricsCategory()
    {
	return MetricsLogger.APPLICATION;
    }
}
