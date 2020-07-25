/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.android.settings.security.applock;

import static com.android.settings.Utils.SETTINGS_PACKAGE_NAME;

import androidx.annotation.WorkerThread;

import android.app.Activity;
import android.app.AppLockManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Filter;
import android.widget.SearchView;

import androidx.lifecycle.ViewModelProviders;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.biometrics.BiometricEnrollBase;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.SubSettings;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.widget.AppLockPreference;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * App lock settings.
 */
public class AppLockSettings extends SubSettings {

    private static final String TAG = "AppLockSettings";

    private static final int RESULT_FINISHED = BiometricEnrollBase.RESULT_FINISHED;
    private static final int CONFIRM_REQUEST = 101;
    private static final int CHOOSE_LOCK_GENERIC_REQUEST = 102;

    @Override
    public Intent getIntent() {
        Intent intent = new Intent(super.getIntent());
        intent.putExtra(EXTRA_SHOW_FRAGMENT, AppLockSettingsFragment.class.getName());
        return intent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return AppLockSettingsFragment.class.getName().equals(fragmentName);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CharSequence msg = getText(R.string.applock_title);
        setTitle(msg);
    }

    public static class AppLockSettingsFragment extends SettingsPreferenceFragment
            implements SearchView.OnQueryTextListener, SearchView.OnCloseListener {

        private final String KEY_SHOW_ON_WAKE = "show_only_on_wake";
        private final String KEY_LOCKED_APPS = "locked_apps";
        private final String KEY_UNLOCKED_APPS = "unlocked_apps";
        private final String KEY_NOTIFICATION_HELP = "applock_notification_info";

        private final char DUPLICATE_CHAR = 6;

        private AppLockManager mAppLockManager;
        private PackageManager mPackageManager;

        private int mUserId;
        private boolean mSearching;
        private boolean mLaunchAuthenticated;
        private SearchView mSearchView;
        private SearchFilter mSearchFilter;
        private PreferenceScreen mPreferenceScreen;
        private SwitchPreference mShowOnlyOnWake;
        private Preference mLocked;
        private Preference mUnlocked;
        private Preference mNotifInfo;

        private final TreeMap<String, AppLockInfo> mLockedApps = new TreeMap<>();
        private final TreeMap<String, AppLockInfo> mUnlockedApps = new TreeMap<>();
        private final ArrayList<AppLockInfo> mAllApps = new ArrayList<>();

        @Override
        public int getMetricsCategory() {
            return -1;
        }

        @Override
        public void onResume() {
            super.onResume();
        }

        @Override
        public void onPause() {
            super.onPause();
            if (mLaunchAuthenticated) {
                getActivity().finish();
            }
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            final Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            inflater.inflate(R.menu.applock_menu, menu);

            final MenuItem searchMenuItem = menu.findItem(R.id.search_app_list_menu);
            if (searchMenuItem != null) {
                mSearchView = (SearchView) searchMenuItem.getActionView();
                mSearchView.setQueryHint(getText(R.string.search_settings));
                mSearchView.setOnQueryTextListener(this);
                mSearchView.setOnCloseListener(this);
            }
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            Log.d(TAG, "onCreate");
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);
            setAnimationAllowed(true);

            mUserId = getActivity().getIntent().getIntExtra(
                    Intent.EXTRA_USER_ID, UserHandle.myUserId());

            launchChooseOrConfirmLock();

            addPreferencesFromResource(R.xml.security_settings_applock);

            mSearching = false;
            mLaunchAuthenticated = false;
            mAppLockManager = (AppLockManager) getContext().getSystemService(Context.APPLOCK_SERVICE);
            mPackageManager = getPrefContext().getPackageManager();
            mPreferenceScreen = getPreferenceScreen();

            mShowOnlyOnWake = mPreferenceScreen.findPreference(KEY_SHOW_ON_WAKE);
            mLocked = mPreferenceScreen.findPreference(KEY_LOCKED_APPS);
            mUnlocked = mPreferenceScreen.findPreference(KEY_UNLOCKED_APPS);
            mNotifInfo = mPreferenceScreen.findPreference(KEY_NOTIFICATION_HELP);

            mShowOnlyOnWake.setChecked(mAppLockManager.getShowOnlyOnWake());
            mShowOnlyOnWake.setOnPreferenceChangeListener((preference, checked) -> {
                mAppLockManager.setShowOnlyOnWake((boolean) checked);
                return true;
            });

            mNotifInfo.setOnPreferenceClickListener((preference) -> {
                performNotifHintAnimation();
                return true;
            });

            final AppLockViewModel model = ViewModelProviders.of(this).get(AppLockViewModel.class);
            if (!model.getAppList().hasActiveObservers()) {
                model.getAppList().observeForever(data -> {
                    updateAppsList(data);
                });
            }
        }

