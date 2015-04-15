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

import android.app.Profile;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.profiles.actions.ItemListAdapter;

public class DozeModeItem implements Item {
    Profile mProfile;

    public DozeModeItem(Profile profile) {
       mProfile = profile;
    }

    @Override
    public ItemListAdapter.RowType getRowType() {
        return ItemListAdapter.RowType.DOZEMODE_ITEM;
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
        text.setText(R.string.doze_title);

        TextView desc = (TextView) view.findViewById(R.id.summary);
        desc.setText(getSummaryString(mProfile));

        return view;
    }

    public static int getSummaryString(Profile profile) {
        switch (profile.getDozeMode()) {
            case Profile.DozeMode.DEFAULT:
                return R.string.profile_action_none; //"leave unchanged"
            case Profile.DozeMode.ENABLE:
                return R.string.profile_action_enable;
            case Profile.DozeMode.DISABLE:
                return R.string.profile_action_disable;
            default: return 0;
        }
    }
}
