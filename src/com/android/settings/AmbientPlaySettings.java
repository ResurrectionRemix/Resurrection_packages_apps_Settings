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

package com.android.settings;

import android.content.Context;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.ambient.AmbientPlayPreferenceController;
import com.android.settings.ambient.AmbientPlayKeyguardPreferenceController;

public class AmbientPlaySettings extends DashboardFragment {
    private static final String TAG = "AmbientPlaySettings";

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        addPreferenceController(new AmbientPlayPreferenceController(context));
        addPreferenceController(new AmbientPlayKeyguardPreferenceController(context));
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.ambient_play_settings;
    }
}
