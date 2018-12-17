/*
 * Copyright (C) 2016 The CyanogenMod project
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
package com.android.settings.rr.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.RILConstants;

/**
 * Helper class which has the same logic as MobileNetworkSettings to display the same
 * network modes and strings as it does.
 */
public class TelephonyUtils {

    private static final String TAG = TelephonyUtils.class.getSimpleName();

    // from MobileNetworkSettings
    public static final String ACTION_PICK_NETWORK_MODE =
            "lineageos.platform.intent.action.NETWORK_MODE_PICKER";
    public static final String EXTRA_NONE_TEXT = "network_mode_picker::neutral_text";
    public static final String EXTRA_SHOW_NONE = "network_mode_picker::show_none";
    public static final String EXTRA_INITIAL_NETWORK_VALUE = "network_mode_picker::selected_mode";
    public static final String EXTRA_NETWORK_PICKER_PICKED_VALUE =
            "network_mode_picker::chosen_value";
    public static final String EXTRA_SUBID = "network_mode_picker::sub_id";

    /**
     * Returns whether the device is voice-capable (meaning, it is also a phone).
     */
    public static boolean isVoiceCapable(Context context) {
        TelephonyManager telephony =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return telephony != null && telephony.isVoiceCapable();
    }

    public static String getNetworkModeString(Context context, int networkMode, int subId) {
        return getNetworkModeString(context,
                networkMode,
                TelephonyManager.from(context).getCurrentPhoneType(subId) /* phone type */,
                show4GForLTE(context)/* show 4G for lte */,
                isSupportTdscdma(context, subId)/* supports TDS CDMA*/,
                isGlobalCDMA(context, subId, isLteOnCdma(context, subId))/* is Global cdma */,
                isWorldMode(context)/* is worldwide */);
    }

    public static String getNetworkModeString(Context context, int networkMode,
            int phoneType, boolean show4GForLTE, boolean isSupportTdsCdma, boolean isGlobalCdma,
            boolean isWorldMode) {
        String r = null;
        switch (networkMode) {
            case RILConstants.NETWORK_MODE_TDSCDMA_WCDMA:
            case RILConstants.NETWORK_MODE_TDSCDMA_GSM_WCDMA:
            case RILConstants.NETWORK_MODE_TDSCDMA_GSM:
            case RILConstants.NETWORK_MODE_WCDMA_ONLY:
            case RILConstants.NETWORK_MODE_GSM_UMTS:
            case RILConstants.NETWORK_MODE_WCDMA_PREF:
            case RILConstants.NETWORK_MODE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
            case RILConstants.NETWORK_MODE_CDMA:
            case RILConstants.NETWORK_MODE_EVDO_NO_CDMA:
            case RILConstants.NETWORK_MODE_GLOBAL:
                r = "network_3G";
                break;
            case RILConstants.NETWORK_MODE_GSM_ONLY:
                r = "network_2G";
                break;
            case RILConstants.NETWORK_MODE_LTE_GSM_WCDMA:
            case RILConstants.NETWORK_MODE_LTE_WCDMA:
            case RILConstants.NETWORK_MODE_LTE_ONLY:
            case RILConstants.NETWORK_MODE_LTE_CDMA_EVDO:
                r = (show4GForLTE)
                        ? "network_4G" : "network_lte";
                break;
            case RILConstants.NETWORK_MODE_CDMA_NO_EVDO:
                r = "network_1x";
                break;
            case RILConstants.NETWORK_MODE_TDSCDMA_ONLY:
                r = "network_tdscdma";
                break;
            case RILConstants.NETWORK_MODE_LTE_TDSCDMA_GSM:
            case RILConstants.NETWORK_MODE_LTE_TDSCDMA_GSM_WCDMA:
            case RILConstants.NETWORK_MODE_LTE_TDSCDMA:
            case RILConstants.NETWORK_MODE_LTE_TDSCDMA_WCDMA:
            case RILConstants.NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
            case RILConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                if (isSupportTdsCdma) {
                    r = "network_lte";
                } else {
                    if (phoneType == RILConstants.CDMA_PHONE || isGlobalCdma || isWorldMode) {
                        r = "network_global";
                    } else {
                        r = (show4GForLTE)
                                ? "network_4G" : "network_lte";
                    }
                }
                break;
            default:
                Log.w(TAG, "unknown phone mode: " + networkMode);
        }

        if (r != null) {
            // grab the phone resources
            final Resources phoneResources = getPhoneResources(context);
            if (phoneResources != null) {
                int id = phoneResources.getIdentifier(r, "string", "com.android.phone");
                if (id > 0) {
                    return phoneResources.getString(id);
                } else {
                    Log.w(TAG, "couldn't find resource id with name: " + r);
                }
            }
        }
        return null;
    }

