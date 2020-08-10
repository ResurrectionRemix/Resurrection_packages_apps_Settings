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

import com.android.settings.rr.utils.RRUtils;
@SearchIndexable
public class QSMainSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {

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

    private LineageSecureSettingListPreference mQsPos;
    private SystemSettingListPreference mQsAuto;
    private SystemSettingListPreference mBgMode;
    private SystemSettingListPreference mIconMode;
    private SystemSettingSwitchPreference mRgb;
    private SystemSettingSwitchPreference mUseFw;
    private SystemSettingSwitchPreference mDarkTile;
    private SystemSettingSwitchPreference mInactiveTile;
    private SystemSettingSwitchPreference mTileGradient;
    private SystemSettingListPreference mTintMode;
    private SystemSettingSwitchPreference mRgbIcon;
    private PreferenceCategory mThemes;
    private SystemSettingColorPickerPreference mBgColor;
    private SystemSettingColorPickerPreference mIconColor;
    private LineageSystemSettingListPreference mQuickPulldown;
    private SystemSettingEditTextPreference mFooterString;
    protected Context mContext;
    private SystemSettingColorPickerPreference mQsPanelColor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rr_qsmain);
		ContentResolver resolver = getActivity().getContentResolver();
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
        if (qsTileStyle == 0) { 
            mTileGradient.setEnabled(true);
        } else {
            mTileGradient.setEnabled(false);
            mTileGradient.setSummary(R.string.qs_themes_warning);
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
        mContext = getActivity().getApplicationContext();

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
        updateThemespref(mRgb.isChecked());
        updateDarktileState(isrgb);
        updateInactivePrefs(tintgradient);
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

    public void updateDarktileState(int rgb) {
        if (rgb == 0){
            mDarkTile.setEnabled(true);
        } else {
            mDarkTile.setEnabled(false);
            mDarkTile.setSummary(R.string.disable_rgb); 
        }
    }
 
    public void updateInactivePrefs(boolean active) {
        int qsTileStyle = Settings.System.getIntForUser(getActivity().getContentResolver(),
                Settings.System.QS_TILE_STYLE, 0,
  	        UserHandle.USER_CURRENT);
       if (qsTileStyle != 0) return;
       if (active)
            mInactiveTile.setEnabled(false);
        else
            mInactiveTile.setEnabled(true);
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
             return true;
        } else if (preference == mTileGradient) {
             boolean value = (Boolean) newValue;
             updateInactivePrefs(value);
             return true;
        }  else if (preference == mUseFw) {
             boolean value = (Boolean) newValue;
             int isrgb = Settings.System.getInt(getContentResolver(),
                Settings.System.QS_TILE_ACCENT_TINT, 0) ;
             return true;
        } else if (preference == mTintMode) {
             int value = Integer.parseInt((String) newValue);
             updatesTintPrefs(value);
             updateDarktileState(value);
             return true;
        } 
        return false;
    }

    public void updatesTintPrefs(int enabled) {
        if (enabled == 2) 
            mRgbIcon.setEnabled(true);
        else 
            mRgbIcon.setEnabled(false);
    }

    public void updateThemespref(boolean enabled) {
        if (enabled) 
            mThemes.setEnabled(false);
        else 
            mThemes.setEnabled(true);
    }

    private void updateprefs(int mode) {
        if (mode == 2)
            mBgColor.setEnabled(true);
        else 
            mBgColor.setEnabled(false);
    }

    private void updatesliderprefs(int mode) {
        if (mode == 0)
            mQsAuto.setEnabled(false);
        else 
            mQsAuto.setEnabled(true);
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
