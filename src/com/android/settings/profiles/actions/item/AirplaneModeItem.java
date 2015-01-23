/*
 * Copyright (C) 2014 The CyanogenMod Project
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
package com.android.settings.profiles.actions.item;

import android.app.AirplaneModeSettings;
import android.app.RingModeSettings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.profiles.actions.ItemListAdapter;

public class AirplaneModeItem implements Item {
    AirplaneModeSettings mSettings;

    public AirplaneModeItem(AirplaneModeSettings airplaneModeSettings) {
        if (airplaneModeSettings == null) {
            airplaneModeSettings = new AirplaneModeSettings();
        }
        mSettings = airplaneModeSettings;
    }

    @Override
    public ItemListAdapter.RowType getRowType() {
        return ItemListAdapter.RowType.AIRPLANEMODE_ITEM;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public View getView(LayoutInflater inflater, View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = inflater.inflate(R.layout.list_two_line_item, parent, false);
            // Do some initialization
        } else {
            view = convertView;
        }

        TextView text = (TextView) view.findViewById(R.id.title);
        text.setText(R.string.profile_airplanemode_title);

        TextView desc = (TextView) view.findViewById(R.id.summary);
        desc.setText(getModeString(mSettings));

        return view;
    }

    public AirplaneModeSettings getSettings() {
        return mSettings;
    }

    public static int getModeString(AirplaneModeSettings settings) {
        if (settings.isOverride()) {
            if (settings.getValue() == 1) {
                return R.string.profile_action_enable;
            } else {
                return R.string.profile_action_disable;
            }
        } else {
            return R.string.profile_action_none;
        }
    }
}
