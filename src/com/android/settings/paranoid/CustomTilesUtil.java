/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.paranoid;

import com.android.internal.telephony.PhoneConstants;
import com.android.settings.R;

import android.content.Context;
import android.net.wimax.WimaxHelper;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

/**
 * THIS CLASS'S DATA MUST BE KEPT UP-TO-DATE WITH THE DATA IN
 * com.android.systemui.statusbar.phone.QuickSettingsController
 * IN THE SystemUI PACKAGE.
 */
public class CustomTilesUtil {
    /**
     * START OF DATA MATCHING BLOCK
     */
    public static final String TILE_USER = "toggleUser";
    public static final String TILE_BATTERY = "toggleBattery";
    public static final String TILE_SETTINGS = "toggleSettings";
    public static final String TILE_WIFI = "toggleWifi";
    public static final String TILE_GPS = "toggleGPS";
    public static final String TILE_BLUETOOTH = "toggleBluetooth";
    public static final String TILE_BRIGHTNESS = "toggleBrightness";
    public static final String TILE_SOUND = "toggleSound";
    public static final String TILE_SYNC = "toggleSync";
    public static final String TILE_WIFIAP = "toggleWifiAp";
    public static final String TILE_SCREENTIMEOUT = "toggleScreenTimeout";
    public static final String TILE_MOBILEDATA = "toggleMobileData";
    public static final String TILE_NETWORKMODE = "toggleNetworkMode";
    public static final String TILE_AUTOROTATE = "toggleAutoRotate";
    public static final String TILE_AIRPLANE = "toggleAirplane";
    public static final String TILE_FLASHLIGHT = "toggleFlashlight";
    public static final String TILE_SLEEP = "toggleSleepMode";
    public static final String TILE_LTE = "toggleLte";
    public static final String TILE_WIMAX = "toggleWimax";
    public static final String TILE_NFC = "toggleNfc";

    private static final String TILE_DELIMITER = "|";
    private static final String TILES_DEFAULT = TILE_USER
            + TILE_DELIMITER + TILE_BRIGHTNESS
            + TILE_DELIMITER + TILE_SETTINGS
            + TILE_DELIMITER + TILE_WIFI
            + TILE_DELIMITER + TILE_MOBILEDATA
            + TILE_DELIMITER + TILE_BATTERY
            + TILE_DELIMITER + TILE_AIRPLANE
            + TILE_DELIMITER + TILE_BLUETOOTH;
    /**
     * END OF DATA MATCHING BLOCK
     */

