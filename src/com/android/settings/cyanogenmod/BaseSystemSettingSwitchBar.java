/*
* Copyright (C) 2014 The CyanogenMod Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.android.settings.cyanogenmod;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.widget.Switch;
import com.android.settings.widget.SwitchBar;

public class BaseSystemSettingSwitchBar implements SwitchBar.OnSwitchChangeListener  {
    private Context mContext;
    private SwitchBar mSwitchBar;
    private SettingsObserver mSettingsObserver;
    private boolean mListeningToOnSwitchChange = false;

    private boolean mStateMachineEvent;

    private final String mSettingKey;
    private final int mDefaultState;

    private final SwitchBarChangeCallback mCallback;
    public interface SwitchBarChangeCallback {
        public void onEnablerChanged(boolean isEnabled);
    }

    public BaseSystemSettingSwitchBar(Context context, SwitchBar switchBar, String key,
                                      boolean defaultState, SwitchBarChangeCallback callback) {
        mContext = context;
        mSwitchBar = switchBar;
        mSettingKey = key;
        mDefaultState = defaultState ? 1 : 0;
        mCallback = callback;
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
        if (mCallback != null) {
            mCallback.onEnablerChanged(checked);
        }
    }

    private void setSwitchState() {
        boolean enabled = Settings.System.getInt(mContext.getContentResolver(),
                mSettingKey, mDefaultState) == 1;
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
                mSettingKey, isChecked ? 1 : 0);

        if (mCallback != null) {
            mCallback.onEnablerChanged(isChecked);
        }
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    mSettingKey), false, this);
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
