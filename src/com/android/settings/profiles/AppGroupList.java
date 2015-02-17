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

import android.annotation.Nullable;
import android.app.AlertDialog;
import android.app.NotificationGroup;
import android.app.ProfileManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.util.cm.ScreenType;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class AppGroupList extends SettingsPreferenceFragment {

    private static final String TAG = "AppGroupSettings";

    private ProfileManager mProfileManager;

    private View mFab;

    // constant value that can be used to check return code from sub activity.
    private static final int APP_GROUP_CONFIG = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.appgroup_list);
        mProfileManager = (ProfileManager) getActivity().getSystemService(Context.PROFILE_SERVICE);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshList();

        // On tablet devices remove the padding
        if (ScreenType.isTablet(getActivity())) {
            getListView().setPadding(0, 0, 0, 0);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.preference_list_with_fab, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mFab = view.findViewById(R.id.floating_action_button);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addAppGroup();
            }
        });
    }

    public void refreshList() {
        PreferenceScreen appgroupList = getPreferenceScreen();
        appgroupList.removeAll();

        // Add the existing app groups
        for (NotificationGroup group : mProfileManager.getNotificationGroups()) {
            PreferenceScreen pref = new PreferenceScreen(getActivity(), null);
            pref.setKey(group.getUuid().toString());
            pref.setTitle(group.getName());
            pref.setPersistent(false);
            appgroupList.addPreference(pref);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference instanceof PreferenceScreen) {
            NotificationGroup group = mProfileManager.getNotificationGroup(
                    UUID.fromString(preference.getKey()));
            editGroup(group);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void addAppGroup() {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View content = inflater.inflate(R.layout.profile_name_dialog, null);
        final TextView prompt = (TextView) content.findViewById(R.id.prompt);
        final EditText entry = (EditText) content.findViewById(R.id.name);

        prompt.setText(R.string.profile_appgroup_name_prompt);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.profile_new_appgroup);
        builder.setView(content);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = entry.getText().toString();
                if (!mProfileManager.notificationGroupExists(name)) {
                    NotificationGroup newGroup = new NotificationGroup(name);
                    mProfileManager.addNotificationGroup(newGroup);

                    refreshList();
                } else {
                    Toast.makeText(getActivity(),
                            R.string.duplicate_appgroup_name, Toast.LENGTH_LONG).show();
                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void editGroup(NotificationGroup group) {
        Bundle args = new Bundle();
        args.putParcelable("NotificationGroup", group);

        startFragment(this, AppGroupConfig.class.getName(), R.string.profile_appgroup_manage,
                APP_GROUP_CONFIG, args);
    }
}