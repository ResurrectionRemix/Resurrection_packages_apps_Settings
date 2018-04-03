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

package com.android.settings.development;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.AppOpsManager.PackageOps;
import android.app.Dialog;
import android.app.backup.IBackupManager;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothCodecStatus;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IShortcutService;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.hardware.usb.IUsbManager;
import android.hardware.usb.UsbManager;
import android.net.NetworkUtils;
import android.net.wifi.IWifiManager;
import android.net.wifi.WifiInfo;
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
import android.os.storage.IStorageManager;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.service.oemlock.OemLockManager;
import android.support.annotation.VisibleForTesting;
import android.support.v14.preference.SwitchPreference;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.IWindowManager;
import android.view.LayoutInflater;
import android.view.ThreadedRenderer;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.webkit.IWebViewUpdateService;
import android.webkit.WebViewFactory;
import android.widget.Switch;
import android.widget.Toast;

import com.android.internal.app.LocalePicker;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.AnimationScalePreference;
import com.android.settings.R;
import com.android.settings.RestrictedSettingsFragment;
import com.android.settings.Settings.AppOpsSummaryActivity;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.dashboard.DashboardFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.webview.WebViewAppPreferenceController;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.development.AbstractEnableAdbPreferenceController;
import com.android.settingslib.drawer.CategoryKey;

import lineageos.providers.LineageSettings;

import org.lineageos.internal.util.FileUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/*
 * Displays preferences for application developers.
 */
