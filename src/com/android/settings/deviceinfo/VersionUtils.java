
package com.android.settings.deviceinfo;

import android.os.SystemProperties;

public class VersionUtils {
    public static String getResurrectionRemixVersion(){
        return SystemProperties.get("ro.rr.version","");
    }
}
