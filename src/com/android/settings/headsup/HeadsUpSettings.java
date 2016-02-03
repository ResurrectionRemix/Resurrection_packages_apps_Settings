/*
 * Copyright (C) 2014 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.headsup;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.cyanogenmod.PackageListAdapter;
import com.android.settings.cyanogenmod.PackageListAdapter.PackageItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cyanogenmod.providers.CMSettings;
import org.cyanogenmod.internal.logging.CMMetricsLogger;

public class HeadsUpSettings extends SettingsPreferenceFragment
        implements AdapterView.OnItemLongClickListener, Preference.OnPreferenceClickListener, OnPreferenceChangeListener {

    private static final int DIALOG_DND_APPS = 0;
    private static final int DIALOG_BLACKLIST_APPS = 1;
    private static final int DIALOG_WHITELIST_APPS = 2;

    private static final String PREF_HEADS_UP_TIME_OUT = "heads_up_time_out";
    private static final String HEADS_UP_SNOOZE_LENGTH_MS = "heads_up_snooze_length_ms";

    private ListPreference mHeadsUpTimeOut;
    private ListPreference mHeadsUpSnooze;

    private PackageListAdapter mPackageAdapter;
    private PackageManager mPackageManager;
    private PreferenceGroup mDndPrefList;
    private PreferenceGroup mBlacklistPrefList;
    private PreferenceGroup mWhitelistPrefList;
    private Preference mAddDndPref;
    private Preference mAddBlacklistPref;
    private Preference mAddWhitelistPref;

    private String mDndPackageList;
    private String mBlacklistPackageList;
    private String mWhitelistPackageList;
    private Map<String, Package> mDndPackages;
    private Map<String, Package> mBlacklistPackages;
    private Map<String, Package> mWhitelistPackages;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get launch-able applications
        addPreferencesFromResource(R.xml.heads_up_settings);
        mPackageManager = getPackageManager();
        mPackageAdapter = new PackageListAdapter(getActivity());

        Resources systemUiResources;
        try {
            systemUiResources = mPackageManager.getResourcesForApplication("com.android.systemui");
        } catch (Exception e) {
            return;
        }

        int defaultTimeOut = systemUiResources.getInteger(systemUiResources.getIdentifier(
                    "com.android.systemui:integer/heads_up_notification_decay", null, null));
        mHeadsUpTimeOut = (ListPreference) findPreference(PREF_HEADS_UP_TIME_OUT);
        mHeadsUpTimeOut.setOnPreferenceChangeListener(this);
        int headsUpTimeOut = Settings.System.getInt(getContentResolver(),
                Settings.System.HEADS_UP_TIMEOUT, defaultTimeOut);
        mHeadsUpTimeOut.setValue(String.valueOf(headsUpTimeOut));
        updateHeadsUpTimeOutSummary(headsUpTimeOut);

        int defaultSnooze = systemUiResources.getInteger(systemUiResources.getIdentifier(
                    "com.android.systemui:integer/heads_up_default_snooze_length_ms", null, null));
        mHeadsUpSnooze = (ListPreference) findPreference(HEADS_UP_SNOOZE_LENGTH_MS);
        mHeadsUpSnooze.setOnPreferenceChangeListener(this);
        int headsUpSnooze = Settings.System.getInt(getContentResolver(),
                Settings.System.HEADS_UP_SNOOZE_LENGTH_MS, defaultSnooze);
        mHeadsUpSnooze.setValue(String.valueOf(headsUpSnooze));
        updateHeadsUpSnoozeSummary(headsUpSnooze);

        mDndPrefList = (PreferenceGroup) findPreference("dnd_applications_list");
        mDndPrefList.setOrderingAsAdded(false);

        mBlacklistPrefList = (PreferenceGroup) findPreference("blacklist_applications");
        mBlacklistPrefList.setOrderingAsAdded(false);

        mWhitelistPrefList = (PreferenceGroup) findPreference("whitelist_applications");
        mWhitelistPrefList.setOrderingAsAdded(false);

        mDndPackages = new HashMap<String, Package>();
        mBlacklistPackages = new HashMap<String, Package>();
        mWhitelistPackages = new HashMap<String, Package>();

        mAddDndPref = findPreference("add_dnd_packages");
        mAddBlacklistPref = findPreference("add_blacklist_packages");
        mAddWhitelistPref = findPreference("add_whitelist_packages");

        mAddDndPref.setOnPreferenceClickListener(this);
        mAddBlacklistPref.setOnPreferenceClickListener(this);
        mAddWhitelistPref.setOnPreferenceClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshCustomApplicationPrefs();
        getListView().setOnItemLongClickListener(this);
    }

    /**
     * Utility classes and supporting methods
     */
    @Override
    public Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final Dialog dialog;
        final ListView list = new ListView(getActivity());
        list.setAdapter(mPackageAdapter);

        builder.setTitle(R.string.profile_choose_app);
        builder.setView(list);
        dialog = builder.create();

        final Toast toast = Toast.makeText(getActivity(), R.string.heads_up_contains_key,
                Toast.LENGTH_SHORT);

        switch (id) {
            case DIALOG_DND_APPS:
                list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent,
                            View view, int position, long id) {
                        PackageItem info = (PackageItem) parent.getItemAtPosition(position);
                        if (!isPackageAlreadyAdded(info.packageName, true)) {
                            addCustomApplicationPref(info.packageName, mDndPackages);
                            dialog.cancel();
                        } else {
                            toast.show();
                        }
                    }
                });
                break;
            case DIALOG_BLACKLIST_APPS:
                list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent,
                            View view, int position, long id) {
                        PackageItem info = (PackageItem) parent.getItemAtPosition(position);
                        if (!isPackageAlreadyAdded(info.packageName, false)) {
                            addCustomApplicationPref(info.packageName, mBlacklistPackages);
                            dialog.cancel();
                        } else {
                            toast.show();
                        }
                    }
                });
                break;
            case DIALOG_WHITELIST_APPS:
                list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent,
                            View view, int position, long id) {
                        PackageItem info = (PackageItem) parent.getItemAtPosition(position);
                        if (!isPackageAlreadyAdded(info.packageName, false)) {
                            addCustomApplicationPref(info.packageName, mWhitelistPackages);
                            dialog.cancel();
                        } else {
                            toast.show();
                        }
                    }
                });
        }
        return dialog;
    }

    @Override
    protected int getMetricsCategory() {
        return CMMetricsLogger.DONT_LOG;
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
        if (mDndPrefList != null && mBlacklistPrefList != null && mWhitelistPrefList != null) {
            mDndPrefList.removeAll();
            mBlacklistPrefList.removeAll();
            mWhitelistPrefList.removeAll();

            for (Package pkg : mDndPackages.values()) {
                try {
                    Preference pref = createPreferenceFromInfo(pkg);
                    mDndPrefList.addPreference(pref);
                } catch (PackageManager.NameNotFoundException e) {
                    // Do nothing
                }
            }

            for (Package pkg : mBlacklistPackages.values()) {
                try {
                    Preference pref = createPreferenceFromInfo(pkg);
                    mBlacklistPrefList.addPreference(pref);
                } catch (PackageManager.NameNotFoundException e) {
                    // Do nothing
                }
            }

            for (Package pkg : mWhitelistPackages.values()) {
                try {
                    Preference pref = createPreferenceFromInfo(pkg);
                    mWhitelistPrefList.addPreference(pref);
                } catch (PackageManager.NameNotFoundException e) {
                    // Do nothing
                }
            }
        }

        // Keep these at the top
        mAddDndPref.setOrder(0);
        mAddBlacklistPref.setOrder(0);
        mAddWhitelistPref.setOrder(0);
        // Add 'add' options
        mDndPrefList.addPreference(mAddDndPref);
        mBlacklistPrefList.addPreference(mAddBlacklistPref);
        mWhitelistPrefList.addPreference(mAddWhitelistPref);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mHeadsUpTimeOut) {
            int headsUpTimeOut = Integer.valueOf((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.HEADS_UP_TIMEOUT,
                    headsUpTimeOut);
            updateHeadsUpTimeOutSummary(headsUpTimeOut);
            return true;
        } else if (preference == mHeadsUpSnooze) {
            int headsUpSnooze = Integer.valueOf((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.HEADS_UP_SNOOZE_LENGTH_MS,
                    headsUpSnooze);
            updateHeadsUpSnoozeSummary(headsUpSnooze);
            return true;
        }
        return false;
    }

    private void updateHeadsUpTimeOutSummary(int value) {
        String summary = getResources().getString(R.string.heads_up_time_out_summary,
                value / 1000);
        mHeadsUpTimeOut.setSummary(summary);
    }

    private void updateHeadsUpSnoozeSummary(int value) {
        String summary = getResources().getString(R.string.heads_up_snooze_summary,
                value / 60000);
        mHeadsUpSnooze.setSummary(summary);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mAddDndPref) {
            showDialog(DIALOG_DND_APPS);
        } else if (preference == mAddBlacklistPref) {
            showDialog(DIALOG_BLACKLIST_APPS);
        } else if (preference == mAddWhitelistPref) {
            showDialog(DIALOG_WHITELIST_APPS);
        }
        return true;
    }

    private void addCustomApplicationPref(String packageName, Map<String,Package> map) {
        Package pkg = new Package(packageName);
        map.put(packageName, pkg);
        savePackageList(false, map);
        refreshCustomApplicationPrefs();
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

        final String dndString = CMSettings.System.getString(getContentResolver(),
                CMSettings.System.HEADS_UP_CUSTOM_VALUES);
        final String blacklistString = CMSettings.System.getString(getContentResolver(),
                CMSettings.System.HEADS_UP_BLACKLIST_VALUES);
        final String whitelistString = CMSettings.System.getString(getContentResolver(),
                CMSettings.System.HEADS_UP_WHITELIST_VALUES);

        if (!TextUtils.equals(mDndPackageList, dndString)) {
            mDndPackageList = dndString;
            mDndPackages.clear();
            parseAndAddToMap(dndString, mDndPackages);
            parsed = true;
        }

        if (!TextUtils.equals(mBlacklistPackageList, blacklistString)) {
            mBlacklistPackageList = blacklistString;
            mBlacklistPackages.clear();
            parseAndAddToMap(blacklistString, mBlacklistPackages);
            parsed = true;
        }

        if (!TextUtils.equals(mWhitelistPackageList, whitelistString)) {
            mWhitelistPackageList = whitelistString;
            mWhitelistPackages.clear();
            parseAndAddToMap(whitelistString, mWhitelistPackages);
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
        String setting;
        if (map == mDndPackages) {
            setting = CMSettings.System.HEADS_UP_CUSTOM_VALUES;
        } else if (map == mBlacklistPackages) {
            setting = CMSettings.System.HEADS_UP_BLACKLIST_VALUES;
        } else {
            setting = CMSettings.System.HEADS_UP_WHITELIST_VALUES;
        }

        List<String> settings = new ArrayList<String>();
        for (Package app : map.values()) {
            settings.add(app.toString());
        }
        final String value = TextUtils.join("|", settings);
        if (preferencesUpdated) {
            if (map == mDndPackages) {
                mDndPackageList = value;
            } else if (map == mBlacklistPackages) {
                mBlacklistPackageList = value;
            } else {
                mWhitelistPackageList = value;
            }
        }
        CMSettings.System.putString(getContentResolver(),
                setting, value);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        final Preference pref =
                (Preference) getPreferenceScreen().getRootAdapter().getItem(position);

        if ((mBlacklistPrefList.findPreference(pref.getKey()) != pref)
                && (mDndPrefList.findPreference(pref.getKey()) != pref)
                && (mWhitelistPrefList.findPreference(pref.getKey()) != pref)) {
            return false;
        }

        if (mAddDndPref == pref || mAddBlacklistPref == pref || mAddWhitelistPref == pref) {
            return false;
        }


        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.dialog_delete_title)
                .setMessage(R.string.dialog_delete_message)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mBlacklistPrefList.findPreference(pref.getKey()) == pref) {
                            removeApplicationPref(pref.getKey(), mBlacklistPackages);
                        } else if (mWhitelistPrefList.findPreference(pref.getKey()) == pref) {
                            removeApplicationPref(pref.getKey(), mWhitelistPackages);
                        } else if (mDndPrefList.findPreference(pref.getKey()) == pref) {
                            removeApplicationPref(pref.getKey(), mDndPackages);
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null);

        builder.show();
        return true;
    }

    private boolean isPackageAlreadyAdded(String packageName, boolean isDndList) {
        boolean result = false;
        if (isDndList) {
            result = mDndPackages.containsKey(packageName);
        } else {
            result = mBlacklistPackages.containsKey(packageName)
                    || mWhitelistPackages.containsKey(packageName);
        }

        return result;
    }
}
