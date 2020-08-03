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
import android.app.AlertDialog;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceCategory;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.os.UserHandle;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.rr.Preferences.*;
import com.android.settings.rr.utils.RRUtils;
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

@SearchIndexable
public class ColorSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {
    private static final String TAG = "ColorSettings";
    private static final String ACCENT_COLOR = "accent_color";
    private static final String GRADIENT_COLOR = "gradient_color_prop";
    private static final String RESET = "reset";
    private static final String RESET_GRAD = "reset_2";
    static final int DEFAULT_ACCENT_COLOR = 0xff4285f4;
    static final int DEFAULT_GRADIENT_COLOR = 0xff0d5bd9;
    private SystemSettingColorPickerPreference mAccentColor;
    private SystemSettingColorPickerPreference mGradientColor;
    private Preference mReset;
    private Preference mResetGrad;


    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rr_color_settings);
        mAccentColor = (SystemSettingColorPickerPreference) findPreference(ACCENT_COLOR);
        int intColor = Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.ACCENT_COLOR, DEFAULT_ACCENT_COLOR, UserHandle.USER_CURRENT);
        String hexColor = String.format("#%08x", (0xff4285f4 & intColor));
        if (hexColor.equals("#ff4285f4")) {
            mAccentColor.setSummary(R.string.theme_picker_default);
        } else {
            mAccentColor.setSummary(hexColor);
        }
        mAccentColor.setNewPreviewColor(intColor);
        mAccentColor.setAlphaSliderEnabled(false);
        mAccentColor.setOnPreferenceChangeListener(this);
        mGradientColor = (SystemSettingColorPickerPreference) findPreference(GRADIENT_COLOR);
        int intgColor = Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.GRADIENT_COLOR_PROP, DEFAULT_ACCENT_COLOR, UserHandle.USER_CURRENT);
        String hexColor1 = String.format("#%08x", (0xff4285f4 & intColor));
        if (hexColor.equals("#ff4285f4")) {
            mGradientColor.setSummary(R.string.theme_picker_default);
        } else {
            mGradientColor.setSummary(hexColor1);
        }
        mGradientColor.setNewPreviewColor(intgColor);
        mGradientColor.setAlphaSliderEnabled(false);
        mGradientColor.setOnPreferenceChangeListener(this);

        mReset = (Preference) findPreference(RESET);
        mResetGrad = (Preference) findPreference(RESET_GRAD);

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

        mFooterPreferenceMixin.createFooterPreference().setTitle(R.string.rr_accent_tutorial);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
         if (preference == mAccentColor) {
             String hex = SystemSettingColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
             if (hex.equals("#ff4285f4")) {
                 mAccentColor.setSummary(R.string.theme_picker_default);
             } else {
                 mAccentColor.setSummary(hex);
             }
             int intHex = SystemSettingColorPickerPreference.convertToColorInt(hex);
                  Settings.System.putIntForUser(getContext().getContentResolver(),
                  Settings.System.ACCENT_COLOR, intHex, UserHandle.USER_CURRENT);
             return true;
           } else if (preference == mGradientColor) {
             String hex = SystemSettingColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
             if (hex.equals("#ff0d5bd9")) {
                 mGradientColor.setSummary(R.string.theme_picker_default);
             } else {
                 mGradientColor.setSummary(hex);
             }
             int intHex = SystemSettingColorPickerPreference.convertToColorInt(hex);
                  Settings.System.putIntForUser(getContext().getContentResolver(),
                  Settings.System.GRADIENT_COLOR_PROP, intHex, UserHandle.USER_CURRENT);
             return true;
           }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(final Preference preference) {
        if (preference == mReset) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
            alertDialog.setTitle(getString(R.string.rr_reset));
            alertDialog.setMessage(getString(R.string.rr_reset_message_warning_sum));
            alertDialog.setPositiveButton(getString(android.R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Settings.System.putIntForUser(getContext().getContentResolver(),
                        Settings.System.ACCENT_COLOR, DEFAULT_ACCENT_COLOR,
                        UserHandle.USER_CURRENT);
                        Settings.System.putIntForUser(getContext().getContentResolver(),
                        Settings.System.GRADIENT_COLOR_PROP, DEFAULT_GRADIENT_COLOR,
                        UserHandle.USER_CURRENT);
                    }
                });
            alertDialog.setNegativeButton(getString(android.R.string.cancel), null);
            alertDialog.show();
        } else if (preference == mResetGrad) {
            int intColor = Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.ACCENT_COLOR, DEFAULT_ACCENT_COLOR, UserHandle.USER_CURRENT);
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
            alertDialog.setTitle(getString(R.string.rr_reset_grad));
            alertDialog.setMessage(getString(R.string.rr_reset_grad_summary));
            alertDialog.setPositiveButton(getString(android.R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Settings.System.putIntForUser(getContext().getContentResolver(),
                        Settings.System.GRADIENT_COLOR_PROP, intColor,
                        UserHandle.USER_CURRENT);
                    }
                });
            alertDialog.setNegativeButton(getString(android.R.string.cancel), null);
            alertDialog.show();
        } else {
          super.onPreferenceTreeClick(preference);
        }
        return true;
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
                    sir.xmlResId = R.xml.rr_color_settings;
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
