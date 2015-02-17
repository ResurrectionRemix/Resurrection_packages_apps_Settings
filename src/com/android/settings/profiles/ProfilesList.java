/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.android.settings.profiles;

import java.util.UUID;

import android.app.Profile;
import android.app.ProfileManager;
import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.android.internal.util.cm.ScreenType;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class ProfilesList extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    static final String TAG = "ProfilesSettings";

    private ProfileManager mProfileManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.profiles_settings);
        mProfileManager = (ProfileManager) getActivity().getSystemService(Context.PROFILE_SERVICE);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshList();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Add a footer to avoid a situation where the FAB would cover the last
        // item's options in a non-scrollable listview.
        ListView listView = getListView();
        View footer = LayoutInflater.from(getActivity())
                .inflate(R.layout.empty_list_entry_footer, listView, false);
        listView.addFooterView(footer);
        listView.setFooterDividersEnabled(false);
        footer.setOnClickListener(null);
    }

    public void refreshList() {
        PreferenceScreen plist = getPreferenceScreen();
        plist.removeAll();

        // Get active profile, if null
        Profile prof = mProfileManager.getActiveProfile();
        String selectedKey = prof != null ? prof.getUuid().toString() : null;

        for (Profile profile : mProfileManager.getProfiles()) {
            Bundle args = new Bundle();
            args.putParcelable(ProfilesSettings.EXTRA_PROFILE, profile);
            args.putBoolean(ProfilesSettings.EXTRA_NEW_PROFILE, false);

            ProfilesPreference ppref = new ProfilesPreference(this, args);
            ppref.setKey(profile.getUuid().toString());
            ppref.setTitle(profile.getName());
            ppref.setPersistent(false);
            ppref.setOnPreferenceChangeListener(this);
            ppref.setSelectable(true);
            ppref.setEnabled(true);

            if (TextUtils.equals(selectedKey, ppref.getKey())) {
                ppref.setChecked(true);
            }

            plist.addPreference(ppref);
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (newValue instanceof String) {
            setSelectedProfile((String) newValue);
            refreshList();
        }
        return true;
    }

    private void setSelectedProfile(String key) {
        try {
            UUID selectedUuid = UUID.fromString(key);
            mProfileManager.setActiveProfile(selectedUuid);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        }
    }
}
