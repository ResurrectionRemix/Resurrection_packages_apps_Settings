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
import android.app.Dialog;
import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.support.v14.preference.SwitchPreference;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.rr.ScreenshotEditPackageListAdapter;
import com.android.settings.rr.ScreenshotEditPackageListAdapter.PackageItem;

public class ScreenShotSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private static final String PREF_SCREENSHOT_EDIT_USER_APP = "screenshot_edit_app";
    private static final int DIALOG_SCREENSHOT_EDIT_APP = 1;

    private Preference mScreenshotEditAppPref;
    private ScreenshotEditPackageListAdapter mPackageAdapter;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rr_screenshot_settings);

        mPackageAdapter = new ScreenshotEditPackageListAdapter(getActivity());
        mScreenshotEditAppPref = findPreference(PREF_SCREENSHOT_EDIT_USER_APP);
        mScreenshotEditAppPref.setOnPreferenceClickListener(this);
    }

    private Dialog selectPackages(int dialogId) {
        switch (dialogId) {
            case DIALOG_SCREENSHOT_EDIT_APP: {
                Dialog dialog;
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                final ListView list = new ListView(getActivity());
                list.setAdapter(mPackageAdapter);
                alertDialog.setTitle(R.string.profile_choose_app);
                alertDialog.setView(list);
                dialog = alertDialog.create();
                list.setOnItemClickListener(new OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        // Add empty application definition, the user will be able to edit it later
                        PackageItem info = (PackageItem) parent.getItemAtPosition(position);
                        Settings.System.putString(getActivity().getContentResolver(),
                                Settings.System.SCREENSHOT_EDIT_USER_APP, info.packageName);
                        dialog.cancel();
                    }
                });
                return dialog;
            }
         }
        return null;
    }
    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mScreenshotEditAppPref) {
            if (mPackageAdapter.getCount() > 0) {
                Dialog packageDialog = (Dialog) selectPackages(DIALOG_SCREENSHOT_EDIT_APP);
                packageDialog.show();
                return true;
            } else {
                Toast.makeText(getActivity(), getActivity().getString(R.string.screenshot_edit_app_no_editor),
                        Toast.LENGTH_LONG).show();
            }
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        return false;
    }
}
