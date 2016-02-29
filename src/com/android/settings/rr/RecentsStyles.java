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
public class RecentsStyles extends SettingsPreferenceFragment  implements Preference.OnPreferenceChangeListener {


    private static final String FAB_COLOR = "fab_button_color";
    private static final String RECENTS_STYLE = "clear_recents_style";	
    private static final String MEMBAR_COLOR = "mem_bar_color";	
    private static final String MEM_TEXT_COLOR = "mem_text_color";
    private static final String CLEAR_BUTTON_COLOR = "clear_button_color";
    private static final String RECENTS_DATE_COLOR = "recents_date_color";
    private static final String RECENTS_CLOCK_COLOR = "recents_clock_color";

    private static final String FAB_ANIM_STYLE = "fab_animation_style";

    static final int DEFAULT = 0xffffffff;
    static final int DEFAULT_BG_MEM_TEXT = 0xffffffff;	
    static final int DEFAULT_BG_MEM_BAR = 0xff009688;	
    static final int DEFAULT_BG_FAB = 0xffDC4C3C;	
    private static final int MENU_RESET = Menu.FIRST;
	

    private ColorPickerPreference mMemTextColor;
    private ColorPickerPreference mMemBarColor;
    private ColorPickerPreference mClearButtonColor;
    private ColorPickerPreference mfabColor;
    private ColorPickerPreference mClockColor;
    private ColorPickerPreference mDateColor;
    
    private ListPreference mClearStyle;
    private ListPreference mFabanimation;	



