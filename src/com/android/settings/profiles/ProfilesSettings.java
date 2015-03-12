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

import android.annotation.Nullable;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.Profile;
import android.app.ProfileManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.support.v4.view.ViewPager;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.android.internal.util.cm.ScreenType;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.SubSettings;
import com.android.settings.cyanogenmod.BaseSystemSettingSwitchBar;

import java.util.UUID;

public class ProfilesSettings extends SettingsPreferenceFragment
        implements BaseSystemSettingSwitchBar.SwitchBarChangeCallback,
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "ProfilesSettings";

    public static final String EXTRA_PROFILE = "Profile";
    public static final String EXTRA_NEW_PROFILE = "new_profile_mode";

    private static final int MENU_RESET = Menu.FIRST;
    private static final int MENU_APP_GROUPS = Menu.FIRST + 1;

    private final IntentFilter mFilter;
    private final BroadcastReceiver mReceiver;

    private ProfileManager mProfileManager;
    private BaseSystemSettingSwitchBar mProfileEnabler;

    private View mAddProfileFab;
    private boolean mEnabled;

    ViewGroup mContainer;

    static Bundle mSavedState;

    public ProfilesSettings() {
        mFilter = new IntentFilter();
        mFilter.addAction(ProfileManager.PROFILES_STATE_CHANGED_ACTION);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (ProfileManager.PROFILES_STATE_CHANGED_ACTION.equals(action)) {
                    updateProfilesEnabledState();
                }
            }
        };

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.profiles_settings);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        FrameLayout frameLayout = new FrameLayout(getActivity());
        mContainer = frameLayout;
        frameLayout.addView(view);
        return frameLayout;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Add a footer to avoid a situation where the FAB would cover the last
        // item's options in a non-scrollable listview.
        ListView listView = getListView();
        View footer = LayoutInflater.from(getActivity())
                .inflate(R.layout.empty_list_entry_footer, listView, false);
        listView.addFooterView(footer);
        listView.setFooterDividersEnabled(false);
        footer.setOnClickListener(null);

        View v = LayoutInflater.from(getActivity())
                .inflate(R.layout.empty_textview, (ViewGroup) view, true);

        TextView emptyTextView = (TextView) v.findViewById(R.id.empty);
        listView.setEmptyView(emptyTextView);

        View fab = LayoutInflater.from(getActivity())
                .inflate(R.layout.fab, mContainer, true);
        mAddProfileFab = fab.findViewById(R.id.floating_action_button);
        mAddProfileFab.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        addProfile();
                    }
                });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        mProfileManager = (ProfileManager) getActivity().getSystemService(Context.PROFILE_SERVICE);
        // After confirming PreferenceScreen is available, we call super.
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mProfileEnabler != null) {
            mProfileEnabler.resume(getActivity());
        }
        getActivity().registerReceiver(mReceiver, mFilter);

        // check if we are enabled
        updateProfilesEnabledState();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mProfileEnabler != null) {
            mProfileEnabler.pause();
        }
        getActivity().unregisterReceiver(mReceiver);
    }

    @Override
    public void onStart() {
        super.onStart();
        final SettingsActivity activity = (SettingsActivity) getActivity();
        mProfileEnabler = new BaseSystemSettingSwitchBar(activity, activity.getSwitchBar(),
                Settings.System.SYSTEM_PROFILES_ENABLED, true, this);
    }

    @Override
    public void onDestroyView() {
        if (mProfileEnabler != null) {
            mProfileEnabler.teardownSwitchBar();
        }
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.add(0, MENU_RESET, 0, R.string.profile_reset_title)
                .setAlphabeticShortcut('r')
                .setEnabled(mEnabled)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(0, MENU_APP_GROUPS, 0, R.string.profile_appgroups_title)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                resetAll();
                return true;
            case MENU_APP_GROUPS:
                startFragment(this, AppGroupList.class.getName(),
                        R.string.profile_appgroups_title, 0, null);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addProfile() {
        Bundle args = new Bundle();
        args.putBoolean(EXTRA_NEW_PROFILE, true);
        args.putParcelable(EXTRA_PROFILE, new Profile(getString(R.string.new_profile_name)));

        SettingsActivity pa = (SettingsActivity) getActivity();
        pa.startPreferencePanel(SetupTriggersFragment.class.getCanonicalName(), args,
                0, null, this, 0);
    }

    private void resetAll() {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.profile_reset_title)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setMessage(R.string.profile_reset_message)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        mProfileManager.resetAll();
                        mProfileManager.setActiveProfile(
                                mProfileManager.getActiveProfile().getUuid());
                        dialog.dismiss();
                        refreshList();

                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void updateProfilesEnabledState() {
        Activity activity = getActivity();

        mEnabled = Settings.System.getInt(activity.getContentResolver(),
                Settings.System.SYSTEM_PROFILES_ENABLED, 1) == 1;
        activity.invalidateOptionsMenu();

        mAddProfileFab.setVisibility(mEnabled ? View.VISIBLE : View.GONE);
        if (!mEnabled) {
            getPreferenceScreen().removeAll(); // empty it
        } else {
            refreshList();
        }
    }

    @Override
    public void onEnablerChanged(boolean isEnabled) {
        Intent intent = new Intent(ProfileManager.PROFILES_STATE_CHANGED_ACTION);
        intent.putExtra(ProfileManager.EXTRA_PROFILES_STATE,
                isEnabled ?
                        ProfileManager.PROFILES_STATE_ENABLED :
                        ProfileManager.PROFILES_STATE_DISABLED);
        getActivity().sendBroadcast(intent);

        // update the reboot dialog state
        Intent u = new Intent();
        u.setAction(Intent.UPDATE_POWER_MENU);
        getActivity().sendBroadcastAsUser(u, UserHandle.ALL);
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
