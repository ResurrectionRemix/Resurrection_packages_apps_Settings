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

import android.app.AlertDialog;
import android.app.RingModeSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.profiles.actions.ItemListAdapter;

public class RingModeItem implements Item {
    RingModeSettings mSettings;

    public RingModeItem(RingModeSettings ringModeSettings) {
        if (ringModeSettings == null) {
            ringModeSettings = new RingModeSettings();
        }
        mSettings = ringModeSettings;
    }

    @Override
    public ItemListAdapter.RowType getRowType() {
        return ItemListAdapter.RowType.RINGMODE_ITEM;
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
        text.setText(R.string.ring_mode_title);

        TextView desc = (TextView) view.findViewById(R.id.summary);
        desc.setText(getModeString(mSettings));

        return view;
    }

    public static int getModeString(RingModeSettings settings) {
        if (settings == null) {
            return R.string.ring_mode_normal;
        }
        if (settings.isOverride()) {
            if (settings.getValue().equals("vibrate")) {
                return R.string.ring_mode_vibrate;
            } else if (settings.getValue().equals("normal")) {
                return R.string.ring_mode_normal;
            } else {
                return R.string.ring_mode_mute;
            }
        } else {
            return R.string.ring_mode_unchanged;
        }
    }

    public RingModeSettings getSettings() {
        return mSettings;
    }
}
