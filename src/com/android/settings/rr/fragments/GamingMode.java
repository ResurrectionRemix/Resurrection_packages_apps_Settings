/*
 * Copyright (C) 2019 crDroid Android Project
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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import androidx.preference.SwitchPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.rr.utils.PackageListAdapter;
import com.android.settings.rr. utils.PackageListAdapter.PackageItem;

import com.android.settings.rr.Preferences.SystemSettingListPreference;
import com.android.settings.rr.Preferences.SystemSettingSwitchPreference;

import com.android.internal.util.rr.DeviceUtils;
import com.android.internal.util.rr.ActionConstants;
// com.android.internal.util.rr.ActionUtils;



import android.provider.SearchIndexableResource;
import com.android.internal.logging.nano.MetricsProto;

import com.android.settings.rr.utils.RRUtils;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
@SearchIndexable
public class GamingMode extends SettingsPreferenceFragment
        implements Preference.OnPreferenceClickListener, Indexable {

    private static final int DIALOG_GAMING_APPS = 1;
    private static final String GAMING_MODE_ENABLED = "gaming_mode_enabled";
    private static final String GAMING_MODE_HW_KEYS = "gaming_mode_hw_keys_toggle";

    private SystemSettingSwitchPreference mGamingModeEnabled;
    private SystemSettingSwitchPreference mHardwareKeysDisable;

    private PackageListAdapter mPackageAdapter;
    private PackageManager mPackageManager;
    private PreferenceGroup mGamingPrefList;
    private Preference mAddGamingPref;

    private String mGamingPackageList;
    private Map<String, Package> mGamingPackages;
    private Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.gaming_mode);

        PreferenceScreen prefScreen = getPreferenceScreen();

        mGamingModeEnabled = (SystemSettingSwitchPreference) findPreference(GAMING_MODE_ENABLED);
        mHardwareKeysDisable = (SystemSettingSwitchPreference) findPreference(GAMING_MODE_HW_KEYS);

        /*final boolean haveNavbar = ActionUtils.hasNavbarByDefault(getActivity());
        if (haveNavbar) {
            prefScreen.removePreference(mHardwareKeysDisable);
        }*/

        mPackageManager = getActivity().getPackageManager();
        mPackageAdapter = new PackageListAdapter(getActivity());

        mGamingPrefList = (PreferenceGroup) findPreference("gamingmode_applications");
        mGamingPrefList.setOrderingAsAdded(false);

        mGamingPackages = new HashMap<String, Package>();

        mAddGamingPref = findPreference("add_gamingmode_packages");

        mAddGamingPref.setOnPreferenceClickListener(this);

        mContext = getActivity().getApplicationContext();
        if (!isAvailable()) {
            try {
                removePreference("gaming_mode_refresh_rate");
            }
            catch (Exception e) {}
        }

        SettingsObserver observer = new SettingsObserver(new Handler(Looper.getMainLooper()));
        observer.observe();

        int anim = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.RR_CONFIG_ANIM, 0);
        try {
            if (anim == 0) {
                removePreference("animation");
            } else if (anim == 1) {
                removePreference("preview");
            } else if (anim == 2) {
                removePreference("animation");
                removePreference("preview");
            }
        } catch (Exception e) {}
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshCustomApplicationPrefs();
    }

    public boolean isAvailable() {
        return mContext.getResources().getBoolean(com.android.internal.R.bool.config_hasVariableRefreshRate);
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();

            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.GAMING_MODE_ACTIVE), false, this,
                    UserHandle.USER_ALL);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri.equals(Settings.System.getUriFor(
                                   Settings.System.GAMING_MODE_ACTIVE))) {
                boolean enable = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.GAMING_MODE_ACTIVE, 0) == 1;
                setGamingControls(!enable);
            }
        }
    }

    private void setGamingControls(boolean enable) {
        mGamingModeEnabled.setEnabled(enable);
    }

    public static void reset(Context mContext) {
        ContentResolver resolver = mContext.getContentResolver();
        Settings.System.putIntForUser(resolver,
                Settings.System.GAMING_MODE_ACTIVE, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.GAMING_MODE_ENABLED, 0, UserHandle.USER_CURRENT);
        Settings.System.putStringForUser(resolver,
                Settings.System.GAMING_MODE_VALUES, "", UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.GAMING_MODE_HW_KEYS_TOGGLE, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.GAMING_MODE_HEADSUP_TOGGLE, 1, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.GAMING_MODE_RINGER_MODE, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.GAMING_MODE_NOTIFICATIONS, 3, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.GAMING_MODE_MANUAL_BRIGHTNESS_TOGGLE, 1, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.GAMING_MODE_DYNAMIC_STATE, 0, UserHandle.USER_CURRENT);
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
        if (mGamingPrefList != null) {
            mGamingPrefList.removeAll();

            for (Package pkg : mGamingPackages.values()) {
                try {
                    Preference pref = createPreferenceFromInfo(pkg);
                    mGamingPrefList.addPreference(pref);
                } catch (PackageManager.NameNotFoundException e) {
                    // Do nothing
                }
            }
        }

        // Keep these at the top
        mAddGamingPref.setOrder(0);
        // Add 'add' options
        mGamingPrefList.addPreference(mAddGamingPref);
    }

    protected void showDialog(int dialogId) {
        switch (dialogId) {
            case DIALOG_GAMING_APPS: {
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
                        addCustomApplicationPref(info.packageName, mGamingPackages);
                        dialog.cancel();
                    }
                });
                dialog.show();
                break;
            }
         }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mAddGamingPref) {
            showDialog(DIALOG_GAMING_APPS);
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.dialog_delete_title)
                    .setMessage(R.string.dialog_delete_message)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (preference == mGamingPrefList.findPreference(preference.getKey())) {
                                removeApplicationPref(preference.getKey(), mGamingPackages);
                            }
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null);

            builder.show();
        }
        return true;
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

        final String gamingModeString = Settings.System.getString(getContentResolver(),
                Settings.System.GAMING_MODE_VALUES);

        if (!TextUtils.equals(mGamingPackageList, gamingModeString)) {
            mGamingPackageList = gamingModeString;
            mGamingPackages.clear();
            parseAndAddToMap(gamingModeString, mGamingPackages);
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
        String setting = map == mGamingPackages ? Settings.System.GAMING_MODE_VALUES : Settings.System.GAMING_MODE_DUMMY;

        List<String> settings = new ArrayList<String>();
        for (Package app : map.values()) {
            settings.add(app.toString());
        }
        final String value = TextUtils.join("|", settings);
        if (preferencesUpdated) {
            if (TextUtils.equals(setting, Settings.System.GAMING_MODE_VALUES)) {
                mGamingPackageList = value;
            }
        }
        Settings.System.putString(getContentResolver(),
                setting, value);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.RESURRECTED; //Error2
    }

    /**
     * For Search.
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean enabled) {
                ArrayList<SearchIndexableResource> result =
                    new ArrayList<SearchIndexableResource>();
                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.gaming_mode;
                    result.add(sir);
                    return result;
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                List<String> keys = super.getNonIndexableKeys(context);
                return keys;
            }
        };
}
