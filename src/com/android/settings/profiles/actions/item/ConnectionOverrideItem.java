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

import android.app.ConnectionSettings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.profiles.actions.ItemListAdapter;

public class ConnectionOverrideItem implements Item {
    int mConnectionId;
    ConnectionSettings mConnectionSettings;

    public static final int CM_MODE_2G = 0;
    public static final int CM_MODE_3G = 1;
    public static final int CM_MODE_4G = 2;
    public static final int CM_MODE_2G3G = 3;
    public static final int CM_MODE_ALL = 4;
    public static final int CM_MODE_UNCHANGED = 5;

    public ConnectionOverrideItem(int connectionId, ConnectionSettings settings) {
        mConnectionId = connectionId;
        if (settings == null) {
            settings = new ConnectionSettings(connectionId);
        }
        this.mConnectionSettings = settings;
    }

    @Override
    public ItemListAdapter.RowType getRowType() {
        return ItemListAdapter.RowType.CONNECTION_ITEM;
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

        TextView title = (TextView) view.findViewById(R.id.title);
        TextView summary = (TextView) view.findViewById(R.id.summary);

        title.setText(getConnectionTitle(mConnectionId));
        summary.setText(getSummary());

        return view;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public static int getConnectionTitle(int connectionId) {
        switch (connectionId) {
            case ConnectionSettings.PROFILE_CONNECTION_BLUETOOTH:
                return R.string.toggleBluetooth;
            case ConnectionSettings.PROFILE_CONNECTION_MOBILEDATA:
                return R.string.toggleData;
            case ConnectionSettings.PROFILE_CONNECTION_2G3G4G:
                return R.string.toggle2g3g4g;
            case ConnectionSettings.PROFILE_CONNECTION_GPS:
                return R.string.toggleGPS;
            case ConnectionSettings.PROFILE_CONNECTION_NFC:
                return R.string.toggleNfc;
            case ConnectionSettings.PROFILE_CONNECTION_SYNC:
                return R.string.toggleSync;
            case ConnectionSettings.PROFILE_CONNECTION_WIFI:
                return R.string.toggleWifi;
            case ConnectionSettings.PROFILE_CONNECTION_WIFIAP:
                return R.string.toggleWifiAp;
            default:
                return 0;
        }
    }

    public int getSummary() {
        if (mConnectionSettings != null) {
            if (mConnectionId == ConnectionSettings.PROFILE_CONNECTION_2G3G4G) { // different options
                if (mConnectionSettings.isOverride()) {
                    switch (mConnectionSettings.getValue()) {
                        case CM_MODE_2G:
                            return R.string.profile_networkmode_2g;
                        case CM_MODE_3G:
                            return R.string.profile_networkmode_3g;
                        case CM_MODE_4G:
                            return R.string.profile_networkmode_4g;
                        case CM_MODE_2G3G:
                            return R.string.profile_networkmode_2g3g;
                        default:
                        case CM_MODE_ALL:
                            return R.string.profile_networkmode_2g3g4g;
                    }
                } else {
                    return R.string.profile_action_none;
                }
            } else if (mConnectionSettings.isOverride()) { // enabled, disabled, or none
                if (mConnectionSettings.getValue() == 1) {
                    return R.string.profile_action_enable;
                } else {
                    return R.string.profile_action_disable;
                }
            } else {
                return R.string.profile_action_none;
            }
        } else {
            return R.string.profile_action_none;
        }
    }

    public ConnectionSettings getSettings() {
        return mConnectionSettings;
    }

    public int getConnectionType() {
        return mConnectionId;
    }
}
