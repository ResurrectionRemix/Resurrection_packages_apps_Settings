/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (C) 2014 The CyanogenMod Project
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

import com.android.internal.view.RotationPolicy;
import com.android.settings.notification.DropDownPreference;
import com.android.settings.notification.DropDownPreference.Callback;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import static android.provider.Settings.Secure.DOZE_ENABLED;
import static android.provider.Settings.Secure.WAKE_GESTURE_ENABLED;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.Dialog;
import android.app.IActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SlimSeekBarPreference;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import com.android.settings.cyanogenmod.DisplayRotation;
import com.android.settings.hardware.DisplayColor;
import com.android.settings.hardware.DisplayGamma;

import org.cyanogenmod.hardware.AdaptiveBacklight;
import org.cyanogenmod.hardware.ColorEnhancement;
import org.cyanogenmod.hardware.SunlightEnhancement;
import org.cyanogenmod.hardware.TapToWake;

public class DisplaySettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, OnPreferenceClickListener, Indexable {
    private static final String TAG = "DisplaySettings";

    /** If there is no setting in the provider, use this. */
    private static final int FALLBACK_SCREEN_TIMEOUT_VALUE = 30000;

    private static final String KEY_SCREEN_TIMEOUT = "screen_timeout";
    private static final String KEY_LCD_DENSITY = "lcd_density";
    private static final String KEY_FONT_SIZE = "font_size";
    private static final String KEY_SCREEN_SAVER = "screensaver";
    private static final String KEY_LIFT_TO_WAKE = "lift_to_wake";
    private static final String KEY_DOZE = "doze";
    private static final String KEY_AUTO_BRIGHTNESS = "auto_brightness";
    private static final String KEY_ADAPTIVE_BACKLIGHT = "adaptive_backlight";
    private static final String KEY_SUNLIGHT_ENHANCEMENT = "sunlight_enhancement";
    private static final String KEY_COLOR_ENHANCEMENT = "color_enhancement";
    private static final String KEY_TAP_TO_WAKE = "double_tap_wake_gesture";
    private static final String KEY_PROXIMITY_WAKE = "proximity_on_wake";
    private static final String KEY_DISPLAY_ROTATION = "display_rotation";
    private static final String KEY_WAKE_WHEN_PLUGGED_OR_UNPLUGGED = "wake_when_plugged_or_unplugged";

    private static final String CATEGORY_ADVANCED = "advanced_display_prefs";
    private static final String KEY_DISPLAY_COLOR = "color_calibration";
    private static final String KEY_DISPLAY_GAMMA = "gamma_tuning";
    private static final String KEY_SCREEN_COLOR_SETTINGS = "screencolor_settings";

    private static final int DLG_GLOBAL_CHANGE_WARNING = 1;

    private ListPreference mLcdDensityPreference;
    private static final String KEY_DOZE_TIMEOUT = "doze_timeout";

    private FontDialogPreference mFontSizePref;
    private PreferenceScreen mDisplayRotationPreference;
    private PreferenceScreen mScreenColorSettings;

    private final Configuration mCurConfig = new Configuration();

    private ListPreference mScreenTimeoutPreference;
    private Preference mScreenSaverPreference;
    private SwitchPreference mLiftToWakePreference;
    private SwitchPreference mDozePreference;
    private SwitchPreference mAutoBrightnessPreference;
    private SwitchPreference mTapToWake;
    private SwitchPreference mWakeWhenPluggedOrUnplugged;

    private SwitchPreference mAdaptiveBacklight;
    private SwitchPreference mSunlightEnhancement;
    private SwitchPreference mColorEnhancement;

    private SlimSeekBarPreference mDozeTimeout;

