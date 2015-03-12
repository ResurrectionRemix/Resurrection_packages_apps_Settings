/*
 * Copyright 2015 EXODUS - Dave Kessler <activethrasher00@gmail.com>
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

package com.android.settings.rr;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/*
 * Xposed exposer => a way to show when users have been submitting logcats for bugs that may
 * have been caused by using Xposed frameworks.
 */

public class XposedNotifyService extends Service {
    private static final String TAG = "XPOSED_FRAMEWORKS_SERVICE";

    // Message to post
    private static final String XPOSED_IS_INSTALLED_MESSAGE = "I SNIFF LITTLE BOYS' BICYCLE SEATS";

    // Repeat interval in milliseconds
    static final int REPEAT_MESSAGE_INTERVAL = 3000;

    static final int MSG_XPOSED_INSTALLED = 100;
    static final int MSG_XPOSED_UNINSTALLED = 101;

    volatile boolean _initInstalled = false;

    LogHandler mHandler = new LogHandler();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!_initInstalled) {
            mHandler.sendEmptyMessageDelayed(MSG_XPOSED_INSTALLED, 1000);
            _initInstalled = true;
        }
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
      return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.sendEmptyMessage(MSG_XPOSED_UNINSTALLED);
    }

    private class LogHandler extends Handler {
        private ScheduledExecutorService mExecutor;

        public LogHandler() {
            mExecutor = Executors.newSingleThreadScheduledExecutor();
        }

        public void handleMessage(Message m) {
            switch (m.what) {
                case MSG_XPOSED_INSTALLED:
                    if (mExecutor != null) {
                        mExecutor.scheduleAtFixedRate(new Runnable() {
                            @Override
                            public void run() {
                                Log.e(TAG, XPOSED_IS_INSTALLED_MESSAGE);
                            }
                        }, 0, REPEAT_MESSAGE_INTERVAL, TimeUnit.MILLISECONDS);
                    }
                    break;
                case MSG_XPOSED_UNINSTALLED:
                    if (_initInstalled && mExecutor != null) {
                        mExecutor.shutdown();
                        _initInstalled = false;
                    }
                    break;
            }
        }
    }
}

