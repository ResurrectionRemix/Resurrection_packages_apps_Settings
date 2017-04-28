/*
 * Copyright (C) 2013 The CyanogenMod project
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

package com.android.settings;

import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings;
import android.support.v7.preference.EditTextPreference;
import android.util.AttributeSet;
import android.util.Log;

import cyanogenmod.providers.CMSettings;

public class HostnamePreference extends EditTextPreference {

    private static final String TAG = "HostnamePreference";

    private static final String PROP_HOSTNAME = "net.hostname";

    public HostnamePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setSummary(getText());
    }

    public HostnamePreference(Context context, AttributeSet attrs) {
        this(context, attrs, com.android.internal.R.attr.editTextPreferenceStyle);
    }

    public HostnamePreference(Context context) {
        this(context, null);
    }

    @Override
    public void setText(String text) {
        if (text == null) {
            Log.e(TAG, "tried to set null hostname, request ignored");
            return;
        }
        // remove any character that is not alphanumeric, period, or hyphen
        text = text.replaceAll("[^-.a-zA-Z0-9]", "");
        if (text.length() == 0) {
            Log.w(TAG, "setting empty hostname");
        } else {
            Log.i(TAG, "hostname has been set: " + text);
        }
        SystemProperties.set(PROP_HOSTNAME, text);
        persistHostname(text);
        setSummary(text);
    }

    @Override
    public String getText() {
        return SystemProperties.get(PROP_HOSTNAME);
    }

    @Override
    public void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        persistHostname(getText());
    }

    public void persistHostname(String hostname) {
        CMSettings.Secure.putString(getContext().getContentResolver(),
                CMSettings.Secure.DEVICE_HOSTNAME, hostname);
    }
}
