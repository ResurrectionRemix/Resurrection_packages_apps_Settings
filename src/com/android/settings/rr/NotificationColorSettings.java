/* 
 * Copyright (C) 2014 RR
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.android.settings.rr.SeekBarPreference;
import com.android.settings.rr.SeekBarPreferenceCham;

import android.provider.SearchIndexableResource;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.internal.logging.MetricsLogger;

import java.util.ArrayList;
import java.util.List;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class NotificationColorSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener ,Indexable {

    private static final String PREF_CAT_COLORS =
            "notification_cat_colors";
    private static final String PREF_MEDIA_BG_MODE =
            "notification_media_bg_mode";
    private static final String PREF_APP_ICON_BG_MODE =
            "notification_app_icon_bg_mode";
    private static final String PREF_APP_ICON_COLOR_MODE =
            "notification_app_icon_color_mode";
    private static final String PREF_BG_COLOR =
            "notification_bg_color";
    private static final String PREF_BG_GUTS_COLOR =
            "notification_bg_guts_color";
    private static final String PREF_APP_ICON_BG_COLOR =
            "notification_app_icon_bg_color";
    private static final String PREF_ICON_COLOR =
            "notification_icon_color";
    private static final String PREF_CLEAR_ALL_ICON_COLOR =
            "notification_drawer_clear_all_icon_color";
    private static final String PREF_TRANSPARENT_VOLUME_DIALOG = "transparent_volume_dialog";
    private static final String PREF_TRANSPARENT_POWER_MENU = "transparent_power_menu";
    private static final String PREF_TRANSPARENT_POWER_DIALOG_DIM = "transparent_power_dialog_dim";
    private static final String PREF_VOLUME_DIALOG_STROKE = "volume_dialog_stroke";
    private static final String PREF_VOLUME_DIALOG_STROKE_COLOR = "volume_dialog_stroke_color";
    private static final String PREF_VOLUME_DIALOG_STROKE_THICKNESS = "volume_dialog_stroke_thickness";
    private static final String PREF_VOLUME_DIALOG_CORNER_RADIUS = "volume_dialog_corner_radius";

    private static final String PREF_VOLUME_DIALOG_STROKE_DASH_WIDTH = "volume_dialog_dash_width";
    private static final String PREF_VOLUME_DIALOG_STROKE_DASH_GAP = "volume_dialog_dash_gap";
    private static final String PREF_QS_PANEL_LOGO = "qs_panel_logo";
    private static final String PREF_QS_PANEL_LOGO_COLOR = "qs_panel_logo_color";
    private static final String PREF_QS_PANEL_LOGO_ALPHA = "qs_panel_logo_alpha";
    private static final String PREF_NOTIFICATION_ALPHA = "notification_alpha";

    private static final String PREF_GRADIENT_ORIENTATION = "volume_dialog_background_gradient_orientation";
    private static final String PREF_USE_CENTER_COLOR = "volume_dialog_background_gradient_use_center_color";
    private static final String PREF_START_COLOR = "volume_dialog_background_color_start";
    private static final String PREF_CENTER_COLOR = "volume_dialog_background_color_center";
    private static final String PREF_END_COLOR = "volume_dialog_background_color_end";
    private static final String BG_COLORS = "volume_bg_colors";

    private static final String VOLUME_DIALOG_SLIDER_COLOR = "volume_dialog_slider_color";
    private static final String VOLUME_DIALOG_SLIDER_INACTIVE_COLOR = "volume_dialog_slider_inactive_color";
    private static final String VOLUME_DIALOG_SLIDER_ICON_COLOR = "volume_dialog_slider_icon_color";
    private static final String VOLUME_DIALOG_EXPAND_BUTTON_COLOR = "volume_dialog_expand_button_color";
    private static final String VOLUME_DIALOG_ICON_COLOR = "volume_dialog_icon_color";

    static final int DEFAULT_QS_PANEL_LOGO_COLOR = 0xFF80CBC4;
    private static final int BACKGROUND_ORIENTATION_T_B = 270;

    private static final int RR_BLUE_GREY = 0xff1b1f23;
    private static final int SYSTEMUI_SECONDARY = 0xff384248;
    private static final int WHITE = 0xffffffff;
    private static final int BLACK = 0xff000000;
    private static final int HOLO_BLUE_LIGHT = 0xff33b5e5;
    private static final int TRANSLUCENT_HOLO_BLUE_LIGHT = 0x4d33b5e5;
    private static final int TRANSLUCENT_WHITE = 0x4dffffff;
    private static final int MATERIAL_GREEN = 0xff009688;
    private static final int MATERIAL_BLUE_GREY = 0xff37474f;
    
    static final int DEFAULT_VOLUME_DIALOG_STROKE_COLOR = 0xFF80CBC4;

    private static final int MENU_RESET = Menu.FIRST;
    private static final int DLG_RESET = 0;

    private static final int DISABLED  = 0;
    private static final int ACCENT    = 1;
    private static final int CUSTOM = 2;

    private ListPreference mMediaBgMode;
    private ListPreference mAppIconBgMode;
    private ListPreference mAppIconColorMode;
    private ColorPickerPreference mBgColor;
    private ColorPickerPreference mBgGutsColor;
    private ColorPickerPreference mAppIconBgColor;
    private ColorPickerPreference mClearAllIconColor;	
    private SeekBarPreferenceCham mVolumeDialogAlpha;	
    private SeekBarPreferenceCham mPowerMenuAlpha;
    private SeekBarPreferenceCham mPowerDialogDim;
    private SeekBarPreferenceCham mNotificationsAlpha;
    private ListPreference mVolumeDialogStroke;
    private ColorPickerPreference mVolumeDialogStrokeColor;
    private SeekBarPreferenceCham mVolumeDialogStrokeThickness;
    private SeekBarPreferenceCham mVolumeDialogCornerRadius;
    private SeekBarPreferenceCham mVolumeDialogDashWidth;
    private SeekBarPreferenceCham mVolumeDialogDashGap;
    private SwitchPreference mUseCenterColor;
    private ColorPickerPreference mStartColor;
    private ColorPickerPreference mCenterColor;
    private ColorPickerPreference mEndColor;
    private ListPreference mGradientOrientation;
    private ListPreference mQSPanelLogo;
    private ColorPickerPreference mQSPanelLogoColor;
    private SeekBarPreferenceCham mQSPanelLogoAlpha;
    private ColorPickerPreference mSliderColor;
    private ColorPickerPreference mSliderInactiveColor;
    private ColorPickerPreference mSliderIconColor;
    private ColorPickerPreference mExpandButtonColor;
    private ColorPickerPreference mIconColor;
    private ContentResolver mResolver;

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.APPLICATION;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        refreshSettings();
    }

    public void refreshSettings() {
        PreferenceScreen prefs = getPreferenceScreen();
        if (prefs != null) {
            prefs.removeAll();
        }

        addPreferencesFromResource(R.xml.notification_colors);
        mResolver = getActivity().getContentResolver();

        int intColor;
        String hexColor;

	    mMediaBgMode = (ListPreference) findPreference(PREF_MEDIA_BG_MODE);
	    int mediaBgMode = Settings.System.getInt(mResolver,
            Settings.System.NOTIFICATION_MEDIA_BG_MODE, 0);
	    mMediaBgMode.setValue(String.valueOf(mediaBgMode));
	    mMediaBgMode.setSummary(mMediaBgMode.getEntry());
	    mMediaBgMode.setOnPreferenceChangeListener(this);

	    mBgColor = (ColorPickerPreference) findPreference(PREF_BG_COLOR);
	    intColor = Settings.System.getInt(mResolver,
            Settings.System.NOTIFICATION_BG_COLOR, WHITE); 
	    mBgColor.setNewPreviewColor(intColor);
	    hexColor = String.format("#%08x", (0xffffffff & intColor));
	    mBgColor.setSummary(hexColor);
	    mBgColor.setResetColors(RR_BLUE_GREY, RR_BLUE_GREY);
	    mBgColor.setOnPreferenceChangeListener(this);

	    mBgGutsColor =
                (ColorPickerPreference) findPreference(PREF_BG_GUTS_COLOR);
	    intColor = Settings.System.getInt(mResolver,
                Settings.System.NOTIFICATION_GUTS_BG_COLOR, SYSTEMUI_SECONDARY); 
	    mBgGutsColor.setNewPreviewColor(intColor);
	    hexColor = String.format("#%08x", (0xffffffff & intColor));
	    mBgGutsColor.setSummary(hexColor);
	    mBgGutsColor.setResetColors(SYSTEMUI_SECONDARY, SYSTEMUI_SECONDARY);
	    mBgGutsColor.setOnPreferenceChangeListener(this);

	    PreferenceCategory colorCat =
                (PreferenceCategory) findPreference(PREF_CAT_COLORS);

	    mClearAllIconColor =
                (ColorPickerPreference) findPreference(PREF_CLEAR_ALL_ICON_COLOR);
	    intColor = Settings.System.getInt(mResolver,
                Settings.System.NOTIFICATION_DRAWER_CLEAR_ALL_ICON_COLOR, WHITE); 
	    mClearAllIconColor.setNewPreviewColor(intColor);
	    hexColor = String.format("#%08x", (0xffffffff & intColor));
	    mClearAllIconColor.setSummary(hexColor);
	    mClearAllIconColor.setResetColors(WHITE, HOLO_BLUE_LIGHT);
	    mClearAllIconColor.setOnPreferenceChangeListener(this);

	    // Volume dialog alpha
            mVolumeDialogAlpha =
                    (SeekBarPreferenceCham) findPreference(PREF_TRANSPARENT_VOLUME_DIALOG);
            int volumeDialogAlpha = Settings.System.getInt(mResolver,
                    Settings.System.TRANSPARENT_VOLUME_DIALOG, 255);
            mVolumeDialogAlpha.setValue(volumeDialogAlpha / 1);
            mVolumeDialogAlpha.setOnPreferenceChangeListener(this);

            // Power menu alpha
            mPowerMenuAlpha =
                    (SeekBarPreferenceCham) findPreference(PREF_TRANSPARENT_POWER_MENU);
            int powerMenuAlpha = Settings.System.getInt(mResolver,
                    Settings.System.TRANSPARENT_POWER_MENU, 100);
            mPowerMenuAlpha.setValue(powerMenuAlpha / 1);
            mPowerMenuAlpha.setOnPreferenceChangeListener(this);

            // Notifications alpha
            mNotificationsAlpha =
                    (SeekBarPreferenceCham) findPreference(PREF_NOTIFICATION_ALPHA);
            int notificationsAlpha = Settings.System.getInt(mResolver,
                    Settings.System.NOTIFICATION_ALPHA, 255);
            mNotificationsAlpha.setValue(notificationsAlpha / 1);
            mNotificationsAlpha.setOnPreferenceChangeListener(this);

	    // Power/reboot dialog dim
            mPowerDialogDim =
                    (SeekBarPreferenceCham) findPreference(PREF_TRANSPARENT_POWER_DIALOG_DIM);
            int powerDialogDim = Settings.System.getInt(mResolver,
                    Settings.System.TRANSPARENT_POWER_DIALOG_DIM, 50);
            mPowerDialogDim.setValue(powerDialogDim / 1);
            mPowerDialogDim.setOnPreferenceChangeListener(this);

	    setHasOptionsMenu(true);
            
            // Volume dialog stroke
            mVolumeDialogStroke =
                    (ListPreference) findPreference(PREF_VOLUME_DIALOG_STROKE);
            int volumeDialogStroke = Settings.System.getIntForUser(mResolver,
                            Settings.System.VOLUME_DIALOG_STROKE, 0,
                            UserHandle.USER_CURRENT);
            mVolumeDialogStroke.setValue(String.valueOf(volumeDialogStroke));
            mVolumeDialogStroke.setSummary(mVolumeDialogStroke.getEntry());
            mVolumeDialogStroke.setOnPreferenceChangeListener(this);

            // Volume dialog stroke color
            mVolumeDialogStrokeColor =
                    (ColorPickerPreference) findPreference(PREF_VOLUME_DIALOG_STROKE_COLOR);
            mVolumeDialogStrokeColor.setOnPreferenceChangeListener(this);
            intColor = Settings.System.getInt(mResolver,
                    Settings.System.VOLUME_DIALOG_STROKE_COLOR, DEFAULT_VOLUME_DIALOG_STROKE_COLOR);
            hexColor = String.format("#%08x", (0xFF80CBC4 & intColor));
            mVolumeDialogStrokeColor.setSummary(hexColor);
            mVolumeDialogStrokeColor.setNewPreviewColor(intColor);

            // Volume dialog stroke thickness
            mVolumeDialogStrokeThickness =
                    (SeekBarPreferenceCham) findPreference(PREF_VOLUME_DIALOG_STROKE_THICKNESS);
            int volumeDialogStrokeThickness = Settings.System.getInt(mResolver,
                    Settings.System.VOLUME_DIALOG_STROKE_THICKNESS, 4);
            mVolumeDialogStrokeThickness.setValue(volumeDialogStrokeThickness / 1);
            mVolumeDialogStrokeThickness.setOnPreferenceChangeListener(this);

            // Volume dialog corner radius
            mVolumeDialogCornerRadius =
                    (SeekBarPreferenceCham) findPreference(PREF_VOLUME_DIALOG_CORNER_RADIUS);
            int volumeDialogCornerRadius = Settings.System.getInt(mResolver,
                    Settings.System.VOLUME_DIALOG_CORNER_RADIUS, 2);
            mVolumeDialogCornerRadius.setValue(volumeDialogCornerRadius / 1);
            mVolumeDialogCornerRadius.setOnPreferenceChangeListener(this);
            
            
             // Volume dialog dash width
             mVolumeDialogDashWidth =
                     (SeekBarPreferenceCham) findPreference(PREF_VOLUME_DIALOG_STROKE_DASH_WIDTH);
             int volumeDialogDashWidth = Settings.System.getInt(mResolver,
                     Settings.System.VOLUME_DIALOG_STROKE_DASH_WIDTH, 0);
             if (volumeDialogDashWidth != 0) {
                 mVolumeDialogDashWidth.setValue(volumeDialogDashWidth / 1);
             } else {
                 mVolumeDialogDashWidth.setValue(0);
             }
             mVolumeDialogDashWidth.setOnPreferenceChangeListener(this);
 
             // Volume dialog dash gap
             mVolumeDialogDashGap =
                     (SeekBarPreferenceCham) findPreference(PREF_VOLUME_DIALOG_STROKE_DASH_GAP);
             int volumeDialogDashGap = Settings.System.getInt(mResolver,
                     Settings.System.VOLUME_DIALOG_STROKE_DASH_GAP, 10);
             mVolumeDialogDashGap.setValue(volumeDialogDashGap / 1);
             mVolumeDialogDashGap.setOnPreferenceChangeListener(this);
             
             VolumeDialogSettingsDisabler(volumeDialogStroke);
            
             // QS panel RR logo
             mQSPanelLogo =
                     (ListPreference) findPreference(PREF_QS_PANEL_LOGO);
             int qSPanelLogo = Settings.System.getIntForUser(mResolver,
                             Settings.System.QS_PANEL_LOGO, 0,
                             UserHandle.USER_CURRENT);
             mQSPanelLogo.setValue(String.valueOf(qSPanelLogo));
             mQSPanelLogo.setSummary(mQSPanelLogo.getEntry());
             mQSPanelLogo.setOnPreferenceChangeListener(this);
 
             // QS panel RR logo color
             mQSPanelLogoColor =
                     (ColorPickerPreference) findPreference(PREF_QS_PANEL_LOGO_COLOR);
             mQSPanelLogoColor.setOnPreferenceChangeListener(this);
             int qSPanelLogoColor = Settings.System.getInt(mResolver,
                     Settings.System.QS_PANEL_LOGO_COLOR, DEFAULT_QS_PANEL_LOGO_COLOR);
             String qSHexLogoColor = String.format("#%08x", (0xFF80CBC4 & qSPanelLogoColor));
             mQSPanelLogoColor.setSummary(qSHexLogoColor);
             mQSPanelLogoColor.setNewPreviewColor(qSPanelLogoColor);
 
             // QS panel RR logo alpha
             mQSPanelLogoAlpha =
                     (SeekBarPreferenceCham) findPreference(PREF_QS_PANEL_LOGO_ALPHA);
             int qSPanelLogoAlpha = Settings.System.getInt(mResolver,
                     Settings.System.QS_PANEL_LOGO_ALPHA, 51);
             mQSPanelLogoAlpha.setValue(qSPanelLogoAlpha / 1);
             mQSPanelLogoAlpha.setOnPreferenceChangeListener(this);

 	    mGradientOrientation = (ListPreference) findPreference(PREF_GRADIENT_ORIENTATION);
            final int orientation = Settings.System.getInt(mResolver,
                    Settings.System.VOLUME_DIALOG_BACKGROUND_GRADIENT_ORIENTATION,
                    BACKGROUND_ORIENTATION_T_B);
            mGradientOrientation.setValue(String.valueOf(orientation));
            mGradientOrientation.setSummary(mGradientOrientation.getEntry());
            mGradientOrientation.setOnPreferenceChangeListener(this);
    
            mStartColor =
                    (ColorPickerPreference) findPreference(PREF_START_COLOR);
            intColor = Settings.System.getInt(mResolver,
                    Settings.System.VOLUME_DIALOG_BACKGROUND_COLOR_START, BLACK); 
            mStartColor.setNewPreviewColor(intColor);
            hexColor = String.format("#%08x", (0xffffffff & intColor));
            mStartColor.setSummary(hexColor);
            mStartColor.setOnPreferenceChangeListener(this);
    
            final boolean useCenterColor = Settings.System.getInt(mResolver,
                    Settings.System.VOLUME_DIALOG_BACKGROUND_GRADIENT_USE_CENTER_COLOR, 0) == 1;;
    
            mUseCenterColor = (SwitchPreference) findPreference(PREF_USE_CENTER_COLOR);
            mUseCenterColor.setChecked(useCenterColor);
            mUseCenterColor.setOnPreferenceChangeListener(this);
    
            mStartColor.setTitle(getResources().getString(R.string.background_start_color_title));

                mCenterColor =
                        (ColorPickerPreference) findPreference(PREF_CENTER_COLOR);
                intColor = Settings.System.getInt(mResolver,
                        Settings.System.VOLUME_DIALOG_BACKGROUND_COLOR_CENTER, BLACK); 
                mCenterColor.setNewPreviewColor(intColor);
                hexColor = String.format("#%08x", (0xffffffff & intColor));
                mCenterColor.setSummary(hexColor);
                mCenterColor.setOnPreferenceChangeListener(this);

    		mEndColor =
                (ColorPickerPreference) findPreference(PREF_END_COLOR);
        	intColor = Settings.System.getInt(mResolver,
               		Settings.System.VOLUME_DIALOG_BACKGROUND_COLOR_END, BLACK); 
        	mEndColor.setNewPreviewColor(intColor);
       		hexColor = String.format("#%08x", (0xffffffff & intColor));
        	mEndColor.setSummary(hexColor);
		mEndColor.setOnPreferenceChangeListener(this);

        	mIconColor =
                (ColorPickerPreference) findPreference(VOLUME_DIALOG_ICON_COLOR);
        	intColor = Settings.System.getInt(mResolver,
              	  Settings.System.VOLUME_DIALOG_ICON_COLOR,
                	MATERIAL_GREEN);
        	mIconColor.setNewPreviewColor(intColor);
      		hexColor = String.format("#%08x", (0xffffffff & intColor));
       		mIconColor.setSummary(hexColor);
mIconColor.setOnPreferenceChangeListener(this);

     		mSliderColor =
                (ColorPickerPreference) findPreference(VOLUME_DIALOG_SLIDER_COLOR);
       		intColor = Settings.System.getInt(mResolver,
               		 Settings.System.VOLUME_DIALOG_SLIDER_COLOR, MATERIAL_GREEN);
       	        mSliderColor.setNewPreviewColor(intColor);
        	hexColor = String.format("#%08x", (0xffffffff & intColor));
       		mSliderColor.setSummary(hexColor);
	        mSliderColor.setOnPreferenceChangeListener(this);

                        mSliderInactiveColor =
                (ColorPickerPreference) findPreference(VOLUME_DIALOG_SLIDER_INACTIVE_COLOR);
        	 intColor = Settings.System.getInt(mResolver,
                         Settings.System.VOLUME_DIALOG_SLIDER_INACTIVE_COLOR, WHITE); 
                 mSliderInactiveColor.setNewPreviewColor(intColor);
                 hexColor = String.format("#%08x", (0xffffffff & intColor));
                 mSliderInactiveColor.setSummary(hexColor);
                 mSliderInactiveColor.setOnPreferenceChangeListener(this);

                 mSliderIconColor =
                         (ColorPickerPreference) findPreference(VOLUME_DIALOG_SLIDER_ICON_COLOR);
                 intColor = Settings.System.getInt(mResolver,
                         Settings.System.VOLUME_DIALOG_SLIDER_ICON_COLOR, WHITE); 
                 mSliderIconColor.setNewPreviewColor(intColor);
                 hexColor = String.format("#%08x", (0xffffffff & intColor));
                 mSliderIconColor.setSummary(hexColor);
                 mSliderIconColor.setOnPreferenceChangeListener(this);

                 mExpandButtonColor =
                         (ColorPickerPreference) findPreference(VOLUME_DIALOG_EXPAND_BUTTON_COLOR);
                 intColor = Settings.System.getInt(mResolver,
                         Settings.System.VOLUME_DIALOG_EXPAND_BUTTON_COLOR, WHITE); 
                 mExpandButtonColor.setNewPreviewColor(intColor);
                 hexColor = String.format("#%08x", (0xffffffff & intColor));
                 mExpandButtonColor.setSummary(hexColor);
	         mExpandButtonColor.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset)
                .setIcon(R.drawable.ic_action_reset) // use the KitKat backup icon
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                showDialogInner(DLG_RESET);
                return true;
             default:
                return super.onContextItemSelected(item);
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean value;
        String hex;
        int intHex;

        if (preference == mMediaBgMode) {
            int mediaBgMode = Integer.valueOf((String) newValue);
            int index = mMediaBgMode.findIndexOfValue((String) newValue);
            Settings.System.putInt(mResolver,
                Settings.System.NOTIFICATION_MEDIA_BG_MODE, mediaBgMode);
            preference.setSummary(mMediaBgMode.getEntries()[index]);
            return true;
        } else if (preference == mAppIconBgMode) {
            int appIconBgMode = Integer.valueOf((String) newValue);
            int index = mAppIconBgMode.findIndexOfValue((String) newValue);
            Settings.System.putInt(mResolver,
                Settings.System.NOTIFICATION_APP_ICON_BG_MODE, appIconBgMode);
            preference.setSummary(mAppIconBgMode.getEntries()[index]);
            refreshSettings();
            return true;
        } else if (preference == mAppIconColorMode) {
            int appIconColorMode = Integer.valueOf((String) newValue);
            int index = mAppIconColorMode.findIndexOfValue((String) newValue);
            Settings.System.putInt(mResolver,
                Settings.System.NOTIFICATION_APP_ICON_COLOR_MODE, appIconColorMode);
            preference.setSummary(mAppIconColorMode.getEntries()[index]);
            return true;
        } else if (preference == mBgColor) {
            hex = ColorPickerPreference.convertToARGB(
                Integer.valueOf(String.valueOf(newValue)));
            intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(mResolver,
                Settings.System.NOTIFICATION_BG_COLOR, intHex);
            preference.setSummary(hex);
            return true;
        } else if (preference == mBgGutsColor) {
            hex = ColorPickerPreference.convertToARGB(
                Integer.valueOf(String.valueOf(newValue)));
            intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(mResolver,
                Settings.System.NOTIFICATION_GUTS_BG_COLOR, intHex);
            preference.setSummary(hex);
            return true;
        } else if (preference == mAppIconBgColor) {
            hex = ColorPickerPreference.convertToARGB(
                Integer.valueOf(String.valueOf(newValue)));
            intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(mResolver,
                Settings.System.NOTIFICATION_APP_ICON_BG_COLOR, intHex);
            preference.setSummary(hex);
            return true;
        } else if (preference == mClearAllIconColor) {
            hex = ColorPickerPreference.convertToARGB(
                Integer.valueOf(String.valueOf(newValue)));
            intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(mResolver,
                Settings.System.NOTIFICATION_DRAWER_CLEAR_ALL_ICON_COLOR, intHex);
            preference.setSummary(hex);
            return true;
        } else if (preference == mVolumeDialogAlpha) {
                int alpha = (Integer) newValue;
                Settings.System.putInt(mResolver,
                        Settings.System.TRANSPARENT_VOLUME_DIALOG, alpha * 1);
                return true;
	}  else if (preference == mPowerMenuAlpha) {
		int alpha = (Integer) newValue;
                Settings.System.putInt(mResolver,
                        Settings.System.TRANSPARENT_POWER_MENU, alpha * 1);
                return true;
        } else if (preference == mNotificationsAlpha) {
                int alpha = (Integer) newValue;
                Settings.System.putInt(mResolver,
                        Settings.System.NOTIFICATION_ALPHA, alpha * 1);
                return true;
	}  else if (preference == mPowerDialogDim) {
		int alpha = (Integer) newValue;
                Settings.System.putInt(mResolver,
                        Settings.System.TRANSPARENT_POWER_DIALOG_DIM, alpha * 1);
                return true;
	}  else if (preference == mVolumeDialogStroke) {
                int volumeDialogStroke = Integer.parseInt((String) newValue);
                int index = mVolumeDialogStroke.findIndexOfValue((String) newValue);
                Settings.System.putIntForUser(mResolver, Settings.System.
                        VOLUME_DIALOG_STROKE, volumeDialogStroke, UserHandle.USER_CURRENT);
                mVolumeDialogStroke.setSummary(mVolumeDialogStroke.getEntries()[index]);
                VolumeDialogSettingsDisabler(volumeDialogStroke);
                return true;
        }  else if (preference == mVolumeDialogStrokeColor) {
                hex = ColorPickerPreference.convertToARGB(
                        Integer.valueOf(String.valueOf(newValue)));
                preference.setSummary(hex);
                intHex = ColorPickerPreference.convertToColorInt(hex);
                Settings.System.putInt(mResolver,
                        Settings.System.VOLUME_DIALOG_STROKE_COLOR, intHex);
                return true;
        }  else if (preference == mVolumeDialogStrokeThickness) {
                int val = (Integer) newValue;
                Settings.System.putInt(mResolver,
                        Settings.System.VOLUME_DIALOG_STROKE_THICKNESS, val * 1);
                return true;
        }  else if (preference == mVolumeDialogCornerRadius) {
                int val = (Integer) newValue;
                Settings.System.putInt(mResolver,
                        Settings.System.VOLUME_DIALOG_CORNER_RADIUS, val * 1);
                return true;
        } else if (preference == mVolumeDialogDashWidth) {
                 int val = (Integer) newValue;
                 Settings.System.putInt(mResolver,
                         Settings.System.VOLUME_DIALOG_STROKE_DASH_WIDTH, val * 1);
                 return true;
        } else if (preference == mVolumeDialogDashGap) {
                 int val = (Integer) newValue;
                 Settings.System.putInt(mResolver,
                         Settings.System.VOLUME_DIALOG_STROKE_DASH_GAP, val * 1);
                 return true;
        } else if (preference == mQSPanelLogo) {
                 int qSPanelLogo = Integer.parseInt((String) newValue);
                 int index = mQSPanelLogo.findIndexOfValue((String) newValue);
                 Settings.System.putIntForUser(mResolver, Settings.System.
                         QS_PANEL_LOGO, qSPanelLogo, UserHandle.USER_CURRENT);
                 mQSPanelLogo.setSummary(mQSPanelLogo.getEntries()[index]);
                 QSPanelLogoSettingsDisabler(qSPanelLogo);
                 return true;
        } else if (preference == mQSPanelLogoColor) {
                 hex = ColorPickerPreference.convertToARGB(
                         Integer.valueOf(String.valueOf(newValue)));
                 preference.setSummary(hex);
                 intHex = ColorPickerPreference.convertToColorInt(hex);
                 Settings.System.putInt(mResolver,
                         Settings.System.QS_PANEL_LOGO_COLOR, intHex);
                 return true;
        } else if (preference == mQSPanelLogoAlpha) {
                 int val = (Integer) newValue;
                 Settings.System.putInt(mResolver,
                        Settings.System.QS_PANEL_LOGO_ALPHA, val * 1);
                return true;
        } else if (preference == mUseCenterColor) {
            value = (Boolean) newValue;
            Settings.System.putInt(mResolver,
                    Settings.System.VOLUME_DIALOG_BACKGROUND_GRADIENT_USE_CENTER_COLOR,
                    value ? 1 : 0);
            refreshSettings();
            return true;
        } else if (preference == mStartColor) {
            hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            intHex = ColorPickerPreference.convertToColorInt(hex);
                Settings.System.putInt(mResolver,
                        Settings.System.VOLUME_DIALOG_BACKGROUND_COLOR_START, intHex);
                preference.setSummary(hex);
                return true;
            } else if (preference == mCenterColor) {
                hex = ColorPickerPreference.convertToARGB(
                        Integer.valueOf(String.valueOf(newValue)));
                intHex = ColorPickerPreference.convertToColorInt(hex);
                Settings.System.putInt(mResolver,
                        Settings.System.VOLUME_DIALOG_BACKGROUND_COLOR_CENTER, intHex);
                preference.setSummary(hex);
                return true;
            } else if (preference == mEndColor) {
                hex = ColorPickerPreference.convertToARGB(
                        Integer.valueOf(String.valueOf(newValue)));
                intHex = ColorPickerPreference.convertToColorInt(hex);
                Settings.System.putInt(mResolver,
                        Settings.System.VOLUME_DIALOG_BACKGROUND_COLOR_END, intHex);
                preference.setSummary(hex);
                return true;
            } else if (preference == mGradientOrientation) {
                int intValue = Integer.valueOf((String) newValue);
                int index = mGradientOrientation.findIndexOfValue((String) newValue);
                Settings.System.putInt(mResolver,
                        Settings.System.VOLUME_DIALOG_BACKGROUND_GRADIENT_ORIENTATION,
                        intValue);
                mGradientOrientation.setSummary(mGradientOrientation.getEntries()[index]);
                return true;
            } else if (preference == mSliderColor) {
            hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(mResolver,
                    Settings.System.VOLUME_DIALOG_SLIDER_COLOR, intHex);
            preference.setSummary(hex);
            return true;
           } else if (preference == mSliderInactiveColor) {
            hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(mResolver,
                    Settings.System.VOLUME_DIALOG_SLIDER_INACTIVE_COLOR, intHex);
            preference.setSummary(hex);
            return true;
          } else if (preference == mSliderIconColor) {
            hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(mResolver,
                    Settings.System.VOLUME_DIALOG_SLIDER_ICON_COLOR, intHex);
            preference.setSummary(hex);
            return true;
          } else if (preference == mExpandButtonColor) {
            hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(mResolver,
                    Settings.System.VOLUME_DIALOG_EXPAND_BUTTON_COLOR, intHex);
            preference.setSummary(hex);
            return true;
	  } else if (preference == mIconColor) {
            hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(mResolver,
                    Settings.System.VOLUME_DIALOG_ICON_COLOR, intHex);
            preference.setSummary(hex);
return true;
	 }
        return false;
    }
    
    private void VolumeDialogSettingsDisabler(int volumeDialogStroke) {
            if (volumeDialogStroke == 0) {
                mVolumeDialogStrokeColor.setEnabled(false);
                mVolumeDialogStrokeThickness.setEnabled(false);
                mVolumeDialogDashWidth.setEnabled(false);
                mVolumeDialogDashGap.setEnabled(false);
            } else if (volumeDialogStroke == 1) {
                mVolumeDialogStrokeColor.setEnabled(false);
                mVolumeDialogStrokeThickness.setEnabled(true);
                mVolumeDialogDashWidth.setEnabled(true);
                mVolumeDialogDashGap.setEnabled(true);
            } else {
                mVolumeDialogStrokeColor.setEnabled(true);
                mVolumeDialogStrokeThickness.setEnabled(true);
                mVolumeDialogDashWidth.setEnabled(true);
                mVolumeDialogDashGap.setEnabled(true);
            }
        }
        
       private void QSPanelLogoSettingsDisabler(int qSPanelLogo) {
             if (qSPanelLogo == 0) {
                 mQSPanelLogoColor.setEnabled(false);
                 mQSPanelLogoAlpha.setEnabled(false);
             } else if (qSPanelLogo == 1) {
                 mQSPanelLogoColor.setEnabled(false);
                 mQSPanelLogoAlpha.setEnabled(true);
             } else {
                 mQSPanelLogoColor.setEnabled(true);
                 mQSPanelLogoAlpha.setEnabled(true);
             }
         }

       private void showDialogInner(int id) {
        DialogFragment newFragment = MyAlertDialogFragment.newInstance(id);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(int id) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            frag.setArguments(args);
            return frag;
        }

        NotificationColorSettings getOwner() {
            return (NotificationColorSettings) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_RESET:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.reset)
                    .setMessage(R.string.dlg_reset_values_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setNeutralButton(R.string.dlg_reset_android,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.NOTIFICATION_MEDIA_BG_MODE, 0);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.NOTIFICATION_APP_ICON_BG_MODE, 0);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.NOTIFICATION_APP_ICON_COLOR_MODE, 0);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.NOTIFICATION_BG_COLOR,
                                    WHITE);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.NOTIFICATION_GUTS_BG_COLOR,
                                    SYSTEMUI_SECONDARY);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.NOTIFICATION_APP_ICON_BG_COLOR,
                                    TRANSLUCENT_WHITE);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.NOTIFICATION_TEXT_COLOR, BLACK);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.NOTIFICATION_ICON_COLOR, BLACK);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.NOTIFICATION_DRAWER_CLEAR_ALL_ICON_COLOR,
                                    WHITE);
                            getOwner().refreshSettings();
                        }
                    })
                    .setPositiveButton(R.string.dlg_reset_rr,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.NOTIFICATION_MEDIA_BG_MODE, 1);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.NOTIFICATION_APP_ICON_BG_MODE, 1);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.NOTIFICATION_APP_ICON_COLOR_MODE, 1);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.NOTIFICATION_BG_COLOR,
                                    WHITE);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.NOTIFICATION_GUTS_BG_COLOR,
                                    SYSTEMUI_SECONDARY);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.NOTIFICATION_APP_ICON_BG_COLOR,
                                    TRANSLUCENT_HOLO_BLUE_LIGHT);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.NOTIFICATION_TEXT_COLOR,
                                    HOLO_BLUE_LIGHT);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.NOTIFICATION_ICON_COLOR,
                                    HOLO_BLUE_LIGHT);
                            Settings.System.putInt(getOwner().mResolver,
                                    Settings.System.NOTIFICATION_DRAWER_CLEAR_ALL_ICON_COLOR,
                                    HOLO_BLUE_LIGHT);
                            getOwner().refreshSettings();
                        }
                    })
                    .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }

        @Override
        public void onCancel(DialogInterface dialog) {

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
                    sir.xmlResId = R.xml.notification_colors;
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
