/*
 * Copyright (C) 2016 RR
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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.SearchIndexableResource;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.List;
import java.util.ArrayList;

import com.android.internal.logging.MetricsLogger;

public class QsVibrate extends SettingsPreferenceFragment {
	

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.rr_qs_vibrate);
    }

    protected int getMetricsCategory()
    {
	return MetricsLogger.APPLICATION;
    }
}