public class DevelopmentSettings extends RestrictedSettingsFragment
        implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener,
        OnPreferenceChangeListener,  OnPreferenceClickListener, SwitchBar.OnSwitchChangeListener, Indexable {
    private static final String TAG = "DevelopmentSettings";

    /**
     * Preference file were development settings prefs are stored.
     */
    public static final String PREF_FILE = "development";

    /**
     * Whether to show the development settings to the user.  Default is false.
     */
    public static final String PREF_SHOW = "show";

    private static final String ADB_TCPIP = "adb_over_network";
    private static final String CLEAR_ADB_KEYS = "clear_adb_keys";
    private static final String ENABLE_TERMINAL = "enable_terminal";
    private static final String KEEP_SCREEN_ON = "keep_screen_on";
    private static final String BT_HCI_SNOOP_LOG = "bt_hci_snoop_log";
    private static final String ENABLE_OEM_UNLOCK = "oem_unlock_enable";
    private static final String HDCP_CHECKING_KEY = "hdcp_checking";
    private static final String HDCP_CHECKING_PROPERTY = "persist.sys.hdcp_checking";
    private static final String LOCAL_BACKUP_PASSWORD = "local_backup_password";
    private static final String HARDWARE_UI_PROPERTY = "persist.sys.ui.hw";
    private static final String MSAA_PROPERTY = "debug.egl.force_msaa";
    private static final String OPENGL_TRACES_PROPERTY = "debug.egl.trace";
    private static final String TUNER_UI_KEY = "tuner_ui";
    private static final String COLOR_TEMPERATURE_PROPERTY = "persist.sys.debug.color_temp";

    private static final String DEBUG_APP_KEY = "debug_app";
    private static final String WAIT_FOR_DEBUGGER_KEY = "wait_for_debugger";
    private static final String MOCK_LOCATION_APP_KEY = "mock_location_app";
    private static final String DEBUG_VIEW_ATTRIBUTES = "debug_view_attributes";
    private static final String FORCE_ALLOW_ON_EXTERNAL_KEY = "force_allow_on_external";
    private static final String STRICT_MODE_KEY = "strict_mode";
    private static final String POINTER_LOCATION_KEY = "pointer_location";
    private static final String SHOW_TOUCHES_KEY = "show_touches";
    private static final String SHOW_SCREEN_UPDATES_KEY = "show_screen_updates";
    private static final String DISABLE_OVERLAYS_KEY = "disable_overlays";
    private static final String SIMULATE_COLOR_SPACE = "simulate_color_space";
    private static final String USB_AUDIO_KEY = "usb_audio";
    private static final String FORCE_HARDWARE_UI_KEY = "force_hw_ui";
    private static final String FORCE_MSAA_KEY = "force_msaa";
    private static final String TRACK_FRAME_TIME_KEY = "track_frame_time";
    private static final String SHOW_NON_RECTANGULAR_CLIP_KEY = "show_non_rect_clip";
    private static final String SHOW_HW_SCREEN_UPDATES_KEY = "show_hw_screen_udpates";
    private static final String SHOW_HW_LAYERS_UPDATES_KEY = "show_hw_layers_udpates";
    private static final String DEBUG_HW_OVERDRAW_KEY = "debug_hw_overdraw";
    private static final String DEBUG_HW_RENDERER_KEY = "debug_hw_renderer";
    private static final String DEBUG_LAYOUT_KEY = "debug_layout";
    private static final String FORCE_RTL_LAYOUT_KEY = "force_rtl_layout_all_locales";
    private static final String WINDOW_ANIMATION_SCALE_KEY = "window_animation_scale";
    private static final String TRANSITION_ANIMATION_SCALE_KEY = "transition_animation_scale";
    private static final String ANIMATOR_DURATION_SCALE_KEY = "animator_duration_scale";
    private static final String OVERLAY_DISPLAY_DEVICES_KEY = "overlay_display_devices";
    private static final String DEBUG_DEBUGGING_CATEGORY_KEY = "debug_debugging_category";
    private static final String SELECT_LOGD_SIZE_KEY = "select_logd_size";
    private static final String SELECT_LOGD_SIZE_PROPERTY = "persist.logd.size";
    private static final String SELECT_LOGD_TAG_PROPERTY = "persist.log.tag";
    // Tricky, isLoggable only checks for first character, assumes silence
    private static final String SELECT_LOGD_TAG_SILENCE = "Settings";
    private static final String SELECT_LOGD_SNET_TAG_PROPERTY = "persist.log.tag.snet_event_log";
    private static final String SELECT_LOGD_RUNTIME_SNET_TAG_PROPERTY = "log.tag.snet_event_log";
    private static final String SELECT_LOGD_DEFAULT_SIZE_PROPERTY = "ro.logd.size";
    private static final String SELECT_LOGD_DEFAULT_SIZE_VALUE = "262144";
    private static final String SELECT_LOGD_SVELTE_DEFAULT_SIZE_VALUE = "65536";
    // 32768 is merely a menu marker, 64K is our lowest log buffer size we replace it with.
    private static final String SELECT_LOGD_MINIMUM_SIZE_VALUE = "65536";
    private static final String SELECT_LOGD_OFF_SIZE_MARKER_VALUE = "32768";
    private static final String SELECT_LOGPERSIST_KEY = "select_logpersist";
    private static final String SELECT_LOGPERSIST_PROPERTY = "persist.logd.logpersistd";
    private static final String ACTUAL_LOGPERSIST_PROPERTY = "logd.logpersistd";
    private static final String SELECT_LOGPERSIST_PROPERTY_SERVICE = "logcatd";
    private static final String SELECT_LOGPERSIST_PROPERTY_CLEAR = "clear";
    private static final String SELECT_LOGPERSIST_PROPERTY_STOP = "stop";
    private static final String SELECT_LOGPERSIST_PROPERTY_BUFFER =
            "persist.logd.logpersistd.buffer";
    private static final String ACTUAL_LOGPERSIST_PROPERTY_BUFFER = "logd.logpersistd.buffer";
    private static final String ACTUAL_LOGPERSIST_PROPERTY_ENABLE = "logd.logpersistd.enable";

    private static final String WIFI_DISPLAY_CERTIFICATION_KEY = "wifi_display_certification";
    private static final String WIFI_VERBOSE_LOGGING_KEY = "wifi_verbose_logging";
    private static final String WIFI_AGGRESSIVE_HANDOVER_KEY = "wifi_aggressive_handover";
    private static final String WIFI_ALLOW_SCAN_WITH_TRAFFIC_KEY = "wifi_allow_scan_with_traffic";
    private static final String USB_CONFIGURATION_KEY = "select_usb_configuration";
    private static final String MOBILE_DATA_ALWAYS_ON = "mobile_data_always_on";
    private static final String TETHERING_HARDWARE_OFFLOAD = "tethering_hardware_offload";
    private static final String KEY_COLOR_MODE = "picture_color_mode";
    private static final String FORCE_RESIZABLE_KEY = "force_resizable_activities";
    private static final String COLOR_TEMPERATURE_KEY = "color_temperature";

    private static final String BLUETOOTH_SHOW_DEVICES_WITHOUT_NAMES_KEY =
            "bluetooth_show_devices_without_names";
    private static final String BLUETOOTH_SHOW_DEVICES_WITHOUT_NAMES_PROPERTY =
            "persist.bluetooth.showdeviceswithoutnames";
    private static final String BLUETOOTH_DISABLE_ABSOLUTE_VOLUME_KEY =
            "bluetooth_disable_absolute_volume";
    private static final String BLUETOOTH_DISABLE_ABSOLUTE_VOLUME_PROPERTY =
            "persist.bluetooth.disableabsvol";
    private static final String BLUETOOTH_AVRCP_VERSION_PROPERTY =
                                    "persist.bluetooth.avrcpversion";
    private static final String BLUETOOTH_ENABLE_INBAND_RINGING_PROPERTY =
                                    "persist.bluetooth.enableinbandringing";
    private static final String BLUETOOTH_BTSNOOP_ENABLE_PROPERTY =
                                    "persist.bluetooth.btsnoopenable";

    private static final String BLUETOOTH_ENABLE_INBAND_RINGING_KEY = "bluetooth_enable_inband_ringing";
    private static final String BLUETOOTH_SELECT_AVRCP_VERSION_KEY = "bluetooth_select_avrcp_version";
    private static final String BLUETOOTH_SELECT_A2DP_CODEC_KEY = "bluetooth_select_a2dp_codec";
    private static final String BLUETOOTH_SELECT_A2DP_SAMPLE_RATE_KEY = "bluetooth_select_a2dp_sample_rate";
    private static final String BLUETOOTH_SELECT_A2DP_BITS_PER_SAMPLE_KEY = "bluetooth_select_a2dp_bits_per_sample";
    private static final String BLUETOOTH_SELECT_A2DP_CHANNEL_MODE_KEY = "bluetooth_select_a2dp_channel_mode";
    private static final String BLUETOOTH_SELECT_A2DP_LDAC_PLAYBACK_QUALITY_KEY = "bluetooth_select_a2dp_ldac_playback_quality";

    private static final String INACTIVE_APPS_KEY = "inactive_apps";

    private static final String ROOT_ACCESS_KEY = "root_access";
    private static final String ROOT_ACCESS_PROPERTY = "persist.sys.root_access";

    private static final String ROOT_APPOPS_KEY = "root_appops";

    private static final String IMMEDIATELY_DESTROY_ACTIVITIES_KEY
            = "immediately_destroy_activities";
    private static final String APP_PROCESS_LIMIT_KEY = "app_process_limit";

    private static final String BACKGROUND_CHECK_KEY = "background_check";

    private static final String SHOW_ALL_ANRS_KEY = "show_all_anrs";

    private static final String SHOW_NOTIFICATION_CHANNEL_WARNINGS_KEY = "show_notification_channel_warnings";

    private static final String TERMINAL_APP_PACKAGE = "com.android.terminal";

    private static final String KEY_CONVERT_FBE = "convert_to_file_encryption";

    private static final String OTA_DISABLE_AUTOMATIC_UPDATE_KEY = "ota_disable_automatic_update";

    private static final String FORCE_AUTHORIZE_SUBSTRATUM_PACKAGES = "force_authorize_substratum_packages";
    
    private static final String DEVELOPMENT_TOOLS = "development_tools";

    private static final int RESULT_DEBUG_APP = 1000;
    private static final int RESULT_MOCK_LOCATION_APP = 1001;

    private static final String FLASH_LOCKED_PROP = "ro.boot.flash.locked";

    private static final String SHORTCUT_MANAGER_RESET_KEY = "reset_shortcut_manager_throttling";

    private static final int REQUEST_CODE_ENABLE_OEM_UNLOCK = 0;

    private static final int[] MOCK_LOCATION_APP_OPS = new int[]{AppOpsManager.OP_MOCK_LOCATION};

    private IWindowManager mWindowManager;
    private IBackupManager mBackupManager;
    private IWebViewUpdateService mWebViewUpdateService;
    private UserManager mUm;
    private WifiManager mWifiManager;
    private OemLockManager mOemLockManager;
    private TelephonyManager mTelephonyManager;

    private SwitchBar mSwitchBar;

    private boolean mHaveDebugSettings;
    private boolean mDontPokeProperties;
    private EnableAdbPreferenceController mEnableAdbController;
    private SwitchPreference mAdbOverNetwork;

    private SwitchPreference mForceAuthorizeSubstratumPackages;

    private Preference mClearAdbKeys;
    private SwitchPreference mEnableTerminal;
    private RestrictedSwitchPreference mKeepScreenOn;
    private SwitchPreference mBtHciSnoopLog;
    private RestrictedSwitchPreference mEnableOemUnlock;
    private SwitchPreference mDebugViewAttributes;
    private SwitchPreference mForceAllowOnExternal;

    private Preference mPassword;
    private String mDebugApp;
    private Preference mDebugAppPref;

    private String mMockLocationApp;
    private Preference mMockLocationAppPref;

    private SwitchPreference mWaitForDebugger;
    private VerifyAppsOverUsbPreferenceController mVerifyAppsOverUsbController;
    private SwitchPreference mWifiDisplayCertification;
    private SwitchPreference mWifiVerboseLogging;
    private SwitchPreference mWifiAggressiveHandover;
    private SwitchPreference mMobileDataAlwaysOn;
    private SwitchPreference mTetheringHardwareOffload;
    private SwitchPreference mBluetoothShowDevicesWithoutNames;
    private SwitchPreference mBluetoothDisableAbsVolume;
    private SwitchPreference mBluetoothEnableInbandRinging;

    private BluetoothA2dp mBluetoothA2dp;
    private final Object mBluetoothA2dpLock = new Object();
    private ListPreference mBluetoothSelectAvrcpVersion;
    private ListPreference mBluetoothSelectA2dpCodec;
    private ListPreference mBluetoothSelectA2dpSampleRate;
    private ListPreference mBluetoothSelectA2dpBitsPerSample;
    private ListPreference mBluetoothSelectA2dpChannelMode;
    private ListPreference mBluetoothSelectA2dpLdacPlaybackQuality;

    private SwitchPreference mOtaDisableAutomaticUpdate;
    private SwitchPreference mWifiAllowScansWithTraffic;
    private SwitchPreference mStrictMode;
    private SwitchPreference mPointerLocation;
    private SwitchPreference mShowTouches;
    private SwitchPreference mShowScreenUpdates;
    private SwitchPreference mDisableOverlays;
    private SwitchPreference mForceHardwareUi;
    private SwitchPreference mForceMsaa;
    private SwitchPreference mShowHwScreenUpdates;
    private SwitchPreference mShowHwLayersUpdates;
    private SwitchPreference mDebugLayout;
    private SwitchPreference mForceRtlLayout;
    private ListPreference mDebugHwOverdraw;
    private ListPreference mDebugHwRenderer;
    private ListPreference mLogdSize;
    private ListPreference mLogpersist;
    private ListPreference mUsbConfiguration;
    private ListPreference mTrackFrameTime;
    private ListPreference mShowNonRectClip;
    private AnimationScalePreference mWindowAnimationScale;
    private AnimationScalePreference mTransitionAnimationScale;
    private AnimationScalePreference mAnimatorDurationScale;
    private ListPreference mOverlayDisplayDevices;

    private WebViewAppPreferenceController mWebViewAppPrefController;

    private ListPreference mSimulateColorSpace;

    private SwitchPreference mUSBAudio;
    private SwitchPreference mImmediatelyDestroyActivities;

    private ListPreference mAppProcessLimit;

    private SwitchPreference mShowAllANRs;

    private SwitchPreference mShowNotificationChannelWarnings;

    private ColorModePreference mColorModePreference;

    private Preference mRootAppops;

    private SwitchPreference mForceResizable;

    private SwitchPreference mColorTemperaturePreference;

    private PreferenceScreen mDevelopmentTools;
    private ListPreference mRootAccess;
    private Object mSelectedRootValue;

    private final ArrayList<Preference> mAllPrefs = new ArrayList<>();

    private final ArrayList<SwitchPreference> mResetSwitchPrefs = new ArrayList<>();

    private final HashSet<Preference> mDisabledPrefs = new HashSet<>();
    // To track whether a confirmation dialog was clicked.
    private boolean mDialogClicked;
    private Dialog mEnableDialog;

    private Dialog mAdbTcpDialog;
    private Dialog mAdbKeysDialog;
    private boolean mUnavailable;
    private Dialog mRootDialog;

    private boolean mLogpersistCleared;
    private Dialog mLogpersistClearDialog;
    private DashboardFeatureProvider mDashboardFeatureProvider;
    private DevelopmentSettingsEnabler mSettingsEnabler;
    private DevelopmentSwitchBarController mSwitchBarController;
    private BugReportPreferenceController mBugReportController;
    private BugReportInPowerPreferenceController mBugReportInPowerController;
    private TelephonyMonitorPreferenceController mTelephonyMonitorController;
    private CameraLaserSensorPreferenceController mCameraLaserSensorController;

    private BroadcastReceiver mEnableAdbReceiver;

    public DevelopmentSettings() {
        super(UserManager.DISALLOW_DEBUGGING_FEATURES);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.DEVELOPMENT;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mSettingsEnabler = new DevelopmentSettingsEnabler(context, getLifecycle());
        mDashboardFeatureProvider = FeatureFactory.getFactory(context)
                .getDashboardFeatureProvider(context);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
        mBackupManager = IBackupManager.Stub.asInterface(
                ServiceManager.getService(Context.BACKUP_SERVICE));
        mWebViewUpdateService = WebViewFactory.getUpdateService();
        mOemLockManager = (OemLockManager) getSystemService(Context.OEM_LOCK_SERVICE);
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        mUm = (UserManager) getSystemService(Context.USER_SERVICE);

        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        mBugReportController = new BugReportPreferenceController(getActivity());
        mBugReportInPowerController = new BugReportInPowerPreferenceController(getActivity());
        mTelephonyMonitorController = new TelephonyMonitorPreferenceController(getActivity());
        mWebViewAppPrefController = new WebViewAppPreferenceController(getActivity());
        mVerifyAppsOverUsbController = new VerifyAppsOverUsbPreferenceController(getActivity());
        mCameraLaserSensorController = new CameraLaserSensorPreferenceController(getActivity());

        setIfOnlyAvailableForAdmins(true);
        if (isUiRestricted() || !Utils.isDeviceProvisioned(getActivity())) {
            // Block access to developer options if the user is not the owner, if user policy
            // restricts it, or if the device has not been provisioned
            mUnavailable = true;
            addPreferencesFromResource(R.xml.placeholder_prefs);
            return;
        }

        addPreferencesFromResource(R.xml.development_prefs);

        final PreferenceGroup debugDebuggingCategory = (PreferenceGroup)
                findPreference(DEBUG_DEBUGGING_CATEGORY_KEY);
        mEnableAdbController = new EnableAdbPreferenceController(getActivity());
        mAdbOverNetwork = findAndInitSwitchPref(ADB_TCPIP);
        mClearAdbKeys = findPreference(CLEAR_ADB_KEYS);
        if (!SystemProperties.getBoolean("ro.adb.secure", false)) {
            if (debugDebuggingCategory != null) {
                debugDebuggingCategory.removePreference(mClearAdbKeys);
            }
        }
        mAllPrefs.add(mClearAdbKeys);
        mEnableTerminal = findAndInitSwitchPref(ENABLE_TERMINAL);
        if (!isPackageInstalled(getActivity(), TERMINAL_APP_PACKAGE)) {
            debugDebuggingCategory.removePreference(mEnableTerminal);
            mEnableTerminal = null;
        }

        mBugReportController.displayPreference(getPreferenceScreen());
        mBugReportInPowerController.displayPreference(getPreferenceScreen());
        mTelephonyMonitorController.displayPreference(getPreferenceScreen());
        mWebViewAppPrefController.displayPreference(getPreferenceScreen());
        mEnableAdbController.displayPreference(getPreferenceScreen());

        mCameraLaserSensorController.displayPreference(getPreferenceScreen());

        mKeepScreenOn = (RestrictedSwitchPreference) findAndInitSwitchPref(KEEP_SCREEN_ON);
        mBtHciSnoopLog = findAndInitSwitchPref(BT_HCI_SNOOP_LOG);
        mEnableOemUnlock = (RestrictedSwitchPreference) findAndInitSwitchPref(ENABLE_OEM_UNLOCK);
        if (!showEnableOemUnlockPreference(getActivity())) {
            removePreference(mEnableOemUnlock);
            mEnableOemUnlock = null;
        }

        mDebugViewAttributes = findAndInitSwitchPref(DEBUG_VIEW_ATTRIBUTES);
        mForceAllowOnExternal = findAndInitSwitchPref(FORCE_ALLOW_ON_EXTERNAL_KEY);
        mPassword = findPreference(LOCAL_BACKUP_PASSWORD);
        mAllPrefs.add(mPassword);
        mForceAuthorizeSubstratumPackages = findAndInitSwitchPref(FORCE_AUTHORIZE_SUBSTRATUM_PACKAGES);

        if (!mUm.isAdminUser()) {
            disableForUser(mClearAdbKeys);
            disableForUser(mEnableTerminal);
            disableForUser(mPassword);
            disableForUser(mForceAuthorizeSubstratumPackages);
        }

        mDebugAppPref = findPreference(DEBUG_APP_KEY);
        mAllPrefs.add(mDebugAppPref);
        mWaitForDebugger = findAndInitSwitchPref(WAIT_FOR_DEBUGGER_KEY);

        mMockLocationAppPref = findPreference(MOCK_LOCATION_APP_KEY);
        mAllPrefs.add(mMockLocationAppPref);

        mVerifyAppsOverUsbController.displayPreference(getPreferenceScreen());

        mStrictMode = findAndInitSwitchPref(STRICT_MODE_KEY);
        mPointerLocation = findAndInitSwitchPref(POINTER_LOCATION_KEY);
        mShowTouches = findAndInitSwitchPref(SHOW_TOUCHES_KEY);
        mShowScreenUpdates = findAndInitSwitchPref(SHOW_SCREEN_UPDATES_KEY);
        mDisableOverlays = findAndInitSwitchPref(DISABLE_OVERLAYS_KEY);
        mForceHardwareUi = findAndInitSwitchPref(FORCE_HARDWARE_UI_KEY);
        mForceMsaa = findAndInitSwitchPref(FORCE_MSAA_KEY);
        mTrackFrameTime = addListPreference(TRACK_FRAME_TIME_KEY);
        mShowNonRectClip = addListPreference(SHOW_NON_RECTANGULAR_CLIP_KEY);
        mShowHwScreenUpdates = findAndInitSwitchPref(SHOW_HW_SCREEN_UPDATES_KEY);
        mShowHwLayersUpdates = findAndInitSwitchPref(SHOW_HW_LAYERS_UPDATES_KEY);
        mDebugLayout = findAndInitSwitchPref(DEBUG_LAYOUT_KEY);
        mForceRtlLayout = findAndInitSwitchPref(FORCE_RTL_LAYOUT_KEY);
        mDebugHwOverdraw = addListPreference(DEBUG_HW_OVERDRAW_KEY);
        mDebugHwRenderer = addListPreference(DEBUG_HW_RENDERER_KEY);
        mWifiDisplayCertification = findAndInitSwitchPref(WIFI_DISPLAY_CERTIFICATION_KEY);
        mWifiVerboseLogging = findAndInitSwitchPref(WIFI_VERBOSE_LOGGING_KEY);
        mWifiAggressiveHandover = findAndInitSwitchPref(WIFI_AGGRESSIVE_HANDOVER_KEY);
        mWifiAllowScansWithTraffic = findAndInitSwitchPref(WIFI_ALLOW_SCAN_WITH_TRAFFIC_KEY);
        mMobileDataAlwaysOn = findAndInitSwitchPref(MOBILE_DATA_ALWAYS_ON);
        mTetheringHardwareOffload = findAndInitSwitchPref(TETHERING_HARDWARE_OFFLOAD);
        mLogdSize = addListPreference(SELECT_LOGD_SIZE_KEY);
        if ("1".equals(SystemProperties.get("ro.debuggable", "0"))) {
            mLogpersist = addListPreference(SELECT_LOGPERSIST_KEY);
        } else {
            mLogpersist = (ListPreference) findPreference(SELECT_LOGPERSIST_KEY);
            if (mLogpersist != null) {
                mLogpersist.setEnabled(false);
                if (debugDebuggingCategory != null) {
                    debugDebuggingCategory.removePreference(mLogpersist);
                }
            }
            mLogpersist = null;
        }
        mUsbConfiguration = addListPreference(USB_CONFIGURATION_KEY);
        mBluetoothShowDevicesWithoutNames =
                findAndInitSwitchPref(BLUETOOTH_SHOW_DEVICES_WITHOUT_NAMES_KEY);
        mBluetoothDisableAbsVolume = findAndInitSwitchPref(BLUETOOTH_DISABLE_ABSOLUTE_VOLUME_KEY);
        mBluetoothEnableInbandRinging = findAndInitSwitchPref(BLUETOOTH_ENABLE_INBAND_RINGING_KEY);
        if (!BluetoothHeadset.isInbandRingingSupported(getContext())) {
            removePreference(mBluetoothEnableInbandRinging);
            mBluetoothEnableInbandRinging = null;
        }

        mBluetoothSelectAvrcpVersion = addListPreference(BLUETOOTH_SELECT_AVRCP_VERSION_KEY);
        mBluetoothSelectA2dpCodec = addListPreference(BLUETOOTH_SELECT_A2DP_CODEC_KEY);
        mBluetoothSelectA2dpSampleRate = addListPreference(BLUETOOTH_SELECT_A2DP_SAMPLE_RATE_KEY);
        mBluetoothSelectA2dpBitsPerSample = addListPreference(BLUETOOTH_SELECT_A2DP_BITS_PER_SAMPLE_KEY);
        mBluetoothSelectA2dpChannelMode = addListPreference(BLUETOOTH_SELECT_A2DP_CHANNEL_MODE_KEY);
        mBluetoothSelectA2dpLdacPlaybackQuality = addListPreference(BLUETOOTH_SELECT_A2DP_LDAC_PLAYBACK_QUALITY_KEY);
        initBluetoothConfigurationValues();

        mWindowAnimationScale = findAndInitAnimationScalePreference(WINDOW_ANIMATION_SCALE_KEY);
        mTransitionAnimationScale = findAndInitAnimationScalePreference(TRANSITION_ANIMATION_SCALE_KEY);
        mAnimatorDurationScale = findAndInitAnimationScalePreference(ANIMATOR_DURATION_SCALE_KEY);
        mOverlayDisplayDevices = addListPreference(OVERLAY_DISPLAY_DEVICES_KEY);
        mSimulateColorSpace = addListPreference(SIMULATE_COLOR_SPACE);
        mUSBAudio = findAndInitSwitchPref(USB_AUDIO_KEY);
        mForceResizable = findAndInitSwitchPref(FORCE_RESIZABLE_KEY);

        mImmediatelyDestroyActivities = (SwitchPreference) findPreference(
                IMMEDIATELY_DESTROY_ACTIVITIES_KEY);
        mAllPrefs.add(mImmediatelyDestroyActivities);
        mResetSwitchPrefs.add(mImmediatelyDestroyActivities);

        mAppProcessLimit = addListPreference(APP_PROCESS_LIMIT_KEY);

        mShowAllANRs = (SwitchPreference) findPreference(
                SHOW_ALL_ANRS_KEY);
        mAllPrefs.add(mShowAllANRs);
        mResetSwitchPrefs.add(mShowAllANRs);

        mShowNotificationChannelWarnings = (SwitchPreference) findPreference(
                SHOW_NOTIFICATION_CHANNEL_WARNINGS_KEY);
        mAllPrefs.add(mShowNotificationChannelWarnings);
        mResetSwitchPrefs.add(mShowNotificationChannelWarnings);

        Preference hdcpChecking = findPreference(HDCP_CHECKING_KEY);
        if (hdcpChecking != null) {
            mAllPrefs.add(hdcpChecking);
            removePreferenceForProduction(hdcpChecking);
        }

        Preference convertFbePreference = findPreference(KEY_CONVERT_FBE);

        try {
            IBinder service = ServiceManager.getService("mount");
            IStorageManager storageManager = IStorageManager.Stub.asInterface(service);
            if (!storageManager.isConvertibleToFBE()) {
                removePreference(KEY_CONVERT_FBE);
            } else if ("file".equals(SystemProperties.get("ro.crypto.type", "none"))) {
                convertFbePreference.setEnabled(false);
                convertFbePreference.setSummary(getResources()
                        .getString(R.string.convert_to_file_encryption_done));
            }
        } catch (RemoteException e) {
            removePreference(KEY_CONVERT_FBE);
        }

        mOtaDisableAutomaticUpdate = findAndInitSwitchPref(OTA_DISABLE_AUTOMATIC_UPDATE_KEY);
        if (!SystemProperties.getBoolean("ro.build.ab_update", false)) {
            removePreference(mOtaDisableAutomaticUpdate);
            mOtaDisableAutomaticUpdate = null;
        }

        mColorModePreference = (ColorModePreference) findPreference(KEY_COLOR_MODE);
        mColorModePreference.updateCurrentAndSupported();
        if (mColorModePreference.getColorModeCount() < 2 ||
                getContext().getResources().getConfiguration().isScreenWideColorGamut()) {
            removePreference(KEY_COLOR_MODE);
            mColorModePreference = null;
        }

        mColorTemperaturePreference = (SwitchPreference) findPreference(COLOR_TEMPERATURE_KEY);
        if (getResources().getBoolean(R.bool.config_enableColorTemperature)) {
            mAllPrefs.add(mColorTemperaturePreference);
            mResetSwitchPrefs.add(mColorTemperaturePreference);
        } else {
            removePreference(COLOR_TEMPERATURE_KEY);
            mColorTemperaturePreference = null;
        }

        mDevelopmentTools = (PreferenceScreen) findPreference(DEVELOPMENT_TOOLS);
        mAllPrefs.add(mDevelopmentTools);

        mRootAccess = (ListPreference) findPreference(ROOT_ACCESS_KEY);
        mRootAccess.setOnPreferenceChangeListener(this);

        mRootAppops = (Preference) findPreference(ROOT_APPOPS_KEY);
        mRootAppops.setOnPreferenceClickListener(this);

        if (!removeRootOptionsIfRequired()) {
            if (FileUtils.fileExists("/system/xbin/su")) {
                mRootAccess.setEntries(R.array.root_access_entries);
                mRootAccess.setEntryValues(R.array.root_access_values);
            } else {
                mRootAccess.setEntries(R.array.root_access_entries_adb);
                mRootAccess.setEntryValues(R.array.root_access_values_adb);
            }
            mAllPrefs.add(mRootAccess);
            mAllPrefs.add(mRootAppops);
        }
        addDashboardCategoryPreferences();
    }

    @VisibleForTesting
    void addDashboardCategoryPreferences() {
        final PreferenceScreen screen = getPreferenceScreen();
        final List<Preference> tilePrefs = mDashboardFeatureProvider.getPreferencesForCategory(
                getActivity(), getPrefContext(), getMetricsCategory(),
                CategoryKey.CATEGORY_SYSTEM_DEVELOPMENT);
        if (tilePrefs != null) {
            for (Preference preference : tilePrefs) {
                screen.addPreference(preference);
            }
        }
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

    private AnimationScalePreference findAndInitAnimationScalePreference(String key) {
        AnimationScalePreference pref = (AnimationScalePreference) findPreference(key);
        pref.setOnPreferenceChangeListener(this);
        pref.setOnPreferenceClickListener(this);
        mAllPrefs.add(pref);
        return pref;
    }

    private SwitchPreference findAndInitSwitchPref(String key) {
        SwitchPreference pref = (SwitchPreference) findPreference(key);
        if (pref == null) {
            throw new IllegalArgumentException("Cannot find preference with key = " + key);
        }
        mAllPrefs.add(pref);
        mResetSwitchPrefs.add(pref);
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

        mSwitchBar = ((SettingsActivity) getActivity()).getSwitchBar();
        mSwitchBarController = new DevelopmentSwitchBarController(
                this /* DevelopmentSettings */, mSwitchBar, !mUnavailable,  getLifecycle());
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
        mResetSwitchPrefs.remove(preference);
    }

    private void setPrefsEnabledState(boolean enabled) {
        for (int i = 0; i < mAllPrefs.size(); i++) {
            Preference pref = mAllPrefs.get(i);
            pref.setEnabled(enabled && !mDisabledPrefs.contains(pref));
        }
        mEnableAdbController.enablePreference(enabled);
        mBugReportInPowerController.enablePreference(enabled);
        mTelephonyMonitorController.enablePreference(enabled);
        mWebViewAppPrefController.enablePreference(enabled);
        mCameraLaserSensorController.enablePreference(enabled);
        updateAllOptions();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mUnavailable) {
            // Show error message
            if (!isUiRestrictedByOnlyAdmin()) {
                getEmptyTextView().setText(R.string.development_settings_not_available);
            }
            getPreferenceScreen().removeAll();
            return;
        }

        // A DeviceAdmin has specified a maximum time until the device
        // will lock...  in this case we can't allow the user to turn
        // on "stay awake when plugged in" because that would defeat the
        // restriction.
        final EnforcedAdmin admin = RestrictedLockUtils.checkIfMaximumTimeToLockIsSet(
                getActivity());
        mKeepScreenOn.setDisabledByAdmin(admin);
        if (admin == null) {
            mDisabledPrefs.remove(mKeepScreenOn);
        } else {
            mDisabledPrefs.add(mKeepScreenOn);
        }

        final boolean lastEnabledState = mSettingsEnabler.getLastEnabledState();
        mSwitchBar.setChecked(lastEnabledState);
        setPrefsEnabledState(lastEnabledState);

        if (mHaveDebugSettings && !lastEnabledState) {
            // Overall debugging is disabled, but there are some debug
            // settings that are enabled.  This is an invalid state.  Switch
            // to debug settings being enabled, so the user knows there is
            // stuff enabled and can turn it all off if they want.
            mSettingsEnabler.enableDevelopmentSettings();
            mSwitchBar.setChecked(lastEnabledState);
            setPrefsEnabledState(lastEnabledState);
        }
        mSwitchBar.show();

        if (mColorModePreference != null) {
            mColorModePreference.startListening();
            mColorModePreference.updateCurrentAndSupported();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mColorModePreference != null) {
            mColorModePreference.stopListening();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_STATE);
        if (getActivity().registerReceiver(mUsbReceiver, filter) == null) {
            updateUsbConfigurationValues();
        }

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            adapter.getProfileProxy(getActivity(),
                                    mBluetoothA2dpServiceListener,
                                    BluetoothProfile.A2DP);
        }
        filter = new IntentFilter();
        filter.addAction(BluetoothA2dp.ACTION_CODEC_CONFIG_CHANGED);
        if (getActivity().registerReceiver(mBluetoothA2dpReceiver, filter) == null) {
            updateBluetoothA2dpConfigurationValues();
        }

        mEnableAdbReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mVerifyAppsOverUsbController.updatePreference();
                updateBugreportOptions();
            }
        };
        LocalBroadcastManager.getInstance(getContext())
                .registerReceiver(mEnableAdbReceiver, new IntentFilter(
                        AbstractEnableAdbPreferenceController.ACTION_ENABLE_ADB_STATE_CHANGED));

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mUnavailable) {
            return;
        }
        getActivity().unregisterReceiver(mUsbReceiver);
        getActivity().unregisterReceiver(mBluetoothA2dpReceiver);
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            adapter.closeProfileProxy(BluetoothProfile.A2DP, mBluetoothA2dp);
            mBluetoothA2dp = null;
        }

        if (mEnableAdbReceiver != null) {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mEnableAdbReceiver);
            mEnableAdbReceiver = null;
        }
    }

    void updateSwitchPreference(SwitchPreference switchPreference, boolean value) {
        switchPreference.setChecked(value);
        mHaveDebugSettings |= value;
    }

    private void updateAllOptions() {
        final Context context = getActivity();
        final ContentResolver cr = context.getContentResolver();
        mHaveDebugSettings = false;
        final Preference enableAdb = findPreference(mEnableAdbController.getPreferenceKey());
        mEnableAdbController.updateState(enableAdb);
        mHaveDebugSettings |= mEnableAdbController.haveDebugSettings();
        if (mEnableTerminal != null) {
            updateSwitchPreference(mEnableTerminal,
                    context.getPackageManager().getApplicationEnabledSetting(TERMINAL_APP_PACKAGE)
                            == PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
        }
        mHaveDebugSettings |= mBugReportInPowerController.updatePreference();
        mHaveDebugSettings |= mTelephonyMonitorController.updatePreference();
        mHaveDebugSettings |= mCameraLaserSensorController.updatePreference();
        updateSwitchPreference(mKeepScreenOn, Settings.Global.getInt(cr,
                Settings.Global.STAY_ON_WHILE_PLUGGED_IN, 0) != 0);
        updateSwitchPreference(mBtHciSnoopLog, SystemProperties.getBoolean(
                BLUETOOTH_BTSNOOP_ENABLE_PROPERTY, false));
        updateSwitchPreference(mDebugViewAttributes, Settings.Global.getInt(cr,
                Settings.Global.DEBUG_VIEW_ATTRIBUTES, 0) != 0);
        updateSwitchPreference(mForceAllowOnExternal, Settings.Global.getInt(cr,
                Settings.Global.FORCE_ALLOW_ON_EXTERNAL, 0) != 0);
        updateHdcpValues();
        updatePasswordSummary();
        updateDebuggerOptions();
        updateMockLocation();
        updateStrictModeVisualOptions();
        updatePointerLocationOptions();
        updateShowTouchesOptions();
        updateFlingerOptions();
        updateHardwareUiOptions();
        updateMsaaOptions();
        updateTrackFrameTimeOptions();
        updateShowNonRectClipOptions();
        updateShowHwScreenUpdatesOptions();
        updateShowHwLayersUpdatesOptions();
        updateDebugHwOverdrawOptions();
        updateDebugHwRendererOptions();
        updateDebugLayoutOptions();
        updateAnimationScaleOptions();
        updateOverlayDisplayDevicesOptions();
        updateImmediatelyDestroyActivitiesOptions();
        updateAppProcessLimitOptions();
        updateShowAllANRsOptions();
        updateShowNotificationChannelWarningsOptions();
        mVerifyAppsOverUsbController.updatePreference();
        if (mOtaDisableAutomaticUpdate != null) {
            updateOtaDisableAutomaticUpdateOptions();
        }
        updateBugreportOptions();
        updateForceRtlOptions();
        updateLogdSizeValues();
        updateLogpersistValues();
        updateWifiDisplayCertificationOptions();
        updateWifiVerboseLoggingOptions();
        updateWifiAggressiveHandoverOptions();
        updateWifiAllowScansWithTrafficOptions();
        updateMobileDataAlwaysOnOptions();
        updateTetheringHardwareOffloadOptions();
        updateSimulateColorSpace();
        updateUSBAudioOptions();
        updateForceResizableOptions();
        Preference webViewAppPref = findPreference(mWebViewAppPrefController.getPreferenceKey());
        mWebViewAppPrefController.updateState(webViewAppPref);
        updateOemUnlockOptions();
        if (mColorTemperaturePreference != null) {
            updateColorTemperature();
        }
        updateBluetoothShowDevicesWithoutUserFriendlyNameOptions();
        updateBluetoothDisableAbsVolumeOptions();
        updateBluetoothEnableInbandRingingOptions();
        updateBluetoothA2dpConfigurationValues();
        updateAdbOverNetwork();
        updateRootAccessOptions();
        updateForceAuthorizeSubstratumPackagesOptions();

    }

    private void updateAdbOverNetwork() {
        int port = LineageSettings.Secure.getInt(getActivity().getContentResolver(),
                LineageSettings.Secure.ADB_PORT, 0);
        boolean enabled = port > 0;

        updateSwitchPreference(mAdbOverNetwork, enabled);

        WifiInfo wifiInfo = null;

        if (enabled) {
            IWifiManager wifiManager = IWifiManager.Stub.asInterface(
                    ServiceManager.getService(Context.WIFI_SERVICE));
            try {
                wifiInfo = wifiManager.getConnectionInfo(getActivity().getOpPackageName());
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
    
    private void writeForceAuthorizeSubstratumPackagesOptions() {
        Settings.Secure.putInt(getActivity().getContentResolver(),
                Settings.Secure.FORCE_AUTHORIZE_SUBSTRATUM_PACKAGES,
                mForceAuthorizeSubstratumPackages.isChecked() ? 1 : 0);
    }

    private void updateForceAuthorizeSubstratumPackagesOptions() {
        mForceAuthorizeSubstratumPackages.setChecked(Settings.Secure.getInt(getActivity().getContentResolver(),
                Settings.Secure.FORCE_AUTHORIZE_SUBSTRATUM_PACKAGES, 0) != 0);
     }

    private void resetDangerousOptions() {
        mDontPokeProperties = true;
        for (int i = 0; i < mResetSwitchPrefs.size(); i++) {
            SwitchPreference cb = mResetSwitchPrefs.get(i);
            if (cb.isChecked()) {
                cb.setChecked(false);
                onPreferenceTreeClick(cb);
            }
        }
        if (mBluetoothEnableInbandRinging != null) {
            mBluetoothEnableInbandRinging.setChecked(true);
            onPreferenceTreeClick(mBluetoothEnableInbandRinging);
        }
        mBugReportInPowerController.resetPreference();
        mEnableAdbController.resetPreference();
        resetDebuggerOptions();
        resetAdbNotifyOptions();
        resetRootAccessOptions();
        writeLogpersistOption(null, true);
        writeLogdSizeOption(null);
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

    private void resetAdbNotifyOptions() {
        LineageSettings.Secure.putInt(getActivity().getContentResolver(),
                LineageSettings.Secure.ADB_NOTIFY, 1);
    }

    private void updateRootAccessOptions() {
        String value = SystemProperties.get(ROOT_ACCESS_PROPERTY, "0");
        mRootAccess.setValue(value);
        mRootAccess.setSummary(getResources()
                .getStringArray(R.array.root_access_entries)[Integer.valueOf(value)]);

        if (mRootAppops != null) {
            mRootAppops.setEnabled(isRootForAppsEnabled());
        }
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

    private void updatePasswordSummary() {
        mPassword.setEnabled(mBackupManager != null);
        if (mBackupManager != null) {
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
    }

    private void writeBtHciSnoopLogOptions() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        SystemProperties.set(BLUETOOTH_BTSNOOP_ENABLE_PROPERTY,
                Boolean.toString(mBtHciSnoopLog.isChecked()));
    }

    private void writeDebuggerOptions() {
        try {
            ActivityManager.getService().setDebugApp(
                    mDebugApp, mWaitForDebugger.isChecked(), true);
        } catch (RemoteException ex) {
        }
    }

    private void writeMockLocation() {
        AppOpsManager appOpsManager = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);

        // Disable the app op of the previous mock location app if such.
        List<PackageOps> packageOps = appOpsManager.getPackagesForOps(MOCK_LOCATION_APP_OPS);
        if (packageOps != null) {
            // Should be one but in case we are in a bad state due to use of command line tools.
            for (PackageOps packageOp : packageOps) {
                if (packageOp.getOps().get(0).getMode() != AppOpsManager.MODE_ERRORED) {
                    String oldMockLocationApp = packageOp.getPackageName();
                    try {
                        ApplicationInfo ai = getActivity().getPackageManager().getApplicationInfo(
                                oldMockLocationApp, PackageManager.GET_DISABLED_COMPONENTS);
                        appOpsManager.setMode(AppOpsManager.OP_MOCK_LOCATION, ai.uid,
                                oldMockLocationApp, AppOpsManager.MODE_ERRORED);
                    } catch (NameNotFoundException e) {
                        /* ignore */
                    }
                }
            }
        }

        // Enable the app op of the new mock location app if such.
        if (!TextUtils.isEmpty(mMockLocationApp)) {
            try {
                ApplicationInfo ai = getActivity().getPackageManager().getApplicationInfo(
                        mMockLocationApp, PackageManager.GET_DISABLED_COMPONENTS);
                appOpsManager.setMode(AppOpsManager.OP_MOCK_LOCATION, ai.uid,
                        mMockLocationApp, AppOpsManager.MODE_ALLOWED);
            } catch (NameNotFoundException e) {
                /* ignore */
            }
        }
    }

    private static void resetDebuggerOptions() {
        try {
            ActivityManager.getService().setDebugApp(
                    null, false, true);
        } catch (RemoteException ex) {
        }
    }

    private void updateDebuggerOptions() {
        mDebugApp = Settings.Global.getString(
                getActivity().getContentResolver(), Settings.Global.DEBUG_APP);
        updateSwitchPreference(mWaitForDebugger, Settings.Global.getInt(
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

    private void updateMockLocation() {
        AppOpsManager appOpsManager = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);

        List<PackageOps> packageOps = appOpsManager.getPackagesForOps(MOCK_LOCATION_APP_OPS);
        if (packageOps != null) {
            for (PackageOps packageOp : packageOps) {
                if (packageOp.getOps().get(0).getMode() == AppOpsManager.MODE_ALLOWED) {
                    mMockLocationApp = packageOps.get(0).getPackageName();
                    break;
                }
            }
        }

        if (!TextUtils.isEmpty(mMockLocationApp)) {
            String label = mMockLocationApp;
            try {
                ApplicationInfo ai = getActivity().getPackageManager().getApplicationInfo(
                        mMockLocationApp, PackageManager.GET_DISABLED_COMPONENTS);
                CharSequence appLabel = getPackageManager().getApplicationLabel(ai);
                if (appLabel != null) {
                    label = appLabel.toString();
                }
            } catch (PackageManager.NameNotFoundException e) {
                /* ignore */
            }

            mMockLocationAppPref.setSummary(getString(R.string.mock_location_app_set, label));
            mHaveDebugSettings = true;
        } else {
            mMockLocationAppPref.setSummary(getString(R.string.mock_location_app_not_set));
        }
    }

    private void updateOtaDisableAutomaticUpdateOptions() {
        // We use the "disabled status" in code, but show the opposite text
        // "Automatic system updates" on screen. So a value 0 indicates the
        // automatic update is enabled.
        updateSwitchPreference(mOtaDisableAutomaticUpdate, Settings.Global.getInt(
                getActivity().getContentResolver(),
                Settings.Global.OTA_DISABLE_AUTOMATIC_UPDATE, 0) != 1);
    }

    private void writeOtaDisableAutomaticUpdateOptions() {
        // We use the "disabled status" in code, but show the opposite text
        // "Automatic system updates" on screen. So a value 0 indicates the
        // automatic update is enabled.
        Settings.Global.putInt(getActivity().getContentResolver(),
                Settings.Global.OTA_DISABLE_AUTOMATIC_UPDATE,
                mOtaDisableAutomaticUpdate.isChecked() ? 0 : 1);
    }

    private static boolean showEnableOemUnlockPreference(Context context) {
        return context.getSystemService(Context.OEM_LOCK_SERVICE) != null &&
                SystemProperties.getBoolean("ro.oem_unlock_supported", false);
    }

    /**
     * Returns whether OEM unlock is allowed by the user and carrier.
     *
     * This does not take into account any restrictions imposed by the device policy.
     */
    private boolean isOemUnlockAllowedByUserAndCarrier() {
        final UserHandle userHandle = UserHandle.of(UserHandle.myUserId());
        return mOemLockManager.isOemUnlockAllowedByCarrier()
                && !mUm.hasBaseUserRestriction(UserManager.DISALLOW_FACTORY_RESET, userHandle);
    }

    private boolean enableOemUnlockPreference() {
        return !isBootloaderUnlocked() && isOemUnlockAllowedByUserAndCarrier();
    }

    private void updateOemUnlockOptions() {
        if (mEnableOemUnlock != null) {
            updateSwitchPreference(mEnableOemUnlock, mOemLockManager.isOemUnlockAllowed());
            updateOemUnlockSettingDescription();
            // Showing mEnableOemUnlock preference as device has persistent data block.
            mEnableOemUnlock.setDisabledByAdmin(null);
            mEnableOemUnlock.setEnabled(enableOemUnlockPreference());
            if (mEnableOemUnlock.isEnabled()) {
                // Check restriction, disable mEnableOemUnlock and apply policy transparency.
                mEnableOemUnlock.checkRestrictionAndSetDisabled(UserManager.DISALLOW_FACTORY_RESET);
            }
        }
    }

    private void updateBugreportOptions() {
        mBugReportController.enablePreference(true);
        mBugReportInPowerController.updateBugreportOptions();
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
        updateSwitchPreference(mStrictMode, currentStrictModeActiveIndex() == 1);
    }

    private void writePointerLocationOptions() {
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.POINTER_LOCATION, mPointerLocation.isChecked() ? 1 : 0);
    }

    private void updatePointerLocationOptions() {
        updateSwitchPreference(mPointerLocation,
                Settings.System.getInt(getActivity().getContentResolver(),
                        Settings.System.POINTER_LOCATION, 0) != 0);
    }

    private void writeShowTouchesOptions() {
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.SHOW_TOUCHES, mShowTouches.isChecked() ? 1 : 0);
    }

    private void updateShowTouchesOptions() {
        updateSwitchPreference(mShowTouches,
                Settings.System.getInt(getActivity().getContentResolver(),
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
                updateSwitchPreference(mShowScreenUpdates, showUpdates != 0);
                @SuppressWarnings("unused")
                int showBackground = reply.readInt();
                int disableOverlays = reply.readInt();
                updateSwitchPreference(mDisableOverlays, disableOverlays != 0);
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
        updateSwitchPreference(mForceHardwareUi,
                SystemProperties.getBoolean(HARDWARE_UI_PROPERTY, false));
    }

    private void writeHardwareUiOptions() {
        SystemProperties.set(HARDWARE_UI_PROPERTY, mForceHardwareUi.isChecked() ? "true" : "false");
        pokeSystemProperties();
    }

    private void updateMsaaOptions() {
        updateSwitchPreference(mForceMsaa, SystemProperties.getBoolean(MSAA_PROPERTY, false));
    }

    private void writeMsaaOptions() {
        SystemProperties.set(MSAA_PROPERTY, mForceMsaa.isChecked() ? "true" : "false");
        pokeSystemProperties();
    }

    private void updateTrackFrameTimeOptions() {
        String value = SystemProperties.get(ThreadedRenderer.PROFILE_PROPERTY);
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
        SystemProperties.set(ThreadedRenderer.PROFILE_PROPERTY,
                newValue == null ? "" : newValue.toString());
        pokeSystemProperties();
        updateTrackFrameTimeOptions();
    }

    private void updateShowNonRectClipOptions() {
        String value = SystemProperties.get(
                ThreadedRenderer.DEBUG_SHOW_NON_RECTANGULAR_CLIP_PROPERTY);
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
        SystemProperties.set(ThreadedRenderer.DEBUG_SHOW_NON_RECTANGULAR_CLIP_PROPERTY,
                newValue == null ? "" : newValue.toString());
        pokeSystemProperties();
        updateShowNonRectClipOptions();
    }

    private void updateShowHwScreenUpdatesOptions() {
        updateSwitchPreference(mShowHwScreenUpdates,
                SystemProperties.getBoolean(ThreadedRenderer.DEBUG_DIRTY_REGIONS_PROPERTY, false));
    }

    private void writeShowHwScreenUpdatesOptions() {
        SystemProperties.set(ThreadedRenderer.DEBUG_DIRTY_REGIONS_PROPERTY,
                mShowHwScreenUpdates.isChecked() ? "true" : null);
        pokeSystemProperties();
    }

    private void updateShowHwLayersUpdatesOptions() {
        updateSwitchPreference(mShowHwLayersUpdates, SystemProperties.getBoolean(
                ThreadedRenderer.DEBUG_SHOW_LAYERS_UPDATES_PROPERTY, false));
    }

    private void writeShowHwLayersUpdatesOptions() {
        SystemProperties.set(ThreadedRenderer.DEBUG_SHOW_LAYERS_UPDATES_PROPERTY,
                mShowHwLayersUpdates.isChecked() ? "true" : null);
        pokeSystemProperties();
    }

    private void updateDebugHwOverdrawOptions() {
        String value = SystemProperties.get(ThreadedRenderer.DEBUG_OVERDRAW_PROPERTY);
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
        SystemProperties.set(ThreadedRenderer.DEBUG_OVERDRAW_PROPERTY,
                newValue == null ? "" : newValue.toString());
        pokeSystemProperties();
        updateDebugHwOverdrawOptions();
    }

    private void updateDebugHwRendererOptions() {
        String value = SystemProperties.get(ThreadedRenderer.DEBUG_RENDERER_PROPERTY);
        if (value == null) {
            value = "";
        }

        CharSequence[] values = mDebugHwRenderer.getEntryValues();
        for (int i = 0; i < values.length; i++) {
            if (value.contentEquals(values[i])) {
                mDebugHwRenderer.setValueIndex(i);
                mDebugHwRenderer.setSummary(mDebugHwRenderer.getEntries()[i]);
                return;
            }
        }
        mDebugHwRenderer.setValueIndex(0);
        mDebugHwRenderer.setSummary(mDebugHwRenderer.getEntries()[0]);
    }

    private void writeDebugHwRendererOptions(Object newValue) {
        SystemProperties.set(ThreadedRenderer.DEBUG_RENDERER_PROPERTY,
                newValue == null ? "" : newValue.toString());
        pokeSystemProperties();
        updateDebugHwRendererOptions();
    }

    private void updateDebugLayoutOptions() {
        updateSwitchPreference(mDebugLayout,
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
     * controlled by development settings
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

    private void updateColorTemperature() {
        updateSwitchPreference(mColorTemperaturePreference,
                SystemProperties.getBoolean(COLOR_TEMPERATURE_PROPERTY, false));
    }

    private void writeColorTemperature() {
        SystemProperties.set(COLOR_TEMPERATURE_PROPERTY,
                mColorTemperaturePreference.isChecked() ? "1" : "0");
        pokeSystemProperties();
        Toast.makeText(getActivity(), R.string.color_temperature_toast, Toast.LENGTH_LONG).show();
    }

    private void updateUSBAudioOptions() {
        updateSwitchPreference(mUSBAudio, Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.USB_AUDIO_AUTOMATIC_ROUTING_DISABLED, 0) != 0);
    }

    private void writeUSBAudioOptions() {
        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.USB_AUDIO_AUTOMATIC_ROUTING_DISABLED,
                mUSBAudio.isChecked() ? 1 : 0);
    }

    private void updateForceResizableOptions() {
        updateSwitchPreference(mForceResizable, Settings.Global.getInt(getContentResolver(),
                Settings.Global.DEVELOPMENT_FORCE_RESIZABLE_ACTIVITIES, 0) != 0);
    }

    private void writeForceResizableOptions() {
        Settings.Global.putInt(getContentResolver(),
                Settings.Global.DEVELOPMENT_FORCE_RESIZABLE_ACTIVITIES,
                mForceResizable.isChecked() ? 1 : 0);
    }

    private void updateForceRtlOptions() {
        updateSwitchPreference(mForceRtlLayout,
                Settings.Global.getInt(getActivity().getContentResolver(),
                        Settings.Global.DEVELOPMENT_FORCE_RTL, 0) != 0);
    }

    private void writeForceRtlOptions() {
        boolean value = mForceRtlLayout.isChecked();
        Settings.Global.putInt(getActivity().getContentResolver(),
                Settings.Global.DEVELOPMENT_FORCE_RTL, value ? 1 : 0);
        SystemProperties.set(Settings.Global.DEVELOPMENT_FORCE_RTL, value ? "1" : "0");
        LocalePicker.updateLocales(getActivity().getResources().getConfiguration().getLocales());
    }

    private void updateWifiDisplayCertificationOptions() {
        updateSwitchPreference(mWifiDisplayCertification, Settings.Global.getInt(
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
        updateSwitchPreference(mWifiVerboseLogging, enabled);
    }

    private void writeWifiVerboseLoggingOptions() {
        mWifiManager.enableVerboseLogging(mWifiVerboseLogging.isChecked() ? 1 : 0);
    }

    private void updateWifiAggressiveHandoverOptions() {
        boolean enabled = mWifiManager.getAggressiveHandover() > 0;
        updateSwitchPreference(mWifiAggressiveHandover, enabled);
    }

    private void writeWifiAggressiveHandoverOptions() {
        mWifiManager.enableAggressiveHandover(mWifiAggressiveHandover.isChecked() ? 1 : 0);
    }

    private void updateWifiAllowScansWithTrafficOptions() {
        boolean enabled = mWifiManager.getAllowScansWithTraffic() > 0;
        updateSwitchPreference(mWifiAllowScansWithTraffic, enabled);
    }

    private void writeWifiAllowScansWithTrafficOptions() {
        mWifiManager.setAllowScansWithTraffic(mWifiAllowScansWithTraffic.isChecked() ? 1 : 0);
    }

    private void updateBluetoothShowDevicesWithoutUserFriendlyNameOptions() {
        updateSwitchPreference(mBluetoothShowDevicesWithoutNames,
                SystemProperties.getBoolean(
                        BLUETOOTH_SHOW_DEVICES_WITHOUT_NAMES_PROPERTY, false));
    }

    private void writeBluetoothShowDevicesWithoutUserFriendlyNameOptions() {
        SystemProperties.set(BLUETOOTH_SHOW_DEVICES_WITHOUT_NAMES_PROPERTY,
                mBluetoothShowDevicesWithoutNames.isChecked() ? "true" : "false");
    }

    private void updateBluetoothDisableAbsVolumeOptions() {
        updateSwitchPreference(mBluetoothDisableAbsVolume,
                SystemProperties.getBoolean(BLUETOOTH_DISABLE_ABSOLUTE_VOLUME_PROPERTY, false));
    }

    private void writeBluetoothDisableAbsVolumeOptions() {
        SystemProperties.set(BLUETOOTH_DISABLE_ABSOLUTE_VOLUME_PROPERTY,
                mBluetoothDisableAbsVolume.isChecked() ? "true" : "false");
    }

    private void updateBluetoothEnableInbandRingingOptions() {
        if (mBluetoothEnableInbandRinging != null) {
            updateSwitchPreference(mBluetoothEnableInbandRinging,
                SystemProperties.getBoolean(BLUETOOTH_ENABLE_INBAND_RINGING_PROPERTY, true));
        }
    }

    private void writeBluetoothEnableInbandRingingOptions() {
        if (mBluetoothEnableInbandRinging != null) {
            SystemProperties.set(BLUETOOTH_ENABLE_INBAND_RINGING_PROPERTY,
                mBluetoothEnableInbandRinging.isChecked() ? "true" : "false");
        }
    }

    private void updateMobileDataAlwaysOnOptions() {
        updateSwitchPreference(mMobileDataAlwaysOn, Settings.Global.getInt(
                getActivity().getContentResolver(),
                Settings.Global.MOBILE_DATA_ALWAYS_ON, 1) != 0);
    }

    private void writeMobileDataAlwaysOnOptions() {
        Settings.Global.putInt(getActivity().getContentResolver(),
                Settings.Global.MOBILE_DATA_ALWAYS_ON,
                mMobileDataAlwaysOn.isChecked() ? 1 : 0);
    }

    private void updateTetheringHardwareOffloadOptions() {
        updateSwitchPreference(mTetheringHardwareOffload, Settings.Global.getInt(
                getActivity().getContentResolver(),
                Settings.Global.TETHER_OFFLOAD_DISABLED, 0) != 1);
    }

    private void writeTetheringHardwareOffloadOptions() {
        Settings.Global.putInt(getActivity().getContentResolver(),
                Settings.Global.TETHER_OFFLOAD_DISABLED,
                mTetheringHardwareOffload.isChecked() ? 0 : 1);
    }

    private String defaultLogdSizeValue() {
        String defaultValue = SystemProperties.get(SELECT_LOGD_DEFAULT_SIZE_PROPERTY);
        if ((defaultValue == null) || (defaultValue.length() == 0)) {
            if (SystemProperties.get("ro.config.low_ram").equals("true")) {
                defaultValue = SELECT_LOGD_SVELTE_DEFAULT_SIZE_VALUE;
            } else {
                defaultValue = SELECT_LOGD_DEFAULT_SIZE_VALUE;
            }
        }
        return defaultValue;
    }

    private void updateLogdSizeValues() {
        if (mLogdSize != null) {
            String currentTag = SystemProperties.get(SELECT_LOGD_TAG_PROPERTY);
            String currentValue = SystemProperties.get(SELECT_LOGD_SIZE_PROPERTY);
            if ((currentTag != null) && currentTag.startsWith(SELECT_LOGD_TAG_SILENCE)) {
                currentValue = SELECT_LOGD_OFF_SIZE_MARKER_VALUE;
            }
            if (mLogpersist != null) {
                String currentLogpersistEnable
                        = SystemProperties.get(ACTUAL_LOGPERSIST_PROPERTY_ENABLE);
                if ((currentLogpersistEnable == null)
                        || !currentLogpersistEnable.equals("true")
                        || currentValue.equals(SELECT_LOGD_OFF_SIZE_MARKER_VALUE)) {
                    writeLogpersistOption(null, true);
                    mLogpersist.setEnabled(false);
                } else if (mSettingsEnabler.getLastEnabledState()) {
                    mLogpersist.setEnabled(true);
                }
            }
            if ((currentValue == null) || (currentValue.length() == 0)) {
                currentValue = defaultLogdSizeValue();
            }
            String[] values = getResources().getStringArray(R.array.select_logd_size_values);
            String[] titles = getResources().getStringArray(R.array.select_logd_size_titles);
            int index = 2; // punt to second entry if not found
            if (SystemProperties.get("ro.config.low_ram").equals("true")) {
                mLogdSize.setEntries(R.array.select_logd_size_lowram_titles);
                titles = getResources().getStringArray(R.array.select_logd_size_lowram_titles);
                index = 1;
            }
            String[] summaries = getResources().getStringArray(R.array.select_logd_size_summaries);
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
        boolean disable = (newValue != null) &&
                (newValue.toString().equals(SELECT_LOGD_OFF_SIZE_MARKER_VALUE));
        String currentTag = SystemProperties.get(SELECT_LOGD_TAG_PROPERTY);
        if (currentTag == null) {
            currentTag = "";
        }
        // filter clean and unstack all references to our setting
        String newTag = currentTag.replaceAll(
                ",+" + SELECT_LOGD_TAG_SILENCE, "").replaceFirst(
                "^" + SELECT_LOGD_TAG_SILENCE + ",*", "").replaceAll(
                ",+", ",").replaceFirst(
                ",+$", "");
        if (disable) {
            newValue = SELECT_LOGD_MINIMUM_SIZE_VALUE;
            // Make sure snet_event_log get through first, but do not override
            String snetValue = SystemProperties.get(SELECT_LOGD_SNET_TAG_PROPERTY);
            if ((snetValue == null) || (snetValue.length() == 0)) {
                snetValue = SystemProperties.get(SELECT_LOGD_RUNTIME_SNET_TAG_PROPERTY);
                if ((snetValue == null) || (snetValue.length() == 0)) {
                    SystemProperties.set(SELECT_LOGD_SNET_TAG_PROPERTY, "I");
                }
            }
            // Silence all log sources, security logs notwithstanding
            if (newTag.length() != 0) {
                newTag = "," + newTag;
            }
            // Stack settings, stack to help preserve original value
            newTag = SELECT_LOGD_TAG_SILENCE + newTag;
        }
        if (!newTag.equals(currentTag)) {
            SystemProperties.set(SELECT_LOGD_TAG_PROPERTY, newTag);
        }
        String defaultValue = defaultLogdSizeValue();
        final String size = ((newValue != null) && (newValue.toString().length() != 0)) ?
                newValue.toString() : defaultValue;
        SystemProperties.set(SELECT_LOGD_SIZE_PROPERTY, defaultValue.equals(size) ? "" : size);
        SystemProperties.set("ctl.start", "logd-reinit");
        pokeSystemProperties();
        updateLogdSizeValues();
    }

    private void updateLogpersistValues() {
        if (mLogpersist == null) {
            return;
        }
        String currentValue = SystemProperties.get(ACTUAL_LOGPERSIST_PROPERTY);
        if (currentValue == null) {
            currentValue = "";
        }
        String currentBuffers = SystemProperties.get(ACTUAL_LOGPERSIST_PROPERTY_BUFFER);
        if ((currentBuffers == null) || (currentBuffers.length() == 0)) {
            currentBuffers = "all";
        }
        int index = 0;
        if (currentValue.equals(SELECT_LOGPERSIST_PROPERTY_SERVICE)) {
            index = 1;
            if (currentBuffers.equals("kernel")) {
                index = 3;
            } else if (!currentBuffers.equals("all") &&
                    !currentBuffers.contains("radio") &&
                    currentBuffers.contains("security") &&
                    currentBuffers.contains("kernel")) {
                index = 2;
                if (!currentBuffers.contains("default")) {
                    String[] contains = {"main", "events", "system", "crash"};
                    for (int i = 0; i < contains.length; i++) {
                        if (!currentBuffers.contains(contains[i])) {
                            index = 1;
                            break;
                        }
                    }
                }
            }
        }
        mLogpersist.setValue(
                getResources().getStringArray(R.array.select_logpersist_values)[index]);
        mLogpersist.setSummary(
                getResources().getStringArray(R.array.select_logpersist_summaries)[index]);
        mLogpersist.setOnPreferenceChangeListener(this);
        if (index != 0) {
            mLogpersistCleared = false;
        } else if (!mLogpersistCleared) {
            // would File.delete() directly but need to switch uid/gid to access
            SystemProperties.set(ACTUAL_LOGPERSIST_PROPERTY, SELECT_LOGPERSIST_PROPERTY_CLEAR);
            pokeSystemProperties();
            mLogpersistCleared = true;
        }
    }

    private void setLogpersistOff(boolean update) {
        SystemProperties.set(SELECT_LOGPERSIST_PROPERTY_BUFFER, "");
        // deal with trampoline of empty properties
        SystemProperties.set(ACTUAL_LOGPERSIST_PROPERTY_BUFFER, "");
        SystemProperties.set(SELECT_LOGPERSIST_PROPERTY, "");
        SystemProperties.set(ACTUAL_LOGPERSIST_PROPERTY,
                update ? "" : SELECT_LOGPERSIST_PROPERTY_STOP);
        pokeSystemProperties();
        if (update) {
            updateLogpersistValues();
        } else {
            for (int i = 0; i < 3; i++) {
                String currentValue = SystemProperties.get(ACTUAL_LOGPERSIST_PROPERTY);
                if ((currentValue == null) || currentValue.equals("")) {
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    private void writeLogpersistOption(Object newValue, boolean skipWarning) {
        if (mLogpersist == null) {
            return;
        }
        String currentTag = SystemProperties.get(SELECT_LOGD_TAG_PROPERTY);
        if ((currentTag != null) && currentTag.startsWith(SELECT_LOGD_TAG_SILENCE)) {
            newValue = null;
            skipWarning = true;
        }

        if ((newValue == null) || newValue.toString().equals("")) {
            if (skipWarning) {
                mLogpersistCleared = false;
            } else if (!mLogpersistCleared) {
                // if transitioning from on to off, pop up an are you sure?
                String currentValue = SystemProperties.get(ACTUAL_LOGPERSIST_PROPERTY);
                if ((currentValue != null) &&
                        currentValue.equals(SELECT_LOGPERSIST_PROPERTY_SERVICE)) {
                    if (mLogpersistClearDialog != null) dismissDialogs();
                    mLogpersistClearDialog = new AlertDialog.Builder(getActivity()).setMessage(
                            getActivity().getResources().getString(
                                    R.string.dev_logpersist_clear_warning_message))
                            .setTitle(R.string.dev_logpersist_clear_warning_title)
                            .setPositiveButton(android.R.string.yes, this)
                            .setNegativeButton(android.R.string.no, this)
                            .show();
                    mLogpersistClearDialog.setOnDismissListener(this);
                    return;
                }
            }
            setLogpersistOff(true);
            return;
        }

        String currentBuffer = SystemProperties.get(ACTUAL_LOGPERSIST_PROPERTY_BUFFER);
        if ((currentBuffer != null) && !currentBuffer.equals(newValue.toString())) {
            setLogpersistOff(false);
        }
        SystemProperties.set(SELECT_LOGPERSIST_PROPERTY_BUFFER, newValue.toString());
        SystemProperties.set(SELECT_LOGPERSIST_PROPERTY, SELECT_LOGPERSIST_PROPERTY_SERVICE);
        pokeSystemProperties();
        for (int i = 0; i < 3; i++) {
            String currentValue = SystemProperties.get(ACTUAL_LOGPERSIST_PROPERTY);
            if ((currentValue != null)
                    && currentValue.equals(SELECT_LOGPERSIST_PROPERTY_SERVICE)) {
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
        updateLogpersistValues();
    }

    private void updateUsbConfigurationValues() {
        if (mUsbConfiguration != null) {
            UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);

            String[] values = getResources().getStringArray(R.array.usb_configuration_values);
            String[] titles = getResources().getStringArray(R.array.usb_configuration_titles);
            int index = 0;
            for (int i = 0; i < titles.length; i++) {
                if (manager.isFunctionEnabled(values[i])) {
                    index = i;
                    break;
                }
            }
            mUsbConfiguration.setValue(values[index]);
            mUsbConfiguration.setSummary(titles[index]);
            mUsbConfiguration.setOnPreferenceChangeListener(this);
        }
    }

    private void writeUsbConfigurationOption(Object newValue) {
        UsbManager manager = (UsbManager) getActivity().getSystemService(Context.USB_SERVICE);
        String function = newValue.toString();
        if (function.equals("none")) {
            manager.setCurrentFunction(function, false);
        } else {
            manager.setCurrentFunction(function, true);
        }
    }

    private void initBluetoothConfigurationValues() {
        String[] values;
        String[] summaries;
        int index;

        // Init the AVRCP Version - Default
        values = getResources().getStringArray(R.array.bluetooth_avrcp_version_values);
        summaries = getResources().getStringArray(R.array.bluetooth_avrcp_versions);
        String value = SystemProperties.get(BLUETOOTH_AVRCP_VERSION_PROPERTY, values[0]);
        index = mBluetoothSelectAvrcpVersion.findIndexOfValue(value);
        mBluetoothSelectAvrcpVersion.setValue(values[index]);
        mBluetoothSelectAvrcpVersion.setSummary(summaries[index]);

        // Init the Codec Type - Default
        values = getResources().getStringArray(R.array.bluetooth_a2dp_codec_values);
        summaries = getResources().getStringArray(R.array.bluetooth_a2dp_codec_summaries);
        index = 0;
        mBluetoothSelectA2dpCodec.setValue(values[index]);
        mBluetoothSelectA2dpCodec.setSummary(summaries[index]);

        // Init the Sample Rate - Default
        values = getResources().getStringArray(R.array.bluetooth_a2dp_codec_sample_rate_values);
        summaries = getResources().getStringArray(R.array.bluetooth_a2dp_codec_sample_rate_summaries);
        index = 0;
        mBluetoothSelectA2dpSampleRate.setValue(values[index]);
        mBluetoothSelectA2dpSampleRate.setSummary(summaries[index]);

        // Init the Bits Per Sample - Default
        values = getResources().getStringArray(R.array.bluetooth_a2dp_codec_bits_per_sample_values);
        summaries = getResources().getStringArray(R.array.bluetooth_a2dp_codec_bits_per_sample_summaries);
        index = 0;
        mBluetoothSelectA2dpBitsPerSample.setValue(values[index]);
        mBluetoothSelectA2dpBitsPerSample.setSummary(summaries[index]);

        // Init the Channel Mode - Default
        values = getResources().getStringArray(R.array.bluetooth_a2dp_codec_channel_mode_values);
        summaries = getResources().getStringArray(R.array.bluetooth_a2dp_codec_channel_mode_summaries);
        index = 0;
        mBluetoothSelectA2dpChannelMode.setValue(values[index]);
        mBluetoothSelectA2dpChannelMode.setSummary(summaries[index]);

        // Init the LDAC Playback Quality - ABR
        values = getResources().getStringArray(R.array.bluetooth_a2dp_codec_ldac_playback_quality_values);
        summaries = getResources().getStringArray(R.array.bluetooth_a2dp_codec_ldac_playback_quality_summaries);
        index = 3;
        mBluetoothSelectA2dpLdacPlaybackQuality.setValue(values[index]);
        mBluetoothSelectA2dpLdacPlaybackQuality.setSummary(summaries[index]);
    }

    private void writeBluetoothAvrcpVersion(Object newValue) {
        SystemProperties.set(BLUETOOTH_AVRCP_VERSION_PROPERTY, newValue.toString());
        int index = mBluetoothSelectAvrcpVersion.findIndexOfValue(newValue.toString());
        if (index >= 0) {
            String[] titles = getResources().getStringArray(R.array.bluetooth_avrcp_versions);
            mBluetoothSelectAvrcpVersion.setSummary(titles[index]);
        }
    }

    private void updateBluetoothA2dpConfigurationValues() {
        int index;
        String[] summaries;
        BluetoothCodecStatus codecStatus = null;
        BluetoothCodecConfig codecConfig = null;
        BluetoothCodecConfig[] codecsLocalCapabilities = null;
        BluetoothCodecConfig[] codecsSelectableCapabilities = null;
        String streaming;
        Resources resources = null;

        synchronized (mBluetoothA2dpLock) {
            if (mBluetoothA2dp != null) {
                codecStatus = mBluetoothA2dp.getCodecStatus();
                if (codecStatus != null) {
                    codecConfig = codecStatus.getCodecConfig();
                    codecsLocalCapabilities = codecStatus.getCodecsLocalCapabilities();
                    codecsSelectableCapabilities = codecStatus.getCodecsSelectableCapabilities();
                }
            }
        }
        if (codecConfig == null) {
            return;
        }

        try {
            resources = getResources();
        } catch (IllegalStateException e) {
            return;
        }
        if (resources == null) {
            return;
        }

        // Update the Codec Type
        index = -1;
        switch (codecConfig.getCodecType()) {
        case BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC:
            index = 1;
            break;
        case BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC:
            index = 2;
            break;
        case BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX:
            index = 3;
            break;
        case BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_HD:
            index = 4;
            break;
        case BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC:
            index = 5;
            break;
        case BluetoothCodecConfig.SOURCE_CODEC_TYPE_INVALID:
        default:
            break;
        }
        if (index >= 0 && mBluetoothSelectA2dpCodec != null) {
            summaries = resources.getStringArray(R.array.bluetooth_a2dp_codec_summaries);
            streaming = resources.getString(R.string.bluetooth_select_a2dp_codec_streaming_label, summaries[index]);
            mBluetoothSelectA2dpCodec.setSummary(streaming);
        }

        // Update the Sample Rate
        index = -1;
        switch (codecConfig.getSampleRate()) {
        case BluetoothCodecConfig.SAMPLE_RATE_44100:
            index = 1;
            break;
        case BluetoothCodecConfig.SAMPLE_RATE_48000:
            index = 2;
            break;
        case BluetoothCodecConfig.SAMPLE_RATE_88200:
            index = 3;
            break;
        case BluetoothCodecConfig.SAMPLE_RATE_96000:
            index = 4;
            break;
        case BluetoothCodecConfig.SAMPLE_RATE_176400:
        case BluetoothCodecConfig.SAMPLE_RATE_192000:
        case BluetoothCodecConfig.SAMPLE_RATE_NONE:
        default:
            break;
        }
        if (index >= 0 && mBluetoothSelectA2dpSampleRate != null) {
            summaries = resources.getStringArray(R.array.bluetooth_a2dp_codec_sample_rate_summaries);
            streaming = resources.getString(R.string.bluetooth_select_a2dp_codec_streaming_label, summaries[index]);
             mBluetoothSelectA2dpSampleRate.setSummary(streaming);
        }

        // Update the Bits Per Sample
        index = -1;
        switch (codecConfig.getBitsPerSample()) {
        case BluetoothCodecConfig.BITS_PER_SAMPLE_16:
            index = 1;
            break;
        case BluetoothCodecConfig.BITS_PER_SAMPLE_24:
            index = 2;
            break;
        case BluetoothCodecConfig.BITS_PER_SAMPLE_32:
            index = 3;
            break;
        case BluetoothCodecConfig.BITS_PER_SAMPLE_NONE:
        default:
            break;
        }
        if (index >= 0 && mBluetoothSelectA2dpBitsPerSample != null) {
            summaries = resources.getStringArray(R.array.bluetooth_a2dp_codec_bits_per_sample_summaries);
            streaming = resources.getString(R.string.bluetooth_select_a2dp_codec_streaming_label, summaries[index]);
            mBluetoothSelectA2dpBitsPerSample.setSummary(streaming);
        }

        // Update the Channel Mode
        index = -1;
        switch (codecConfig.getChannelMode()) {
        case BluetoothCodecConfig.CHANNEL_MODE_MONO:
            index = 1;
            break;
        case BluetoothCodecConfig.CHANNEL_MODE_STEREO:
            index = 2;
            break;
        case BluetoothCodecConfig.CHANNEL_MODE_NONE:
        default:
            break;
        }
        if (index >= 0 && mBluetoothSelectA2dpChannelMode != null) {
            summaries = resources.getStringArray(R.array.bluetooth_a2dp_codec_channel_mode_summaries);
            streaming = resources.getString(R.string.bluetooth_select_a2dp_codec_streaming_label, summaries[index]);
             mBluetoothSelectA2dpChannelMode.setSummary(streaming);
        }

        // Update the LDAC Playback Quality
        // The actual values are 0, 1, 2 - those are extracted
        // as mod-10 remainders of a larger value.
        // The reason is because within BluetoothCodecConfig we cannot use
        // a codec-specific value of zero.
        index = (int)codecConfig.getCodecSpecific1();
        if (index > 0) {
            index %= 10;
        } else {
            index = -1;
        }
        switch (index) {
        case 0:
        case 1:
        case 2:
        case 3:
            break;
        default:
            index = -1;
            break;
        }
        if (index >= 0 && mBluetoothSelectA2dpLdacPlaybackQuality != null) {
            summaries = resources.getStringArray(R.array.bluetooth_a2dp_codec_ldac_playback_quality_summaries);
            streaming = resources.getString(R.string.bluetooth_select_a2dp_codec_streaming_label, summaries[index]);
            mBluetoothSelectA2dpLdacPlaybackQuality.setSummary(streaming);
        }
    }

    private void writeBluetoothConfigurationOption(Preference preference,
                                                   Object newValue) {
        String[] summaries;
        int index;
        int codecTypeValue = BluetoothCodecConfig.SOURCE_CODEC_TYPE_INVALID;
        int codecPriorityValue = BluetoothCodecConfig.CODEC_PRIORITY_DEFAULT;
        int sampleRateValue = BluetoothCodecConfig.SAMPLE_RATE_NONE;
        int bitsPerSampleValue = BluetoothCodecConfig.BITS_PER_SAMPLE_NONE;
        int channelModeValue = BluetoothCodecConfig.CHANNEL_MODE_NONE;
        long codecSpecific1Value = 0;
        long codecSpecific2Value = 0;
        long codecSpecific3Value = 0;
        long codecSpecific4Value = 0;

        // Codec Type
        String codecType = mBluetoothSelectA2dpCodec.getValue();
        if (preference == mBluetoothSelectA2dpCodec) {
            codecType = newValue.toString();
            index = mBluetoothSelectA2dpCodec.findIndexOfValue(newValue.toString());
            if (index >= 0) {
                summaries = getResources().getStringArray(R.array.bluetooth_a2dp_codec_summaries);
                mBluetoothSelectA2dpCodec.setSummary(summaries[index]);
            }
        }
        index = mBluetoothSelectA2dpCodec.findIndexOfValue(codecType);
        switch (index) {
        case 0:
            // Reset the priority of the current codec to default
            String oldValue = mBluetoothSelectA2dpCodec.getValue();
            switch (mBluetoothSelectA2dpCodec.findIndexOfValue(oldValue)) {
            case 0:
                break;      // No current codec
            case 1:
                codecTypeValue = BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC;
                break;
            case 2:
                codecTypeValue = BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC;
                break;
            case 3:
                codecTypeValue = BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX;
                break;
            case 4:
                codecTypeValue = BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_HD;
                break;
            case 5:
                codecTypeValue = BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC;
                break;
            default:
                break;
            }
            break;
        case 1:
            codecTypeValue = BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC;
            codecPriorityValue = BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST;
            break;
        case 2:
            codecTypeValue = BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC;
            codecPriorityValue = BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST;
            break;
        case 3:
            codecTypeValue = BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX;
            codecPriorityValue = BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST;
            break;
        case 4:
            codecTypeValue = BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_HD;
            codecPriorityValue = BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST;
            break;
        case 5:
            codecTypeValue = BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC;
            codecPriorityValue = BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST;
            break;
        case 6:
        synchronized (mBluetoothA2dpLock) {
            if (mBluetoothA2dp != null) {
                mBluetoothA2dp.enableOptionalCodecs();
            }
        }
        return;
        case 7:
        synchronized (mBluetoothA2dpLock) {
            if (mBluetoothA2dp != null) {
                mBluetoothA2dp.disableOptionalCodecs();
            }
        }
        return;
        default:
            break;
        }

        // Sample Rate
        String sampleRate = mBluetoothSelectA2dpSampleRate.getValue();
        if (preference == mBluetoothSelectA2dpSampleRate) {
            sampleRate = newValue.toString();
            index = mBluetoothSelectA2dpSampleRate.findIndexOfValue(newValue.toString());
            if (index >= 0) {
                summaries = getResources().getStringArray(R.array.bluetooth_a2dp_codec_sample_rate_summaries);
                mBluetoothSelectA2dpSampleRate.setSummary(summaries[index]);
            }
        }
        index = mBluetoothSelectA2dpSampleRate.findIndexOfValue(sampleRate);
        switch (index) {
        case 0:
            // Reset to default
            break;
        case 1:
            sampleRateValue = BluetoothCodecConfig.SAMPLE_RATE_44100;
            break;
        case 2:
            sampleRateValue = BluetoothCodecConfig.SAMPLE_RATE_48000;
            break;
        case 3:
            sampleRateValue = BluetoothCodecConfig.SAMPLE_RATE_88200;
            break;
        case 4:
            sampleRateValue = BluetoothCodecConfig.SAMPLE_RATE_96000;
            break;
        default:
            break;
        }

        // Bits Per Sample
        String bitsPerSample = mBluetoothSelectA2dpBitsPerSample.getValue();
        if (preference == mBluetoothSelectA2dpBitsPerSample) {
            bitsPerSample = newValue.toString();
            index = mBluetoothSelectA2dpBitsPerSample.findIndexOfValue(newValue.toString());
            if (index >= 0) {
                summaries = getResources().getStringArray(R.array.bluetooth_a2dp_codec_bits_per_sample_summaries);
                mBluetoothSelectA2dpBitsPerSample.setSummary(summaries[index]);
            }
        }
        index = mBluetoothSelectA2dpBitsPerSample.findIndexOfValue(bitsPerSample);
        switch (index) {
        case 0:
            // Reset to default
            break;
        case 1:
            bitsPerSampleValue = BluetoothCodecConfig.BITS_PER_SAMPLE_16;
            break;
        case 2:
            bitsPerSampleValue = BluetoothCodecConfig.BITS_PER_SAMPLE_24;
            break;
        case 3:
            bitsPerSampleValue = BluetoothCodecConfig.BITS_PER_SAMPLE_32;
            break;
        default:
            break;
        }

        // Channel Mode
        String channelMode = mBluetoothSelectA2dpChannelMode.getValue();
        if (preference == mBluetoothSelectA2dpChannelMode) {
            channelMode = newValue.toString();
            index = mBluetoothSelectA2dpChannelMode.findIndexOfValue(newValue.toString());
            if (index >= 0) {
                summaries = getResources().getStringArray(R.array.bluetooth_a2dp_codec_channel_mode_summaries);
                mBluetoothSelectA2dpChannelMode.setSummary(summaries[index]);
            }
        }
        index = mBluetoothSelectA2dpChannelMode.findIndexOfValue(channelMode);
        switch (index) {
        case 0:
            // Reset to default
            break;
        case 1:
            channelModeValue = BluetoothCodecConfig.CHANNEL_MODE_MONO;
            break;
        case 2:
            channelModeValue = BluetoothCodecConfig.CHANNEL_MODE_STEREO;
            break;
        default:
            break;
        }

        // LDAC Playback Quality
        String ldacPlaybackQuality = mBluetoothSelectA2dpLdacPlaybackQuality.getValue();
        if (preference == mBluetoothSelectA2dpLdacPlaybackQuality) {
            ldacPlaybackQuality = newValue.toString();
            index = mBluetoothSelectA2dpLdacPlaybackQuality.findIndexOfValue(newValue.toString());
            if (index >= 0) {
                summaries = getResources().getStringArray(R.array.bluetooth_a2dp_codec_ldac_playback_quality_summaries);
                mBluetoothSelectA2dpLdacPlaybackQuality.setSummary(summaries[index]);
            }
        }
        index = mBluetoothSelectA2dpLdacPlaybackQuality.findIndexOfValue(ldacPlaybackQuality);
        switch (index) {
        case 0:
        case 1:
        case 2:
        case 3:
            codecSpecific1Value = 1000 + index;
            break;
        default:
            break;
        }

        BluetoothCodecConfig codecConfig =
            new BluetoothCodecConfig(codecTypeValue, codecPriorityValue,
                                     sampleRateValue, bitsPerSampleValue,
                                     channelModeValue, codecSpecific1Value,
                                     codecSpecific2Value, codecSpecific3Value,
                                     codecSpecific4Value);

        synchronized (mBluetoothA2dpLock) {
            if (mBluetoothA2dp != null) {
                mBluetoothA2dp.setCodecConfigPreference(codecConfig);
            }
        }
    }

    private void writeImmediatelyDestroyActivitiesOptions() {
        try {
            ActivityManager.getService().setAlwaysFinish(
                    mImmediatelyDestroyActivities.isChecked());
        } catch (RemoteException ex) {
        }
    }

    private void updateImmediatelyDestroyActivitiesOptions() {
        updateSwitchPreference(mImmediatelyDestroyActivities, Settings.Global.getInt(
                getActivity().getContentResolver(), Settings.Global.ALWAYS_FINISH_ACTIVITIES, 0)
                != 0);
    }

    private void updateAnimationScaleValue(int which, AnimationScalePreference pref) {
        try {
            float scale = mWindowManager.getAnimationScale(which);
            if (scale != 0.75) {
                mHaveDebugSettings = true;
            }
          pref.setScale(scale);
        } catch (RemoteException e) {
        }
    }

    private void updateAnimationScaleOptions() {
        updateAnimationScaleValue(0, mWindowAnimationScale);
        updateAnimationScaleValue(1, mTransitionAnimationScale);
        updateAnimationScaleValue(2, mAnimatorDurationScale);
    }

    private void writeAnimationScaleOption(int which, AnimationScalePreference pref, Object newValue) {
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
                Settings.Global.OVERLAY_DISPLAY_DEVICES, (String) newValue);
        updateOverlayDisplayDevicesOptions();
    }

    private void updateAppProcessLimitOptions() {
        try {
            int limit = ActivityManager.getService().getProcessLimit();
            CharSequence[] values = mAppProcessLimit.getEntryValues();
            for (int i = 0; i < values.length; i++) {
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
            ActivityManager.getService().setProcessLimit(limit);
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
        updateSwitchPreference(mShowAllANRs, Settings.Secure.getInt(
                getActivity().getContentResolver(), Settings.Secure.ANR_SHOW_BACKGROUND, 0) != 0);
    }

    private void writeShowNotificationChannelWarningsOptions() {
        Settings.Global.putInt(getActivity().getContentResolver(),
                Settings.Global.SHOW_NOTIFICATION_CHANNEL_WARNINGS,
                mShowNotificationChannelWarnings.isChecked() ? 1 : 0);
    }

    private void updateShowNotificationChannelWarningsOptions() {
        final int defaultWarningEnabled = Build.TYPE.equals("eng") ? 1 : 0;
        updateSwitchPreference(mShowNotificationChannelWarnings, Settings.Global.getInt(
                getActivity().getContentResolver(),
                Settings.Global.SHOW_NOTIFICATION_CHANNEL_WARNINGS, defaultWarningEnabled) != 0);
    }

    private void confirmEnableOemUnlock() {
        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    mOemLockManager.setOemUnlockAllowedByUser(true);
                }
            }
        };

        DialogInterface.OnDismissListener onDismissListener =
                new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (getActivity() == null) {
                            return;
                        }
                        updateAllOptions();
                    }
                };

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.confirm_enable_oem_unlock_title)
                .setMessage(R.string.confirm_enable_oem_unlock_text)
                .setPositiveButton(R.string.enable_text, onClickListener)
                .setNegativeButton(android.R.string.cancel, null)
                .setOnDismissListener(onDismissListener)
                .create()
                .show();
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        if (switchView != mSwitchBar.getSwitch()) {
            return;
        }
        final boolean lastEnabledState = mSettingsEnabler.getLastEnabledState();
        if (isChecked != lastEnabledState) {
            if (isChecked) {
                mDialogClicked = false;
                if (mEnableDialog != null) dismissDialogs();
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
                mSettingsEnabler.disableDevelopmentSettings();
                setPrefsEnabledState(false);
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
        } else if (requestCode == RESULT_MOCK_LOCATION_APP) {
            if (resultCode == Activity.RESULT_OK) {
                mMockLocationApp = data.getAction();
                writeMockLocation();
                updateMockLocation();
            }
        } else if (requestCode == REQUEST_CODE_ENABLE_OEM_UNLOCK) {
            if (resultCode == Activity.RESULT_OK) {
                if (mEnableOemUnlock.isChecked()) {
                    confirmEnableOemUnlock();
                } else {
                    mOemLockManager.setOemUnlockAllowedByUser(false);
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (Utils.isMonkeyRunning()) {
            return false;
        }

        if (mBugReportInPowerController.handlePreferenceTreeClick(preference)) {
            return true;
        }

        if (mTelephonyMonitorController.handlePreferenceTreeClick(preference)) {
            return true;
        }

        if (mWebViewAppPrefController.handlePreferenceTreeClick(preference)) {
            return true;
        }

        if (mVerifyAppsOverUsbController.handlePreferenceTreeClick(preference)) {
            return true;
        }

        if (mEnableAdbController.handlePreferenceTreeClick(preference)) {
            return true;
        }

        if (mCameraLaserSensorController.handlePreferenceTreeClick(preference)) {
            return true;
        }

        if (preference == mAdbOverNetwork) {
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
                LineageSettings.Secure.putInt(getActivity().getContentResolver(),
                        LineageSettings.Secure.ADB_PORT, -1);
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
        } else if (preference == mKeepScreenOn) {
            Settings.Global.putInt(getActivity().getContentResolver(),
                    Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                    mKeepScreenOn.isChecked() ?
                            (BatteryManager.BATTERY_PLUGGED_AC | BatteryManager.BATTERY_PLUGGED_USB
                                    | BatteryManager.BATTERY_PLUGGED_WIRELESS) : 0);
        } else if (preference == mBtHciSnoopLog) {
            writeBtHciSnoopLogOptions();
        } else if (preference == mEnableOemUnlock && mEnableOemUnlock.isEnabled()) {
            if (mEnableOemUnlock.isChecked()) {
                if (!showKeyguardConfirmation(getResources(), REQUEST_CODE_ENABLE_OEM_UNLOCK)) {
                    confirmEnableOemUnlock();
                }
            } else {
                mOemLockManager.setOemUnlockAllowedByUser(false);
            }
        } else if (preference == mMockLocationAppPref) {
            Intent intent = new Intent(getActivity(), AppPicker.class);
            intent.putExtra(AppPicker.EXTRA_REQUESTIING_PERMISSION,
                    Manifest.permission.ACCESS_MOCK_LOCATION);
            startActivityForResult(intent, RESULT_MOCK_LOCATION_APP);
        } else if (preference == mDebugViewAttributes) {
            Settings.Global.putInt(getActivity().getContentResolver(),
                    Settings.Global.DEBUG_VIEW_ATTRIBUTES,
                    mDebugViewAttributes.isChecked() ? 1 : 0);
        } else if (preference == mForceAllowOnExternal) {
            Settings.Global.putInt(getActivity().getContentResolver(),
                    Settings.Global.FORCE_ALLOW_ON_EXTERNAL,
                    mForceAllowOnExternal.isChecked() ? 1 : 0);
        } else if (preference == mDebugAppPref) {
            Intent intent = new Intent(getActivity(), AppPicker.class);
            intent.putExtra(AppPicker.EXTRA_DEBUGGABLE, true);
            startActivityForResult(intent, RESULT_DEBUG_APP);
        } else if (preference == mWaitForDebugger) {
            writeDebuggerOptions();
        } else if (preference == mOtaDisableAutomaticUpdate) {
            writeOtaDisableAutomaticUpdateOptions();
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
        } else if (preference == mImmediatelyDestroyActivities) {
            writeImmediatelyDestroyActivitiesOptions();
        } else if (preference == mShowAllANRs) {
            writeShowAllANRsOptions();
        } else if (preference == mShowNotificationChannelWarnings) {
            writeShowNotificationChannelWarningsOptions();
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
        } else if (preference == mMobileDataAlwaysOn) {
            writeMobileDataAlwaysOnOptions();
        } else if (preference == mTetheringHardwareOffload) {
            writeTetheringHardwareOffloadOptions();
        } else if (preference == mColorTemperaturePreference) {
            writeColorTemperature();
        } else if (preference == mUSBAudio) {
            writeUSBAudioOptions();
        } else if (preference == mForceResizable) {
            writeForceResizableOptions();
        } else if (preference == mBluetoothShowDevicesWithoutNames) {
            writeBluetoothShowDevicesWithoutUserFriendlyNameOptions();
        } else if (preference == mForceAuthorizeSubstratumPackages) {
            writeForceAuthorizeSubstratumPackagesOptions();
        } else if (preference == mBluetoothDisableAbsVolume) {
            writeBluetoothDisableAbsVolumeOptions();
        } else if (preference == mBluetoothEnableInbandRinging) {
            writeBluetoothEnableInbandRingingOptions();
        } else if (SHORTCUT_MANAGER_RESET_KEY.equals(preference.getKey())) {
            resetShortcutManagerThrottling();
        } else {
            return super.onPreferenceTreeClick(preference);
        }

        return false;
    }

    private boolean showKeyguardConfirmation(Resources resources, int requestCode) {
        return new ChooseLockSettingsHelper(getActivity(), this).launchConfirmationActivity(
                requestCode, resources.getString(R.string.oem_unlock_enable));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (HDCP_CHECKING_KEY.equals(preference.getKey())) {
            SystemProperties.set(HDCP_CHECKING_PROPERTY, newValue.toString());
            updateHdcpValues();
            pokeSystemProperties();
            return true;
        } else if (preference == mBluetoothSelectAvrcpVersion) {
           writeBluetoothAvrcpVersion(newValue);
           return true;
        } else if ((preference == mBluetoothSelectA2dpCodec) ||
                   (preference == mBluetoothSelectA2dpSampleRate) ||
                   (preference == mBluetoothSelectA2dpBitsPerSample) ||
                   (preference == mBluetoothSelectA2dpChannelMode) ||
                   (preference == mBluetoothSelectA2dpLdacPlaybackQuality)) {
            writeBluetoothConfigurationOption(preference, newValue);
            return true;
        } else if (preference == mLogdSize) {
            writeLogdSizeOption(newValue);
            return true;
        } else if (preference == mLogpersist) {
            writeLogpersistOption(newValue, false);
            return true;
        } else if (preference == mUsbConfiguration) {
            writeUsbConfigurationOption(newValue);
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
        } else if (preference == mTrackFrameTime) {
            writeTrackFrameTimeOptions(newValue);
            return true;
        } else if (preference == mDebugHwOverdraw) {
            writeDebugHwOverdrawOptions(newValue);
            return true;
        } else if (preference == mDebugHwRenderer) {
            writeDebugHwRendererOptions(newValue);
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

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mWindowAnimationScale ||
                preference == mTransitionAnimationScale ||
                preference == mAnimatorDurationScale) {
            ((AnimationScalePreference) preference).click();
        }
        if (preference == mRootAppops) {
            Activity mActivity = getActivity();
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.putExtra("appops_tab", getString(R.string.app_ops_categories_su));
            intent.setClass(mActivity, AppOpsSummaryActivity.class);
            mActivity.startActivity(intent);
            return true;
        }
        return false;
    }

    private void dismissDialogs() {
        mEnableAdbController.dismissDialogs();
        if (mAdbKeysDialog != null) {
            mAdbKeysDialog.dismiss();
            mAdbKeysDialog = null;
        }
        if (mEnableDialog != null) {
            mEnableDialog.dismiss();
            mEnableDialog = null;
        }
        if (mLogpersistClearDialog != null) {
            mLogpersistClearDialog.dismiss();
            mLogpersistClearDialog = null;
        }
        if (mAdbTcpDialog != null) {
            mAdbTcpDialog.dismiss();
            mAdbTcpDialog = null;
        }
        if (mRootDialog != null) {
            mRootDialog.dismiss();
            mRootDialog = null;
        }
    }

    public void onClick(DialogInterface dialog, int which) {
        if (dialog == mAdbKeysDialog) {
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
                mSettingsEnabler.enableDevelopmentSettings();
                setPrefsEnabledState(true);
            } else {
                // Reset the toggle
                mSwitchBar.setChecked(false);
            }
        } else if (dialog == mLogpersistClearDialog) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                setLogpersistOff(true);
            } else {
                updateLogpersistValues();
            }
        } else if (dialog == mAdbTcpDialog) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                LineageSettings.Secure.putInt(getActivity().getContentResolver(),
                        LineageSettings.Secure.ADB_PORT, 5555);
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
        if (dialog == mEnableDialog) {
            if (!mDialogClicked) {
                mSwitchBar.setChecked(false);
            }
            mEnableDialog = null;
        } else if (dialog == mLogpersistClearDialog) {
            mLogpersistClearDialog = null;
        } else if (dialog == mAdbTcpDialog) {
            updateAdbOverNetwork();
            mAdbTcpDialog = null;
        } else if (dialog == mRootDialog) {
            updateRootAccessOptions();
            mRootDialog = null;
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

    private BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateUsbConfigurationValues();
        }
    };

    private BroadcastReceiver mBluetoothA2dpReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "mBluetoothA2dpReceiver.onReceive intent=" + intent);
            String action = intent.getAction();

            if (BluetoothA2dp.ACTION_CODEC_CONFIG_CHANGED.equals(action)) {
                BluetoothCodecStatus codecStatus =
                    (BluetoothCodecStatus)intent.getParcelableExtra(BluetoothCodecStatus.EXTRA_CODEC_STATUS);
                Log.d(TAG, "Received BluetoothCodecStatus=" + codecStatus);
                updateBluetoothA2dpConfigurationValues();
            }
        }
    };

    private BluetoothProfile.ServiceListener mBluetoothA2dpServiceListener =
        new BluetoothProfile.ServiceListener() {
            public void onServiceConnected(int profile,
                                           BluetoothProfile proxy) {
                synchronized (mBluetoothA2dpLock) {
                    mBluetoothA2dp = (BluetoothA2dp) proxy;
                }
                updateBluetoothA2dpConfigurationValues();
            }

            public void onServiceDisconnected(int profile) {
                synchronized (mBluetoothA2dpLock) {
                    mBluetoothA2dp = null;
                }
                updateBluetoothA2dpConfigurationValues();
            }
        };

    public static class SystemPropPoker extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            String[] services = ServiceManager.listServices();
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

    private static boolean isPackageInstalled(Context context, String packageName) {
        try {
            return context.getPackageManager().getPackageInfo(packageName, 0) != null;
        } catch (NameNotFoundException e) {
            return false;
        }
    }


    /**
     * For Search.
     */
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return context.getSharedPreferences(DevelopmentSettings.PREF_FILE,
                            Context.MODE_PRIVATE).getBoolean(
                            DevelopmentSettings.PREF_SHOW,
                            android.os.Build.TYPE.equals("eng"));
                }

                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {

                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.development_prefs;
                    return Arrays.asList(sir);
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    final List<String> keys = super.getNonIndexableKeys(context);

                    if (!showEnableOemUnlockPreference(context)) {
                        keys.add(ENABLE_OEM_UNLOCK);
                    }
                    return keys;
                }
            };

    private void resetShortcutManagerThrottling() {
        final IShortcutService service = IShortcutService.Stub.asInterface(
                ServiceManager.getService(Context.SHORTCUT_SERVICE));
        if (service != null) {
            try {
                service.resetThrottling();
                Toast.makeText(getActivity(), R.string.reset_shortcut_manager_throttling_complete,
                        Toast.LENGTH_SHORT).show();
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to reset rate limiting", e);
            }
        }
    }

    private void updateOemUnlockSettingDescription() {
        if (mEnableOemUnlock != null) {
            int oemUnlockSummary = R.string.oem_unlock_enable_summary;
            if (isBootloaderUnlocked()) {
                oemUnlockSummary = R.string.oem_unlock_enable_disabled_summary_bootloader_unlocked;
            } else if (isSimLockedDevice()) {
                oemUnlockSummary = R.string.oem_unlock_enable_disabled_summary_sim_locked_device;
            } else if (!isOemUnlockAllowedByUserAndCarrier()) {
                // If the device isn't SIM-locked but OEM unlock is disallowed by some party, this
                // means either some other carrier restriction is in place or the device hasn't been
                // able to confirm which restrictions (SIM-lock or otherwise) apply.
                oemUnlockSummary =
                        R.string.oem_unlock_enable_disabled_summary_connectivity_or_locked;
            }
            mEnableOemUnlock.setSummary(getString(oemUnlockSummary));
        }
    }

    /** Returns {@code true} if the device is SIM-locked. Otherwise, returns {@code false}. */
    private boolean isSimLockedDevice() {
        int phoneCount = mTelephonyManager.getPhoneCount();
        for (int i = 0; i < phoneCount; i++) {
            if (mTelephonyManager.getAllowedCarriers(i).size() > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if the bootloader has been unlocked. Otherwise, returns {code false}.
     */
    private boolean isBootloaderUnlocked() {
        return mOemLockManager.isDeviceOemUnlocked();
    }


}
