/*
 * Copyright (C) 2020 The Dirty Unicorns Project
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

package com.android.settings.rr.fragments;

import java.util.ArrayList;
import java.util.List;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.search.SearchIndexable;
import com.android.settings.rr.Preferences.*;

import android.app.ActionBar;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.UserHandle;
import android.content.ContentResolver;
import androidx.preference.PreferenceCategory;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;
import com.android.settings.Utils;
import com.android.settings.rr.utils.RRUtils;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;
import android.provider.Settings;
import android.provider.SearchIndexableResource;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
@SearchIndexable
public class PulseSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {
    private static final String TAG = PulseSettings.class.getSimpleName();
    private static final String NAVBAR_PULSE_ENABLED_KEY = "navbar_pulse_enabled";
    private static final String LOCKSCREEN_PULSE_ENABLED_KEY = "lockscreen_pulse_enabled";
    private static final String PULSE_SMOOTHING_KEY = "pulse_smoothing_enabled";
    private static final String PULSE_COLOR_MODE_KEY = "pulse_color_mode";
    private static final String PULSE_COLOR_MODE_CHOOSER_KEY = "pulse_color_user";
    private static final String PULSE_COLOR_MODE_LAVA_SPEED_KEY = "pulse_lavalamp_speed";
    private static final String PULSE_RENDER_CATEGORY_SOLID = "pulse_2";
    private static final String PULSE_RENDER_CATEGORY_FADING = "pulse_fading_bars_category";
    private static final String PULSE_RENDER_MODE_KEY = "pulse_render_style";
    private static final int RENDER_STYLE_FADING_BARS = 0;
    private static final int RENDER_STYLE_SOLID_LINES = 1;
    private static final int COLOR_TYPE_ACCENT = 0;
    private static final int COLOR_TYPE_USER = 1;
    private static final int COLOR_TYPE_LAVALAMP = 2;
    private static final int COLOR_TYPE_AUTO = 3;

    private SwitchPreference mNavbarPulse;
    private SwitchPreference mLockscreenPulse;
    private SwitchPreference mPulseSmoothing;
    private Preference mRenderMode;
    private ListPreference mColorModePref;
    private SystemSettingColorPickerPreference mColorPickerPref;
    private Preference mLavaSpeedPref;
    protected Context mContext;

    private PreferenceCategory mFadingBarsCat;
    private PreferenceCategory mSolidBarsCat;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pulse_settings);
        mContext = getActivity().getApplicationContext();
        mFooterPreferenceMixin.createFooterPreference()
                .setTitle(R.string.pulse_help_policy_notice_summary);

        ContentResolver resolver = getContentResolver();
        mNavbarPulse = (SwitchPreference) findPreference(NAVBAR_PULSE_ENABLED_KEY);
        boolean navbarPulse = Settings.System.getIntForUser(resolver,
                Settings.System.NAVBAR_PULSE_ENABLED, 0, UserHandle.USER_CURRENT) != 0;
        mNavbarPulse.setChecked(navbarPulse);
        mNavbarPulse.setOnPreferenceChangeListener(this);
        mLockscreenPulse = (SwitchPreference) findPreference(LOCKSCREEN_PULSE_ENABLED_KEY);
        boolean lockscreenPulse = Settings.System.getIntForUser(resolver,
                Settings.System.LOCKSCREEN_PULSE_ENABLED, 0, UserHandle.USER_CURRENT) != 0;
        mLockscreenPulse.setChecked(lockscreenPulse);
        mLockscreenPulse.setOnPreferenceChangeListener(this);

        mColorModePref = (ListPreference) findPreference(PULSE_COLOR_MODE_KEY);
        mColorPickerPref = (SystemSettingColorPickerPreference) findPreference(PULSE_COLOR_MODE_CHOOSER_KEY);
        int Color = Settings.System.getInt(getContentResolver(),
                Settings.System.PULSE_COLOR_USER, 0x3980FF);
        mColorPickerPref.setOnPreferenceChangeListener(this);
        String ColorHex = convertToRGB(Color);
        mColorPickerPref.setSummary(ColorHex);
        mLavaSpeedPref = findPreference(PULSE_COLOR_MODE_LAVA_SPEED_KEY);
        mColorModePref.setOnPreferenceChangeListener(this);
        int colorMode = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.PULSE_COLOR_MODE, COLOR_TYPE_ACCENT, UserHandle.USER_CURRENT);
        updateColorPrefs(colorMode);
        
        mRenderMode = findPreference(PULSE_RENDER_MODE_KEY);
        mRenderMode.setOnPreferenceChangeListener(this);
        mFadingBarsCat = (PreferenceCategory) findPreference(
                PULSE_RENDER_CATEGORY_FADING);
        mSolidBarsCat = (PreferenceCategory) findPreference(
                PULSE_RENDER_CATEGORY_SOLID);
        mPulseSmoothing = (SwitchPreference) findPreference(PULSE_SMOOTHING_KEY);
        updateAllPrefs();
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

    public static String convertToRGB(int color) {
        String red = Integer.toHexString(Color.red(color));
        String green = Integer.toHexString(Color.green(color));
        String blue = Integer.toHexString(Color.blue(color));

        if (red.length() == 1) {
            red = "0" + red;
        }

        if (green.length() == 1) {
            green = "0" + green;
        }

        if (blue.length() == 1) {
            blue = "0" + blue;
        }

        return "#" + red + green + blue;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getContext().getContentResolver();
        if (preference == mNavbarPulse) {
            boolean val = (Boolean) newValue;
            Settings.System.putIntForUser(resolver,
                Settings.System.NAVBAR_PULSE_ENABLED, val ? 1 : 0, UserHandle.USER_CURRENT);
            updateAllPrefs();
            return true;
        } else if (preference == mLockscreenPulse) {
            boolean val = (Boolean) newValue;
            Settings.System.putIntForUser(resolver,
                Settings.System.LOCKSCREEN_PULSE_ENABLED, val ? 1 : 0, UserHandle.USER_CURRENT);
            updateAllPrefs();
            return true;
        } else if (preference == mColorModePref) {
            updateColorPrefs(Integer.valueOf(String.valueOf(newValue)));
            return true;
        } else if (preference == mRenderMode) {
            updateRenderCategories(Integer.valueOf(String.valueOf(newValue)));
            return true;
        }
        return false;
    }

    private void updateAllPrefs() {
        ContentResolver resolver = getContentResolver();
        boolean navbarPulse = Settings.System.getIntForUser(resolver,
                Settings.System.NAVBAR_PULSE_ENABLED, 0, UserHandle.USER_CURRENT) != 0;
        boolean lockscreenPulse = Settings.System.getIntForUser(resolver,
                Settings.System.LOCKSCREEN_PULSE_ENABLED, 0, UserHandle.USER_CURRENT) != 0;
        mPulseSmoothing.setEnabled(navbarPulse || lockscreenPulse);
        mColorModePref.setEnabled(navbarPulse || lockscreenPulse);
        if (navbarPulse || lockscreenPulse) {
            int colorMode = Settings.System.getIntForUser(resolver,
                Settings.System.PULSE_COLOR_MODE, COLOR_TYPE_ACCENT, UserHandle.USER_CURRENT);
            updateColorPrefs(colorMode);
        } else {
            mColorPickerPref.setEnabled(false);
            mLavaSpeedPref.setEnabled(false);
        }
        mRenderMode.setEnabled(navbarPulse || lockscreenPulse);
        if (navbarPulse || lockscreenPulse) {
            int renderMode = Settings.System.getIntForUser(resolver,
                Settings.System.PULSE_RENDER_STYLE, RENDER_STYLE_FADING_BARS, UserHandle.USER_CURRENT);
            updateRenderCategories(renderMode);
        } else {
            mFadingBarsCat.setEnabled(false);
            mSolidBarsCat.setEnabled(false);
        }
    }

    private void updateColorPrefs(int val) {
        switch (val) {
            case COLOR_TYPE_ACCENT:
                mColorPickerPref.setEnabled(false);
                mLavaSpeedPref.setEnabled(false);
                break;
            case COLOR_TYPE_USER:
                mColorPickerPref.setEnabled(true);
                mLavaSpeedPref.setEnabled(false);
                break;
            case COLOR_TYPE_LAVALAMP:
                mColorPickerPref.setEnabled(false);
                mLavaSpeedPref.setEnabled(true);
                break;
            case COLOR_TYPE_AUTO:
                mColorPickerPref.setEnabled(false);
                mLavaSpeedPref.setEnabled(false);
                break;
        }
    }

    private void updateRenderCategories(int mode) {
        mFadingBarsCat.setEnabled(mode == RENDER_STYLE_FADING_BARS);
        mSolidBarsCat.setEnabled(mode == RENDER_STYLE_SOLID_LINES);
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
                    sir.xmlResId = R.xml.pulse_settings;
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
