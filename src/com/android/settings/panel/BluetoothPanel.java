/*
 * Copyright (C) 2019 The Android Open Source Project
 * Copyright (C) 2020 CarbonROM
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

package com.android.settings.panel;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.android.settings.R;
import com.android.settings.SubSettings;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.slices.SliceBuilderUtils;
import com.android.settings.connecteddevice.ConnectedDeviceDashboardFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Panel data class for Bluetooth settings.
 */
public class BluetoothPanel implements PanelContent {

    private final Context mContext;

    public static BluetoothPanel create(Context context) {
        return new BluetoothPanel(context);
    }

    private BluetoothPanel(Context context) {
        mContext = context.getApplicationContext();
    }

    @Override
    public CharSequence getTitle() {
        return mContext.getText(R.string.bluetooth_settings);
    }

    @Override
    public List<Uri> getSlices() {
        final List<Uri> uris = new ArrayList<>();
        uris.add(CustomSliceRegistry.BLUETOOTH_URI);
        uris.add(CustomSliceRegistry.BLUETOOTH_DEVICES_SLICE_URI);
        return uris;
    }

    @Override
    public Intent getSeeMoreIntent() {
        final String screenTitle =
                mContext.getText(R.string.bluetooth_settings).toString();
        final Intent intent = SliceBuilderUtils.buildSearchResultPageIntent(mContext,
                ConnectedDeviceDashboardFragment.class.getName(),
                null /* key */,
                screenTitle,
                SettingsEnums.SETTINGS_CONNECTED_DEVICE_CATEGORY);
        intent.setClassName(mContext.getPackageName(), SubSettings.class.getName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PANEL_BLUETOOTH;
    }
}
