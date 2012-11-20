/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.profiles;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.VibratorSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import com.android.settings.R;

public class VibratorPreference extends Preference implements
        CompoundButton.OnCheckedChangeListener, View.OnClickListener {

    private boolean mProtectFromCheckedChange = false;

    private CheckBox mCheckBox;

    final static String TAG = "VibratorPreference";

    private ProfileConfig.VibratorItem mVibratorItem;

    final static int defaultChoice = -1;

    private int currentChoice;

    /**
     * @param context
     * @param attrs
     * @param defStyle
     */
    public VibratorPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * @param context
     * @param attrs
     */
    public VibratorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * @param context
     */
    public VibratorPreference(Context context) {
        super(context);
        init();
    }

    @Override
    public View getView(View convertView, ViewGroup parent) {
        View view = super.getView(convertView, parent);

        View widget = view.findViewById(R.id.profile_checkbox);
        if ((widget != null) && widget instanceof CheckBox) {
            mCheckBox = (CheckBox) widget;
            mCheckBox.setOnCheckedChangeListener(this);

            mProtectFromCheckedChange = true;
            mCheckBox.setChecked(isChecked());
            mProtectFromCheckedChange = false;
        }

        View textLayout = view.findViewById(R.id.text_layout);
        if ((textLayout != null) && textLayout instanceof LinearLayout) {
            textLayout.setOnClickListener(this);
        }

        return view;
    }

    private void init() {
        setLayoutResource(R.layout.preference_streamvolume);
    }

    public boolean isChecked() {
        return mVibratorItem != null && mVibratorItem.mSettings.isOverride();
    }

    public void setVibratorItem(ProfileConfig.VibratorItem vibratorItem) {
        mVibratorItem = vibratorItem;

        if (mCheckBox != null) {
            mCheckBox.setChecked(mVibratorItem.mSettings.isOverride());
        }
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (mProtectFromCheckedChange) {
            return;
        }

        mVibratorItem.mSettings.setOverride(isChecked);

        callChangeListener(isChecked);
    }

    protected Dialog createVibratorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        final String[] VibratorValues = getContext().getResources().getStringArray(R.array.profile_vibrator_values);

        currentChoice = mVibratorItem.mSettings.getValue();

        builder.setTitle(mVibratorItem.mLabel);
        builder.setSingleChoiceItems(R.array.profile_vibrator_entries, currentChoice,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        currentChoice = item;
                    }
                });

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (currentChoice != defaultChoice) {
                    int value = Integer.parseInt(VibratorValues[currentChoice]);
                    mVibratorItem.mSettings.setValue(value);
                    switch (value) {
                    case VibratorSettings.OFF:
                        setSummary(getContext().getString(R.string.vibrator_state_disabled));
                        break;
                    case VibratorSettings.SILENT:
                        setSummary(getContext().getString(R.string.vibrator_state_silent));
                        break;
                    default:
                        setSummary(getContext().getString(R.string.vibrator_state_enabled));
                        break;
                    }
                }
            }
        });

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
            }
        });
        return builder.create();
    }

    public ProfileConfig.VibratorItem getVibratorItem() {
        return mVibratorItem;
    }

    @Override
    public void onClick(android.view.View v) {
        if ((v != null) && (R.id.text_layout == v.getId())) {
            createVibratorDialog().show();
        }
    }
}
