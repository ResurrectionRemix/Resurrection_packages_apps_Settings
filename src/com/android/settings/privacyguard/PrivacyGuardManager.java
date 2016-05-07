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

package com.android.settings.privacyguard;

import android.app.FragmentTransaction;
import android.view.animation.AnimationUtils;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
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
import com.android.settings.Settings.AppOpsSummaryActivity;
import com.android.settings.SubSettings;
import com.android.settings.applications.AppOpsDetails;
import com.android.settings.applications.AppOpsState;
import com.android.settings.applications.AppOpsState.OpsTemplate;
import com.android.settings.privacyguard.AppInfoLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PrivacyGuardManager extends Fragment
        implements OnItemClickListener, OnItemLongClickListener,
                   LoaderManager.LoaderCallbacks<List<PrivacyGuardManager.AppInfo>> {

    private static final String TAG = "PrivacyGuardManager";

    private TextView mNoUserAppsInstalled;
    private ListView mAppsList;
    private View mLoadingContainer;
    private PrivacyGuardAppListAdapter mAdapter;
    private List<AppInfo> mApps;

    private Activity mActivity;

    private SharedPreferences mPreferences;
    private AppOpsManager mAppOps;

    private int mSavedFirstVisiblePosition = AdapterView.INVALID_POSITION;
    private int mSavedFirstItemOffset;

    // keys for extras and icicles
    private final static String LAST_LIST_POS = "last_list_pos";
    private final static String LAST_LIST_OFFSET = "last_list_offset";

    // Privacy Guard Fragment
    private final static String PRIVACY_GUARD_FRAGMENT_TAG = "privacy_guard_fragment";

    // holder for package data passed into the adapter
    public static final class AppInfo {
        String title;
        String packageName;
        boolean enabled;
        boolean privacyGuardEnabled;
        int uid;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mActivity = getActivity();
        mAppOps = (AppOpsManager)getActivity().getSystemService(Context.APP_OPS_SERVICE);

        View hostView = inflater.inflate(R.layout.privacy_guard_manager, container, false);

        Fragment privacyGuardPrefs = PrivacyGuardPrefs.newInstance();
        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.privacy_guard_prefs, privacyGuardPrefs,
                PRIVACY_GUARD_FRAGMENT_TAG);
        fragmentTransaction.commit();
        return hostView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mNoUserAppsInstalled = (TextView) mActivity.findViewById(R.id.error);

        mAppsList = (ListView) mActivity.findViewById(R.id.apps_list);
        mAppsList.setOnItemClickListener(this);
        mAppsList.setOnItemLongClickListener(this);

        mLoadingContainer = mActivity.findViewById(R.id.loading_container);

        // get shared preference
        mPreferences = mActivity.getSharedPreferences("privacy_guard_manager", Activity.MODE_PRIVATE);
        if (savedInstanceState == null && !mPreferences.getBoolean("first_help_shown", false)) {
            showHelp();
        }

        if (savedInstanceState != null) {
            mSavedFirstVisiblePosition = savedInstanceState.getInt(LAST_LIST_POS,
                    AdapterView.INVALID_POSITION);
            mSavedFirstItemOffset = savedInstanceState.getInt(LAST_LIST_OFFSET, 0);
        } else {
            mSavedFirstVisiblePosition = AdapterView.INVALID_POSITION;
            mSavedFirstItemOffset = 0;
        }

        // load apps and construct the list
        scheduleAppsLoad();

        setHasOptionsMenu(true);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(LAST_LIST_POS, mSavedFirstVisiblePosition);
        outState.putInt(LAST_LIST_OFFSET, mSavedFirstItemOffset);
    }

    @Override
    public void onPause() {
        super.onPause();

        // Remember where the list is scrolled to so we can restore the scroll position
        // when we come back to this activity and *after* we complete querying for the
        // conversations.
        mSavedFirstVisiblePosition = mAppsList.getFirstVisiblePosition();
        View firstChild = mAppsList.getChildAt(0);
        mSavedFirstItemOffset = (firstChild == null) ? 0 : firstChild.getTop();
    }

    @Override
    public void onResume() {
        super.onResume();

        // rebuild the list; the user might have changed settings inbetween
        scheduleAppsLoad();
    }

    @Override
    public Loader<List<AppInfo>> onCreateLoader(int id, Bundle args) {
        mLoadingContainer.startAnimation(AnimationUtils.loadAnimation(
              mActivity, android.R.anim.fade_in));
        mAppsList.startAnimation(AnimationUtils.loadAnimation(
              mActivity, android.R.anim.fade_out));

        mAppsList.setVisibility(View.INVISIBLE);
        mLoadingContainer.setVisibility(View.VISIBLE);
        return new AppInfoLoader(mActivity, shouldShowSystemApps());
    }

    @Override
    public void onLoadFinished(Loader<List<AppInfo>> loader, List<AppInfo> apps) {
        mApps = apps;
        prepareAppAdapter();

        mLoadingContainer.startAnimation(AnimationUtils.loadAnimation(
              mActivity, android.R.anim.fade_out));
        mAppsList.startAnimation(AnimationUtils.loadAnimation(
              mActivity, android.R.anim.fade_in));

        if (mSavedFirstVisiblePosition != AdapterView.INVALID_POSITION) {
            mAppsList.setSelectionFromTop(mSavedFirstVisiblePosition, mSavedFirstItemOffset);
            mSavedFirstVisiblePosition = AdapterView.INVALID_POSITION;
        }

        mLoadingContainer.setVisibility(View.INVISIBLE);
        mAppsList.setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoaderReset(Loader<List<AppInfo>> loader) {
    }

    private void scheduleAppsLoad() {
        getLoaderManager().restartLoader(0, null, this);
    }

    private void prepareAppAdapter() {
        // if app list is empty inform the user
        // else go ahead and construct the list
        if (mApps == null || mApps.isEmpty()) {
            mNoUserAppsInstalled.setText(R.string.privacy_guard_no_user_apps);
            mNoUserAppsInstalled.setVisibility(View.VISIBLE);
            mAppsList.setVisibility(View.GONE);
            mAppsList.setAdapter(null);
        } else {
            mNoUserAppsInstalled.setVisibility(View.GONE);
            mAppsList.setVisibility(View.VISIBLE);
            mAdapter = createAdapter();
            mAppsList.setAdapter(mAdapter);
            mAppsList.setFastScrollEnabled(true);
        }
    }

    private PrivacyGuardAppListAdapter createAdapter() {
        String lastSectionIndex = null;
        ArrayList<String> sections = new ArrayList<String>();
        ArrayList<Integer> positions = new ArrayList<Integer>();
        int count = mApps.size(), offset = 0;

        for (int i = 0; i < count; i++) {
            AppInfo app = mApps.get(i);
            String sectionIndex;

            if (!app.enabled) {
                sectionIndex = "--"; //XXX
            } else if (app.title.isEmpty()) {
                sectionIndex = "";
            } else {
                sectionIndex = app.title.substring(0, 1).toUpperCase();
            }

            if (lastSectionIndex == null ||
                    !TextUtils.equals(sectionIndex, lastSectionIndex)) {
                sections.add(sectionIndex);
                positions.add(offset);
                lastSectionIndex = sectionIndex;
            }
            offset++;
        }

        return new PrivacyGuardAppListAdapter(mActivity, mApps, sections, positions);
    }

    private void resetPrivacyGuard() {
        if (mApps == null || mApps.isEmpty()) {
            return;
        }
        showResetDialog();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // on click change the privacy guard status for this item
        final AppInfo app = (AppInfo) parent.getItemAtPosition(position);

        app.privacyGuardEnabled = !app.privacyGuardEnabled;
        mAppOps.setPrivacyGuardSettingForPackage(app.uid, app.packageName, app.privacyGuardEnabled);

        mAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        // on long click open app details window
        final AppInfo app = (AppInfo) parent.getItemAtPosition(position);

        Bundle args = new Bundle();
        args.putString(AppOpsDetails.ARG_PACKAGE_NAME, app.packageName);

        SubSettings ssa = (SubSettings) getActivity();
        ssa.startPreferencePanel(AppOpsDetails.class.getName(), args,
                R.string.privacy_guard_manager_title, null, this, 2);
        return true;
    }


    private boolean shouldShowSystemApps() {
        return mPreferences.getBoolean("show_system_apps", false);
    }

    public static class HelpDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.privacy_guard_help_title)
                    .setMessage(R.string.privacy_guard_help_text)
                    .setNegativeButton(R.string.dlg_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .create();
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            getActivity().getSharedPreferences("privacy_guard_manager", Activity.MODE_PRIVATE)
                    .edit()
                    .putBoolean("first_help_shown", true)
                    .commit();
        }
    }

    private void showHelp() {
        HelpDialogFragment fragment = new HelpDialogFragment();
        fragment.show(getFragmentManager(), "help_dialog");
    }

    public static class ResetDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.privacy_guard_reset_title)
                    .setMessage(R.string.privacy_guard_reset_text)
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                ((PrivacyGuardManager)getTargetFragment()).doReset();
                        }
                    })
                    .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Do nothing
                        }
                    })
                    .create();
        }
    }

    private void doReset() {
        // turn off privacy guard for all apps shown in the current list
        for (AppInfo app : mApps) {
            app.privacyGuardEnabled = false;
        }
        mAppOps.resetAllModes();
        mAdapter.notifyDataSetChanged();
    }

    private void showResetDialog() {
        ResetDialogFragment dialog = new ResetDialogFragment();
        dialog.setTargetFragment(this, 0);
        dialog.show(getFragmentManager(), "reset_dialog");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.privacy_guard_manager, menu);
        menu.findItem(R.id.show_system_apps).setChecked(shouldShowSystemApps());
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
            case R.id.show_system_apps:
                final String prefName = "show_system_apps";
                // set the menu checkbox and save it in
                // shared preference and rebuild the list
                item.setChecked(!item.isChecked());
                mPreferences.edit().putBoolean(prefName, item.isChecked()).commit();
                scheduleAppsLoad();
                return true;
            case R.id.advanced:
                Intent i = new Intent(Intent.ACTION_MAIN);
                i.setClass(mActivity, AppOpsSummaryActivity.class);
                mActivity.startActivity(i);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }
}