        private void updateAppsList(List<AppLockInfo> entries) {
            for (AppLockInfo info : entries) {
                boolean locked = info.isAppLocked();
                if (locked) {
                    addToLocked(info.getLabel(), info);
                } else {
                    addToUnlocked(info.getLabel(), info);
                }
            }
            mAllApps.addAll(mLockedApps.values());
            mAllApps.addAll(mUnlockedApps.values());
            addPreferences();
        }

        private void updateAppsLocked(Preference preference, boolean isLocked) {
            String label = preference.getTitle().toString();
            if (isLocked) {
                mAppLockManager.addAppToList(preference.getKey());
                AppLockInfo info = mUnlockedApps.get(label);
                info.setAppLocked(true);
                addToLocked(label, info);
                removeFromUnlocked(label);
            } else {
                mAppLockManager.removeAppFromList(preference.getKey());
                AppLockInfo info = mLockedApps.get(label);
                info.setAppLocked(false);
                addToUnlocked(label, info);
                removeFromLocked(label);
            }
            updatePreferenceOrder();
            if (mSearching) {
                int totalAppsShown = 0;
                int lockedAppsShown = 0;
                final int count = mPreferenceScreen.getPreferenceCount();
                for (int i = 0; i < count; i++) {
                    Preference pref = mPreferenceScreen.getPreference(i);
                    if ((pref instanceof AppLockPreference) && pref.isVisible()) {
                        totalAppsShown++;
                        if ((pref == preference && isLocked) || (pref != preference
                                && ((AppLockPreference) pref).isChecked())) {
                            lockedAppsShown++;
                        }
                    }
                }
                updateCategoryVisibility(lockedAppsShown, totalAppsShown - lockedAppsShown);
            } else {
                updateCategoryVisibility(mLockedApps.size(), mUnlockedApps.size());
            }
        }

        private void addToLocked(String label, AppLockInfo info) {
            handleDuplicate(mLockedApps.put(label, info), mLockedApps);
        }

        private void addToUnlocked(String label, AppLockInfo info) {
            handleDuplicate(mUnlockedApps.put(label, info), mUnlockedApps);
        }

        private void handleDuplicate(AppLockInfo duplicate,
                TreeMap<String, AppLockInfo> map) {
            int count = 1;
            while (duplicate != null) {
                String dup = duplicate.getLabel();
                for (int i = 0; i < count; i++) {
                    dup += DUPLICATE_CHAR;
                }
                duplicate.setLabel(dup);
                duplicate = map.put(dup, duplicate);
                count++;
            }
        }

        private void removeFromLocked(String label) {
            mLockedApps.remove(label);
        }

        private void removeFromUnlocked(String label) {
            mUnlockedApps.remove(label);
        }

        private void updatePreferenceOrder() {
            int order = 0;
            for (AppLockInfo info : mLockedApps.values()) {
                AppLockPreference pref = mPreferenceScreen.findPreference(info.getPackageName());
                pref.setOrder(order);
                order++;
            }
            mUnlocked.setOrder(order);
            order++;
            for (AppLockInfo info : mUnlockedApps.values()) {
                AppLockPreference pref = mPreferenceScreen.findPreference(info.getPackageName());
                pref.setOrder(order);
                order++;
            }
        }

        private void addPreferences() {
            int order = 0;
            for (AppLockInfo info : mLockedApps.values()) {
                AppLockPreference pref = createPreference(info);
                mPreferenceScreen.addPreference(pref);
                pref.setOrder(order);
                order++;
            }
            mUnlocked.setOrder(order);
            order++;
            for (AppLockInfo info : mUnlockedApps.values()) {
                AppLockPreference pref = createPreference(info);
                mPreferenceScreen.addPreference(pref);
                pref.setOrder(order);
                order++;
            }
            updateCategoryVisibility(mLockedApps.size(), mUnlockedApps.size());
        }

        private void performNotifHintAnimation() {
            for (AppLockInfo info : mLockedApps.values()) {
                AppLockPreference pref = mPreferenceScreen.findPreference(info.getPackageName());
                pref.startHintAnimation();
            }
        }

