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

package com.android.settings.rr.fragments;

import java.util.ArrayList;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

import com.android.settings.rr.Preferences.CustomSeekBarPreference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.utils.du.ActionConstants;
import com.android.internal.utils.du.ActionHandler;
import com.android.internal.utils.du.DUActionUtils;
import com.android.internal.utils.du.Config.ButtonConfig;
import com.android.settings.R;
import com.android.settings.rr.IconPickHelper;
import com.android.settings.rr.preferences.ActionPreference;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.PreferenceScreen;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.provider.Settings;
import android.text.TextUtils;

public class FlingSettings extends ActionFragment implements
        Preference.OnPreferenceChangeListener, IconPickHelper.OnPickListener {
    private static final String TAG = FlingSettings.class.getSimpleName();
    public static final String FLING_LOGO_URI = "fling_custom_icon_config";

    Context mContext;
    IconPickHelper mIconPickHelper;

    SwitchPreference mShowLogo;
    SwitchPreference mAnimateLogo;
    SwitchPreference mShowRipple;
    SwitchPreference mTrailsEnabled;
    SwitchPreference mKbCursors;

    CustomSeekBarPreference mTrailsWidth;
    CustomSeekBarPreference mLongPressTimeout;

    CustomSeekBarPreference mLogoOpacity;

    CustomSeekBarPreference mSwipePortRight;
    CustomSeekBarPreference mSwipePortLeft;
    CustomSeekBarPreference mSwipeLandRight;
    CustomSeekBarPreference mSwipeLandLeft;
    CustomSeekBarPreference mSwipeVertUp;
    CustomSeekBarPreference mSwipeVertDown;

    ColorPickerPreference mRippleColor;
    ColorPickerPreference mTrailsColor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.fling_settings);

        mFooterPreferenceMixin.createFooterPreference().setTitle(R.string.fling_back_home_policy);

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

        mTrailsWidth = (CustomSeekBarPreference) findPreference("du_fling_trails_width");
        int width = Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.FLING_TRAILS_WIDTH, 15, UserHandle.USER_CURRENT);
        mTrailsWidth.setValue(width);
        mTrailsWidth.setOnPreferenceChangeListener(this);

        // NOTE: we display to the user actual timeouts starting from touch event
        // but framework wants the value less tap timeout, which is 100ms
        // so we always write 100ms less but display 100ms more
        mLongPressTimeout = (CustomSeekBarPreference) findPreference("du_fling_longpress_pref");
        int val = Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.FLING_LONGPRESS_TIMEOUT, 250, UserHandle.USER_CURRENT);
        val += 100;
        mLongPressTimeout.setValue(val);
        mLongPressTimeout.setOnPreferenceChangeListener(this);

        final boolean isTablet = !DUActionUtils.navigationBarCanMove();

        mSwipePortRight = (CustomSeekBarPreference) findPreference("du_fling_longswipe_port_right");
        val = Settings.Secure.getIntForUser(
                getContentResolver(), Settings.Secure.FLING_LONGSWIPE_THRESHOLD_RIGHT_PORT,
                isTablet ? 30 : 40, UserHandle.USER_CURRENT);
        mSwipePortRight.setValue(val);
        mSwipePortRight.setOnPreferenceChangeListener(this);

        mSwipePortLeft = (CustomSeekBarPreference) findPreference("du_fling_longswipe_port_left");
        val = Settings.Secure.getIntForUser(
                getContentResolver(), Settings.Secure.FLING_LONGSWIPE_THRESHOLD_LEFT_PORT,
                isTablet ? 30 : 40, UserHandle.USER_CURRENT);
        mSwipePortLeft.setValue(val);
        mSwipePortLeft.setOnPreferenceChangeListener(this);

        mSwipeLandRight = (CustomSeekBarPreference) findPreference("du_fling_longswipe_land_right");
        mSwipeLandLeft = (CustomSeekBarPreference) findPreference("du_fling_longswipe_land_left");
        mSwipeVertUp = (CustomSeekBarPreference) findPreference("du_fling_longswipe_vert_up");
        mSwipeVertDown = (CustomSeekBarPreference) findPreference("du_fling_longswipe_vert_down");

        PreferenceCategory longSwipeCategory = (PreferenceCategory) getPreferenceScreen()
                .findPreference("eos_long_swipe_category");

        if (isTablet) {
            longSwipeCategory.removePreference(mSwipeVertUp);
            longSwipeCategory.removePreference(mSwipeVertDown);
            val = Settings.Secure.getIntForUser(
                    getContentResolver(), Settings.Secure.FLING_LONGSWIPE_THRESHOLD_RIGHT_LAND,
                    25, UserHandle.USER_CURRENT);
            mSwipeLandRight.setValue(val);
            mSwipeLandRight.setOnPreferenceChangeListener(this);

            val = Settings.Secure.getIntForUser(
                    getContentResolver(), Settings.Secure.FLING_LONGSWIPE_THRESHOLD_LEFT_LAND,
                    25, UserHandle.USER_CURRENT);
            mSwipeLandLeft.setValue(val);
            mSwipeLandLeft.setOnPreferenceChangeListener(this);
        } else {
            longSwipeCategory.removePreference(mSwipeLandRight);
            longSwipeCategory.removePreference(mSwipeLandLeft);
            val = Settings.Secure.getIntForUser(
                    getContentResolver(), Settings.Secure.FLING_LONGSWIPE_THRESHOLD_UP_LAND,
                    40, UserHandle.USER_CURRENT);
            mSwipeVertUp.setValue(val);
            mSwipeVertUp.setOnPreferenceChangeListener(this);

            val = Settings.Secure.getIntForUser(
                    getContentResolver(), Settings.Secure.FLING_LONGSWIPE_THRESHOLD_DOWN_LAND,
                    40, UserHandle.USER_CURRENT);
            mSwipeVertDown.setValue(val);
            mSwipeVertDown.setOnPreferenceChangeListener(this);
        }

        mKbCursors = (SwitchPreference) findPreference("fling_keyboard_cursors");
        mKbCursors.setChecked(Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.FLING_KEYBOARD_CURSORS, 1,
                UserHandle.USER_CURRENT) == 1);
        mKbCursors.setOnPreferenceChangeListener(this);

        mLogoOpacity = (CustomSeekBarPreference) findPreference("fling_logo_opacity");
        int alpha = Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.FLING_LOGO_OPACITY, 255,
                UserHandle.USER_CURRENT);
        mLogoOpacity.setValue(alpha);
        mLogoOpacity.setOnPreferenceChangeListener(this);

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
    public boolean onPreferenceTreeClick(Preference preference) {
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

        return super.onPreferenceTreeClick(preference);
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
                    Settings.Secure.FLING_TRAILS_WIDTH, val, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mLongPressTimeout) {
            int val = (Integer) newValue;
            val -= 100;
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.FLING_LONGPRESS_TIMEOUT, val, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mSwipePortRight) {
            int val = (Integer) newValue;
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.FLING_LONGSWIPE_THRESHOLD_RIGHT_PORT, val,
                    UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mSwipePortLeft) {
            int val = (Integer) newValue;
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.FLING_LONGSWIPE_THRESHOLD_LEFT_PORT, val,
                    UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mSwipeLandRight) {
            int val = (Integer) newValue;
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.FLING_LONGSWIPE_THRESHOLD_RIGHT_LAND, val,
                    UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mSwipeLandLeft) {
            int val = (Integer) newValue;
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.FLING_LONGSWIPE_THRESHOLD_LEFT_LAND, val,
                    UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mSwipeVertUp) {
            int val = (Integer) newValue;
            Settings.Secure
                    .putIntForUser(getContentResolver(),
                            Settings.Secure.FLING_LONGSWIPE_THRESHOLD_UP_LAND, val,
                            UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mSwipeVertDown) {
            int val = (Integer) newValue;
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.FLING_LONGSWIPE_THRESHOLD_DOWN_LAND, val,
                    UserHandle.USER_CURRENT);
            return true;
        } else if (preference.equals(mKbCursors)) {
            boolean enabled = ((Boolean) newValue).booleanValue();
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.FLING_KEYBOARD_CURSORS, enabled ? 1 : 0,
                    UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mLogoOpacity) {
            int val = (Integer) newValue;
            Settings.Secure.putIntForUser(getContentResolver(),
                    Settings.Secure.FLING_LOGO_OPACITY, val,
                    UserHandle.USER_CURRENT);
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
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.RESURRECTED;
    }
}
