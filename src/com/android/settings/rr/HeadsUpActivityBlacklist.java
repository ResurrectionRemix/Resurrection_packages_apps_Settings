/*
 * Copyright (C) 2018 AICP
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

import android.os.Bundle;
import android.provider.Settings;

import com.android.settings.rr.utils.PackageListAdapter;
import com.android.settings.R;

import java.util.HashMap;

public class HeadsUpActivityBlacklist extends ChooserActivityBlacklist {

    private static final String KEY_HEADSUP_BLACKLIST = "add_headsup_blacklist_packages";

    @Override
    protected int getPreferenceResource() {
        return R.xml.headsup_activity_blacklist;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeAllPreferences();
    }

    protected void initializeAllPreferences() {
        mBlacklistPackageStore = Settings.System.HEADS_UP_BLACKLIST_VALUES;
        mPackageManager = getActivity().getPackageManager();
        mPackageAdapter = new PackageListAdapter(getActivity());
        mBlacklistPrefList = getPreferenceScreen();
        mBlacklistPrefList.setOrderingAsAdded(false);
        mBlacklistPackages = new HashMap<String, Package>();
        mAddBlacklistPref = findPreference(KEY_HEADSUP_BLACKLIST);
        mAddBlacklistPref.setOnPreferenceClickListener(this);
    }
}
