/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings.profiles;

import android.app.ProfileManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.widget.Switch;
import android.widget.Toast;
import com.android.settings.R;
import com.android.settings.WirelessSettings;
import com.android.settings.search.Index;
import com.android.settings.widget.SwitchBar;
import com.android.settings.wifi.WifiSettings;

import java.util.concurrent.atomic.AtomicBoolean;

public class ProfileEnabler implements SwitchBar.OnSwitchChangeListener  {
    private Context mContext;
    private SwitchBar mSwitchBar;
    private SettingsObserver mSettingsObserver;
    private boolean mListeningToOnSwitchChange = false;

    private boolean mStateMachineEvent;

    public ProfileEnabler(Context context, SwitchBar switchBar) {
        mContext = context;
        mSwitchBar = switchBar;
        mSettingsObserver = new SettingsObserver(new Handler());
        setupSwitchBar();
    }

    public void setupSwitchBar() {
        setSwitchState();
        if (!mListeningToOnSwitchChange) {
            mSwitchBar.addOnSwitchChangeListener(this);
            mListeningToOnSwitchChange = true;
        }
        mSwitchBar.show();
    }

    public void teardownSwitchBar() {
        if (mListeningToOnSwitchChange) {
            mSwitchBar.removeOnSwitchChangeListener(this);
            mListeningToOnSwitchChange = false;
        }
        mSwitchBar.hide();
    }

    public void resume(Context context) {
        mContext = context;
        if (!mListeningToOnSwitchChange) {
            mSwitchBar.addOnSwitchChangeListener(this);
            mSettingsObserver.observe();

            mListeningToOnSwitchChange = true;
        }
    }

    public void pause() {
        if (mListeningToOnSwitchChange) {
            mSwitchBar.removeOnSwitchChangeListener(this);
            mSettingsObserver.unobserve();

            mListeningToOnSwitchChange = false;
        }
    }

    private void setSwitchBarChecked(boolean checked) {
        mStateMachineEvent = true;
        mSwitchBar.setChecked(checked);
        mStateMachineEvent = false;
    }

    private void setSwitchState() {
        boolean enabled = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.SYSTEM_PROFILES_ENABLED, 1) == 1;
        mStateMachineEvent = true;
        setSwitchBarChecked(enabled);
        mStateMachineEvent = false;
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        //Do nothing if called as a result of a state machine event
        if (mStateMachineEvent) {
            return;
        }

        // Handle a switch change
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.SYSTEM_PROFILES_ENABLED, isChecked ? 1 : 0);

        // Send a broadcast intent to the world
        // TODO Enabling or disabling profiles should be at ProfileManager, not here
        Intent intent=new Intent(ProfileManager.PROFILES_STATE_CHANGED_ACTION);
        intent.putExtra(
                ProfileManager.EXTRA_PROFILES_STATE,
                isChecked ?
                        ProfileManager.PROFILES_STATE_ENABLED :
                        ProfileManager.PROFILES_STATE_DISABLED);
        mContext.sendBroadcast(intent);
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.SYSTEM_PROFILES_ENABLED), false, this);
            update();
        }

        void unobserve() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            update();
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            update();
        }

        public void update() {
            setSwitchState();
        }
    }
}
