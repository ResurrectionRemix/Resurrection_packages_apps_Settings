/*Copyright (C) 2015 The ResurrectionRemix Project
     Licensed under the Apache License, Version 2.0 (the "License");
     you may not use this file except in compliance with the License.
     You may obtain a copy of the License at

          http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.
*/
package com.android.settings.rr;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Color;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceCategory;
import androidx.preference.Preference.OnPreferenceChangeListener;
import android.text.TextUtils;
import androidx.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.os.UserHandle;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.rr.Preferences.*;
import com.android.settings.rr.utils.RRUtils;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;
import lineageos.hardware.LineageHardwareManager;
import android.hardware.display.DcDimmingManager;
import android.provider.Settings;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import lineageos.preference.LineageSystemSettingSwitchPreference;
import com.android.settings.gestures.SystemNavigationPreferenceController;
import android.provider.Settings;
import android.view.ViewConfiguration;

@SearchIndexable
public class MiscInterfaceSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {
    private static final String TAG = "MiscInterfaceSettings";
    private static final String KEY_DOZE_ON_CHARGE = "doze_on_charge";
    private static final String KEY_PROX_WAKE = "proximity_on_wake";
    private static final String KEY_HIGH_TOUCH = "high_touch_sensitivity_enable";
    private static final String PIXEL = "pixel_nav_animation";

    private static final String KEY_ASPECT_RATIO_APPS_ENABLED = "aspect_ratio_apps_enabled";
    private static final String KEY_ASPECT_RATIO_APPS_LIST = "aspect_ratio_apps_list";
    private static final String KEY_ASPECT_RATIO_CATEGORY = "aspect_ratio_category";
    private static final String KEY_ASPECT_RATIO_APPS_LIST_SCROLLER = "aspect_ratio_apps_list_scroller";
    private static final String KEY_SCREENSHOT_DELAY = "screenshot_delay";
    private static final String KEY_DISPLAY_CAT = "rr_display";

