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
import android.content.Intent;
import android.content.res.Resources;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.ListPreference;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Context;
import android.provider.Settings;
import android.os.UserHandle;
import android.net.Uri;
import android.util.Log;
import android.provider.SearchIndexableResource;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import lineageos.preference.LineageSystemSettingListPreference;
import lineageos.preference.LineageSecureSettingListPreference;
import com.android.settings.R;
import com.android.settings.rr.Preferences.*;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;
import lineageos.providers.LineageSettings;
import net.margaritov.preference.colorpicker.ColorPickerPreference;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import com.android.internal.util.rr.Utils;
import com.android.settings.rr.utils.RRUtils;
@SearchIndexable
public class QSMainSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {
    private static final String TAG = "QSMainSettings";

    private static final String STATUS_BAR_QUICK_QS_PULLDOWN = "qs_quick_pulldown";
    private static final int PULLDOWN_DIR_NONE = 0;
    private static final int PULLDOWN_DIR_RIGHT = 1;
    private static final int PULLDOWN_DIR_LEFT = 2;
    private static final String BG_COLOR = "notif_bg_color";
    private static final String ICON_COLOR = "notif_icon_color";
    private static final String BG_MODE = "notif_bg_color_mode";
    private static final String ICON_MODE = "notif_icon_color_mode";
    private static final String QS_POS = "qs_show_brightness_slider";
    private static final String QS_AUTO = "qs_auto_icon_pos";
    private static final String QS_BUTTONS = "qs_brightness_buttons";
    private static final String RR_FOOTER_TEXT_STRING = "rr_footer_text_string";
    private static final String QS_PANEL_COLOR = "qs_panel_color";
    private static final String THEMES = "qs_themes";
    private static final String RGB = "qs_panel_bg_rgb";
    static final int DEFAULT_QS_PANEL_COLOR = 0xffffffff;
    private static final String GRAD_TILE = "qs_tile_gradient";
    private static final String TILE_INACTIVE = "qs_tile_accent_tint_inactive";
    private static final String QS_FW = "qs_panel_bg_use_fw";
    private static final String DARK_TILE = "qs_tile_icon_primary";
    private static final String RGB_ICON = "qs_tile_rgb_tint";
    private static final String RGB_MODE = "qs_tile_accent_tint";
    private static final String QS_DATA_MODE = "qs_datausage_location";
    private static final String QS_DATA_USAGE = "qs_datausage";
    private static final String QS_BG_FILTER = "qs_panel_bg_filter";
    private static final String QS_BLUR_INT = "qs_background_blur_intensity";
    private static final String QS_RADIUS = "qs_background_blur_alpha";
    private static final String QS_FILTER_COLOR = "qs_panel_filter_color";
    private static final String QS_TITLE = "qs_tile_title_visibility";
    private static final String QS_LABEL_CAT = "qs_label_options";
    private static final String QS_LABEL_TINT = "qs_label_use_new_tint";
    private static final String QS_LABEL_INACTIVE = "qs_label_inactive_tint";
    private static final String FILE_QSPANEL_SELECT = "file_qspanel_select";
    private static final String QS_IMAGE_SWITCH = "qs_panel_type_background";
    private static final String QS_IMAGE = "qs_image";
    private static final String QS_BG_ALPHA = "qs_panel_bg_alpha";
    private static final String QS_GRADIENT_BG = "qs_new_bg_enabled";
    private static final int REQUEST_PICK_IMAGE = 0;

