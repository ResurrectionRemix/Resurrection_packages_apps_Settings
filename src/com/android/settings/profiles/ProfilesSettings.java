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
import android.provider.Settings;
import android.support.v4.view.ViewPager;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.SubSettings;
import com.android.settings.Utils;

public class ProfilesSettings extends SettingsPreferenceFragment {
    private static final String TAG = "ProfilesSettings";

    public static final String EXTRA_PROFILE = "Profile";
    public static final String EXTRA_NEW_PROFILE = "new_profile_mode";

    private static final int MENU_RESET = Menu.FIRST;

    private final IntentFilter mFilter;
    private final BroadcastReceiver mReceiver;

    private ProfileManager mProfileManager;
    private ProfileEnabler mProfileEnabler;

    private ViewPager mViewPager;
    private TextView mEmptyText;
    private ProfilesPagerAdapter mAdapter;
    private ImageView mAddProfileFab;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mContainer = container;

        View view = inflater.inflate(R.layout.profile_tabs, container, false);
        mViewPager = (ViewPager) view.findViewById(R.id.pager);
        mEmptyText = (TextView) view.findViewById(R.id.empty);
        mAddProfileFab = (ImageView) view.findViewById(R.id.floating_action_button);
        mAddProfileFab.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        addProfile();
                    }
                });

        mAdapter = new ProfilesPagerAdapter(getChildFragmentManager());
        mViewPager.setAdapter(mAdapter);

        return view;
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

        // If running on a phone, remove padding around tabs
        if (!Utils.isTablet(getActivity())) {
            mContainer.setPadding(0, 0, 0, 0);
        }
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
        mProfileEnabler = new ProfileEnabler(activity, activity.getSwitchBar());
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
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                resetAll();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addProfile() {
        Bundle args = new Bundle();
        args.putBoolean(EXTRA_NEW_PROFILE, true);
        args.putParcelable(EXTRA_PROFILE, new Profile(getString(R.string.new_profile_name)));

        SubSettings pa = (SubSettings) getActivity();
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
                        mAdapter.refreshProfiles();
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
        mViewPager.setVisibility(mEnabled ? View.VISIBLE : View.GONE);
        mEmptyText.setVisibility(mEnabled ? View.GONE : View.VISIBLE);
    }

    class ProfilesPagerAdapter extends FragmentStatePagerAdapter {
        Fragment[] frags = { new ProfilesList() };
        String[] titles = { getString(R.string.profile_profiles_manage) };

        ProfilesPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return frags[position];
        }

        @Override
        public int getCount() {
            return frags.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return titles[position];
        }

        public void refreshProfiles() {
            ((ProfilesList) frags[0]).refreshList();
        }

    }
}
