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

package com.android.settings.net;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.provider.Settings;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.android.settings.WirelessSettings;

public class MobileDataEnabler implements CompoundButton.OnCheckedChangeListener {
    private final Context mContext;
    private Switch mSwitch;
    private boolean mStateMachineEvent;
    private IntentFilter mAirplaneFilter;

    public MobileDataEnabler(Context context, Switch switch_) {
        mContext = context;
        mSwitch = switch_;
        mAirplaneFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setSwitchState();
        }
    };

    public void resume() {
        mSwitch.setOnCheckedChangeListener(this);
        mContext.registerReceiver(mReceiver, mAirplaneFilter);
        setSwitchState();
    }

    public void pause() {
        mSwitch.setOnCheckedChangeListener(null);
        mContext.unregisterReceiver(mReceiver);
    }

    public void setSwitch(Switch switch_) {
        if (mSwitch == switch_)
            return;
        mSwitch.setOnCheckedChangeListener(null);
        mSwitch = switch_;
        mSwitch.setOnCheckedChangeListener(this);
        setSwitchState();
    }

    private void setSwitchState() {
        ConnectivityManager cm =
                (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        boolean airplane = WirelessSettings.isRadioAllowed(mContext, Settings.Global.RADIO_CELL);

        mStateMachineEvent = true;
        mSwitch.setChecked(cm.getMobileDataEnabled() && airplane);
        mSwitch.setEnabled(airplane);
        mStateMachineEvent = false;
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (mStateMachineEvent) {
            return;
        }
        if (!WirelessSettings.isRadioAllowed(mContext, Settings.Global.RADIO_CELL)) {
            buttonView.setChecked(false);
            return;
        }
        ConnectivityManager cm =
                (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        cm.setMobileDataEnabled(isChecked);
    }

}
