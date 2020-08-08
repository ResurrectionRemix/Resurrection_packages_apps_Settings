package com.android.settings.rr.fragments.navbar;

import android.content.pm.ActivityInfo;
import android.provider.Settings;

public class AppSwitchDoubleTap extends BackLongPress {

    @Override
    protected void setPackage(String packageName, String friendlyAppString) {
        Settings.System.putString(
                getContentResolver(), Settings.System.KEY_APP_SWITCH_DOUBLE_TAP_CUSTOM_APP, packageName);
        Settings.System.putString(
                getContentResolver(), Settings.System.KEY_APP_SWITCH_DOUBLE_TAP_CUSTOM_APP_FR_NAME,
                friendlyAppString);
    }

    @Override
    protected void setPackageActivity(ActivityInfo ai) {
        Settings.System.putString(
                getContentResolver(), Settings.System.KEY_APP_SWITCH_DOUBLE_TAP_CUSTOM_ACTIVITY,
                ai != null ? ai.name : "NONE");
    }
}

