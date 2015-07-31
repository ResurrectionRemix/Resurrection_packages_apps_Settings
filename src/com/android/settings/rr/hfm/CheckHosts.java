/*
 * Copyright (C) 2014 Dirty Unicorns
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

package com.android.settings.rr.hfm;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class CheckHosts extends Service {

    private static final String TAG = "HFMCheckHosts";

    public static final String HFM_DISABLE_ADS = "hfm_disable_ads";

    private Context mContext;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        mContext = this;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Log.i(TAG, "Started check");
        HfmHelpers.checkStatus(mContext);
    }
}
