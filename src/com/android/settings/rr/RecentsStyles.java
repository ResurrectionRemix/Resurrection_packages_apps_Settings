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
public class RecentsStyles extends SettingsPreferenceFragment  implements Preference.OnPreferenceChangeListener ,Indexable {


    private static final String FAB_COLOR = "fab_button_color";
    private static final String RECENTS_STYLE = "clear_recents_style";	
    private static final String MEMBAR_COLOR = "mem_bar_color";	
    private static final String MEM_TEXT_COLOR = "mem_text_color";
    private static final String CLEAR_BUTTON_COLOR = "clear_button_color";
    private static final String RECENTS_DATE_COLOR = "recents_date_color";
    private static final String RECENTS_CLOCK_COLOR = "recents_clock_color";   
    private static final String PIN = "pin_button_color";	
    private static final String MULTI_WINDOW = "mw_button_color";
    private static final String FLOATING_BUTTON = "float_button_color";
    private static final String CLOSE_APP = "kill_app_button_color";
    private static final String APP_ICON = "tv_app_color";
    private static final String APP_DESC = "tv_app_text_color";
    private static final String FAB_ANIM_STYLE = "fab_animation_style";

    static final int DEFAULT = 0xffffffff;
    static final int DEFAULT_BG_ICON = 0x00000000;	
    static final int DEFAULT_BG_MEM_BAR = 0xff009688;	
    static final int DEFAULT_BG_FAB = 0xffDC4C3C;	
    private static final int MENU_RESET = Menu.FIRST;
	

