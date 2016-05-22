/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.applications;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.cyanogenmod.ProtectedAppsReceiver;

import cyanogenmod.providers.CMSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ProtectedAppsActivity extends Activity {
    private static final int REQ_ENTER_PATTERN = 1;
    private static final int REQ_RESET_PATTERN = 2;

    private static final String NEEDS_UNLOCK = "needs_unlock";
    private static final String TARGET_INTENT = "target_intent";

    private ListView mListView;

    private static final int MENU_RESET = 0;
    private static final int MENU_RESET_LOCK = 1;

    private PackageManager mPackageManager;

    private AppsAdapter mAppsAdapter;

    private ArrayList<ComponentName> mProtect;

    private boolean mWaitUserAuth = false;
    private boolean mUserIsAuth = false;
    private Intent mTargetIntent;
    private int mOrientation;

    private HashSet<ComponentName> mProtectedApps = new HashSet<ComponentName>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Handle incoming target activity
        Intent incomingIntent = getIntent();
        if (incomingIntent.hasExtra("com.android.settings.PROTECTED_APP_TARGET_INTENT")) {
            mTargetIntent =
                    incomingIntent.getParcelableExtra(
                            "com.android.settings.PROTECTED_APP_TARGET_INTENT");
        }

        setTitle(R.string.protected_apps);
        setContentView(R.layout.hidden_apps_list);

        mPackageManager = getPackageManager();
        mAppsAdapter = new AppsAdapter(this, R.layout.hidden_apps_list_item);
        mAppsAdapter.setNotifyOnChange(true);

        mListView = (ListView) findViewById(R.id.protected_apps_list);
        mListView.setAdapter(mAppsAdapter);

        mProtect = new ArrayList<ComponentName>();

        if (savedInstanceState != null) {
            mUserIsAuth = savedInstanceState.getBoolean(NEEDS_UNLOCK);
            mTargetIntent = savedInstanceState.getParcelable(TARGET_INTENT);
        } else {
            if (!mUserIsAuth) {
                // Require unlock
                mWaitUserAuth = true;
                Intent lockPattern = new Intent(this, LockPatternActivity.class);
                startActivityForResult(lockPattern, REQ_ENTER_PATTERN);
            } else {
                //LAUNCH
                if (mTargetIntent != null) {
                    launchTargetActivityInfoAndFinish();
                }
            }
        }
        mOrientation = getResources().getConfiguration().orientation;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(NEEDS_UNLOCK, mUserIsAuth);
        outState.putParcelable(TARGET_INTENT, mTargetIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        AsyncTask<Void, Void, List<AppEntry>> refreshAppsTask =
                new AsyncTask<Void, Void, List<AppEntry>>() {

                    @Override
                    protected void onPostExecute(List<AppEntry> apps) {
                        mAppsAdapter.clear();
                        mAppsAdapter.addAll(apps);
                    }

                    @Override
                    protected List<AppEntry> doInBackground(Void... params) {
                        return refreshApps();
                    }
                };
        refreshAppsTask.execute(null, null, null);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        // Update Protected Apps list
        updateProtectedComponentsList();
    }

    private void updateProtectedComponentsList() {
        String protectedComponents = CMSettings.Secure.getString(getContentResolver(),
                CMSettings.Secure.PROTECTED_COMPONENTS);
        protectedComponents = protectedComponents == null ? "" : protectedComponents;
        String [] flattened = protectedComponents.split("\\|");
        mProtectedApps = new HashSet<ComponentName>(flattened.length);
        for (String flat : flattened) {
            ComponentName cmp = ComponentName.unflattenFromString(flat);
            if (cmp != null) {
                mProtectedApps.add(cmp);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        // Close this app to prevent unauthorized access when
        // 1) not waiting for authorization and
        // 2) there is no portrait/landscape mode switching
        if (!mWaitUserAuth && (mOrientation == getResources().getConfiguration().orientation)) {
            finish();
        }
    }

    private boolean getProtectedStateFromComponentName(ComponentName componentName) {
        return mProtectedApps.contains(componentName);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQ_ENTER_PATTERN:
                mWaitUserAuth = false;
                switch (resultCode) {
                    case RESULT_OK:
                        //Nothing to do, proceed!
                        mUserIsAuth = true;
                        if (mTargetIntent != null) {
                            launchTargetActivityInfoAndFinish();
                        }
                        break;
                    case RESULT_CANCELED:
                        // user failed to define a pattern, do not lock the folder
                        finish();
                        break;
                }
                break;
            case REQ_RESET_PATTERN:
                mWaitUserAuth = false;
                mUserIsAuth = false;
        }
    }

    private void launchTargetActivityInfoAndFinish() {
        Intent launchIntent = mTargetIntent;
        startActivity(launchIntent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_RESET, 0, R.string.menu_hidden_apps_delete)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(0, MENU_RESET_LOCK, 0, R.string.menu_hidden_apps_reset_lock)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }

    private void reset() {
        ArrayList<ComponentName> componentsList = new ArrayList<ComponentName>();

        // Check to see if any components that have been protected that aren't present in
        // the ListView. This can happen if there are components which have been protected
        // but do not respond to the queryIntentActivities for Launcher Category
        ContentResolver resolver = getContentResolver();
        String hiddenComponents = CMSettings.Secure.getString(resolver,
                CMSettings.Secure.PROTECTED_COMPONENTS);

        if (hiddenComponents != null && !hiddenComponents.equals("")) {
            for (String flattened : hiddenComponents.split("\\|")) {
                ComponentName cmp = ComponentName.unflattenFromString(flattened);

                if (!componentsList.contains(cmp)) {
                    componentsList.add(cmp);
                }
            }
        }

        AppProtectList list = new AppProtectList(componentsList,
                PackageManager.COMPONENT_VISIBLE_STATUS);
        StoreComponentProtectedStatus task = new StoreComponentProtectedStatus(this);
        task.execute(list);
    }

    private void resetLock() {
        mWaitUserAuth = true;
        Intent lockPattern = new Intent(LockPatternActivity.RECREATE_PATTERN, null,
                this, LockPatternActivity.class);
        startActivityForResult(lockPattern, REQ_RESET_PATTERN);
    }

    private List<AppEntry> refreshApps() {
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = mPackageManager.queryIntentActivities(mainIntent, 0);
        Collections.sort(apps, new ResolveInfo.DisplayNameComparator(mPackageManager));
        List<AppEntry> appEntries = new ArrayList<AppEntry>(apps.size());
        for (ResolveInfo info : apps) {
            appEntries.add(new AppEntry(info));
        }
        return appEntries;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                reset();
                return true;
            case MENU_RESET_LOCK:
                resetLock();
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private final class AppEntry {
        public final ComponentName componentName;
        public final String title;

        public AppEntry(ResolveInfo info) {
            ActivityInfo aInfo = info.activityInfo;
            componentName = new ComponentName(aInfo.packageName, aInfo.name);
            title = info.loadLabel(mPackageManager).toString();
        }
    }

    private final class AppProtectList {
        public final ArrayList<ComponentName> componentNames;
        public final boolean state;

        public AppProtectList(ArrayList<ComponentName> componentNames, boolean state) {
            this.componentNames = new ArrayList<ComponentName>();
            for (ComponentName cn : componentNames) {
                this.componentNames.add(cn.clone());
            }

            this.state = state;
        }
    }

    public class StoreComponentProtectedStatus extends AsyncTask<AppProtectList, Void, Void> {
        private ProgressDialog mDialog;
        private Context mContext;

        public StoreComponentProtectedStatus(Context context) {
            mContext = context;
            mDialog = new ProgressDialog(mContext);
        }

        @Override
        protected void onPreExecute() {
            mDialog.setMessage(getResources().getString(R.string.saving_protected_components));
            mDialog.setCancelable(false);
            mDialog.setCanceledOnTouchOutside(false);
            mDialog.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (mDialog.isShowing()) {
                mDialog.dismiss();
            }

            mAppsAdapter.notifyDataSetChanged();
        }

        @Override
        protected Void doInBackground(final AppProtectList... args) {
            for (AppProtectList appList : args) {
                ProtectedAppsReceiver.updateProtectedAppComponentsAndNotify(mContext,
                        appList.componentNames, appList.state);
            }

            updateProtectedComponentsList();
            return null;
        }
    }

    /**
     * App view holder used to reuse the views inside the list.
     */
    private static class AppViewHolder {
        public final View container;
        public final TextView title;
        public final ImageView icon;
        public final View launch;
        public final CheckBox checkBox;

        public AppViewHolder(View parentView) {
            container = parentView.findViewById(R.id.app_item);
            icon = (ImageView) parentView.findViewById(R.id.icon);
            title = (TextView) parentView.findViewById(R.id.title);
            launch = parentView.findViewById(R.id.launch_app);
            checkBox = (CheckBox) parentView.findViewById(R.id.checkbox);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public class AppsAdapter extends ArrayAdapter<AppEntry> {

        private final LayoutInflater mInflator;

        private ConcurrentHashMap<String, Drawable> mIcons;
        private Drawable mDefaultImg;
        private List<AppEntry> mApps;

        public AppsAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);

            mApps = new ArrayList<AppEntry>();

            mInflator = LayoutInflater.from(context);

            // set the default icon till the actual app icon is loaded in async task
            mDefaultImg = context.getResources().getDrawable(android.R.mipmap.sym_def_app_icon);
            mIcons = new ConcurrentHashMap<String, Drawable>();
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            AppViewHolder viewHolder;

            if (convertView == null) {
                convertView = mInflator.inflate(R.layout.hidden_apps_list_item, parent, false);
                viewHolder = new AppViewHolder(convertView);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (AppViewHolder) convertView.getTag();
            }

            AppEntry app = getItem(position);

            viewHolder.title.setText(app.title);

            Drawable icon = mIcons.get(app.componentName.getPackageName());
            viewHolder.icon.setImageDrawable(icon != null ? icon : mDefaultImg);

            boolean state = getProtectedStateFromComponentName(app.componentName);
            viewHolder.checkBox.setChecked(state);
            if (state) {
                viewHolder.launch.setVisibility(View.VISIBLE);
                viewHolder.launch.setTag(app);
                viewHolder.launch.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ComponentName cName = ((AppEntry)v.getTag()).componentName;
                        Intent intent = new Intent();
                        intent.setClassName(cName.getPackageName(), cName.getClassName());
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                });
            } else {
                viewHolder.launch.setVisibility(View.GONE);
            }

            viewHolder.container.setTag(position);
            viewHolder.container.setOnClickListener(mAppClickListener);
            return convertView;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
            // If we have new items, we have to load their icons
            // If items were deleted, remove them from our mApps
            List<AppEntry> newApps = new ArrayList<AppEntry>(getCount());
            List<AppEntry> oldApps = new ArrayList<AppEntry>(getCount());
            for (int i = 0; i < getCount(); i++) {
                AppEntry app = getItem(i);
                if (mApps.contains(app)) {
                    oldApps.add(app);
                } else {
                    newApps.add(app);
                }
            }

            if (newApps.size() > 0) {
                new LoadIconsTask().execute(newApps.toArray(new AppEntry[] {}));
                newApps.addAll(oldApps);
                mApps = newApps;
            } else {
                mApps = oldApps;
            }
        }

        /**
         * An asynchronous task to load the icons of the installed applications.
         */
        private class LoadIconsTask extends AsyncTask<AppEntry, Void, Void> {
            @Override
            protected Void doInBackground(AppEntry... apps) {
                for (AppEntry app : apps) {
                    try {
                        String packageName = app.componentName.getPackageName();
                        if (mIcons.containsKey(packageName)) {
                            continue;
                        }
                        Drawable icon = mPackageManager.getApplicationIcon(packageName);
                        mIcons.put(packageName, icon);
                        publishProgress();
                    } catch (PackageManager.NameNotFoundException e) {
                        // ignored; app will show up with default image
                    }
                }

                return null;
            }

            @Override
            protected void onProgressUpdate(Void... progress) {
                notifyDataSetChanged();
            }
        }
    }

    private View.OnClickListener mAppClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int position = (Integer) v.getTag();
            ComponentName cn = mAppsAdapter.getItem(position).componentName;
            ArrayList<ComponentName> componentsList = new ArrayList<ComponentName>();
            componentsList.add(cn);
            boolean state = getProtectedStateFromComponentName(cn);

            AppProtectList list = new AppProtectList(componentsList, state);
            StoreComponentProtectedStatus task =
                    new StoreComponentProtectedStatus(ProtectedAppsActivity.this);
            task.execute(list);
        }
    };
}