    private LineageSecureSettingListPreference mQsPos;
    private SystemSettingListPreference mQsAuto;
    private SystemSettingListPreference mBgMode;
    private SystemSettingListPreference mIconMode;
    private SystemSettingSwitchPreference mRgb;
    private SystemSettingSwitchPreference mUseFw;
    private SystemSettingSwitchPreference mDarkTile;
    private SystemSettingSwitchPreference mInactiveTile;
    private SystemSettingSwitchPreference mTileGradient;
    private SystemSettingSwitchPreference mButtons;
    private SystemSettingListPreference mTintMode;
    private SystemSettingListPreference mQsData;
    private SystemSettingListPreference mDataLoc;
    private SystemSettingSwitchPreference mRgbIcon;
    private SystemSettingListPreference mBgFilter;
    private SystemSettingSeekBarPreference mBlurRad;
    private SystemSettingSeekBarPreference mBlurInt;
    private SystemSettingSeekBarPreference mQsOpacity;
    private PreferenceCategory mThemes;
    private SystemSettingColorPickerPreference mBgColor;
    private SystemSettingColorPickerPreference mIconColor;
    private LineageSystemSettingListPreference mQuickPulldown;
    private SystemSettingEditTextPreference mFooterString;
    protected Context mContext;
    private SystemSettingColorPickerPreference mQsPanelColor;
    private SystemSettingColorPickerPreference mFilterColor;
    private SystemSettingSwitchPreference mTitle;
    private SystemSettingListPreference mLabelTint;
    private SystemSettingSwitchPreference mInactiveLabel;
    private SystemSettingSwitchPreference mQsNewBG;
    private PreferenceCategory mLabels;
    private SystemSettingSwitchPreference mQsImage;
    private Preference mQsPanelImage;
    private PreferenceCategory mQsImageCat;
    private boolean mIsRGB;
    private boolean mIsImage;
    private boolean TintGradEnabled = false;
    private int mDefaultGradientColor;
    private int mDefaultAccentColor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rr_qsmain);
		ContentResolver resolver = getActivity().getContentResolver();
        mContext = getActivity().getApplicationContext();

        mQsPanelImage = findPreference(FILE_QSPANEL_SELECT);
        mQsImage = (SystemSettingSwitchPreference) findPreference(QS_IMAGE_SWITCH);
        mTitle =
                (SystemSettingSwitchPreference) findPreference(QS_TITLE);
        mLabelTint =
                (SystemSettingListPreference) findPreference(QS_LABEL_TINT);
        mInactiveLabel =
                (SystemSettingSwitchPreference) findPreference(QS_LABEL_INACTIVE);
        mLabels =
                (PreferenceCategory) findPreference(QS_LABEL_CAT);
        mQsImageCat =
                (PreferenceCategory) findPreference(QS_IMAGE);
        mQsOpacity =
                (SystemSettingSeekBarPreference) findPreference(QS_BG_ALPHA);
        mQsNewBG =
                (SystemSettingSwitchPreference) findPreference(QS_GRADIENT_BG);
        mTitle.setOnPreferenceChangeListener(this);
        mLabelTint.setOnPreferenceChangeListener(this);
        mQsImage.setOnPreferenceChangeListener(this);

        mBgFilter =
                (SystemSettingListPreference) findPreference(QS_BG_FILTER);
        mBgFilter.setOnPreferenceChangeListener(this);
        mBlurRad =
                (SystemSettingSeekBarPreference) findPreference(QS_RADIUS);
        mBlurInt =
                (SystemSettingSeekBarPreference) findPreference(QS_BLUR_INT);

        mFilterColor = (SystemSettingColorPickerPreference) findPreference(QS_FILTER_COLOR);
        mFilterColor.setOnPreferenceChangeListener(this);
        int intColor = Settings.System.getIntForUser(getActivity().getContentResolver(),
                Settings.System.QS_PANEL_FILTER_COLOR, DEFAULT_QS_PANEL_COLOR, UserHandle.USER_CURRENT);
        String hexColor = String.format("#%08x", (0xffffffff & intColor));
        mFilterColor.setSummary(hexColor);
        mFilterColor.setNewPreviewColor(intColor);
        mButtons =
                (SystemSettingSwitchPreference) findPreference(QS_BUTTONS);
        int filter = Settings.System.getInt(getContentResolver(),
                Settings.System.QS_PANEL_BG_FILTER, 0) ;

        mRgb =
                (SystemSettingSwitchPreference) findPreference(RGB);
        mRgb.setOnPreferenceChangeListener(this);

        mUseFw =
                (SystemSettingSwitchPreference) findPreference(QS_FW);
        mUseFw.setOnPreferenceChangeListener(this);
        mDarkTile =
                (SystemSettingSwitchPreference) findPreference(DARK_TILE);
        mThemes =
                (PreferenceCategory) findPreference(THEMES);
        int isrgb = Settings.System.getInt(getContentResolver(),
                Settings.System.QS_TILE_ACCENT_TINT, 0) ;
        mTintMode =
                (SystemSettingListPreference) findPreference(RGB_MODE);
        mRgbIcon =
                (SystemSettingSwitchPreference) findPreference(RGB_ICON);
        mTintMode.setOnPreferenceChangeListener(this);
        int qsTileStyle = Settings.System.getIntForUser(resolver,
                Settings.System.QS_TILE_STYLE, 0,
  	        UserHandle.USER_CURRENT);
        mTileGradient = (SystemSettingSwitchPreference) findPreference(GRAD_TILE);
        mTileGradient.setOnPreferenceChangeListener(this);
        mInactiveTile = (SystemSettingSwitchPreference) findPreference(TILE_INACTIVE);
        mDefaultAccentColor = getResources().getColor(
                       com.android.internal.R.color.accent_device_default_light);
        mDefaultGradientColor = getResources().getColor(
                       com.android.internal.R.color.gradient_device_default);
        int accentColor = Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.ACCENT_COLOR, mDefaultGradientColor, UserHandle.USER_CURRENT);

        int gradientColor = Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.GRADIENT_COLOR_PROP, mDefaultGradientColor, UserHandle.USER_CURRENT);
        boolean isSameColor = accentColor == gradientColor;
        if (qsTileStyle == 0 && !isSameColor) { 
            mTileGradient.setEnabled(true);
        } else {
            mTileGradient.setEnabled(false);
            if (isSameColor) {
                mTileGradient.setSummary(R.string.qs_tile_grad_warning);
            } else {
                mTileGradient.setSummary(R.string.qs_themes_warning);
            }
        }
        mFooterString = (SystemSettingEditTextPreference) findPreference(RR_FOOTER_TEXT_STRING);
        mFooterString.setOnPreferenceChangeListener(this);
        String footerString = Settings.System.getString(getContentResolver(),
                RR_FOOTER_TEXT_STRING);
        if (footerString != null && footerString != "")
            mFooterString.setText(footerString);
        else {
            mFooterString.setText("Resurrection Remix");
            Settings.System.putString(getActivity().getContentResolver(),
                    Settings.System.RR_FOOTER_TEXT_STRING, "Resurrection Remix");
        }
        mQuickPulldown =
                (LineageSystemSettingListPreference) findPreference(STATUS_BAR_QUICK_QS_PULLDOWN);
        mQsPos =
                (LineageSecureSettingListPreference) findPreference(QS_POS);
        mQsPos.setOnPreferenceChangeListener(this);
        mQsAuto =
                (SystemSettingListPreference) findPreference(QS_AUTO);
        int position = LineageSettings.Secure.getInt(getContentResolver(),
                LineageSettings.Secure.QS_SHOW_BRIGHTNESS_SLIDER, 1);

        mQuickPulldown.setOnPreferenceChangeListener(this);
        updateQuickPulldownSummary(mQuickPulldown.getIntValue(0));
        int dataloc = Settings.System.getInt(getContentResolver(),
                Settings.System.QS_DATAUSAGE, 0);
        mQsData =
                (SystemSettingListPreference) findPreference(QS_DATA_USAGE);
        mQsData.setOnPreferenceChangeListener(this);
        mDataLoc =
                (SystemSettingListPreference) findPreference(QS_DATA_MODE);


        int color = Settings.System.getInt(getContentResolver(),
                Settings.System.NOTIF_CLEAR_ALL_BG_COLOR, 0x3980FF) ;

        int iconColor = Settings.System.getInt(getContentResolver(),
                Settings.System.NOTIF_CLEAR_ALL_ICON_COLOR, 0x3980FF);

        int mode = Settings.System.getInt(getContentResolver(),
                Settings.System.NOTIF_DISMISALL_COLOR_MODE, 0);

        int iconmode = Settings.System.getInt(getContentResolver(),
                Settings.System.NOTIF_DISMISALL_ICON_COLOR_MODE, 0);
        boolean tintgradient = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.QS_TILE_GRADIENT, 0, UserHandle.USER_CURRENT) == 1;

        mBgMode = (SystemSettingListPreference) findPreference(BG_MODE);
        mBgMode.setOnPreferenceChangeListener(this);

        mBgColor = (SystemSettingColorPickerPreference) findPreference(BG_COLOR);
        mBgColor.setNewPreviewColor(color);
        mBgColor.setAlphaSliderEnabled(false);
        String Hex = convertToRGB(color);
        mBgColor.setSummary(Hex);
        mBgColor.setOnPreferenceChangeListener(this);

        mIconMode = (SystemSettingListPreference) findPreference(ICON_MODE);
        mIconMode.setOnPreferenceChangeListener(this);

        mIconColor = (SystemSettingColorPickerPreference) findPreference(ICON_COLOR);
        mIconColor.setNewPreviewColor(iconColor);
        String Hex2 = convertToRGB(iconColor);
        mIconColor.setAlphaSliderEnabled(false);
        mIconColor.setSummary(Hex2);
        mIconColor.setOnPreferenceChangeListener(this);
        updatesTintPrefs(isrgb);
        getQsPanelColorPref();
        updateprefs(mode);
        updateIconprefs(iconmode);
        updatesliderprefs(position);
        updateDarktileState(isrgb);
        updateInactivePrefs(tintgradient);
        updateQsDataLoc(dataloc);
        updateBlurPrefs(filter);
        updatelabelCat(mTitle.isChecked());
        updateQsImageCat(mRgb.isChecked());
        updateThemespref(mRgb.isChecked());
        updateThemesprefImage(mQsImage.isChecked());

        if (Utils.isWifiOnly(mContext)) {
            mDataLoc.setVisible(false);
            mQsData.setVisible(false);
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
        if (preference == mQsPanelImage) {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_PICK_IMAGE);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

   @Override
    public void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (requestCode == REQUEST_PICK_IMAGE) {
            if (resultCode != Activity.RESULT_OK) {
                return;
            }
            final Uri imageUri = result.getData();
            Settings.System.putString(getContentResolver(), Settings.System.QS_PANEL_CUSTOM_IMAGE, imageUri.toString());
        }
    }

    public void updateQsDataLoc(int loc) {
        if (loc == 0){
            mDataLoc.setEnabled(false);
        } else {
            mDataLoc.setEnabled(true);
        }
    }

    public void updateQsImageCat(boolean enabled) {
        if (enabled){
            mQsImageCat.setEnabled(false);
        } else {
            mQsImageCat.setEnabled(true);
        }
    }

    public void updatelabelCat(boolean enabled) {
        int labeltint = Settings.System.getIntForUser(getActivity().getContentResolver(),
                Settings.System.QS_LABEL_USE_NEW_TINT, 0,
  	        UserHandle.USER_CURRENT);
        int valueInd = mLabelTint.findIndexOfValue(String.valueOf(labeltint));
        mLabelTint.setValueIndex(valueInd);
        if (!enabled){
            mLabels.setEnabled(false);
            mLabelTint.setSummary(R.string.qs_title_disabled);
            mInactiveLabel.setSummary(R.string.qs_title_disabled);
        } else {
            mLabels.setEnabled(true);
            mLabelTint.setSummary(mLabelTint.getEntry());
            mInactiveLabel.setSummary(R.string.qs_label_inactive_tint_summary);
        }
    }

    public void updateBlurPrefs(int filter) {
        if (filter == 1 || filter == 2 || filter == 5) {
            mBlurInt.setEnabled(false);
        } else {
            mBlurInt.setEnabled(true);
        }
        if (filter == 5 || filter == 6) {
            mFilterColor.setEnabled(true);
        } else {
            mFilterColor.setEnabled(false);
        }
    }
 
    public void updateDarktileState(int rgb) {
        int qsTileStyle = Settings.System.getIntForUser(getActivity().getContentResolver(),
                Settings.System.QS_TILE_STYLE, 0,
  	        UserHandle.USER_CURRENT);
        if ((rgb == 0 || rgb == 3 || rgb == 4 
            || rgb == 5) && (qsTileStyle == 7
            || qsTileStyle == 9 || qsTileStyle == 10 
            || qsTileStyle == 12 || qsTileStyle == 13
            || qsTileStyle == 16 || qsTileStyle == 17
            || qsTileStyle == 27)) {
            mDarkTile.setEnabled(false);
            mDarkTile.setSummary(R.string.already_enabled_sum); 
        } else if (rgb == 0) {
            mDarkTile.setEnabled(true);
        }  else {
            mDarkTile.setEnabled(false);
            mDarkTile.setSummary(R.string.disable_rgb); 
        }
    }

    public void updateInactivePrefs(boolean active) {
        int qsTileStyle = Settings.System.getIntForUser(getActivity().getContentResolver(),
                Settings.System.QS_TILE_STYLE, 0,
  	        UserHandle.USER_CURRENT);
        int isrgb = Settings.System.getIntForUser(getActivity().getContentResolver(),
                Settings.System.QS_TILE_ACCENT_TINT, 0,
                UserHandle.USER_CURRENT);
       if (qsTileStyle == 27) {
           mInactiveTile.setEnabled(false);
           mInactiveTile.setSummary(R.string.switch_tile_warning);
           return;
       }
       if (qsTileStyle != 0) return;
       if (active)
            mInactiveTile.setEnabled(false);
        else
            mInactiveTile.setEnabled(true);
        TintGradEnabled = active;
        updatesTintPrefs(isrgb);
    }

    private void getQsPanelColorPref() {
        mQsPanelColor = (SystemSettingColorPickerPreference) findPreference(QS_PANEL_COLOR);
        mQsPanelColor.setOnPreferenceChangeListener(this);
        int intColor = Settings.System.getIntForUser(getActivity().getContentResolver(),
                Settings.System.QS_PANEL_BG_COLOR, DEFAULT_QS_PANEL_COLOR, UserHandle.USER_CURRENT);
        String hexColor = String.format("#%08x", (0xffffffff & intColor));
        mQsPanelColor.setSummary(hexColor);
        mQsPanelColor.setNewPreviewColor(intColor);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
		ContentResolver resolver = getActivity().getContentResolver();
         if (preference == mQuickPulldown) {
             int value = Integer.parseInt((String) newValue);
             updateQuickPulldownSummary(value);
             return true;
        } else if (preference == mBgMode) {
             int value = Integer.parseInt((String) newValue);
             updateprefs(value);
             return true;
        } else if (preference == mBgFilter) {
             int value = Integer.parseInt((String) newValue);
             updateBlurPrefs(value);
             return true;
        } else if (preference ==  mQsData) {
             int value = Integer.parseInt((String) newValue);
             updateQsDataLoc(value);
             return true;
        } else if (preference == mIconMode) {
             int value = Integer.parseInt((String) newValue);
             updateIconprefs(value);
             return true;
        } else if (preference == mBgColor) {
             String hex = convertToRGB(
                    Integer.valueOf(String.valueOf(newValue)));
             preference.setSummary(hex);
             return true;
        } else if (preference == mIconColor) {
             String hex = convertToRGB(
                    Integer.valueOf(String.valueOf(newValue)));
             preference.setSummary(hex);
             return true;
        }  else if (preference == mQsPos) {
             int value = Integer.parseInt((String) newValue);
             updatesliderprefs(value);
             if (value == 3 || value == 4) {
                 RRUtils.showSystemUiRestartDialog(getContext());
             }
             return true;
        } else if (preference == mFooterString) {
            String value = (String) newValue;
            if (value != "" && value != null) {
                Settings.System.putString(getActivity().getContentResolver(),
                      Settings.System.RR_FOOTER_TEXT_STRING, value);
             } else {
                mFooterString.setText("Resurrection Remix");
                Settings.System.putString(getActivity().getContentResolver(),
                        Settings.System.RR_FOOTER_TEXT_STRING, "Resurrection Remix");
            }
            return true;
        }  else if (preference == mQsPanelColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.QS_PANEL_BG_COLOR, intHex, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mRgb) {
             boolean value = (Boolean) newValue;
             updateThemespref(value);
             updateQsImageCat(value);
             return true;
        } else if (preference == mQsImage) {
             boolean value = (Boolean) newValue;
             updateThemesprefImage(value);
             return true;
        } else if (preference == mTitle) {
             boolean value = (Boolean) newValue;
             updatelabelCat(value);
             return true;
        } else if (preference == mTileGradient) {
             boolean value = (Boolean) newValue;
             updateInactivePrefs(value);
             return true;
        }  else if (preference == mUseFw) {
             boolean value = (Boolean) newValue;
             return true;
        } else if (preference == mTintMode) {
             int value = Integer.parseInt((String) newValue);
             updatesTintPrefs(value);
             updateDarktileState(value);
             return true;
        } else if (preference == mFilterColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.QS_PANEL_FILTER_COLOR, intHex, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mLabelTint) {
            int labeltint = Integer.valueOf((String) newValue);
            mLabelTint.setSummary(mLabelTint.getEntries()[labeltint]);
            return true;
        } 
        return false;
    }

    public void updatesTintPrefs(int enabled) {
        int qsTileStyle = Settings.System.getIntForUser(getActivity().getContentResolver(),
                Settings.System.QS_TILE_STYLE, 0,
  	        UserHandle.USER_CURRENT);
        boolean tintgradient = Settings.System.getIntForUser(getActivity().getContentResolver(),
                Settings.System.QS_TILE_GRADIENT, 0, UserHandle.USER_CURRENT) == 1;
        if (enabled == 2 || enabled == 3 || enabled == 4 || enabled == 5) { 
            if (qsTileStyle == 27) {
                mRgbIcon.setEnabled(false);
                mRgbIcon.setSummary(R.string.rgb_already_enabled);
            } else {
                if (TintGradEnabled) {
                    mRgbIcon.setEnabled(false);
                    mRgbIcon.setSummary(R.string.rgb_already_enabled);
                    TintGradEnabled = false;
                } else {
                   mRgbIcon.setEnabled(true);
                   mRgbIcon.setSummary(R.string.qs_tile_rgb_tint_summary);
                } 
            }
        } 
        else {
            mRgbIcon.setEnabled(false);
            mRgbIcon.setSummary(R.string.rgb_already_disabled);
        }
    }

    public void updateThemespref(boolean rgb) {
		ContentResolver resolver = getActivity().getContentResolver();
        boolean mIsImage = Settings.System.getIntForUser(resolver,
                Settings.System.QS_PANEL_CUSTOM_IMAGE, 0, UserHandle.USER_CURRENT)  != 0;
        if (mIsImage) {
            return;
        }
        if (rgb) 
            mThemes.setEnabled(false);
        else if(mQsImage.isChecked())
            mThemes.setEnabled(false);
        else
            mThemes.setEnabled(true);
        if (rgb) {
            mQsOpacity.setEnabled(false);
            mQsOpacity.setSummary(R.string.opacity_warning);
        } else {
            mQsOpacity.setEnabled(true);
            mQsOpacity.setSummary(R.string.qs_opactiy);
        }
    }

    public void updateThemesprefImage(boolean image) {
		ContentResolver resolver = getActivity().getContentResolver();
        boolean mIsRGB = Settings.System.getIntForUser(resolver,
                Settings.System.QS_PANEL_BG_RGB, 0, UserHandle.USER_CURRENT)  != 0;
        if (mIsRGB) { 
            return;
        }
        if (image) {
            mThemes.setEnabled(false);
            mQsNewBG.setEnabled(false);
        } else {
            mThemes.setEnabled(true);
            mQsNewBG.setEnabled(true);
        }
    }

    private void updateprefs(int mode) {
        if (mode == 2)
            mBgColor.setEnabled(true);
        else 
            mBgColor.setEnabled(false);
    }

    private void updatesliderprefs(int mode) {
        if (mode == 0) {
            mQsAuto.setEnabled(false);
            mButtons.setEnabled(false);
        } else {
            mQsAuto.setEnabled(true);
            mButtons.setEnabled(true);
        }
    }


    private void updateIconprefs(int mode) {
        if (mode == 2)
            mIconColor.setEnabled(true);
        else 
            mIconColor.setEnabled(false);
    }

    private void updateQuickPulldownSummary(int value) {
        String summary="";
        switch (value) {
            case PULLDOWN_DIR_NONE:
                summary = getResources().getString(
                    R.string.status_bar_quick_qs_pulldown_off);
                break;

            case PULLDOWN_DIR_LEFT:
            case PULLDOWN_DIR_RIGHT:
                summary = getResources().getString(
                    R.string.status_bar_quick_qs_pulldown_summary,
                    getResources().getString(value == PULLDOWN_DIR_LEFT
                        ? R.string.status_bar_quick_qs_pulldown_summary_left
                        : R.string.status_bar_quick_qs_pulldown_summary_right));
                break;
        }
        mQuickPulldown.setSummary(summary);
    }


    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
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
                    sir.xmlResId = R.xml.rr_qsmain;
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
