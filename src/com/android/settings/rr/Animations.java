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
import android.content.Context;
import android.content.ContentResolver;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceCategory;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.provider.Settings;
import android.view.View;
import android.widget.Toast;
import com.android.settings.Utils;
import java.util.HashSet;

import java.util.Arrays;
import android.os.Vibrator;
import java.util.HashSet;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.Handler;
import android.database.ContentObserver;
import android.content.Context;
import android.content.Intent;
import android.provider.SearchIndexableResource;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

import com.android.settings.rr.utils.RRUtils;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;


import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
@SearchIndexable
public class Animations extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {

    private static final String SCREEN_OFF_ANIMATION = "screen_off_animation";
    private static final String KEY_TOAST_ANIMATION = "toast_animation";
    private static final String KEY_LISTVIEW_ANIMATION = "listview_animation";
    private static final String KEY_LISTVIEW_INTERPOLATOR = "listview_interpolator";
    private static final String KEY_SS_TABS_EFFECT = "tabs_effect";
    private static final String SCROLLINGCACHE_PREF = "pref_scrollingcache";
    private static final String SCROLLINGCACHE_PERSIST_PROP = "persist.sys.scrollingcache";
    private static final String SCROLLINGCACHE_DEFAULT = "1";
    private static final String KEY_REFRESH_RATE = "refresh_rate_setting";

