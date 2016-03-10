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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SwitchPreference;
import com.android.settings.rr.SeekBarPreference;
import android.provider.Settings;
import com.android.settings.util.Helpers;
import org.cyanogenmod.internal.util.CmLockPatternUtils;
import com.android.settings.Utils;
import android.provider.SearchIndexableResource;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.util.Log;

import com.android.internal.logging.MetricsLogger;
import cyanogenmod.providers.CMSettings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.util.Helpers;
import net.margaritov.preference.colorpicker.ColorPickerPreference;

import java.util.List;
import java.util.ArrayList;

public class QsColors extends SettingsPreferenceFragment  implements Preference.OnPreferenceChangeListener {

 private static final String QS_HEADER_TEXT_COLOR = "qs_header_text_color";
 private static final String QS_HEADER_COLOR = "qs_header_color";
 private static final String QS_TEXT_COLOR = "qs_text_color";
 private static final String QS_ICON_COLOR = "qs_icon_color";
 private static final String QS_BACKGROUND_COLOR = "qs_bg_color";
 private static final String QS_COLOR_SWITCH = "qs_color_switch";
 private static final String QS_ICON = "qs_brightness_icon_color";

    static final int DEFAULT = 0xffffffff;
    static final int DEFAULT_BG = 0xff263238;
    static final int DEFAULT_HEADER_BG = 0xff384248;
    static final int DEFAULT_SECONDARY_TEXT = 0xb3ffffff;
    static final int DEFAULT_TEXT = 0xffffffff;
    
    private static final int MENU_RESET = Menu.FIRST;
	

    private ColorPickerPreference mHeaderTextColor;
    private ColorPickerPreference mHeaderColor;
    private ColorPickerPreference mQsTextColor;
    private ColorPickerPreference mQsIconColor;	
    private ColorPickerPreference mQsBgColor;	
    private ColorPickerPreference mSliderIconColor;
    private SwitchPreference mQsColorSwitch;
   

 @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.rr_qs_colors);
        PreferenceScreen prefSet = getPreferenceScreen();
        final ContentResolver resolver = getActivity().getContentResolver();

   	int intColor;
        String hexColor;

        mHeaderTextColor = (ColorPickerPreference) findPreference(QS_HEADER_TEXT_COLOR);
        mHeaderTextColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.QS_HEADER_TEXT_COLOR, DEFAULT);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mHeaderTextColor.setSummary(hexColor);
        mHeaderTextColor.setNewPreviewColor(intColor);

        mHeaderColor = (ColorPickerPreference) findPreference(QS_HEADER_COLOR);
        mHeaderColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.QS_HEADER_COLOR,DEFAULT_HEADER_BG);
        hexColor = String.format("#%08x", (0xff384248 & intColor));
        mHeaderColor.setSummary(hexColor);
        mHeaderColor.setNewPreviewColor(intColor);

        mQsTextColor = (ColorPickerPreference) findPreference(QS_TEXT_COLOR);
        mQsTextColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.QS_TEXT_COLOR, DEFAULT_SECONDARY_TEXT);
        hexColor = String.format("#%08x", (0xb3ffffff & intColor));
        mQsTextColor.setSummary(hexColor);
        mQsTextColor.setNewPreviewColor(intColor);

        mQsIconColor = (ColorPickerPreference) findPreference(QS_ICON_COLOR);
        mQsIconColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.QS_ICON_COLOR, DEFAULT);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mQsIconColor.setSummary(hexColor);
        mQsIconColor.setNewPreviewColor(intColor);

       	mQsBgColor = (ColorPickerPreference) findPreference(QS_BACKGROUND_COLOR);
        mQsBgColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.QS_BACKGROUND_COLOR, DEFAULT_BG);
        hexColor = String.format("#%08x", (0xff263238 & intColor));
        mQsBgColor.setSummary(hexColor);
        mQsBgColor.setNewPreviewColor(intColor);

       	mSliderIconColor = (ColorPickerPreference) findPreference(QS_ICON);
        mSliderIconColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.QS_BRIGHTNESS_ICON_COLOR, DEFAULT);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mSliderIconColor.setSummary(hexColor);
        mSliderIconColor.setNewPreviewColor(intColor);


        mQsColorSwitch = (SwitchPreference) prefSet.findPreference(QS_COLOR_SWITCH);
        mQsColorSwitch.setChecked((Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.QS_COLOR_SWITCH, 0) == 1));
	mQsColorSwitch.setOnPreferenceChangeListener(this);

	
	setHasOptionsMenu(true);

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
	  if (preference == mHeaderTextColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.QS_HEADER_TEXT_COLOR, intHex);
            return true;
         } else if (preference == mHeaderColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.QS_HEADER_COLOR, intHex);
            return true;
         } else if (preference == mQsTextColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.QS_TEXT_COLOR, intHex);
            return true;
         } else if (preference == mQsIconColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.QS_ICON_COLOR, intHex);
            return true;
         }  else if (preference == mQsBgColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.QS_BACKGROUND_COLOR, intHex);	
            return true;
         }  else if (preference == mQsColorSwitch) {
             boolean value = (Boolean) newValue;
             Settings.System.putInt(resolver, Settings.System.QS_COLOR_SWITCH, value ? 1 : 0);
             return true;
         }  else if (preference == mSliderIconColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.QS_BRIGHTNESS_ICON_COLOR, intHex);	
            return true;
         }
	return false;
	}


 @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset)
                .setIcon(R.drawable.ic_settings_reset)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                resetToDefault();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void resetToDefault() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle(R.string.header_colors_reset_title);
        alertDialog.setMessage(R.string.header_colors_reset_message);
        alertDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                resetValues();
            }
        });
        alertDialog.setNegativeButton(R.string.cancel, null);
        alertDialog.create().show();
    }

    private void resetValues() {
        Settings.System.putInt(getContentResolver(),
                Settings.System.QS_HEADER_TEXT_COLOR, DEFAULT);
        mHeaderTextColor.setNewPreviewColor(DEFAULT);
        mHeaderTextColor.setSummary(R.string.default_string);
        Settings.System.putInt(getContentResolver(),
                Settings.System.QS_HEADER_COLOR, DEFAULT_HEADER_BG);
        mHeaderColor.setNewPreviewColor(DEFAULT_HEADER_BG);
        mHeaderColor.setSummary(R.string.default_string);
        Settings.System.putInt(getContentResolver(),
                Settings.System.QS_ICON_COLOR, DEFAULT);
        mQsIconColor.setNewPreviewColor(DEFAULT);
        mQsIconColor.setSummary(R.string.default_string);
        Settings.System.putInt(getContentResolver(),
                Settings.System.QS_BACKGROUND_COLOR, DEFAULT_BG);
        mQsBgColor.setNewPreviewColor(DEFAULT_BG);
        mQsBgColor.setSummary(R.string.default_string);
        Settings.System.putInt(getContentResolver(),
                Settings.System.QS_BRIGHTNESS_ICON_COLOR, DEFAULT);
        mSliderIconColor.setNewPreviewColor(DEFAULT);
        mSliderIconColor.setSummary(R.string.default_string);

    }

}