    // Keep sorted according to titleResId's string value
    public static final LinkedHashMap<String, TileInfo> TILES = new LinkedHashMap<String, TileInfo>();
    static {
        TILES.put(TILE_AIRPLANE, new CustomTilesUtil.TileInfo(
                TILE_AIRPLANE, R.string.title_tile_airplane,
                "com.android.systemui:drawable/ic_qs_airplane_off"));
        TILES.put(TILE_BATTERY, new CustomTilesUtil.TileInfo(
                TILE_BATTERY, R.string.title_tile_battery,
                "com.android.systemui:drawable/ic_qs_battery_unknown"));
        TILES.put(TILE_BLUETOOTH, new CustomTilesUtil.TileInfo(
                TILE_BLUETOOTH, R.string.title_tile_bluetooth,
                "com.android.systemui:drawable/ic_qs_bluetooth_neutral"));
        TILES.put(TILE_BRIGHTNESS, new CustomTilesUtil.TileInfo(
                TILE_BRIGHTNESS, R.string.title_tile_brightness,
                "com.android.systemui:drawable/ic_qs_brightness_auto_off"));
        TILES.put(TILE_SLEEP, new CustomTilesUtil.TileInfo(
                TILE_SLEEP, R.string.title_tile_sleep,
                "com.android.systemui:drawable/ic_qs_sleep"));
        TILES.put(TILE_GPS, new CustomTilesUtil.TileInfo(
                TILE_GPS, R.string.title_tile_gps,
                "com.android.systemui:drawable/ic_qs_gps_neutral"));
        TILES.put(TILE_MOBILEDATA, new CustomTilesUtil.TileInfo(
                TILE_MOBILEDATA, R.string.title_tile_mobiledata,
                "com.android.systemui:drawable/ic_qs_signal_4"));
        TILES.put(TILE_NETWORKMODE, new CustomTilesUtil.TileInfo(
                TILE_NETWORKMODE, R.string.title_tile_networkmode,
                "com.android.systemui:drawable/stat_2g3g_on"));
        TILES.put(TILE_AUTOROTATE, new CustomTilesUtil.TileInfo(
                TILE_AUTOROTATE, R.string.title_tile_autorotate,
                "com.android.systemui:drawable/ic_qs_auto_rotate"));
        TILES.put(TILE_SETTINGS, new CustomTilesUtil.TileInfo(
                TILE_SETTINGS, R.string.title_tile_settings,
                "com.android.systemui:drawable/ic_qs_settings"));
        TILES.put(TILE_SOUND, new CustomTilesUtil.TileInfo(
                TILE_SOUND, R.string.title_tile_sound,
                "com.android.systemui:drawable/ic_qs_ring_on"));
        TILES.put(TILE_SYNC, new CustomTilesUtil.TileInfo(
                TILE_SYNC, R.string.title_tile_sync,
                "com.android.systemui:drawable/ic_qs_sync_off"));
        TILES.put(TILE_FLASHLIGHT, new CustomTilesUtil.TileInfo(
                TILE_FLASHLIGHT, R.string.title_tile_flashlight,
                "com.android.systemui:drawable/ic_qs_flashlight_off"));
        TILES.put(TILE_WIFI, new CustomTilesUtil.TileInfo(
                TILE_WIFI, R.string.title_tile_wifi,
                "com.android.systemui:drawable/ic_qs_wifi_4"));
        TILES.put(TILE_WIFIAP, new CustomTilesUtil.TileInfo(
                TILE_WIFIAP, R.string.title_tile_wifiap,
                "com.android.systemui:drawable/ic_qs_wifi_ap_neutral"));
        TILES.put(TILE_USER, new CustomTilesUtil.TileInfo(
                TILE_USER, R.string.title_tile_user,
                "com.android.systemui:drawable/ic_qs_default_user"));
        TILES.put(TILE_NFC, new CustomTilesUtil.TileInfo(
                TILE_NFC, R.string.title_tile_nfc,
                "com.android.systemui:drawable/ic_qs_nfc_off"));

/* These toggles are not available yet.  Comment out for now
        TILES.put(TILE_SCREENTIMEOUT, new CustomTilesUtil.TileInfo(
                TILE_SCREENTIMEOUT, R.string.title_tile_screentimeout,
                "com.android.systemui:drawable/stat_screen_timeout_on"));
        if(PhoneConstants.LTE_ON_CDMA_TRUE == TelephonyManager.getDefault().getLteOnCdmaMode() ||
           TelephonyManager.getDefault().getLteOnGsmMode() != 0) {
            TILES.put(TILE_LTE, new CustomTilesUtil.TileInfo(
                    TILE_LTE, R.string.title_tile_lte,
                    "com.android.systemui:drawable/stat_lte_on"));
       }
       TILES.put(TILE_WIMAX, new CustomTilesUtil.TileInfo(
                TILE_WIMAX, R.string.title_tile_wimax,
                "com.android.systemui:drawable/stat_wimax_on"));
*/
    }

    public static String getCurrentTiles(Context context) {
        String tiles = Settings.System.getString(context.getContentResolver(),
                Settings.System.QUICK_SETTINGS_TILES);
        if (tiles == null) {
            tiles = TILES_DEFAULT;
        }
        return tiles;
    }

    public static void saveCurrentTiles(Context context, String tiles) {
        Settings.System.putString(context.getContentResolver(),
                Settings.System.QUICK_SETTINGS_TILES, tiles);
    }

    public static void resetTiles(Context context) {
        Settings.System.putString(context.getContentResolver(),
                Settings.System.QUICK_SETTINGS_TILES, TILES_DEFAULT);
    }

    public static String mergeInNewTileString(String oldString, String newString) {
        ArrayList<String> oldList = getTileListFromString(oldString);
        ArrayList<String> newList = getTileListFromString(newString);
        ArrayList<String> mergedList = new ArrayList<String>();

        // add any items from oldlist that are in new list
        for (String tile : oldList) {
            if (newList.contains(tile)) {
                mergedList.add(tile);
            }
        }

        // append anything in newlist that isn't already in the merged list to
        // the end of the list
        for (String tile : newList) {
            if (!mergedList.contains(tile)) {
                mergedList.add(tile);
            }
        }

        // return merged list
        return getTileStringFromList(mergedList);
    }

    public static ArrayList<String> getTileListFromString(String tiles) {
        return new ArrayList<String>(Arrays.asList(tiles.split("\\|")));
    }

    public static String getTileStringFromList(ArrayList<String> tiles) {
        if (tiles == null || tiles.size() <= 0) {
            return "";
        } else {
            String s = tiles.get(0);
            for (int i = 1; i < tiles.size(); i++) {
                s += TILE_DELIMITER + tiles.get(i);
            }
            return s;
        }
    }

    public static class TileInfo {
        private String mId;
        private int mTitleResId;
        private String mIcon;

        public TileInfo(String id, int titleResId, String icon) {
            mId = id;
            mTitleResId = titleResId;
            mIcon = icon;
        }

        public String getId() {
            return mId;
        }

        public int getTitleResId() {
            return mTitleResId;
        }

        public String getIcon() {
            return mIcon;
        }
    }
}