    private ListPreference mRefreshRate;
    private Context mContext;
    private ListPreference mScreenOffAnimation;
    private ListPreference mToastAnimation;
    private ListPreference mListViewAnimation;
    private ListPreference mListViewInterpolator;
    ListPreference mListViewTabsEffect;
    private ListPreference mScrollingCachePref;
    private int peakRate;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rr_animations);

        mContext = getActivity();
        peakRate = getResources().getInteger(com.android.internal.R.integer.config_defaultPeakRefreshRate);
        ContentResolver resolver = getActivity().getContentResolver();
        PreferenceScreen prefs = getPreferenceScreen();
        mListViewAnimation = (ListPreference) prefs.findPreference(KEY_LISTVIEW_ANIMATION);
        int listviewanimation = Settings.Global.getInt(getContentResolver(),
                Settings.Global.LISTVIEW_ANIMATION, 0);
        mListViewAnimation.setValue(String.valueOf(listviewanimation));
        mListViewAnimation.setSummary(mListViewAnimation.getEntry());
        mListViewAnimation.setOnPreferenceChangeListener(this);
        mListViewInterpolator = (ListPreference) prefs.findPreference(KEY_LISTVIEW_INTERPOLATOR);
        int listviewinterpolator = Settings.Global.getInt(getContentResolver(),
                Settings.Global.LISTVIEW_INTERPOLATOR, 0);
        mListViewInterpolator.setValue(String.valueOf(listviewinterpolator));
        mListViewInterpolator.setSummary(mListViewInterpolator.getEntry());
        mListViewInterpolator.setOnPreferenceChangeListener(this);
        mListViewInterpolator.setEnabled(listviewanimation > 0);

        mScreenOffAnimation = (ListPreference) findPreference(SCREEN_OFF_ANIMATION);
        int screenOffStyle = Settings.System.getInt(resolver,
                Settings.System.SCREEN_OFF_ANIMATION, 0);
        mScreenOffAnimation.setValue(String.valueOf(screenOffStyle));
        mScreenOffAnimation.setSummary(mScreenOffAnimation.getEntry());
        mScreenOffAnimation.setOnPreferenceChangeListener(this);
	
        mScrollingCachePref = (ListPreference) findPreference(SCROLLINGCACHE_PREF);
        mScrollingCachePref.setValue(SystemProperties.get(SCROLLINGCACHE_PERSIST_PROP,
                SystemProperties.get(SCROLLINGCACHE_PERSIST_PROP, SCROLLINGCACHE_DEFAULT)));
        mScrollingCachePref.setSummary(mScrollingCachePref.getEntry());
        mScrollingCachePref.setOnPreferenceChangeListener(this);

        mRefreshRate = (ListPreference) findPreference(KEY_REFRESH_RATE);
        if (isAvailable()) {
           int defVarRateSetting = getContext().getResources().getInteger(
                 com.android.internal.R.integer.config_defaultVariableRefreshRateSetting);
            int refreshRate = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.REFRESH_RATE_SETTING, defVarRateSetting);
            mRefreshRate.setValue(String.valueOf(refreshRate));
            mRefreshRate.setOnPreferenceChangeListener(this);
            updateRefreshRate(refreshRate);
        } else {
          removePreference("refresh_rate_setting");
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

    public boolean isAvailable() {
        return mContext.getResources().getBoolean(com.android.internal.R.bool.config_hasVariableRefreshRate);
    }


    public boolean onPreferenceChange(Preference preference, Object newValue) {
	ContentResolver resolver = getActivity().getContentResolver();
         if (preference == mListViewAnimation) {
            int value = Integer.parseInt((String) newValue);
            int index = mListViewAnimation.findIndexOfValue((String) newValue);
            Settings.Global.putInt(getContentResolver(),
                    Settings.Global.LISTVIEW_ANIMATION, value);
            mListViewAnimation.setSummary(mListViewAnimation.getEntries()[index]);
            mListViewInterpolator.setEnabled(value > 0);
            return true;
        } else if (preference == mListViewInterpolator) {
            int value = Integer.parseInt((String) newValue);
            int index = mListViewInterpolator.findIndexOfValue((String) newValue);
            Settings.Global.putInt(getContentResolver(),
                    Settings.Global.LISTVIEW_INTERPOLATOR, value);
            mListViewInterpolator.setSummary(mListViewInterpolator.getEntries()[index]);
            return true;
        } else  if (preference == mScreenOffAnimation) {
            String value = (String) newValue;
            Settings.System.putInt(resolver,
                    Settings.System.SCREEN_OFF_ANIMATION, Integer.valueOf(value));
            int valueIndex = mScreenOffAnimation.findIndexOfValue(value);
            mScreenOffAnimation.setSummary(mScreenOffAnimation.getEntries()[valueIndex]);
            return true;
        } else if (preference == mRefreshRate) {
            int refreshRate = Integer.valueOf((String) newValue);
            Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.REFRESH_RATE_SETTING, refreshRate);
            updateRefreshRate(refreshRate);
            return true;
        } else if (preference == mScrollingCachePref) {
            if (newValue != null) {
                String value = (String) newValue;
                SystemProperties.set(SCROLLINGCACHE_PERSIST_PROP, (String) newValue);
                int valueIndex = mScrollingCachePref.findIndexOfValue(value);
                mScrollingCachePref.setSummary(mScrollingCachePref.getEntries()[valueIndex]);
                return true;
            }
        }
        return false;
    }



    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }


    public void updateRefreshRate(int refreshRate) {
        switch (refreshRate) {
            case 0:
            default:
                Settings.System.putInt(mContext.getContentResolver(),
                        Settings.System.PEAK_REFRESH_RATE, peakRate);
                Settings.System.putInt(mContext.getContentResolver(),
                        Settings.System.MIN_REFRESH_RATE, 60);
                break;
            case 1:
                Settings.System.putInt(mContext.getContentResolver(),
                        Settings.System.PEAK_REFRESH_RATE, 60);
                Settings.System.putInt(mContext.getContentResolver(),
                        Settings.System.MIN_REFRESH_RATE, 60);
                break;
            case 2:
                Settings.System.putInt(mContext.getContentResolver(),
                        Settings.System.PEAK_REFRESH_RATE, 90);
                Settings.System.putInt(mContext.getContentResolver(),
                        Settings.System.MIN_REFRESH_RATE, 90);
            case 3:
                Settings.System.putInt(mContext.getContentResolver(),
                        Settings.System.PEAK_REFRESH_RATE, 120);
                Settings.System.putInt(mContext.getContentResolver(),
                        Settings.System.MIN_REFRESH_RATE, 120);
                break;
        }
        updateRefreshRateSummary(refreshRate);
    }

    public void updateRefreshRateSummary(int refreshRate) {
        if (refreshRate == 1) {
            mRefreshRate.setSummary(R.string.refresh_rate_summary_60);
        } else if (refreshRate == 2) {
            mRefreshRate.setSummary(R.string.refresh_rate_summary_90);
        } else if (refreshRate == 3) {
            mRefreshRate.setSummary(R.string.refresh_rate_summary_120);
        } else {
            if (peakRate > 90) {
                mRefreshRate.setSummary(R.string.refresh_rate_summary_auto_120);
            } else {
                mRefreshRate.setSummary(R.string.refresh_rate_summary_auto);
            }
        }
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
                    sir.xmlResId = R.xml.rr_animations;
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
