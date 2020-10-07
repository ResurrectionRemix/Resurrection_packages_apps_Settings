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
import android.content.Intent;
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
import android.widget.Toast;
import android.os.UserHandle;
import android.provider.Settings;
import android.net.Uri;

import com.android.settings.Utils;
import com.android.settings.rr.Preferences.CustomSeekBarPreference;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.rr.Preferences.SystemSettingColorPickerPreference;

import android.provider.SearchIndexableResource;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

import com.android.settings.rr.utils.RRUtils;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settings.rr.Preferences.*;
import android.provider.Settings;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import com.android.settings.security.OwnerInfoPreferenceController;

@SearchIndexable
public class LockStyleSettings extends DashboardFragment implements
        Preference.OnPreferenceChangeListener, Indexable, 
        OwnerInfoPreferenceController.OwnerInfoCallback {
    private static final String TAG = "LockStyleSettings";
    private static final String DATE_POS = "lock_date_alignment";
    private static final String DATE_PADDING = "lockscreen_date_padding";
    private static final String OWNER_POS = "lock_ownerinfo_alignment";
    private static final String OWNER_PADDING = "lockscreen_item_padding";
    private static final String CLOCK_CAT = "ls_clock";
    private static final String TYPE_CAT = "ls_type";
    private static final String ANALOG_CAT = "ls_analog";
    private static final String OWNER_INFO = "lock_ownerinfo_fonts";
    private static final String OWNER_INFO_SIZE = "lockowner_font_size";

    private SystemSettingListPreference mDatepos;
    private SystemSettingSeekBarPreference mDatePadding;
    private SystemSettingListPreference mOwnerPos;
    private SystemSettingSeekBarPreference mOwnerPadding;
    private OwnerInfoPreferenceController mOwnerInfoPreferenceController;

    private PreferenceCategory mClockCat;
    private PreferenceCategory mTypeCat;
    private PreferenceCategory mAnalogCat;
    private Preference mOwnerInfoFont;
    private Preference mOwnerInfoStyle;
    private Preference mOwnerInfoAlign;
    private Preference mOwnerInfoPadding;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mFooterPreferenceMixin.createFooterPreference().setTitle(R.string.clock_ui_warning);
		ContentResolver resolver = getActivity().getContentResolver();
        int datepos = Settings.System.getInt(resolver,
                Settings.System.LOCK_DATE_ALIGNMENT, 1);

        int ownerpos = Settings.System.getInt(resolver,
                Settings.System.LOCK_OWNERINFO_ALIGNMENT, 1);
        mClockCat = (PreferenceCategory) findPreference(CLOCK_CAT);
        mTypeCat = (PreferenceCategory) findPreference(TYPE_CAT);
        mAnalogCat = (PreferenceCategory) findPreference(ANALOG_CAT);

        mOwnerInfoFont = (Preference) findPreference(OWNER_INFO);
        mOwnerInfoStyle = (Preference) findPreference(OWNER_INFO_SIZE);

        mDatepos = (SystemSettingListPreference) findPreference(DATE_POS);
        mOwnerPadding = (SystemSettingSeekBarPreference) findPreference(OWNER_PADDING);
        mDatePadding = (SystemSettingSeekBarPreference) findPreference(DATE_PADDING);
        mOwnerPos = (SystemSettingListPreference) findPreference(OWNER_POS);
        mDatepos.setOnPreferenceChangeListener(this);
        mOwnerPos.setOnPreferenceChangeListener(this);

        updateDatePref(datepos);
        updateOwnerPref(ownerpos);
        updateVisiblePrefs();
        updateSummaries();
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
    public void onOwnerInfoUpdated() {
        updateSummaries();
    }

    public void updateSummaries() {
        if (mOwnerInfoPreferenceController != null) {
            mOwnerInfoPreferenceController.updateSummary();
            if (!mOwnerInfoPreferenceController.isOwnerInfoEnabled()) {
                mOwnerPos.setEnabled(false);
                mOwnerPadding.setEnabled(false);
                mOwnerInfoFont.setEnabled(false);
                mOwnerInfoStyle.setEnabled(false);
            } else {
                mOwnerPos.setEnabled(true);
                mOwnerPadding.setEnabled(true);
                mOwnerInfoFont.setEnabled(true);
                mOwnerInfoStyle.setEnabled(true);

            }
        }
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        final Lifecycle lifecycle = getSettingsLifecycle();
        mOwnerInfoPreferenceController =
                new OwnerInfoPreferenceController(context, this, lifecycle);
        controllers.add(mOwnerInfoPreferenceController);
        return controllers;
    }

    public void updateDatePref(int pos) {
        if (pos == 1) {
            mDatePadding.setEnabled(false);
        } else {
            mDatePadding.setEnabled(true);
        }
    }


    public void updateOwnerPref(int pos) {
        if (pos == 1) {
            mOwnerPadding.setEnabled(false);
        } else {
            mOwnerPadding.setEnabled(true);
        }
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.rr_lock_ui;
    }


    public void updateVisiblePrefs() {
		ContentResolver resolver = getActivity().getContentResolver();
        String currentClock = Settings.Secure.getString(
                resolver, Settings.Secure.LOCK_SCREEN_CUSTOM_CLOCK_FACE);
        boolean type1 = currentClock == null ? false : (currentClock.contains("TypeClockAltController"));
        boolean type2 = currentClock == null ? false : (currentClock.contains("TypeClockAltAccentController"));
        boolean type3 = currentClock == null ? false : (currentClock.contains("TypeClockController"));
        boolean type4 = currentClock == null ? false : (currentClock.contains("TypeClockAccentController"));

        boolean analog1 = currentClock == null ? false : (currentClock.contains("AnalogClockController"));
        boolean analog2 = currentClock == null ? false : (currentClock.contains("BinaryClockController"));
        boolean analog3 = currentClock == null ? false : (currentClock.contains("BubbleClockController"));
        boolean analog4 = currentClock == null ? false : (currentClock.contains("CustomNumClockController"));
        boolean analog5 = currentClock == null ? false : (currentClock.contains("DotClockController"));
        boolean analog6 = currentClock == null ? false : (currentClock.contains("OPNumbersClockController"));
        boolean analog7 = currentClock == null ? false : (currentClock.contains("OPAnalogClockController"));
        boolean analog8 = currentClock == null ? false : (currentClock.contains("SneekyClockController"));
        boolean analog9 = currentClock == null ? false : (currentClock.contains("SpectrumClockController"));
        boolean analog10 = currentClock == null ? false : (currentClock.contains("SpideyClockController"));
        boolean analog11 = currentClock == null ? false : (currentClock.contains("LineageClockController"));
        boolean analog12 = currentClock == null ? false : (currentClock.contains("OPRomanClockController"));
        boolean analog13 = currentClock == null ? false : (currentClock.contains("OPMinimalClockController"));
        boolean analog14 = currentClock == null ? false : (currentClock.contains("OPMinimalismClockController"));

        boolean clock1 = currentClock == null ? false : (currentClock.contains("DefaultClockController"));
        boolean clock2 = currentClock == null ? false : (currentClock.contains("SamsungClockController"));
        boolean clock3 = currentClock == null ? false : (currentClock.contains("SamsungAccentClockController"));
        boolean clock4 = currentClock == null ? false : (currentClock.contains("SamsungHighlightClockController"));
        boolean clock5 = currentClock == null ? false : (currentClock.contains("MNMLBoxClockController"));
        boolean clock6 = currentClock == null ? false : (currentClock.contains("MNMLMinimalClockController"));
        boolean clock7 = currentClock == null ? false : (currentClock.contains("DividedLinesClockController"));
        boolean clock8 = currentClock == null ? false : (currentClock.contains("SfunyClockController"));
        if (currentClock == null) {
            mClockCat.setVisible(true);
            mTypeCat.setVisible(false);
            mAnalogCat.setVisible(false);
            return;
        }

        if (type1 || type2 || type3 || type4) {
            mClockCat.setVisible(false);
            mTypeCat.setVisible(true);
            mAnalogCat.setVisible(false);
        } else if (clock1 || clock2 || clock3 || clock4 
            || clock5 || clock6 || clock7 || clock8) {
            mClockCat.setVisible(true);
            mTypeCat.setVisible(false);
            mAnalogCat.setVisible(false);
        } else if (analog1 || analog2 || analog3 || analog4
            || analog5 || analog6 || analog7 || analog8
            || analog9 || analog10 || analog11 || analog12 
            || analog13 || analog14){
            mClockCat.setVisible(false);
            mTypeCat.setVisible(false);
            mAnalogCat.setVisible(true);
        }
    }
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mDatepos) {
             int value = Integer.parseInt((String) newValue);
             updateDatePref(value);
             return true;
        } else if (preference == mOwnerPos) {
             int value = Integer.parseInt((String) newValue);
             updateOwnerPref(value);
             return true;
        } 
       return true;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
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
                    sir.xmlResId = R.xml.rr_lock_ui;
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