    private static boolean isSupportTdscdma(Context context, int subId) {
        final Resources phoneResources = getPhoneResources(context);
        if (phoneResources != null) {
            int id = phoneResources.getIdentifier("config_support_tdscdma",
                    "bool", "com.android.phone");
            if (phoneResources.getBoolean(id)) {
                return true;
            }

            final String operatorNumeric = TelephonyManager.from(context)
                    .getSimOperatorNumeric(subId);

            int tdcdmaArrId = phoneResources.getIdentifier("config_support_tdscdma_roaming_on_networks",
                    "string-array", "com.android.phone");

            if (tdcdmaArrId > 0) {
                String[] numericArray = phoneResources.getStringArray(tdcdmaArrId);
                if (numericArray.length == 0 || operatorNumeric == null) {
                    return false;
                }
                for (String numeric : numericArray) {
                    if (operatorNumeric.equals(numeric)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean show4GForLTE(Context context) {
        try {
            Context con = context.createPackageContext("com.android.systemui", 0);
            int id = con.getResources().getIdentifier("config_show4GForLTE",
                    "bool", "com.android.systemui");
            return con.getResources().getBoolean(id);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private static boolean isGlobalCDMA(Context context, int subId, boolean isLteOnCdma) {
        final CarrierConfigManager carrierConfigMan = (CarrierConfigManager)
                context.getSystemService(Context.CARRIER_CONFIG_SERVICE);
        final PersistableBundle carrierConfig = carrierConfigMan.getConfigForSubId(subId);
        return isLteOnCdma
                && carrierConfig.getBoolean(CarrierConfigManager.KEY_SHOW_CDMA_CHOICES_BOOL);
    }

    private static boolean isLteOnCdma(Context context, int subId) {
        return TelephonyManager.from(context).getLteOnCdmaMode(subId)
                == PhoneConstants.LTE_ON_CDMA_TRUE;
    }

    private static boolean isWorldMode(Context context) {
        boolean worldModeOn = false;
        final TelephonyManager tm = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);

        Resources phoneResources = getPhoneResources(context);
        if (phoneResources != null) {
            int id = phoneResources.getIdentifier("config_world_mode",
                    "string", "com.android.phone");

            if (id > 0) {
                final String configString = phoneResources.getString(id);

                if (!TextUtils.isEmpty(configString)) {
                    String[] configArray = configString.split(";");
                    // Check if we have World mode configuration set to True only or config is set to True
                    // and SIM GID value is also set and matches to the current SIM GID.
                    if (configArray != null &&
                            ((configArray.length == 1 && configArray[0].equalsIgnoreCase("true")) ||
                                    (configArray.length == 2 && !TextUtils.isEmpty(configArray[1]) &&
                                            tm != null && configArray[1].equalsIgnoreCase(tm.getGroupIdLevel1())))) {
                        worldModeOn = true;
                    }
                }
            } else {
                Log.w(TAG, "couldn't find resource of config_world_mode");
            }
        }

        return worldModeOn;
    }

    private static Resources getPhoneResources(Context context) {
        try {
            final Context packageContext = context.createPackageContext("com.android.phone", 0);
            return packageContext.getResources();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        Log.w(TAG, "couldn't locate resources for com.android.phone!");
        return null;
    }
}
