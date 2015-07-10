/*
 * Copyright (C) 2015 The CyanogenMod Project
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

import cyanogenmod.profiles.BrightnessSettings;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.profiles.actions.ItemListAdapter;

public class BrightnessItem implements Item {
    BrightnessSettings mSettings;

    public BrightnessItem(BrightnessSettings brightnessSettings) {
        if (brightnessSettings == null) {
            brightnessSettings = new BrightnessSettings();
        }
        mSettings = brightnessSettings;
    }

    @Override
    public ItemListAdapter.RowType getRowType() {
        return ItemListAdapter.RowType.BRIGHTNESS_ITEM;
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
        text.setText(R.string.profile_brightness_title);

        Context context = inflater.getContext();
        TextView desc = (TextView) view.findViewById(R.id.summary);
        if (mSettings.isOverride()) {
            desc.setText(context.getResources().getString(
                    R.string.profile_brightness_override_summary,
                    (int)((mSettings.getValue() * 100f)/255)));
        } else {
            desc.setText(context.getString(R.string.profile_action_none));
        }

        return view;
    }

    public BrightnessSettings getSettings() {
        return mSettings;
    }

}
