/*
 * Copyright (C) 2007 The Android Open Source Project
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
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.security.KeyStore;
import android.service.trust.TrustAgentService;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.rr.SeekBarPreference;
import com.android.settings.Settings.LockScreenSettingsActivity;
import com.android.settings.TrustAgentUtils.TrustAgentComponentInfo;
import com.android.settings.cyanogenmod.LiveLockScreenSettings;
import com.android.settings.fingerprint.FingerprintEnrollIntroduction;
import com.android.settings.fingerprint.FingerprintSettings;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Index;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import cyanogenmod.providers.CMSettings;

import java.util.ArrayList;
import java.util.List;

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;
import static cyanogenmod.content.Intent.ACTION_OPEN_LIVE_LOCKSCREEN_SETTINGS;

/**
 * Gesture lock pattern settings.
 */
public class SecuritySettings extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener, DialogInterface.OnClickListener, Indexable {

    private static final String TAG = "SecuritySettings";
    private static final String TRUST_AGENT_CLICK_INTENT = "trust_agent_click_intent";
    private static final Intent TRUST_AGENT_INTENT =
            new Intent(TrustAgentService.SERVICE_INTERFACE);

    // Fitler types for this panel
    private static final String FILTER_TYPE_EXTRA = "filter_type";
    private static final int TYPE_LOCKSCREEN_EXTRA = 0;
    private static final int TYPE_SECURITY_EXTRA = 1;
    private static final int TYPE_EXTERNAL_RESOLUTION = 2;

    // Lock Settings
    private static final String KEY_UNLOCK_SET_OR_CHANGE = "unlock_set_or_change";
    private static final String KEY_VISIBLE_PATTERN = "visiblepattern";
    private static final String KEY_VISIBLE_ERROR_PATTERN = "visible_error_pattern";
    private static final String KEY_VISIBLE_DOTS = "visibledots";
    private static final String KEY_SECURITY_CATEGORY = "security_category";
    private static final String KEY_DEVICE_ADMIN_CATEGORY = "device_admin_category";
    private static final String KEY_LOCK_AFTER_TIMEOUT = "lock_after_timeout";
    private static final String KEY_OWNER_INFO_SETTINGS = "owner_info_settings";
    private static final String KEY_ADVANCED_SECURITY = "advanced_security";
    private static final String KEY_MANAGE_TRUST_AGENTS = "manage_trust_agents";
    private static final String KEY_FINGERPRINT_SETTINGS = "fingerprint_settings";
    private static final String KEY_DIRECTLY_SHOW = "directlyshow";

    private static final String KEY_LOCKSCREEN_ENABLED_INTERNAL = "lockscreen_enabled_internally";

    private static final int SET_OR_CHANGE_LOCK_METHOD_REQUEST = 123;
    private static final int CHANGE_TRUST_AGENT_SETTINGS = 126;

    // Misc Settings
    private static final String KEY_SIM_LOCK = "sim_lock";
    private static final String KEY_SIM_LOCK_SETTINGS = "sim_lock_settings";
    private static final String KEY_SHOW_PASSWORD = "show_password";
    private static final String KEY_CREDENTIAL_STORAGE_TYPE = "credential_storage_type";
    private static final String KEY_RESET_CREDENTIALS = "credentials_reset";
    private static final String KEY_CREDENTIALS_INSTALL = "credentials_install";
    private static final String KEY_TOGGLE_INSTALL_APPLICATIONS = "toggle_install_applications";
    private static final String KEY_POWER_INSTANTLY_LOCKS = "power_button_instantly_locks";
    private static final String KEY_CREDENTIALS_MANAGER = "credentials_management";
    private static final String PACKAGE_MIME_TYPE = "application/vnd.android.package-archive";
    private static final String KEY_TRUST_AGENT = "trust_agent";
    private static final String KEY_SCREEN_PINNING = "screen_pinning_settings";
    private static final String KEY_SMS_SECURITY_CHECK_PREF = "sms_security_check_limit";
    private static final String KEY_GENERAL_CATEGORY = "general_category";
    private static final String KEY_LIVE_LOCK_SCREEN = "live_lock_screen";
    private static final String KEY_PACKAGE_INSTALL_OVERLAY_CHECK = "toggle_package_install_overlay_check";

    // These switch preferences need special handling since they're not all stored in Settings.
    private static final String SWITCH_PREFERENCE_KEYS[] = { KEY_LOCK_AFTER_TIMEOUT,
            KEY_VISIBLE_PATTERN, KEY_POWER_INSTANTLY_LOCKS, KEY_DIRECTLY_SHOW, KEY_VISIBLE_ERROR_PATTERN, KEY_VISIBLE_DOTS,
            KEY_POWER_INSTANTLY_LOCKS, KEY_SHOW_PASSWORD, KEY_TOGGLE_INSTALL_APPLICATIONS };

    // Only allow one trust agent on the platform.
    private static final boolean ONLY_ONE_TRUST_AGENT = true;

    private static final int MY_USER_ID = UserHandle.myUserId();

    private static final String LIVE_LOCK_SCREEN_FEATURE = "org.cyanogenmod.livelockscreen";

    private PackageManager mPM;
    private DevicePolicyManager mDPM;
    private SubscriptionManager mSubscriptionManager;

    private ChooseLockSettingsHelper mChooseLockSettingsHelper;
    private LockPatternUtils mLockPatternUtils;
    private ListPreference mLockAfter;

