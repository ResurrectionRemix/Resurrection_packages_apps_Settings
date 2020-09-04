/*
 * Copyright (C) 2014-2015 The CyanogenMod Project
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

package com.android.settings.rr.input;

import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceCategory;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;

import android.provider.SearchIndexableResource;
import com.android.internal.logging.nano.MetricsProto;

import com.android.settings.rr.Preferences.*;
import com.android.settings.rr.utils.RRUtils;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;
import com.android.settings.rr.Preferences.*;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import net.margaritov.preference.colorpicker.ColorPickerPreference;
@SearchIndexable
public class PowerMenuActions extends SettingsPreferenceFragment
                implements Preference.OnPreferenceChangeListener, Indexable {
    private static final String DIM = "transparent_power_dialog_dim";
    private static final String FILTER = "power_menu_bg";

    private SystemSettingSwitchPreference mFilter;
    private SystemSettingSeekBarPreference mDim;
    private static final String KEY_POWERMENU_TORCH = "powermenu_torch";
    private static final String POWER_MENU_ANIMATIONS = "power_menu_animations";
    private static final String KEY_BG_STYLE = "power_menu_bg_style";
    private static final String KEY_BG_BLUR_RADIUS = "power_menu_bg_blur_radius";
    private static final String FILTER_COLOR = "powermenu_filter_color";
    static final int DEFAULT_QS_PANEL_COLOR = 0xffffffff;

    private SwitchPreference mPowermenuTorch;
    private ListPreference mPowerMenuAnimations;
    private SystemSettingListPreference mBgStyle;
    private SystemSettingSeekBarPreference mBlurRadius;
    private SystemSettingColorPickerPreference mFilterColor;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.power_menu_settings);

        final ContentResolver resolver = getActivity().getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();
        mPowermenuTorch = (SwitchPreference) findPreference(KEY_POWERMENU_TORCH);
        mPowermenuTorch.setOnPreferenceChangeListener(this);
        mPowermenuTorch.setChecked((Settings.System.getInt(resolver,
                Settings.System.POWERMENU_TORCH, 0) == 1));

        mFilterColor = (SystemSettingColorPickerPreference) findPreference(FILTER_COLOR);
        mFilterColor.setOnPreferenceChangeListener(this);
        int intColor = Settings.System.getIntForUser(getActivity().getContentResolver(),
                Settings.System.POWERMENU_FILTER_COLOR, DEFAULT_QS_PANEL_COLOR, UserHandle.USER_CURRENT);
        String hexColor = String.format("#%08x", (0xffffffff & intColor));
        mFilterColor.setSummary(hexColor);
        mFilterColor.setNewPreviewColor(intColor);

        mPowerMenuAnimations = (ListPreference) findPreference(POWER_MENU_ANIMATIONS);
        mPowerMenuAnimations.setValue(String.valueOf(Settings.System.getInt(
                getContentResolver(), Settings.System.POWER_MENU_ANIMATIONS, 0)));
        mPowerMenuAnimations.setSummary(mPowerMenuAnimations.getEntry());
        mPowerMenuAnimations.setOnPreferenceChangeListener(this);

        mDim = (SystemSettingSeekBarPreference) findPreference(DIM);
        mFilter = (SystemSettingSwitchPreference) findPreference(FILTER);
        mFilter.setOnPreferenceChangeListener(this);
        boolean ischecked = mFilter.isChecked();

        mBlurRadius = (SystemSettingSeekBarPreference) findPreference(KEY_BG_BLUR_RADIUS);
        mBlurRadius.setOnPreferenceChangeListener(this);
        int value = Settings.System.getInt(resolver, KEY_BG_BLUR_RADIUS, 100);
        mBlurRadius.setValue(value);

        mBgStyle = (SystemSettingListPreference) findPreference(KEY_BG_STYLE);
        mBgStyle.setOnPreferenceChangeListener(this);
        value = Settings.System.getInt(resolver, KEY_BG_STYLE, 0);
        mBgStyle.setValue(String.valueOf(value));
        mBlurRadius.setEnabled(value != 1 && value != 2); // if filter is blur
        updatecolorpref(value);
        updatepref(ischecked);
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

    public void updatecolorpref(int filter) {
        if (filter == 5 || filter == 6) {
            mFilterColor.setEnabled(true);
        } else {
            mFilterColor.setEnabled(false);
        }
    }

    public void updatepref(boolean enabled) {
        if (enabled) 
            mDim.setEnabled(false);
        else
            mDim.setEnabled(true);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mFilter) {
               boolean value = (Boolean) newValue;
               updatepref(value);
              return true;
        } else  if (preference == mPowermenuTorch) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.POWERMENU_TORCH, value ? 1 : 0);
            return true;
        } else if (preference == mPowerMenuAnimations) {
            Settings.System.putInt(getContentResolver(), Settings.System.POWER_MENU_ANIMATIONS,
                    Integer.valueOf((String) newValue));
            mPowerMenuAnimations.setValue(String.valueOf(newValue));
            mPowerMenuAnimations.setSummary(mPowerMenuAnimations.getEntry());
            return true;
        } else if (preference == mBgStyle) {
            int value = Integer.parseInt((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    KEY_BG_STYLE, value);
            mBlurRadius.setEnabled(value != 1 && value != 2); // if filter is blur
            updatecolorpref(value);
            return true;
        } else if (preference == mBlurRadius) {
            int value = (Integer) newValue;
            Settings.System.putInt(getActivity().getContentResolver(),
                    KEY_BG_BLUR_RADIUS, value);
            return true;
        } else if (preference == mFilterColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.POWERMENU_FILTER_COLOR, intHex, UserHandle.USER_CURRENT);
            return true;
        }
        return false;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.RESURRECTED;
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
                    sir.xmlResId = R.xml.power_menu_settings;
                    result.add(sir);
                    return result;
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                List<String> keys = super.getNonIndexableKeys(context);
                return keys;
            }
        };
}

