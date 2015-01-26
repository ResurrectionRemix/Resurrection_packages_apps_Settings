/*
 * Copyright (C) 2015 The CyanogenMod Project
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
package com.android.settings.privacyguard;

import android.app.AppOpsManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;

import com.android.settings.privacyguard.PrivacyGuardManager.AppInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * An asynchronous loader implementation that loads AppInfo structures.
 */
/* package */ class AppInfoLoader extends AsyncTaskLoader<List<AppInfo>> {
    private PackageManager mPm;
    private boolean mShowSystemApps;
    private AppOpsManager mAppOps;

    public AppInfoLoader(Context context, boolean showSystemApps) {
        super(context);
        mPm = context.getPackageManager();
        mAppOps = (AppOpsManager)context.getSystemService(Context.APP_OPS_SERVICE);
        mShowSystemApps = showSystemApps;
    }

    @Override
    public List<AppInfo> loadInBackground() {
        return loadInstalledApps();
    }

    @Override
    public void onStartLoading() {
        forceLoad();
    }

    @Override
    public void onStopLoading() {
        cancelLoad();
    }

    @Override
    protected void onReset() {
        cancelLoad();
    }

    /**
    * Uses the package manager to query for all currently installed apps
    * for the list.
    *
    * @return the complete List off installed applications (@code PrivacyGuardAppInfo)
    */
    private List<AppInfo> loadInstalledApps() {
        List<AppInfo> apps = new ArrayList<AppInfo>();
        List<PackageInfo> packages = mPm.getInstalledPackages(
            PackageManager.GET_PERMISSIONS | PackageManager.GET_SIGNATURES);

        for (PackageInfo info : packages) {
            final ApplicationInfo appInfo = info.applicationInfo;

            // skip all system apps if they shall not be included
            if (!mShowSystemApps && (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                continue;
            }

            AppInfo app = new AppInfo();
            app.title = appInfo.loadLabel(mPm).toString();
            app.packageName = info.packageName;
            app.enabled = appInfo.enabled;
            app.uid = info.applicationInfo.uid;
            app.privacyGuardEnabled = mAppOps.getPrivacyGuardSettingForPackage(
                    app.uid, app.packageName);
            apps.add(app);
        }

        // sort the apps by their enabled state, then by title
        Collections.sort(apps, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo lhs, AppInfo rhs) {
                if (lhs.enabled != rhs.enabled) {
                    return lhs.enabled ? -1 : 1;
                }
                return lhs.title.compareToIgnoreCase(rhs.title);
            }
        });

        return apps;
    }

}
