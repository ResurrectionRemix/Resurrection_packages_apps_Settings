/*
 * Copyright (C) 2013 SlimRoms
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

package com.android.settings;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.util.Slog;
import android.widget.CompoundButton;
import android.widget.Switch;
import com.android.settings.util.Helpers; 

import java.util.List;

public class TRDSEnabler implements CompoundButton.OnCheckedChangeListener {
    private final Context mContext;
    private Switch mSwitch;
    private boolean mStateMachineEvent;

    public TRDSEnabler(Context context, Switch switch_) {
        mContext = context;
        mSwitch = switch_;
    }

    public void resume() {
        mSwitch.setOnCheckedChangeListener(this);
        setSwitchState();
    }

    public void pause() {
        mSwitch.setOnCheckedChangeListener(null);
    }

    public void setSwitch(Switch switch_) {
        if (mSwitch == switch_) return;
        mSwitch.setOnCheckedChangeListener(null);
        mSwitch = switch_;
        mSwitch.setOnCheckedChangeListener(this);
        setSwitchState();
    }

    private void setSwitchState() {
        boolean enabled = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.UI_INVERTED_MODE, 0) == 1;
        mStateMachineEvent = true;
        mSwitch.setChecked(enabled);
        mStateMachineEvent = false;
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (mStateMachineEvent) {
            return;
        }
        // Handle a switch change
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.UI_INVERTED_MODE, isChecked ? 1 : 0);
        Helpers.restartSystemUI();

        ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> pids = am.getRunningAppProcesses();
           for(int i = 0; i < pids.size(); i++) {
               ActivityManager.RunningAppProcessInfo info = pids.get(i);
               if(info.processName.equalsIgnoreCase("com.android.contacts")) {
                    am.killBackgroundProcesses("com.android.contacts");
               }
               if(info.processName.equalsIgnoreCase("com.google.android.gm")) {
                    am.killBackgroundProcesses("com.google.android.gm");
               }
               if(info.processName.equalsIgnoreCase("com.android.email")) {
                    am.killBackgroundProcesses("com.android.email");
               }
               if(info.processName.equalsIgnoreCase("com.android.vending")) {
                    am.killBackgroundProcesses("com.android.vending");
               }
               if(info.processName.equalsIgnoreCase("com.google.android.talk")) {
                    am.killBackgroundProcesses("com.google.android.talk");
               }
               if(info.processName.equalsIgnoreCase("com.android.mms")) {
                    am.killBackgroundProcesses("com.android.mms");
               }
               if(info.processName.equalsIgnoreCase("com.google.android.googlequicksearchbox")) {
                    am.killBackgroundProcesses("com.google.android.googlequicksearchbox");
               }
               if(info.processName.equalsIgnoreCase("com.google.android.youtube")) {
                    am.killBackgroundProcesses("com.google.android.youtube");
               }
           }
    }

}
