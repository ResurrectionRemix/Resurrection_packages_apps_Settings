/*
 *  Copyright (C) 2015-2018 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
*/
package com.android.settings.rr.omnigears.interfacesettings;
import android.os.Bundle;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import androidx.preference.*;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;
import android.provider.Settings;
import com.android.settings.rr.Preferences.*;
import com.android.settings.rr.utils.RRUtils;
import com.android.settings.search.Indexable.SearchIndexProvider;
import android.provider.SearchIndexableResource;
import android.os.UserHandle;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
@SearchIndexable
public class LockscreenItemSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener{
    private static final String TAG = "LockscreenItemSettings";
    static final String WEATHER_STYLE = "lockscreen_weather_style";
    static final String WEATHER_CAT = "weather";
    private static final String POSITION = "lockscreen_weather_alignment";
    private static final String WEATHER_PADDING = "lockscreen_weather_padding";
    private static final String STYLE = "lockscreen_weather_selection";

    private SystemSettingListPreference mPos;
    private SystemSettingListPreference mStyle;
    private SystemSettingSeekBarPreference mPadding;
    private PreferenceCategory mWeather;
    private SystemSettingSwitchPreference mWeatherStyle;
    private int mDefaultGradientColor;
    private int mDefaultAccentColor;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.lockscreenitems);
		ContentResolver resolver = getActivity().getContentResolver();
        mWeatherStyle = (SystemSettingSwitchPreference) findPreference(WEATHER_STYLE);
        mWeather = (PreferenceCategory) findPreference (WEATHER_CAT);
        mWeatherStyle.setOnPreferenceChangeListener(this);
        mPadding = (SystemSettingSeekBarPreference) findPreference(WEATHER_PADDING);
        mPos = (SystemSettingListPreference) findPreference(POSITION);
        mPos.setOnPreferenceChangeListener(this);
        String currentClock = Settings.Secure.getString(
                resolver, Settings.Secure.LOCK_SCREEN_CUSTOM_CLOCK_FACE);
        int position = Settings.System.getInt(resolver,
                Settings.System.LOCKSCREEN_WEATHER_ALIGNMENT, 1);
        updatePaddingPref(position);
        boolean customface1 = currentClock == null ? false : (currentClock.contains("MNMLBoxClockController"));
        boolean customface2 = currentClock == null ? false : (currentClock.contains("DividedLinesClockController"));
        int accentColor = Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.ACCENT_COLOR, mDefaultGradientColor, UserHandle.USER_CURRENT);

        int gradientColor = Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.GRADIENT_COLOR_PROP, mDefaultGradientColor, UserHandle.USER_CURRENT);
        String[] defaultgrad = getResources().getStringArray(
                R.array.lockscreen_date_selection_entries);
        String[] defaultgradentries = getResources().getStringArray(
                R.array.lockscreen_date_selection_values);
        String[] systementries = getResources().getStringArray(
                R.array.lockscreen_date_selection2_entries);
        String[] systemvalues = getResources().getStringArray(
                R.array.lockscreen_date_selection2_values);
        mStyle = (SystemSettingListPreference) findPreference(STYLE);
        if (accentColor == gradientColor) {
            mStyle.setEntries(systementries);
            mStyle.setEntryValues(systemvalues);
        } else {
            mStyle.setEntries(defaultgrad);
            mStyle.setEntryValues(defaultgradentries);
        }
        if (!isDateEnabled() || customface1 || customface2) {
            mWeatherStyle.setEnabled(false);
            if (customface1)
                mWeatherStyle.setSummary(R.string.mnml_summary);
            else if (customface2)
                mWeatherStyle.setSummary(R.string.divided_clock_summary);
            else
                mWeatherStyle.setSummary(R.string.date_disabled_summary);
        } else {
            mWeatherStyle.setEnabled(true);
            mWeatherStyle.setSummary(R.string.lock_screen_weather_style_summary);
            updateprefs(mWeatherStyle.isChecked());
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
    public boolean onPreferenceTreeClick(Preference preference) {
        return super.onPreferenceTreeClick(preference);
    }

    public void updateprefs(boolean enabled) {
        if (enabled) {
            mWeather.setEnabled(false);
            mFooterPreferenceMixin.createFooterPreference().setTitle(R.string.pixel_weather_warning);
        } else {
            mWeather.setEnabled(true);
            mFooterPreferenceMixin.createFooterPreference().setTitle(R.string.lockscreen_weather_summary);
        }
    }

    public void  updatePaddingPref(int pos) {
        if (pos == 1) {
            mPadding.setEnabled(false);
        } else {
            mPadding.setEnabled(true);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mWeatherStyle) {
               boolean value = (Boolean) newValue;
               updateprefs(value);
              return true;
        } else if (preference == mPos) {
               int value = Integer.parseInt((String) newValue);
               updatePaddingPref(value);
              return true;
        }
        return false;
    }

    public boolean isDateEnabled() {
       return Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.LOCKSCREEN_DATE, 1) == 1;
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
                    sir.xmlResId = R.xml.lockscreenitems;
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

