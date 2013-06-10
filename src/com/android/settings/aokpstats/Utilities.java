/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.android.settings.aokpstats;

import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import java.math.BigInteger;
import java.net.NetworkInterface;
import java.security.MessageDigest;

public class Utilities {
    public static String getUniqueID(Context ctx) {
        String device_id = null;
        String wifiInterface = SystemProperties.get("wifi.interface");
        try {
            String wifiMac = new String(NetworkInterface.getByName(
                    wifiInterface).getHardwareAddress());
            device_id = digest(wifiMac);
        } catch (Exception e) {
        }
        return device_id;
    }

    public static String getCarrier(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String carrier = tm.getNetworkOperatorName();
        if (TextUtils.isEmpty(carrier)) {
            carrier = "Unknown";
        }
        return carrier;
    }

    public static String getCarrierId(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String carrierId = tm.getNetworkOperator();
        if (TextUtils.isEmpty(carrierId)) {
            carrierId = "0";
        }
        return carrierId;
    }

    public static String getCountryCode(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String countryCode = tm.getNetworkCountryIso();
        if (TextUtils.isEmpty(countryCode)) {
            countryCode = "Unknown";
        }
        return countryCode;
    }

    public static String getDevice() {
        String device = SystemProperties.get("ro.aokp.device");
        return device == null ? SystemProperties.get("ro.product.device") : device;
    }

    public static String getModVersion() {
        String version = SystemProperties.get("ro.aokp.version");
        String branch = SystemProperties.get("ro.aokp.branch");
        if (version == null || branch == null || !version.startsWith("aokp")) {
            return "KANG";
        } else {
            String[] splitVer = version.split("_");
            if (version.contains("milestone")) {
                return branch + "_" + splitVer[3]; // exact milestone version
            } else {
                return branch + "_" + splitVer[2]; // nightly || unofficial
            }
        }
    }

    public static String digest(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return new BigInteger(1, md.digest(input.getBytes())).toString(16).toUpperCase();
        } catch (Exception e) {
            return null;
        }
    }
}
