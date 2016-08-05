/*
 * Copyright (C) 2012 ParanoidAndroid Project
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

import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;

import com.android.internal.logging.MetricsLogger;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class Halo extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_HALO_HIDE = "halo_hide";
    private static final String KEY_HALO_SIZE = "halo_size";
    private static final String KEY_HALO_COLOR = "halo_color";
    private static final String KEY_HALO_PAUSE = "halo_pause";
    private static final String KEY_HALO_MSGBOX = "halo_msgbox";
    private static final String KEY_HALO_MSGBOX_ANIMATION = "halo_msgbox_animation";
    private static final String KEY_HALO_NOTIFY_COUNT = "halo_notify_count";
    private static final String KEY_HALO_UNLOCK_PING = "halo_unlock_ping";

    private ListPreference mHaloSize;
    private ColorPickerPreference mHaloColor;
    private SwitchPreference mHaloHide;
    private SwitchPreference mHaloPause;
    private ListPreference mHaloNotifyCount;
    private ListPreference mHaloMsgAnimate;
    private SwitchPreference mHaloMsgBox;
    private SwitchPreference mHaloUnlockPing;

    private Context mContext;

    @Override
    public int getMetricsCategory() {
        return MetricsLogger.HALO_SETTINGS;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.halo_settings);
        PreferenceScreen prefSet = getPreferenceScreen();
        mContext = getActivity();

        mHaloHide = (SwitchPreference) prefSet.findPreference(KEY_HALO_HIDE);
        mHaloHide.setChecked(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.HALO_HIDE, 0) == 1);

        int isLowRAM = (!ActivityManager.isLowRamDeviceStatic()) ? 0 : 1;
        mHaloPause = (SwitchPreference) prefSet.findPreference(KEY_HALO_PAUSE);
        mHaloPause.setChecked(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.HALO_PAUSE, isLowRAM) == 1);

        mHaloSize = (ListPreference) prefSet.findPreference(KEY_HALO_SIZE);
        try {
            float haloSize = Settings.Secure.getFloat(mContext.getContentResolver(),
                    Settings.Secure.HALO_SIZE, 1.0f);
            mHaloSize.setValue(String.valueOf(haloSize));
        } catch(Exception ex) {
            // So what
        }
        mHaloSize.setOnPreferenceChangeListener(this);

        mHaloColor = (ColorPickerPreference) prefSet.findPreference(KEY_HALO_COLOR);
        mHaloColor.setOnPreferenceChangeListener(this);

        mHaloMsgBox = (SwitchPreference) prefSet.findPreference(KEY_HALO_MSGBOX);
        mHaloMsgBox.setChecked(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.HALO_MSGBOX, 1) == 1);

        mHaloUnlockPing = (SwitchPreference) prefSet.findPreference(KEY_HALO_UNLOCK_PING);
        mHaloUnlockPing.setChecked(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.HALO_UNLOCK_PING, 0) == 1);

        mHaloNotifyCount = (ListPreference) prefSet.findPreference(KEY_HALO_NOTIFY_COUNT);
        try {
            int haloCounter = Settings.Secure.getInt(mContext.getContentResolver(),
                    Settings.Secure.HALO_NOTIFY_COUNT, 4);
            mHaloNotifyCount.setValue(String.valueOf(haloCounter));
        } catch(Exception ex) {
            // fail...
        }
        mHaloNotifyCount.setOnPreferenceChangeListener(this);

        mHaloMsgAnimate = (ListPreference) prefSet.findPreference(KEY_HALO_MSGBOX_ANIMATION);
        try {
            int haloMsgAnimation = Settings.Secure.getInt(mContext.getContentResolver(),
                    Settings.Secure.HALO_MSGBOX_ANIMATION, 2);
            mHaloMsgAnimate.setValue(String.valueOf(haloMsgAnimation));
        } catch(Exception ex) {
            // fail...
        }
        mHaloMsgAnimate.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mHaloHide) {
            Settings.Secure.putInt(mContext.getContentResolver(),
                    Settings.Secure.HALO_HIDE, mHaloHide.isChecked()
                    ? 1 : 0);
        } else if (preference == mHaloPause) {
            Settings.Secure.putInt(mContext.getContentResolver(),
                    Settings.Secure.HALO_PAUSE, mHaloPause.isChecked()
                    ? 1 : 0);
        } else if (preference == mHaloMsgBox) {
            Settings.Secure.putInt(mContext.getContentResolver(),
                    Settings.Secure.HALO_MSGBOX, mHaloMsgBox.isChecked()
                    ? 1 : 0);
        } else if (preference == mHaloUnlockPing) {
            Settings.Secure.putInt(mContext.getContentResolver(),
                    Settings.Secure.HALO_UNLOCK_PING, mHaloUnlockPing.isChecked()
                    ? 1 : 0);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mHaloSize) {
            float haloSize = Float.valueOf((String) newValue);
            Settings.Secure.putFloat(getActivity().getContentResolver(),
                    Settings.Secure.HALO_SIZE, haloSize);
            return true;
	} else if (preference == mHaloColor) {
            int haloColor = Integer.valueOf(String.valueOf(newValue));
            Settings.Secure.putInt(getActivity().getContentResolver(),
                    Settings.Secure.HALO_COLOR, haloColor);
            return true;
        } else if (preference == mHaloMsgAnimate) {
            int haloMsgAnimation = Integer.valueOf((String) newValue);
            Settings.Secure.putInt(getActivity().getContentResolver(),
                    Settings.Secure.HALO_MSGBOX_ANIMATION, haloMsgAnimation);
            return true;
        } else if (preference == mHaloNotifyCount) {
            int haloNotifyCount = Integer.valueOf((String) newValue);
            Settings.Secure.putInt(getActivity().getContentResolver(),
                    Settings.Secure.HALO_NOTIFY_COUNT, haloNotifyCount);
            return true;
        }
        return false;
    }
}