    private SwitchPreference mVisiblePattern;
    private SwitchPreference mDirectlyShow;
    private SwitchPreference mVisibleErrorPattern;
    private SwitchPreference mVisibleDots;

    private SwitchPreference mShowPassword;

    private KeyStore mKeyStore;
    private Preference mResetCredentials;

    private SwitchPreference mToggleAppInstallation;
    private DialogInterface mWarnInstallApps;
    private SwitchPreference mPowerButtonInstantlyLocks;
    private ListPreference mSmsSecurityCheck;
    private SwitchPreference mPackageInstallOverlayCheck;

    private boolean mIsPrimary;

    private Intent mTrustAgentClickIntent;

    private Preference mOwnerInfoPref;
    private int mFilterType = TYPE_SECURITY_EXTRA;

    private Preference mLockscreenDisabledPreference;

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.SECURITY;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Ugly hack for legacy shortcuts :'(
        Intent intent = getActivity().getIntent();
        ComponentName componentName = intent.getComponent();
        if (componentName.getClassName().equals(
                LockScreenSettingsActivity.class.getName())) {
            mFilterType = TYPE_LOCKSCREEN_EXTRA;
        } else {
            Bundle bundle = getArguments();
            if (bundle != null) {
                mFilterType = bundle.getInt(FILTER_TYPE_EXTRA, TYPE_SECURITY_EXTRA);
            }
        }

        Bundle extras = getActivity().getIntent().getExtras();
        // Even uglier hack to make cts verifier expectations make sense.
        if (extras != null && extras.get(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS) != null &&
                extras.get(SettingsActivity.EXTRA_SHOW_FRAGMENT_AS_SHORTCUT) == null) {
            mFilterType = TYPE_EXTERNAL_RESOLUTION;
        }

        mSubscriptionManager = SubscriptionManager.from(getActivity());

        mLockPatternUtils = new LockPatternUtils(getActivity());

        mDPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);

        mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());

        if (savedInstanceState != null
                && savedInstanceState.containsKey(TRUST_AGENT_CLICK_INTENT)) {
            mTrustAgentClickIntent = savedInstanceState.getParcelable(TRUST_AGENT_CLICK_INTENT);
        }
    }

    private static int getResIdForLockUnlockScreen(Context context,
            LockPatternUtils lockPatternUtils) {
        int resid = 0;
        if (!lockPatternUtils.isSecure(MY_USER_ID)) {
            if (lockPatternUtils.isLockScreenDisabled(MY_USER_ID)) {
                resid = R.xml.security_settings_lockscreen;
            } else {
                resid = R.xml.security_settings_chooser;
            }
        } else {
            switch (lockPatternUtils.getKeyguardStoredPasswordQuality(MY_USER_ID)) {
                case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                    resid = R.xml.security_settings_pattern;
                    break;
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
                    resid = R.xml.security_settings_pin;
                    break;
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
                    resid = R.xml.security_settings_password;
                    break;
            }
        }
        return resid;
    }

    /**
     * Important!
     *
     * Don't forget to update the SecuritySearchIndexProvider if you are doing any change in the
     * logic or adding/removing preferences here.
     */
    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.security_settings);
        root = getPreferenceScreen();

        // Add package manager to check if features are available
        PackageManager pm = getPackageManager();

        // Add options for device encryption
        mIsPrimary = MY_USER_ID == UserHandle.USER_OWNER;

        if (CMSettings.Secure.getIntForUser(getContentResolver(),
                CMSettings.Secure.LOCKSCREEN_INTERNALLY_ENABLED, 1, UserHandle.USER_OWNER) != 1) {
            // lock screen is disabled by quick settings tile, let the user know!~
            mLockscreenDisabledPreference = new Preference(getActivity());
            mLockscreenDisabledPreference.setKey(KEY_LOCKSCREEN_ENABLED_INTERNAL);
            mLockscreenDisabledPreference.setTitle(R.string.lockscreen_disabled_by_qs_tile_title);
            mLockscreenDisabledPreference.setSummary(R.string.lockscreen_disabled_by_qs_tile_summary);
            root.addPreference(mLockscreenDisabledPreference);
        }

        final boolean securityOrExternal = mFilterType == TYPE_SECURITY_EXTRA
                || mFilterType == TYPE_EXTERNAL_RESOLUTION;
        final boolean lockscreenOrExternal = mFilterType == TYPE_SECURITY_EXTRA
                || mFilterType == TYPE_EXTERNAL_RESOLUTION;

        if (lockscreenOrExternal) {
            // Add options for lock/unlock screen
            final int resid = getResIdForLockUnlockScreen(getActivity(), mLockPatternUtils);
            addPreferencesFromResource(resid);

            mOwnerInfoPref = findPreference(KEY_OWNER_INFO_SETTINGS);
            if (mOwnerInfoPref != null) {
                mOwnerInfoPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        OwnerInfoSettings.show(SecuritySettings.this);
                        return true;
                    }
                });
            }
        }

        if (mIsPrimary && securityOrExternal) {
            if (LockPatternUtils.isDeviceEncryptionEnabled()) {
                // The device is currently encrypted.
                addPreferencesFromResource(R.xml.security_settings_encrypted);
            } else {
                // This device supports encryption but isn't encrypted.
                addPreferencesFromResource(R.xml.security_settings_unencrypted);
            }
        }

        if (lockscreenOrExternal) {
            // Fingerprint and trust agents
            PreferenceGroup securityCategory = (PreferenceGroup)
                    root.findPreference(KEY_SECURITY_CATEGORY);
            if (securityCategory != null) {
                maybeAddFingerprintPreference(securityCategory);
                addTrustAgentSettings(securityCategory);
            }

            // lock after preference
            mLockAfter = (ListPreference) root.findPreference(KEY_LOCK_AFTER_TIMEOUT);
            if (mLockAfter != null) {
                setupLockAfterPreference();
                updateLockAfterPreferenceSummary();
            }

            // visible pattern
            mVisiblePattern = (SwitchPreference) root.findPreference(KEY_VISIBLE_PATTERN);

            // visible error pattern
            mVisibleErrorPattern = (SwitchPreference) root.findPreference(
                    KEY_VISIBLE_ERROR_PATTERN);

            // visible dots
            mVisibleDots = (SwitchPreference) root.findPreference(KEY_VISIBLE_DOTS);

            // lock instantly on power key press
            mPowerButtonInstantlyLocks = (SwitchPreference) root.findPreference(
                    KEY_POWER_INSTANTLY_LOCKS);
            Preference trustAgentPreference = root.findPreference(KEY_TRUST_AGENT);
            if (mPowerButtonInstantlyLocks != null &&
                    trustAgentPreference != null &&
                    trustAgentPreference.getTitle().length() > 0) {
                mPowerButtonInstantlyLocks.setSummary(getString(
                        R.string.lockpattern_settings_power_button_instantly_locks_summary,
                        trustAgentPreference.getTitle()));
            }

            // Add live lock screen preference if supported
            PreferenceGroup generalCategory = (PreferenceGroup)
                    root.findPreference(KEY_GENERAL_CATEGORY);
            if (pm.hasSystemFeature(LIVE_LOCK_SCREEN_FEATURE) && generalCategory != null) {
                boolean moveToTop = getResources().getBoolean(
                        R.bool.config_showLiveLockScreenSettingsFirst);

                PreferenceGroup groupToAddTo = moveToTop ? root : generalCategory;
                Preference liveLockPreference = new Preference(getContext(), null);
                liveLockPreference.setIntent(new Intent(ACTION_OPEN_LIVE_LOCKSCREEN_SETTINGS));
                liveLockPreference.setOrder(-1);
                setLiveLockScreenPreferenceTitleAndSummary(liveLockPreference);
                groupToAddTo.addPreference(liveLockPreference);
            }
        }

        if (securityOrExternal) {
            // Append the rest of the settings
            addPreferencesFromResource(R.xml.security_settings_misc);


        // directly show
        mDirectlyShow = (SwitchPreference) root.findPreference(KEY_DIRECTLY_SHOW);

        // lock instantly on power key press
        mPowerButtonInstantlyLocks = (SwitchPreference) root.findPreference(
                KEY_POWER_INSTANTLY_LOCKS);
        Preference trustAgentPreference = root.findPreference(KEY_TRUST_AGENT);
        if (mPowerButtonInstantlyLocks != null &&
                trustAgentPreference != null &&
                trustAgentPreference.getTitle().length() > 0) {
            mPowerButtonInstantlyLocks.setSummary(getString(
                    R.string.lockpattern_settings_power_button_instantly_locks_summary,
                    trustAgentPreference.getTitle()));
        }
            // Do not display SIM lock for devices without an Icc card
            CarrierConfigManager cfgMgr = (CarrierConfigManager)
                    getActivity().getSystemService(Context.CARRIER_CONFIG_SERVICE);
            PersistableBundle b = cfgMgr.getConfig();
            PreferenceGroup iccLockGroup = (PreferenceGroup) root.findPreference(KEY_SIM_LOCK);
            Preference iccLock = root.findPreference(KEY_SIM_LOCK_SETTINGS);

            if (!mIsPrimary
                    || b.getBoolean(CarrierConfigManager.KEY_HIDE_SIM_LOCK_SETTINGS_BOOL)) {
                root.removePreference(iccLockGroup);
            } else {
                SubscriptionManager subMgr = SubscriptionManager.from(getActivity());
                TelephonyManager tm = TelephonyManager.getDefault();
                int numPhones = tm.getPhoneCount();
                boolean hasAnySim = false;

                for (int i = 0; i < numPhones; i++) {
                    final Preference pref;

                    if (numPhones > 1) {
                        SubscriptionInfo sir = subMgr.getActiveSubscriptionInfoForSimSlotIndex(i);
                        if (sir == null) {
                            continue;
                        }

                        pref = new Preference(getActivity());
                        pref.setOrder(iccLock.getOrder());
                        pref.setTitle(getString(R.string.sim_card_lock_settings_title, i + 1));
                        pref.setSummary(sir.getDisplayName());

                        Intent intent = new Intent(getActivity(), IccLockSettings.class);
                        intent.putExtra(IccLockSettings.EXTRA_SUB_ID, sir.getSubscriptionId());
                        intent.putExtra(IccLockSettings.EXTRA_SUB_DISPLAY_NAME,
                                sir.getDisplayName());
                        pref.setIntent(intent);

                        iccLockGroup.addPreference(pref);
                    } else {
                        pref = iccLock;
                    }

                    // Do not display SIM lock for devices without an Icc card
                    hasAnySim |= tm.hasIccCard(i);

                    int simState = tm.getSimState(i);
                    boolean simPresent = simState != TelephonyManager.SIM_STATE_ABSENT
                            && simState != TelephonyManager.SIM_STATE_UNKNOWN
                            && simState != TelephonyManager.SIM_STATE_CARD_IO_ERROR;
                    if (!simPresent) {
                        pref.setEnabled(false);
                    }
                }

                if (!hasAnySim) {
                    root.removePreference(iccLockGroup);
                } else if (numPhones > 1) {
                    iccLockGroup.removePreference(iccLock);
                }
            }

            if (Settings.System.getInt(getContentResolver(),
                    Settings.System.LOCK_TO_APP_ENABLED, 0) != 0) {
                root.findPreference(KEY_SCREEN_PINNING).setSummary(
                        getResources().getString(R.string.switch_on_text));
            }
            // SMS rate limit security check
            boolean isTelephony = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
            if (isTelephony) {
                mSmsSecurityCheck = (ListPreference) root.findPreference(KEY_SMS_SECURITY_CHECK_PREF);
                mSmsSecurityCheck.setOnPreferenceChangeListener(this);
                int smsSecurityCheck = Integer.valueOf(mSmsSecurityCheck.getValue());
                updateSmsSecuritySummary(smsSecurityCheck);
            }

            // Show password
            mShowPassword = (SwitchPreference) root.findPreference(KEY_SHOW_PASSWORD);
            mResetCredentials = root.findPreference(KEY_RESET_CREDENTIALS);

            // Credential storage
  final UserManager um = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);

            mKeyStore = KeyStore.getInstance(); // needs to be initialized for onResume()
            if (!um.hasUserRestriction(UserManager.DISALLOW_CONFIG_CREDENTIALS)) {
                Preference credentialStorageType = root.findPreference(KEY_CREDENTIAL_STORAGE_TYPE);

                final int storageSummaryRes =
                        mKeyStore.isHardwareBacked() ? R.string.credential_storage_type_hardware
                                : R.string.credential_storage_type_software;
                credentialStorageType.setSummary(storageSummaryRes);
            } else {
                PreferenceGroup credentialsManager = (PreferenceGroup)
                        root.findPreference(KEY_CREDENTIALS_MANAGER);
                credentialsManager.removePreference(root.findPreference(KEY_RESET_CREDENTIALS));
                credentialsManager.removePreference(root.findPreference(KEY_CREDENTIALS_INSTALL));
                credentialsManager.removePreference(root.findPreference(KEY_CREDENTIAL_STORAGE_TYPE));
            }

        // Advanced Security features
        PreferenceGroup advancedCategory =
                (PreferenceGroup)root.findPreference(KEY_ADVANCED_SECURITY);
        if (advancedCategory != null) {
            Preference manageAgents = advancedCategory.findPreference(KEY_MANAGE_TRUST_AGENTS);
            if (manageAgents != null && !mLockPatternUtils.isSecure(MY_USER_ID)) {
                manageAgents.setEnabled(false);
                manageAgents.setSummary(R.string.disabled_because_no_backup_security);
            }
        }

	mPackageInstallOverlayCheck = (SwitchPreference) findPreference(
                KEY_PACKAGE_INSTALL_OVERLAY_CHECK);

        // the index. This call is expected to be fairly cheap, but we may want to do something
        // smarter in the future.
        Index.getInstance(getActivity())
                .updateFromClassNameResource(SecuritySettings.class.getName(), true, true);

            // Application install
            PreferenceGroup deviceAdminCategory = (PreferenceGroup)
                    root.findPreference(KEY_DEVICE_ADMIN_CATEGORY);
            mToggleAppInstallation = (SwitchPreference) findPreference(
                    KEY_TOGGLE_INSTALL_APPLICATIONS);
            mToggleAppInstallation.setChecked(isNonMarketAppsAllowed());
            // Side loading of apps.
            // Disable for restricted profiles. For others, check if policy disallows it.
            mToggleAppInstallation.setEnabled(!um.getUserInfo(MY_USER_ID).isRestricted());
            if (um.hasUserRestriction(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)
                    || um.hasUserRestriction(UserManager.DISALLOW_INSTALL_APPS)) {
                mToggleAppInstallation.setEnabled(false);
            }


            // The above preferences come and go based on security state, so we need to update
            // the index. This call is expected to be fairly cheap, but we may want to do something
            // smarter in the future.
            Index.getInstance(getActivity())
                    .updateFromClassNameResource(SecuritySettings.class.getName(), true, true);
        }
        for (int i = 0; i < SWITCH_PREFERENCE_KEYS.length; i++) {
            final Preference pref = findPreference(SWITCH_PREFERENCE_KEYS[i]);
            if (pref != null) pref.setOnPreferenceChangeListener(this);
        }

        return root;
    }

    private void maybeAddFingerprintPreference(PreferenceGroup securityCategory) {
        FingerprintManager fpm = (FingerprintManager) getActivity().getSystemService(
                Context.FINGERPRINT_SERVICE);
        if (!fpm.isHardwareDetected()) {
            Log.v(TAG, "No fingerprint hardware detected!!");
            return;
        }
        Preference fingerprintPreference = new Preference(securityCategory.getContext());
        fingerprintPreference.setKey(KEY_FINGERPRINT_SETTINGS);
        fingerprintPreference.setTitle(R.string.security_settings_fingerprint_preference_title);
        Intent intent = new Intent();
        final List<Fingerprint> items = fpm.getEnrolledFingerprints();
        final int fingerprintCount = items != null ? items.size() : 0;
        final String clazz;
        if (fingerprintCount > 0) {
            fingerprintPreference.setSummary(getResources().getQuantityString(
                    R.plurals.security_settings_fingerprint_preference_summary,
                    fingerprintCount, fingerprintCount));
            clazz = FingerprintSettings.class.getName();
        } else {
            fingerprintPreference.setSummary(
                    R.string.security_settings_fingerprint_preference_summary_none);
            clazz = FingerprintEnrollIntroduction.class.getName();
        }
        intent.setClassName("com.android.settings", clazz);
        fingerprintPreference.setIntent(intent);
        securityCategory.addPreference(fingerprintPreference);
    }

    private void addTrustAgentSettings(PreferenceGroup securityCategory) {
        final boolean hasSecurity = mLockPatternUtils.isSecure(MY_USER_ID);
        ArrayList<TrustAgentComponentInfo> agents =
                getActiveTrustAgents(getPackageManager(), mLockPatternUtils, mDPM);
        for (int i = 0; i < agents.size(); i++) {
            final TrustAgentComponentInfo agent = agents.get(i);
            Preference trustAgentPreference =
                    new Preference(securityCategory.getContext());
            trustAgentPreference.setKey(KEY_TRUST_AGENT);
            trustAgentPreference.setTitle(agent.title);
            trustAgentPreference.setSummary(agent.summary);
            // Create intent for this preference.
            Intent intent = new Intent();
            intent.setComponent(agent.componentName);
            intent.setAction(Intent.ACTION_MAIN);
            trustAgentPreference.setIntent(intent);
            // Add preference to the settings menu.
            securityCategory.addPreference(trustAgentPreference);

            if (agent.disabledByAdministrator) {
                trustAgentPreference.setEnabled(false);
                trustAgentPreference.setSummary(R.string.trust_agent_disabled_device_admin);
            } else if (!hasSecurity) {
                trustAgentPreference.setEnabled(false);
                trustAgentPreference.setSummary(R.string.disabled_because_no_backup_security);
            }
        }
    }

    private static ArrayList<TrustAgentComponentInfo> getActiveTrustAgents(
            PackageManager pm, LockPatternUtils utils, DevicePolicyManager dpm) {
        ArrayList<TrustAgentComponentInfo> result = new ArrayList<TrustAgentComponentInfo>();
        List<ResolveInfo> resolveInfos = pm.queryIntentServices(TRUST_AGENT_INTENT,
                PackageManager.GET_META_DATA);
        List<ComponentName> enabledTrustAgents = utils.getEnabledTrustAgents(MY_USER_ID);

        boolean disableTrustAgents = (dpm.getKeyguardDisabledFeatures(null)
                & DevicePolicyManager.KEYGUARD_DISABLE_TRUST_AGENTS) != 0;

        if (enabledTrustAgents != null && !enabledTrustAgents.isEmpty()) {
            for (int i = 0; i < resolveInfos.size(); i++) {
                ResolveInfo resolveInfo = resolveInfos.get(i);
                if (resolveInfo.serviceInfo == null) continue;
                if (!TrustAgentUtils.checkProvidePermission(resolveInfo, pm)) continue;
                TrustAgentComponentInfo trustAgentComponentInfo =
                        TrustAgentUtils.getSettingsComponent(pm, resolveInfo);
                if (trustAgentComponentInfo.componentName == null ||
                        !enabledTrustAgents.contains(
                                TrustAgentUtils.getComponentName(resolveInfo)) ||
                        TextUtils.isEmpty(trustAgentComponentInfo.title)) continue;
                if (disableTrustAgents && dpm.getTrustAgentConfiguration(
                        null, TrustAgentUtils.getComponentName(resolveInfo)) == null) {
                    trustAgentComponentInfo.disabledByAdministrator = true;
                }
                result.add(trustAgentComponentInfo);
                if (ONLY_ONE_TRUST_AGENT) break;
            }
        }
        return result;
    }

    private boolean isNonMarketAppsAllowed() {
        return Settings.Global.getInt(getContentResolver(),
                                      Settings.Global.INSTALL_NON_MARKET_APPS, 0) > 0;
    }

    private void setNonMarketAppsAllowed(boolean enabled) {
        final UserManager um = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);
        if (um.hasUserRestriction(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)) {
            return;
        }
        // Change the system setting
        Settings.Global.putInt(getContentResolver(), Settings.Global.INSTALL_NON_MARKET_APPS,
                                enabled ? 1 : 0);
    }

    private void warnAppInstallation() {
        // TODO: DialogFragment?
        mWarnInstallApps = new AlertDialog.Builder(getActivity()).setTitle(
                getResources().getString(R.string.error_title))
                .setIcon(com.android.internal.R.drawable.ic_dialog_alert)
                .setMessage(getResources().getString(R.string.install_all_warning))
                .setPositiveButton(android.R.string.yes, this)
                .setNegativeButton(android.R.string.no, this)
                .show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (dialog == mWarnInstallApps) {
            boolean turnOn = which == DialogInterface.BUTTON_POSITIVE;
            setNonMarketAppsAllowed(turnOn);
            if (mToggleAppInstallation != null) {
                mToggleAppInstallation.setChecked(turnOn);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mWarnInstallApps != null) {
            mWarnInstallApps.dismiss();
        }
    }

    private void updateSmsSecuritySummary(int selection) {
        String message = selection > 0
                ? getString(R.string.sms_security_check_limit_summary, selection)
                : getString(R.string.sms_security_check_limit_summary_none);
        mSmsSecurityCheck.setSummary(message);
    }

    private void setupLockAfterPreference() {
        // Compatible with pre-Froyo
        long currentTimeout = Settings.Secure.getLong(getContentResolver(),
                Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, 5000);
        mLockAfter.setValue(String.valueOf(currentTimeout));
        mLockAfter.setOnPreferenceChangeListener(this);
        final long adminTimeout = (mDPM != null ? mDPM.getMaximumTimeToLock(null) : 0);
        final long displayTimeout = Math.max(0,
                Settings.System.getInt(getContentResolver(), SCREEN_OFF_TIMEOUT, 0));
        if (adminTimeout > 0) {
            // This setting is a slave to display timeout when a device policy is enforced.
            // As such, maxLockTimeout = adminTimeout - displayTimeout.
            // If there isn't enough time, shows "immediately" setting.
            disableUnusableTimeouts(Math.max(0, adminTimeout - displayTimeout));
        }
    }

    private void updateLockAfterPreferenceSummary() {
        // Update summary message with current value
        long currentTimeout = Settings.Secure.getLong(getContentResolver(),
                Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, 5000);
        final CharSequence[] entries = mLockAfter.getEntries();
        final CharSequence[] values = mLockAfter.getEntryValues();
        int best = 0;
        for (int i = 0; i < values.length; i++) {
            long timeout = Long.valueOf(values[i].toString());
            if (currentTimeout >= timeout) {
                best = i;
            }
        }

        Preference preference = getPreferenceScreen().findPreference(KEY_TRUST_AGENT);
        if (preference != null && preference.getTitle().length() > 0) {
            if (Long.valueOf(values[best].toString()) == 0) {
                mLockAfter.setSummary(getString(R.string.lock_immediately_summary_with_exception,
                        preference.getTitle()));
            } else {
                mLockAfter.setSummary(getString(R.string.lock_after_timeout_summary_with_exception,
                        entries[best], preference.getTitle()));
            }
        } else {
            mLockAfter.setSummary(getString(R.string.lock_after_timeout_summary, entries[best]));
        }
    }

    private void disableUnusableTimeouts(long maxTimeout) {
        final CharSequence[] entries = mLockAfter.getEntries();
        final CharSequence[] values = mLockAfter.getEntryValues();
        ArrayList<CharSequence> revisedEntries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> revisedValues = new ArrayList<CharSequence>();
        for (int i = 0; i < values.length; i++) {
            long timeout = Long.valueOf(values[i].toString());
            if (timeout <= maxTimeout) {
                revisedEntries.add(entries[i]);
                revisedValues.add(values[i]);
            }
        }
        if (revisedEntries.size() != entries.length || revisedValues.size() != values.length) {
            mLockAfter.setEntries(
                    revisedEntries.toArray(new CharSequence[revisedEntries.size()]));
            mLockAfter.setEntryValues(
                    revisedValues.toArray(new CharSequence[revisedValues.size()]));
            final int userPreference = Integer.valueOf(mLockAfter.getValue());
            if (userPreference <= maxTimeout) {
                mLockAfter.setValue(String.valueOf(userPreference));
            } else {
                // There will be no highlighted selection since nothing in the list matches
                // maxTimeout. The user can still select anything less than maxTimeout.
                // TODO: maybe append maxTimeout to the list and mark selected.
            }
        }
        mLockAfter.setEnabled(revisedEntries.size() > 0);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mTrustAgentClickIntent != null) {
            outState.putParcelable(TRUST_AGENT_CLICK_INTENT, mTrustAgentClickIntent);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Make sure we reload the preference hierarchy since some of these settings
        // depend on others...
        createPreferenceHierarchy();

        final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();
        if (mVisiblePattern != null) {
          mVisiblePattern.setChecked(lockPatternUtils.isVisiblePatternEnabled(MY_USER_ID));
        }
        if (mVisibleErrorPattern != null) {
            mVisibleErrorPattern.setChecked(lockPatternUtils.isShowErrorPath(MY_USER_ID));
        }
        if (mVisibleDots != null) {
            mVisibleDots.setChecked(lockPatternUtils.isVisibleDotsEnabled(MY_USER_ID));
        }
        if (mPowerButtonInstantlyLocks != null) {
            mPowerButtonInstantlyLocks.setChecked(lockPatternUtils.getPowerButtonInstantlyLocks(
                    MY_USER_ID));
        }

        if (mDirectlyShow != null) {
            mDirectlyShow.setChecked(lockPatternUtils.shouldPassToSecurityView(
                    MY_USER_ID));
        }

        if (mShowPassword != null) {
            mShowPassword.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.TEXT_SHOW_PASSWORD, 1) != 0);
        }

        if (mResetCredentials != null) {
            mResetCredentials.setEnabled(!mKeyStore.isEmpty());
        }

        if (mPackageInstallOverlayCheck != null) {
            mPackageInstallOverlayCheck.setChecked(Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.PACKAGE_INSTALL_OVERLAY_CHECK_DISABLED, 0) != 0);
        }

        updateOwnerInfo();
    }

    public void updateOwnerInfo() {
        if (mOwnerInfoPref != null) {
            mOwnerInfoPref.setSummary(mLockPatternUtils.isOwnerInfoEnabled(MY_USER_ID)
                    ? mLockPatternUtils.getOwnerInfo(MY_USER_ID)
                    : getString(R.string.owner_info_settings_summary));
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        final String key = preference.getKey();
        if (KEY_UNLOCK_SET_OR_CHANGE.equals(key)) {
            startFragment(this, "com.android.settings.ChooseLockGeneric$ChooseLockGenericFragment",
                    R.string.lock_settings_picker_title, SET_OR_CHANGE_LOCK_METHOD_REQUEST, null);
        } else if (KEY_TRUST_AGENT.equals(key)) {
            ChooseLockSettingsHelper helper =
                    new ChooseLockSettingsHelper(this.getActivity(), this);
            mTrustAgentClickIntent = preference.getIntent();
            boolean confirmationLaunched = helper.launchConfirmationActivity(
                    CHANGE_TRUST_AGENT_SETTINGS, preference.getTitle());
            if (!confirmationLaunched && mTrustAgentClickIntent != null) {
                // If this returns false, it means no password confirmation is required.
                startActivity(mTrustAgentClickIntent);
                mTrustAgentClickIntent = null;
            }
        } else if (KEY_LOCKSCREEN_ENABLED_INTERNAL.equals(key)) {
            CMSettings.Secure.putIntForUser(getActivity().getContentResolver(),
                    CMSettings.Secure.LOCKSCREEN_INTERNALLY_ENABLED,
                    1, UserHandle.USER_CURRENT);
            mLockscreenDisabledPreference.setEnabled(false);
            mLockscreenDisabledPreference.setSummary(
                    R.string.lockscreen_disabled_by_qs_tile_summary_enabled);
        } else if (KEY_PACKAGE_INSTALL_OVERLAY_CHECK.equals(key)) {
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.PACKAGE_INSTALL_OVERLAY_CHECK_DISABLED,
                    mPackageInstallOverlayCheck.isChecked() ? 1 : 0);
        } else {
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        return true;
    }

    /**
     * see confirmPatternThenDisableAndClear
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHANGE_TRUST_AGENT_SETTINGS && resultCode == Activity.RESULT_OK) {
            if (mTrustAgentClickIntent != null) {
                startActivity(mTrustAgentClickIntent);
                mTrustAgentClickIntent = null;
            }
            return;
        }
        createPreferenceHierarchy();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        boolean result = true;
        final String key = preference.getKey();
        final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();
        if (KEY_LOCK_AFTER_TIMEOUT.equals(key)) {
            int timeout = Integer.parseInt((String) value);
            try {
                Settings.Secure.putInt(getContentResolver(),
                        Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, timeout);
            } catch (NumberFormatException e) {
                Log.e("SecuritySettings", "could not persist lockAfter timeout setting", e);
            }
            updateLockAfterPreferenceSummary();
        } else if (KEY_VISIBLE_PATTERN.equals(key)) {
            lockPatternUtils.setVisiblePatternEnabled((Boolean) value, MY_USER_ID);
        } else if (KEY_DIRECTLY_SHOW.equals(key)) {
            lockPatternUtils.setPassToSecurityView((Boolean) value, MY_USER_ID);
        } else if (KEY_VISIBLE_ERROR_PATTERN.equals(key)) {
            lockPatternUtils.setShowErrorPath((Boolean) value, MY_USER_ID);
        } else if (KEY_VISIBLE_DOTS.equals(key)) {
            lockPatternUtils.setVisibleDotsEnabled((Boolean) value, MY_USER_ID);
        } else if (KEY_POWER_INSTANTLY_LOCKS.equals(key)) {
            mLockPatternUtils.setPowerButtonInstantlyLocks((Boolean) value, MY_USER_ID);
        } else if (KEY_SHOW_PASSWORD.equals(key)) {
            Settings.System.putInt(getContentResolver(), Settings.System.TEXT_SHOW_PASSWORD,
                    ((Boolean) value) ? 1 : 0);
            lockPatternUtils.setVisiblePasswordEnabled((Boolean) value, MY_USER_ID);
        } else if (KEY_TOGGLE_INSTALL_APPLICATIONS.equals(key)) {
            if ((Boolean) value) {
                mToggleAppInstallation.setChecked(false);
                warnAppInstallation();
                // Don't change Switch status until user makes choice in dialog, so return false.
                result = false;
            } else {
                setNonMarketAppsAllowed(false);
            }

        } else if (KEY_SMS_SECURITY_CHECK_PREF.equals(key)) {
            int smsSecurityCheck = Integer.valueOf((String) value);
            Settings.Global.putInt(getContentResolver(), Settings.Global.SMS_OUTGOING_CHECK_MAX_COUNT,
                    smsSecurityCheck);
            updateSmsSecuritySummary(smsSecurityCheck);
        }
        return result;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_security;
    }

    /**
     * Loads the title and summary for live lock screen preference.  If an external package supports
     * the {@link cyanogenmod.content.Intent#ACTION_OPEN_LIVE_LOCKSCREEN_SETTINGS} we attempt to
     * load the title and summary from that package and use defaults if those cannot be loaded or
     * no other package is found to support the action.
     * @param pref
     */
    private void setLiveLockScreenPreferenceTitleAndSummary(Preference pref) {
        String title = getString(R.string.live_lock_screen_title);
        String summary = getString(R.string.live_lock_screen_summary);
        PackageManager pm = getPackageManager();
        List<ResolveInfo> infos = pm.queryIntentActivities(
                new Intent(ACTION_OPEN_LIVE_LOCKSCREEN_SETTINGS), 0);
        if (infos != null && infos.size() > 1) {
            for (ResolveInfo info : infos) {
                if (!getActivity().getPackageName().equals(info.activityInfo.packageName)) {
                    try {
                        final Context ctx = getActivity().createPackageContext(
                                info.activityInfo.packageName, 0);
                        final Resources res = ctx.getResources();
                        int titleId = res.getIdentifier("live_lock_screen_title", "string",
                                info.activityInfo.packageName);
                        int summaryId = res.getIdentifier("live_lock_screen_summary", "string",
                                info.activityInfo.packageName);
                        if (titleId !=0 && summaryId != 0) {
                            title = res.getString(titleId);
                            summary = res.getString(summaryId);
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        /* ignore and use defaults */
                    }
                    break;
                }
            }
        }

        pref.setTitle(title);
        pref.setSummary(summary);
    }

    /**
     * For Search. Please keep it in sync when updating "createPreferenceHierarchy()"
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new SecuritySearchIndexProvider();

    private static class SecuritySearchIndexProvider extends BaseSearchIndexProvider {

        boolean mIsPrimary;

        public SecuritySearchIndexProvider() {
            super();

            mIsPrimary = MY_USER_ID == UserHandle.USER_OWNER;
        }

        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(
                Context context, boolean enabled) {

            List<SearchIndexableResource> result = new ArrayList<SearchIndexableResource>();

            LockPatternUtils lockPatternUtils = new LockPatternUtils(context);
            // Add options for lock/unlock screen
            int resId = getResIdForLockUnlockScreen(context, lockPatternUtils);

            SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = resId;
            result.add(sir);

            if (mIsPrimary) {
                DevicePolicyManager dpm = (DevicePolicyManager)
                        context.getSystemService(Context.DEVICE_POLICY_SERVICE);

                switch (dpm.getStorageEncryptionStatus()) {
                    case DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE:
                        // The device is currently encrypted.
                        resId = R.xml.security_settings_encrypted;
                        break;
                    case DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE:
                        // This device supports encryption but isn't encrypted.
                        resId = R.xml.security_settings_unencrypted;
                        break;
                }

                sir = new SearchIndexableResource(context);
                sir.xmlResId = resId;
                result.add(sir);
            }

            // Append the rest of the settings
            sir = new SearchIndexableResource(context);
            sir.xmlResId = R.xml.security_settings_misc;
            result.add(sir);

            return result;
        }

        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();
            final Resources res = context.getResources();

            final String screenTitle = res.getString(R.string.security_settings_title);

            SearchIndexableRaw data = new SearchIndexableRaw(context);
            data.title = screenTitle;
            data.screenTitle = screenTitle;
            result.add(data);

            if (!mIsPrimary) {
                int resId = (UserManager.get(context).isLinkedUser()) ?
                        R.string.profile_info_settings_title : R.string.user_info_settings_title;

                data = new SearchIndexableRaw(context);
                data.title = res.getString(resId);
                data.screenTitle = screenTitle;
                result.add(data);
            }

            // Fingerprint
            FingerprintManager fpm =
                    (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
            if (fpm.isHardwareDetected()) {
                // This catches the title which can be overloaded in an overlay
                data = new SearchIndexableRaw(context);
                data.title = res.getString(R.string.security_settings_fingerprint_preference_title);
                data.screenTitle = screenTitle;
                result.add(data);
                // Fallback for when the above doesn't contain "fingerprint"
                data = new SearchIndexableRaw(context);
                data.title = res.getString(R.string.fingerprint_manage_category_title);
                data.screenTitle = screenTitle;
                result.add(data);
            }

            // Credential storage
            final UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);

            if (!um.hasUserRestriction(UserManager.DISALLOW_CONFIG_CREDENTIALS)) {
                KeyStore keyStore = KeyStore.getInstance();

                final int storageSummaryRes = keyStore.isHardwareBacked() ?
                        R.string.credential_storage_type_hardware :
                        R.string.credential_storage_type_software;

                data = new SearchIndexableRaw(context);
                data.title = res.getString(storageSummaryRes);
                data.screenTitle = screenTitle;
                result.add(data);
            }

            // Advanced
            final LockPatternUtils lockPatternUtils = new LockPatternUtils(context);
            if (lockPatternUtils.isSecure(MY_USER_ID)) {
                ArrayList<TrustAgentComponentInfo> agents =
                        getActiveTrustAgents(context.getPackageManager(), lockPatternUtils,
                                context.getSystemService(DevicePolicyManager.class));
                for (int i = 0; i < agents.size(); i++) {
                    final TrustAgentComponentInfo agent = agents.get(i);
                    data = new SearchIndexableRaw(context);
                    data.title = agent.title;
                    data.screenTitle = screenTitle;
                    result.add(data);
                }
            }
            return result;
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            final List<String> keys = new ArrayList<String>();

            LockPatternUtils lockPatternUtils = new LockPatternUtils(context);
            // Add options for lock/unlock screen
            int resId = getResIdForLockUnlockScreen(context, lockPatternUtils);

            // Do not display SIM lock for devices without an Icc card
            TelephonyManager tm = TelephonyManager.getDefault();
            if (!mIsPrimary || !tm.hasIccCard()) {
                keys.add(KEY_SIM_LOCK);
            }

            final UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
            if (um.hasUserRestriction(UserManager.DISALLOW_CONFIG_CREDENTIALS)) {
                keys.add(KEY_CREDENTIALS_MANAGER);
            }

            // TrustAgent settings disappear when the user has no primary security.
            if (!lockPatternUtils.isSecure(MY_USER_ID)) {
                keys.add(KEY_TRUST_AGENT);
                keys.add(KEY_MANAGE_TRUST_AGENTS);
            }

            return keys;
        }
    }

}