    private SystemSettingSwitchPreference mAod;
    private SystemSettingSwitchPreference mPixel;
    private LineageSystemSettingSwitchPreference mWakeProx;
    private LineageSystemSettingSwitchPreference mHighTouch;
    private AppMultiSelectListPreference mAspectRatioAppsSelect;
    private ScrollAppsViewPreference mAspectRatioApps;
    private CustomSeekBarPreference mScreenshotDelay;
    private PreferenceCategory mDisplay;


    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rr_interface_other_settings);
        Context mContext = getActivity().getApplicationContext();
        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();
        mAod = (SystemSettingSwitchPreference) findPreference(KEY_DOZE_ON_CHARGE);
        mDisplay = (PreferenceCategory) findPreference(KEY_DISPLAY_CAT);
        mPixel = (SystemSettingSwitchPreference) findPreference(PIXEL);
        mWakeProx = (LineageSystemSettingSwitchPreference) findPreference(KEY_PROX_WAKE);
        mHighTouch = (LineageSystemSettingSwitchPreference) findPreference(KEY_HIGH_TOUCH);
        DcDimmingManager dm = (DcDimmingManager) mContext
                            .getSystemService(Context.DC_DIM_SERVICE);
        if (dm == null || !dm.isAvailable()) {
            mDisplay.setVisible(false);
        } 
        boolean dozeAlwaysOnDisplayAvailable = getContext().getResources().
                getBoolean(com.android.internal.R.bool.config_dozeAlwaysOnDisplayAvailable);
        if (!dozeAlwaysOnDisplayAvailable && mAod != null) {
            getPreferenceScreen().removePreference(mAod);
        }
        if (SystemNavigationPreferenceController.isEdgeToEdgeEnabled(getContext())) {
            mPixel.setEnabled(false);
            mPixel.setSummary(R.string.navbar_not_active_pulse);
        } else {
            mPixel.setEnabled(true);
        }

        mScreenshotDelay = (CustomSeekBarPreference) findPreference(KEY_SCREENSHOT_DELAY);
        int delay = (int) ViewConfiguration.get(getActivity()).getScreenshotChordKeyTimeout();
        mScreenshotDelay.setDefaultValue(delay);

        boolean hasAlertSlider = getContext().getResources().getBoolean(
                com.android.internal.R.bool.config_hasAlertSlider);
        if (!hasAlertSlider) {
             removePreference("alert_slider_notifications");
        }
        final PreferenceCategory aspectRatioCategory =
                (PreferenceCategory) getPreferenceScreen().findPreference(KEY_ASPECT_RATIO_CATEGORY);
        final boolean supportMaxAspectRatio = getResources().getBoolean(com.android.internal.R.bool.config_haveHigherAspectRatioScreen);
        if (!supportMaxAspectRatio) {
            getPreferenceScreen().removePreference(aspectRatioCategory);
        } else {
            mAspectRatioAppsSelect = (AppMultiSelectListPreference) findPreference(KEY_ASPECT_RATIO_APPS_LIST);
            mAspectRatioApps = (ScrollAppsViewPreference) findPreference(KEY_ASPECT_RATIO_APPS_LIST_SCROLLER);
            final String valuesString = Settings.System.getString(resolver, Settings.System.OMNI_ASPECT_RATIO_APPS_LIST);
            List<String> valuesList = new ArrayList<String>();
            if (!TextUtils.isEmpty(valuesString)) {
                valuesList.addAll(Arrays.asList(valuesString.split(":")));
                mAspectRatioApps.setVisible(true);
                mAspectRatioApps.setValues(valuesList);
            } else {
                mAspectRatioApps.setVisible(false);
            }
            mAspectRatioAppsSelect.setValues(valuesList);
            mAspectRatioAppsSelect.setOnPreferenceChangeListener(this);
        }
        int anim = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.RR_CONFIG_ANIM, 0);
        try {
            if (anim == 0) {
                removePreference("animation");
            } else if (anim == 1) {
                removePreference("preview");
            } else if (anim == 2) {
                removePreference("animation");
                removePreference("preview");
            }
        } catch (Exception e) {}
     
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mAspectRatioAppsSelect) {
            Collection<String> valueList = (Collection<String>) newValue;
            mAspectRatioApps.setVisible(false);
            if (valueList != null) {
                Settings.System.putString(resolver, Settings.System.OMNI_ASPECT_RATIO_APPS_LIST,
                        TextUtils.join(":", valueList));
                mAspectRatioApps.setVisible(true);
                mAspectRatioApps.setValues(valueList);
            } else {
                Settings.System.putString(resolver, Settings.System.OMNI_ASPECT_RATIO_APPS_LIST, "");
            }
            return true;
        }
        return false;
    }

     /**
     * For Search.
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean enabled) {
                ArrayList<SearchIndexableResource> result =
                    new ArrayList<SearchIndexableResource>();
                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.rr_interface_other_settings;
                    result.add(sir);
                    return result;
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                List<String> keys = super.getNonIndexableKeys(context);
                    DcDimmingManager dm = (DcDimmingManager) context
                            .getSystemService(Context.DC_DIM_SERVICE);
                    LineageHardwareManager hardware = LineageHardwareManager.getInstance(context);
                    if (!context.getResources().getBoolean(
                            org.lineageos.platform.internal.R.bool.config_proximityCheckOnWake)) {
                        keys.add(KEY_PROX_WAKE);
                    }
                    if (!hardware.isSupported(
                            LineageHardwareManager.FEATURE_HIGH_TOUCH_SENSITIVITY)) {
                        keys.add(KEY_HIGH_TOUCH);
                    }
                    if (dm == null || !dm.isAvailable()) {
                        keys.add(KEY_DISPLAY_CAT);
                        keys.add("dc_dimming");
                    }
                    boolean hasAlertSlider = context.getResources().getBoolean(
                       com.android.internal.R.bool.config_hasAlertSlider);
                    if (!hasAlertSlider) {
                        keys.add("alert_slider_notifications");
                    }
                return keys;
            }
        };
}
