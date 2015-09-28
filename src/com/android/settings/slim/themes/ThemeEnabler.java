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

package com.android.settings.slim.themes;

import android.content.Context;
import android.content.res.Configuration;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.SwitchPreference;
import android.provider.Settings;
import com.android.internal.util.slim.ActionConstants;
import com.android.internal.util.slim.Action;

import com.android.settings.R;

public class ThemeEnabler implements OnPreferenceClickListener {
    private final Context mContext;
    private SwitchPreference mSwitchPreference;
    private boolean mStateMachineEvent;

    public ThemeEnabler(Context context, SwitchPreference switchPreference_) {
        mContext = context;
        mSwitchPreference = switchPreference_;
    }

    public void resume() {
        mSwitchPreference.setOnPreferenceClickListener(this);
        setSwitchState();
    }

    public void pause() {
        mSwitchPreference.setOnPreferenceClickListener(null);
    }

    public void setSwitchPreference(SwitchPreference switchPreference_) {
        if (mSwitchPreference == switchPreference_) return;
        mSwitchPreference.setOnPreferenceClickListener(null);
        mSwitchPreference = switchPreference_;
        mSwitchPreference.setOnPreferenceClickListener(this);
        setSwitchState();
    }

    public void setSwitchState() {
        boolean state = mContext.getResources().getConfiguration().uiThemeMode
                    == Configuration.UI_THEME_MODE_HOLO_DARK;
        mStateMachineEvent = true;
        mSwitchPreference.setChecked(state);
        mStateMachineEvent = false;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (mStateMachineEvent) {
            return true;
        }
        Action.processAction(mContext, ActionConstants.ACTION_THEME_SWITCH, false);
        setSwitchState();
        return true;
    }

}
