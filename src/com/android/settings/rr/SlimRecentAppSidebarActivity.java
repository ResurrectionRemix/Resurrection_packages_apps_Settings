/*
 * Copyright (C) 2015-2017 Android Ice Cold Project
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

import android.app.Fragment;
import android.os.Bundle;

import com.android.settings.rr.dslv.ActionListViewSettings;

public class SlimRecentAppSidebarActivity extends SubActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionListViewSettings fragment = new ActionListViewSettings();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        args.putInt("actionMode", 7);
        args.putInt("maxAllowedActions", -1);
        args.putBoolean("useAppPickerOnly", true);
        args.putString("fragment", "com.android.settings.rr.RecentAppSidebarFragment");
        getFragmentManager().beginTransaction().replace(android.R.id.content, fragment).commit();
    }
}
