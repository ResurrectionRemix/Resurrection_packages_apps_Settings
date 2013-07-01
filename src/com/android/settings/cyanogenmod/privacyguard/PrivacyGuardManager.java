/*
 * Copyright (C) 2013 SlimRoms Project
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

package com.android.settings.cyanogenmod.privacyguard;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PrivacyGuardManager extends Fragment
        implements OnItemClickListener, OnItemLongClickListener {

    private static final String TAG = "PrivacyGuardManager";

    private TextView mNoUserAppsInstalled;
    private ListView mAppsList;
    private PrivacyGuardAppListAdapter mAdapter;
    private List<AppInfo> mApps;

    private PackageManager mPm;
    private Activity mActivity;

    private SharedPreferences mPreferences;

    // array of critical permissions where privacy guard
    // can hide the information
    private static final String[] PERMISSION_FILTER = new String[] {
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
        android.Manifest.permission.READ_HISTORY_BOOKMARKS,
        android.Manifest.permission.WRITE_HISTORY_BOOKMARKS,
        android.Manifest.permission.READ_CALENDAR,
        android.Manifest.permission.WRITE_CALENDAR,
        android.Manifest.permission.READ_CONTACTS,
        android.Manifest.permission.WRITE_CONTACTS,
        android.Manifest.permission.READ_CALL_LOG,
        android.Manifest.permission.WRITE_CALL_LOG,
        "com.android.voicemail.permission.READ_WRITE_ALL_VOICEMAIL",
        android.Manifest.permission.READ_SMS,
        android.Manifest.permission.RECEIVE_SMS,
        android.Manifest.permission.SEND_SMS,
        android.Manifest.permission.WRITE_SMS,
        android.Manifest.permission.BROADCAST_SMS
    };

    // holder for package data passed into the adapter
    public static final class AppInfo {
        String title;
        String packageName;
        boolean privacyGuardEnabled;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mActivity = getActivity();
        mPm = mActivity.getPackageManager();

        return inflater.inflate(R.layout.privacy_guard_manager, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mNoUserAppsInstalled = (TextView) mActivity.findViewById(R.id.error);

        mAppsList = (ListView) mActivity.findViewById(R.id.apps_list);
        mAppsList.setOnItemClickListener(this);
        mAppsList.setOnItemLongClickListener(this);

        // get shared preference
        mPreferences = mActivity.getSharedPreferences("privacy_guard_manager", Activity.MODE_PRIVATE);
        if (!mPreferences.getBoolean("first_help_shown", false)) {
            showHelp();
        }

        // load apps and construct the list
        loadApps();
        setHasOptionsMenu(true);
    }

    private void loadApps() {
        mApps = loadInstalledApps();

        // if app list is empty inform the user
        // else go ahead and construct the list
        if (mApps == null || mApps.isEmpty()) {
            if (shouldFilterByPermission()) {
                mNoUserAppsInstalled.setText(R.string.privacy_guard_filter_does_not_match);
            } else {
                mNoUserAppsInstalled.setText(R.string.privacy_guard_no_user_apps);
            }
            mNoUserAppsInstalled.setVisibility(View.VISIBLE);
            mAppsList.setVisibility(View.GONE);
        } else {
            mNoUserAppsInstalled.setVisibility(View.GONE);
            mAppsList.setVisibility(View.VISIBLE);
            mAdapter = new PrivacyGuardAppListAdapter(mActivity, mApps);
            mAppsList.setAdapter(mAdapter);
        }
    }

    private void resetPrivacyGuard() {
        if (mApps == null || mApps.isEmpty()) {
            return;
        }
        // turn off privacy guard for all apps shown in the current list
        for (AppInfo app : mApps) {
            if (app.privacyGuardEnabled) {
                mPm.setPrivacyGuardSetting(app.packageName, false);
                app.privacyGuardEnabled = false;
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // on click change the privacy guard status for this item
        final AppInfo app = (AppInfo) parent.getItemAtPosition(position);

        app.privacyGuardEnabled = !app.privacyGuardEnabled;
        mPm.setPrivacyGuardSetting(app.packageName, app.privacyGuardEnabled);

        mAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        // on long click open app details window
        final AppInfo app = (AppInfo) parent.getItemAtPosition(position);

        try {
            startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + app.packageName)));
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Couldn't open app details activity", e);
        }

        return true;
    }

    /**
    * Uses the package manager to query for all currently installed apps
    * for the list.
    *
    * @return the complete List off installed applications (@code PrivacyGuardAppInfo)
    */
    private List<AppInfo> loadInstalledApps() {
        List<AppInfo> apps = new ArrayList<AppInfo>();
        List<PackageInfo> packages = mPm.getInstalledPackages(PackageManager.GET_PERMISSIONS);
        boolean showSystemApps = shouldShowSystemApps();
        boolean filterByPermission = shouldFilterByPermission();

        for (PackageInfo info : packages) {
            final ApplicationInfo appInfo = info.applicationInfo;

            // skip system apps if they shall not be included
            if (!showSystemApps && (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                continue;
            }

            if (filterByPermission) {
                final String[] requestedPermissions = info.requestedPermissions;
                if (requestedPermissions == null || !filterAppPermissions(requestedPermissions)) {
                    continue;
                }
            }

            AppInfo app = new AppInfo();
            app.title = appInfo.loadLabel(mPm).toString();
            app.packageName = info.packageName;
            app.privacyGuardEnabled = mPm.getPrivacyGuardSetting(app.packageName);
            apps.add(app);
        }

        // sort the apps by title
        Collections.sort(apps, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo lhs, AppInfo rhs) {
                return lhs.title.compareToIgnoreCase(rhs.title);
            }
        });

        return apps;
    }

    private boolean filterAppPermissions(final String[] requestedPermissions) {
        for (String requested : requestedPermissions) {
            for (String filtered : PERMISSION_FILTER) {
               if (requested.equals(filtered)) {
                    return true;
               }
            }
        }
        return false;
    }

    private boolean shouldShowSystemApps() {
        return mPreferences.getBoolean("show_system_apps", false);
    }

    private boolean shouldFilterByPermission() {
        return mPreferences.getBoolean("filter_by_permission", true);
    }

    private class HelpDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.privacy_guard_help_title)
                    .setMessage(R.string.privacy_guard_help_text)
                    .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .create();
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            mPreferences.edit().putBoolean("first_help_shown", true).commit();
        }
    }

    private void showHelp() {
        HelpDialogFragment fragment = new HelpDialogFragment();
        fragment.show(getFragmentManager(), "help_dialog");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.privacy_guard_manager, menu);
        menu.findItem(R.id.show_system_apps).setChecked(shouldShowSystemApps());
        menu.findItem(R.id.filter_app_permissions).setChecked(shouldFilterByPermission());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.help:
                showHelp();
                return true;
            case R.id.reset:
                resetPrivacyGuard();
                return true;
            case R.id.filter_app_permissions:
            case R.id.show_system_apps:
                final String prefName = item.getItemId() == R.id.filter_app_permissions
                        ? "filter_by_permission" : "show_system_apps";
                // set the menu checkbox and save it in
                // shared preference and rebuild the list
                item.setChecked(!item.isChecked());
                mPreferences.edit().putBoolean(prefName, item.isChecked()).commit();
                loadApps();
                return true;
             default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // rebuild the list; the user might have changed settings inbetween
        loadApps();
    }
}
