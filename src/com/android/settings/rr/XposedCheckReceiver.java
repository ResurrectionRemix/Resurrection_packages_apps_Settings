/*
 * Copyright 2015 EXODUS - Dave Kessler <activethrasher00@gmail.com>
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

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.android.settings.R;

/*
 * Xposed exposer => a way to show when users have been submitting logcats for bugs that may
 * have been caused by using Xposed frameworks.
 */

public class XposedCheckReceiver extends BroadcastReceiver {
    private static final String TAG = "XPOSED_RECEIVER";

    // Package name
    private static final String XPOSED_PACKAGE = "de.robv.android.xposed.installer";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        Intent xposedNotifyService = new Intent(ctx, XposedNotifyService.class);
        if (isInstalled(ctx, XPOSED_PACKAGE)) {
            if (!isServiceRunning(XposedNotifyService.class, ctx)) {
                ctx.startService(xposedNotifyService);
            }
        } else {
            ctx.stopService(xposedNotifyService);
        }
    }

    private boolean isServiceRunning(Class<?> serviceClass, Context ctx) {
        ActivityManager manager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i(TAG, "Check service already running");
                return true;
            }
        }
        Log.i(TAG, "Check service not running");
        return false;
    }

    private boolean isInstalled(Context ctx, final String packageName) {
        final PackageManager pm = ctx.getPackageManager();
        String mVersion;
        try {
            mVersion = pm.getPackageInfo(packageName, 0).versionName;
            if (mVersion.equals(null)) {
                return false;
            }
        } catch (NameNotFoundException e) {
            return false;
        }
        return true;
    }
}
