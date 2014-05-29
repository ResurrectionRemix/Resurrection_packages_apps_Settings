/*
 * Copyright (C) 2012-2014 The CyanogenMod Project
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

package com.android.settings.cyanogenmod;

import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.hardware.Camera;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class LockscreenInterface extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String LOCKSCREEN_GENERAL_CATEGORY = "lockscreen_general_category";
    private static final String LOCKSCREEN_WIDGETS_CATEGORY = "lockscreen_widgets_category";
    private static final String KEY_BATTERY_STATUS = "lockscreen_battery_status";
    private static final String KEY_LOCKSCREEN_BUTTONS = "lockscreen_buttons";
    private static final String KEY_ENABLE_WIDGETS = "keyguard_enable_widgets";
    private static final String KEY_LOCK_CLOCK = "lock_clock";
    private static final String KEY_ENABLE_CAMERA = "keyguard_enable_camera";
    private static final String KEY_ENABLE_MAXIMIZE_WIGETS = "lockscreen_maximize_widgets";
    private static final String KEY_LOCKSCREEN_MODLOCK_ENABLED = "lockscreen_modlock_enabled";
    private static final String KEY_LOCKSCREEN_TARGETS = "lockscreen_targets";
    private static final String PREF_LOCKSCREEN_TORCH = "lockscreen_torch";
    private static final String LOCKSCREEN_QUICK_UNLOCK_CONTROL = "lockscreen_quick_unlock_control";
    
    private CheckBoxPreference mEnableKeyguardWidgets;
    private CheckBoxPreference mEnableCameraWidget;
    private CheckBoxPreference mEnableModLock;
    private CheckBoxPreference mEnableMaximizeWidgets;
    private ListPreference mBatteryStatus;
    private Preference mLockscreenTargets;
    private CheckBoxPreference mGlowpadTorch;
    private CheckBoxPreference mQuickUnlock;
    
    private ChooseLockSettingsHelper mChooseLockSettingsHelper;
    private LockPatternUtils mLockUtils;
    private DevicePolicyManager mDPM;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.lockscreen_interface_settings);
        
        PreferenceScreen prefs = getPreferenceScreen();

        mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());
        mLockUtils = mChooseLockSettingsHelper.utils();
        mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        // Find categories
        PreferenceCategory generalCategory = (PreferenceCategory)
                findPreference(LOCKSCREEN_GENERAL_CATEGORY);
        PreferenceCategory widgetsCategory = (PreferenceCategory)
                findPreference(LOCKSCREEN_WIDGETS_CATEGORY);

        // Find preferences
        mEnableKeyguardWidgets = (CheckBoxPreference) findPreference(KEY_ENABLE_WIDGETS);
        mEnableCameraWidget = (CheckBoxPreference) findPreference(KEY_ENABLE_CAMERA);
        mEnableMaximizeWidgets = (CheckBoxPreference) findPreference(KEY_ENABLE_MAXIMIZE_WIGETS);
        mLockscreenTargets = findPreference(KEY_LOCKSCREEN_TARGETS);
        mEnableCameraWidget = (CheckBoxPreference) findPreference(KEY_ENABLE_CAMERA);

        mGlowpadTorch = (CheckBoxPreference) findPreference(PREF_LOCKSCREEN_TORCH);
        mGlowpadTorch.setChecked(Settings.System.getInt(
                getActivity().getApplicationContext().getContentResolver(),
                Settings.System.LOCKSCREEN_GLOWPAD_TORCH, 0) == 1);
        mGlowpadTorch.setOnPreferenceChangeListener(this);

        // Remove glowpad torch if device doesn't have torch
        if (!hasTorch()) {
            prefs.removePreference(mGlowpadTorch);
        }

        mEnableModLock = (CheckBoxPreference) findPreference(KEY_LOCKSCREEN_MODLOCK_ENABLED);
        if (mEnableModLock != null) {
            mEnableModLock.setOnPreferenceChangeListener(this);
        }
        mBatteryStatus = (ListPreference) findPreference(KEY_BATTERY_STATUS);
        if (mBatteryStatus != null) {
            mBatteryStatus.setOnPreferenceChangeListener(this);
        }

        // Remove lockscreen button actions if device doesn't have hardware keys
        if (!hasButtons()) {
            generalCategory.removePreference(findPreference(KEY_LOCKSCREEN_BUTTONS));
        }

        // Enable or disable camera widget based on device and policy
        if (Camera.getNumberOfCameras() == 0) {
            widgetsCategory.removePreference(mEnableCameraWidget);
            mEnableCameraWidget = null;
            mLockUtils.setCameraEnabled(false);
        } else if (mLockUtils.isSecure()) {
            checkDisabledByPolicy(mEnableCameraWidget,
                    DevicePolicyManager.KEYGUARD_DISABLE_SECURE_CAMERA);
        }

        boolean canEnableModLockscreen = false;
        final String keyguardPackage = getActivity().getString(
                com.android.internal.R.string.config_keyguardPackage);
        final Bundle keyguard_metadata = Utils.getApplicationMetadata(
                getActivity(), keyguardPackage);
        if (keyguard_metadata != null) {
            canEnableModLockscreen = keyguard_metadata.getBoolean(
                    "com.cyanogenmod.keyguard", false);
        }

        if (mEnableModLock != null && !canEnableModLockscreen) {
            generalCategory.removePreference(mEnableModLock);
            mEnableModLock = null;
        }

        // Remove cLock settings item if not installed
        if (!Utils.isPackageInstalled(getActivity(), "com.cyanogenmod.lockclock")) {
            widgetsCategory.removePreference(findPreference(KEY_LOCK_CLOCK));
        }

        // Remove maximize widgets on tablets
        if (!Utils.isPhone(getActivity())) {
            widgetsCategory.removePreference(
                    mEnableMaximizeWidgets);
        }
	    // Quick unlock
	    mQuickUnlock = (CheckBoxPreference) findPreference(LOCKSCREEN_QUICK_UNLOCK_CONTROL);
		if (mQuickUnlock != null) {
		mQuickUnlock.setChecked(Settings.System.getInt(getContentResolver(),
		Settings.System.LOCKSCREEN_QUICK_UNLOCK_CONTROL, 0) == 1);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Update custom widgets and camera
        if (mEnableKeyguardWidgets != null) {
            mEnableKeyguardWidgets.setChecked(mLockUtils.getWidgetsEnabled());
        }
	    if (mQuickUnlock != null) {
            mQuickUnlock.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                  Settings.System.LOCKSCREEN_QUICK_UNLOCK_CONTROL, 0) == 1);
        }
        if (mEnableCameraWidget != null) {
            mEnableCameraWidget.setChecked(mLockUtils.getCameraEnabled());
        }

        // Update battery status
        if (mBatteryStatus != null) {
            ContentResolver cr = getActivity().getContentResolver();
            int batteryStatus = Settings.System.getInt(cr,
                    Settings.System.LOCKSCREEN_BATTERY_VISIBILITY, 0);
            mBatteryStatus.setValueIndex(batteryStatus);
            mBatteryStatus.setSummary(mBatteryStatus.getEntries()[batteryStatus]);
        }

        // Update mod lockscreen status
        if (mEnableModLock != null) {
            ContentResolver cr = getActivity().getContentResolver();
            boolean checked = Settings.System.getInt(
                    cr, Settings.System.LOCKSCREEN_MODLOCK_ENABLED, 1) == 1;
            mEnableModLock.setChecked(checked);
        }

        updateAvailableModLockPreferences();
    }

    private void updateAvailableModLockPreferences() {
        if (mEnableModLock == null) {
            return;
        }

        boolean enabled = !mEnableModLock.isChecked();
        if (mEnableKeyguardWidgets != null) {
            // Enable or disable lockscreen widgets based on policy
            if(!checkDisabledByPolicy(mEnableKeyguardWidgets,
                    DevicePolicyManager.KEYGUARD_DISABLE_WIDGETS_ALL)) {
                mEnableKeyguardWidgets.setEnabled(enabled);
            }
        }
        if (mEnableMaximizeWidgets != null) {
            mEnableMaximizeWidgets.setEnabled(enabled);
        }
        if (mLockscreenTargets != null) {
            mLockscreenTargets.setEnabled(enabled);
        }
        if (mBatteryStatus != null) {
            mBatteryStatus.setEnabled(enabled);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        final String key = preference.getKey();

        if (KEY_ENABLE_WIDGETS.equals(key)) {
            mLockUtils.setWidgetsEnabled(mEnableKeyguardWidgets.isChecked());
            return true;
        } else if (KEY_ENABLE_CAMERA.equals(key)) {
            mLockUtils.setCameraEnabled(mEnableCameraWidget.isChecked());
            return true;
        } else if (preference == mQuickUnlock) {
            Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.LOCKSCREEN_QUICK_UNLOCK_CONTROL, mQuickUnlock.isChecked() ? 1 : 0);
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver cr = getActivity().getContentResolver();
        if (preference == mBatteryStatus) {
            int value = Integer.valueOf((String) objValue);
            int index = mBatteryStatus.findIndexOfValue((String) objValue);
            Settings.System.putInt(cr, Settings.System.LOCKSCREEN_BATTERY_VISIBILITY, value);
            mBatteryStatus.setSummary(mBatteryStatus.getEntries()[index]);
            return true;
        } else if (preference == mEnableModLock) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(cr, Settings.System.LOCKSCREEN_MODLOCK_ENABLED,
                    value ? 1 : 0);
            // force it so update picks up correct values
            ((CheckBoxPreference) preference).setChecked(value);
            updateAvailableModLockPreferences();
            return true;
        } else if (preference == mGlowpadTorch) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_GLOWPAD_TORCH,
                    (Boolean) objValue ? 1 : 0);
            return true;
        }

        return false;
    }

    /**
     * Checks if the device has hardware buttons.
     * @return has Buttons
     */
    public boolean hasButtons() {
        return (getResources().getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys) > 0);
    }
    
   /**
     * Checks if the device has torch.
     * @return has torch
     */
    public boolean hasTorch() {
        return getResources().getBoolean(com.android.internal.R.bool.config_enableTorch);
    }
    
    /**
     * Checks if a specific policy is disabled by a device administrator, and disables the
     * provided preference if so.
     * @param preference Preference
     * @param feature Feature
     * @return True if disabled.
     */
    private boolean checkDisabledByPolicy(Preference preference, int feature) {
        boolean disabled = featureIsDisabled(feature);

        if (disabled) {
            preference.setSummary(R.string.security_enable_widgets_disabled_summary);
        }

        preference.setEnabled(!disabled);
        return disabled;
    }

    /**
     * Checks if a specific policy is disabled by a device administrator.
     * @param feature Feature
     * @return Is disabled
     */
    private boolean featureIsDisabled(int feature) {
        return (mDPM.getKeyguardDisabledFeatures(null) & feature) != 0;
    }

}
