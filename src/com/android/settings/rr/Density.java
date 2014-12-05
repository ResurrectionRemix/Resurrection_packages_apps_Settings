/*
 * Copyright (C) 2013 The Android Open Source Project
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
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.IPackageDataObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.rr.util.CMDProcessor;
import com.android.settings.rr.util.CommandResult;
import com.android.settings.rr.util.Helpers;
import com.android.settings.rr.util.AbstractAsyncSuCMDProcessor;

public class Density extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String TAG = "Density";

    ListPreference mStockDensity;
    Preference mReboot;
    ListPreference mCustomDensity;

    private static final int DIALOG_DENSITY = 101;
    private static final int DIALOG_WARN_DENSITY = 102;

    protected Context mContext;

    int newDensityValue;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.density);

        mContext = getActivity().getApplicationContext();

        String currentDensity = SystemProperties.get("ro.sf.lcd_density");
        PreferenceScreen prefs = getPreferenceScreen();

        mStockDensity = (ListPreference) findPreference("stock_density");
        mStockDensity.setOnPreferenceChangeListener(this);

        mReboot = findPreference("reboot");

        mCustomDensity = (ListPreference) findPreference("lcd_density");
        mCustomDensity.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mReboot) {
            PowerManager pm = (PowerManager) getActivity()
                    .getSystemService(Context.POWER_SERVICE);
            pm.reboot("Resetting density");
            return true;

        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        LayoutInflater factory = LayoutInflater.from(mContext);

        switch (dialogId) {
            case DIALOG_DENSITY:
                final View textEntryView = factory.inflate(
                        R.layout.alert_dialog_text_entry, null);
                return new AlertDialog.Builder(getActivity())
                        .setTitle(getResources().getString(R.string.set_custom_density_title))
                        .setView(textEntryView)
                        .setPositiveButton(getResources().getString(R.string.set_custom_density_set), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                EditText dpi = (EditText) textEntryView.findViewById(R.id.dpi_edit);
                                Editable text = dpi.getText();
                                Log.i(TAG, text.toString());

                                try {
                                    newDensityValue = Integer.parseInt(text.toString());
                                    showDialog(DIALOG_WARN_DENSITY);
                                } catch (Exception e) {
                                    mCustomDensity.setSummary(getResources().getString(R.string.custom_density_summary_invalid));
                                }

                            }
                        })
                        .setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                dialog.dismiss();
                            }
                        }).create();
            case DIALOG_WARN_DENSITY:
                return new AlertDialog.Builder(getActivity())
                        .setTitle(getResources().getString(R.string.custom_density_dialog_title))
                        .setMessage(
                                getResources().getString(R.string.custom_density_dialog_summary))
                        .setCancelable(false)
                        .setNeutralButton(getResources().getString(R.string.custom_density_dialog_button_got), new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                if (setLcdDensity(newDensityValue)) {
                                    mCustomDensity.setSummary(newDensityValue + "");
                                }
                            }
                        })
                        .setPositiveButton(getResources().getString(R.string.custom_density_dialog_button_reboot), new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (setLcdDensity(newDensityValue)) {
                                    PowerManager pm = (PowerManager) getActivity()
                                            .getSystemService(Context.POWER_SERVICE);
                                    pm.reboot("Resetting density");
                                }
                            }
                        })
                        .setNegativeButton(getResources().getString(R.string.cancel),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                        .create();
        }
        return null;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mCustomDensity) {
            String strValue = (String) newValue;
            if (strValue.equals(getResources().getString(R.string.custom_density))) {
                showDialog(DIALOG_DENSITY);
                return true;
            } else {
                newDensityValue = Integer.parseInt((String) newValue);
                showDialog(DIALOG_WARN_DENSITY);
                return true;
            }
        } else if (preference == mStockDensity) {
            newDensityValue = Integer.parseInt((String) newValue);
            if (setLcdDensity(newDensityValue)) {
                mStockDensity.setSummary(getResources().getString(
                    R.string.stock_density_changed_summary) + newDensityValue);
            }
            return true;
        }

        return false;
    }

    private boolean setLcdDensity(int newDensity) {
        Helpers.getMount("rw");
        if (!CMDProcessor.runSuCommand(
        "busybox sed -i 's|ro.sf.lcd_density=.*|" + "ro.sf.lcd_density" + "=" + newDensity + "|' " + "/system/build.prop")
        .success()) 
        {
            showRootDeniedInfoDialog();
            return false;
        }
        Helpers.getMount("ro");
        return true;
    }

    private void showRootDeniedInfoDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle(R.string.su_dialog_error_title);
        alertDialog.setMessage(R.string.su_dialog_error_message);
        alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        alertDialog.create().show();
    }
}
