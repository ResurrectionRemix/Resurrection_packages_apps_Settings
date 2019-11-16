/*
 * Copyright (c) 2019 crDroid Android Project
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

package com.android.settings.rr.fragments;


import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.internal.util.rr.GamingModeController;

public class GamingModeReceiver extends BroadcastReceiver {

    private static final String TAG = "GamingModeReceiver";
    private static final boolean DEBUG = false;

    public GamingModeReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null &&
                intent.getAction().equals(GamingModeController.GAMING_MODE_TURN_OFF)) {
            if (DEBUG) Log.d(TAG, "Received " + GamingModeController.GAMING_MODE_TURN_OFF);
            Settings.System.putInt(context.getContentResolver(), Settings.System.GAMING_MODE_ACTIVE, 0);
        } else if (intent.getAction() != null &&
                intent.getAction().equals(GamingModeController.GAMING_MODE_TURN_ON)) {
            if (DEBUG) Log.d(TAG, "Received " + GamingModeController.GAMING_MODE_TURN_ON);
            Settings.System.putInt(context.getContentResolver(), Settings.System.GAMING_MODE_ACTIVE, 1);
        }
    }
}
