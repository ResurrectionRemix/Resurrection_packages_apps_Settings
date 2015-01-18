/*
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (C) 2013-2014 The CyanogenMod Project
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
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.app.backup.IBackupManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.NetworkUtils;
import android.net.wifi.IWifiManager;
import android.net.wifi.WifiInfo;
import android.hardware.usb.IUsbManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StrictMode;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.hardware.usb.UsbManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.HardwareRenderer;
import android.view.IWindowManager;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Switch;
import android.widget.TextView;

import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.widget.SwitchBar;
import dalvik.system.VMRuntime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/*
 * Displays preferences for application developers.
 */
public class DevelopmentSettings extends SettingsPreferenceFragment
        implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener,
                OnPreferenceChangeListener, SwitchBar.OnSwitchChangeListener, Indexable {
    private static final String TAG = "DevelopmentSettings";

    /**
     * Preference file were development settings prefs are stored.
     */
    public static final String PREF_FILE = "development";

    /**
     * Whether to show the development settings to the user.  Default is false.
     */
    public static final String PREF_SHOW = "show";

    private static final String ENABLE_ADB = "enable_adb";
    private static final String ADB_NOTIFY = "adb_notify";
    private static final String ADB_TCPIP = "adb_over_network";
    private static final String CLEAR_ADB_KEYS = "clear_adb_keys";
    private static final String ENABLE_TERMINAL = "enable_terminal";
    private static final String KEEP_SCREEN_ON = "keep_screen_on";
    private static final String BT_HCI_SNOOP_LOG = "bt_hci_snoop_log";
    private static final String ENABLE_OEM_UNLOCK = "oem_unlock_enable";
    private static final String ALLOW_MOCK_LOCATION = "allow_mock_location";
    private static final String HDCP_CHECKING_KEY = "hdcp_checking";
    private static final String HDCP_CHECKING_PROPERTY = "persist.sys.hdcp_checking";
    private static final String ENABLE_QUICKBOOT= "enable_quickboot";
    private static final String QUICKBOOT_PACKAGE_NAME = "com.qapp.quickboot";
    private static final String LOCAL_BACKUP_PASSWORD = "local_backup_password";
    private static final String HARDWARE_UI_PROPERTY = "persist.sys.ui.hw";
    private static final String MSAA_PROPERTY = "debug.egl.force_msaa";
    private static final String BUGREPORT = "bugreport";
    private static final String BUGREPORT_IN_POWER_KEY = "bugreport_in_power";
    private static final String OPENGL_TRACES_PROPERTY = "debug.egl.trace";

    private static final String DEBUG_APP_KEY = "debug_app";
    private static final String WAIT_FOR_DEBUGGER_KEY = "wait_for_debugger";
    private static final String VERIFY_APPS_OVER_USB_KEY = "verify_apps_over_usb";
    private static final String DEBUG_VIEW_ATTRIBUTES =  "debug_view_attributes";
    private static final String STRICT_MODE_KEY = "strict_mode";
    private static final String POINTER_LOCATION_KEY = "pointer_location";
    private static final String SHOW_TOUCHES_KEY = "show_touches";
    private static final String SHOW_SCREEN_UPDATES_KEY = "show_screen_updates";
    private static final String DISABLE_OVERLAYS_KEY = "disable_overlays";
    private static final String SIMULATE_COLOR_SPACE = "simulate_color_space";
    private static final String USE_NUPLAYER_KEY = "use_nuplayer";
    private static final String USB_AUDIO_KEY = "usb_audio";
    private static final String USE_AWESOMEPLAYER_PROPERTY = "persist.sys.media.use-awesome";
    private static final String SHOW_CPU_USAGE_KEY = "show_cpu_usage";
    private static final String FORCE_HARDWARE_UI_KEY = "force_hw_ui";
    private static final String FORCE_MSAA_KEY = "force_msaa";
    private static final String TRACK_FRAME_TIME_KEY = "track_frame_time";
    private static final String SHOW_NON_RECTANGULAR_CLIP_KEY = "show_non_rect_clip";
    private static final String SHOW_HW_SCREEN_UPDATES_KEY = "show_hw_screen_udpates";
    private static final String SHOW_HW_LAYERS_UPDATES_KEY = "show_hw_layers_udpates";
    private static final String DEBUG_HW_OVERDRAW_KEY = "debug_hw_overdraw";
    private static final String DEBUG_LAYOUT_KEY = "debug_layout";
    private static final String FORCE_RTL_LAYOUT_KEY = "force_rtl_layout_all_locales";
    private static final String WINDOW_ANIMATION_SCALE_KEY = "window_animation_scale";
    private static final String TRANSITION_ANIMATION_SCALE_KEY = "transition_animation_scale";
    private static final String ANIMATOR_DURATION_SCALE_KEY = "animator_duration_scale";
    private static final String OVERLAY_DISPLAY_DEVICES_KEY = "overlay_display_devices";
    private static final String DEBUG_DEBUGGING_CATEGORY_KEY = "debug_debugging_category";
    private static final String DEBUG_APPLICATIONS_CATEGORY_KEY = "debug_applications_category";
    private static final String WIFI_DISPLAY_CERTIFICATION_KEY = "wifi_display_certification";
    private static final String WIFI_VERBOSE_LOGGING_KEY = "wifi_verbose_logging";
    private static final String WIFI_AGGRESSIVE_HANDOVER_KEY = "wifi_aggressive_handover";
    private static final String WIFI_ALLOW_SCAN_WITH_TRAFFIC_KEY = "wifi_allow_scan_with_traffic";
    private static final String SELECT_LOGD_SIZE_KEY = "select_logd_size";
    private static final String SELECT_LOGD_SIZE_PROPERTY = "persist.logd.size";
    private static final String SELECT_LOGD_DEFAULT_SIZE_PROPERTY = "ro.logd.size";

    private static final String OPENGL_TRACES_KEY = "enable_opengl_traces";

    private static final String ROOT_ACCESS_KEY = "root_access";
    private static final String ROOT_ACCESS_PROPERTY = "persist.sys.root_access";

    private static final String IMMEDIATELY_DESTROY_ACTIVITIES_KEY
            = "immediately_destroy_activities";
    private static final String APP_PROCESS_LIMIT_KEY = "app_process_limit";

    private static final String SHOW_ALL_ANRS_KEY = "show_all_anrs";

    private static final String KILL_APP_LONGPRESS_BACK = "kill_app_longpress_back";

    private static final String PROCESS_STATS = "proc_stats";

    private static final String TAG_CONFIRM_ENFORCE = "confirm_enforce";

    private static final String PACKAGE_MIME_TYPE = "application/vnd.android.package-archive";

    private static final String TERMINAL_APP_PACKAGE = "com.android.terminal";

    private static final String DEVELOPMENT_TOOLS = "development_tools";

    private static final String ADVANCED_REBOOT_KEY = "advanced_reboot";

    private static final String DEVELOPMENT_SHORTCUT_KEY = "development_shortcut";

    private static final int RESULT_DEBUG_APP = 1000;

    private static final String PERSISTENT_DATA_BLOCK_PROP = "ro.frp.pst";

    private static String DEFAULT_LOG_RING_BUFFER_SIZE_IN_BYTES = "262144"; // 256K

    private IWindowManager mWindowManager;
    private IBackupManager mBackupManager;
    private DevicePolicyManager mDpm;
    private UserManager mUm;
    private WifiManager mWifiManager;
    private UsbManager mUsbManager;

    private SwitchBar mSwitchBar;
    private boolean mLastEnabledState;
    private boolean mHaveDebugSettings;
    private boolean mDontPokeProperties;

    private SwitchPreference mEnableAdb;
    private SwitchPreference mAdbNotify;
    private SwitchPreference mAdbOverNetwork;
    private Preference mClearAdbKeys;
    private SwitchPreference mEnableTerminal;
    private Preference mBugreport;
    private SwitchPreference mBugreportInPower;
    private SwitchPreference mKeepScreenOn;
    private SwitchPreference mBtHciSnoopLog;
    private SwitchPreference mEnableOemUnlock;
    private SwitchPreference mQuickBoot;
    private SwitchPreference mAllowMockLocation;
    private SwitchPreference mDebugViewAttributes;

    private PreferenceScreen mPassword;
    private String mDebugApp;
    private Preference mDebugAppPref;
    private SwitchPreference mWaitForDebugger;
    private SwitchPreference mVerifyAppsOverUsb;
    private SwitchPreference mWifiDisplayCertification;
    private SwitchPreference mWifiVerboseLogging;
    private SwitchPreference mWifiAggressiveHandover;

    private SwitchPreference mWifiAllowScansWithTraffic;
    private SwitchPreference mStrictMode;
    private SwitchPreference mPointerLocation;
    private SwitchPreference mShowTouches;
    private SwitchPreference mShowScreenUpdates;
    private SwitchPreference mDisableOverlays;
    private SwitchPreference mShowCpuUsage;
    private SwitchPreference mForceHardwareUi;
    private SwitchPreference mForceMsaa;
    private SwitchPreference mShowHwScreenUpdates;
    private SwitchPreference mShowHwLayersUpdates;
    private SwitchPreference mDebugLayout;
    private SwitchPreference mForceRtlLayout;
    private ListPreference mDebugHwOverdraw;
    private ListPreference mLogdSize;
    private ListPreference mTrackFrameTime;
    private ListPreference mShowNonRectClip;
    private ListPreference mWindowAnimationScale;
    private ListPreference mTransitionAnimationScale;
    private ListPreference mAnimatorDurationScale;
    private ListPreference mOverlayDisplayDevices;
    private ListPreference mOpenGLTraces;

    private ListPreference mSimulateColorSpace;

    private SwitchPreference mUseNuplayer;
    private SwitchPreference mUSBAudio;
    private SwitchPreference mImmediatelyDestroyActivities;

    private ListPreference mAppProcessLimit;

    private SwitchPreference mShowAllANRs;
    private SwitchPreference mKillAppLongpressBack;

    private PreferenceScreen mProcessStats;
    private ListPreference mRootAccess;
    private Object mSelectedRootValue;
    private PreferenceScreen mDevelopmentTools;

    private SwitchPreference mAdvancedReboot;

    private SwitchPreference mDevelopmentShortcut;

    private final ArrayList<Preference> mAllPrefs = new ArrayList<Preference>();

    private final ArrayList<SwitchPreference> mResetCbPrefs
            = new ArrayList<SwitchPreference>();

    private final HashSet<Preference> mDisabledPrefs = new HashSet<Preference>();
    // To track whether a confirmation dialog was clicked.
    private boolean mDialogClicked;
    private Dialog mEnableDialog;
    private Dialog mAdbDialog;
    private Dialog mAdbTcpDialog;
    private Dialog mAdbKeysDialog;
    private boolean mUnavailable;
    private Dialog mRootDialog;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
        mBackupManager = IBackupManager.Stub.asInterface(
                ServiceManager.getService(Context.BACKUP_SERVICE));
        mDpm = (DevicePolicyManager)getActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
        mUm = (UserManager) getSystemService(Context.USER_SERVICE);

        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        mUsbManager = (UsbManager)getActivity().getSystemService(Context.USB_SERVICE);

        if (android.os.Process.myUserHandle().getIdentifier() != UserHandle.USER_OWNER
                || mUm.hasUserRestriction(UserManager.DISALLOW_DEBUGGING_FEATURES)) {
            mUnavailable = true;
            setPreferenceScreen(new PreferenceScreen(getActivity(), null));
            return;
        }

        addPreferencesFromResource(R.xml.development_prefs);

        final PreferenceGroup debugDebuggingCategory = (PreferenceGroup)
                findPreference(DEBUG_DEBUGGING_CATEGORY_KEY);

        mEnableAdb = findAndInitSwitchPref(ENABLE_ADB);
        mAdbNotify = (SwitchPreference) findPreference(ADB_NOTIFY);
        mAllPrefs.add(mAdbNotify);
        mAdbOverNetwork = findAndInitSwitchPref(ADB_TCPIP);
        mClearAdbKeys = findPreference(CLEAR_ADB_KEYS);
        if (!SystemProperties.getBoolean("ro.adb.secure", false)) {
            if (debugDebuggingCategory != null) {
                debugDebuggingCategory.removePreference(mClearAdbKeys);
            }
        }
        mAllPrefs.add(mClearAdbKeys);
        mEnableTerminal = findAndInitSwitchPref(ENABLE_TERMINAL);
        if (!Utils.isPackageInstalled(getActivity(), TERMINAL_APP_PACKAGE)) {
            debugDebuggingCategory.removePreference(mEnableTerminal);
            mEnableTerminal = null;
        }

        mBugreport = findPreference(BUGREPORT);
        mBugreportInPower = findAndInitSwitchPref(BUGREPORT_IN_POWER_KEY);
        mKeepScreenOn = findAndInitSwitchPref(KEEP_SCREEN_ON);
        mBtHciSnoopLog = findAndInitSwitchPref(BT_HCI_SNOOP_LOG);
        mEnableOemUnlock = findAndInitSwitchPref(ENABLE_OEM_UNLOCK);
        if (!showEnableOemUnlockPreference()) {
            removePreference(mEnableOemUnlock);
            mEnableOemUnlock = null;
        }
        mQuickBoot = findAndInitSwitchPref(ENABLE_QUICKBOOT);
        mAllowMockLocation = findAndInitSwitchPref(ALLOW_MOCK_LOCATION);
        mDebugViewAttributes = findAndInitSwitchPref(DEBUG_VIEW_ATTRIBUTES);
        mPassword = (PreferenceScreen) findPreference(LOCAL_BACKUP_PASSWORD);
        mAllPrefs.add(mPassword);
        mAdvancedReboot = findAndInitSwitchPref(ADVANCED_REBOOT_KEY);
        mDevelopmentShortcut = findAndInitSwitchPref(DEVELOPMENT_SHORTCUT_KEY);


        if (!android.os.Process.myUserHandle().equals(UserHandle.OWNER)) {
            disableForUser(mEnableAdb);
            disableForUser(mClearAdbKeys);
            disableForUser(mEnableTerminal);
            disableForUser(mPassword);
            disableForUser(mAdvancedReboot);
            disableForUser(mDevelopmentShortcut);
            disableForUser(mQuickBoot);
        }

        if (!Utils.isPackageInstalled(getActivity(), QUICKBOOT_PACKAGE_NAME, false)) {
            removePreference(mQuickBoot);
        }

        mDebugAppPref = findPreference(DEBUG_APP_KEY);
        mAllPrefs.add(mDebugAppPref);
        mWaitForDebugger = findAndInitSwitchPref(WAIT_FOR_DEBUGGER_KEY);
        mVerifyAppsOverUsb = (SwitchPreference) findPreference(VERIFY_APPS_OVER_USB_KEY);
        mAllPrefs.add(mVerifyAppsOverUsb);
        if (!showVerifierSetting()) {
            if (debugDebuggingCategory != null) {
                debugDebuggingCategory.removePreference(mVerifyAppsOverUsb);
            } else {
                mVerifyAppsOverUsb.setEnabled(false);
            }
        }
        mStrictMode = findAndInitSwitchPref(STRICT_MODE_KEY);
        mPointerLocation = findAndInitSwitchPref(POINTER_LOCATION_KEY);
        mShowTouches = findAndInitSwitchPref(SHOW_TOUCHES_KEY);
        mShowScreenUpdates = findAndInitSwitchPref(SHOW_SCREEN_UPDATES_KEY);
        mDisableOverlays = findAndInitSwitchPref(DISABLE_OVERLAYS_KEY);
        mShowCpuUsage = findAndInitSwitchPref(SHOW_CPU_USAGE_KEY);
        mForceHardwareUi = findAndInitSwitchPref(FORCE_HARDWARE_UI_KEY);
        mForceMsaa = findAndInitSwitchPref(FORCE_MSAA_KEY);
        mTrackFrameTime = addListPreference(TRACK_FRAME_TIME_KEY);
        mShowNonRectClip = addListPreference(SHOW_NON_RECTANGULAR_CLIP_KEY);
        mShowHwScreenUpdates = findAndInitSwitchPref(SHOW_HW_SCREEN_UPDATES_KEY);
        mShowHwLayersUpdates = findAndInitSwitchPref(SHOW_HW_LAYERS_UPDATES_KEY);
        mDebugLayout = findAndInitSwitchPref(DEBUG_LAYOUT_KEY);
        mForceRtlLayout = findAndInitSwitchPref(FORCE_RTL_LAYOUT_KEY);
        mDebugHwOverdraw = addListPreference(DEBUG_HW_OVERDRAW_KEY);
        mWifiDisplayCertification = findAndInitSwitchPref(WIFI_DISPLAY_CERTIFICATION_KEY);
        mWifiVerboseLogging = findAndInitSwitchPref(WIFI_VERBOSE_LOGGING_KEY);
        mWifiAggressiveHandover = findAndInitSwitchPref(WIFI_AGGRESSIVE_HANDOVER_KEY);
        mWifiAllowScansWithTraffic = findAndInitSwitchPref(WIFI_ALLOW_SCAN_WITH_TRAFFIC_KEY);
        mLogdSize = addListPreference(SELECT_LOGD_SIZE_KEY);

        mWindowAnimationScale = addListPreference(WINDOW_ANIMATION_SCALE_KEY);
        mTransitionAnimationScale = addListPreference(TRANSITION_ANIMATION_SCALE_KEY);
        mAnimatorDurationScale = addListPreference(ANIMATOR_DURATION_SCALE_KEY);
        mOverlayDisplayDevices = addListPreference(OVERLAY_DISPLAY_DEVICES_KEY);
        mOpenGLTraces = addListPreference(OPENGL_TRACES_KEY);
        mSimulateColorSpace = addListPreference(SIMULATE_COLOR_SPACE);
        mUseNuplayer = (SwitchPreference) findPreference(USE_NUPLAYER_KEY);
        mAllPrefs.add(mUseNuplayer);
        mUSBAudio = findAndInitSwitchPref(USB_AUDIO_KEY);

        mImmediatelyDestroyActivities = (SwitchPreference) findPreference(
                IMMEDIATELY_DESTROY_ACTIVITIES_KEY);
        mAllPrefs.add(mImmediatelyDestroyActivities);
        mResetCbPrefs.add(mImmediatelyDestroyActivities);

        mAppProcessLimit = addListPreference(APP_PROCESS_LIMIT_KEY);

        mShowAllANRs = (SwitchPreference) findPreference(
                SHOW_ALL_ANRS_KEY);
        mAllPrefs.add(mShowAllANRs);
        mResetCbPrefs.add(mShowAllANRs);

        mKillAppLongpressBack = findAndInitSwitchPref(KILL_APP_LONGPRESS_BACK);

        Preference hdcpChecking = findPreference(HDCP_CHECKING_KEY);
        if (hdcpChecking != null) {
            mAllPrefs.add(hdcpChecking);
            removePreferenceForProduction(hdcpChecking);
        }

        mProcessStats = (PreferenceScreen) findPreference(PROCESS_STATS);
        mAllPrefs.add(mProcessStats);
        mRootAccess = (ListPreference) findPreference(ROOT_ACCESS_KEY);
        mRootAccess.setOnPreferenceChangeListener(this);
        if (!removeRootOptionsIfRequired()) {
            mAllPrefs.add(mRootAccess);
        }

        mDevelopmentTools = (PreferenceScreen) findPreference(DEVELOPMENT_TOOLS);
        mAllPrefs.add(mDevelopmentTools);
    }

    private ListPreference addListPreference(String prefKey) {
        ListPreference pref = (ListPreference) findPreference(prefKey);
        mAllPrefs.add(pref);
        pref.setOnPreferenceChangeListener(this);
        return pref;
    }

    private void disableForUser(Preference pref) {
        if (pref != null) {
            pref.setEnabled(false);
            mDisabledPrefs.add(pref);
        }
    }

    private SwitchPreference findAndInitSwitchPref(String key) {
        SwitchPreference pref = (SwitchPreference) findPreference(key);
        if (pref == null) {
            throw new IllegalArgumentException("Cannot find preference with key = " + key);
        }
        if (key == ENABLE_ADB &&
                UsbManager.USB_FUNCTION_CHARGING.equals(mUsbManager.getDefaultFunction())) {
            pref.setSummary(getResources().getString(R.string.enable_adb_summary_charging));
            disableForUser(pref);
        }
        mAllPrefs.add(pref);
        mResetCbPrefs.add(pref);
        return pref;
    }

    private boolean removeRootOptionsIfRequired() {
        // user builds don't get root, and eng always gets root
        if (!(Build.IS_DEBUGGABLE || "eng".equals(Build.TYPE))) {
            if (mRootAccess != null) {
                getPreferenceScreen().removePreference(mRootAccess);
                return true;
            }
        }

        return false;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final SettingsActivity activity = (SettingsActivity) getActivity();

        mSwitchBar = activity.getSwitchBar();
       if (mUnavailable) {
            mSwitchBar.setEnabled(false);
            return;
        }

        mSwitchBar.addOnSwitchChangeListener(this);
    }

    private boolean removePreferenceForProduction(Preference preference) {
        if ("user".equals(Build.TYPE)) {
            removePreference(preference);
            return true;
        }
        return false;
    }

    private void removePreference(Preference preference) {
        getPreferenceScreen().removePreference(preference);
        mAllPrefs.remove(preference);
    }

    private void setPrefsEnabledState(boolean enabled) {
        for (int i = 0; i < mAllPrefs.size(); i++) {
            Preference pref = mAllPrefs.get(i);
            pref.setEnabled(enabled && !mDisabledPrefs.contains(pref));
        }
        updateAllOptions();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mUnavailable) {
            // Show error message
            TextView emptyView = (TextView) getView().findViewById(android.R.id.empty);
            getListView().setEmptyView(emptyView);
            if (emptyView != null) {
                emptyView.setText(R.string.development_settings_not_available);
            }
            return;
        }

        if (mDpm.getMaximumTimeToLock(null) > 0) {
            // A DeviceAdmin has specified a maximum time until the device
            // will lock...  in this case we can't allow the user to turn
            // on "stay awake when plugged in" because that would defeat the
            // restriction.
            mDisabledPrefs.add(mKeepScreenOn);
        } else {
            mDisabledPrefs.remove(mKeepScreenOn);
        }

        final ContentResolver cr = getActivity().getContentResolver();
        mLastEnabledState = Settings.Global.getInt(cr,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0;
        mSwitchBar.setChecked(mLastEnabledState);
        setPrefsEnabledState(mLastEnabledState);

        if (mHaveDebugSettings && !mLastEnabledState) {
            // Overall debugging is disabled, but there are some debug
            // settings that are enabled.  This is an invalid state.  Switch
            // to debug settings being enabled, so the user knows there is
            // stuff enabled and can turn it all off if they want.
            Settings.Global.putInt(getActivity().getContentResolver(),
                    Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);
            mLastEnabledState = true;
            mSwitchBar.setChecked(mLastEnabledState);
            setPrefsEnabledState(mLastEnabledState);
        }
        mSwitchBar.show();
        updateKillAppLongpressBackOptions();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mUnavailable) {
            return;
        }
        mSwitchBar.removeOnSwitchChangeListener(this);
        mSwitchBar.hide();
    }

    void updateSwitch(SwitchPreference Switch, boolean value) {
        Switch.setChecked(value);
        mHaveDebugSettings |= value;
    }

    private void updateAllOptions() {
        final Context context = getActivity();
        final ContentResolver cr = context.getContentResolver();
        mHaveDebugSettings = false;
        updateSwitch(mEnableAdb, Settings.Global.getInt(cr,
                Settings.Global.ADB_ENABLED, 0) != 0);
        mAdbNotify.setChecked(Settings.Secure.getInt(cr,
                Settings.Secure.ADB_NOTIFY, 1) != 0);
        updateAdbOverNetwork();
        if (mEnableTerminal != null) {
            updateSwitch(mEnableTerminal,
                    context.getPackageManager().getApplicationEnabledSetting(TERMINAL_APP_PACKAGE)
                    == PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
        }
        updateSwitch(mBugreportInPower, Settings.Secure.getInt(cr,
                Settings.Secure.BUGREPORT_IN_POWER_MENU, 0) != 0);
        updateSwitch(mKeepScreenOn, Settings.Global.getInt(cr,
                Settings.Global.STAY_ON_WHILE_PLUGGED_IN, 0) != 0);
        updateSwitch(mBtHciSnoopLog, Settings.Secure.getInt(cr,
                Settings.Secure.BLUETOOTH_HCI_LOG, 0) != 0);
        if (mEnableOemUnlock != null) {
            updateSwitch(mEnableOemUnlock, Utils.isOemUnlockEnabled(getActivity()));
        }
        updateSwitch(mAllowMockLocation, Settings.Secure.getInt(cr,
                Settings.Secure.ALLOW_MOCK_LOCATION, 0) != 0);
        updateSwitch(mDebugViewAttributes, Settings.Global.getInt(cr,
                Settings.Global.DEBUG_VIEW_ATTRIBUTES, 0) != 0);
        updateSwitch(mQuickBoot, Settings.System.getInt(cr, ENABLE_QUICKBOOT, 0) != 0);
        updateHdcpValues();
        updatePasswordSummary();
        updateDebuggerOptions();
        updateStrictModeVisualOptions();
        updatePointerLocationOptions();
        updateShowTouchesOptions();
        updateFlingerOptions();
        updateCpuUsageOptions();
        updateHardwareUiOptions();
        updateMsaaOptions();
        updateTrackFrameTimeOptions();
        updateShowNonRectClipOptions();
        updateShowHwScreenUpdatesOptions();
        updateShowHwLayersUpdatesOptions();
        updateDebugHwOverdrawOptions();
        updateDebugLayoutOptions();
        updateAnimationScaleOptions();
        updateOverlayDisplayDevicesOptions();
        updateOpenGLTracesOptions();
        updateImmediatelyDestroyActivitiesOptions();
        updateAppProcessLimitOptions();
        updateShowAllANRsOptions();
        updateVerifyAppsOverUsbOptions();
        updateBugreportOptions();
        updateForceRtlOptions();
        updateLogdSizeValues();
        updateWifiDisplayCertificationOptions();
        updateWifiVerboseLoggingOptions();
        updateWifiAggressiveHandoverOptions();
        updateWifiAllowScansWithTrafficOptions();
        updateSimulateColorSpace();
        updateUseNuplayerOptions();
        updateUSBAudioOptions();
        updateRootAccessOptions();
        updateAdvancedRebootOptions();
        updateDevelopmentShortcutOptions();
    }

    private void writeAdvancedRebootOptions() {
        Settings.Secure.putInt(getActivity().getContentResolver(),
                Settings.Secure.ADVANCED_REBOOT,
                mAdvancedReboot.isChecked() ? 1 : 0);
    }

    private void updateAdvancedRebootOptions() {
        mAdvancedReboot.setChecked(Settings.Secure.getInt(getActivity().getContentResolver(),
                Settings.Secure.ADVANCED_REBOOT, 0) != 0);
    }

    private void resetDevelopmentShortcutOptions() {
        Settings.Secure.putInt(getActivity().getContentResolver(),
                Settings.Secure.DEVELOPMENT_SHORTCUT, 0);
    }

    private void writeDevelopmentShortcutOptions() {
        Settings.Secure.putInt(getActivity().getContentResolver(),
                Settings.Secure.DEVELOPMENT_SHORTCUT,
                mDevelopmentShortcut.isChecked() ? 1 : 0);
    }

    private void updateDevelopmentShortcutOptions() {
        mDevelopmentShortcut.setChecked(Settings.Secure.getInt(getActivity().getContentResolver(),
                Settings.Secure.DEVELOPMENT_SHORTCUT, 0) != 0);
    }

    private void updateAdbOverNetwork() {
        int port = Settings.Secure.getInt(getActivity().getContentResolver(),
                Settings.Secure.ADB_PORT, 0);
        boolean enabled = port > 0;

        updateSwitch(mAdbOverNetwork, enabled);

        WifiInfo wifiInfo = null;

        if (enabled) {
            IWifiManager wifiManager = IWifiManager.Stub.asInterface(
                    ServiceManager.getService(Context.WIFI_SERVICE));
            try {
                wifiInfo = wifiManager.getConnectionInfo();
            } catch (RemoteException e) {
                Log.e(TAG, "wifiManager, getConnectionInfo()", e);
            }
        }

        if (wifiInfo != null) {
            String hostAddress = NetworkUtils.intToInetAddress(
                    wifiInfo.getIpAddress()).getHostAddress();
            mAdbOverNetwork.setSummary(hostAddress + ":" + String.valueOf(port));
        } else {
            mAdbOverNetwork.setSummary(R.string.adb_over_network_summary);
        }
    }

    private void resetDangerousOptions() {
        mDontPokeProperties = true;
        for (int i=0; i<mResetCbPrefs.size(); i++) {
            SwitchPreference cb = mResetCbPrefs.get(i);
            if (cb.isChecked()) {
                cb.setChecked(false);
                onPreferenceTreeClick(null, cb);
            }
        }
        resetDebuggerOptions();
        writeLogdSizeOption(null);
        resetRootAccessOptions();
        resetAdbNotifyOptions();
        resetVerifyAppsOverUsbOptions();
        resetDevelopmentShortcutOptions();
        resetUseNuplayerOptions();
        writeAnimationScaleOption(0, mWindowAnimationScale, null);
        writeAnimationScaleOption(1, mTransitionAnimationScale, null);
        writeAnimationScaleOption(2, mAnimatorDurationScale, null);
        // Only poke the color space setting if we control it.
        if (usingDevelopmentColorSpace()) {
            writeSimulateColorSpace(-1);
        }
        writeOverlayDisplayDevicesOptions(null);
        writeAppProcessLimitOptions(null);
        mHaveDebugSettings = false;
        updateAllOptions();
        mDontPokeProperties = false;
        pokeSystemProperties();
    }

    private void updateRootAccessOptions() {
        String value = SystemProperties.get(ROOT_ACCESS_PROPERTY, "0");
        mRootAccess.setValue(value);
        mRootAccess.setSummary(getResources()
                .getStringArray(R.array.root_access_entries)[Integer.valueOf(value)]);
    }

    public static boolean isRootForAppsEnabled() {
        int value = SystemProperties.getInt(ROOT_ACCESS_PROPERTY, 0);
        boolean daemonState =
                SystemProperties.get("init.svc.su_daemon", "absent").equals("running");
        return daemonState && (value == 1 || value == 3);
    }

    private void writeRootAccessOptions(Object newValue) {
        String oldValue = SystemProperties.get(ROOT_ACCESS_PROPERTY, "0");
        SystemProperties.set(ROOT_ACCESS_PROPERTY, newValue.toString());
        if (Integer.valueOf(newValue.toString()) < 2 && !oldValue.equals(newValue)
                && "1".equals(SystemProperties.get("service.adb.root", "0"))) {
            SystemProperties.set("service.adb.root", "0");
            Settings.Secure.putInt(getActivity().getContentResolver(),
                    Settings.Secure.ADB_ENABLED, 0);
            Settings.Secure.putInt(getActivity().getContentResolver(),
                    Settings.Secure.ADB_ENABLED, 1);
        }
        updateRootAccessOptions();
    }

    private void resetRootAccessOptions() {
        String oldValue = SystemProperties.get(ROOT_ACCESS_PROPERTY, "0");
        SystemProperties.set(ROOT_ACCESS_PROPERTY, "0");
        if (!oldValue.equals("0") && "1".equals(SystemProperties.get("service.adb.root", "0"))) {
            SystemProperties.set("service.adb.root", "0");
            Settings.Secure.putInt(getActivity().getContentResolver(),
                    Settings.Secure.ADB_ENABLED, 0);
            Settings.Secure.putInt(getActivity().getContentResolver(),
                    Settings.Secure.ADB_ENABLED, 1);
        }
        updateRootAccessOptions();
    }

    private void resetAdbNotifyOptions() {
        Settings.Secure.putInt(getActivity().getContentResolver(),
                Settings.Secure.ADB_NOTIFY, 1);
    }

    private void resetUseNuplayerOptions() {
        SystemProperties.set(USE_AWESOMEPLAYER_PROPERTY, "0");
    }

    private void updateHdcpValues() {
        ListPreference hdcpChecking = (ListPreference) findPreference(HDCP_CHECKING_KEY);
        if (hdcpChecking != null) {
            String currentValue = SystemProperties.get(HDCP_CHECKING_PROPERTY);
            String[] values = getResources().getStringArray(R.array.hdcp_checking_values);
            String[] summaries = getResources().getStringArray(R.array.hdcp_checking_summaries);
            int index = 1; // Defaults to drm-only. Needs to match with R.array.hdcp_checking_values
            for (int i = 0; i < values.length; i++) {
                if (currentValue.equals(values[i])) {
                    index = i;
                    break;
                }
            }
            hdcpChecking.setValue(values[index]);
            hdcpChecking.setSummary(summaries[index]);
            hdcpChecking.setOnPreferenceChangeListener(this);
        }
    }

    private void writeKillAppLongpressBackOptions() {
        Settings.Secure.putInt(getActivity().getContentResolver(),
                Settings.Secure.KILL_APP_LONGPRESS_BACK,
                mKillAppLongpressBack.isChecked() ? 1 : 0);
    }

    private void updateKillAppLongpressBackOptions() {
        mKillAppLongpressBack.setChecked(Settings.Secure.getInt(
            getActivity().getContentResolver(), Settings.Secure.KILL_APP_LONGPRESS_BACK, 0) != 0);
    }

    private void updatePasswordSummary() {
        try {
            if (mBackupManager.hasBackupPassword()) {
                mPassword.setSummary(R.string.local_backup_password_summary_change);
            } else {
                mPassword.setSummary(R.string.local_backup_password_summary_none);
            }
        } catch (RemoteException e) {
            // Not much we can do here
        }
    }

    private void writeBtHciSnoopLogOptions() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        adapter.configHciSnoopLog(mBtHciSnoopLog.isChecked());
        Settings.Secure.putInt(getActivity().getContentResolver(),
                Settings.Secure.BLUETOOTH_HCI_LOG,
                mBtHciSnoopLog.isChecked() ? 1 : 0);
    }

    private void writeDebuggerOptions() {
        try {
            ActivityManagerNative.getDefault().setDebugApp(
                mDebugApp, mWaitForDebugger.isChecked(), true);
        } catch (RemoteException ex) {
        }
    }

    private static void resetDebuggerOptions() {
        try {
            ActivityManagerNative.getDefault().setDebugApp(
                    null, false, true);
        } catch (RemoteException ex) {
        }
    }

    private void updateDebuggerOptions() {
        mDebugApp = Settings.Global.getString(
                getActivity().getContentResolver(), Settings.Global.DEBUG_APP);
        updateSwitch(mWaitForDebugger, Settings.Global.getInt(
                getActivity().getContentResolver(), Settings.Global.WAIT_FOR_DEBUGGER, 0) != 0);
        if (mDebugApp != null && mDebugApp.length() > 0) {
            String label;
            try {
                ApplicationInfo ai = getActivity().getPackageManager().getApplicationInfo(mDebugApp,
                        PackageManager.GET_DISABLED_COMPONENTS);
                CharSequence lab = getActivity().getPackageManager().getApplicationLabel(ai);
                label = lab != null ? lab.toString() : mDebugApp;
            } catch (PackageManager.NameNotFoundException e) {
                label = mDebugApp;
            }
            mDebugAppPref.setSummary(getResources().getString(R.string.debug_app_set, label));
            mWaitForDebugger.setEnabled(true);
            mHaveDebugSettings = true;
        } else {
            mDebugAppPref.setSummary(getResources().getString(R.string.debug_app_not_set));
            mWaitForDebugger.setEnabled(false);
        }
    }

    private void resetVerifyAppsOverUsbOptions() {
        Settings.Global.putInt(getActivity().getContentResolver(),
              Settings.Global.PACKAGE_VERIFIER_INCLUDE_ADB, 1);
    }

    private void updateVerifyAppsOverUsbOptions() {
        updateSwitch(mVerifyAppsOverUsb, Settings.Global.getInt(getActivity().getContentResolver(),
                Settings.Global.PACKAGE_VERIFIER_INCLUDE_ADB, 1) != 0);
        mVerifyAppsOverUsb.setEnabled(enableVerifierSetting());
    }

    private void writeVerifyAppsOverUsbOptions() {
        Settings.Global.putInt(getActivity().getContentResolver(),
              Settings.Global.PACKAGE_VERIFIER_INCLUDE_ADB, mVerifyAppsOverUsb.isChecked() ? 1 : 0);
    }

    private boolean enableVerifierSetting() {
        final ContentResolver cr = getActivity().getContentResolver();
        if (Settings.Global.getInt(cr, Settings.Global.ADB_ENABLED, 0) == 0) {
            return false;
        }
        if (Settings.Global.getInt(cr, Settings.Global.PACKAGE_VERIFIER_ENABLE, 1) == 0) {
            return false;
        } else {
            final PackageManager pm = getActivity().getPackageManager();
            final Intent verification = new Intent(Intent.ACTION_PACKAGE_NEEDS_VERIFICATION);
            verification.setType(PACKAGE_MIME_TYPE);
            verification.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            final List<ResolveInfo> receivers = pm.queryBroadcastReceivers(verification, 0);
            if (receivers.size() == 0) {
                return false;
            }
        }
        return true;
    }

    private boolean showVerifierSetting() {
        return Settings.Global.getInt(getActivity().getContentResolver(),
                Settings.Global.PACKAGE_VERIFIER_SETTING_VISIBLE, 1) > 0;
    }

    private static boolean showEnableOemUnlockPreference() {
        return !SystemProperties.get(PERSISTENT_DATA_BLOCK_PROP).equals("");
    }

    private void updateBugreportOptions() {
        if ("user".equals(Build.TYPE)) {
            final ContentResolver resolver = getActivity().getContentResolver();
            final boolean adbEnabled = Settings.Global.getInt(
                    resolver, Settings.Global.ADB_ENABLED, 0) != 0;
            if (adbEnabled) {
                mBugreport.setEnabled(true);
                mBugreportInPower.setEnabled(true);
            } else {
                mBugreport.setEnabled(false);
                mBugreportInPower.setEnabled(false);
                mBugreportInPower.setChecked(false);
                Settings.Secure.putInt(resolver, Settings.Secure.BUGREPORT_IN_POWER_MENU, 0);
            }
        } else {
            mBugreportInPower.setEnabled(true);
        }
    }

    // Returns the current state of the system property that controls
    // strictmode flashes.  One of:
    //    0: not explicitly set one way or another
    //    1: on
    //    2: off
    private static int currentStrictModeActiveIndex() {
        if (TextUtils.isEmpty(SystemProperties.get(StrictMode.VISUAL_PROPERTY))) {
            return 0;
        }
        boolean enabled = SystemProperties.getBoolean(StrictMode.VISUAL_PROPERTY, false);
        return enabled ? 1 : 2;
    }

    private void writeStrictModeVisualOptions() {
        try {
            mWindowManager.setStrictModeVisualIndicatorPreference(mStrictMode.isChecked()
                    ? "1" : "");
        } catch (RemoteException e) {
        }
    }

    private void updateStrictModeVisualOptions() {
        updateSwitch(mStrictMode, currentStrictModeActiveIndex() == 1);
    }

    private void writePointerLocationOptions() {
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.POINTER_LOCATION, mPointerLocation.isChecked() ? 1 : 0);
    }

    private void updatePointerLocationOptions() {
        updateSwitch(mPointerLocation, Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.POINTER_LOCATION, 0) != 0);
    }

    private void writeShowTouchesOptions() {
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.SHOW_TOUCHES, mShowTouches.isChecked() ? 1 : 0);
    }

    private void updateShowTouchesOptions() {
        updateSwitch(mShowTouches, Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.SHOW_TOUCHES, 0) != 0);
    }

    private void updateFlingerOptions() {
        // magic communication with surface flinger.
        try {
            IBinder flinger = ServiceManager.getService("SurfaceFlinger");
            if (flinger != null) {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                flinger.transact(1010, data, reply, 0);
                @SuppressWarnings("unused")
                int showCpu = reply.readInt();
                @SuppressWarnings("unused")
                int enableGL = reply.readInt();
                int showUpdates = reply.readInt();
                updateSwitch(mShowScreenUpdates, showUpdates != 0);
                @SuppressWarnings("unused")
                int showBackground = reply.readInt();
                int disableOverlays = reply.readInt();
                updateSwitch(mDisableOverlays, disableOverlays != 0);
                reply.recycle();
                data.recycle();
            }
        } catch (RemoteException ex) {
        }
    }

    private void writeShowUpdatesOption() {
        try {
            IBinder flinger = ServiceManager.getService("SurfaceFlinger");
            if (flinger != null) {
                Parcel data = Parcel.obtain();
                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                final int showUpdates = mShowScreenUpdates.isChecked() ? 1 : 0;
                data.writeInt(showUpdates);
                flinger.transact(1002, data, null, 0);
                data.recycle();

                updateFlingerOptions();
            }
        } catch (RemoteException ex) {
        }
    }

    private void writeDisableOverlaysOption() {
        try {
            IBinder flinger = ServiceManager.getService("SurfaceFlinger");
            if (flinger != null) {
                Parcel data = Parcel.obtain();
                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                final int disableOverlays = mDisableOverlays.isChecked() ? 1 : 0;
                data.writeInt(disableOverlays);
                flinger.transact(1008, data, null, 0);
                data.recycle();

                updateFlingerOptions();
            }
        } catch (RemoteException ex) {
        }
    }

    private void updateHardwareUiOptions() {
        updateSwitch(mForceHardwareUi, SystemProperties.getBoolean(HARDWARE_UI_PROPERTY, false));
    }

    private void writeHardwareUiOptions() {
        SystemProperties.set(HARDWARE_UI_PROPERTY, mForceHardwareUi.isChecked() ? "true" : "false");
        pokeSystemProperties();
    }

    private void updateMsaaOptions() {
        updateSwitch(mForceMsaa, SystemProperties.getBoolean(MSAA_PROPERTY, false));
    }

    private void writeMsaaOptions() {
        SystemProperties.set(MSAA_PROPERTY, mForceMsaa.isChecked() ? "true" : "false");
        pokeSystemProperties();
    }

    private void updateTrackFrameTimeOptions() {
        String value = SystemProperties.get(HardwareRenderer.PROFILE_PROPERTY);
        if (value == null) {
            value = "";
        }

        CharSequence[] values = mTrackFrameTime.getEntryValues();
        for (int i = 0; i < values.length; i++) {
            if (value.contentEquals(values[i])) {
                mTrackFrameTime.setValueIndex(i);
                mTrackFrameTime.setSummary(mTrackFrameTime.getEntries()[i]);
                return;
            }
        }
        mTrackFrameTime.setValueIndex(0);
        mTrackFrameTime.setSummary(mTrackFrameTime.getEntries()[0]);
    }

    private void writeTrackFrameTimeOptions(Object newValue) {
        SystemProperties.set(HardwareRenderer.PROFILE_PROPERTY,
                newValue == null ? "" : newValue.toString());
        pokeSystemProperties();
        updateTrackFrameTimeOptions();
    }

    private void updateShowNonRectClipOptions() {
        String value = SystemProperties.get(
                HardwareRenderer.DEBUG_SHOW_NON_RECTANGULAR_CLIP_PROPERTY);
        if (value == null) {
            value = "hide";
        }

        CharSequence[] values = mShowNonRectClip.getEntryValues();
        for (int i = 0; i < values.length; i++) {
            if (value.contentEquals(values[i])) {
                mShowNonRectClip.setValueIndex(i);
                mShowNonRectClip.setSummary(mShowNonRectClip.getEntries()[i]);
                return;
            }
        }
        mShowNonRectClip.setValueIndex(0);
        mShowNonRectClip.setSummary(mShowNonRectClip.getEntries()[0]);
    }

    private void writeShowNonRectClipOptions(Object newValue) {
        SystemProperties.set(HardwareRenderer.DEBUG_SHOW_NON_RECTANGULAR_CLIP_PROPERTY,
                newValue == null ? "" : newValue.toString());
        pokeSystemProperties();
        updateShowNonRectClipOptions();
    }

    private void updateShowHwScreenUpdatesOptions() {
        updateSwitch(mShowHwScreenUpdates,
                SystemProperties.getBoolean(HardwareRenderer.DEBUG_DIRTY_REGIONS_PROPERTY, false));
    }

    private void writeShowHwScreenUpdatesOptions() {
        SystemProperties.set(HardwareRenderer.DEBUG_DIRTY_REGIONS_PROPERTY,
                mShowHwScreenUpdates.isChecked() ? "true" : null);
        pokeSystemProperties();
    }

    private void updateShowHwLayersUpdatesOptions() {
        updateSwitch(mShowHwLayersUpdates, SystemProperties.getBoolean(
                HardwareRenderer.DEBUG_SHOW_LAYERS_UPDATES_PROPERTY, false));
    }

    private void writeShowHwLayersUpdatesOptions() {
        SystemProperties.set(HardwareRenderer.DEBUG_SHOW_LAYERS_UPDATES_PROPERTY,
                mShowHwLayersUpdates.isChecked() ? "true" : null);
        pokeSystemProperties();
    }

    private void updateDebugHwOverdrawOptions() {
        String value = SystemProperties.get(HardwareRenderer.DEBUG_OVERDRAW_PROPERTY);
        if (value == null) {
            value = "";
        }

        CharSequence[] values = mDebugHwOverdraw.getEntryValues();
        for (int i = 0; i < values.length; i++) {
            if (value.contentEquals(values[i])) {
                mDebugHwOverdraw.setValueIndex(i);
                mDebugHwOverdraw.setSummary(mDebugHwOverdraw.getEntries()[i]);
                return;
            }
        }
        mDebugHwOverdraw.setValueIndex(0);
        mDebugHwOverdraw.setSummary(mDebugHwOverdraw.getEntries()[0]);
    }

    private void writeDebugHwOverdrawOptions(Object newValue) {
        SystemProperties.set(HardwareRenderer.DEBUG_OVERDRAW_PROPERTY,
                newValue == null ? "" : newValue.toString());
        pokeSystemProperties();
        updateDebugHwOverdrawOptions();
    }

    private void updateDebugLayoutOptions() {
        updateSwitch(mDebugLayout,
                SystemProperties.getBoolean(View.DEBUG_LAYOUT_PROPERTY, false));
    }

    private void writeDebugLayoutOptions() {
        SystemProperties.set(View.DEBUG_LAYOUT_PROPERTY,
                mDebugLayout.isChecked() ? "true" : "false");
        pokeSystemProperties();
    }

    private void updateSimulateColorSpace() {
        final ContentResolver cr = getContentResolver();
        final boolean enabled = Settings.Secure.getInt(
                cr, Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, 0) != 0;
        if (enabled) {
            final String mode = Integer.toString(Settings.Secure.getInt(
                    cr, Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER,
                    AccessibilityManager.DALTONIZER_DISABLED));
            mSimulateColorSpace.setValue(mode);
            final int index = mSimulateColorSpace.findIndexOfValue(mode);
            if (index < 0) {
                // We're using a mode controlled by accessibility preferences.
                mSimulateColorSpace.setSummary(getString(R.string.daltonizer_type_overridden,
                        getString(R.string.accessibility_display_daltonizer_preference_title)));
            } else {
                mSimulateColorSpace.setSummary("%s");
            }
        } else {
            mSimulateColorSpace.setValue(
                    Integer.toString(AccessibilityManager.DALTONIZER_DISABLED));
        }
    }

    /**
     * @return <code>true</code> if the color space preference is currently
     *         controlled by development settings
     */
    private boolean usingDevelopmentColorSpace() {
        final ContentResolver cr = getContentResolver();
        final boolean enabled = Settings.Secure.getInt(
                cr, Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, 0) != 0;
        if (enabled) {
            final String mode = Integer.toString(Settings.Secure.getInt(
                    cr, Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER,
                    AccessibilityManager.DALTONIZER_DISABLED));
            final int index = mSimulateColorSpace.findIndexOfValue(mode);
            if (index >= 0) {
                // We're using a mode controlled by developer preferences.
                return true;
            }
        }
        return false;
    }

    private void writeSimulateColorSpace(Object value) {
        final ContentResolver cr = getContentResolver();
        final int newMode = Integer.parseInt(value.toString());
        if (newMode < 0) {
            Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, 0);
        } else {
            Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, 1);
            Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER, newMode);
        }
    }

    private void updateUseNuplayerOptions() {
        updateSwitch(
                mUseNuplayer, !SystemProperties.getBoolean(USE_AWESOMEPLAYER_PROPERTY, false));
    }

    private void writeUseNuplayerOptions() {
        SystemProperties.set(
                USE_AWESOMEPLAYER_PROPERTY, mUseNuplayer.isChecked() ? "false" : "true");
        pokeSystemProperties();
    }

    private void updateUSBAudioOptions() {
        updateSwitch(mUSBAudio, Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.USB_AUDIO_AUTOMATIC_ROUTING_DISABLED, 0) != 0);
    }

    private void writeUSBAudioOptions() {
        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.USB_AUDIO_AUTOMATIC_ROUTING_DISABLED,
                mUSBAudio.isChecked() ? 1 : 0);
    }

    private void updateForceRtlOptions() {
        updateSwitch(mForceRtlLayout, Settings.Global.getInt(getActivity().getContentResolver(),
                Settings.Global.DEVELOPMENT_FORCE_RTL, 0) != 0);
    }

    private void writeForceRtlOptions() {
        boolean value = mForceRtlLayout.isChecked();
        Settings.Global.putInt(getActivity().getContentResolver(),
                Settings.Global.DEVELOPMENT_FORCE_RTL, value ? 1 : 0);
        SystemProperties.set(Settings.Global.DEVELOPMENT_FORCE_RTL, value ? "1" : "0");
        LocalePicker.updateLocale(getActivity().getResources().getConfiguration().locale);
    }

    private void updateWifiDisplayCertificationOptions() {
        updateSwitch(mWifiDisplayCertification, Settings.Global.getInt(
                getActivity().getContentResolver(),
                Settings.Global.WIFI_DISPLAY_CERTIFICATION_ON, 0) != 0);
    }

    private void writeWifiDisplayCertificationOptions() {
        Settings.Global.putInt(getActivity().getContentResolver(),
                Settings.Global.WIFI_DISPLAY_CERTIFICATION_ON,
                mWifiDisplayCertification.isChecked() ? 1 : 0);
    }

    private void updateWifiVerboseLoggingOptions() {
        boolean enabled = mWifiManager.getVerboseLoggingLevel() > 0;
        updateSwitch(mWifiVerboseLogging, enabled);
    }

    private void writeWifiVerboseLoggingOptions() {
        mWifiManager.enableVerboseLogging(mWifiVerboseLogging.isChecked() ? 1 : 0);
    }

    private void updateWifiAggressiveHandoverOptions() {
        boolean enabled = mWifiManager.getAggressiveHandover() > 0;
        updateSwitch(mWifiAggressiveHandover, enabled);
    }

    private void writeWifiAggressiveHandoverOptions() {
        mWifiManager.enableAggressiveHandover(mWifiAggressiveHandover.isChecked() ? 1 : 0);
    }

    private void updateWifiAllowScansWithTrafficOptions() {
        boolean enabled = mWifiManager.getAllowScansWithTraffic() > 0;
        updateSwitch(mWifiAllowScansWithTraffic, enabled);
    }

    private void writeWifiAllowScansWithTrafficOptions() {
        mWifiManager.setAllowScansWithTraffic(mWifiAllowScansWithTraffic.isChecked() ? 1 : 0);
    }

    private void updateLogdSizeValues() {
        if (mLogdSize != null) {
            String currentValue = SystemProperties.get(SELECT_LOGD_SIZE_PROPERTY);
            if (currentValue == null) {
                currentValue = SystemProperties.get(SELECT_LOGD_DEFAULT_SIZE_PROPERTY);
                if (currentValue == null) {
                    currentValue = "256K";
                }
            }
            String[] values = getResources().getStringArray(R.array.select_logd_size_values);
            String[] titles = getResources().getStringArray(R.array.select_logd_size_titles);
            if (SystemProperties.get("ro.config.low_ram").equals("true")) {
                mLogdSize.setEntries(R.array.select_logd_size_lowram_titles);
                titles = getResources().getStringArray(R.array.select_logd_size_lowram_titles);
            }
            String[] summaries = getResources().getStringArray(R.array.select_logd_size_summaries);
            int index = 1; // punt to second entry if not found
            for (int i = 0; i < titles.length; i++) {
                if (currentValue.equals(values[i])
                        || currentValue.equals(titles[i])) {
                    index = i;
                    break;
                }
            }
            mLogdSize.setValue(values[index]);
            mLogdSize.setSummary(summaries[index]);
            mLogdSize.setOnPreferenceChangeListener(this);
        }
    }

    private void writeLogdSizeOption(Object newValue) {
        String currentValue = SystemProperties.get(SELECT_LOGD_DEFAULT_SIZE_PROPERTY);
        if (currentValue != null) {
            DEFAULT_LOG_RING_BUFFER_SIZE_IN_BYTES = currentValue;
        }
        final String size = (newValue != null) ?
                newValue.toString() : DEFAULT_LOG_RING_BUFFER_SIZE_IN_BYTES;
        SystemProperties.set(SELECT_LOGD_SIZE_PROPERTY, size);
        pokeSystemProperties();
        try {
            Process p = Runtime.getRuntime().exec("logcat -b all -G " + size);
            p.waitFor();
            Log.i(TAG, "Logcat ring buffer sizes set to: " + size);
        } catch (Exception e) {
            Log.w(TAG, "Cannot set logcat ring buffer sizes", e);
        }
        updateLogdSizeValues();
    }

    private void updateCpuUsageOptions() {
        updateSwitch(mShowCpuUsage, Settings.Global.getInt(getActivity().getContentResolver(),
                Settings.Global.SHOW_PROCESSES, 0) != 0);
    }

    private void writeCpuUsageOptions() {
        boolean value = mShowCpuUsage.isChecked();
        Settings.Global.putInt(getActivity().getContentResolver(),
                Settings.Global.SHOW_PROCESSES, value ? 1 : 0);
        Intent service = (new Intent())
                .setClassName("com.android.systemui", "com.android.systemui.LoadAverageService");
        if (value) {
            getActivity().startService(service);
        } else {
            getActivity().stopService(service);
        }
    }

    private void writeImmediatelyDestroyActivitiesOptions() {
        try {
            ActivityManagerNative.getDefault().setAlwaysFinish(
                    mImmediatelyDestroyActivities.isChecked());
        } catch (RemoteException ex) {
        }
    }

    private void updateImmediatelyDestroyActivitiesOptions() {
        updateSwitch(mImmediatelyDestroyActivities, Settings.Global.getInt(
            getActivity().getContentResolver(), Settings.Global.ALWAYS_FINISH_ACTIVITIES, 0) != 0);
    }

    private void updateAnimationScaleValue(int which, ListPreference pref) {
        try {
            float scale = mWindowManager.getAnimationScale(which);
            if (scale != 1) {
                mHaveDebugSettings = true;
            }
            CharSequence[] values = pref.getEntryValues();
            for (int i=0; i<values.length; i++) {
                float val = Float.parseFloat(values[i].toString());
                if (scale <= val) {
                    pref.setValueIndex(i);
                    pref.setSummary(pref.getEntries()[i]);
                    return;
                }
            }
            pref.setValueIndex(values.length-1);
            pref.setSummary(pref.getEntries()[0]);
        } catch (RemoteException e) {
        }
    }

    private void updateAnimationScaleOptions() {
        updateAnimationScaleValue(0, mWindowAnimationScale);
        updateAnimationScaleValue(1, mTransitionAnimationScale);
        updateAnimationScaleValue(2, mAnimatorDurationScale);
    }

    private void writeAnimationScaleOption(int which, ListPreference pref, Object newValue) {
        try {
            float scale = newValue != null ? Float.parseFloat(newValue.toString()) : 1;
            mWindowManager.setAnimationScale(which, scale);
            updateAnimationScaleValue(which, pref);
        } catch (RemoteException e) {
        }
    }

    private void updateOverlayDisplayDevicesOptions() {
        String value = Settings.Global.getString(getActivity().getContentResolver(),
                Settings.Global.OVERLAY_DISPLAY_DEVICES);
        if (value == null) {
            value = "";
        }

        CharSequence[] values = mOverlayDisplayDevices.getEntryValues();
        for (int i = 0; i < values.length; i++) {
            if (value.contentEquals(values[i])) {
                mOverlayDisplayDevices.setValueIndex(i);
                mOverlayDisplayDevices.setSummary(mOverlayDisplayDevices.getEntries()[i]);
                return;
            }
        }
        mOverlayDisplayDevices.setValueIndex(0);
        mOverlayDisplayDevices.setSummary(mOverlayDisplayDevices.getEntries()[0]);
    }

    private void writeOverlayDisplayDevicesOptions(Object newValue) {
        Settings.Global.putString(getActivity().getContentResolver(),
                Settings.Global.OVERLAY_DISPLAY_DEVICES, (String)newValue);
        updateOverlayDisplayDevicesOptions();
    }

    private void updateOpenGLTracesOptions() {
        String value = SystemProperties.get(OPENGL_TRACES_PROPERTY);
        if (value == null) {
            value = "";
        }

        CharSequence[] values = mOpenGLTraces.getEntryValues();
        for (int i = 0; i < values.length; i++) {
            if (value.contentEquals(values[i])) {
                mOpenGLTraces.setValueIndex(i);
                mOpenGLTraces.setSummary(mOpenGLTraces.getEntries()[i]);
                return;
            }
        }
        mOpenGLTraces.setValueIndex(0);
        mOpenGLTraces.setSummary(mOpenGLTraces.getEntries()[0]);
    }

    private void writeOpenGLTracesOptions(Object newValue) {
        SystemProperties.set(OPENGL_TRACES_PROPERTY, newValue == null ? "" : newValue.toString());
        pokeSystemProperties();
        updateOpenGLTracesOptions();
    }

    private void updateAppProcessLimitOptions() {
        try {
            int limit = ActivityManagerNative.getDefault().getProcessLimit();
            CharSequence[] values = mAppProcessLimit.getEntryValues();
            for (int i=0; i<values.length; i++) {
                int val = Integer.parseInt(values[i].toString());
                if (val >= limit) {
                    if (i != 0) {
                        mHaveDebugSettings = true;
                    }
                    mAppProcessLimit.setValueIndex(i);
                    mAppProcessLimit.setSummary(mAppProcessLimit.getEntries()[i]);
                    return;
                }
            }
            mAppProcessLimit.setValueIndex(0);
            mAppProcessLimit.setSummary(mAppProcessLimit.getEntries()[0]);
        } catch (RemoteException e) {
        }
    }

    private void writeAppProcessLimitOptions(Object newValue) {
        try {
            int limit = newValue != null ? Integer.parseInt(newValue.toString()) : -1;
            ActivityManagerNative.getDefault().setProcessLimit(limit);
            updateAppProcessLimitOptions();
        } catch (RemoteException e) {
        }
    }

    private void writeShowAllANRsOptions() {
        Settings.Secure.putInt(getActivity().getContentResolver(),
                Settings.Secure.ANR_SHOW_BACKGROUND,
                mShowAllANRs.isChecked() ? 1 : 0);
    }

    private void updateShowAllANRsOptions() {
        updateSwitch(mShowAllANRs, Settings.Secure.getInt(
            getActivity().getContentResolver(), Settings.Secure.ANR_SHOW_BACKGROUND, 0) != 0);
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        if (switchView != mSwitchBar.getSwitch()) {
            return;
        }
        if (isChecked != mLastEnabledState) {
            if (isChecked) {
                mDialogClicked = false;
                if (mEnableDialog != null) {
                    dismissDialogs();
                }
                mEnableDialog = new AlertDialog.Builder(getActivity()).setMessage(
                        getActivity().getResources().getString(
                                R.string.dev_settings_warning_message))
                        .setTitle(R.string.dev_settings_warning_title)
                        .setPositiveButton(android.R.string.yes, this)
                        .setNegativeButton(android.R.string.no, this)
                        .show();
                mEnableDialog.setOnDismissListener(this);
            } else {
                resetDangerousOptions();
                Settings.Global.putInt(getActivity().getContentResolver(),
                        Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);
                mLastEnabledState = isChecked;
                setPrefsEnabledState(mLastEnabledState);

                // Hide development settings from the Settings menu (Android 4.2 behaviour)
                getActivity().getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE).edit()
                    .putBoolean(PREF_SHOW, false)
                    .apply();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RESULT_DEBUG_APP) {
            if (resultCode == Activity.RESULT_OK) {
                mDebugApp = data.getAction();
                writeDebuggerOptions();
                updateDebuggerOptions();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (Utils.isMonkeyRunning()) {
            return false;
        }

        if (preference == mEnableAdb) {
            if (mEnableAdb.isChecked()) {
                mDialogClicked = false;
                if (mAdbDialog != null) {
                    dismissDialogs();
                }
                mAdbDialog = new AlertDialog.Builder(getActivity()).setMessage(
                        getActivity().getResources().getString(R.string.adb_warning_message))
                        .setTitle(R.string.adb_warning_title)
                        .setPositiveButton(android.R.string.yes, this)
                        .setNegativeButton(android.R.string.no, this)
                        .show();
                mAdbDialog.setOnDismissListener(this);
            } else {
                Settings.Global.putInt(getActivity().getContentResolver(),
                        Settings.Global.ADB_ENABLED, 0);
                mVerifyAppsOverUsb.setEnabled(false);
                mVerifyAppsOverUsb.setChecked(false);
                updateBugreportOptions();
            }
        } else if (preference == mAdbNotify) {
            Settings.Secure.putInt(getActivity().getContentResolver(),
                    Settings.Secure.ADB_NOTIFY,
                    mAdbNotify.isChecked() ? 1 : 0);
        } else if (preference == mAdbOverNetwork) {
            if (mAdbOverNetwork.isChecked()) {
                if (mAdbTcpDialog != null) {
                    dismissDialogs();
                }
                mAdbTcpDialog = new AlertDialog.Builder(getActivity()).setMessage(
                        getResources().getString(R.string.adb_over_network_warning))
                        .setTitle(R.string.adb_over_network)
                        .setPositiveButton(android.R.string.yes, this)
                        .setNegativeButton(android.R.string.no, this)
                        .show();
                mAdbTcpDialog.setOnDismissListener(this);
            } else {
                Settings.Secure.putInt(getActivity().getContentResolver(),
                        Settings.Secure.ADB_PORT, -1);
                updateAdbOverNetwork();
            }
        } else if (preference == mClearAdbKeys) {
            if (mAdbKeysDialog != null) dismissDialogs();
            mAdbKeysDialog = new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.adb_keys_warning_message)
                        .setPositiveButton(android.R.string.ok, this)
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
        } else if (preference == mEnableTerminal) {
            final PackageManager pm = getActivity().getPackageManager();
            pm.setApplicationEnabledSetting(TERMINAL_APP_PACKAGE,
                    mEnableTerminal.isChecked() ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            : PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, 0);
        } else if (preference == mBugreportInPower) {
            Settings.Secure.putInt(getActivity().getContentResolver(),
                    Settings.Secure.BUGREPORT_IN_POWER_MENU,
                    mBugreportInPower.isChecked() ? 1 : 0);
        } else if (preference == mKeepScreenOn) {
            Settings.Global.putInt(getActivity().getContentResolver(),
                    Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                    mKeepScreenOn.isChecked() ?
                            (BatteryManager.BATTERY_PLUGGED_AC | BatteryManager.BATTERY_PLUGGED_USB) : 0);
        } else if (preference == mBtHciSnoopLog) {
            writeBtHciSnoopLogOptions();
        } else if (preference == mEnableOemUnlock) {
            Utils.setOemUnlockEnabled(getActivity(), mEnableOemUnlock.isChecked());
        } else if (preference == mAllowMockLocation) {
            Settings.Secure.putInt(getActivity().getContentResolver(),
                    Settings.Secure.ALLOW_MOCK_LOCATION,
                    mAllowMockLocation.isChecked() ? 1 : 0);
        } else if (preference == mDebugViewAttributes) {
            Settings.Global.putInt(getActivity().getContentResolver(),
                    Settings.Global.DEBUG_VIEW_ATTRIBUTES,
                    mDebugViewAttributes.isChecked() ? 1 : 0);
        } else if (preference == mQuickBoot) {
            Settings.System.putInt(getActivity().getContentResolver(), ENABLE_QUICKBOOT,
                    mQuickBoot.isChecked() ? 1 : 0);
        } else if (preference == mDebugAppPref) {
            startActivityForResult(new Intent(getActivity(), AppPicker.class), RESULT_DEBUG_APP);
        } else if (preference == mWaitForDebugger) {
            writeDebuggerOptions();
        } else if (preference == mVerifyAppsOverUsb) {
            writeVerifyAppsOverUsbOptions();
        } else if (preference == mStrictMode) {
            writeStrictModeVisualOptions();
        } else if (preference == mPointerLocation) {
            writePointerLocationOptions();
        } else if (preference == mShowTouches) {
            writeShowTouchesOptions();
        } else if (preference == mShowScreenUpdates) {
            writeShowUpdatesOption();
        } else if (preference == mDisableOverlays) {
            writeDisableOverlaysOption();
        } else if (preference == mShowCpuUsage) {
            writeCpuUsageOptions();
        } else if (preference == mImmediatelyDestroyActivities) {
            writeImmediatelyDestroyActivitiesOptions();
        } else if (preference == mShowAllANRs) {
            writeShowAllANRsOptions();
        } else if (preference == mForceHardwareUi) {
            writeHardwareUiOptions();
        } else if (preference == mForceMsaa) {
            writeMsaaOptions();
        } else if (preference == mShowHwScreenUpdates) {
            writeShowHwScreenUpdatesOptions();
        } else if (preference == mShowHwLayersUpdates) {
            writeShowHwLayersUpdatesOptions();
        } else if (preference == mDebugLayout) {
            writeDebugLayoutOptions();
        } else if (preference == mForceRtlLayout) {
            writeForceRtlOptions();
        } else if (preference == mWifiDisplayCertification) {
            writeWifiDisplayCertificationOptions();
        } else if (preference == mWifiVerboseLogging) {
            writeWifiVerboseLoggingOptions();
        } else if (preference == mWifiAggressiveHandover) {
            writeWifiAggressiveHandoverOptions();
        } else if (preference == mWifiAllowScansWithTraffic) {
            writeWifiAllowScansWithTrafficOptions();
        } else if (preference == mUseNuplayer) {
            writeUseNuplayerOptions();
        } else if (preference == mUSBAudio) {
            writeUSBAudioOptions();
        } else if (preference == mAdvancedReboot) {
            writeAdvancedRebootOptions();
        } else if (preference == mDevelopmentShortcut) {
            writeDevelopmentShortcutOptions();
        } else if (preference == mKillAppLongpressBack) {
            writeKillAppLongpressBackOptions();
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (HDCP_CHECKING_KEY.equals(preference.getKey())) {
            SystemProperties.set(HDCP_CHECKING_PROPERTY, newValue.toString());
            updateHdcpValues();
            pokeSystemProperties();
            return true;
        } else if (preference == mLogdSize) {
            writeLogdSizeOption(newValue);
            return true;
        } else if (preference == mWindowAnimationScale) {
            writeAnimationScaleOption(0, mWindowAnimationScale, newValue);
            return true;
        } else if (preference == mTransitionAnimationScale) {
            writeAnimationScaleOption(1, mTransitionAnimationScale, newValue);
            return true;
        } else if (preference == mAnimatorDurationScale) {
            writeAnimationScaleOption(2, mAnimatorDurationScale, newValue);
            return true;
        } else if (preference == mOverlayDisplayDevices) {
            writeOverlayDisplayDevicesOptions(newValue);
            return true;
        } else if (preference == mOpenGLTraces) {
            writeOpenGLTracesOptions(newValue);
            return true;
        } else if (preference == mTrackFrameTime) {
            writeTrackFrameTimeOptions(newValue);
            return true;
        } else if (preference == mDebugHwOverdraw) {
            writeDebugHwOverdrawOptions(newValue);
            return true;
        } else if (preference == mShowNonRectClip) {
            writeShowNonRectClipOptions(newValue);
            return true;
        } else if (preference == mAppProcessLimit) {
            writeAppProcessLimitOptions(newValue);
            return true;
        } else if (preference == mSimulateColorSpace) {
            writeSimulateColorSpace(newValue);
            return true;
        } else if (preference == mRootAccess) {
            if ("0".equals(SystemProperties.get(ROOT_ACCESS_PROPERTY, "0"))
                    && !"0".equals(newValue)) {
                mSelectedRootValue = newValue;
                mDialogClicked = false;
                if (mRootDialog != null) {
                    dismissDialogs();
                }
                mRootDialog = new AlertDialog.Builder(getActivity())
                        .setMessage(getResources().getString(R.string.root_access_warning_message))
                        .setTitle(R.string.root_access_warning_title)
                        .setPositiveButton(android.R.string.yes, this)
                        .setNegativeButton(android.R.string.no, this).show();
                mRootDialog.setOnDismissListener(this);
            } else {
                writeRootAccessOptions(newValue);
            }
            return true;
        }
        return false;
    }

    private void dismissDialogs() {
        if (mAdbDialog != null) {
            mAdbDialog.dismiss();
            mAdbDialog = null;
        }
        if (mAdbTcpDialog != null) {
            mAdbTcpDialog.dismiss();
            mAdbTcpDialog = null;
        }
        if (mAdbKeysDialog != null) {
            mAdbKeysDialog.dismiss();
            mAdbKeysDialog = null;
        }
        if (mEnableDialog != null) {
            mEnableDialog.dismiss();
            mEnableDialog = null;
        }
        if (mRootDialog != null) {
            mRootDialog.dismiss();
            mRootDialog = null;
        }
    }

    public void onClick(DialogInterface dialog, int which) {
        if (dialog == mAdbDialog) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                mDialogClicked = true;
                Settings.Global.putInt(getActivity().getContentResolver(),
                        Settings.Global.ADB_ENABLED, 1);
                mVerifyAppsOverUsb.setEnabled(true);
                updateVerifyAppsOverUsbOptions();
                updateBugreportOptions();
            }
        } else if (dialog == mAdbTcpDialog) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                Settings.Secure.putInt(getActivity().getContentResolver(),
                        Settings.Secure.ADB_PORT, 5555);
            }
        } else if (dialog == mAdbKeysDialog) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                try {
                    IBinder b = ServiceManager.getService(Context.USB_SERVICE);
                    IUsbManager service = IUsbManager.Stub.asInterface(b);
                    service.clearUsbDebuggingKeys();
                } catch (RemoteException e) {
                    Log.e(TAG, "Unable to clear adb keys", e);
                }
            }
        } else if (dialog == mEnableDialog) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                mDialogClicked = true;
                Settings.Global.putInt(getActivity().getContentResolver(),
                        Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);
                mLastEnabledState = true;
                setPrefsEnabledState(mLastEnabledState);

                // Make sure the development settings is visible in the main Settings menu
                // This is needed since we may have just turned off dev settings and want to
                // turn it on again
                getActivity().getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE).edit()
                    .putBoolean(PREF_SHOW, true)
                    .apply();
            }
        } else if (dialog == mRootDialog) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                writeRootAccessOptions(mSelectedRootValue);
            } else {
                // Reset the option
                writeRootAccessOptions("0");
            }
        }
    }

    public void onDismiss(DialogInterface dialog) {
        // Assuming that onClick gets called first
        if (dialog == mAdbDialog) {
            if (!mDialogClicked) {
                mEnableAdb.setChecked(false);
            }
            mAdbDialog = null;
        } else if (dialog == mAdbTcpDialog) {
            updateAdbOverNetwork();
            mAdbTcpDialog = null;
        } else if (dialog == mEnableDialog) {
            if (!mDialogClicked) {
                mSwitchBar.setChecked(false);
            }
            mEnableDialog = null;
        }
    }

    @Override
    public void onDestroy() {
        dismissDialogs();
        super.onDestroy();
    }

    void pokeSystemProperties() {
        if (!mDontPokeProperties) {
            //noinspection unchecked
            (new SystemPropPoker()).execute();
        }
    }

    static class SystemPropPoker extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            String[] services;
            try {
                services = ServiceManager.listServices();
            } catch (RemoteException e) {
                return null;
            }
            for (String service : services) {
                IBinder obj = ServiceManager.checkService(service);
                if (obj != null) {
                    Parcel data = Parcel.obtain();
                    try {
                        obj.transact(IBinder.SYSPROPS_TRANSACTION, data, null, 0);
                    } catch (RemoteException e) {
                    } catch (Exception e) {
                        Log.i(TAG, "Someone wrote a bad service '" + service
                                + "' that doesn't like to be poked: " + e);
                    }
                    data.recycle();
                }
            }
            return null;
        }
    }

    /**
     * For Search.
     */
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

                private boolean isShowingDeveloperOptions(Context context) {
                    return context.getSharedPreferences(DevelopmentSettings.PREF_FILE,
                            Context.MODE_PRIVATE).getBoolean(
                                    DevelopmentSettings.PREF_SHOW,
                                    android.os.Build.TYPE.equals("eng"));
                }

                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {

                    if (!isShowingDeveloperOptions(context)) {
                        return null;
                    }

                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.development_prefs;
                    return Arrays.asList(sir);
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    if (!isShowingDeveloperOptions(context)) {
                        return null;
                    }

                    final List<String> keys = new ArrayList<String>();
                    if (!showEnableOemUnlockPreference()) {
                        keys.add(ENABLE_OEM_UNLOCK);
                    }
                    return keys;
                }
            };
}
