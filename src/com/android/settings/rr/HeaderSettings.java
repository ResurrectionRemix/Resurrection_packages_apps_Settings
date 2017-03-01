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

import android.app.AlertDialog;
import android.content.Context;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.provider.Settings;

import com.android.internal.logging.MetricsProto.MetricsEvent;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;


import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import com.android.internal.util.rr.PackageUtils;

public class HeaderSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "HeaderSettings";
        private static final String CUSTOM_HEADER_IMAGE = "status_bar_custom_header";
        private static final String DAYLIGHT_HEADER_PACK = "daylight_header_pack";
        private static final String DEFAULT_HEADER_PACKAGE = "com.android.systemui";
        private static final String CUSTOM_HEADER_IMAGE_SHADOW = "status_bar_custom_header_shadow";
        private static final String CUSTOM_HEADER_PROVIDER = "custom_header_provider";
        private static final String CUSTOM_HEADER_BROWSE = "custom_header_browse";

        private ListPreference mDaylightHeaderPack;
        private SeekBarPreference mHeaderShadow;
        private ListPreference mHeaderProvider;
        private String mDaylightHeaderProvider;
        private PreferenceScreen mHeaderBrowse;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.rr_header_items);
        ContentResolver resolver = getActivity().getContentResolver();

             String settingHeaderPackage = Settings.System.getString(resolver,
                     Settings.System.STATUS_BAR_DAYLIGHT_HEADER_PACK);
             if (settingHeaderPackage == null) {
                 settingHeaderPackage = DEFAULT_HEADER_PACKAGE;
             }
             mDaylightHeaderPack = (ListPreference) findPreference(DAYLIGHT_HEADER_PACK);
 
             List<String> entries = new ArrayList<String>();
             List<String> values = new ArrayList<String>();
             getAvailableHeaderPacks(entries, values);
             mDaylightHeaderPack.setEntries(entries.toArray(new String[entries.size()]));
             mDaylightHeaderPack.setEntryValues(values.toArray(new String[values.size()]));
 
             int valueIndex = mDaylightHeaderPack.findIndexOfValue(settingHeaderPackage);
             if (valueIndex == -1) {
                 // no longer found
                 settingHeaderPackage = DEFAULT_HEADER_PACKAGE;
                 Settings.System.putString(resolver,
                         Settings.System.STATUS_BAR_DAYLIGHT_HEADER_PACK, settingHeaderPackage);
                 valueIndex = mDaylightHeaderPack.findIndexOfValue(settingHeaderPackage);
             }
             mDaylightHeaderPack.setValueIndex(valueIndex >= 0 ? valueIndex : 0);
             mDaylightHeaderPack.setSummary(mDaylightHeaderPack.getEntry());
             mDaylightHeaderPack.setOnPreferenceChangeListener(this);
 
             mHeaderShadow = (SeekBarPreference) findPreference(CUSTOM_HEADER_IMAGE_SHADOW);
             final int headerShadow = Settings.System.getInt(resolver,
                     Settings.System.STATUS_BAR_CUSTOM_HEADER_SHADOW, 80);
             mHeaderShadow.setValue((int)(((double) headerShadow / 255) * 100));
             mHeaderShadow.setOnPreferenceChangeListener(this);
 
            mDaylightHeaderProvider = getResources().getString(R.string.daylight_header_provider);
            String providerName = Settings.System.getString(resolver,
                    Settings.System.STATUS_BAR_CUSTOM_HEADER_PROVIDER);
            if (providerName == null) {
                providerName = mDaylightHeaderProvider;
            }
            mHeaderProvider = (ListPreference) findPreference(CUSTOM_HEADER_PROVIDER);
            valueIndex = mHeaderProvider.findIndexOfValue(providerName);
            mHeaderProvider.setValueIndex(valueIndex >= 0 ? valueIndex : 0);
            mHeaderProvider.setSummary(mHeaderProvider.getEntry());
            mHeaderProvider.setOnPreferenceChangeListener(this);
            mDaylightHeaderPack.setEnabled(providerName.equals(mDaylightHeaderProvider));

            mHeaderBrowse = (PreferenceScreen) findPreference(CUSTOM_HEADER_BROWSE);
            mHeaderBrowse.setEnabled(isBrowseHeaderAvailable());

 
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
    ContentResolver resolver = getActivity().getContentResolver();
              if (preference == mDaylightHeaderPack) {
                  String value = (String) newValue;
                  Settings.System.putString(resolver,
                          Settings.System.STATUS_BAR_DAYLIGHT_HEADER_PACK, value);
                  int valueIndex = mDaylightHeaderPack.findIndexOfValue(value);
                  mDaylightHeaderPack.setSummary(mDaylightHeaderPack.getEntries()[valueIndex]);
                  return true;
              } else if (preference == mHeaderShadow) {
                  Integer headerShadow = (Integer) newValue;
                  int realHeaderValue = (int) (((double) headerShadow / 100) * 255);
                  Settings.System.putInt(resolver,
                          Settings.System.STATUS_BAR_CUSTOM_HEADER_SHADOW, realHeaderValue);
                  return true;
              }  else if (preference == mHeaderProvider) {
                String value = (String) newValue;
                Settings.System.putString(resolver,
                        Settings.System.STATUS_BAR_CUSTOM_HEADER_PROVIDER, value);
                int valueIndex = mHeaderProvider.findIndexOfValue(value);
                mHeaderProvider.setSummary(mHeaderProvider.getEntries()[valueIndex]);
                mDaylightHeaderPack.setEnabled(value.equals(mDaylightHeaderProvider));
             } 
        return false;
    }

          private void getAvailableHeaderPacks(List<String> entries, List<String> values) {
              Intent i = new Intent();
              PackageManager packageManager = getActivity().getPackageManager();
              i.setAction("org.omnirom.DaylightHeaderPack");
              for (ResolveInfo r : packageManager.queryIntentActivities(i, 0)) {
                  String packageName = r.activityInfo.packageName;
                  if (packageName.equals(DEFAULT_HEADER_PACKAGE)) {
                      values.add(0, packageName);
                  } else {
                      values.add(packageName);
                  }
                  String label = r.activityInfo.loadLabel(getActivity().getPackageManager()).toString();
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
                  values.add(packageName  + "/"  + r.activityInfo.name);
  
                  String label = r.activityInfo.loadLabel(getActivity().getPackageManager()).toString();
                  if (label == null) {
                      label = packageName;
                  }
                  entries.add(label);
              }
          }

        private boolean isBrowseHeaderAvailable() {
            PackageManager pm = getActivity().getPackageManager();
            Intent browse = new Intent();
            browse.setClassName("org.omnirom.omnistyle", "org.omnirom.omnistyle.BrowseHeaderActivity");
            return pm.resolveActivity(browse, 0) != null;
        }
 
}