        private void updatePreferencesPostSearch(ArrayList<AppLockInfo> results) {
            int lockedAppsShown = 0;
            for (AppLockInfo info : results) {
                String packageName = info.getPackageName();
                AppLockPreference pref = mPreferenceScreen.findPreference(packageName);
                if (pref != null) pref.setVisible(true);
                if (info.isAppLocked()) lockedAppsShown++;
            }
            ArrayList<AppLockInfo> negPackages = new ArrayList<>(mAllApps);
            negPackages.removeAll(results);
            for (AppLockInfo info : negPackages) {
                String packageName = info.getPackageName();
                AppLockPreference pref = mPreferenceScreen.findPreference(packageName);
                if (pref != null) pref.setVisible(false);
            }
            updateCategoryVisibility(lockedAppsShown, results.size() - lockedAppsShown);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == CHOOSE_LOCK_GENERIC_REQUEST
                    || requestCode == CONFIRM_REQUEST) {
                if (resultCode != RESULT_FINISHED && resultCode != RESULT_OK) {
                    // Authentication failed, close Activity.
                    getActivity().finish();
                } else {
                    mLaunchAuthenticated = true;
                }
            }
        }

        @Override
        public boolean onQueryTextSubmit(String query) {
            return false;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            if (mSearchFilter == null) {
                mSearchFilter = new SearchFilter();
            }
            mSearching = true;
            mSearchFilter.filter(newText);
            return false;
        }

        @Override
        public boolean onClose() {
            mSearching = false;
            updatePreferencesPostSearch(mAllApps);
            return false;
        }

        private void launchChooseOrConfirmLock() {
            Intent intent = new Intent();
            ChooseLockSettingsHelper helper = new ChooseLockSettingsHelper(getActivity(), this);
            if (!helper.launchConfirmationActivity(CONFIRM_REQUEST,
                    getString(R.string.applock_title),
                    null, null, mUserId, true /* foregroundOnly */)) {
                intent.setClassName(SETTINGS_PACKAGE_NAME, ChooseLockGeneric.class.getName());
                intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.MINIMUM_QUALITY_KEY,
                        DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
                intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.HIDE_DISABLED_PREFS,
                        true);
                intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
                startActivityForResult(intent, CHOOSE_LOCK_GENERIC_REQUEST);
            }
        }

        private AppLockPreference createPreference(AppLockInfo info) {
            String label = info.getLabel().toString();
            Drawable icon = info.getIcon();
            String packageName = info.getPackageName();
            boolean locked = info.isAppLocked();
            AppLockPreference pref = new AppLockPreference(getPrefContext(), mAppLockManager,
                    packageName);
            pref.setTitle(label);
            pref.setIcon(icon);
            pref.setKey(packageName);
            pref.setChecked(locked);
            pref.setOnPreferenceChangeListener((preference, isLocked) -> {
                updateAppsLocked(preference, (boolean) isLocked);
                return true;
            });
            return pref;
        }

        private void updateCategoryVisibility(int lockedApps, int unlockedApps) {
            if (lockedApps == 0) {
                mLocked.setVisible(false);
                mNotifInfo.setVisible(false);
            } else {
                mLocked.setVisible(true);
                mNotifInfo.setVisible(true);
            }
            if (unlockedApps == 0) {
                mUnlocked.setVisible(false);
            } else {
                mUnlocked.setVisible(true);
            }
        }

        /**
         * An array filter that constrains the content of the array adapter with a substring.
         * Item that does not contains the specified substring will be removed from the list.</p>
         */
        private class SearchFilter extends Filter {
            @WorkerThread
            @Override
            protected FilterResults performFiltering(CharSequence query) {
                final ArrayList<AppLockInfo> matchedEntries = new ArrayList<>();
                if (!TextUtils.isEmpty(query)) {
                    for (AppLockInfo info : mAllApps) {
                        if (info.getLabel().toString()
                                .toLowerCase().contains(query.toString().toLowerCase())) {
                            matchedEntries.add(info);
                        }
                    }
                } else {
                    matchedEntries.addAll(mAllApps);
                }
                final FilterResults results = new FilterResults();
                results.values = matchedEntries;
                results.count = matchedEntries.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                updatePreferencesPostSearch((ArrayList<AppLockInfo>) results.values);
            }
        }
    }
}