 @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DEVELOPMENT;
    }

 @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.recent_styles);
        ContentResolver resolver = getActivity().getContentResolver();
        PreferenceScreen prefSet = getPreferenceScreen();
	
        int intColor;
        String hexColor;

        mfabColor = (ColorPickerPreference) prefSet.findPreference(FAB_COLOR);
        mfabColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.FAB_BUTTON_COLOR, DEFAULT_BG_FAB);
        hexColor = String.format("#%08x", (0xffDC4C3C & intColor));
        mfabColor.setSummary(hexColor);
        mfabColor.setNewPreviewColor(intColor);


        mClearStyle = (ListPreference) prefSet.findPreference(RECENTS_STYLE);
        mClearStyle.setValue(String.valueOf(Settings.System.getInt(
                getContentResolver(), Settings.System.CLEAR_RECENTS_STYLE, 0)));
        mClearStyle.setSummary(mClearStyle.getEntry());
        mClearStyle.setOnPreferenceChangeListener(this);

        mFabanimation = (ListPreference) prefSet.findPreference(FAB_ANIM_STYLE);
        mFabanimation.setValue(String.valueOf(Settings.System.getInt(
                getContentResolver(), Settings.System.FAB_ANIMATION_STYLE, 0)));
        mFabanimation.setSummary(mFabanimation.getEntry());
        mFabanimation.setOnPreferenceChangeListener(this);


        mMemTextColor = (ColorPickerPreference) prefSet.findPreference(MEM_TEXT_COLOR);
        mMemTextColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.MEM_TEXT_COLOR, DEFAULT);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mMemTextColor.setSummary(hexColor);
        mMemTextColor.setNewPreviewColor(intColor);

        mMemBarColor= (ColorPickerPreference) prefSet.findPreference(MEMBAR_COLOR);
        mMemBarColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.MEM_BAR_COLOR, DEFAULT_BG_MEM_BAR);
        hexColor = String.format("#%08x", (0xff009688 & intColor));
        mMemBarColor.setSummary(hexColor);
        mMemBarColor.setNewPreviewColor(intColor);

        mClearButtonColor= (ColorPickerPreference) prefSet.findPreference(CLEAR_BUTTON_COLOR);
        mClearButtonColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.CLEAR_BUTTON_COLOR, getResources().getColor(R.color.floating_action_button_touch_tint));
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mClearButtonColor.setSummary(hexColor);
        mClearButtonColor.setNewPreviewColor(intColor);

        mClockColor= (ColorPickerPreference) prefSet.findPreference(RECENTS_CLOCK_COLOR);
        mClockColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.RECENTS_CLOCK_COLOR, DEFAULT);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mClockColor.setSummary(hexColor);
        mClockColor.setNewPreviewColor(intColor);

        mDateColor= (ColorPickerPreference) prefSet.findPreference(RECENTS_DATE_COLOR);
        mDateColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.RECENTS_DATE_COLOR, DEFAULT);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mDateColor.setSummary(hexColor);
        mDateColor.setNewPreviewColor(intColor);

	setHasOptionsMenu(true);
}



	@Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
	if (preference == mClearStyle) {
            Settings.System.putInt(getContentResolver(), Settings.System.CLEAR_RECENTS_STYLE,
                    Integer.valueOf((String) newValue));
            mClearStyle.setValue(String.valueOf(newValue));
            mClearStyle.setSummary(mClearStyle.getEntry());    
        } else if (preference == mfabColor) {
                String hex = ColorPickerPreference.convertToARGB(
                        Integer.valueOf(String.valueOf(newValue)));
                preference.setSummary(hex);
                int intHex = ColorPickerPreference.convertToColorInt(hex);
                Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                        Settings.System.FAB_BUTTON_COLOR, intHex);
                return true;
        }  else if (preference == mMemTextColor) {
                String hex = ColorPickerPreference.convertToARGB(
                        Integer.valueOf(String.valueOf(newValue)));
                preference.setSummary(hex);
                int intHex = ColorPickerPreference.convertToColorInt(hex);
                Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                        Settings.System.MEM_TEXT_COLOR, intHex);
                return true;
        } else if (preference == mMemBarColor) {
                String hex = ColorPickerPreference.convertToARGB(
                        Integer.valueOf(String.valueOf(newValue)));
                preference.setSummary(hex);
                int intHex = ColorPickerPreference.convertToColorInt(hex);
                Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                        Settings.System.MEM_BAR_COLOR, intHex);
                return true;
        }  else if (preference == mClearButtonColor) {
                String hex = ColorPickerPreference.convertToARGB(
                        Integer.valueOf(String.valueOf(newValue)));
                preference.setSummary(hex);
                int intHex = ColorPickerPreference.convertToColorInt(hex);
                Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                        Settings.System.CLEAR_BUTTON_COLOR, intHex);
                return true;
        } else if (preference == mClockColor) {
                String hex = ColorPickerPreference.convertToARGB(
                        Integer.valueOf(String.valueOf(newValue)));
                preference.setSummary(hex);
                int intHex = ColorPickerPreference.convertToColorInt(hex);
                Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                        Settings.System.RECENTS_CLOCK_COLOR, intHex);
                return true;
        }  else if (preference == mDateColor) {
                String hex = ColorPickerPreference.convertToARGB(
                        Integer.valueOf(String.valueOf(newValue)));
                preference.setSummary(hex);
                int intHex = ColorPickerPreference.convertToColorInt(hex);
                Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                        Settings.System.RECENTS_DATE_COLOR, intHex);
                return true;
        } else if (preference == mFabanimation) {
            Settings.System.putInt(getContentResolver(), Settings.System.FAB_ANIMATION_STYLE,
                    Integer.valueOf((String) newValue));
            mFabanimation.setValue(String.valueOf(newValue));
            mFabanimation.setSummary(mFabanimation.getEntry());  
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
        alertDialog.setTitle(R.string.lockscreen_colors_reset_title);
        alertDialog.setMessage(R.string.lockscreen_colors_reset_message);
        alertDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                resetValues();
            }
        });
        alertDialog.setNegativeButton(R.string.cancel, null);
        alertDialog.create().show();
    }

    private void resetValues() {
	ContentResolver resolver = getActivity().getContentResolver();
        Settings.System.putInt(getContentResolver(),
                Settings.System.FAB_BUTTON_COLOR, DEFAULT_BG_FAB);
        mfabColor.setNewPreviewColor(DEFAULT_BG_FAB);
        mfabColor.setSummary(R.string.default_string);
        Settings.System.putInt(getContentResolver(),
                Settings.System.FAB_BUTTON_COLOR, DEFAULT_BG_FAB);
        mMemTextColor.setNewPreviewColor(DEFAULT_BG_MEM_TEXT);
        mMemTextColor.setSummary(R.string.default_string);
        Settings.System.putInt(getContentResolver(),
                Settings.System.MEM_BAR_COLOR,DEFAULT_BG_MEM_TEXT);
        mMemBarColor.setNewPreviewColor(DEFAULT_BG_MEM_BAR);
        mMemBarColor.setSummary(R.string.default_string);
        Settings.System.putInt(getContentResolver(),
                Settings.System.MEM_BAR_COLOR, DEFAULT_BG_MEM_BAR);
        mClearButtonColor.setNewPreviewColor(DEFAULT);
        mClearButtonColor.setSummary(R.string.default_string);
        Settings.System.putInt(getContentResolver(),
                Settings.System.CLEAR_BUTTON_COLOR, DEFAULT); 
        mClockColor.setNewPreviewColor(DEFAULT);
        mClockColor.setSummary(R.string.default_string);
        Settings.System.putInt(getContentResolver(),
                Settings.System.RECENTS_CLOCK_COLOR, DEFAULT);
        mDateColor.setNewPreviewColor(DEFAULT);
        mDateColor.setSummary(R.string.default_string);
        Settings.System.putInt(getContentResolver(),
                Settings.System.RECENTS_DATE_COLOR, DEFAULT); 
    }


    @Override
    public void onResume() {
        super.onResume();
    }

}
