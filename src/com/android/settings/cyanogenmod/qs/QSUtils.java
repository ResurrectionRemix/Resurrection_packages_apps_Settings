/*
 * Copyright (C) 2015 The CyanogenMod Project
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
package com.android.settings.cyanogenmod.qs;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.settings.R;

public class QSUtils {

    public static View parseQSTile(Context context, String tileType) {
        QSTileHolder item = getAdapterItem(context, tileType);

        View qsTile = LayoutInflater.from(context).inflate(R.layout.qs_item, null);
        if (item.getName() != null) {
            ((ImageView) qsTile.findViewById(android.R.id.icon)).setImageResource(
                    item.getDrawableId());
            ((TextView) qsTile.findViewById(android.R.id.title)).setText(item.getName());
        }
        qsTile.setTag(tileType);
        return qsTile;
    }

    public static QSTileHolder getAdapterItem(Context context, String tileType) {
        int resId = -1;
        int stringId = -1;

        if (tileType.equals("wifi")) {
            resId = R.drawable.ic_settings_wireless;
            stringId = R.string.wifi_quick_toggle_title;
        } else if (tileType.equals("bt")) {
            resId = R.drawable.ic_settings_bluetooth2;
            stringId = R.string.bluetooth_settings_title;
        } else if (tileType.equals("inversion")) {
            resId = R.drawable.ic_settings_accessibility;
            stringId = R.string.accessibility_display_inversion_preference_title;
        } else if (tileType.equals("cell")) {
            resId = R.drawable.ic_qs_signal;
            stringId = R.string.cellular_data_title;
        } else if (tileType.equals("airplane")) {
            resId = R.drawable.ic_qs_airplane;
            stringId = R.string.airplane_mode;
        } else if (tileType.equals("rotation")) {
            resId = R.drawable.ic_qs_rotation;
            stringId = R.string.display_rotation_title;
        } else if (tileType.equals("flashlight")) {
            resId = R.drawable.ic_qs_flashlight;
            stringId = R.string.power_flashlight;
        } else if (tileType.equals("location")) {
            resId = R.drawable.ic_settings_location;
            stringId = R.string.location_title;
        } else if (tileType.equals("cast")) {
            resId = R.drawable.ic_qs_cast;
            stringId = R.string.cast_screen;
        } else if (tileType.equals("hotspot")) {
            resId = R.drawable.ic_qs_hotspot;
            stringId = R.string.hotspot;
        }

        String name = null;

        if (stringId != -1) {
            name = context.getString(stringId);
        }

        QSTileHolder item = new QSTileHolder(resId, tileType, name);

        return item;
    }
}
