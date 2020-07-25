/*
 * Copyright (C) 2020 Paranoid Android
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.display;

import static android.hardware.display.DcDimmingManager.MODE_AUTO_OFF;
import static android.hardware.display.DcDimmingManager.MODE_AUTO_TIME;
import static android.hardware.display.DcDimmingManager.MODE_AUTO_BRIGHTNESS;
import static android.hardware.display.DcDimmingManager.MODE_AUTO_FULL;

import android.content.Context;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.hardware.display.DcDimmingManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Switch;

import androidx.preference.PreferenceManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settingslib.TwoTargetPreference;

public class DcDimmingPreference extends TwoTargetPreference {

    private Switch mSwitch;
    private boolean mChecked;
    private final DcDimmingManager mDcDimmingManager;

    public DcDimmingPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mDcDimmingManager = (DcDimmingManager) context.getSystemService(Context.DC_DIM_SERVICE);
        if (mDcDimmingManager != null && mDcDimmingManager.isAvailable()) {
            SettingsObserver settingsObserver = new SettingsObserver(new Handler());
            settingsObserver.observe();
        }
    }

    @Override
    protected int getSecondTargetResId() {
        return R.layout.preference_widget_master_switch;
    }

    @Override
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        super.onAttachedToHierarchy(preferenceManager);
        if (mDcDimmingManager == null || !mDcDimmingManager.isAvailable()) {
            setVisible(false);
            return;
        }
        boolean active = mDcDimmingManager.isDcDimmingOn();
        setChecked(active);
        updateSummary(active);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        final View widgetView = holder.findViewById(android.R.id.widget_frame);
        if (widgetView != null) {
            widgetView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mSwitch != null && !mSwitch.isEnabled()) {
                        return;
                    }
                    setUserChecked(!mChecked);
                    if (!callChangeListener(mChecked)) {
                        setUserChecked(!mChecked);
                    } else {
                        persistBoolean(mChecked);
                    }
                }
            });
        }

        mSwitch = (Switch) holder.findViewById(R.id.switchWidget);
        if (mSwitch != null) {
            mSwitch.setContentDescription(getTitle());
            mSwitch.setChecked(mChecked);
        }
    }

    public void setUserChecked(boolean checked) {
        setChecked(checked);
        mDcDimmingManager.setDcDimming(checked);
        updateSummary(mDcDimmingManager.isDcDimmingOn());
    }

    private void updateSummary(boolean active) {
        final boolean auto = mDcDimmingManager.getAutoMode() != MODE_AUTO_OFF;
        String detail;
        if (active) {
            detail = getContext().getString(auto
                    ? R.string.dc_dimming_on_auto_mode_auto
                    : R.string.dark_ui_summary_on_auto_mode_never);
        } else {
            detail = getContext().getString(auto
                    ? R.string.dc_dimming_off_auto_mode_auto
                    : R.string.dark_ui_summary_off_auto_mode_never);
        }
        String summary = getContext().getString(active
                ? R.string.dark_ui_summary_on
                : R.string.dark_ui_summary_off, detail);

        setSummary(summary);
    }

    public void setChecked(boolean checked) {
        mChecked = checked;
        if (mSwitch != null) {
            mSwitch.setChecked(checked);
        }
    }

    private class SettingsObserver extends ContentObserver {

        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = getContext().getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.DC_DIMMING_AUTO_MODE), false, this,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.DC_DIMMING_STATE), false, this,
                    UserHandle.USER_ALL);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSummary(mDcDimmingManager.isDcDimmingOn());
            setChecked(mDcDimmingManager.isDcDimmingOn());
        }
    }
}