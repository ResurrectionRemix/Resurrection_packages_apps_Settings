/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothPan;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.UserManager;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.datausage.DataSaverBackend;
import com.android.settings.wifi.WifiApDialog;
import com.android.settings.wifi.WifiApEnabler;
import com.android.settingslib.TetherUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static android.net.ConnectivityManager.TETHERING_BLUETOOTH;
import static android.net.ConnectivityManager.TETHERING_USB;
import static android.net.ConnectivityManager.TETHERING_WIFI;

/*
 * Displays preferences for Tethering.
 */
public class TetherSettings extends RestrictedSettingsFragment
        implements DialogInterface.OnClickListener, Preference.OnPreferenceChangeListener,
        DataSaverBackend.Listener {

    private static final String USB_TETHER_SETTINGS = "usb_tether_settings";
    private static final String ENABLE_WIFI_AP = "enable_wifi_ap";
    private static final String ENABLE_WIFI_AP_EXT = "enable_wifi_ap_ext";
    private static final String ENABLE_BLUETOOTH_TETHERING = "enable_bluetooth_tethering";
    private static final String TETHER_CHOICE = "TETHER_TYPE";
    private static final String HOTSPOT_TIMEOUT = "hotstpot_inactivity_timeout";
    private static final String TETHERING_HELP = "tethering_help";
    private static final String DATA_SAVER_FOOTER = "disabled_on_data_saver";
    private static final String ACTION_EXTRA = "choice";
    private static final String ACTION_EXTRA_VALUE = "value";
    private static final String SHAREPREFERENCE_DEFAULT_WIFI = "def_wifiap_set";
    private static final String SHAREPREFERENCE_FIFE_NAME = "MY_PERFS";
    private static final String ACTION_HOTSPOT_CONFIGURE = "Hotspot_PreConfigure";
    private static final String ACTION_HOTSPOT_POST_CONFIGURE = "Hotspot_PostConfigure";
    private static final String CONFIGURE_RESULT = "PreConfigure_result";
    private static final String ACTION_HOTSPOT_CONFIGURE_RRSPONSE =
            "Hotspot_PreConfigure_Response";


    private static final int DIALOG_AP_SETTINGS = 1;

    private static final String TAG = "TetheringSettings";

    private SwitchPreference mUsbTether;

    private WifiApEnabler mWifiApEnabler;
    private Preference mEnableWifiAp;
    private PreferenceScreen mTetherHelp;

    private SwitchPreference mBluetoothTether;

    private ListPreference mHotspotInactivityTimeout;

    private BroadcastReceiver mTetherChangeReceiver;

    private String[] mUsbRegexs;

    private String[] mWifiRegexs;

    private String[] mBluetoothRegexs;
    private AtomicReference<BluetoothPan> mBluetoothPan = new AtomicReference<BluetoothPan>();

    private Handler mHandler = new Handler();
    private OnStartTetheringCallback mStartTetheringCallback;

    private static final String WIFI_AP_SSID_AND_SECURITY = "wifi_ap_ssid_and_security";
    private static final int CONFIG_SUBTEXT = R.string.wifi_tether_configure_subtext;

    private String[] mSecurityType;
    private Preference mCreateNetwork;

    private WifiApDialog mDialog;
    private WifiManager mWifiManager;
    private WifiConfiguration mWifiConfig = null;
    private ConnectivityManager mCm;

    private boolean mRestartWifiApAfterConfigChange;

    private boolean mUsbConnected;
    private boolean mMassStorageActive;

    private boolean mBluetoothEnableForTether;

    /* One of INVALID, WIFI_TETHERING, USB_TETHERING or BLUETOOTH_TETHERING */
    private int mTetherChoice = -1;

    /* Stores the package name and the class name of the provisioning app */
    private String[] mProvisionApp;
    private static final int PROVISION_REQUEST = 0;

    private boolean mUnavailable;
    private BroadcastReceiver mConfigureReceiver;
    private DataSaverBackend mDataSaverBackend;
    private boolean mDataSaverEnabled;
    private Preference mDataSaverFooter;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.TETHER;
    }

    public TetherSettings() {
        super(UserManager.DISALLOW_CONFIG_TETHERING);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if(icicle != null) {
            mTetherChoice = icicle.getInt(TETHER_CHOICE);
        }
        addPreferencesFromResource(R.xml.tether_prefs);

        mDataSaverBackend = new DataSaverBackend(getContext());
        mDataSaverEnabled = mDataSaverBackend.isDataSaverEnabled();
        mDataSaverFooter = findPreference(DATA_SAVER_FOOTER);

        setIfOnlyAvailableForAdmins(true);
        if (isUiRestricted()) {
            mUnavailable = true;
            setPreferenceScreen(new PreferenceScreen(getPrefContext(), null));
            return;
        }

        final Activity activity = getActivity();
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            adapter.getProfileProxy(activity.getApplicationContext(), mProfileServiceListener,
                    BluetoothProfile.PAN);
        }

        mCreateNetwork = findPreference(WIFI_AP_SSID_AND_SECURITY);

        boolean enableWifiApSettingsExt = getResources().getBoolean(R.bool.show_wifi_hotspot_settings);
        boolean isWifiApEnabled = getResources().getBoolean(R.bool.hide_wifi_hotspot);
        checkDefaultValue(getActivity());
        if (enableWifiApSettingsExt) {
            mEnableWifiAp =
                    (HotspotPreference) findPreference(ENABLE_WIFI_AP_EXT);
            getPreferenceScreen().removePreference(findPreference(ENABLE_WIFI_AP));
            getPreferenceScreen().removePreference(mCreateNetwork);
            Intent intent = new Intent();
            intent.setAction("com.qti.ap.settings");
            intent.setPackage("com.qualcomm.qti.extsettings");
            mEnableWifiAp.setIntent(intent);
        } else {
            mEnableWifiAp =
                    (SwitchPreference) findPreference(ENABLE_WIFI_AP);
            getPreferenceScreen().removePreference(findPreference(ENABLE_WIFI_AP_EXT));
        }
        if (isWifiApEnabled) {
            getPreferenceScreen().removePreference(mEnableWifiAp);
            getPreferenceScreen().removePreference(mCreateNetwork);
        }

        if (getResources().getBoolean(
                R.bool.config_regional_hotspot_tether_help_enable)) {
            mTetherHelp = (PreferenceScreen) findPreference(TETHERING_HELP);
        } else {
            getPreferenceScreen().removePreference(findPreference(TETHERING_HELP));
        }

        mUsbTether = (SwitchPreference) findPreference(USB_TETHER_SETTINGS);
        mBluetoothTether = (SwitchPreference) findPreference(ENABLE_BLUETOOTH_TETHERING);
        mHotspotInactivityTimeout = (ListPreference) findPreference(HOTSPOT_TIMEOUT);

        mDataSaverBackend.addListener(this);

        mCm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        mUsbRegexs = mCm.getTetherableUsbRegexs();
        mWifiRegexs = mCm.getTetherableWifiRegexs();
        mBluetoothRegexs = mCm.getTetherableBluetoothRegexs();

        final boolean usbAvailable = mUsbRegexs.length != 0;
        final boolean wifiAvailable = mWifiRegexs.length != 0;
        final boolean bluetoothAvailable = mBluetoothRegexs.length != 0;

        if (!usbAvailable || Utils.isMonkeyRunning()) {
            getPreferenceScreen().removePreference(mUsbTether);
        }

        if (wifiAvailable && !Utils.isMonkeyRunning()) {
            mWifiApEnabler = new WifiApEnabler(activity, mDataSaverBackend, mEnableWifiAp);
            initWifiTethering();
        } else {
            getPreferenceScreen().removePreference(mEnableWifiAp);
            getPreferenceScreen().removePreference(mCreateNetwork);
        }

        if (!bluetoothAvailable) {
            getPreferenceScreen().removePreference(mBluetoothTether);
        } else {
            BluetoothPan pan = mBluetoothPan.get();
            if (pan != null && pan.isTetheringOn()) {
                mBluetoothTether.setChecked(true);
            } else {
                mBluetoothTether.setChecked(false);
            }
        }
        mHotspotInactivityTimeout.setOnPreferenceChangeListener(this);
        // Set initial state based on Data Saver mode.
        onDataSaverChanged(mDataSaverBackend.isDataSaverEnabled());
    }

    @Override
    public void onDestroy() {
        mDataSaverBackend.remListener(this);
        super.onDestroy();
    }

    @Override
    public void onDataSaverChanged(boolean isDataSaving) {
        mDataSaverEnabled = isDataSaving;
        mEnableWifiAp.setEnabled(!mDataSaverEnabled);
        mUsbTether.setEnabled(!mDataSaverEnabled);
        mBluetoothTether.setEnabled(!mDataSaverEnabled);
        mDataSaverFooter.setVisible(mDataSaverEnabled);
    }

    @Override
    public void onWhitelistStatusChanged(int uid, boolean isWhitelisted) {
    }

    @Override
    public void onBlacklistStatusChanged(int uid, boolean isBlacklisted)  {
    }

    private void initWifiTethering() {
        final Activity activity = getActivity();
        mWifiConfig = mWifiManager.getWifiApConfiguration();
        mSecurityType = getResources().getStringArray(R.array.wifi_ap_security);
        mRestartWifiApAfterConfigChange = false;

        if (mCreateNetwork == null) {
            return;
        }

        if (mWifiConfig == null) {
            final String s = activity.getString(
                    com.android.internal.R.string.wifi_tether_configure_ssid_default);
            mCreateNetwork.setSummary(String.format(activity.getString(CONFIG_SUBTEXT),
                    s, mSecurityType[WifiApDialog.OPEN_INDEX]));
        } else {
            int index = WifiApDialog.getSecurityTypeIndex(mWifiConfig);
            mCreateNetwork.setSummary(String.format(activity.getString(CONFIG_SUBTEXT),
                    mWifiConfig.SSID,
                    mSecurityType[index]));
        }
        updateHotspotTimeoutSummary(mWifiConfig);
    }

    private void updateHotspotTimeoutSummary(WifiConfiguration wifiConfig) {
        if (wifiConfig == null) {
            mHotspotInactivityTimeout.setValue("0");
            mHotspotInactivityTimeout.setSummary(
                    getString(R.string.hotstpot_inactivity_timeout_never_summary_text));
        } else {
            mHotspotInactivityTimeout.setValue(Long.toString(wifiConfig.wifiApInactivityTimeout));
            if (wifiConfig.wifiApInactivityTimeout > 0) {
                mHotspotInactivityTimeout.setSummary(String.format(
                        getString(R.string.hotstpot_inactivity_timeout_summary_text,
                                mHotspotInactivityTimeout.getEntry())));
            } else {
                mHotspotInactivityTimeout.setSummary(
                        getString(R.string.hotstpot_inactivity_timeout_never_summary_text));
            }
        }
    }

    @Override
    public Dialog onCreateDialog(int id) {
        if (id == DIALOG_AP_SETTINGS) {
            final Activity activity = getActivity();
            mDialog = new WifiApDialog(activity, this, mWifiConfig);
            return mDialog;
        }

        return null;
    }

    private class TetherChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context content, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ConnectivityManager.ACTION_TETHER_STATE_CHANGED)) {
                // TODO - this should understand the interface types
                ArrayList<String> available = intent.getStringArrayListExtra(
                        ConnectivityManager.EXTRA_AVAILABLE_TETHER);
                ArrayList<String> active = intent.getStringArrayListExtra(
                        ConnectivityManager.EXTRA_ACTIVE_TETHER);
                ArrayList<String> errored = intent.getStringArrayListExtra(
                        ConnectivityManager.EXTRA_ERRORED_TETHER);
                updateState(available.toArray(new String[available.size()]),
                        active.toArray(new String[active.size()]),
                        errored.toArray(new String[errored.size()]));
                if (mWifiManager.getWifiApState() == WifiManager.WIFI_AP_STATE_DISABLED
                        && mRestartWifiApAfterConfigChange) {
                    mRestartWifiApAfterConfigChange = false;
                    Log.d(TAG, "Restarting WifiAp due to prior config change.");
                    startTethering(TETHERING_WIFI);
                }
            } else if (action.equals(WifiManager.WIFI_AP_STATE_CHANGED_ACTION)) {
                int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_AP_STATE, 0);
                if (state == WifiManager.WIFI_AP_STATE_DISABLED
                        && mRestartWifiApAfterConfigChange) {
                    mRestartWifiApAfterConfigChange = false;
                    Log.d(TAG, "Restarting WifiAp due to prior config change.");
                    startTethering(TETHERING_WIFI);
                }
            } else if (action.equals(Intent.ACTION_MEDIA_SHARED)) {
                mMassStorageActive = true;
                updateState();
            } else if (action.equals(Intent.ACTION_MEDIA_UNSHARED)) {
                mMassStorageActive = false;
                updateState();
            } else if (action.equals(UsbManager.ACTION_USB_STATE)) {
                mUsbConnected = intent.getBooleanExtra(UsbManager.USB_CONNECTED, false);
                updateState();
            } else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                if (mBluetoothEnableForTether) {
                    switch (intent
                            .getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                        case BluetoothAdapter.STATE_ON:
                            startTethering(TETHERING_BLUETOOTH);
                            mBluetoothEnableForTether = false;
                            break;

                        case BluetoothAdapter.STATE_OFF:
                        case BluetoothAdapter.ERROR:
                            mBluetoothEnableForTether = false;
                            break;

                        default:
                            // ignore transition states
                    }
                }
                updateState();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mUnavailable) {
            if (!isUiRestrictedByOnlyAdmin()) {
                getEmptyTextView().setText(R.string.tethering_settings_not_available);
            }
            getPreferenceScreen().removeAll();
            return;
        }

        final Activity activity = getActivity();

        mStartTetheringCallback = new OnStartTetheringCallback(this);

        mMassStorageActive = Environment.MEDIA_SHARED.equals(Environment.getExternalStorageState());
        mTetherChangeReceiver = new TetherChangeReceiver();
        IntentFilter filter = new IntentFilter(ConnectivityManager.ACTION_TETHER_STATE_CHANGED);
        filter.addAction(WifiManager.WIFI_AP_STATE_CHANGED_ACTION);
        Intent intent = activity.registerReceiver(mTetherChangeReceiver, filter);

        filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_STATE);
        activity.registerReceiver(mTetherChangeReceiver, filter);

        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_SHARED);
        filter.addAction(Intent.ACTION_MEDIA_UNSHARED);
        filter.addDataScheme("file");
        activity.registerReceiver(mTetherChangeReceiver, filter);

        filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        activity.registerReceiver(mTetherChangeReceiver, filter);

        if (intent != null) mTetherChangeReceiver.onReceive(activity, intent);
        if (mWifiApEnabler != null) {
            mEnableWifiAp.setOnPreferenceChangeListener(this);
            mWifiApEnabler.resume();
        }

        updateState();
        registerConfigureReceiver(getActivity());
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mUnavailable) {
            return;
        }
        getActivity().unregisterReceiver(mTetherChangeReceiver);
        mTetherChangeReceiver = null;
        mStartTetheringCallback = null;
        if (mWifiApEnabler != null) {
            mEnableWifiAp.setOnPreferenceChangeListener(null);
            mWifiApEnabler.pause();
        }
        unRegisterConfigureReceiver();
    }

    private void updateState() {
        String[] available = mCm.getTetherableIfaces();
        String[] tethered = mCm.getTetheredIfaces();
        String[] errored = mCm.getTetheringErroredIfaces();
        updateState(available, tethered, errored);
    }

    private void updateState(String[] available, String[] tethered,
            String[] errored) {
        updateUsbState(available, tethered, errored);
        updateBluetoothState(available, tethered, errored);
    }


    private void updateUsbState(String[] available, String[] tethered,
            String[] errored) {
        boolean usbAvailable = mUsbConnected && !mMassStorageActive;
        int usbError = ConnectivityManager.TETHER_ERROR_NO_ERROR;
        for (String s : available) {
            for (String regex : mUsbRegexs) {
                if (s.matches(regex)) {
                    if (usbError == ConnectivityManager.TETHER_ERROR_NO_ERROR) {
                        usbError = mCm.getLastTetherError(s);
                    }
                }
            }
        }
        boolean usbTethered = false;
        for (String s : tethered) {
            for (String regex : mUsbRegexs) {
                if (s.matches(regex)) usbTethered = true;
            }
        }
        boolean usbErrored = false;
        for (String s: errored) {
            for (String regex : mUsbRegexs) {
                if (s.matches(regex)) usbErrored = true;
            }
        }

        if (usbTethered) {
            mUsbTether.setSummary(R.string.usb_tethering_active_subtext);
            mUsbTether.setEnabled(!mDataSaverEnabled);
            mUsbTether.setChecked(true);
        } else if (usbAvailable) {
            if (usbError == ConnectivityManager.TETHER_ERROR_NO_ERROR) {
                mUsbTether.setSummary(R.string.usb_tethering_available_subtext);
            } else {
                mUsbTether.setSummary(R.string.usb_tethering_errored_subtext);
            }
            mUsbTether.setEnabled(!mDataSaverEnabled);
            mUsbTether.setChecked(false);
        } else if (usbErrored) {
            mUsbTether.setSummary(R.string.usb_tethering_errored_subtext);
            mUsbTether.setEnabled(false);
            mUsbTether.setChecked(false);
        } else if (mMassStorageActive) {
            mUsbTether.setSummary(R.string.usb_tethering_storage_active_subtext);
            mUsbTether.setEnabled(false);
            mUsbTether.setChecked(false);
        } else {
            mUsbTether.setSummary(R.string.usb_tethering_unavailable_subtext);
            mUsbTether.setEnabled(false);
            mUsbTether.setChecked(false);
        }
    }

    private void updateBluetoothState(String[] available, String[] tethered,
            String[] errored) {
        boolean bluetoothErrored = false;
        for (String s: errored) {
            for (String regex : mBluetoothRegexs) {
                if (s.matches(regex)) bluetoothErrored = true;
            }
        }

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            return;
        }
        int btState = adapter.getState();
        if (btState == BluetoothAdapter.STATE_TURNING_OFF) {
            mBluetoothTether.setEnabled(false);
            mBluetoothTether.setSummary(R.string.bluetooth_turning_off);
        } else if (btState == BluetoothAdapter.STATE_TURNING_ON) {
            mBluetoothTether.setEnabled(false);
            mBluetoothTether.setSummary(R.string.bluetooth_turning_on);
        } else {
            BluetoothPan bluetoothPan = mBluetoothPan.get();
            if (btState == BluetoothAdapter.STATE_ON && bluetoothPan != null
                    && bluetoothPan.isTetheringOn()) {
                mBluetoothTether.setChecked(true);
                mBluetoothTether.setEnabled(!mDataSaverEnabled);
                int bluetoothTethered = bluetoothPan.getConnectedDevices().size();
                if (bluetoothTethered > 1) {
                    String summary = getString(
                            R.string.bluetooth_tethering_devices_connected_subtext,
                            bluetoothTethered);
                    mBluetoothTether.setSummary(summary);
                } else if (bluetoothTethered == 1) {
                    mBluetoothTether.setSummary(
                            R.string.bluetooth_tethering_device_connected_subtext);
                } else if (bluetoothErrored) {
                    mBluetoothTether.setSummary(R.string.bluetooth_tethering_errored_subtext);
                } else {
                    mBluetoothTether.setSummary(R.string.bluetooth_tethering_available_subtext);
                }
            } else {
                mBluetoothTether.setEnabled(!mDataSaverEnabled);
                mBluetoothTether.setChecked(false);
                mBluetoothTether.setSummary(R.string.bluetooth_tethering_off_subtext);
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        if (preference == mEnableWifiAp) {
            boolean enable = (Boolean) value;

            if (enable) {
                startTethering(TETHERING_WIFI);
            } else {
                mCm.stopTethering(TETHERING_WIFI);
            }
            return false;
        } else if (preference == mHotspotInactivityTimeout) {
            if (mWifiConfig != null) {
                mWifiConfig.wifiApInactivityTimeout = Long.parseLong((String) value);
                updateHotspotTimeoutSummary(mWifiConfig);
                if (mWifiManager.getWifiApState() == WifiManager.WIFI_AP_STATE_ENABLED) {
                    mWifiManager.setWifiApEnabled(null, false);
                    mWifiManager.setWifiApEnabled(mWifiConfig, true);
                } else {
                    mWifiManager.setWifiApConfiguration(mWifiConfig);
                }
                return true;
            }
        }
        return false;
    }

    public static boolean isProvisioningNeededButUnavailable(Context context) {
        return (TetherUtil.isProvisioningNeeded(context)
                && !isIntentAvailable(context));
    }

    private static boolean isIntentAvailable(Context context) {
        String[] provisionApp = context.getResources().getStringArray(
                com.android.internal.R.array.config_mobile_hotspot_provision_app);
        if (provisionApp.length < 2) {
            return false;
        }
        final PackageManager packageManager = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName(provisionApp[0], provisionApp[1]);

        return (packageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY).size() > 0);
    }

    private void startTethering(int choice) {
        if (choice == TETHERING_BLUETOOTH) {
            // Turn on Bluetooth first.
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter.getState() == BluetoothAdapter.STATE_OFF) {
                mBluetoothEnableForTether = true;
                adapter.enable();
                mBluetoothTether.setSummary(R.string.bluetooth_turning_on);
                mBluetoothTether.setEnabled(false);
                return;
            }
        }

        mCm.startTethering(choice, true, mStartTetheringCallback, mHandler);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mUsbTether) {
            if (mUsbTether.isChecked()) {
                startTethering(TETHERING_USB);
            } else {
                mCm.stopTethering(TETHERING_USB);
            }
        } else if (preference == mBluetoothTether) {
            if (mBluetoothTether.isChecked()) {
                startTethering(TETHERING_BLUETOOTH);
            } else {
                mCm.stopTethering(TETHERING_BLUETOOTH);
                // No ACTION_TETHER_STATE_CHANGED is fired or bluetooth unless a device is
                // connected. Need to update state manually.
                updateState();
            }
        } else if (preference == mCreateNetwork) {
            showDialog(DIALOG_AP_SETTINGS);
        } else if (getResources().getBoolean(
                R.bool.config_regional_hotspot_tether_help_enable)
                && preference == mTetherHelp) {
            AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
            alert.setTitle(R.string.tethering_help_dialog_title);
            alert.setMessage(R.string.tethering_help_dialog_text);
            alert.setPositiveButton(R.string.okay, null);
            alert.show();
        }

        return super.onPreferenceTreeClick(preference);
    }

    public void onClick(DialogInterface dialogInterface, int button) {
        if (button == DialogInterface.BUTTON_POSITIVE) {
            mWifiConfig = mDialog.getConfig();
            if (mWifiConfig != null) {
                /**
                 * if soft AP is stopped, bring up
                 * else restart with new config
                 * TODO: update config on a running access point when framework support is added
                 */
                if (mWifiManager.getWifiApState() == WifiManager.WIFI_AP_STATE_ENABLED) {
                    Log.d("TetheringSettings",
                            "Wifi AP config changed while enabled, stop and restart");
                    mRestartWifiApAfterConfigChange = true;
                    mCm.stopTethering(TETHERING_WIFI);
                }
                mWifiManager.setWifiApConfiguration(mWifiConfig);
                int index = WifiApDialog.getSecurityTypeIndex(mWifiConfig);
                mCreateNetwork.setSummary(String.format(getActivity().getString(CONFIG_SUBTEXT),
                        mWifiConfig.SSID,
                        mSecurityType[index]));
            }
        }
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_tether;
    }

    private void checkDefaultValue(Context ctx) {
        boolean def_ssid = ctx.getResources().getBoolean(
                R.bool.hotspot_default_ssid_with_imei_enable);
        boolean clear_pwd = ctx.getResources().getBoolean( R.bool.use_empty_password_default);
        if (def_ssid || clear_pwd) {
            SharedPreferences sharedPreferences = ctx.getSharedPreferences(
                    SHAREPREFERENCE_FIFE_NAME,Activity.MODE_PRIVATE);
            boolean hasSetDefaultValue = sharedPreferences.getBoolean(
                    SHAREPREFERENCE_DEFAULT_WIFI, false);
            if ((!hasSetDefaultValue) && (setDefaultValue(ctx , def_ssid , clear_pwd))) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(SHAREPREFERENCE_DEFAULT_WIFI,true);
                editor.commit();
            }
        }
    }

    private boolean setDefaultValue(Context ctx, boolean default_ssid, boolean clear_password) {
        WifiManager wifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            return false;
        }
        WifiConfiguration wifiAPConfig = wifiManager.getWifiApConfiguration();
        if (wifiAPConfig == null) {
            return false;
        }
        if (default_ssid) {
            TelephonyManager tm = (TelephonyManager) ctx.getSystemService(
                    Context.TELEPHONY_SERVICE);
            String deviceId = tm.getDeviceId();
            String lastFourDigits = "";
            if ((deviceId != null) && (deviceId.length() > 3)) {
                lastFourDigits =  deviceId.substring(deviceId.length()-4);
            }
            wifiAPConfig.SSID = Build.MODEL;
            if ((!TextUtils.isEmpty(lastFourDigits)) && (wifiAPConfig.SSID != null)
                    && (wifiAPConfig.SSID.indexOf(lastFourDigits) < 0)) {
                 wifiAPConfig.SSID += " " + lastFourDigits;
            }
        }
        if (clear_password) {
            wifiAPConfig.preSharedKey = "";
        }
        wifiManager.setWifiApConfiguration(wifiAPConfig);
        return true;
    }

    private void unRegisterConfigureReceiver() {
        if (mConfigureReceiver != null) {
            getActivity().unregisterReceiver(mConfigureReceiver);
            mConfigureReceiver = null;
        }
    }

    private void registerConfigureReceiver(Context ctx) {
        IntentFilter filter = new IntentFilter(ACTION_HOTSPOT_CONFIGURE_RRSPONSE);
        if (mConfigureReceiver == null) {
            mConfigureReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (action.equals(ACTION_HOTSPOT_CONFIGURE_RRSPONSE)) {
                        boolean result = intent.getBooleanExtra(CONFIGURE_RESULT,true);
                        if (result) {
                            startTethering(mTetherChoice);
                        } else {
                            mWifiApEnabler.setChecked(false);
                        }
                    }
                }
            };
        }
        ctx.registerReceiver(mConfigureReceiver, filter);
    }
    private BluetoothProfile.ServiceListener mProfileServiceListener =
            new BluetoothProfile.ServiceListener() {
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            mBluetoothPan.set((BluetoothPan) proxy);
        }
        public void onServiceDisconnected(int profile) {
            mBluetoothPan.set(null);
        }
    };

    private static final class OnStartTetheringCallback extends
            ConnectivityManager.OnStartTetheringCallback {
        final WeakReference<TetherSettings> mTetherSettings;

        OnStartTetheringCallback(TetherSettings settings) {
            mTetherSettings = new WeakReference<TetherSettings>(settings);
        }

        @Override
        public void onTetheringStarted() {
            update();
        }

        @Override
        public void onTetheringFailed() {
            update();
        }

        private void update() {
            TetherSettings settings = mTetherSettings.get();
            if (settings != null) {
                settings.updateState();
            }
        }
    }
}
