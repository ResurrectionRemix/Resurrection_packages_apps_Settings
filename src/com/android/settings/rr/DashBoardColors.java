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
import net.margaritov.preference.colorpicker.ColorPickerPreference;

import java.util.List;
import java.util.ArrayList;

public class DashBoardColors extends SettingsPreferenceFragment  implements Preference.OnPreferenceChangeListener ,Indexable {

 private static final String DASHBOARD_ICON_COLOR = "db_icon_color";
 private static final String DASHBOARD_TEXT_COLOR = "db_text_color";
 static final int DEFAULT = 0xffffffff;
 private static final int MENU_RESET = Menu.FIRST;
	

    private ColorPickerPreference mIconColor;
    private ColorPickerPreference mTextColor;

 @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.dashboard_colors);
        PreferenceScreen prefSet = getPreferenceScreen();
        final ContentResolver resolver = getActivity().getContentResolver();

   	int intColor;
        String hexColor;

        mIconColor = (ColorPickerPreference) findPreference(DASHBOARD_ICON_COLOR);
        mIconColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.DB_ICON_COLOR, DEFAULT);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mIconColor.setSummary(hexColor);
        mIconColor.setNewPreviewColor(intColor);

        mTextColor = (ColorPickerPreference) findPreference(DASHBOARD_TEXT_COLOR);
        mTextColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.DB_TEXT_COLOR, DEFAULT);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mTextColor.setSummary(hexColor);
        mTextColor.setNewPreviewColor(intColor);
	
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
	  if (preference == mIconColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.DB_ICON_COLOR, intHex);
            return true;
         } else if (preference == mTextColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.DB_TEXT_COLOR, intHex);
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
                Settings.System.DB_ICON_COLOR, DEFAULT);
        mIconColor.setNewPreviewColor(DEFAULT);
        mIconColor.setSummary(R.string.default_string);
        Settings.System.putInt(getContentResolver(),
                Settings.System.DB_TEXT_COLOR, DEFAULT);
        mTextColor.setNewPreviewColor(DEFAULT);
        mTextColor.setSummary(R.string.default_string);

    }
    
            public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                                                                             boolean enabled) {
                     ArrayList<SearchIndexableResource> result =
                             new ArrayList<SearchIndexableResource>();
 
                     SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.rr_header_colors;
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