    private ColorPickerPreference mMemTextColor;
    private ColorPickerPreference mMemBarColor;
    private ColorPickerPreference mClearButtonColor;
    private ColorPickerPreference mfabColor;
    private ColorPickerPreference mClockColor;
    private ColorPickerPreference mDateColor;
    private ColorPickerPreference mPinColor;
    private ColorPickerPreference mMwColor;
    private ColorPickerPreference mFloatColor;
    private ColorPickerPreference mCloseColor;
    private ColorPickerPreference mAppColor;
    private ColorPickerPreference mAppText;
    
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
        
        
        mPinColor= (ColorPickerPreference) prefSet.findPreference(PIN);
        mPinColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.PIN_BUTTON_COLOR, DEFAULT);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mPinColor.setSummary(hexColor);
        mPinColor.setNewPreviewColor(intColor);
        
        mFloatColor= (ColorPickerPreference) prefSet.findPreference(FLOATING_BUTTON);
        mFloatColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.FLOAT_BUTTON_COLOR, DEFAULT);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mFloatColor.setSummary(hexColor);
        mFloatColor.setNewPreviewColor(intColor);        
        
        
        mMwColor= (ColorPickerPreference) prefSet.findPreference(MULTI_WINDOW);
        mMwColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.MW_BUTTON_COLOR, DEFAULT);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mMwColor.setSummary(hexColor);
        mMwColor.setNewPreviewColor(intColor);        
        
        
        mCloseColor= (ColorPickerPreference) prefSet.findPreference(CLOSE_APP);
        mCloseColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.KILL_APP_BUTTON_COLOR, DEFAULT);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mCloseColor.setSummary(hexColor);
        mCloseColor.setNewPreviewColor(intColor);        
        
        
        mAppColor= (ColorPickerPreference) prefSet.findPreference(APP_ICON);
        mAppColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.TV_APP_COLOR, DEFAULT);
        hexColor = String.format("#%08x", (0x00000000 & intColor));
        mAppColor.setSummary(hexColor);
        mAppColor.setNewPreviewColor(intColor);        
        
        
        mAppText= (ColorPickerPreference) prefSet.findPreference(APP_DESC);
        mAppText.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.TV_APP_TEXT_COLOR, DEFAULT);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mAppText.setSummary(hexColor);
        mAppText.setNewPreviewColor(intColor);        

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
        } else if (preference == mMwColor) {
                String hex = ColorPickerPreference.convertToARGB(
                        Integer.valueOf(String.valueOf(newValue)));
                preference.setSummary(hex);
                int intHex = ColorPickerPreference.convertToColorInt(hex);
                Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                        Settings.System.MW_BUTTON_COLOR, intHex);
                return true;
        } else if (preference == mFloatColor) {
                String hex = ColorPickerPreference.convertToARGB(
                        Integer.valueOf(String.valueOf(newValue)));
                preference.setSummary(hex);
                int intHex = ColorPickerPreference.convertToColorInt(hex);
                Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                        Settings.System.FLOAT_BUTTON_COLOR, intHex);
                return true;
        } else if (preference == mPinColor) {
                String hex = ColorPickerPreference.convertToARGB(
                        Integer.valueOf(String.valueOf(newValue)));
                preference.setSummary(hex);
                int intHex = ColorPickerPreference.convertToColorInt(hex);
                Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                        Settings.System.PIN_BUTTON_COLOR, intHex);
                return true;
        } else if (preference == mCloseColor) {
                String hex = ColorPickerPreference.convertToARGB(
                        Integer.valueOf(String.valueOf(newValue)));
                preference.setSummary(hex);
                int intHex = ColorPickerPreference.convertToColorInt(hex);
                Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                        Settings.System.KILL_APP_BUTTON_COLOR, intHex);
                return true;
        } else if (preference == mAppColor) {
                String hex = ColorPickerPreference.convertToARGB(
                        Integer.valueOf(String.valueOf(newValue)));
                preference.setSummary(hex);
                int intHex = ColorPickerPreference.convertToColorInt(hex);
                Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                        Settings.System.TV_APP_COLOR, intHex);
                return true;
        } else if (preference == mAppText) {
                String hex = ColorPickerPreference.convertToARGB(
                        Integer.valueOf(String.valueOf(newValue)));
                preference.setSummary(hex);
                int intHex = ColorPickerPreference.convertToColorInt(hex);
                Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                        Settings.System.TV_APP_TEXT_COLOR, intHex);
                return true;
        }  else if (preference == mFabanimation) {
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
        mMemTextColor.setNewPreviewColor(DEFAULT);
        mMemTextColor.setSummary(R.string.default_string);
        Settings.System.putInt(getContentResolver(),
                Settings.System.MEM_BAR_COLOR,DEFAULT);
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
        mMwColor.setNewPreviewColor(DEFAULT);
        mMwColor.setSummary(R.string.default_string);
        Settings.System.putInt(getContentResolver(),
                Settings.System.MW_BUTTON_COLOR, DEFAULT);                 
        mFloatColor.setNewPreviewColor(DEFAULT);
        mFloatColor.setSummary(R.string.default_string);
        Settings.System.putInt(getContentResolver(),
                Settings.System.FLOAT_BUTTON_COLOR, DEFAULT);                 
        mPinColor.setNewPreviewColor(DEFAULT);
        mPinColor.setSummary(R.string.default_string);
        Settings.System.putInt(getContentResolver(),
                Settings.System.PIN_BUTTON_COLOR, DEFAULT);               
        mCloseColor.setNewPreviewColor(DEFAULT);
        mCloseColor.setSummary(R.string.default_string);
        Settings.System.putInt(getContentResolver(),
                Settings.System.KILL_APP_BUTTON_COLOR, DEFAULT);                
        mAppColor.setNewPreviewColor(DEFAULT_BG_ICON);
        mAppColor.setSummary(R.string.default_string);
        Settings.System.putInt(getContentResolver(),
                Settings.System.TV_APP_COLOR, DEFAULT_BG_ICON);               
        mAppText.setNewPreviewColor(DEFAULT);
        mAppText.setSummary(R.string.default_string);
        Settings.System.putInt(getContentResolver(),
                Settings.System.TV_APP_TEXT_COLOR, DEFAULT);                 
    }


    @Override
    public void onResume() {
        super.onResume();
    }
    
	    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                                                                             boolean enabled) {
                     ArrayList<SearchIndexableResource> result =
                             new ArrayList<SearchIndexableResource>();
 
                     SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.recent_styles;
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
