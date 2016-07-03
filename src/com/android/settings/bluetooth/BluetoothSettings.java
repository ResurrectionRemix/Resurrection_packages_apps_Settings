/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings.bluetooth;

import static android.os.UserManager.DISALLOW_CONFIG_BLUETOOTH;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.style.TextAppearanceSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;
import android.widget.Toolbar;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.LinkifyUtils;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.location.ScanningSettings;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.bluetooth.BluetoothDeviceFilter;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import cyanogenmod.providers.CMSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * BluetoothSettings is the Settings screen for Bluetooth configuration and
 * connection management.
 */
public final class BluetoothSettings extends DeviceListPreferenceFragment implements Indexable {
    private static final String TAG = "BluetoothSettings";

    private static final int MENU_ID_SCAN = Menu.FIRST;
    private static final int MENU_ID_RENAME_DEVICE = Menu.FIRST + 1;
    private static final int MENU_ID_SHOW_RECEIVED = Menu.FIRST + 2;
    private static final int MENU_ID_ACCEPT_ALL_FILES = Menu.FIRST + 3;

    /* Private intent to show the list of received files */
    private static final String BTOPP_ACTION_OPEN_RECEIVED_FILES =
            "android.btopp.intent.action.OPEN_RECEIVED_FILES";

    private static View mSettingsDialogView = null;

    private BluetoothEnabler mBluetoothEnabler;

    private PreferenceGroup mPairedDevicesCategory;
    private PreferenceGroup mAvailableDevicesCategory;
    private boolean mAvailableDevicesCategoryIsPresent;

    private boolean mInitialScanStarted;
    private boolean mInitiateDiscoverable;

    private TextView mEmptyView;
    private SwitchBar mSwitchBar;

    private final IntentFilter mIntentFilter;


    // accessed from inner class (not private to avoid thunks)
    Preference mMyDevicePreference;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            final int state =
                intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

            if (action.equals(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED)) {
                updateDeviceName(context);
            }

