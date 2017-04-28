/*
 * Copyright (C) 2014 The CyanogenMod Project
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

package com.android.settings;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

public class AnimationScalePreference extends CustomDialogPreference
    implements SeekBar.OnSeekBarChangeListener {

    private TextView mScaleText;
    private IntervalSeekBar mSeekBar;

    private float mScale = 1.0f;

    public AnimationScalePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);

        setDialogLayoutResource(R.layout.preference_dialog_animation_scale);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mScaleText = (TextView) view.findViewById(R.id.scale);
        mScaleText.setText(String.valueOf(mScale) + "x");

        mSeekBar = (IntervalSeekBar) view.findViewById(R.id.scale_seekbar);
        mSeekBar.setProgressFloat(mScale);
        mSeekBar.setOnSeekBarChangeListener(this);
    }

    public void setScale(float scale) {
        mScale = scale;
        setSummary(String.valueOf(scale) + "x");
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            callChangeListener(mSeekBar.getProgressFloat());
        }
    }

    @Override
    protected void onClick() {
        // Ignore this until an explicit call to click()
    }

    public void click() {
        super.onClick();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        mScaleText.setText(String.valueOf(mSeekBar.getProgressFloat()) + "x");
    }

    // Not used
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}
