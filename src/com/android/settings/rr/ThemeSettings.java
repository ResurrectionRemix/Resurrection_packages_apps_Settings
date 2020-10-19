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

import static android.os.UserHandle.USER_SYSTEM;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.app.UiModeManager;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Context;
import android.content.Intent;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import androidx.fragment.app.Fragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceCategory;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.os.UserHandle;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.ServiceManager;
import android.os.Process;
import android.os.PowerManager;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.rr.Preferences.*;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;
import lineageos.hardware.LineageHardwareManager;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import lineageos.preference.LineageSystemSettingSwitchPreference;
import android.provider.Settings;
import java.util.Objects;
import com.android.settingslib.utils.ThreadUtils;
import com.android.internal.statusbar.ThemeAccentUtils;
import com.android.internal.util.rr.RRUtils;
import com.android.settings.rr.preview.AboutSettingsPreview;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.development.OverlayCategoryPreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settings.display.darkmode.DarkModeObserver;
import com.android.settings.gestures.SystemNavigationPreferenceController;
@SearchIndexable
public class ThemeSettings extends DashboardFragment implements
        Preference.OnPreferenceChangeListener, Indexable {
    private static final String TAG = "ThemeSettings";
    private static final String PREF_THEME_SWITCH = "theme_switch";
    private static final String HEADER = "qs_header_style";
    private static final String RESET = "reset";
    private static final String ACCENT = "accent";
    private static final String SETTINGS_FRAG = "com.android.settings";
    private static final String SETTINGS_ACTION = "com.android.settings.Settings$AccentColorSettingsActivity";
    private static final String KEY_RR_BG = "android.theme.customization.rr_tools_bg";
    private static final String ANIM = "animation";
    private static final String STATIC = "preview";
    private static final String NAV_STYLE = "navbar_base";
    private static final String QS_TILE_STYLE = "qs_tile_style";
    private static final String DARK_TEXT = "dark_ui_text";
    private static final String BRIGHTNESS = "brightness_slider_style";
    private static final String SWITCH = "berry_switch_style";

    private IOverlayManager mOverlayService;
    private UiModeManager mUiModeManager;
    private LineageSystemSettingSwitchPreference mWakeProx;
    private LineageSystemSettingSwitchPreference mHighTouch;
    private ListPreference mThemeSwitch;
    private ListPreference mHeaderStyle;
    private ListPreference mRRbg;
    private Preference mReset;
    private Preference mDarkUIText;
    protected Context mContext;
    private Preference mAccent;
    private boolean mEnabled;
    private DarkModeObserver mDarkModeObserver;
    private Runnable mCallback;
    private ListPreference mQsTileStyle;
    private PreferenceCategory mNavStyle;
    PowerManager mPowerManager;
    private SystemSettingListPreference mBrightness;
    private SystemSettingListPreference mSwitch;
    private int mDefaultGradientColor;
    private int mDefaultAccentColor;

    private Preference mAnim;
    private AboutSettingsPreview mStatic;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mContext = getActivity();

        mDefaultAccentColor = getResources().getColor(
                       com.android.internal.R.color.accent_device_default_light);
        mDefaultGradientColor = getResources().getColor(
                       com.android.internal.R.color.gradient_device_default);

        int accentColor = Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.ACCENT_COLOR, mDefaultGradientColor, UserHandle.USER_CURRENT);

        int gradientColor = Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.GRADIENT_COLOR_PROP, mDefaultGradientColor, UserHandle.USER_CURRENT);

        mAccent = (Preference) findPreference(ACCENT);
		ContentResolver resolver = getActivity().getContentResolver();
        mThemeSwitch = (ListPreference) findPreference(PREF_THEME_SWITCH);
        mSwitch = (SystemSettingListPreference) findPreference(SWITCH);
        mHeaderStyle = (ListPreference) findPreference(HEADER);
        mBrightness = (SystemSettingListPreference) findPreference(BRIGHTNESS);
        mNavStyle = (PreferenceCategory) findPreference(NAV_STYLE);
        PreferenceScreen screen = getPreferenceScreen();
        mRRbg = (ListPreference) findPreference(KEY_RR_BG);
        mDarkUIText = (Preference) findPreference(DARK_TEXT);
        mThemeSwitch.setOnPreferenceChangeListener(this);
        mDarkModeObserver = new DarkModeObserver(mContext);
        mPowerManager = mContext.getSystemService(PowerManager.class);
        int systemTheme = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.SYSTEM_THEME, 0, UserHandle.USER_CURRENT);
        int valueIndex = mThemeSwitch.findIndexOfValue(String.valueOf(systemTheme));
        mThemeSwitch.setValueIndex(valueIndex);
        mThemeSwitch.setSummary(mThemeSwitch.getEntry());
        mThemeSwitch.setOnPreferenceChangeListener(this);
        String[] defaultgrad1 = getResources().getStringArray(
                R.array.switch_style_entries);
        String[] defaultgradentries1 = getResources().getStringArray(
                R.array.switch_style_values);
        String[] systementries1 = getResources().getStringArray(
                R.array.switch_style_2_entries);
        String[] systemvalues1 = getResources().getStringArray(
                R.array.switch_style_2_values);
        if (accentColor == gradientColor) {
            mSwitch.setEntries(systementries1);
            mSwitch.setEntryValues(systemvalues1);
        } else {
            mSwitch.setEntries(defaultgrad1);
            mSwitch.setEntryValues(defaultgradentries1);
        }

        mQsTileStyle = (ListPreference) findPreference(QS_TILE_STYLE);
        int qsTileStyle = Settings.System.getIntForUser(resolver,
                Settings.System.QS_TILE_STYLE, 0,
  	        UserHandle.USER_CURRENT);
        int valueInd = mQsTileStyle.findIndexOfValue(String.valueOf(qsTileStyle));
        mQsTileStyle.setValueIndex(valueInd >= 0 ? valueInd : 0);
        mQsTileStyle.setSummary(mQsTileStyle.getEntry());
        mQsTileStyle.setOnPreferenceChangeListener(this);
        String[] defaultgrad = getResources().getStringArray(
                R.array.brightness_slider_style_selector_entries);
        String[] defaultgradentries = getResources().getStringArray(
                R.array.brightness_slider_style_selector_values);
        String[] systementries = getResources().getStringArray(
                R.array.brightness_slider_style_selector2_entries);
        String[] systemvalues = getResources().getStringArray(
                R.array.brightness_slider_style_selector2_values);

        if (accentColor == gradientColor) {
            mBrightness.setEntries(systementries);
            mBrightness.setEntryValues(systemvalues);
        } else {
            mBrightness.setEntries(defaultgrad);
            mBrightness.setEntryValues(defaultgradentries);
        }

        if (SystemNavigationPreferenceController.isEdgeToEdgeEnabled(mContext) 
            || SystemNavigationPreferenceController.isSwipeUpEnabled(mContext)) {
            mNavStyle.setVisible(false);
        }
        boolean batterySaver = mPowerManager.isPowerSaveMode();

        mCallback = () -> {
            final boolean active = (getContext().getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_YES) != 0;
            if (active || batterySaver) {
                mThemeSwitch.setEnabled(true);
                mDarkUIText.setEnabled(true);
            } else {
                mThemeSwitch.setEnabled(false);
                mDarkUIText.setEnabled(false);
                mThemeSwitch.setSummary(R.string.dark_ui_warning);
            }
        };
        mDarkModeObserver.subscribe(mCallback);
        mAnim = (Preference) findPreference(ANIM);
        mStatic = (AboutSettingsPreview) findPreference(STATIC);
        boolean enabled = Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.QS_HIDE_GRADIENT, 0) == 1;
        if (enabled || batterySaver) {
            mHeaderStyle.setEnabled(false);
            mHeaderStyle.setSummary(R.string.gardient_enabled_summary);
        }

        int nav = Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.RR_CONFIG_STYLE, 0);
        if (nav != 2) {
            if (mRRbg != null) {
                screen.removePreference(mRRbg);
            }
        }
        int anim = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.RR_CONFIG_ANIM, 0);
        try {
            if (anim == 0) {
                screen.removePreference(mAnim);
            } else if (anim == 1) {
                screen.removePreference(mStatic);
            } else if (anim == 2) {
                screen.removePreference(mAnim);
                screen.removePreference(mStatic);
            }
        } catch (Exception e) {}
        mFooterPreferenceMixin.createFooterPreference().setTitle(R.string.rr_themes_tutorial);
    }



    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getSettingsLifecycle(), this);
    }

    @Override
    public boolean onPreferenceTreeClick(final Preference preference) {
         if (preference == mAccent) {
              Intent settings = new Intent(Intent.ACTION_MAIN);
              settings.setClassName(SETTINGS_FRAG, SETTINGS_ACTION);
              startActivity(settings);
        } else {
          super.onPreferenceTreeClick(preference);
        }
        return true;
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(
            Context context, Lifecycle lifecycle, Fragment fragment) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new OverlayCategoryPreferenceController(context,
                "android.theme.customization.font"));
        controllers.add(new OverlayCategoryPreferenceController(context,
                "android.theme.customization.adaptive_icon_shape"));
        controllers.add(new OverlayCategoryPreferenceController(context,
                "android.theme.customization.icon_pack.android"));
	    controllers.add(new OverlayCategoryPreferenceController(context,
		"android.theme.customization.statusbar_height"));
	     controllers.add(new OverlayCategoryPreferenceController(context,
		    "android.theme.customization.ui_radius"));
	     controllers.add(new OverlayCategoryPreferenceController(context,
		    "android.theme.customization.rr_tools_icon"));
	     controllers.add(new OverlayCategoryPreferenceController(context,
		    "android.theme.customization.rr_tools_bg"));
        return controllers;
    }

    @Override
    public void onResume() {
        super.onResume();
        mDarkModeObserver.subscribe(mCallback);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDarkModeObserver.unsubscribe();
    }


    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.rr_theme_settings;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
		ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mThemeSwitch) {
            int systemThemeValue = Integer.valueOf((String) newValue);
            Settings.System.putIntForUser(mContext.getContentResolver(),
                    Settings.System.SYSTEM_THEME, systemThemeValue, UserHandle.USER_CURRENT);
             mThemeSwitch.setSummary(mThemeSwitch.getEntries()[systemThemeValue]);
            return true;
        } else if (preference == mQsTileStyle) {
             int qsTileStyleValue = Integer.valueOf((String) newValue);
             Settings.System.putIntForUser(resolver, Settings.System.QS_TILE_STYLE,
                     qsTileStyleValue, UserHandle.USER_CURRENT);
             mQsTileStyle.setSummary(mQsTileStyle.getEntries()[qsTileStyleValue]);
             return true;
        } 
        return false;
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.rr_theme_settings;
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
