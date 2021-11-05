/*
 * Copyright (C) 2021 The LineageOS Project
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

package com.android.settings.fuelgauge;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.core.BasePreferenceController;

import vendor.lineage.fastcharge.V1_0.IFastCharge;

import java.util.NoSuchElementException;

/**
 * Controller to change and update the fast charging toggle
 */
public class FastChargingPreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_FAST_CHARGING = "fast_charging";
    private static final String TAG = "FastChargingPreferenceController";

    private IFastCharge mFastCharge = null;

    public FastChargingPreferenceController(Context context) {
        super(context, KEY_FAST_CHARGING);
        try {
            mFastCharge = IFastCharge.getService();
        } catch (NoSuchElementException | RemoteException e) {
            Log.e(TAG, "Failed to get IFastCharge interface", e);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return mFastCharge != null ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        boolean fastChargingEnabled = false;

        try {
            fastChargingEnabled = mFastCharge.isEnabled();
        } catch (RemoteException e) {
            Log.e(TAG, "isEnabled failed", e);
        }

        ((SwitchPreference) preference).setChecked(fastChargingEnabled);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean shouldEnableFastCharging = (Boolean) newValue;

        try {
            mFastCharge.setEnabled(shouldEnableFastCharging);
            updateState(preference);
        } catch (RemoteException e) {
            Log.e(TAG, "setEnabled failed", e);
        }

        return false;
    }
}
