/* Copyright (c) 2015, The Linux Foundation. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.
    * Neither the name of The Linux Foundation nor the names of its
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package com.android.settings.location;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.util.Log;

import com.android.settings.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class IzatSettingsInjector extends SettingsInjector {
    static final String TAG = "IzatSettingsInjector";
    private static final boolean PRINT_DEBUG_LOG = Log.isLoggable(TAG, Log.DEBUG);

    private static final String[] GS_PACKAGE_NAMES = {"com.google.android.gms",
        "com.google.android.location"};
    private final String IZAT_EULA_PACKAGE_NAME = "com.qualcomm.location.XT";

    private static final int GS_PRESENCE_UNKNOWN = 0;
    private static final int GS_PRESENT = 1;
    private static final int GS_NOT_PRESENT = 2;
    private static int mGsExists = GS_PRESENCE_UNKNOWN;

    public static SettingsInjector getSettingInjector(Context context) {

        if (mGsExists == GS_PRESENCE_UNKNOWN) {
            checkGsPresence(context);
        }

        if (mGsExists == GS_PRESENT) {
            return new SettingsInjector(context);
        } else {
            return new IzatSettingsInjector(context);
        }
    }

    private IzatSettingsInjector(Context context) {
        super(context);
    }

    private static void checkGsPresence(Context context) {
        mGsExists = GS_NOT_PRESENT;

        List<ApplicationInfo> packages;
        PackageManager pm = context.getPackageManager();
        packages = pm.getInstalledApplications(0);

        for (ApplicationInfo packageInfo : packages) {
            if (mGsExists != GS_PRESENT) {
                for (String packageName : GS_PACKAGE_NAMES) {
                    if (packageInfo.packageName.equals(packageName)) {
                        if (PRINT_DEBUG_LOG) Log.d(TAG, "Found GS Packages");
                        mGsExists = GS_PRESENT;
                        break;
                    }
                }
            } else {
                break;
            }
        }
    }

    @Override
    protected InjectedSetting parseServiceInfo(ResolveInfo service, UserHandle userHandle,
            PackageManager pm) throws XmlPullParserException, IOException {

        ServiceInfo si = service.serviceInfo;
        if (si.packageName.equals(IZAT_EULA_PACKAGE_NAME)) {
             return null;
        }

        return super.parseServiceInfo(service, userHandle, pm);
    }
}