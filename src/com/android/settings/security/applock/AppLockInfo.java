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

import android.app.AppLockManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

public class AppLockInfo {

    private String mLabel;
    private String mPackageName;
    private Drawable mIcon;
    private boolean mAppLocked;

    AppLockInfo(ResolveInfo info, PackageManager pm, AppLockManager am) {
        mLabel = info.loadLabel(pm).toString();
        mIcon = info.loadIcon(pm);
        mPackageName = info.activityInfo.packageName;
        mAppLocked = am.isAppLocked(mPackageName);
    }

    String getLabel() {
        return mLabel;
    }

    void setLabel(String label) {
        mLabel = label;
    }

    String getPackageName() {
        return mPackageName;
    }

    boolean isAppLocked() {
        return mAppLocked;
    }

    void setAppLocked(boolean locked) {
        mAppLocked = locked;
    }

    Drawable getIcon() {
        return mIcon;
    }

    @Override
    public String toString() {
        return mLabel;
    }
}
