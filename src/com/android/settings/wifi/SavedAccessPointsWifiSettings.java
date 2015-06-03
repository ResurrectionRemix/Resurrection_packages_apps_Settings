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

package com.android.settings.wifi;

import static android.os.UserManager.DISALLOW_CONFIG_WIFI;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.android.settings.DraggableSortListView;
import com.android.settings.R;
import com.android.settings.RestrictedSettingsFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UI to manage saved networks/access points.
 */
public class SavedAccessPointsWifiSettings extends RestrictedSettingsFragment
        implements DialogInterface.OnClickListener, Indexable {

    private static final String TAG = "SavedAccessPointsWifiSettings";

    private DraggableSortListView.DropListener mDropListener =
            new DraggableSortListView.DropListener() {
        @Override
        public void drop(int from, int to) {
            if (from == to) return;

            PreferenceScreen preferences = getPreferenceScreen();
            int count = preferences.getPreferenceCount();

            // Sort the new list
            List<AccessPoint> aps = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                aps.add((AccessPoint) preferences.getPreference(i));
            }
            AccessPoint o = aps.remove(from);
            aps.add(to, o);

            // Update the priorities
            for (int i = 0; i < count; i++) {
                AccessPoint ap = aps.get(i);
                WifiConfiguration config = ap.getConfig();
                config.priority = count - i;

                mWifiManager.updateNetwork(config);
            }

            // Now, save all the Wi-Fi configuration with its new priorities
            mWifiManager.saveConfiguration();
            mPrioritiesOrderChanged = true;

            // Redraw the listview
            initPreferences();
        }
    };

    private final ContentObserver mSettingsObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            mNetworksListView.setDropListener(isAutoConfigPriorities() ? null : mDropListener);
            getActivity().invalidateOptionsMenu();
        }
    };


    private static final int MENU_ID_AUTO_CONFIG_PRIORITIES = Menu.FIRST;

    private WifiDialog mDialog;
    private WifiManager mWifiManager;
    private AccessPoint mDlgAccessPoint;
    private Bundle mAccessPointSavedState;
    private AccessPoint mSelectedAccessPoint;
    private boolean mPrioritiesOrderChanged;

    private DraggableSortListView mNetworksListView;

    // Instance state key
    private static final String SAVE_DIALOG_ACCESS_POINT_STATE = "wifi_ap_state";
    private static final String PRIORITIES_ORDER_CHANGED_STATE = "priorities_order_changed";

    public SavedAccessPointsWifiSettings() {
        super(DISALLOW_CONFIG_WIFI);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.wifi_display_saved_access_points);
    }

    @Override
    public void onResume() {
        super.onResume();
        initPreferences();

        mNetworksListView.setDropListener(isAutoConfigPriorities() ? null : mDropListener);
        getActivity().invalidateOptionsMenu();
        ContentResolver resolver = getContentResolver();
        resolver.registerContentObserver(Settings.Global.getUriFor(
                Settings.Global.WIFI_AUTO_PRIORITIES_CONFIGURATION), false, mSettingsObserver);
    }

    @Override
    public void onPause() {
        super.onResume();
        getContentResolver().unregisterContentObserver(mSettingsObserver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mNetworksListView = new DraggableSortListView(getActivity());
        mNetworksListView.setId(android.R.id.list);
        mNetworksListView.setDropListener(isAutoConfigPriorities() ? null : mDropListener);
        return mNetworksListView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(SAVE_DIALOG_ACCESS_POINT_STATE)) {
                mAccessPointSavedState =
                    savedInstanceState.getBundle(SAVE_DIALOG_ACCESS_POINT_STATE);
            }
            mPrioritiesOrderChanged = savedInstanceState.getBoolean(
                    PRIORITIES_ORDER_CHANGED_STATE, false);
        }

        registerForContextMenu(getListView());
        setHasOptionsMenu(true);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        if (mPrioritiesOrderChanged) {
            // Send a disconnect to ensure the new wifi priorities are detected
            mWifiManager.disconnect();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // If the user is not allowed to configure wifi, do not show the menu.
        if (isUiRestricted()) return;

        addOptionsMenuItems(menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    void addOptionsMenuItems(Menu menu) {
        menu.add(Menu.NONE, MENU_ID_AUTO_CONFIG_PRIORITIES, 0, R.string.wifi_auto_config_priorities)
                .setCheckable(true)
                .setChecked(isAutoConfigPriorities())
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // If the user is not allowed to configure wifi, do not handle menu selections.
        if (isUiRestricted()) return false;

        switch (item.getItemId()) {
            case MENU_ID_AUTO_CONFIG_PRIORITIES:
                boolean autoConfig = !item.isChecked();

                // Set the system settings and refresh the listview
                Settings.Global.putInt(getActivity().getContentResolver(),
                        Settings.Global.WIFI_AUTO_PRIORITIES_CONFIGURATION, autoConfig ? 1 : 0);
                mNetworksListView.setDropListener(autoConfig ? null : mDropListener);
                item.setChecked(autoConfig);

                if (!autoConfig) {
                    // Reenable all the entries
                    PreferenceScreen preferences = getPreferenceScreen();
                    int count = preferences.getPreferenceCount();
                    for (int i = 0; i < count; i++) {
                        AccessPoint ap = (AccessPoint) preferences.getPreference(i);
                        WifiConfiguration config = ap.getConfig();
                        mWifiManager.enableNetwork(config.networkId, false);
                    }
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initPreferences() {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        final Context context = getActivity();

        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        final List<AccessPoint> accessPoints = constructSavedAccessPoints(context, mWifiManager);

        preferenceScreen.setOrderingAsAdded(false);
        preferenceScreen.removeAll();

        final int accessPointsSize = accessPoints.size();
        for (int i = 0; i < accessPointsSize; ++i){
            final AccessPoint ap = accessPoints.get(i);
            ap.setOrder(i);
            preferenceScreen.addPreference(ap);
        }

        if(getPreferenceScreen().getPreferenceCount() < 1) {
            Log.w(TAG, "Saved networks activity loaded, but there are no saved networks!");
        }
    }

    private static List<WifiConfiguration> getConfiguredNetworks(WifiManager wifiManager) {
        List<WifiConfiguration> networks = wifiManager.getConfiguredNetworks();
        if (networks == null) {
            networks = new ArrayList<WifiConfiguration>();
        }
        return networks;
    }

    private static List<AccessPoint> constructSavedAccessPoints(Context context,
            WifiManager wifiManager){
        List<AccessPoint> accessPoints = new ArrayList<AccessPoint>();
        Map<String, List<ScanResult>> resultsMap = new HashMap<String, List<ScanResult>>();

        final List<WifiConfiguration> configs = getConfiguredNetworks(wifiManager);
        final List<ScanResult> scanResults = wifiManager.getScanResults();

        if (configs != null) {
            //Construct a Map for quick searching of a wifi network via ssid.
            final int scanResultsSize = scanResults.size();
            for (int i = 0; i < scanResultsSize; ++i){
                final ScanResult result = scanResults.get(i);
                List<ScanResult> res = resultsMap.get(result.SSID);

                if(res == null){
                    res = new ArrayList<ScanResult>();
                    resultsMap.put(result.SSID, res);
                }

                res.add(result);
            }

            final int configsSize = configs.size();
            for (int i = 0; i < configsSize; ++i){
                WifiConfiguration config = configs.get(i);
                if (config.selfAdded && config.numAssociation == 0) {
                    continue;
                }
                AccessPoint accessPoint = new AccessPoint(context, config, true);
                accessPoint.setSortPreference(false);;

                final List<ScanResult> results = resultsMap.get(accessPoint.ssid);
                if(results != null){
                    final int resultsSize = results.size();
                    for (int j = 0; j < resultsSize; ++j){
                        accessPoint.update(results.get(j));
                    }
                }

                accessPoint.setShowSummary(true);
                accessPoints.add(accessPoint);
            }
        }

        // Sort network list by priority (or by network id if the priority is the same)
        Collections.sort(accessPoints, new Comparator<AccessPoint>() {
            @Override
            public int compare(AccessPoint lhs, AccessPoint rhs) {
                WifiConfiguration lwc = lhs.getConfig();
                WifiConfiguration rwc = rhs.getConfig();

                // > priority -- > lower position
                if (lwc.priority < rwc.priority) return 1;
                if (lwc.priority > rwc.priority) return -1;
                // < network id -- > lower position
                if (lhs.networkId < rhs.networkId) return -1;
                if (lhs.networkId > rhs.networkId) return 1;
                return 0;
            }
        });

        return accessPoints;
    }

    private void showDialog(AccessPoint accessPoint, boolean edit) {
        if (mDialog != null) {
            removeDialog(WifiSettings.WIFI_DIALOG_ID);
            mDialog = null;
        }

        // Save the access point and edit mode
        mDlgAccessPoint = accessPoint;

        showDialog(WifiSettings.WIFI_DIALOG_ID);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case WifiSettings.WIFI_DIALOG_ID:
                if (mDlgAccessPoint == null) { // For re-launch from saved state
                    mDlgAccessPoint = new AccessPoint(getActivity(), mAccessPointSavedState);
                    // Reset the saved access point data
                    mAccessPointSavedState = null;
                }
                mSelectedAccessPoint = mDlgAccessPoint;
                mDialog = new WifiDialog(getActivity(), this, mDlgAccessPoint,
                        false /* not editting */, true /* hide the submit button */);
                return mDialog;

        }
        return super.onCreateDialog(dialogId);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // If the dialog is showing, save its state.
        if (mDialog != null && mDialog.isShowing()) {
            if (mDlgAccessPoint != null) {
                mAccessPointSavedState = new Bundle();
                mDlgAccessPoint.saveWifiState(mAccessPointSavedState);
                outState.putBundle(SAVE_DIALOG_ACCESS_POINT_STATE, mAccessPointSavedState);
            }
        }
        outState.putBoolean(PRIORITIES_ORDER_CHANGED_STATE, mPrioritiesOrderChanged);
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int button) {
        if (button == WifiDialog.BUTTON_FORGET && mSelectedAccessPoint != null) {
            mWifiManager.forget(mSelectedAccessPoint.networkId, null);
            getPreferenceScreen().removePreference(mSelectedAccessPoint);
            mSelectedAccessPoint = null;
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        if (preference instanceof AccessPoint) {
            showDialog((AccessPoint) preference, false);
            return true;
        } else{
            return super.onPreferenceTreeClick(screen, preference);
        }
    }

    private boolean isAutoConfigPriorities() {
        return Settings.Global.getInt(getActivity().getContentResolver(),
                Settings.Global.WIFI_AUTO_PRIORITIES_CONFIGURATION, 1) != 0;
    }

    /**
     * For search.
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();
                final Resources res = context.getResources();
                final String title = res.getString(R.string.wifi_saved_access_points_titlebar);

                // Add fragment title
                SearchIndexableRaw data = new SearchIndexableRaw(context);
                data.title = title;
                data.screenTitle = title;
                data.enabled = enabled;
                result.add(data);

                // Add available Wi-Fi access points
                WifiManager wifiManager =
                        (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                final List<AccessPoint> accessPoints =
                        constructSavedAccessPoints(context, wifiManager);

                final int accessPointsSize = accessPoints.size();
                for (int i = 0; i < accessPointsSize; ++i){
                    data = new SearchIndexableRaw(context);
                    data.title = accessPoints.get(i).getTitle().toString();
                    data.screenTitle = title;
                    data.enabled = enabled;
                    result.add(data);
                }

                return result;
            }
        };
}
