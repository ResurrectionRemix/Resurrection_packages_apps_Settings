/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.android.settings.security.applock;

import static com.android.settings.Utils.SETTINGS_PACKAGE_NAME;

import android.app.Application;
import android.app.AppLockManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.List;

public class AppLockViewModel extends AndroidViewModel {

    private final AppListLiveData mLiveData;

    public AppLockViewModel(Application application) {
        super(application);
        mLiveData = new AppListLiveData(application);
    }

    public LiveData<List<AppLockInfo>> getAppList() {
        return mLiveData;
    }
}

class AppListLiveData extends LiveData<List<AppLockInfo>> {

    private final PackageManager mPackageManager;
    private final AppLockManager mAppLockManager;
    private int mCurrentDataVersion;

    public AppListLiveData(Context context) {
        mPackageManager = context.getPackageManager();
        mAppLockManager = (AppLockManager) context.getSystemService(Context.APPLOCK_SERVICE);
        loadSupportedAppData();
    }

    void loadSupportedAppData() {
        final int dataVersion = ++mCurrentDataVersion;

        new AsyncTask<Void, Void, List<AppLockInfo>>() {
            @Override
            protected List<AppLockInfo> doInBackground(Void... voids) {
                Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
                mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

                List<ResolveInfo> apps = mPackageManager.queryIntentActivities(mainIntent,
                        PackageManager.GET_META_DATA);

                List<AppLockInfo> entries = new ArrayList<>();
                if (apps != null) {
                    for (ResolveInfo app : apps) {
                        if (app.activityInfo.packageName.equals(SETTINGS_PACKAGE_NAME)) {
                            continue;
                        }
                        AppLockInfo entry = new AppLockInfo(app, mPackageManager, mAppLockManager);
                        entries.add(entry);
                    }
                }
                return entries;
            }

            @Override
            protected void onPostExecute(List<AppLockInfo> data) {
                if (mCurrentDataVersion == dataVersion) {
                    setValue(data);
                }
            }
        }.execute();
    }
}