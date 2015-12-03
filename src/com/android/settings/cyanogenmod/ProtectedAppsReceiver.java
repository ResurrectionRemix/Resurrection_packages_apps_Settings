/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.cyanogenmod;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.util.Log;

import static cyanogenmod.content.Intent.ACTION_PROTECTED;
import static cyanogenmod.content.Intent.ACTION_PROTECTED_CHANGED;
import static cyanogenmod.content.Intent.EXTRA_PROTECTED_COMPONENTS;
import static cyanogenmod.content.Intent.EXTRA_PROTECTED_STATE;

import cyanogenmod.providers.CMSettings;

import java.util.ArrayList;
import java.util.HashSet;

public class ProtectedAppsReceiver extends BroadcastReceiver {
    private static final String TAG = "ProtectedAppsReceiver";

    private static final String PROTECTED_APP_PERMISSION = cyanogenmod.platform.Manifest
            .permission.PROTECTED_APP;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_PROTECTED.equals(intent.getAction())) {
            boolean protect = intent.getBooleanExtra(EXTRA_PROTECTED_STATE,
                    PackageManager.COMPONENT_VISIBLE_STATUS);
            ArrayList<ComponentName> components =
                    intent.getParcelableArrayListExtra(EXTRA_PROTECTED_COMPONENTS);
            if (components != null) {
                updateProtectedAppComponentsAndNotify(context, components, protect);
            }
        }
    }

    public static void updateProtectedAppComponentsAndNotify(Context context,
            ArrayList<ComponentName> components, boolean state) {
        updateProtectedAppComponents(context, components, state);
        updateSettingsSecure(context, components, state);
        notifyProtectedChanged(context, components, state);
    }

    public static void updateProtectedAppComponents(Context context,
            ArrayList<ComponentName> components, boolean state) {
        PackageManager pm = context.getPackageManager();

        for (ComponentName component : components) {
            try {
                pm.setComponentProtectedSetting(component, state);
            } catch (NoSuchMethodError nsm) {
                Log.e(TAG, "Unable to protected app via PackageManager");
            }
        }
    }

    public static void updateSettingsSecure(Context context,
            ArrayList<ComponentName> components, boolean state) {
        ContentResolver resolver = context.getContentResolver();
        String hiddenComponents = CMSettings.Secure.getString(resolver,
                CMSettings.Secure.PROTECTED_COMPONENTS);
        HashSet<ComponentName> newComponentList = new HashSet<ComponentName>();

        if (hiddenComponents != null) {
            for (String flattened : hiddenComponents.split("\\|")) {
                ComponentName cmp = ComponentName.unflattenFromString(flattened);
                if (cmp != null) {
                    newComponentList.add(cmp);
                }
            }
        }

        boolean update = state == PackageManager.COMPONENT_PROTECTED_STATUS
            ? newComponentList.addAll(components)
            : newComponentList.removeAll(components);

        if (update) {
            StringBuilder flattenedList = new StringBuilder();
            for (ComponentName cmp : newComponentList) {
                if (flattenedList.length() > 0) {
                    flattenedList.append("|");
                }
                flattenedList.append(cmp.flattenToString());
            }
            CMSettings.Secure.putString(resolver, CMSettings.Secure.PROTECTED_COMPONENTS,
                    flattenedList.toString());
        }
    }

    public static void notifyProtectedChanged(Context context,
            ArrayList<ComponentName> components, boolean state) {
        Intent intent = new Intent(ACTION_PROTECTED_CHANGED);
        intent.putExtra(EXTRA_PROTECTED_STATE, state);
        intent.putExtra(EXTRA_PROTECTED_COMPONENTS, components);

        context.sendBroadcast(intent, PROTECTED_APP_PERMISSION);
    }
}
