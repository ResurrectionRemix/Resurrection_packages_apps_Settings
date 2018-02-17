/*
 * Copyright (C) 2018 deletescape <deletescape@gmail.com>
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

package com.android.settings.rr.Preferences;


import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.preference.Preference;
import android.util.AttributeSet;

import com.android.settings.R;

public class DevicePreference extends Preference {

    public DevicePreference(Context context, AttributeSet attrs,
                            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    public DevicePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public DevicePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setSelectable(false);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.DevicePreference);
        String codename = ta.getString(R.styleable.DevicePreference_codename);
        String maintainer = ta.getString(R.styleable.DevicePreference_maintainer);
        int type = ta.getInt(R.styleable.DevicePreference_type, 0);
        String summary = context.getString(R.string.device_summary);
        ta.recycle();
        setSummary(String.format(summary, codename, maintainer));
        setIcon(type == 1 ? R.drawable.tablet_tint : R.drawable.phone_tint);
    }
}

