/*
 * Copyright (C) 2017 The ABC rom
 * Copyright (C) 2017 AICP
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

package com.android.settings.rr.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.RadioButton;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.rr.utils.PackageListAdapter;
import com.android.settings.rr.utils.PackageListAdapter.PackageItem;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.rr.utils.RRUtils;
import com.android.settings.search.Indexable.SearchIndexProvider;

public class SlimRecentsBlacklist extends SettingsPreferenceFragment implements
        Preference.OnPreferenceClickListener {

    private static final String TAG = "SlimRecentsBlacklist";

    private static final int DIALOG_BLACKLIST_APPS = 1;

    private PackageListAdapter mPackageAdapter;
    private PackageManager mPackageManager;
    private PreferenceGroup mBlacklistPrefList;
    private Preference mAddBlacklistPref;
    private String mBlacklistPackageList;
    private Map<String, Package> mBlacklistPackages;

    private AlertDialog mDialog;
    private ListView mListView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.slim_recents_blacklist);
        initializeAllPreferences();
    }

    protected void showDialog(int dialogId) {
        switch (dialogId) {
            case DIALOG_BLACKLIST_APPS: {
                Dialog dialog;
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                final ListView list = new ListView(getActivity());
                list.setAdapter(mPackageAdapter);
                alertDialog.setTitle(R.string.profile_choose_app);
                alertDialog.setView(list);
                dialog = alertDialog.create();
                list.setOnItemClickListener(new OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        // Add empty application definition, the user will be able to edit it later
                        PackageItem info = (PackageItem) parent.getItemAtPosition(position);
                        addCustomApplicationPref(info.packageName, mBlacklistPackages);
                        dialog.cancel();
                    }
                });
                dialog.show();
                break;
            }
         }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshCustomApplicationPrefs();
    }

    private void initializeAllPreferences() {
        mPackageManager = getActivity().getPackageManager();
        mPackageAdapter = new PackageListAdapter(getActivity());
        //mBlacklistPrefList = (PreferenceGroup) findPreference("blacklist_applications");
        mBlacklistPrefList = getPreferenceScreen();
        mBlacklistPrefList.setOrderingAsAdded(false);
        mBlacklistPackages = new HashMap<String, Package>();
        mAddBlacklistPref = findPreference("add_blacklist_packages");
        mAddBlacklistPref.setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mAddBlacklistPref) {
            showDialog(DIALOG_BLACKLIST_APPS);
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.dialog_delete_title)
                        .setMessage(R.string.dialog_delete_message)
                        //.setIconAttribute(android.R.attr.alertDialogIcon)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                removeApplicationPref(preference.getKey(), mBlacklistPackages);
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null);

            builder.show();
        }
        return true;
    }

    /**
     * Application class
     */
    private static class Package {
        public String name;
        /**
         * Stores all the application values in one call
         * @param name
         */
        public Package(String name) {
            this.name = name;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(name);
            return builder.toString();
        }

        public static Package fromString(String value) {
            if (TextUtils.isEmpty(value)) {
                return null;
            }

            try {
                Package item = new Package(value);
                return item;
            } catch (NumberFormatException e) {
                return null;
            }
        }

    };

    private void refreshCustomApplicationPrefs() {
        if (!parsePackageList()) {
            return;
        }

        // Add the Application Preferences
        if (mBlacklistPrefList != null) {
            mBlacklistPrefList.removeAll();

            for (Package pkg : mBlacklistPackages.values()) {
                try {
                    Preference pref = createPreferenceFromInfo(pkg);
                    mBlacklistPrefList.addPreference(pref);
                } catch (PackageManager.NameNotFoundException e) {
                    // Do nothing
                }
            }
        }

        // Keep these at the top
        mAddBlacklistPref.setOrder(0);
        // Add 'add' options
        mBlacklistPrefList.addPreference(mAddBlacklistPref);
    }

     private void addCustomApplicationPref(String packageName, Map<String,Package> map) {
        Package pkg = map.get(packageName);
        if (pkg == null) {
            pkg = new Package(packageName);
            map.put(packageName, pkg);
            savePackageList(false, map);
            refreshCustomApplicationPrefs();
        }
    }

    private Preference createPreferenceFromInfo(Package pkg)
            throws PackageManager.NameNotFoundException {
        PackageInfo info = mPackageManager.getPackageInfo(pkg.name,
                PackageManager.GET_META_DATA);
        Preference pref =
                new Preference(getActivity());

        pref.setKey(pkg.name);
        pref.setTitle(info.applicationInfo.loadLabel(mPackageManager));
        pref.setIcon(info.applicationInfo.loadIcon(mPackageManager));
        pref.setPersistent(false);
        pref.setOnPreferenceClickListener(this);
        return pref;
    }

    private void removeApplicationPref(String packageName, Map<String,Package> map) {
        if (map.remove(packageName) != null) {
            savePackageList(false, map);
            refreshCustomApplicationPrefs();
        }
    }

    private boolean parsePackageList() {
        boolean parsed = false;
        final String blacklistString = Settings.System.getString(getActivity().getContentResolver(),
                Settings.System.SLIM_RECENTS_BLACKLIST_VALUES);

        if (!TextUtils.equals(mBlacklistPackageList, blacklistString)) {
            mBlacklistPackageList = blacklistString;
            mBlacklistPackages.clear();
            parseAndAddToMap(blacklistString, mBlacklistPackages);
            parsed = true;
        }

        return parsed;
    }

    private void parseAndAddToMap(String baseString, Map<String,Package> map) {
        if (baseString == null) {
            return;
        }

        final String[] array = TextUtils.split(baseString, "\\|");
        for (String item : array) {
            if (TextUtils.isEmpty(item)) {
                continue;
            }
            Package pkg = Package.fromString(item);
            map.put(pkg.name, pkg);
        }
    }

    private void savePackageList(boolean preferencesUpdated, Map<String,Package> map) {
        String setting = Settings.System.SLIM_RECENTS_BLACKLIST_VALUES;

        List<String> settings = new ArrayList<String>();
        for (Package app : map.values()) {
            settings.add(app.toString());
        }
        final String value = TextUtils.join("|", settings);
        if (preferencesUpdated) {
            mBlacklistPackageList = value;
        }
        Settings.System.putString(getActivity().getContentResolver(),
                setting, value);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        RRUtils.addSearchIndexProvider(R.xml.slim_recents_blacklist);
}
