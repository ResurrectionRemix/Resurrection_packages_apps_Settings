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
package com.android.settings.profiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import cyanogenmod.app.Profile;
import cyanogenmod.app.ProfileManager;
import cyanogenmod.providers.CMSettings;

import java.util.UUID;

public class SetupDefaultProfileReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (CMSettings.System.getInt(context.getContentResolver(),
                CMSettings.System.SYSTEM_PROFILES_ENABLED, 1) == 1) {
            ProfileManager profileManager = ProfileManager.getInstance(context);
            Profile defaultProfile = profileManager.getProfile(
                    UUID.fromString("0230226d-0d05-494a-a9bd-d222a1117655"));
            if (defaultProfile != null) {
                SetupActionsFragment.fillProfileWithCurrentSettings(context, defaultProfile);
                profileManager.updateProfile(defaultProfile);
            }
        }
    }
}
