/*
 * Copyright (C) 2015 The CyanogenMod Project
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
package com.android.settings.livedisplay;

import static cyanogenmod.hardware.CMHardwareManager.FEATURE_ADAPTIVE_BACKLIGHT;
import static cyanogenmod.hardware.CMHardwareManager.FEATURE_COLOR_ENHANCEMENT;
import static cyanogenmod.hardware.CMHardwareManager.FEATURE_DISPLAY_MODES;
import static cyanogenmod.hardware.CMHardwareManager.FEATURE_DISPLAY_GAMMA_CALIBRATION;
import static cyanogenmod.hardware.CMHardwareManager.FEATURE_SUNLIGHT_ENHANCEMENT;

import android.app.Activity;
import android.database.ContentObserver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.util.Log;

import com.android.internal.util.ArrayUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import cyanogenmod.hardware.CMHardwareManager;
import cyanogenmod.hardware.DisplayMode;

import java.util.ArrayList;
import java.util.List;

public class LiveDisplay extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {

    private static final String TAG = "LiveDisplay";

    private static final String KEY_CATEGORY_LIVE_DISPLAY = "live_display_options";
    private static final String KEY_CATEGORY_ADVANCED = "advanced";

    private static final String KEY_LIVE_DISPLAY = "live_display";
    private static final String KEY_LIVE_DISPLAY_AUTO_OUTDOOR_MODE =
            "display_auto_outdoor_mode";
    private static final String KEY_LIVE_DISPLAY_LOW_POWER = "display_low_power";
    private static final String KEY_LIVE_DISPLAY_COLOR_ENHANCE = "display_color_enhance";
    private static final String KEY_LIVE_DISPLAY_TEMPERATURE = "live_display_color_temperature";

    private static final String KEY_DISPLAY_COLOR = "color_calibration";
    private static final String KEY_DISPLAY_GAMMA = "gamma_tuning";
    private static final String KEY_SCREEN_COLOR_SETTINGS = "screencolor_settings";

    private static final String KEY_LIVE_DISPLAY_COLOR_PROFILE = "live_display_color_profile";

    public static final int MODE_OFF = 0;
    public static final int MODE_NIGHT = 1;
    public static final int MODE_AUTO = 2;
    public static final int MODE_OUTDOOR = 3;
    public static final int MODE_DAY = 4;

    private final Handler mHandler = new Handler();
    private final SettingsObserver mObserver = new SettingsObserver();

    private ListPreference mLiveDisplay;

    private SwitchPreference mColorEnhancement;
    private SwitchPreference mLowPower;
    private SwitchPreference mOutdoorMode;

    private PreferenceScreen mScreenColorSettings;
    private DisplayTemperature mDisplayTemperature;

    private ListPreference mColorProfile;

    private String[] mModeEntries;
    private String[] mModeValues;
    private String[] mModeSummaries;

    private int mDefaultDayTemperature;
    private int mDefaultNightTemperature;

    private boolean mHasDisplayModes = false;

    private CMHardwareManager mHardware;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Activity activity = getActivity();
        final ContentResolver resolver = activity.getContentResolver();
        final Resources res = getResources();

        mDefaultDayTemperature = res.getInteger(
                com.android.internal.R.integer.config_dayColorTemperature);
        mDefaultNightTemperature = res.getInteger(
                com.android.internal.R.integer.config_nightColorTemperature);

        mHardware = CMHardwareManager.getInstance(activity);

        addPreferencesFromResource(R.xml.livedisplay);

        PreferenceCategory liveDisplayPrefs = (PreferenceCategory)
                findPreference(KEY_CATEGORY_LIVE_DISPLAY);
        PreferenceCategory advancedPrefs = (PreferenceCategory)
                findPreference(KEY_CATEGORY_ADVANCED);

        int adaptiveMode = Settings.System.getIntForUser(resolver,
                Settings.System.DISPLAY_TEMPERATURE_MODE,
                0, UserHandle.USER_CURRENT);
        mLiveDisplay = (ListPreference) findPreference(KEY_LIVE_DISPLAY);
        mLiveDisplay.setValue(String.valueOf(adaptiveMode));

        mModeEntries = res.getStringArray(
                com.android.internal.R.array.live_display_entries);
        mModeValues = res.getStringArray(
                com.android.internal.R.array.live_display_values);
        mModeSummaries = res.getStringArray(
                com.android.internal.R.array.live_display_summaries);

        // Remove outdoor mode from lists if there is no support
        if (!mHardware.isSupported(FEATURE_SUNLIGHT_ENHANCEMENT)) {
            int idx = ArrayUtils.indexOf(mModeValues, String.valueOf(MODE_OUTDOOR));
            String[] entriesTemp = new String[mModeEntries.length - 1];
            String[] valuesTemp = new String[mModeValues.length - 1];
            String[] summariesTemp = new String[mModeSummaries.length - 1];
            int j = 0;
            for (int i = 0; i < mModeEntries.length; i++) {
                if (i == idx) {
                    continue;
                }
                entriesTemp[j] = mModeEntries[i];
                valuesTemp[j] = mModeValues[i];
                summariesTemp[j] = mModeSummaries[i];
                j++;
            }
            mModeEntries = entriesTemp;
            mModeValues = valuesTemp;
            mModeSummaries = summariesTemp;
        }

        mLiveDisplay.setEntries(mModeEntries);
        mLiveDisplay.setEntryValues(mModeValues);
        mLiveDisplay.setOnPreferenceChangeListener(this);

        mDisplayTemperature = (DisplayTemperature) findPreference(KEY_LIVE_DISPLAY_TEMPERATURE);

        mColorProfile = (ListPreference) findPreference(KEY_LIVE_DISPLAY_COLOR_PROFILE);
        if (liveDisplayPrefs != null && mColorProfile != null
                && (!mHardware.isSupported(FEATURE_DISPLAY_MODES) || !updateDisplayModes())) {
            liveDisplayPrefs.removePreference(mColorProfile);
        } else {
            mHasDisplayModes = true;
            mColorProfile.setOnPreferenceChangeListener(this);
        }

        mOutdoorMode = (SwitchPreference) findPreference(KEY_LIVE_DISPLAY_AUTO_OUTDOOR_MODE);
        if (liveDisplayPrefs != null && mOutdoorMode != null
                && !mHardware.isSupported(FEATURE_SUNLIGHT_ENHANCEMENT)) {
            liveDisplayPrefs.removePreference(mOutdoorMode);
            mOutdoorMode = null;
        }

        mLowPower = (SwitchPreference) findPreference(KEY_LIVE_DISPLAY_LOW_POWER);
        if (advancedPrefs != null && mLowPower != null
                && !mHardware.isSupported(FEATURE_ADAPTIVE_BACKLIGHT)) {
            advancedPrefs.removePreference(mLowPower);
            mLowPower = null;
        }

        mColorEnhancement = (SwitchPreference) findPreference(KEY_LIVE_DISPLAY_COLOR_ENHANCE);
        if (advancedPrefs != null && mColorEnhancement != null
                && !mHardware.isSupported(FEATURE_COLOR_ENHANCEMENT)) {
            advancedPrefs.removePreference(mColorEnhancement);
            mColorEnhancement = null;
        }

        if (advancedPrefs != null
                && !mHardware.isSupported(FEATURE_DISPLAY_GAMMA_CALIBRATION)) {
            Preference gammaPref = findPreference(KEY_DISPLAY_GAMMA);
            if (gammaPref != null) {
                advancedPrefs.removePreference(gammaPref);
            }
        }

        mScreenColorSettings = (PreferenceScreen) findPreference(KEY_SCREEN_COLOR_SETTINGS);
        if (advancedPrefs != null) {
            if (mScreenColorSettings != null &&
                    (mHasDisplayModes ||!isPostProcessingSupported(getActivity()))) {
                advancedPrefs.removePreference(mScreenColorSettings);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateModeSummary();
        updateTemperatureSummary();
        updateColorProfileSummary(null);
        mObserver.register(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        mObserver.register(false);
    }

    private boolean updateDisplayModes() {
        final DisplayMode[] modes = mHardware.getDisplayModes();
        if (modes == null || modes.length == 0) {
            return false;
        }

        final DisplayMode cur = mHardware.getDefaultDisplayMode();
        int curId = -1;
        String[] entries = new String[modes.length];
        String[] values = new String[modes.length];
        String[] summaries = new String[modes.length];
        for (int i = 0; i < modes.length; i++) {
            values[i] = String.valueOf(modes[i].id);
            entries[i] = modes[i].name;
            summaries[i] = modes[i].name;
            if (cur != null && modes[i].id == cur.id) {
                curId = -1;
            }
        }
        mColorProfile.setEntries(entries);
        mColorProfile.setEntryValues(values);
        if (curId >= 0) {
            mColorProfile.setValue(String.valueOf(curId));
        }

        return true;
    }

    private void updateColorProfileSummary(String value) {
        if (!mHasDisplayModes) {
            return;
        }

        if (value == null) {
            DisplayMode cur = mHardware.getDefaultDisplayMode();
            if (cur != null && cur.id >= 0) {
                value = String.valueOf(cur.id);
            }
        }

        int idx = mColorProfile.findIndexOfValue(value);
        if (idx < 0) {
            Log.e(TAG, "No summary resource found for profile " + value);
            mColorProfile.setSummary(null);
            return;
        }

        mColorProfile.setValue(value);

        String entry = mColorProfile.getEntries()[idx].toString();
        String name = entry.toLowerCase().replace(" ", "_");
        String summaryRes = String.format("live_display_color_profile_%s_summary", name);
        int resId = getResources().getIdentifier(summaryRes, "string", "com.android.settings");
        if (resId <= 0) {
            Log.e(TAG, "No summary resource found for profile " + name);
            mColorProfile.setSummary(null);
            return;
        }
        mColorProfile.setSummary(String.format("%s - %s", entry.toString(),
                getResources().getString(resId)));
    }

    private void updateModeSummary() {
        int mode = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.DISPLAY_TEMPERATURE_MODE,
                MODE_OFF, UserHandle.USER_CURRENT);

        int index = ArrayUtils.indexOf(mModeValues, String.valueOf(mode));
        if (index < 0) {
            index = ArrayUtils.indexOf(mModeValues, String.valueOf(MODE_OFF));
        }

        mLiveDisplay.setSummary(mModeSummaries[index]);

        if (mDisplayTemperature != null) {
            mDisplayTemperature.setEnabled(mode != MODE_OFF);
        }
        if (mOutdoorMode != null) {
            mOutdoorMode.setEnabled(mode != MODE_OFF);
        }
    }

    private void updateTemperatureSummary() {
        int day = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.DISPLAY_TEMPERATURE_DAY,
                mDefaultDayTemperature,
                UserHandle.USER_CURRENT);
        int night = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.DISPLAY_TEMPERATURE_NIGHT,
                mDefaultNightTemperature,
                UserHandle.USER_CURRENT);

        mDisplayTemperature.setSummary(getResources().getString(
                R.string.live_display_color_temperature_summary, day, night));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mLiveDisplay) {
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.DISPLAY_TEMPERATURE_MODE,
                    Integer.valueOf((String)objValue), UserHandle.USER_CURRENT);
        } else if (preference == mColorProfile) {
            int id = Integer.valueOf((String)objValue);
            Log.i("LiveDisplay", "Setting mode: " + id);
            for (DisplayMode mode : mHardware.getDisplayModes()) {
                if (mode.id == id) {
                    mHardware.setDisplayMode(mode, true);
                    updateColorProfileSummary((String)objValue);
                    break;
                }
            }
        }
        return true;
    }

    private static boolean isPostProcessingSupported(Context context) {
        return Utils.isPackageInstalled(context, "com.qualcomm.display");
    }

    private final class SettingsObserver extends ContentObserver {
        private final Uri DISPLAY_TEMPERATURE_DAY_URI =
                Settings.System.getUriFor(Settings.System.DISPLAY_TEMPERATURE_DAY);
        private final Uri DISPLAY_TEMPERATURE_NIGHT_URI =
                Settings.System.getUriFor(Settings.System.DISPLAY_TEMPERATURE_NIGHT);
        private final Uri DISPLAY_TEMPERATURE_MODE_URI =
                Settings.System.getUriFor(Settings.System.DISPLAY_TEMPERATURE_MODE);

        public SettingsObserver() {
            super(mHandler);
        }

        public void register(boolean register) {
            final ContentResolver cr = getContentResolver();
            if (register) {
                cr.registerContentObserver(DISPLAY_TEMPERATURE_DAY_URI, false, this, UserHandle.USER_ALL);
                cr.registerContentObserver(DISPLAY_TEMPERATURE_NIGHT_URI, false, this, UserHandle.USER_ALL);
                cr.registerContentObserver(DISPLAY_TEMPERATURE_MODE_URI, false, this, UserHandle.USER_ALL);
            } else {
                cr.unregisterContentObserver(this);
            }
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange,  uri);
            updateModeSummary();
            updateTemperatureSummary();
        }
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
        private boolean mHasSunlightEnhancement, mHasColorEnhancement, mHasLowPower;
        private boolean mHasDisplayGamma;

        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                boolean enabled) {
            ArrayList<SearchIndexableResource> result =
                    new ArrayList<SearchIndexableResource>();

            SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = R.xml.livedisplay;
            result.add(sir);

            return result;
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
             CMHardwareManager hardware = CMHardwareManager.getInstance(context);

            ArrayList<String> result = new ArrayList<String>();
            if (!hardware.isSupported(FEATURE_DISPLAY_MODES)) {
                result.add(KEY_LIVE_DISPLAY_COLOR_PROFILE);
            }
            if (!hardware.isSupported(FEATURE_SUNLIGHT_ENHANCEMENT)) {
                result.add(KEY_LIVE_DISPLAY_AUTO_OUTDOOR_MODE);
            }
            if (!hardware.isSupported(FEATURE_COLOR_ENHANCEMENT)) {
                result.add(KEY_LIVE_DISPLAY_COLOR_ENHANCE);
            }
            if (!hardware.isSupported(FEATURE_ADAPTIVE_BACKLIGHT)) {
                result.add(KEY_LIVE_DISPLAY_LOW_POWER);
            }
            if (!isPostProcessingSupported(context)) {
                result.add(KEY_SCREEN_COLOR_SETTINGS);
            } else {
                result.add(KEY_DISPLAY_COLOR);
            }
            if (!hardware.isSupported(FEATURE_DISPLAY_GAMMA_CALIBRATION)) {
                result.add(KEY_DISPLAY_GAMMA);
            }
            return result;
        }
    };
}