    private ContentObserver mAccelerometerRotationObserver =
            new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            updateDisplayRotationPreferenceDescription();
        }
    };

    private final RotationPolicy.RotationPolicyListener mRotationPolicyListener =
            new RotationPolicy.RotationPolicyListener() {
        @Override
        public void onChange() {
            updateDisplayRotationPreferenceDescription();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Activity activity = getActivity();
        final ContentResolver resolver = activity.getContentResolver();

        addPreferencesFromResource(R.xml.display_settings);

        mDisplayRotationPreference = (PreferenceScreen) findPreference(KEY_DISPLAY_ROTATION);

        mScreenSaverPreference = findPreference(KEY_SCREEN_SAVER);
        if (mScreenSaverPreference != null
                && getResources().getBoolean(
                        com.android.internal.R.bool.config_dreamsSupported) == false) {
            getPreferenceScreen().removePreference(mScreenSaverPreference);
        }

        mScreenTimeoutPreference = (ListPreference) findPreference(KEY_SCREEN_TIMEOUT);
        final long currentTimeout = Settings.System.getLong(resolver, SCREEN_OFF_TIMEOUT,
                FALLBACK_SCREEN_TIMEOUT_VALUE);
        mScreenTimeoutPreference.setValue(String.valueOf(currentTimeout));
        mScreenTimeoutPreference.setOnPreferenceChangeListener(this);
        disableUnusableTimeouts(mScreenTimeoutPreference);
        updateTimeoutPreferenceDescription(currentTimeout);
        updateDisplayRotationPreferenceDescription();

        mLcdDensityPreference = (ListPreference) findPreference(KEY_LCD_DENSITY);
        int defaultDensity = DisplayMetrics.DENSITY_DEVICE;
        String[] densityEntries = new String[8];
        for (int idx = 0; idx < 8; ++idx) {
            int pct = (75 + idx*5);
            densityEntries[idx] = Integer.toString(defaultDensity * pct / 100);
        }
        int currentDensity = DisplayMetrics.DENSITY_CURRENT;
        mLcdDensityPreference.setEntries(densityEntries);
        mLcdDensityPreference.setEntryValues(densityEntries);
        mLcdDensityPreference.setValue(String.valueOf(currentDensity));
        mLcdDensityPreference.setOnPreferenceChangeListener(this);
        updateLcdDensityPreferenceDescription(currentDensity);

        mFontSizePref = (FontDialogPreference) findPreference(KEY_FONT_SIZE);
        mFontSizePref.setOnPreferenceChangeListener(this);
        mFontSizePref.setOnPreferenceClickListener(this);

        if (isAutomaticBrightnessAvailable(getResources())) {
            mAutoBrightnessPreference = (SwitchPreference) findPreference(KEY_AUTO_BRIGHTNESS);
            mAutoBrightnessPreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_AUTO_BRIGHTNESS);
        }

        if (isLiftToWakeAvailable(activity)) {
            mLiftToWakePreference = (SwitchPreference) findPreference(KEY_LIFT_TO_WAKE);
            mLiftToWakePreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_LIFT_TO_WAKE);
        }

        PreferenceCategory advancedPrefs = (PreferenceCategory) findPreference(CATEGORY_ADVANCED);

        mAdaptiveBacklight = (SwitchPreference) findPreference(KEY_ADAPTIVE_BACKLIGHT);
        if (!isAdaptiveBacklightSupported()) {
            advancedPrefs.removePreference(mAdaptiveBacklight);
            mAdaptiveBacklight = null;
        }

        mSunlightEnhancement = (SwitchPreference) findPreference(KEY_SUNLIGHT_ENHANCEMENT);
        if (!isSunlightEnhancementSupported()) {
            advancedPrefs.removePreference(mSunlightEnhancement);
            mSunlightEnhancement = null;
        }

        mColorEnhancement = (SwitchPreference) findPreference(KEY_COLOR_ENHANCEMENT);
        if (!isColorEnhancementSupported()) {
            advancedPrefs.removePreference(mColorEnhancement);
            mColorEnhancement = null;
        }

        if (!DisplayColor.isSupported()) {
            advancedPrefs.removePreference(findPreference(KEY_DISPLAY_COLOR));
        }
        if (!DisplayGamma.isSupported()) {
            advancedPrefs.removePreference(findPreference(KEY_DISPLAY_GAMMA));
        }

        if (isDozeAvailable(activity)) {
            mDozePreference = (SwitchPreference) findPreference(KEY_DOZE);
            mDozePreference.setOnPreferenceChangeListener(this);
            // Doze timeout
            mDozeTimeout = (SlimSeekBarPreference) findPreference(KEY_DOZE_TIMEOUT);
            if (mDozeTimeout != null) {
                mDozeTimeout.setDefault(3000);
                mDozeTimeout.isMilliseconds(true);
                mDozeTimeout.setInterval(1);
                mDozeTimeout.minimumValue(100);
                mDozeTimeout.multiplyValue(100);
                mDozeTimeout.setOnPreferenceChangeListener(this);
            }
        } else {
            removePreference(KEY_DOZE);
            removePreference(KEY_DOZE_TIMEOUT);
        }

        mTapToWake = (SwitchPreference) findPreference(KEY_TAP_TO_WAKE);
        if (!isTapToWakeSupported()) {
            advancedPrefs.removePreference(mTapToWake);
            mTapToWake = null;
        }

        boolean proximityCheckOnWait = getResources().getBoolean(
                com.android.internal.R.bool.config_proximityCheckOnWake);
        if (!proximityCheckOnWait) {
            advancedPrefs.removePreference(findPreference(KEY_PROXIMITY_WAKE));
            Settings.System.putInt(getContentResolver(), Settings.System.PROXIMITY_ON_WAKE, 1);
        }

        mWakeWhenPluggedOrUnplugged =
                (SwitchPreference) findPreference(KEY_WAKE_WHEN_PLUGGED_OR_UNPLUGGED);

        mScreenColorSettings = (PreferenceScreen) findPreference(KEY_SCREEN_COLOR_SETTINGS);
        if (!isPostProcessingSupported()) {
            getPreferenceScreen().removePreference(mScreenColorSettings);
        }
    }

    private static boolean allowAllRotations(Context context) {
        return Resources.getSystem().getBoolean(
                com.android.internal.R.bool.config_allowAllRotations);
    }

    private static boolean isLiftToWakeAvailable(Context context) {
        SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        return sensors != null && sensors.getDefaultSensor(Sensor.TYPE_WAKE_GESTURE) != null;
    }

    private static boolean isDozeAvailable(Context context) {
        String name = Build.IS_DEBUGGABLE ? SystemProperties.get("debug.doze.component") : null;
        if (TextUtils.isEmpty(name)) {
            name = context.getResources().getString(
                    com.android.internal.R.string.config_dozeComponent);
        }
        return !TextUtils.isEmpty(name);
    }

    private static boolean isAutomaticBrightnessAvailable(Resources res) {
        return res.getBoolean(com.android.internal.R.bool.config_automatic_brightness_available);
    }

    private void updateDisplayRotationPreferenceDescription() {
        if (mDisplayRotationPreference == null) {
            // The preference was removed, do nothing
            return;
        }

        // We have a preference, lets update the summary
        boolean rotationEnabled = Settings.System.getInt(getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION, 0) != 0;

        if (!rotationEnabled) {
            mDisplayRotationPreference.setSummary(R.string.display_rotation_disabled);
            return;
        }

        StringBuilder summary = new StringBuilder();
        int mode = Settings.System.getInt(getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION_ANGLES,
                DisplayRotation.ROTATION_0_MODE
                | DisplayRotation.ROTATION_90_MODE
                | DisplayRotation.ROTATION_270_MODE);
        ArrayList<String> rotationList = new ArrayList<String>();
        String delim = "";

        if ((mode & DisplayRotation.ROTATION_0_MODE) != 0) {
            rotationList.add("0");
        }
        if ((mode & DisplayRotation.ROTATION_90_MODE) != 0) {
            rotationList.add("90");
        }
        if ((mode & DisplayRotation.ROTATION_180_MODE) != 0) {
            rotationList.add("180");
        }
        if ((mode & DisplayRotation.ROTATION_270_MODE) != 0) {
            rotationList.add("270");
        }
        for (int i = 0; i < rotationList.size(); i++) {
            summary.append(delim).append(rotationList.get(i));
            if ((rotationList.size() - i) > 2) {
                delim = ", ";
            } else {
                delim = " & ";
            }
        }
        summary.append(" " + getString(R.string.display_rotation_unit));
        mDisplayRotationPreference.setSummary(summary);
    }

    private void updateTimeoutPreferenceDescription(long currentTimeout) {
        ListPreference preference = mScreenTimeoutPreference;
        String summary;
        if (currentTimeout < 0) {
            // Unsupported value
            summary = "";
        } else {
            final CharSequence[] entries = preference.getEntries();
            final CharSequence[] values = preference.getEntryValues();
            if (entries == null || entries.length == 0) {
                summary = "";
            } else {
                int best = 0;
                for (int i = 0; i < values.length; i++) {
                    long timeout = Long.parseLong(values[i].toString());
                    if (currentTimeout >= timeout) {
                        best = i;
                    }
                }
                summary = preference.getContext().getString(R.string.screen_timeout_summary,
                        entries[best]);
            }
        }
        preference.setSummary(summary);
    }

    private void updateLcdDensityPreferenceDescription(int currentDensity) {
        ListPreference preference = mLcdDensityPreference;
        String summary;
        if (currentDensity < 10 || currentDensity >= 1000) {
            // Unsupported value
            summary = "";
        }
        else {
            summary = Integer.toString(currentDensity) + " DPI";
        }
        preference.setSummary(summary);
    }

    private void disableUnusableTimeouts(ListPreference screenTimeoutPreference) {
        final DevicePolicyManager dpm =
                (DevicePolicyManager) getActivity().getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        final long maxTimeout = dpm != null ? dpm.getMaximumTimeToLock(null) : 0;
        if (maxTimeout == 0) {
            return; // policy not enforced
        }
        final CharSequence[] entries = screenTimeoutPreference.getEntries();
        final CharSequence[] values = screenTimeoutPreference.getEntryValues();
        ArrayList<CharSequence> revisedEntries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> revisedValues = new ArrayList<CharSequence>();
        for (int i = 0; i < values.length; i++) {
            long timeout = Long.parseLong(values[i].toString());
            if (timeout <= maxTimeout) {
                revisedEntries.add(entries[i]);
                revisedValues.add(values[i]);
            }
        }
        if (revisedEntries.size() != entries.length || revisedValues.size() != values.length) {
            final int userPreference = Integer.parseInt(screenTimeoutPreference.getValue());
            screenTimeoutPreference.setEntries(
                    revisedEntries.toArray(new CharSequence[revisedEntries.size()]));
            screenTimeoutPreference.setEntryValues(
                    revisedValues.toArray(new CharSequence[revisedValues.size()]));
            if (userPreference <= maxTimeout) {
                screenTimeoutPreference.setValue(String.valueOf(userPreference));
            } else if (revisedValues.size() > 0
                    && Long.parseLong(revisedValues.get(revisedValues.size() - 1).toString())
                    == maxTimeout) {
                // If the last one happens to be the same as the max timeout, select that
                screenTimeoutPreference.setValue(String.valueOf(maxTimeout));
            } else {
                // There will be no highlighted selection since nothing in the list matches
                // maxTimeout. The user can still select anything less than maxTimeout.
                // TODO: maybe append maxTimeout to the list and mark selected.
            }
        }
        screenTimeoutPreference.setEnabled(revisedEntries.size() > 0);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Doze timeout
        if (mDozeTimeout != null) {
            final int statusDozeTimeout = Settings.System.getInt(getContentResolver(),
                    Settings.System.DOZE_TIMEOUT, 3000);
            // minimum 100 is 1 interval of the 100 multiplier
            mDozeTimeout.setInitValue((statusDozeTimeout / 100) - 1);
        }

        updateDisplayRotationPreferenceDescription();
        if (mAdaptiveBacklight != null) {
            mAdaptiveBacklight.setChecked(AdaptiveBacklight.isEnabled());
        }

        if (mSunlightEnhancement != null) {
            if (SunlightEnhancement.isAdaptiveBacklightRequired() &&
                    !AdaptiveBacklight.isEnabled()) {
                mSunlightEnhancement.setEnabled(false);
            } else {
                mSunlightEnhancement.setChecked(SunlightEnhancement.isEnabled());
            }
        }

        if (mColorEnhancement != null) {
            mColorEnhancement.setChecked(ColorEnhancement.isEnabled());
        }

        if (mTapToWake != null) {
            mTapToWake.setChecked(TapToWake.isEnabled());
        }

        RotationPolicy.registerRotationPolicyListener(getActivity(),
                mRotationPolicyListener);

        final ContentResolver resolver = getContentResolver();

        // Display rotation observer
        resolver.registerContentObserver(
                Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION), true,
                mAccelerometerRotationObserver);

        if (mAdaptiveBacklight != null) {
            mAdaptiveBacklight.setChecked(AdaptiveBacklight.isEnabled());
        }

        // Default value for wake-on-plug behavior from config.xml
        boolean wakeUpWhenPluggedOrUnpluggedConfig = getResources().getBoolean(
                com.android.internal.R.bool.config_unplugTurnsOnScreen);

        mWakeWhenPluggedOrUnplugged.setChecked(Settings.Global.getInt(getContentResolver(),
                Settings.Global.WAKE_WHEN_PLUGGED_OR_UNPLUGGED,
                (wakeUpWhenPluggedOrUnpluggedConfig ? 1 : 0)) == 1);

        updateState();
    }

    @Override
    public void onPause() {
        super.onPause();

        RotationPolicy.unregisterRotationPolicyListener(getActivity(),
                mRotationPolicyListener);

        // Display rotation observer
        getContentResolver().unregisterContentObserver(mAccelerometerRotationObserver);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        if (dialogId == DLG_GLOBAL_CHANGE_WARNING) {
            return Utils.buildGlobalChangeWarningDialog(getActivity(),
                    R.string.global_font_change_title,
                    new Runnable() {
                        public void run() {
                            mFontSizePref.click();
                        }
                    });
        }
        return null;
    }

    private void updateState() {
        readFontSizePreference(mFontSizePref);
        updateScreenSaverSummary();

        // Update auto brightness if it is available.
        if (mAutoBrightnessPreference != null) {
            int brightnessMode = Settings.System.getInt(getContentResolver(),
                    SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL);
            mAutoBrightnessPreference.setChecked(brightnessMode != SCREEN_BRIGHTNESS_MODE_MANUAL);
        }

        // Update lift-to-wake if it is available.
        if (mLiftToWakePreference != null) {
            int value = Settings.Secure.getInt(getContentResolver(), WAKE_GESTURE_ENABLED, 0);
            mLiftToWakePreference.setChecked(value != 0);
        }

        // Update doze if it is available.
        if (mDozePreference != null) {
            int value = Settings.Secure.getInt(getContentResolver(), DOZE_ENABLED, 1);
            mDozePreference.setChecked(value != 0);
        }
    }

    private void updateScreenSaverSummary() {
        if (mScreenSaverPreference != null) {
            mScreenSaverPreference.setSummary(
                    DreamSettings.getSummaryTextWithDreamName(getActivity()));
        }
    }

    public void writeLcdDensityPreference(int value) {
        try {
            SystemProperties.set("persist.sys.lcd_density", Integer.toString(value));
        }
        catch (Exception e) {
            Log.w(TAG, "Unable to save LCD density");
        }
        try {
            final IActivityManager am = ActivityManagerNative.asInterface(ServiceManager.checkService("activity"));
            if (am != null) {
                am.restart();
            }
        }
        catch (RemoteException e) {
            Log.e(TAG, "Failed to restart");
        }
    }

    private static boolean isTapToWakeSupported() {
        try {
            return TapToWake.isSupported();
        } catch (NoClassDefFoundError e) {
            // Hardware abstraction framework not installed
            return false;
        }
    }

    /**
     * Reads the current font size and sets the value in the summary text
     */
    public void readFontSizePreference(Preference pref) {
        try {
            mCurConfig.updateFrom(ActivityManagerNative.getDefault().getConfiguration());
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to retrieve font size");
        }

        // report the current size in the summary text
        final Resources res = getResources();
        String fontDesc = FontDialogPreference.getFontSizeDescription(res, mCurConfig.fontScale);
        pref.setSummary(getString(R.string.summary_font_size, fontDesc));
    }

    public void writeFontSizePreference(Object objValue) {
        try {
            mCurConfig.fontScale = Float.parseFloat(objValue.toString());
            ActivityManagerNative.getDefault().updatePersistentConfiguration(mCurConfig);
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to save font size");
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mTapToWake) {
            return TapToWake.setEnabled(mTapToWake.isChecked());
        } else if (preference == mAdaptiveBacklight) {
            if (mSunlightEnhancement != null &&
                    SunlightEnhancement.isAdaptiveBacklightRequired()) {
                mSunlightEnhancement.setEnabled(mAdaptiveBacklight.isChecked());
            }
            return AdaptiveBacklight.setEnabled(mAdaptiveBacklight.isChecked());
        } else if (preference == mSunlightEnhancement) {
            return SunlightEnhancement.setEnabled(mSunlightEnhancement.isChecked());
        } else if (preference == mColorEnhancement) {
            return ColorEnhancement.setEnabled(mColorEnhancement.isChecked());
        } else if (preference == mWakeWhenPluggedOrUnplugged) {
            Settings.Global.putInt(getContentResolver(),
                    Settings.Global.WAKE_WHEN_PLUGGED_OR_UNPLUGGED,
                    mWakeWhenPluggedOrUnplugged.isChecked() ? 1 : 0);
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (KEY_SCREEN_TIMEOUT.equals(key)) {
            try {
                int value = Integer.parseInt((String) objValue);
                Settings.System.putInt(getContentResolver(), SCREEN_OFF_TIMEOUT, value);
                updateTimeoutPreferenceDescription(value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist screen timeout setting", e);
            }
        }
        if (KEY_LCD_DENSITY.equals(key)) {
            int value = Integer.parseInt((String) objValue);
            writeLcdDensityPreference(value);
            updateLcdDensityPreferenceDescription(value);
        }
        if (KEY_FONT_SIZE.equals(key)) {
            writeFontSizePreference(objValue);
        }
        if (preference == mAutoBrightnessPreference) {
            boolean auto = (Boolean) objValue;
            Settings.System.putInt(getContentResolver(), SCREEN_BRIGHTNESS_MODE,
                    auto ? SCREEN_BRIGHTNESS_MODE_AUTOMATIC : SCREEN_BRIGHTNESS_MODE_MANUAL);
        }
        if (preference == mLiftToWakePreference) {
            boolean value = (Boolean) objValue;
            Settings.Secure.putInt(getContentResolver(), WAKE_GESTURE_ENABLED, value ? 1 : 0);
        }
        if (preference == mDozePreference) {
            boolean value = (Boolean) objValue;
            Settings.Secure.putInt(getContentResolver(), DOZE_ENABLED, value ? 1 : 0);
        }
        if (preference == mDozeTimeout) {
            int dozeTimeout = Integer.valueOf((String) objValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.DOZE_TIMEOUT, dozeTimeout);
        }
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mFontSizePref) {
            if (Utils.hasMultipleUsers(getActivity())) {
                showDialog(DLG_GLOBAL_CHANGE_WARNING);
                return true;
            } else {
                mFontSizePref.click();
            }
        }
        return false;
    }


    /**
     * Restore the properties associated with this preference on boot
       @param ctx A valid context
     */
    public static void restore(Context ctx) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        if (isTapToWakeSupported()) {
            final boolean enabled = prefs.getBoolean(KEY_TAP_TO_WAKE,
                TapToWake.isEnabled());

            if (!TapToWake.setEnabled(enabled)) {
                Log.e(TAG, "Failed to restore tap-to-wake settings.");
            } else {
                Log.d(TAG, "Tap-to-wake settings restored.");
            }
        }

        if (isAdaptiveBacklightSupported()) {
            final boolean enabled = prefs.getBoolean(KEY_ADAPTIVE_BACKLIGHT,
                    AdaptiveBacklight.isEnabled());
            if (!AdaptiveBacklight.setEnabled(enabled)) {
                Log.e(TAG, "Failed to restore adaptive backlight settings.");
            } else {
                Log.d(TAG, "Adaptive backlight settings restored.");
            }
        }

        if (isSunlightEnhancementSupported()) {
            final boolean enabled = prefs.getBoolean(KEY_SUNLIGHT_ENHANCEMENT,
                    SunlightEnhancement.isEnabled());
            if (SunlightEnhancement.isAdaptiveBacklightRequired() &&
                    !AdaptiveBacklight.isEnabled()) {
                SunlightEnhancement.setEnabled(false);
                Log.d(TAG, "SRE requires CABC, disabled");
            } else {
                if (!SunlightEnhancement.setEnabled(enabled)) {
                    Log.e(TAG, "Failed to restore SRE settings.");
                } else {
                    Log.d(TAG, "SRE settings restored.");
                }
            }
        }

        if (isColorEnhancementSupported()) {
            final boolean enabled = prefs.getBoolean(KEY_COLOR_ENHANCEMENT,
                    ColorEnhancement.isEnabled());
            if (!ColorEnhancement.setEnabled(enabled)) {
                Log.e(TAG, "Failed to restore color enhancement settings.");
            } else {
                Log.d(TAG, "Color enhancement settings restored.");
            }
        }
    }

    private boolean isPostProcessingSupported() {
        boolean ret = true;
        final PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo("com.qualcomm.display", PackageManager.GET_META_DATA);
        } catch (NameNotFoundException e) {
            ret = false;
        }
        return ret;
    }

    private static boolean isAdaptiveBacklightSupported() {
        try {
            return AdaptiveBacklight.isSupported();
        } catch (NoClassDefFoundError e) {
            // Hardware abstraction framework not installed
            return false;
        }
    }

    private static boolean isSunlightEnhancementSupported() {
        try {
            return SunlightEnhancement.isSupported();
        } catch (NoClassDefFoundError e) {
            // Hardware abstraction framework not installed
            return false;
        }
    }

    private static boolean isColorEnhancementSupported() {
        try {
            return ColorEnhancement.isSupported();
        } catch (NoClassDefFoundError e) {
            // Hardware abstraction framework not installed
            return false;
        }
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.display_settings;
                    result.add(sir);

                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    ArrayList<String> result = new ArrayList<String>();
                    if (!context.getResources().getBoolean(
                            com.android.internal.R.bool.config_dreamsSupported)) {
                        result.add(KEY_SCREEN_SAVER);
                    }
                    if (!isAutomaticBrightnessAvailable(context.getResources())) {
                        result.add(KEY_AUTO_BRIGHTNESS);
                    }
                    if (!isLiftToWakeAvailable(context)) {
                        result.add(KEY_LIFT_TO_WAKE);
                    }
                    if (!isDozeAvailable(context)) {
                        result.add(KEY_DOZE);
                    }
                    return result;
                }
            };
}
