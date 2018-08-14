/*
 *Copyright (c) 2018, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted (subject to the limitations in the
 * disclaimer below) provided that the following conditions are met:
 *
 *    * Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *
 *    * Redistributions in binary form must reproduce the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer in the documentation and/or other materials provided
 *      with the distribution.
 *
 *    * Neither the name of The Linux Foundation nor the names of its
 *      contributors may be used to endorse or promote products derived
 *      from this software without specific prior written permission.

 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE
 * GRANTED BY THIS LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT
 * HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.connecteddevice;

import android.content.pm.PackageManager;
import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.bluetooth.BluetoothDeviceUpdater;
import com.android.settings.bluetooth.SavedBluetoothTwsDeviceUpdater;
import com.android.settings.connecteddevice.dock.DockUpdater;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.DockUpdaterFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/**
 * Controller to maintain the {@link PreferenceGroup} for all
 * saved TWS+ devices. It uses {@link DevicePreferenceCallback} to add/remove {@link Preference}
 */
public class SavedTwsDeviceGroupController extends BasePreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnStart, OnStop,
        DevicePreferenceCallback {

    private static final String KEY = "saved_tws_device_list";

    PreferenceGroup mPreferenceGroup;
    private BluetoothDeviceUpdater mBluetoothDeviceUpdater;
    private DockUpdater mSavedDockUpdater;

    public SavedTwsDeviceGroupController(Context context) {
        super(context, KEY);

        DockUpdaterFeatureProvider dockUpdaterFeatureProvider =
                FeatureFactory.getFactory(context).getDockUpdaterFeatureProvider();
        mSavedDockUpdater =
                dockUpdaterFeatureProvider.getSavedDockUpdater(context, this);
    }

    @Override
    public void onStart() {
        mBluetoothDeviceUpdater.registerCallback();
        mSavedDockUpdater.registerCallback();
    }

    @Override
    public void onStop() {
        mBluetoothDeviceUpdater.unregisterCallback();
        mSavedDockUpdater.unregisterCallback();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        if (isAvailable()) {
            mPreferenceGroup = (PreferenceGroup) screen.findPreference(KEY);
            mPreferenceGroup.setVisible(false);
            mBluetoothDeviceUpdater.setPrefContext(screen.getContext());
            mBluetoothDeviceUpdater.forceUpdate();
            mSavedDockUpdater.forceUpdate();
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void onDeviceAdded(Preference preference) {
        if (mPreferenceGroup.getPreferenceCount() == 0) {
            mPreferenceGroup.setVisible(true);
        }
        mPreferenceGroup.addPreference(preference);
    }

    @Override
    public void onDeviceRemoved(Preference preference) {
        mPreferenceGroup.removePreference(preference);
        if (mPreferenceGroup.getPreferenceCount() == 0) {
            mPreferenceGroup.setVisible(false);
        }
    }

    public void init(DashboardFragment fragment) {
        mBluetoothDeviceUpdater = new SavedBluetoothTwsDeviceUpdater(fragment.getContext(),
                fragment, SavedTwsDeviceGroupController.this);
    }

    public void setBluetoothDeviceUpdater(BluetoothDeviceUpdater bluetoothDeviceUpdater) {
        mBluetoothDeviceUpdater = bluetoothDeviceUpdater;
    }

    public void setSavedDockUpdater(DockUpdater savedDockUpdater) {
        mSavedDockUpdater = savedDockUpdater;
    }
}
