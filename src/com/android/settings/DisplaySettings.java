/*
 * Copyright (C) 2010 The Android Open Source Project
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
import android.app.ActivityManager;
import android.app.UiModeManager;
import android.app.WallpaperManager;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceCategory;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.app.NightDisplayController;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.view.RotationPolicy;
import com.android.settings.accessibility.ToggleFontSizePreferenceFragment;
import com.android.settings.dashboard.DashboardSummary;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;

import java.util.ArrayList;
import java.util.List;

import static android.provider.Settings.Secure.CAMERA_GESTURE_DISABLED;
import static android.provider.Settings.Secure.DOUBLE_TAP_TO_WAKE;
import static android.provider.Settings.Secure.DOZE_ENABLED;
import static android.provider.Settings.Secure.WAKE_GESTURE_ENABLED;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import cyanogenmod.hardware.CMHardwareManager;

public class DisplaySettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {
    private static final String TAG = "DisplaySettings";

    /** If there is no setting in the provider, use this. */
    private static final int FALLBACK_SCREEN_TIMEOUT_VALUE = 30000;

    private static final String KEY_CATEGORY_DISPLAY = "display";
    private static final String KEY_SCREEN_TIMEOUT = "screen_timeout";
    private static final String KEY_SCREEN_SAVER = "screensaver";
    private static final String KEY_LIFT_TO_WAKE = "lift_to_wake";
    private static final String KEY_TAP_TO_WAKE = "tap_to_wake";
    private static final String KEY_AUTO_BRIGHTNESS = "auto_brightness";
    private static final String KEY_AUTO_ROTATE = "auto_rotate";
    private static final String KEY_NIGHT_DISPLAY = "night_display";
    private static final String KEY_NIGHT_MODE = "night_mode";
    private static final String KEY_CAMERA_GESTURE = "camera_gesture";
    private static final String KEY_WALLPAPER = "wallpaper";
    private static final String KEY_VR_DISPLAY_PREF = "vr_display_pref";

    private ListPreference mNightModePreference;

    private Preference mFontSizePref;
    private Preference mScreenSaverPreference;

    private SwitchPreference mLiftToWakePreference;
    private SwitchPreference mTapToWakePreference;
    private SwitchPreference mAutoBrightnessPreference;
    private SwitchPreference mCameraGesturePreference;

    private SwitchPreference mProximityCheckOnWakePreference;
    private SwitchPreference mWakeWhenPluggedOrUnplugged;

    private TimeoutListPreference mScreenTimeoutPreference;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.DISPLAY;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Activity activity = getActivity();
        final ContentResolver resolver = activity.getContentResolver();

        addPreferencesFromResource(R.xml.display_settings);

        PreferenceCategory displayPrefs = (PreferenceCategory)
        findPreference(KEY_CATEGORY_DISPLAY);

        mScreenSaverPreference = findPreference(KEY_SCREEN_SAVER);
        if (mScreenSaverPreference != null &&
            getResources().getBoolean(
                com.android.internal.R.bool.config_dreamsSupported) == false) {
            getPreferenceScreen().removePreference(mScreenSaverPreference);
        }

        mScreenTimeoutPreference = (TimeoutListPreference) findPreference(KEY_SCREEN_TIMEOUT);

        if (displayPrefs != null) {
            mAutoBrightnessPreference = (SwitchPreference) findPreference(KEY_AUTO_BRIGHTNESS);
            if (mAutoBrightnessPreference != null) {
                if (isAutomaticBrightnessAvailable(getResources())) {
                    mAutoBrightnessPreference.setOnPreferenceChangeListener(this);
                } else {
                    displayPrefs.removePreference(mAutoBrightnessPreference);
                }
            }

            if (!NightDisplayController.isAvailable(activity)) {
                removePreference(KEY_NIGHT_DISPLAY);
            }

            mLiftToWakePreference = (SwitchPreference) findPreference(KEY_LIFT_TO_WAKE);
            if (mLiftToWakePreference != null) {
                if (isLiftToWakeAvailable(activity)) {
                    mLiftToWakePreference.setOnPreferenceChangeListener(this);
                } else {
                    displayPrefs.removePreference(mLiftToWakePreference);
                }
            }

            mTapToWakePreference = (SwitchPreference) findPreference(KEY_TAP_TO_WAKE);
            if (mTapToWakePreference != null) {
                if (isTapToWakeAvailable(getResources())) {
                    mTapToWakePreference.setOnPreferenceChangeListener(this);
                } else {
                    displayPrefs.removePreference(mTapToWakePreference);
                }
            }

            mCameraGesturePreference = (SwitchPreference) findPreference(KEY_CAMERA_GESTURE);
            if (mCameraGesturePreference != null) {
                if (isCameraGestureAvailable(getResources())) {
                    mCameraGesturePreference.setOnPreferenceChangeListener(this);
                } else {
                    displayPrefs.removePreference(mCameraGesturePreference);
                }
            }

            DropDownPreference rotatePreference =
                (DropDownPreference) findPreference(KEY_AUTO_ROTATE);
            if (rotatePreference != null) {
                if (RotationPolicy.isRotationLockToggleVisible(activity)) {
                    int rotateLockedResourceId;
                    // The following block sets the string used when rotation is locked.
                    // If the device locks specifically to portrait or landscape (rather than current
                    // rotation), then we use a different string to include this information.
                    if (allowAllRotations(activity)) {
                        rotateLockedResourceId = R.string.display_auto_rotate_stay_in_current;
                    } else {
                        if (RotationPolicy.getRotationLockOrientation(activity) ==
                            Configuration.ORIENTATION_PORTRAIT) {
                            rotateLockedResourceId =
                                R.string.display_auto_rotate_stay_in_portrait;
                        } else {
                            rotateLockedResourceId =
                                R.string.display_auto_rotate_stay_in_landscape;
                        }
                    }
                    rotatePreference.setEntries(new CharSequence[] {
                        activity.getString(R.string.display_auto_rotate_rotate),
                            activity.getString(rotateLockedResourceId),
                    });
                    rotatePreference.setEntryValues(new CharSequence[] {
                        "0",
                        "1"
                    });
                    rotatePreference.setValueIndex(RotationPolicy.isRotationLocked(activity) ?
                        1 : 0);
                    rotatePreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            final boolean locked = Integer.parseInt((String) newValue) != 0;
                            MetricsLogger.action(getActivity(), MetricsEvent.ACTION_ROTATION_LOCK,
                                locked);
                            RotationPolicy.setRotationLock(activity, locked);
                            return true;
                        }
                    });
                } else {
                    displayPrefs.removePreference(rotatePreference);
                }
            }


            DropDownPreference vrDisplayPref =
                (DropDownPreference) findPreference(KEY_VR_DISPLAY_PREF);
            if (vrDisplayPref != null) {
                if (isVrDisplayModeAvailable(activity)) {
                    vrDisplayPref.setEntries(new CharSequence[] {
                        activity.getString(R.string.display_vr_pref_low_persistence),
                            activity.getString(R.string.display_vr_pref_off),
                    });
                    vrDisplayPref.setEntryValues(new CharSequence[] {
                        "0",
                        "1"
                    });

                    final Context c = activity;
                    int currentUser = ActivityManager.getCurrentUser();
                    int current = Settings.Secure.getIntForUser(c.getContentResolver(),
                        Settings.Secure.VR_DISPLAY_MODE,
                        /*default*/
                        Settings.Secure.VR_DISPLAY_MODE_LOW_PERSISTENCE,
                        currentUser);
                    vrDisplayPref.setValueIndex(current);
                    vrDisplayPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            int i = Integer.parseInt((String) newValue);
                            int u = ActivityManager.getCurrentUser();
                            if (!Settings.Secure.putIntForUser(c.getContentResolver(),
                                    Settings.Secure.VR_DISPLAY_MODE,
                                    i, u)) {
                                Log.e(TAG, "Could not change setting for " +
                                    Settings.Secure.VR_DISPLAY_MODE);
                            }
                            return true;
                        }
                    });
                } else {
                    displayPrefs.removePreference(vrDisplayPref);
                }
            }
        }

        mNightModePreference = (ListPreference) findPreference(KEY_NIGHT_MODE);
        if (mNightModePreference != null) {
            final UiModeManager uiManager = (UiModeManager) getSystemService(
                Context.UI_MODE_SERVICE);
            final int currentNightMode = uiManager.getNightMode();
            mNightModePreference.setValue(String.valueOf(currentNightMode));
            mNightModePreference.setOnPreferenceChangeListener(this);
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

    private static boolean isTapToWakeAvailable(Resources res) {
        return res.getBoolean(com.android.internal.R.bool.config_supportDoubleTapWake);
    }

    private static boolean isAutomaticBrightnessAvailable(Resources res) {
        return res.getBoolean(com.android.internal.R.bool.config_automatic_brightness_available);
    }

    private static boolean isCameraGestureAvailable(Resources res) {
        boolean configSet = res.getInteger(
                com.android.internal.R.integer.config_cameraLaunchGestureSensorType) != -1;
        return configSet &&
                !SystemProperties.getBoolean("gesture.disable_camera_launch", false);
    }

    private static boolean isVrDisplayModeAvailable(Context context) {
        PackageManager pm = context.getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_VR_MODE_HIGH_PERFORMANCE);
    }

    private void updateTimeoutPreferenceDescription(long currentTimeout) {
        TimeoutListPreference preference = mScreenTimeoutPreference;
        String summary;
        if (preference.isDisabledByAdmin()) {
            summary = getString(R.string.disabled_by_policy_title);
        } else if (currentTimeout < 0) {
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
                summary = getString(R.string.screen_timeout_summary, entries[best]);
            }
        }
        preference.setSummary(summary);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateState();

        final long currentTimeout = Settings.System.getLong(getActivity().getContentResolver(),
                SCREEN_OFF_TIMEOUT, FALLBACK_SCREEN_TIMEOUT_VALUE);
        mScreenTimeoutPreference.setValue(String.valueOf(currentTimeout));
        mScreenTimeoutPreference.setOnPreferenceChangeListener(this);
        final DevicePolicyManager dpm = (DevicePolicyManager) getActivity().getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        if (dpm != null) {
            final EnforcedAdmin admin = RestrictedLockUtils.checkIfMaximumTimeToLockIsSet(
                    getActivity());
            final long maxTimeout = dpm
                    .getMaximumTimeToLockForUserAndProfiles(UserHandle.myUserId());
            mScreenTimeoutPreference.removeUnusableTimeouts(maxTimeout, admin);
        }
        updateTimeoutPreferenceDescription(currentTimeout);

        disablePreferenceIfManaged(KEY_WALLPAPER, UserManager.DISALLOW_SET_WALLPAPER);
    }

    private void updateState() {
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

        // Update tap to wake if it is available.
        if (mTapToWakePreference != null) {
            int value = Settings.Secure.getInt(getContentResolver(), DOUBLE_TAP_TO_WAKE, 0);
            mTapToWakePreference.setChecked(value != 0);
        }

        // Update camera gesture #1 if it is available.
        if (mCameraGesturePreference != null) {
            int value = Settings.Secure.getInt(getContentResolver(), CAMERA_GESTURE_DISABLED, 0);
            mCameraGesturePreference.setChecked(value == 0);
        }
    }

    private void updateScreenSaverSummary() {
        if (mScreenSaverPreference != null) {
            mScreenSaverPreference.setSummary(
                    DreamSettings.getSummaryTextWithDreamName(getActivity()));
        }
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
        if (preference == mAutoBrightnessPreference) {
            boolean auto = (Boolean) objValue;
            Settings.System.putInt(getContentResolver(), SCREEN_BRIGHTNESS_MODE,
                    auto ? SCREEN_BRIGHTNESS_MODE_AUTOMATIC : SCREEN_BRIGHTNESS_MODE_MANUAL);
        }
        if (preference == mLiftToWakePreference) {
            boolean value = (Boolean) objValue;
            Settings.Secure.putInt(getContentResolver(), WAKE_GESTURE_ENABLED, value ? 1 : 0);
        }
        if (preference == mTapToWakePreference) {
            boolean value = (Boolean) objValue;
            Settings.Secure.putInt(getContentResolver(), DOUBLE_TAP_TO_WAKE, value ? 1 : 0);
        }
        if (preference == mCameraGesturePreference) {
            boolean value = (Boolean) objValue;
            Settings.Secure.putInt(getContentResolver(), CAMERA_GESTURE_DISABLED,
                    value ? 0 : 1 /* Backwards because setting is for disabling */);
        }
        if (preference == mNightModePreference) {
            try {
                final int value = Integer.parseInt((String) objValue);
                final UiModeManager uiManager = (UiModeManager) getSystemService(
                        Context.UI_MODE_SERVICE);
                uiManager.setNightMode(value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist night mode setting", e);
            }
        }
        return true;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_uri_display;
    }

    private void disablePreferenceIfManaged(String key, String restriction) {
        final RestrictedPreference pref = (RestrictedPreference) findPreference(key);
        if (pref != null) {
            pref.setDisabledByAdmin(null);
            if (RestrictedLockUtils.hasBaseUserRestriction(getActivity(), restriction,
                    UserHandle.myUserId())) {
                pref.setEnabled(false);
            } else {
                pref.checkRestrictionAndSetDisabled(restriction);
            }
        }
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {
        private final Context mContext;
        private final SummaryLoader mLoader;

        private SummaryProvider(Context context, SummaryLoader loader) {
            mContext = context;
            mLoader = loader;
        }

        @Override
        public void setListening(boolean listening) {
            if (listening) {
                updateSummary();
            }
        }

        private void updateSummary() {
            boolean auto = Settings.System.getInt(mContext.getContentResolver(),
                    SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
                    == SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
            mLoader.setSummary(this, mContext.getString(auto ? R.string.display_summary_on
                    : R.string.display_summary_off));
        }
    }

    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY
            = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity,
                                                                   SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };

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
                    if (!NightDisplayController.isAvailable(context)) {
                        result.add(KEY_NIGHT_DISPLAY);
                    }
                    if (!isLiftToWakeAvailable(context)) {
                        result.add(KEY_LIFT_TO_WAKE);
                    }
                    if (!RotationPolicy.isRotationLockToggleVisible(context)) {
                        result.add(KEY_AUTO_ROTATE);
                    }
                    if (!isTapToWakeAvailable(context.getResources())) {
                        result.add(KEY_TAP_TO_WAKE);
                    }
                    if (!isCameraGestureAvailable(context.getResources())) {
                        result.add(KEY_CAMERA_GESTURE);
                    }
                    if (!isVrDisplayModeAvailable(context)) {
                        result.add(KEY_VR_DISPLAY_PREF);
                    }
                    if (!context.getResources().getBoolean(
                            org.cyanogenmod.platform.internal.R.bool.config_proximityCheckOnWake)) {
                        result.add("proximity_on_wake");
                    }
                    if (!CMHardwareManager.getInstance(context).
                            isSupported(CMHardwareManager.FEATURE_HIGH_TOUCH_SENSITIVITY)) {
                        result.add("high_touch_sensitivity_enable");
                    }
                    return result;
                }
            };
}

