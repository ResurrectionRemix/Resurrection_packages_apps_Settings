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

import android.app.NotificationGroup;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import cyanogenmod.app.Profile;
import cyanogenmod.app.ProfileGroup;
import cyanogenmod.app.ProfileManager;

import com.android.settings.R;
import com.android.settings.profiles.actions.ItemListAdapter;

import java.util.UUID;

public class AppGroupItem implements Item {
    Profile mProfile;
    ProfileGroup mGroup;
    NotificationGroup mNotifGroup;

    public AppGroupItem() {
        // empty app group will act as a "Add/remove app groups" item
    }

    public AppGroupItem(Profile profile, ProfileGroup group, NotificationGroup nGroup) {
        mProfile = profile;
        if (group == null) {
            throw new UnsupportedOperationException("profile group can't be null");
        }
        mGroup = group;
        mNotifGroup = nGroup;
    }

    @Override
    public ItemListAdapter.RowType getRowType() {
        return ItemListAdapter.RowType.APP_GROUP_ITEM;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public UUID getGroupUuid() {
        if (mGroup != null) {
            return mGroup.getUuid();
        }
        return null;
    }

    @Override
    public View getView(LayoutInflater inflater, View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = inflater.inflate(R.layout.list_two_line_item, parent, false);
        } else {
            view = convertView;
        }
        TextView text = (TextView) view.findViewById(R.id.title);
        TextView desc = (TextView) view.findViewById(R.id.summary);

        if (mGroup != null) {
            if (mNotifGroup != null) {
                text.setText(mNotifGroup.getName());
            } else {
                text.setText("<unknown>");
            }
            desc.setVisibility(View.GONE);
        } else {
            text.setText(R.string.profile_app_group_item_instructions);
            desc.setText(R.string.profile_app_group_item_instructions_summary);

            desc.setVisibility(View.VISIBLE);
        }

        return view;
    }
}
