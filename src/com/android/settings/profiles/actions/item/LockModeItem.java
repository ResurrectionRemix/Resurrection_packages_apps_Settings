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

public class LockModeItem implements Item {
    Profile mProfile;

    public LockModeItem(Profile profile) {
       mProfile = profile;
    }

    @Override
    public ItemListAdapter.RowType getRowType() {
        return ItemListAdapter.RowType.LOCKSCREENMODE_ITEM;
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
        text.setText(R.string.profile_lockmode_title);

        TextView desc = (TextView) view.findViewById(R.id.summary);
        desc.setText(getSummaryString(mProfile));

        return view;
    }

    public static int getSummaryString(Profile profile) {
        switch (profile.getScreenLockMode()) {
            case Profile.LockMode.DEFAULT:
                return R.string.profile_action_none; //"leave unchanged"
            case Profile.LockMode.DISABLE:
                return R.string.profile_lockmode_disabled_summary;
            case Profile.LockMode.INSECURE:
                return R.string.profile_lockmode_insecure_summary;
            default: return 0;
        }
    }
}
