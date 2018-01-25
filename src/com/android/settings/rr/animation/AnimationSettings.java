package com.android.settings.rr.animation;

import android.app.Activity;
import android.content.Context;
import android.content.ContentResolver;
import android.os.Bundle;
import android.os.SystemProperties;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.provider.Settings;


import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

public class AnimationSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String SCROLLINGCACHE_PREF = "pref_scrollingcache";
    private static final String SCROLLINGCACHE_PERSIST_PROP = "persist.sys.scrollingcache";
    private static final String SCROLLINGCACHE_DEFAULT = "1";
    private static final String KEY_SS_TABS_EFFECT = "tabs_effect";

	  protected Context mContext;
      private ListPreference mScrollingCachePref;
      ListPreference mListViewTabsEffect;


      protected ContentResolver mContentRes;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rr_animation_settings);

        mScrollingCachePref = (ListPreference) findPreference(SCROLLINGCACHE_PREF);
        mScrollingCachePref.setValue(SystemProperties.get(SCROLLINGCACHE_PERSIST_PROP,
                SystemProperties.get(SCROLLINGCACHE_PERSIST_PROP, SCROLLINGCACHE_DEFAULT)));
        mScrollingCachePref.setOnPreferenceChangeListener(this);

        mListViewTabsEffect = (ListPreference) findPreference(KEY_SS_TABS_EFFECT);
        int tabsEffect = Settings.System.getInt(getContentResolver(),
                Settings.System.RR_SETTINGS_TABS_EFFECT, 0);
        mListViewTabsEffect.setValue(String.valueOf(tabsEffect));
        mListViewTabsEffect.setSummary(mListViewTabsEffect.getEntry());
        mListViewTabsEffect.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mScrollingCachePref) {
            if (newValue != null) {
                SystemProperties.set(SCROLLINGCACHE_PERSIST_PROP, (String) newValue);
                return true;
            }
        } else if (preference == mListViewTabsEffect) {
            int value = Integer.valueOf((String) newValue);
            int index = mListViewTabsEffect.findIndexOfValue((String) newValue);
            Settings.System.putInt(getContentResolver(),
                     Settings.System.RR_SETTINGS_TABS_EFFECT, value);
            mListViewTabsEffect.setSummary(mListViewTabsEffect.getEntries()[index]);
            return true;
         }
        return false;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

}
