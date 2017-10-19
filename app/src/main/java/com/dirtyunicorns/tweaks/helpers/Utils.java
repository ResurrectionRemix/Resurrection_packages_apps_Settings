/*
 * Copyright (C) 2017 The Dirty Unicorns Project
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

package com.dirtyunicorns.tweaks.helpers;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.fingerprint.FingerprintManager;
import android.support.v4.app.ActivityCompat;

public class Utils {

    public static boolean isPackageInstalled(String packagename, PackageManager packageManager) {
        try {
            packageManager.getPackageInfo(packagename, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static int tint(int color, double factor) {
        int a = Color.alpha(color);
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);

        return Color.argb(a, Math.max((int) (r * factor), 0), Math.max((int) (g * factor), 0), Math.max((int) (b * factor), 0));
    }

    public static boolean hasDeviceFingerprintSupport(Context context) {
        FingerprintManager fingerprintManager = (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT) == PackageManager.PERMISSION_GRANTED &&
                (fingerprintManager != null && fingerprintManager.isHardwareDetected()) && fingerprintManager.hasEnrolledFingerprints();
    }
}