            if (state == BluetoothAdapter.STATE_ON) {
                mInitiateDiscoverable = true;
            }
        }

        private void updateDeviceName(Context context) {
            if (mLocalAdapter.isEnabled() && mMyDevicePreference != null) {
                mMyDevicePreference.setSummary(context.getResources().getString(
                            R.string.bluetooth_is_visible_message, mLocalAdapter.getName()));
            }
        }
    };

    public BluetoothSettings() {
        super(DISALLOW_CONFIG_BLUETOOTH);
        mIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.BLUETOOTH;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        /* Don't auto start scan if screen reconstructs due to frozen screen*/
        mInitialScanStarted = (savedInstanceState != null);
        mInitiateDiscoverable = true;

        mEmptyView = (TextView) getView().findViewById(android.R.id.empty);
        getListView().setEmptyView(mEmptyView);

        final SettingsActivity activity = (SettingsActivity) getActivity();
        mSwitchBar = activity.getSwitchBar();

        mBluetoothEnabler = new BluetoothEnabler(activity, mSwitchBar);
        mBluetoothEnabler.setupSwitchBar();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Activity activity = getActivity();
        float titleTextSize;
        int actionBarHeight;
        int switchBarHeight;
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            titleTextSize = activity.getResources().getDimensionPixelSize(
                    R.dimen.bluetooth_landscape_title_textsize);
            switchBarHeight = activity.getResources().getDimensionPixelSize(
                    R.dimen.bluetooth_landscape_switchbar_height);
            actionBarHeight = activity.getResources().getDimensionPixelSize(
                    R.dimen.bluetooth_landscape_actionbar_height);
        } else {
            titleTextSize = activity.getResources().getDimensionPixelSize(
                    R.dimen.bluetooth_portrait_title_textsize);
            switchBarHeight = activity.getResources().getDimensionPixelSize(
                    R.dimen.bluetooth_portrait_switchbar_height);
            actionBarHeight = activity.getResources().getDimensionPixelSize(
                    R.dimen.bluetooth_portrait_switchbar_height);
        }
        resetBarSize(titleTextSize, actionBarHeight, switchBarHeight);
    }

    private void resetBarSize(float titleTextSize, int actionBarHeight, int switchBarHeight) {
        Activity activity = getActivity();
        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        int titleId = Resources.getSystem().getIdentifier("action_bar", "id", "android");
        Toolbar toolbar = (Toolbar) activity.getWindow().findViewById(titleId);
        TextView title = null;
        if (toolbar != null) {
            LayoutParams layoutParams = toolbar.getLayoutParams();
            layoutParams.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    actionBarHeight, displayMetrics);
            for (int i = 0; i < toolbar.getChildCount(); ++i) {
                if (toolbar.getChildAt(i) instanceof TextView) {
                    title = (TextView) toolbar.getChildAt(i);
                }
                Toolbar.LayoutParams childLayoutParams = (Toolbar.LayoutParams) toolbar.getChildAt(
                        i).getLayoutParams();
                childLayoutParams.gravity = Gravity.CENTER_VERTICAL;
            }
        }
        if (title != null)
            title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, titleTextSize);
        if (mSwitchBar != null) {
            LayoutParams layoutParams = mSwitchBar.getLayoutParams();
            layoutParams.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    switchBarHeight, displayMetrics);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mBluetoothEnabler.teardownSwitchBar();
    }

    @Override
    void addPreferencesForActivity() {
        addPreferencesFromResource(R.xml.bluetooth_settings);

        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        // resume BluetoothEnabler before calling super.onResume() so we don't get
        // any onDeviceAdded() callbacks before setting up view in updateContent()
        if (mBluetoothEnabler != null) {
            mBluetoothEnabler.resume(getActivity());
        }
        if (mLocalAdapter != null) {
            // enable page and inquiry scan
            mLocalAdapter.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE);
        }
        super.onResume();

        mInitiateDiscoverable = true;

        if (isUiRestricted()) {
            setDeviceListGroup(getPreferenceScreen());
            removeAllDevices();
            mEmptyView.setText(R.string.bluetooth_empty_list_user_restricted);
            return;
        }

        getActivity().registerReceiver(mReceiver, mIntentFilter);
        if (mLocalAdapter != null) {
            updateContent(mLocalAdapter.getBluetoothState());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mBluetoothEnabler != null) {
            mBluetoothEnabler.pause();
        }

        // Make the device only visible to connected devices.
        if (mLocalAdapter != null) {
            mLocalAdapter.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE);
        }

        if (isUiRestricted()) {
            return;
        }

        getActivity().unregisterReceiver(mReceiver);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mLocalAdapter == null) return;
        // If the user is not allowed to configure bluetooth, do not show the menu.
        if (isUiRestricted()) return;

        boolean bluetoothIsEnabled = mLocalAdapter.getBluetoothState() == BluetoothAdapter.STATE_ON;
        boolean isDiscovering = mLocalAdapter.isDiscovering();
        int textId = isDiscovering ? R.string.bluetooth_searching_for_devices :
            R.string.bluetooth_search_for_devices;

        boolean isAcceptAllFilesEnabled = CMSettings.System.getInt(getContentResolver(),
                CMSettings.System.BLUETOOTH_ACCEPT_ALL_FILES, 0) == 1;

        menu.add(Menu.NONE, MENU_ID_SCAN, 0, textId)
                .setEnabled(bluetoothIsEnabled && !isDiscovering)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, MENU_ID_RENAME_DEVICE, 0, R.string.bluetooth_rename_device)
                .setEnabled(bluetoothIsEnabled)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, MENU_ID_SHOW_RECEIVED, 0, R.string.bluetooth_show_received_files)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, MENU_ID_ACCEPT_ALL_FILES, 0, R.string.bluetooth_accept_all_files)
                .setCheckable(true)
                .setChecked(isAcceptAllFilesEnabled)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ID_SCAN:
                if (mLocalAdapter.getBluetoothState() == BluetoothAdapter.STATE_ON) {
                    MetricsLogger.action(getActivity(), MetricsLogger.ACTION_BLUETOOTH_SCAN);
                    startScanning();
                }
                return true;

            case MENU_ID_RENAME_DEVICE:
                MetricsLogger.action(getActivity(), MetricsLogger.ACTION_BLUETOOTH_RENAME);
                new BluetoothNameDialogFragment().show(
                        getFragmentManager(), "rename device");
                return true;

            case MENU_ID_SHOW_RECEIVED:
                MetricsLogger.action(getActivity(), MetricsLogger.ACTION_BLUETOOTH_FILES);
                Intent intent = new Intent(BTOPP_ACTION_OPEN_RECEIVED_FILES);
                getActivity().sendBroadcast(intent);
                return true;

            case MENU_ID_ACCEPT_ALL_FILES:
                item.setChecked(!item.isChecked());
                CMSettings.System.putInt(getContentResolver(),
                        CMSettings.System.BLUETOOTH_ACCEPT_ALL_FILES,
                        item.isChecked() ? 1 : 0);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startScanning() {
        if (isUiRestricted()) {
            return;
        }

        if (!mAvailableDevicesCategoryIsPresent) {
            getPreferenceScreen().addPreference(mAvailableDevicesCategory);
            mAvailableDevicesCategoryIsPresent = true;
        }

        if (mAvailableDevicesCategory != null) {
            setDeviceListGroup(mAvailableDevicesCategory);
            removeAllDevices();
        }

        mLocalManager.getCachedDeviceManager().clearNonBondedDevices();
        if (mAvailableDevicesCategory != null) {
            mAvailableDevicesCategory.removeAll();
        } else {
              Log.e(TAG, "mAvailableDevicesCategory is null.");
        }
        mInitialScanStarted = true;
        mLocalAdapter.startScanning(true);
    }

    @Override
    void onDevicePreferenceClick(BluetoothDevicePreference btPreference) {
        mLocalAdapter.stopScanning();
        super.onDevicePreferenceClick(btPreference);
    }

    private void addDeviceCategory(PreferenceGroup preferenceGroup, int titleId,
            BluetoothDeviceFilter.Filter filter, boolean addCachedDevices) {
        preferenceGroup.setTitle(titleId);
        getPreferenceScreen().addPreference(preferenceGroup);
        setFilter(filter);
        setDeviceListGroup(preferenceGroup);
        if (addCachedDevices) {
            addCachedDevices();
        }
        preferenceGroup.setEnabled(true);
    }

    private void updateContent(int bluetoothState) {
        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        int messageId = 0;

        switch (bluetoothState) {
            case BluetoothAdapter.STATE_ON:
                preferenceScreen.removeAll();
                preferenceScreen.setOrderingAsAdded(true);
                mDevicePreferenceMap.clear();

                if (isUiRestricted()) {
                    messageId = R.string.bluetooth_empty_list_user_restricted;
                    break;
                }

                // Paired devices category
                if (mPairedDevicesCategory == null) {
                    mPairedDevicesCategory = new PreferenceCategory(getActivity());
                } else {
                    mPairedDevicesCategory.removeAll();
                }
                addDeviceCategory(mPairedDevicesCategory,
                        R.string.bluetooth_preference_paired_devices,
                        BluetoothDeviceFilter.BONDED_DEVICE_FILTER, true);
                int numberOfPairedDevices = mPairedDevicesCategory.getPreferenceCount();

                if (isUiRestricted() || numberOfPairedDevices <= 0) {
                    preferenceScreen.removePreference(mPairedDevicesCategory);
                }

                // Available devices category
                if (mAvailableDevicesCategory == null) {
                    mAvailableDevicesCategory = new BluetoothProgressCategory(getActivity());
                    mAvailableDevicesCategory.setSelectable(false);
                } else {
                    mAvailableDevicesCategory.removeAll();
                }
                addDeviceCategory(mAvailableDevicesCategory,
                        R.string.bluetooth_preference_found_devices,
                        BluetoothDeviceFilter.UNBONDED_DEVICE_FILTER, mInitialScanStarted);
                int numberOfAvailableDevices = mAvailableDevicesCategory.getPreferenceCount();

                if (!mInitialScanStarted) {
                    startScanning();
                }

                if (mMyDevicePreference == null) {
                    mMyDevicePreference = new Preference(getActivity());
                }

                mMyDevicePreference.setSummary(getResources().getString(
                            R.string.bluetooth_is_visible_message, mLocalAdapter.getName()));
                mMyDevicePreference.setSelectable(false);
                preferenceScreen.addPreference(mMyDevicePreference);

                getActivity().invalidateOptionsMenu();

                // mLocalAdapter.setScanMode is internally synchronized so it is okay for multiple
                // threads to execute.
                if (mInitiateDiscoverable) {
                    // Make the device visible to other devices.
                    mLocalAdapter.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE);
                    mInitiateDiscoverable = false;
                }
                return; // not break

            case BluetoothAdapter.STATE_TURNING_OFF:
                messageId = R.string.bluetooth_turning_off;
                break;

            case BluetoothAdapter.STATE_OFF:
                setOffMessage();
                /* reset the progress icon only when available device category present */
                if(mAvailableDevicesCategoryIsPresent) {
                    ((BluetoothProgressCategory)mAvailableDevicesCategory).setProgress(false);
                }
                if (isUiRestricted()) {
                    messageId = R.string.bluetooth_empty_list_user_restricted;
                }
                break;

            case BluetoothAdapter.STATE_TURNING_ON:
                messageId = R.string.bluetooth_turning_on;
                mInitialScanStarted = false;
                break;
        }

        setDeviceListGroup(preferenceScreen);
        removeAllDevices();
        if (messageId != 0) {
            mEmptyView.setText(messageId);
        }
        if (!isUiRestricted()) {
            getActivity().invalidateOptionsMenu();
        }
    }

    private void setOffMessage() {
        if (mEmptyView == null) {
            return;
        }
        final CharSequence briefText = getText(R.string.bluetooth_empty_list_bluetooth_off);

        final ContentResolver resolver = getActivity().getContentResolver();
        final boolean bleScanningMode = Settings.Global.getInt(
                resolver, Settings.Global.BLE_SCAN_ALWAYS_AVAILABLE, 0) == 1;

        if (!bleScanningMode) {
            // Show only the brief text if the scanning mode has been turned off.
            mEmptyView.setText(briefText, TextView.BufferType.SPANNABLE);
        } else {
            final StringBuilder contentBuilder = new StringBuilder();
            contentBuilder.append(briefText);
            contentBuilder.append("\n\n");
            contentBuilder.append(getText(R.string.ble_scan_notify_text));
            LinkifyUtils.linkify(mEmptyView, contentBuilder, new LinkifyUtils.OnClickListener() {
                @Override
                public void onClick() {
                    final SettingsActivity activity =
                            (SettingsActivity) BluetoothSettings.this.getActivity();
                    activity.startPreferencePanel(ScanningSettings.class.getName(), null,
                            R.string.location_scanning_screen_title, null, null, 0);
                }
            });
        }
        getPreferenceScreen().removeAll();
    }

    @Override
    public void onBluetoothStateChanged(int bluetoothState) {
        super.onBluetoothStateChanged(bluetoothState);
        // If BT is turned off/on staying in the same BT Settings screen
        // discoverability to be set again
        if (BluetoothAdapter.STATE_ON == bluetoothState)
            mInitiateDiscoverable = true;
        updateContent(bluetoothState);
    }

    @Override
    public void onScanningStateChanged(boolean started) {
        super.onScanningStateChanged(started);
        // Update options' enabled state
        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }
    }

    @Override
    public void onDeviceBondStateChanged(CachedBluetoothDevice cachedDevice, int bondState) {
        setDeviceListGroup(getPreferenceScreen());
        removeAllDevices();
        updateContent(mLocalAdapter.getBluetoothState());
    }

    private final View.OnClickListener mDeviceProfilesListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // User clicked on advanced options icon for a device in the list
            if (!(v.getTag() instanceof CachedBluetoothDevice)) {
                Log.w(TAG, "onClick() called for other View: " + v);
                return;
            }

            final CachedBluetoothDevice device = (CachedBluetoothDevice) v.getTag();
            Bundle args = new Bundle();
            args.putString(DeviceProfilesSettings.ARG_DEVICE_ADDRESS,
                    device.getDevice().getAddress());
            DeviceProfilesSettings profileSettings = new DeviceProfilesSettings();
            profileSettings.setArguments(args);
            profileSettings.show(getFragmentManager(),
                    DeviceProfilesSettings.class.getSimpleName());
        }
    };

    /**
     * Add a listener, which enables the advanced settings icon.
     * @param preference the newly added preference
     */
    @Override
    void initDevicePreference(BluetoothDevicePreference preference) {
        CachedBluetoothDevice cachedDevice = preference.getCachedDevice();
        if (cachedDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
            // Only paired device have an associated advanced settings screen
            preference.setOnSettingsClickListener(mDeviceProfilesListener);
        }
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_bluetooth;
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {

                final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();

                final Resources res = context.getResources();

                // Add fragment title
                SearchIndexableRaw data = new SearchIndexableRaw(context);
                data.title = res.getString(R.string.bluetooth_settings);
                data.screenTitle = res.getString(R.string.bluetooth_settings);
                result.add(data);

                // Add cached paired BT devices
                LocalBluetoothManager lbtm = Utils.getLocalBtManager(context);
                // LocalBluetoothManager.getInstance can return null if the device does not
                // support bluetooth (e.g. the emulator).
                if (lbtm != null) {
                    Set<BluetoothDevice> bondedDevices =
                            lbtm.getBluetoothAdapter().getBondedDevices();

                    for (BluetoothDevice device : bondedDevices) {
                        data = new SearchIndexableRaw(context);
                        data.title = device.getName();
                        data.screenTitle = res.getString(R.string.bluetooth_settings);
                        data.enabled = enabled;
                        result.add(data);
                    }
                }
                return result;
            }
        };
}
