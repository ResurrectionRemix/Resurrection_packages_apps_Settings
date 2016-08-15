/*
 * Copyright (C) 2014 TeamEos
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

import java.util.ArrayList;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.utils.du.ActionConstants;
import com.android.internal.utils.du.ActionHandler;
import com.android.internal.utils.du.Config.ButtonConfig;
import com.android.settings.R;
import com.android.settings.rr.ActionPreference;
import com.android.settings.rr.IconPickHelper;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.provider.Settings;
import android.text.TextUtils;

import android.provider.SearchIndexableResource;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import java.util.ArrayList;
import java.util.List;
import com.android.settings.rr.SeekBarPreferenceCham;

public class FlingSettings extends ActionFragment implements
        Preference.OnPreferenceChangeListener, IconPickHelper.OnPickListener  , Indexable {
    private static final String TAG = FlingSettings.class.getSimpleName();
    public static final String FLING_LOGO_URI = "fling_custom_icon_config";

    Context mContext;
    IconPickHelper mIconPickHelper;

    SwitchPreference mShowLogo;
    SwitchPreference mAnimateLogo;
    SwitchPreference mShowRipple;
    SwitchPreference mTrailsEnabled;

    ColorPickerPreference mLogoColor;
    ColorPickerPreference mRippleColor;
    ColorPickerPreference mTrailsColor;
    SeekBarPreferenceCham mTrailsWidth;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.fling_settings);

        ActionBar bar = getActivity().getActionBar();
        if (bar != null) {
            bar.setTitle(R.string.fling_interface);
        }

        mContext = (Context) getActivity();
        mIconPickHelper = new IconPickHelper(getActivity(), this);

        mShowLogo = (SwitchPreference) findPreference("eos_fling_show_logo");
        mShowLogo.setChecked(Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.FLING_LOGO_VISIBLE, 1) == 1);
        mShowLogo.setOnPreferenceChangeListener(this);

        mAnimateLogo = (SwitchPreference) findPreference("eos_fling_animate_logo");
        mAnimateLogo.setChecked(Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.FLING_LOGO_ANIMATES, 1) == 1);
        mAnimateLogo.setOnPreferenceChangeListener(this);

        int logoColor = Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.FLING_LOGO_COLOR, Color.WHITE, UserHandle.USER_CURRENT);
        mLogoColor = (ColorPickerPreference) findPreference("eos_fling_logo_color");
        mLogoColor.setNewPreviewColor(logoColor);
        mLogoColor.setOnPreferenceChangeListener(this);

        mShowRipple = (SwitchPreference) findPreference("eos_fling_show_ripple");
        mShowRipple.setChecked(Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.FLING_RIPPLE_ENABLED, 1) == 1);
        mShowRipple.setOnPreferenceChangeListener(this);

        int rippleColor = Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.FLING_RIPPLE_COLOR, Color.WHITE, UserHandle.USER_CURRENT);
        mRippleColor = (ColorPickerPreference) findPreference("eos_fling_ripple_color");
        mRippleColor.setNewPreviewColor(rippleColor);
        mRippleColor.setOnPreferenceChangeListener(this);

        mTrailsEnabled = (SwitchPreference) findPreference("eos_fling_trails_enable");
        mTrailsEnabled.setChecked(Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.FLING_TRAILS_ENABLED, 1) == 1);
        mTrailsEnabled.setOnPreferenceChangeListener(this);

        int trailsColor = Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.FLING_TRAILS_COLOR, Color.WHITE, UserHandle.USER_CURRENT);
        mTrailsColor = (ColorPickerPreference) findPreference("eos_fling_trails_color");
        mTrailsColor.setNewPreviewColor(trailsColor);
        mTrailsColor.setOnPreferenceChangeListener(this);

        mTrailsWidth = (SeekBarPreferenceCham) findPreference("du_fling_trails_width");
        int width = Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.FLING_TRAILS_WIDTH, 15, UserHandle.USER_CURRENT);
        mTrailsWidth.setValue(width / 1);
        mTrailsWidth.setOnPreferenceChangeListener(this);

        onPreferenceScreenLoaded(ActionConstants.getDefaults(ActionConstants.FLING));
    }

    @Override
    public void iconPicked(String iconType, String iconPackage, String iconName) {
        if (TextUtils.isEmpty(iconType)
                || TextUtils.isEmpty(iconPackage)
                || TextUtils.isEmpty(iconName)) {
            return;
        }
        ButtonConfig logoConfig = ButtonConfig.getButton(mContext, FLING_LOGO_URI, true);
        logoConfig.setCustomIconUri(iconType, iconPackage, iconName);
        ButtonConfig.setButton(mContext, logoConfig, FLING_LOGO_URI, true);
    }

    @Override
    public void imagePicked(Uri uri) {
        if (uri != null) {
            ButtonConfig logoConfig = ButtonConfig.getButton(mContext, FLING_LOGO_URI, true);
            logoConfig.setCustomImageUri(uri);
            ButtonConfig.setButton(mContext, logoConfig, FLING_LOGO_URI, true);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mIconPickHelper.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == findPreference("fling_custom_logo_pick")) {
            mIconPickHelper.pickIcon(getId(), IconPickHelper.REQUEST_PICK_ICON_PACK);
            return true;
        } else if (preference == findPreference("fling_custom_logo_reset")) {
            ButtonConfig logoConfig = ButtonConfig.getButton(mContext, FLING_LOGO_URI, true);
            logoConfig.clearCustomIconIconUri();
            ButtonConfig.setButton(mContext, logoConfig, FLING_LOGO_URI, true);
            return true;
        } else if (preference == findPreference("fling_custom_logo_gallery_pick")) {
            mIconPickHelper.pickIcon(getId(), IconPickHelper.REQUEST_PICK_ICON_GALLERY);
            return true;
        }
 
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.equals(mShowLogo)) {
            boolean enabled = ((Boolean) newValue).booleanValue();
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.FLING_LOGO_VISIBLE, enabled ? 1 : 0);
            return true;
        } else if (preference.equals(mAnimateLogo)) {
            boolean enabled = ((Boolean) newValue).booleanValue();
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.FLING_LOGO_ANIMATES, enabled ? 1 : 0);
            return true;
        } else if (preference.equals(mLogoColor)) {
            int color = ((Integer) newValue).intValue();
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.FLING_LOGO_COLOR, color);
            return true;
        } else if (preference.equals(mShowRipple)) {
            boolean enabled = ((Boolean) newValue).booleanValue();
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.FLING_RIPPLE_ENABLED, enabled ? 1 : 0);
            return true;
        } else if (preference.equals(mRippleColor)) {
            int color = ((Integer) newValue).intValue();
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.FLING_RIPPLE_COLOR, color);
            return true;
        } else if (preference.equals(mTrailsEnabled)) {
            boolean enabled = ((Boolean) newValue).booleanValue();
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.FLING_TRAILS_ENABLED, enabled ? 1 : 0);
            return true;
        } else if (preference.equals(mTrailsColor)) {
            int color = ((Integer) newValue).intValue();
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.FLING_TRAILS_COLOR, color);
            return true;
        } else if (preference == mTrailsWidth) {
            int val = (Integer) newValue;
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.FLING_TRAILS_WIDTH, val * 1, UserHandle.USER_CURRENT);
            return true;
        }
        return false;
    }

    protected boolean usesExtendedActionsList() {
        return true;
    }

    protected void onActionPolicyEnforced(ArrayList<ActionPreference> prefs) {
        enforceAction(prefs, ActionHandler.SYSTEMUI_TASK_BACK);
        enforceAction(prefs, ActionHandler.SYSTEMUI_TASK_HOME);
    }

    /*
     * Iterate the list: if only one instance, disable it otherwise, enable
     */
    private void enforceAction(ArrayList<ActionPreference> prefs, String action) {
        ArrayList<ActionPreference> actionPrefs = new ArrayList<ActionPreference>();
        for (ActionPreference pref : prefs) {
            if (pref.getActionConfig().getAction().equals(action)) {
                actionPrefs.add(pref);
            }
        }
        boolean moreThanOne = actionPrefs.size() > 1;
        for (ActionPreference pref : actionPrefs) {
            pref.setEnabled(moreThanOne);
        }
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DONT_TRACK_ME_BRO;
    }
    
         public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                                                                             boolean enabled) {
                     ArrayList<SearchIndexableResource> result =
                             new ArrayList<SearchIndexableResource>();
 
                     SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.fling_settings;
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
