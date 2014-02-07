/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.settings;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RadioButton;

import java.util.ArrayList;
import java.util.List;

public class HomeSettings extends SettingsPreferenceFragment {
    static final String TAG = "HomeSettings";

    static final int REQUESTING_UNINSTALL = 10;

    public static final String HOME_PREFS = "home_prefs";
    public static final String HOME_PREFS_DO_SHOW = "do_show";

    public static final String HOME_SHOW_NOTICE = "show";

    PreferenceGroup mPrefGroup;

    PackageManager mPm;
    ComponentName[] mHomeComponentSet;
    ArrayList<HomeAppPreference> mPrefs;
    HomeAppPreference mCurrentHome = null;
    final IntentFilter mHomeFilter;
    boolean mShowNotice;

    public HomeSettings() {
        mHomeFilter = new IntentFilter(Intent.ACTION_MAIN);
        mHomeFilter.addCategory(Intent.CATEGORY_HOME);
        mHomeFilter.addCategory(Intent.CATEGORY_DEFAULT);
    }

    OnClickListener mHomeClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            int index = (Integer)v.getTag();
            HomeAppPreference pref = mPrefs.get(index);
            if (!pref.isChecked) {
                makeCurrentHome(pref);
            }
        }
    };

    OnClickListener mDeleteClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            int index = (Integer)v.getTag();
            uninstallApp(mPrefs.get(index));
        }
    };

    void makeCurrentHome(HomeAppPreference newHome) {
        if (mCurrentHome != null) {
            mCurrentHome.setChecked(false);
        }
        newHome.setChecked(true);
        mCurrentHome = newHome;

        mPm.replacePreferredActivity(mHomeFilter, IntentFilter.MATCH_CATEGORY_EMPTY,
                mHomeComponentSet, newHome.activityName);
    }

    void uninstallApp(HomeAppPreference pref) {
        // Uninstallation is done by asking the OS to do it
       Uri packageURI = Uri.parse("package:" + pref.uninstallTarget);
       Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageURI);
       uninstallIntent.putExtra(Intent.EXTRA_UNINSTALL_ALL_USERS, false);
       int requestCode = REQUESTING_UNINSTALL + (pref.isChecked ? 1 : 0);
       startActivityForResult(uninstallIntent, requestCode);
   }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Rebuild the list now that we might have nuked something
        buildHomeActivitiesList();

        // if the previous home app is now gone, fall back to the system one
        if (requestCode > REQUESTING_UNINSTALL) {
            // if mCurrentHome has gone null, it means we didn't find the previously-
            // default home app when rebuilding the list, i.e. it was the one we
            // just uninstalled.  When that happens we make the system-bundled
            // home app the active default.
            if (mCurrentHome == null) {
                for (int i = 0; i < mPrefs.size(); i++) {
                    HomeAppPreference pref = mPrefs.get(i);
                    if (pref.isSystem) {
                        makeCurrentHome(pref);
                        break;
                    }
                }
            }
        }

        boolean hasSettingsPanel = false;
        for (HomeAppPreference pref : mPrefs) {
            if (pref.prefsIntent != null) {
                hasSettingsPanel = true;
                break;
            }
        }

        // If we're down to just one possible home app, back out of this settings
        // fragment and show a dialog explaining to the user that they won't see
        // 'Home' settings now until such time as there are multiple available.
        if (mPrefs.size() < 2 && !hasSettingsPanel) {
            if (mShowNotice) {
                mShowNotice = false;
                Settings.requestHomeNotice();
            }
            finishFragment();
        }
    }

    void buildHomeActivitiesList() {
        ArrayList<ResolveInfo> homeActivities = new ArrayList<ResolveInfo>();
        ComponentName currentDefaultHome  = mPm.getHomeActivities(homeActivities);

        Intent prefsIntent = new Intent(Intent.ACTION_MAIN);
        prefsIntent.addCategory("com.cyanogenmod.category.LAUNCHER_PREFERENCES");
        List<ResolveInfo> prefsActivities = mPm.queryIntentActivities(prefsIntent, 0);

        Context context = getActivity();
        mCurrentHome = null;
        mPrefGroup.removeAll();
        mPrefs = new ArrayList<HomeAppPreference>();
        mHomeComponentSet = new ComponentName[homeActivities.size()];
        int prefIndex = 0;
        for (int i = 0; i < homeActivities.size(); i++) {
            final ResolveInfo candidate = homeActivities.get(i);
            final ActivityInfo info = candidate.activityInfo;
            Intent resolvedPrefsIntent = null;
            ComponentName activityName = new ComponentName(info.packageName, info.name);
            mHomeComponentSet[i] = activityName;

            for (ResolveInfo prefInfo : prefsActivities) {
                if (info.packageName.equals(prefInfo.activityInfo.packageName)) {
                    resolvedPrefsIntent = new Intent(prefsIntent);
                    resolvedPrefsIntent.setPackage(info.packageName);
                    break;
                }
            }

            try {
                Drawable icon = info.loadIcon(mPm);
                CharSequence name = info.loadLabel(mPm);
                HomeAppPreference pref = new HomeAppPreference(context, activityName, prefIndex,
                        icon, name, this, info, resolvedPrefsIntent);
                mPrefs.add(pref);
                mPrefGroup.addPreference(pref);
                pref.setEnabled(true);
                if (activityName.equals(currentDefaultHome)) {
                    mCurrentHome = pref;
                }
                prefIndex++;
            } catch (Exception e) {
                Log.v(TAG, "Problem dealing with activity " + activityName, e);
            }
        }

        if (mCurrentHome != null) {
            new Handler().post(new Runnable() {
               public void run() {
                   mCurrentHome.setChecked(true);
               }
            });
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.home_selection);

        mPm = getPackageManager();
        mPrefGroup = (PreferenceGroup) findPreference("home");

        Bundle args = getArguments();
        mShowNotice = (args != null) && args.getBoolean(HOME_SHOW_NOTICE, false);

        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        buildHomeActivitiesList();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        HomeAppPreference selectedPref = null;

        for (HomeAppPreference pref : mPrefs) {
            if (pref.isChecked) {
                selectedPref = pref;
                break;
            }
        }

        super.onCreateOptionsMenu(menu, inflater);

        if (selectedPref != null && selectedPref.prefsIntent != null) {
            menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.settings_label)
                    .setIntent(selectedPref.prefsIntent)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
    }

    class HomeAppPreference extends Preference {
        ComponentName activityName;
        int index;
        HomeSettings fragment;
        final ColorFilter grayscaleFilter;
        boolean isChecked;
        final Intent prefsIntent;

        boolean isSystem;
        String uninstallTarget;

        public HomeAppPreference(Context context, ComponentName activity,
                int i, Drawable icon, CharSequence title,
                HomeSettings parent, ActivityInfo info, Intent prefsIntent) {
            super(context);
            setLayoutResource(R.layout.preference_home_app);
            setIcon(icon);
            setTitle(title);
            activityName = activity;
            fragment = parent;
            index = i;
            this.prefsIntent = prefsIntent;

            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(0f);
            float[] matrix = colorMatrix.getArray();
            matrix[18] = 0.5f;
            grayscaleFilter = new ColorMatrixColorFilter(colorMatrix);

            determineTargets(info);
        }

        // Check whether this activity is bundled on the system, with awareness
        // of the META_HOME_ALTERNATE mechanism.
        private void determineTargets(ActivityInfo info) {
            final Bundle meta = info.metaData;
            if (meta != null) {
                final String altHomePackage = meta.getString(ActivityManager.META_HOME_ALTERNATE);
                if (altHomePackage != null) {
                    try {
                        final int match = mPm.checkSignatures(info.packageName, altHomePackage);
                        if (match >= PackageManager.SIGNATURE_MATCH) {
                            PackageInfo altInfo = mPm.getPackageInfo(altHomePackage, 0);
                            final int altFlags = altInfo.applicationInfo.flags;
                            isSystem = (altFlags & ApplicationInfo.FLAG_SYSTEM) != 0;
                            uninstallTarget = altInfo.packageName;
                            return;
                        }
                    } catch (Exception e) {
                        // e.g. named alternate package not found during lookup
                        Log.w(TAG, "Unable to compare/resolve alternate", e);
                    }
                }
            }
            // No suitable metadata redirect, so use the package's own info
            isSystem = (info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            uninstallTarget = info.packageName;
        }

        @Override
        protected void onBindView(View view) {
            super.onBindView(view);

            RadioButton radio = (RadioButton) view.findViewById(R.id.home_radio);
            radio.setChecked(isChecked);

            Integer indexObj = new Integer(index);

            ImageView icon = (ImageView) view.findViewById(R.id.home_app_uninstall);
            if (isSystem) {
                icon.setEnabled(false);
                icon.setColorFilter(grayscaleFilter);
            } else {
                icon.setOnClickListener(mDeleteClickListener);
                icon.setTag(indexObj);
            }

            View v = view.findViewById(R.id.home_app_pref);
            v.setOnClickListener(mHomeClickListener);
            v.setTag(indexObj);
        }

        void setChecked(boolean state) {
            if (state != isChecked) {
                isChecked = state;
                notifyChanged();
                getActivity().invalidateOptionsMenu();
            }
        }
    }
}
