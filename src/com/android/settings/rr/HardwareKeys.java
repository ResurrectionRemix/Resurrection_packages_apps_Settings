/*
 * Copyright (C) 2017 The Dirty Unicorns Project
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

package com.android.settings.rr;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.PowerManager;
import android.os.ServiceManager;
import android.provider.SearchIndexableResource;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v14.preference.SwitchPreference;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable.SearchIndexProvider;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.smartnav.ActionFragment;

import com.android.internal.utils.ActionConstants;
import com.android.internal.utils.ActionUtils;
import com.android.settings.rr.Preferences.CustomSeekBarPreference;
import com.android.settings.rr.utils.TelephonyUtils;
import com.android.settings.rr.input.*;
import lineageos.providers.LineageSettings;

import java.util.ArrayList;
import java.util.List;

public class HardwareKeys extends ActionFragment implements Preference.OnPreferenceChangeListener {
    private static final String HWKEY_DISABLE = "hardware_keys_disable";

    // category keys
    private static final String CATEGORY_HWKEY = "hardware_keys";
    private static final String CATEGORY_BACK = "back_key";
    private static final String CATEGORY_HOME = "home_key";
    private static final String CATEGORY_MENU = "menu_key";
    private static final String CATEGORY_ASSIST = "assist_key";
    private static final String CATEGORY_APPSWITCH = "app_switch_key";
    private static final String CATEGORY_VOLUME = "volume_keys";
    private static final String CATEGORY_POWER = "power_key";
    private static final String KEY_HOME_ANSWER_CALL = "home_answer_call";
    private static final String KEY_BUTTON_BACKLIGHT = "button_backlight";
    private static final String KEY_HOME_WAKE_SCREEN = "home_wake_screen";
    // Masks for checking presence of hardware keys.
    // Must match values in frameworks/base/core/res/res/values/config.xml
    public static final int KEY_MASK_HOME = 0x01;
    public static final int KEY_MASK_BACK = 0x02;
    public static final int KEY_MASK_MENU = 0x04;
    public static final int KEY_MASK_ASSIST = 0x08;
    public static final int KEY_MASK_APP_SWITCH = 0x10;
    public static final int KEY_MASK_CAMERA = 0x20;
    public static final int KEY_MASK_VOLUME = 0x40;

    private SwitchPreference mHwKeyDisable;

    private CustomSeekBarPreference mButtonTimoutBar;
    private CustomSeekBarPreference mManualButtonBrightness;
    private PreferenceCategory mButtonBackLightCategory;
    private SwitchPreference mHomeAnswerCall;
    private SwitchPreference mHomeWakeScreen;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rr_hw_keys);

        final PreferenceScreen prefScreen = getPreferenceScreen();
        ContentResolver resolver = getContentResolver();

        final boolean needsNavbar = ActionUtils.hasNavbarByDefault(getActivity());
        final PreferenceCategory hwkeyCat = (PreferenceCategory) prefScreen
                .findPreference(CATEGORY_HWKEY);
        int keysDisabled = 0;
        if (!needsNavbar) {
            mHwKeyDisable = (SwitchPreference) findPreference(HWKEY_DISABLE);
        } else {
            prefScreen.removePreference(hwkeyCat);
        }

        final ButtonBacklightBrightness backlight =
                (ButtonBacklightBrightness) findPreference(KEY_BUTTON_BACKLIGHT);
        if (!backlight.isButtonSupported() /*&& !backlight.isKeyboardSupported()*/) {
            prefScreen.removePreference(backlight);
        }

        // bits for hardware keys present on device
        final int deviceKeys = getResources().getInteger(
                org.lineageos.platform.internal.R.integer.config_deviceHardwareKeys);

        // read bits for present hardware keys
        final boolean hasHomeKey = (deviceKeys & KEY_MASK_HOME) != 0;
        final boolean hasBackKey = (deviceKeys & KEY_MASK_BACK) != 0;
        final boolean hasMenuKey = (deviceKeys & KEY_MASK_MENU) != 0;
        final boolean hasAssistKey = (deviceKeys & KEY_MASK_ASSIST) != 0;
        final boolean hasAppSwitchKey = (deviceKeys & KEY_MASK_APP_SWITCH) != 0;

        // load categories and init/remove preferences based on device
        // configuration
        final PreferenceCategory backCategory = (PreferenceCategory) prefScreen
                .findPreference(CATEGORY_BACK);
        final PreferenceCategory homeCategory = (PreferenceCategory) prefScreen
                .findPreference(CATEGORY_HOME);
        final PreferenceCategory menuCategory = (PreferenceCategory) prefScreen
                .findPreference(CATEGORY_MENU);
        final PreferenceCategory assistCategory = (PreferenceCategory) prefScreen
                .findPreference(CATEGORY_ASSIST);
        final PreferenceCategory appSwitchCategory = (PreferenceCategory) prefScreen
                .findPreference(CATEGORY_APPSWITCH);

        // Home button answers calls.
        mHomeAnswerCall = (SwitchPreference) findPreference(KEY_HOME_ANSWER_CALL);

        if (!TelephonyUtils.isVoiceCapable(getActivity())) {
            homeCategory.removePreference(mHomeAnswerCall);
            mHomeAnswerCall = null;
        }

        mHomeWakeScreen = (SwitchPreference) findPreference(KEY_HOME_WAKE_SCREEN);

        // back key
        if (!hasBackKey) {
            prefScreen.removePreference(backCategory);
        }

        // home key
        if (!hasHomeKey) {
            prefScreen.removePreference(homeCategory);
        }

        // App switch key (recents)
        if (!hasAppSwitchKey) {
            prefScreen.removePreference(appSwitchCategory);
        }

        // menu key
        if (!hasMenuKey) {
            prefScreen.removePreference(menuCategory);
        }

        // search/assist key
        if (!hasAssistKey) {
            prefScreen.removePreference(assistCategory);
        }

        // let super know we can load ActionPreferences
        onPreferenceScreenLoaded(ActionConstants.getDefaults(ActionConstants.HWKEYS));

        // load preferences first
        setActionPreferencesEnabled(keysDisabled == 0);
    }

    @Override
    protected boolean usesExtendedActionsList() {
        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mHomeAnswerCall) {
            handleToggleHomeButtonAnswersCallPreferenceClick();
            return true;
        } else if (preference == mHomeWakeScreen) {
            handleToggleHomeButtonWakeScreenPreferenceClick();
            return true;
        }

        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.RESURRECTED;
    }

    private void handleToggleHomeButtonWakeScreenPreferenceClick() {
        LineageSettings.System.putInt(getContentResolver(),
                LineageSettings.System.HOME_WAKE_SCREEN, (mHomeWakeScreen.isChecked() ? 1 : 0));
    }

    private void handleToggleHomeButtonAnswersCallPreferenceClick() {
        LineageSettings.Secure.putInt(getContentResolver(),
                LineageSettings.Secure.RING_HOME_BUTTON_BEHAVIOR, (mHomeAnswerCall.isChecked()
                        ? LineageSettings.Secure.RING_HOME_BUTTON_BEHAVIOR_ANSWER
                        : LineageSettings.Secure.RING_HOME_BUTTON_BEHAVIOR_DO_NOTHING));
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List < SearchIndexableResource > getXmlResourcesToIndex(Context context,
                boolean enabled) {
                ArrayList < SearchIndexableResource > resources =
                    new ArrayList < SearchIndexableResource > ();
                SearchIndexableResource res = new SearchIndexableResource(context);
                res.xmlResId = R.xml.rr_hw_keys;
                resources.add(res);
                return resources;
            }

            @Override
            public List < String > getNonIndexableKeys(Context context) {
                List < String > keys = super.getNonIndexableKeys(context);
                final Resources res = context.getResources();

                // bits for hardware keys present on device
                final int deviceKeys = res.getInteger(
                    org.lineageos.platform.internal.R.integer.config_deviceHardwareKeys);

                // read bits for present hardware keys
                final boolean hasHomeKey = (deviceKeys & KEY_MASK_HOME) != 0;
                final boolean hasBackKey = (deviceKeys & KEY_MASK_BACK) != 0;
                final boolean hasMenuKey = (deviceKeys & KEY_MASK_MENU) != 0;
                final boolean hasAssistKey = (deviceKeys & KEY_MASK_ASSIST) != 0;
                final boolean hasAppSwitchKey = (deviceKeys & KEY_MASK_APP_SWITCH) != 0;
                //As ButtonBacklightBrightness.isButtonSupported()
                final boolean hasBacklight = (res.getInteger(
                    com.android.internal.R.integer.config_buttonBrightnessSettingDefault) > 0) &&
                    (hasHomeKey || hasBackKey || hasMenuKey || hasAssistKey || hasAppSwitchKey);

                if (!TelephonyUtils.isVoiceCapable(context)) keys.add(KEY_HOME_ANSWER_CALL);
                if (ActionUtils.hasNavbarByDefault(context)) keys.add(CATEGORY_HWKEY);

                // back light
                if (!hasBacklight) keys.add(KEY_BUTTON_BACKLIGHT);
                // back key
                if (!hasBackKey) keys.add(CATEGORY_BACK);
                // home key
                if (!hasHomeKey) keys.add(CATEGORY_HOME);
                // App switch key (recents)
                if (!hasAppSwitchKey) keys.add(CATEGORY_APPSWITCH);
                // menu key
                if (!hasMenuKey) keys.add(CATEGORY_MENU);
                // search/assist key
                if (!hasAssistKey) keys.add(CATEGORY_ASSIST);

                return keys;
            }
        };
}
