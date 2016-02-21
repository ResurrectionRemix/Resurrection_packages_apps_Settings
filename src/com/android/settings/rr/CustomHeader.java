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

public class CustomHeader extends SettingsPreferenceFragment  implements Preference.OnPreferenceChangeListener {
 private static final String CUSTOM_HEADER_IMAGE = "status_bar_custom_header";
 private static final String DAYLIGHT_HEADER_PACK = "daylight_header_pack";
 private static final String DEFAULT_HEADER_PACKAGE = "com.android.systemui";
 private static final String CUSTOM_HEADER_IMAGE_SHADOW = "status_bar_custom_header_shadow";

    private ListPreference mDaylightHeaderPack;
    private SwitchPreference mCustomHeaderImage;	
    private SeekBarPreference mHeaderShadow; 

 @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.rr_custom_header);
        PreferenceScreen prefSet = getPreferenceScreen();
        final ContentResolver resolver = getActivity().getContentResolver();


	// header image packs
        final boolean customHeaderImage = Settings.System.getInt(getContentResolver(),
                Settings.System.STATUS_BAR_CUSTOM_HEADER, 0) == 1;
        mCustomHeaderImage = (SwitchPreference) findPreference(CUSTOM_HEADER_IMAGE);
        mCustomHeaderImage.setChecked(customHeaderImage);

         String imageHeaderPackage = Settings.System.getString(getContentResolver(),
                 Settings.System.STATUS_BAR_DAYLIGHT_HEADER_PACK);
         if (imageHeaderPackage == null) {
             imageHeaderPackage = DEFAULT_HEADER_PACKAGE;
         }
         mDaylightHeaderPack = (ListPreference) findPreference(DAYLIGHT_HEADER_PACK);
         List<String> entries = new ArrayList<String>();
         List<String> values = new ArrayList<String>();
         getAvailableHeaderPacks(entries, values);
         mDaylightHeaderPack.setEntries(entries.toArray(new String[entries.size()]));
         mDaylightHeaderPack.setEntryValues(values.toArray(new String[values.size()]));
 
         int valueIndexHeader = mDaylightHeaderPack.findIndexOfValue(imageHeaderPackage);
         if (valueIndexHeader == -1) {
             // no longer found
             imageHeaderPackage = DEFAULT_HEADER_PACKAGE;
             Settings.System.putString(getContentResolver(),
                     Settings.System.STATUS_BAR_DAYLIGHT_HEADER_PACK, imageHeaderPackage);
             valueIndexHeader = mDaylightHeaderPack.findIndexOfValue(imageHeaderPackage);
         }
         mDaylightHeaderPack.setValueIndex(valueIndexHeader >= 0 ? valueIndexHeader : 0);
         mDaylightHeaderPack.setSummary(mDaylightHeaderPack.getEntry());
         mDaylightHeaderPack.setOnPreferenceChangeListener(this);

        mHeaderShadow = (SeekBarPreference) findPreference(CUSTOM_HEADER_IMAGE_SHADOW);
        final int headerShadow = Settings.System.getInt(getContentResolver(),
                Settings.System.STATUS_BAR_CUSTOM_HEADER_SHADOW, 0);
        mHeaderShadow.setValue((int)((headerShadow / 255) * 100));
        mHeaderShadow.setOnPreferenceChangeListener(this);

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
	 if (preference == mDaylightHeaderPack) {
             String value = (String) newValue;
             Settings.System.putString(getContentResolver(),
                     Settings.System.STATUS_BAR_DAYLIGHT_HEADER_PACK, value);
             int valueIndex = mDaylightHeaderPack.findIndexOfValue(value);
             mDaylightHeaderPack.setSummary(mDaylightHeaderPack.getEntries()[valueIndex]);
             return true;
         } else if (preference == mHeaderShadow) {
            Integer headerShadow = (Integer) newValue;
            int realHeaderValue = (int) (((double) headerShadow / 100) * 255);
            Settings.System.putInt(getContentResolver(),
              Settings.System.STATUS_BAR_CUSTOM_HEADER_SHADOW, realHeaderValue);
	    return true;
	 }
	return false;
	}

  private void getAvailableHeaderPacks(List<String> entries, List<String> values) {
        Intent i = new Intent();
        PackageManager packageManager = getPackageManager();
        i.setAction("org.omnirom.DaylightHeaderPack");
        for (ResolveInfo r : packageManager.queryIntentActivities(i, 0)) {
            String packageName = r.activityInfo.packageName;
            if (packageName.equals(DEFAULT_HEADER_PACKAGE)) {
                values.add(0, packageName);
            } else {
                values.add(packageName);
            }
            String label = r.activityInfo.loadLabel(getPackageManager()).toString();
            if (label == null) {
                label = r.activityInfo.packageName;
            }
            if (packageName.equals(DEFAULT_HEADER_PACKAGE)) {
                entries.add(0, label);
            } else {
                entries.add(label);
            }
        }
        i.setAction("org.omnirom.DaylightHeaderPack1");
        for (ResolveInfo r : packageManager.queryIntentActivities(i, 0)) {
            String packageName = r.activityInfo.packageName;
            values.add(packageName  + "/" + r.activityInfo.name);
            String label = r.activityInfo.loadLabel(getPackageManager()).toString();
            if (label == null) {
                label = packageName;
            }
            entries.add(label);
        }
    }



	@Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
	if (preference == mCustomHeaderImage) {
                final boolean value = ((SwitchPreference)preference).isChecked();
                Settings.System.putInt(getContentResolver(),
                        Settings.System.STATUS_BAR_CUSTOM_HEADER, value ? 1 : 0);
                return true;
            }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

}
