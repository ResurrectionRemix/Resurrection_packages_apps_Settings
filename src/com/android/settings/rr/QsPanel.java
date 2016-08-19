/*
* Copyright (C) 2016 RR
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
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
import android.preference.ListPreference;
import android.preference.MultiSelectListPreferenceFix;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SwitchPreference;
import android.provider.Settings;
import com.android.settings.util.Helpers;
import org.cyanogenmod.internal.util.CmLockPatternUtils;
import com.android.settings.Utils;
import android.provider.SearchIndexableResource;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import com.android.internal.logging.MetricsLogger;
import cyanogenmod.providers.CMSettings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

import java.util.Set;

import android.text.TextUtils;
import android.util.Log;
import android.view.View;

public class QsPanel extends SettingsPreferenceFragment  implements Preference.OnPreferenceChangeListener ,Indexable {

 private static final String TAG = QsPanel.class.getSimpleName();
 private static final String PREF_BLOCK_ON_SECURE_KEYGUARD = "block_on_secure_keyguard";
 private static final String STATUS_BAR_QUICK_QS_PULLDOWN = "qs_quick_pulldown";
 private static final String PREF_SMART_PULLDOWN = "smart_pulldown";
 private static final String PREF_NUM_COLUMNS = "sysui_qs_num_columns";
 private static final String PREF_TILE_ANIM_STYLE = "qs_tile_animation_style";
 private static final String PREF_TILE_ANIM_DURATION = "qs_tile_animation_duration";
 private static final String PREF_TILE_ANIM_INTERPOLATOR = "qs_tile_animation_interpolator";
 private static final String QS_TASK_ANIMATION = "qs_task_animation";
 private static final String PREF_THEMES_TILE = "themes_tile_components";
 private MultiSelectListPreferenceFix mThemesTile;


    private SwitchPreference mBlockOnSecureKeyguard;
    private ListPreference mQuickPulldown;
    private ListPreference mSmartPulldown;
    private ListPreference mNumColumns;	
    private ListPreference mTileAnimationStyle;
    private ListPreference mTileAnimationDuration;
    private ListPreference mTileAnimationInterpolator;
    private ListPreference mAnimation;
    	
    private static final int MY_USER_ID = UserHandle.myUserId();
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.rr_qs_panel);
        PreferenceScreen prefSet = getPreferenceScreen();
        final ContentResolver resolver = getActivity().getContentResolver();
	final CmLockPatternUtils lockPatternUtils = new CmLockPatternUtils(getActivity());

        Resources res = getResources();
	mQuickPulldown = (ListPreference) findPreference(STATUS_BAR_QUICK_QS_PULLDOWN);
        int quickPulldown = CMSettings.System.getInt(resolver,
                CMSettings.System.STATUS_BAR_QUICK_QS_PULLDOWN, 1);
        mQuickPulldown.setValue(String.valueOf(quickPulldown));
        if (quickPulldown == 0) {
            // quick pulldown deactivated
            mQuickPulldown.setSummary(res.getString(R.string.status_bar_quick_qs_pulldown_off));
        } else {
            String direction = res.getString(quickPulldown == 2
                    ? R.string.status_bar_quick_qs_pulldown_left
                    : R.string.status_bar_quick_qs_pulldown_right);
            mQuickPulldown.setSummary(
                    res.getString(R.string.status_bar_quick_qs_pulldown_summary, direction));
        }
        mQuickPulldown.setOnPreferenceChangeListener(this);

        // Block QS on secure LockScreen
        mBlockOnSecureKeyguard = (SwitchPreference) findPreference(PREF_BLOCK_ON_SECURE_KEYGUARD);
        if (lockPatternUtils.isSecure(MY_USER_ID)) {
                mBlockOnSecureKeyguard.setChecked(Settings.Secure.getIntForUser(resolver,
                        Settings.Secure.STATUS_BAR_LOCKED_ON_SECURE_KEYGUARD, 1, UserHandle.USER_CURRENT) == 1);
                mBlockOnSecureKeyguard.setOnPreferenceChangeListener(this);
        } else if (mBlockOnSecureKeyguard != null) {
                prefSet.removePreference(mBlockOnSecureKeyguard);
        }

         mSmartPulldown = (ListPreference) findPreference(PREF_SMART_PULLDOWN);
         mSmartPulldown.setOnPreferenceChangeListener(this);
         int smartPulldown = Settings.System.getInt(resolver,
                 Settings.System.QS_SMART_PULLDOWN, 0);
         mSmartPulldown.setValue(String.valueOf(smartPulldown));
         updateSmartPulldownSummary(smartPulldown);
        
	 // Number of QS Columns 3,4,5
         mNumColumns = (ListPreference) findPreference(PREF_NUM_COLUMNS);
         int numColumns = Settings.System.getIntForUser(resolver,
                    Settings.System.QS_NUM_TILE_COLUMNS, getDefaultNumColums(),
                    UserHandle.USER_CURRENT);
         mNumColumns.setValue(String.valueOf(numColumns));
         updateNumColumnsSummary(numColumns);
         mNumColumns.setOnPreferenceChangeListener(this);
            
         mTileAnimationStyle = (ListPreference) findPreference(PREF_TILE_ANIM_STYLE);
         int tileAnimationStyle = Settings.System.getIntForUser(getContentResolver(),
                 Settings.System.ANIM_TILE_STYLE, 0,
                 UserHandle.USER_CURRENT);
         mTileAnimationStyle.setValue(String.valueOf(tileAnimationStyle));
         updateTileAnimationStyleSummary(tileAnimationStyle);
         updateAnimTileStyle(tileAnimationStyle);
         mTileAnimationStyle.setOnPreferenceChangeListener(this);
 
         mTileAnimationDuration = (ListPreference) findPreference(PREF_TILE_ANIM_DURATION);
         int tileAnimationDuration = Settings.System.getIntForUser(getContentResolver(),
                 Settings.System.ANIM_TILE_DURATION, 1500,
                 UserHandle.USER_CURRENT);
         mTileAnimationDuration.setValue(String.valueOf(tileAnimationDuration));
         updateTileAnimationDurationSummary(tileAnimationDuration);
         mTileAnimationDuration.setOnPreferenceChangeListener(this);
         
         mTileAnimationInterpolator = (ListPreference) findPreference(PREF_TILE_ANIM_INTERPOLATOR);
        int tileAnimationInterpolator = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.ANIM_TILE_INTERPOLATOR, 0,
                UserHandle.USER_CURRENT);
        mTileAnimationInterpolator.setValue(String.valueOf(tileAnimationInterpolator));
        updateTileAnimationInterpolatorSummary(tileAnimationInterpolator);
        mTileAnimationInterpolator.setOnPreferenceChangeListener(this);

        mAnimation = (ListPreference) findPreference(QS_TASK_ANIMATION);
        mAnimation.setValue(String.valueOf(Settings.System.getInt(
                getContentResolver(), Settings.System.QS_TASK_ANIMATION, 8)));
        mAnimation.setSummary(mAnimation.getEntry());
        mAnimation.setOnPreferenceChangeListener(this);

	mThemesTile = (MultiSelectListPreferenceFix) findPreference(PREF_THEMES_TILE);
        mThemesTile.setValues(getThemesTileValues());
        mThemesTile.setOnPreferenceChangeListener(this);

    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.NOTIFICATION_DRAWER_SETTINGS;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
	ContentResolver resolver = getActivity().getContentResolver();
	Resources res = getResources();
	if (preference == mBlockOnSecureKeyguard) {
                Settings.Secure.putInt(resolver,
                        Settings.Secure.STATUS_BAR_LOCKED_ON_SECURE_KEYGUARD,
                        (Boolean) newValue ? 1 : 0);
                return true;
	} else if (preference == mQuickPulldown) {
            int quickPulldown = Integer.valueOf((String) newValue);
            CMSettings.System.putInt(resolver, CMSettings.System.STATUS_BAR_QUICK_QS_PULLDOWN,
                    quickPulldown);
            if (quickPulldown == 0) {
                // quick pulldown deactivated
                mQuickPulldown.setSummary(res.getString(R.string.status_bar_quick_qs_pulldown_off));
            } else {
                String direction = res.getString(quickPulldown == 2
                        ? R.string.status_bar_quick_qs_pulldown_left
                        : R.string.status_bar_quick_qs_pulldown_right);
                mQuickPulldown.setSummary(
                        res.getString(R.string.status_bar_quick_qs_pulldown_summary, direction));
            }
            return true;
	} else if (preference == mSmartPulldown) {
            int smartPulldown = Integer.valueOf((String) newValue);
            Settings.System.putInt(resolver, Settings.System.QS_SMART_PULLDOWN, smartPulldown);
            updateSmartPulldownSummary(smartPulldown);
            return true;
	} else if (preference == mNumColumns) {
                int numColumns = Integer.valueOf((String) newValue);
                Settings.System.putIntForUser(resolver, Settings.System.QS_NUM_TILE_COLUMNS,
                        numColumns, UserHandle.USER_CURRENT);
                updateNumColumnsSummary(numColumns);
                return true;
         } else if (preference == mTileAnimationStyle) {
             int tileAnimationStyle = Integer.valueOf((String) newValue);
             Settings.System.putIntForUser(getContentResolver(), Settings.System.ANIM_TILE_STYLE,
                     tileAnimationStyle, UserHandle.USER_CURRENT);
             updateTileAnimationStyleSummary(tileAnimationStyle);
             updateAnimTileStyle(tileAnimationStyle);
             return true;
         } else if (preference == mTileAnimationDuration) {
             int tileAnimationDuration = Integer.valueOf((String) newValue);
             Settings.System.putIntForUser(getContentResolver(), Settings.System.ANIM_TILE_DURATION,
                     tileAnimationDuration, UserHandle.USER_CURRENT);
             updateTileAnimationDurationSummary(tileAnimationDuration);
             return true;
         } else if (preference == mTileAnimationInterpolator) {
            int tileAnimationInterpolator = Integer.valueOf((String) newValue);
            Settings.System.putIntForUser(getContentResolver(), Settings.System.ANIM_TILE_INTERPOLATOR,
                    tileAnimationInterpolator, UserHandle.USER_CURRENT);
            updateTileAnimationInterpolatorSummary(tileAnimationInterpolator);
            return true;
	  } else if (preference == mAnimation) {
            Settings.System.putInt(getContentResolver(), Settings.System.QS_TASK_ANIMATION,
                    Integer.valueOf((String) newValue));
            mAnimation.setValue(String.valueOf(newValue));
            mAnimation.setSummary(mAnimation.getEntry());
            return true;
	  }  else if (preference == mThemesTile) {
            Set<String> vals = (Set<String>) newValue;
//            Log.e(TAG, "mThemesTileChanged " + vals.toString());
            setThemesTileValues(vals);
            return true;
        }
         return false;
	}

    private void setThemesTileValues(Set<String> vals) {
        if (vals.isEmpty()) {
            // if user unchecks everything, reset to default
            vals.addAll(Arrays.asList(getResources().getStringArray(
                    R.array.themes_tile_default_values)));
//            Log.e(TAG, "setThemesTileValues called but is empty list = " + vals.toString());
            mThemesTile.setValues(vals);
        }
//        Log.e(TAG, "setThemesTileValues called = " + vals.toString());
        StringBuilder b = new StringBuilder();
        for (String val : vals) {
            b.append(val);
            b.append("|");
        }
        String newVal = b.toString();
        if (newVal.endsWith("|")) {
            newVal = removeLastChar(newVal);
        }
//        Log.e(TAG, "Themes tile components writing to provider = " + newVal);
        Settings.Secure.putStringForUser(getContentResolver(),
                Settings.Secure.THEMES_TILE_COMPONENTS,
                newVal, UserHandle.USER_CURRENT);
    }

    private Set<String> getThemesTileValues() {
        Set<String> vals = new HashSet<>();
        String components = Settings.Secure.getStringForUser(getContentResolver(),
                Settings.Secure.THEMES_TILE_COMPONENTS,
                UserHandle.USER_CURRENT);
        if (components != null) {
//            Log.e(TAG, "Themes tile components from provider raw = " + components);
        }
        if (TextUtils.isEmpty(components)) {
            vals.addAll(Arrays.asList(getResources().getStringArray(
                    R.array.themes_tile_default_values)));
//            Log.e(TAG, "Themes tile components from provider is empty. get defaults = " + vals.toString());
        } else {
            vals.addAll(Arrays.asList(components.split("\\|")));
//            Log.e(TAG, "Themes tile components from provider = " + vals.toString());
        }
        return vals;
    }

    static String removeLastChar(String s) {
        if (s == null || s.length() == 0) {
            return s;
        }
        return s.substring(0, s.length() - 1);
    }

     private void updateSmartPulldownSummary(int value) {
        Resources res = getResources();

        if (value == 0) {
            // Smart pulldown deactivated
            mSmartPulldown.setSummary(res.getString(R.string.smart_pulldown_off));
        } else {
            String type = null;
            switch (value) {
                case 1:
                    type = res.getString(R.string.smart_pulldown_dismissable);
                    break;
                case 2:
                    type = res.getString(R.string.smart_pulldown_persistent);
                    break;
                default:
                    type = res.getString(R.string.smart_pulldown_all);
                    break;
            }
            // Remove title capitalized formatting
            type = type.toLowerCase();
            mSmartPulldown.setSummary(res.getString(R.string.smart_pulldown_summary, type));
        }
    }
    
      private void updateNumColumnsSummary(int numColumns) {
            String prefix = (String) mNumColumns.getEntries()[mNumColumns.findIndexOfValue(String
                    .valueOf(numColumns))];
            mNumColumns.setSummary(getResources().getString(R.string.qs_num_columns_showing, prefix));
        }
        
            private void updateTileAnimationStyleSummary(int tileAnimationStyle) {
         String prefix = (String) mTileAnimationStyle.getEntries()[mTileAnimationStyle.findIndexOfValue(String
                 .valueOf(tileAnimationStyle))];
         mTileAnimationStyle.setSummary(getResources().getString(R.string.qs_set_animation_style, prefix));
     }
 
     private void updateTileAnimationDurationSummary(int tileAnimationDuration) {
         String prefix = (String) mTileAnimationDuration.getEntries()[mTileAnimationDuration.findIndexOfValue(String
                 .valueOf(tileAnimationDuration))];
         mTileAnimationDuration.setSummary(getResources().getString(R.string.qs_set_animation_duration, prefix));
     }
     
     private void updateTileAnimationInterpolatorSummary(int tileAnimationInterpolator) {
        String prefix = (String) mTileAnimationInterpolator.getEntries()[mTileAnimationInterpolator.findIndexOfValue(String
                .valueOf(tileAnimationInterpolator))];
        mTileAnimationInterpolator.setSummary(getResources().getString(R.string.qs_set_animation_interpolator, prefix));
    }
 
     private void updateAnimTileStyle(int tileAnimationStyle) {
         if (mTileAnimationDuration != null) {
             if (tileAnimationStyle == 0) {
                 mTileAnimationDuration.setSelectable(false);
                 mTileAnimationInterpolator.setSelectable(false);
             } else {
                 mTileAnimationDuration.setSelectable(true);
                 mTileAnimationInterpolator.setSelectable(true);
             }
         }
     }

     private int getDefaultNumColums() {
            try {
                Resources res = getActivity().getPackageManager()
                        .getResourcesForApplication("com.android.systemui");
                int val = res.getInteger(res.getIdentifier("quick_settings_num_columns", "integer",
                        "com.android.systemui")); // better not be larger than 5, that's as high as the
                                                  // list goes atm
                return Math.max(1, val);
            } catch (Exception e) {
                return 3;
            }
        }
        
            
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                                                                             boolean enabled) {
                     ArrayList<SearchIndexableResource> result =
                             new ArrayList<SearchIndexableResource>();
 
                     SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.rr_qs_panel;
                     result.add(sir);
 
                     return result;
                 }
 
                 @Override
                 public List<String> getNonIndexableKeys(Context context) {
                     final List<String> keys = new ArrayList<String>();
                     return keys;
                 }
         };
}

