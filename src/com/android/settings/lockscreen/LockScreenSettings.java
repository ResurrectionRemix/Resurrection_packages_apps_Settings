/*
 * Copyright (C) 2013 The CyanogenMod project
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

package com.android.settings.lockscreen;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.security.KeyStore;
import android.service.trust.TrustAgentService;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.ManageFingerprints;
import com.android.settings.R;
import com.android.settings.SecuritySettings;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.TrustAgentUtils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Index;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;

import java.util.ArrayList;
import java.util.List;

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

public class LockScreenSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener, Indexable {

    private static final String KEY_SECURITY_CATEGORY = "security_category";
    private static final String KEY_GENERAL_CATEGORY = "general_category";
    private static final String KEY_OWNER_INFO_SETTINGS = "owner_info_settings";

    // Lock Settings
    private static final String KEY_UNLOCK_SET_OR_CHANGE = "unlock_set_or_change";
    private static final String KEY_BIOMETRIC_WEAK_IMPROVE_MATCHING =
            "biometric_weak_improve_matching";
    private static final String KEY_BIOMETRIC_WEAK_LIVELINESS = "biometric_weak_liveliness";
    private static final String KEY_LOCK_ENABLED = "lockenabled";
    private static final String KEY_VISIBLE_PATTERN = "visiblepattern";
    private static final String KEY_VISIBLE_ERROR_PATTERN = "visible_error_pattern";
    private static final String KEY_VISIBLE_DOTS = "visibledots";
    private static final String KEY_LOCK_AFTER_TIMEOUT = "lock_after_timeout";
    private static final String KEY_POWER_INSTANTLY_LOCKS = "power_button_instantly_locks";
    private static final String KEY_TRUST_AGENT = "trust_agent";
    private static final String KEY_MANAGE_TRUST_AGENTS = "manage_trust_agents";
    private static final String KEY_SHOW_VISUALIZER = "lockscreen_visualizer";
    private static final String KEY_MANAGE_FINGERPRINTS = "manage_fingerprints";

    private static final int SET_OR_CHANGE_LOCK_METHOD_REQUEST = 123;
    private static final int CONFIRM_EXISTING_FOR_BIOMETRIC_WEAK_IMPROVE_REQUEST = 124;
    private static final int CONFIRM_EXISTING_FOR_BIOMETRIC_WEAK_LIVELINESS_OFF = 125;


    private static final String TRUST_AGENT_CLICK_INTENT = "trust_agent_click_intent";
    private static final int CHANGE_TRUST_AGENT_SETTINGS = 126;

    private Intent mTrustAgentClickIntent;

    private boolean mIsPrimary;

    private ChooseLockSettingsHelper mChooseLockSettingsHelper;
    private LockPatternUtils mLockPatternUtils;
    private ListPreference mLockAfter;

    private SwitchPreference mBiometricWeakLiveliness;
    private SwitchPreference mVisiblePattern;
    private SwitchPreference mVisibleErrorPattern;
    private SwitchPreference mVisibleDots;
    private SwitchPreference mPowerButtonInstantlyLocks;

    private DevicePolicyManager mDPM;

    // These switch preferences need special handling since they're not all stored in Settings.
    private static final String SWITCH_PREFERENCE_KEYS[] = { KEY_LOCK_AFTER_TIMEOUT,
            KEY_LOCK_ENABLED, KEY_VISIBLE_PATTERN, KEY_VISIBLE_ERROR_PATTERN, KEY_VISIBLE_DOTS,
            KEY_BIOMETRIC_WEAK_LIVELINESS, KEY_POWER_INSTANTLY_LOCKS };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLockPatternUtils = new LockPatternUtils(getActivity());
        mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());

        if (savedInstanceState != null
                && savedInstanceState.containsKey(TRUST_AGENT_CLICK_INTENT)) {
            mTrustAgentClickIntent = savedInstanceState.getParcelable(TRUST_AGENT_CLICK_INTENT);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        createPreferenceHierarchy();

        final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();
        if (mBiometricWeakLiveliness != null) {
            mBiometricWeakLiveliness.setChecked(
                    lockPatternUtils.isBiometricWeakLivelinessEnabled());
        }
        if (mVisiblePattern != null) {
            mVisiblePattern.setChecked(lockPatternUtils.isVisiblePatternEnabled());
        }
        if (mVisibleErrorPattern != null) {
            mVisibleErrorPattern.setChecked(lockPatternUtils.isShowErrorPath());
        }
        if (mVisibleDots != null) {
            mVisibleDots.setChecked(lockPatternUtils.isVisibleDotsEnabled());
        }
        if (mPowerButtonInstantlyLocks != null) {
            mPowerButtonInstantlyLocks.setChecked(lockPatternUtils.getPowerButtonInstantlyLocks());
        }
    }

    private void createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.security_settings);
        root = getPreferenceScreen();

        // Add package manager to check if features are available
        PackageManager pm = getPackageManager();

        // Add options for lock/unlock screen
        final int resid = getResIdForLockUnlockScreen(getActivity(), mLockPatternUtils);
        addPreferencesFromResource(resid);

        // Add options for device encryption
        mIsPrimary = UserHandle.myUserId() == UserHandle.USER_OWNER;

        if (!mIsPrimary) {
            // Rename owner info settings
            Preference ownerInfoPref = findPreference(KEY_OWNER_INFO_SETTINGS);
            if (ownerInfoPref != null) {
                if (UserManager.get(getActivity()).isLinkedUser()) {
                    ownerInfoPref.setTitle(R.string.profile_info_settings_title);
                } else {
                    ownerInfoPref.setTitle(R.string.user_info_settings_title);
                }
            }
        }

        // lock after preference
        mLockAfter = (ListPreference) root.findPreference(KEY_LOCK_AFTER_TIMEOUT);
        if (mLockAfter != null) {
            setupLockAfterPreference();
            updateLockAfterPreferenceSummary();
        }

        // biometric weak liveliness
        mBiometricWeakLiveliness =
                (SwitchPreference) root.findPreference(KEY_BIOMETRIC_WEAK_LIVELINESS);

        // visible pattern
        mVisiblePattern = (SwitchPreference) root.findPreference(KEY_VISIBLE_PATTERN);

        // visible error pattern
        mVisibleErrorPattern = (SwitchPreference) root.findPreference(KEY_VISIBLE_ERROR_PATTERN);

        // visible dots
        mVisibleDots = (SwitchPreference) root.findPreference(KEY_VISIBLE_DOTS);

        // lock instantly on power key press
        mPowerButtonInstantlyLocks = (SwitchPreference) root.findPreference(
                KEY_POWER_INSTANTLY_LOCKS);
        String trustAgentName = getActiveTrustAgentTitle();
        if (mPowerButtonInstantlyLocks != null &&
                trustAgentName != null &&
                trustAgentName.length() > 0) {
            mPowerButtonInstantlyLocks.setSummary(getString(
                    R.string.lockpattern_settings_power_button_instantly_locks_summary,
                    trustAgentName));
        }

        PreferenceGroup securityCategory = (PreferenceGroup)
                root.findPreference(KEY_SECURITY_CATEGORY);
        // don't display visible pattern if biometric and backup is not pattern
        if (resid == R.xml.security_settings_biometric_weak &&
                mLockPatternUtils.getKeyguardStoredPasswordQuality() !=
                        DevicePolicyManager.PASSWORD_QUALITY_SOMETHING) {
            if (securityCategory != null && mVisiblePattern != null &&
                   mVisibleErrorPattern != null && mVisibleDots != null) {
                securityCategory.removePreference(mVisiblePattern);
                securityCategory.removePreference(mVisibleErrorPattern);
                securityCategory.removePreference(mVisibleDots);
            }
        }

        PreferenceGroup generalCategory = (PreferenceGroup)
                root.findPreference(KEY_GENERAL_CATEGORY);
        // remove lockscreen visualizer option on low end gfx devices
        if (!ActivityManager.isHighEndGfx() && generalCategory != null) {
            SwitchPreference displayVisualizer = (SwitchPreference)
                    generalCategory.findPreference(KEY_SHOW_VISUALIZER);
            if (displayVisualizer != null) {
                generalCategory.removePreference(displayVisualizer);
            }
        }

        // Trust Agent preferences
        if (securityCategory != null) {
            final boolean hasSecurity = mLockPatternUtils.isSecure();
            ArrayList<TrustAgentUtils.TrustAgentComponentInfo> agents =
                    SecuritySettings.getActiveTrustAgents(getPackageManager(), mLockPatternUtils);
            for (int i = 0; i < agents.size(); i++) {
                final TrustAgentUtils.TrustAgentComponentInfo agent = agents.get(i);
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
                if (!hasSecurity) {
                    trustAgentPreference.setEnabled(false);
                    trustAgentPreference.setSummary(R.string.disabled_because_no_backup_security);
                }
            }
        }

        for (int i = 0; i < SWITCH_PREFERENCE_KEYS.length; i++) {
            final Preference pref = findPreference(SWITCH_PREFERENCE_KEYS[i]);
            if (pref != null) pref.setOnPreferenceChangeListener(this);
        }
    }

    private String getActiveTrustAgentTitle() {
        final boolean hasSecurity = mLockPatternUtils.isSecure();
        ArrayList<TrustAgentUtils.TrustAgentComponentInfo> agents =
                SecuritySettings.getActiveTrustAgents(getPackageManager(), mLockPatternUtils);
        for (int i = 0; i < agents.size(); i++) {
            final TrustAgentUtils.TrustAgentComponentInfo agent = agents.get(i);
            if (agent.title != null) {
                return agent.title;
            }

        }
        return null;
    }
    private static int getResIdForLockUnlockScreen(Context context,
                                                   LockPatternUtils lockPatternUtils) {
        int resid = 0;
        if (!lockPatternUtils.isSecure()) {
            // if there are multiple users, disable "None" setting
            UserManager mUm = (UserManager) context.getSystemService(Context.USER_SERVICE);
            List<UserInfo> users = mUm.getUsers(true);
            final boolean singleUser = users.size() == 1;

            if (singleUser && lockPatternUtils.isLockScreenDisabled()) {
                resid = R.xml.security_settings_lockscreen;
            } else {
                resid = R.xml.security_settings_chooser;
            }
        } else if (lockPatternUtils.usingFingerprint()
                && lockPatternUtils.isFingerprintInstalled(context)) {
            resid = R.xml.security_settings_fingerprint;
        } else if (lockPatternUtils.usingBiometricWeak() &&
                lockPatternUtils.isBiometricWeakInstalled()) {
                resid = R.xml.security_settings_biometric_weak;
        } else if (lockPatternUtils.usingFingerprint()) {
            resid = R.xml.security_settings_fingerprint;
        } else {
            switch (lockPatternUtils.getKeyguardStoredPasswordQuality()) {
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

        String trustAgentName = getActiveTrustAgentTitle();
        if (trustAgentName != null && trustAgentName.length() > 0) {
            mLockAfter.setSummary(getString(R.string.lock_after_timeout_summary_with_exception,
                    entries[best], trustAgentName));
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

    public void startBiometricWeakImprove(){
        Intent intent = new Intent();
        intent.setClassName("com.android.facelock", "com.android.facelock.AddToSetup");
        startActivity(intent);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CONFIRM_EXISTING_FOR_BIOMETRIC_WEAK_IMPROVE_REQUEST &&
                resultCode == Activity.RESULT_OK) {
            startBiometricWeakImprove();
            return;
        } else if (requestCode == CONFIRM_EXISTING_FOR_BIOMETRIC_WEAK_LIVELINESS_OFF &&
                resultCode == Activity.RESULT_OK) {
            final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();
            lockPatternUtils.setBiometricWeakLivelinessEnabled(false);
            // Setting the mBiometricWeakLiveliness checked value to false is handled when onResume
            // is called by grabbing the value from lockPatternUtils.  We can't set it here
            // because mBiometricWeakLiveliness could be null
            return;
        } else if (requestCode == CHANGE_TRUST_AGENT_SETTINGS && resultCode == Activity.RESULT_OK) {
            if (mTrustAgentClickIntent != null) {
                startActivity(mTrustAgentClickIntent);
                mTrustAgentClickIntent = null;
            }
            return;
        } else if (requestCode == SET_OR_CHANGE_LOCK_METHOD_REQUEST) {
            Index.getInstance(
                    getActivity().getApplicationContext()).updateFromClassNameResource(
                    LockScreenSettings.class.getName(), true, true);
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
        } else if (KEY_LOCK_ENABLED.equals(key)) {
            lockPatternUtils.setLockPatternEnabled((Boolean) value);
        } else if (KEY_VISIBLE_PATTERN.equals(key)) {
            lockPatternUtils.setVisiblePatternEnabled((Boolean) value);
        } else if (KEY_VISIBLE_ERROR_PATTERN.equals(key)) {
            lockPatternUtils.setShowErrorPath((Boolean) value);
        } else if (KEY_VISIBLE_DOTS.equals(key)) {
            lockPatternUtils.setVisibleDotsEnabled((Boolean) value);
        } else  if (KEY_BIOMETRIC_WEAK_LIVELINESS.equals(key)) {
            if ((Boolean) value) {
                lockPatternUtils.setBiometricWeakLivelinessEnabled(true);
            } else {
                // In this case the user has just unchecked the checkbox, but this action requires
                // them to confirm their password.  We need to re-check the checkbox until
                // they've confirmed their password
                mBiometricWeakLiveliness.setChecked(true);
                ChooseLockSettingsHelper helper =
                        new ChooseLockSettingsHelper(this.getActivity(), this);
                if (!helper.launchConfirmationActivity(
                        CONFIRM_EXISTING_FOR_BIOMETRIC_WEAK_LIVELINESS_OFF, null, null)) {
                    // If this returns false, it means no password confirmation is required, so
                    // go ahead and uncheck it here.
                    // Note: currently a backup is required for biometric_weak so this code path
                    // can't be reached, but is here in case things change in the future
                    lockPatternUtils.setBiometricWeakLivelinessEnabled(false);
                    mBiometricWeakLiveliness.setChecked(false);
                }
            }
        } else if (KEY_POWER_INSTANTLY_LOCKS.equals(key)) {
            mLockPatternUtils.setPowerButtonInstantlyLocks((Boolean) value);
        }
        return result;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        final String key = preference.getKey();
        if (KEY_UNLOCK_SET_OR_CHANGE.equals(key)) {
            startFragment(this, "com.android.settings.ChooseLockGeneric$ChooseLockGenericFragment",
                    R.string.lock_settings_picker_title, SET_OR_CHANGE_LOCK_METHOD_REQUEST, null);
        } else if (KEY_MANAGE_FINGERPRINTS.equals((key))) {
            Intent intent = ManageFingerprints.createIntent(getActivity(), false, true, true);
            startActivity(intent);
        } else if (KEY_BIOMETRIC_WEAK_IMPROVE_MATCHING.equals(key)) {
            ChooseLockSettingsHelper helper =
                    new ChooseLockSettingsHelper(this.getActivity(), this);
            if (!helper.launchConfirmationActivity(
                    CONFIRM_EXISTING_FOR_BIOMETRIC_WEAK_IMPROVE_REQUEST, null, null)) {
                // If this returns false, it means no password confirmation is required, so
                // go ahead and start improve.
                // Note: currently a backup is required for biometric_weak so this code path
                // can't be reached, but is here in case things change in the future
                startBiometricWeakImprove();
            }
        } else if (KEY_TRUST_AGENT.equals(key)) {
                ChooseLockSettingsHelper helper =
                        new ChooseLockSettingsHelper(this.getActivity(), this);
                mTrustAgentClickIntent = preference.getIntent();
                if (!helper.launchConfirmationActivity(CHANGE_TRUST_AGENT_SETTINGS, null, null) &&
                        mTrustAgentClickIntent != null) {
                    // If this returns false, it means no password confirmation is required.
                    startActivity(mTrustAgentClickIntent);
                    mTrustAgentClickIntent = null;
                }
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mTrustAgentClickIntent != null) {
            outState.putParcelable(TRUST_AGENT_CLICK_INTENT, mTrustAgentClickIntent);
        }
    }

    /**
     * For Search. Please keep it in sync when updating "createPreferenceHierarchy()"
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new LockScreenSearchIndexProvider();

    private static class LockScreenSearchIndexProvider extends BaseSearchIndexProvider {

        boolean mIsPrimary;

        public LockScreenSearchIndexProvider() {
            super();

            mIsPrimary = UserHandle.myUserId() == UserHandle.USER_OWNER;
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

            // Advanced
            final LockPatternUtils lockPatternUtils = new LockPatternUtils(context);
            if (lockPatternUtils.isSecure()) {
                ArrayList<TrustAgentUtils.TrustAgentComponentInfo> agents =
                        SecuritySettings.getActiveTrustAgents(context.getPackageManager(),
                                lockPatternUtils);
                for (int i = 0; i < agents.size(); i++) {
                    final TrustAgentUtils.TrustAgentComponentInfo agent = agents.get(i);
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

            // don't display visible pattern if biometric and backup is not pattern
            if (resId == R.xml.security_settings_biometric_weak &&
                    lockPatternUtils.getKeyguardStoredPasswordQuality() !=
                            DevicePolicyManager.PASSWORD_QUALITY_SOMETHING) {
                keys.add(KEY_VISIBLE_PATTERN);
                keys.add(KEY_VISIBLE_ERROR_PATTERN);
                keys.add(KEY_VISIBLE_DOTS);
            }

            // TrustAgent settings disappear when the user has no primary security.
            if (!lockPatternUtils.isSecure()) {
                keys.add(KEY_TRUST_AGENT);
                keys.add(KEY_MANAGE_TRUST_AGENTS);
            }

            // hidden on low end gfx devices.
            if (!ActivityManager.isHighEndGfx()) {
                keys.add(KEY_SHOW_VISUALIZER);
            }

            return keys;
        }
    }
}
