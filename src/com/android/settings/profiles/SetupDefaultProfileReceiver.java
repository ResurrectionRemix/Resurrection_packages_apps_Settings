package com.android.settings.profiles;

import android.app.Profile;
import android.app.ProfileManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

public class SetupDefaultProfileReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Settings.System.getInt(context.getContentResolver(),
                Settings.System.SYSTEM_PROFILES_ENABLED, 1) == 1) {
            ProfileManager profileManager = (ProfileManager) context
                    .getSystemService(Context.PROFILE_SERVICE);
            Profile defaultProfile = profileManager.getProfile("Default");
            if (defaultProfile != null) {
                SetupActionsFragment.fillProfileWithCurrentSettings(context, defaultProfile);
                profileManager.updateProfile(defaultProfile);
            }
        }


    }
}
