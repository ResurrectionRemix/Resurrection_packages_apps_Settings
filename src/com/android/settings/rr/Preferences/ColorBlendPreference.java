/*
 * Copyright (C) 2018 Android Ice Cold Project
 * Author: SpiritCroc <dev@spiritcroc.de>
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

package com.android.settings.rr.Preferences;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceDataStore;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.settings.rr.utils.RRUtils;

import net.margaritov.preference.colorpicker.ColorPickerDialog;

import com.android.settings.R;

public class ColorBlendPreference extends Preference
        implements Preference.OnPreferenceClickListener {

    private static final String TAG = "ColorBlendPreference";

    private String mColorStartKey;
    private String mColorEndKey;
    private String mBlendReverseKey;

    private String mPreviewStartText;
    private String mPreviewEndText;
    private String mPreviewBetweenText;

    private int mDefaultColorStart = 0xffff0000;
    private int mDefaultColorEnd = 0xff00ff00;
    private boolean mDefaultBlendReverse = false;

    private int mColorStart = mDefaultColorStart;
    private int mColorEnd = mDefaultColorEnd;
    private boolean mBlendReverse = mDefaultBlendReverse;

    // Preview values for the dialog before persisting settings
    private int mPreviewColorStart;
    private int mPreviewColorEnd;
    private boolean mPreviewBlendReverse;

    // Preview views
    private View mViewColorStart;
    private View mViewColorEnd;
    private View mViewColorBetween;

    private AlertDialog mDialog;

    // Dialog views
    private View mDialogPreviewColorStart;
    private View mDialogPreviewColorEnd;
    private View mDialogPreviewColorBetween;
    private SeekBar mDialogColorPreviewSlider;
    private TextView mDialogColorPreviewText;
    private TextView mDialogColorPreviewStartText;
    private TextView mDialogColorPreviewEndText;
    private TextView mDialogColorPreviewBetweenText;

    public ColorBlendPreference(Context context, AttributeSet attrs,
                               int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    public ColorBlendPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public ColorBlendPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ColorBlendPreference(Context context) {
        super(context);
        init(context, null);
    }

    private void init(Context context, AttributeSet attrs) {
        setOnPreferenceClickListener(this);
        if (attrs != null) {
            final TypedArray a = context.obtainStyledAttributes(attrs,
                    R.styleable.ColorBlendPreference);
            mDefaultColorStart =
                    a.getColor(R.styleable.ColorBlendPreference_defaultValueColorStart,
                            mDefaultColorStart);
            mDefaultColorEnd =
                    a.getColor(R.styleable.ColorBlendPreference_defaultValueColorEnd,
                            mDefaultColorEnd);
            mDefaultBlendReverse =
                    a.getBoolean(R.styleable.ColorBlendPreference_defaultValueBlendReverse,
                            mDefaultBlendReverse);
            mColorStartKey = a.getString(R.styleable.ColorBlendPreference_keyColorStart);
            mColorEndKey = a.getString(R.styleable.ColorBlendPreference_keyColorEnd);
            mBlendReverseKey = a.getString(R.styleable.ColorBlendPreference_keyBlendReverse);

            mPreviewStartText = a.getString(R.styleable.ColorBlendPreference_previewStartText);
            if (mPreviewStartText == null) {
                mPreviewStartText = getContext().getString(R.string.color_preview_start);
            }

            mPreviewEndText = a.getString(R.styleable.ColorBlendPreference_previewEndText);
            if (mPreviewEndText == null) {
                mPreviewEndText = getContext().getString(R.string.color_preview_end);
            }

            mPreviewBetweenText = a.getString(R.styleable.ColorBlendPreference_previewBetweenText);
            if (mPreviewBetweenText == null) {
                mPreviewBetweenText = getContext().getString(R.string.color_preview_between);
            }

            a.recycle();
            loadPreferences();
        }
        setWidgetLayoutResource(R.layout.color_blend_preview);
    }

    @Override
    public void setPreferenceDataStore(PreferenceDataStore dataStore) {
        super.setPreferenceDataStore(dataStore);
        loadPreferences();
    }

    private ColorPickerDialog.OnColorChangedListener mStartColorChangedListener =
            new ColorPickerDialog.OnColorChangedListener() {
                @Override
                public void onColorChanged(int color) {
                    mPreviewColorStart = color;
                    updateDialogPreview();
                }
            };
    private ColorPickerDialog.OnColorChangedListener mEndColorChangedListener =
            new ColorPickerDialog.OnColorChangedListener() {
                @Override
                public void onColorChanged(int color) {
                    mPreviewColorEnd = color;
                    updateDialogPreview();
                }
            };

    private void loadPreferences() {
        PreferenceDataStore preferenceStore = getPreferenceDataStore();
        if (preferenceStore == null) {
            Log.i(TAG, "No preference data store available, not using persisted values");
            mColorStart = mDefaultColorStart;
            mColorEnd = mDefaultColorEnd;
            mBlendReverse = mDefaultBlendReverse;
        } else {
            if (mColorStartKey != null) {
                mColorStart = preferenceStore.getInt(mColorStartKey, mDefaultColorStart);
            }
            if (mColorEndKey != null) {
                mColorEnd = preferenceStore.getInt(mColorEndKey, mDefaultColorEnd);
            }
            if (mBlendReverseKey != null) {
                mBlendReverse = preferenceStore.getBoolean(mBlendReverseKey, mDefaultBlendReverse);
            }
        }
        mPreviewColorStart = mColorStart;
        mPreviewColorEnd = mColorEnd;
        mPreviewBlendReverse = mBlendReverse;
    }

    private void setValues(int colorStart, int colorEnd, boolean blendReverse) {
        mColorStart = colorStart;
        mColorEnd = colorEnd;
        mBlendReverse = blendReverse;
        mPreviewColorStart = mColorStart;
        mPreviewColorEnd = mColorEnd;
        mPreviewBlendReverse = mBlendReverse;
        PreferenceDataStore preferenceStore = getPreferenceDataStore();
        if (preferenceStore != null) {
            preferenceStore.putInt(mColorStartKey, mColorStart);
            preferenceStore.putInt(mColorEndKey, mColorEnd);
            preferenceStore.putBoolean(mBlendReverseKey, mBlendReverse);
        } else {
            Log.i(TAG, "No preference data store available, not persisting values");
        }
        updatePreview();
    }

    private void persistValues() {
        if (mDialog == null) {
            Log.e(TAG, "persistValues() called without open dialog");
            return;
        }
        setValues(mPreviewColorStart, mPreviewColorEnd, mPreviewBlendReverse);
        Log.d(TAG, "Persisting values");
        PreferenceDataStore preferenceStore = getPreferenceDataStore();
        if (preferenceStore == null) {
            return;
        }
        if (mColorStartKey == null) {
            Log.e(TAG, "Missing color start key");
        } else {
            preferenceStore.putInt(mColorStartKey, mPreviewColorStart);
        }
        if (mColorEndKey == null) {
            Log.e(TAG, "Missing color end key");
        } else {
            preferenceStore.putInt(mColorEndKey, mPreviewColorEnd);
        }
        if (mBlendReverseKey == null) {
            Log.e(TAG, "Missing blend reverse key");
        } else {
            preferenceStore.putBoolean(mBlendReverseKey, mPreviewBlendReverse);
        }
        // Reload persisted values
        loadPreferences();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        showDialog();
        return true;
    }

    private void showDialog() {
        mPreviewColorStart = mColorStart;
        mPreviewColorEnd = mColorEnd;
        mPreviewBlendReverse = mBlendReverse;
        mDialog = new AlertDialog.Builder(getContext())
                .setTitle(getTitle())
                .setView(R.layout.color_blend_preference_dialog)
                .setPositiveButton(R.string.color_blend_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        persistValues();
                        mDialog = null;
                    }
                })
                .setNegativeButton(R.string.color_blend_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Only close dialog
                        mDialog = null;
                    }
                })
                .setNeutralButton(R.string.color_blend_reset, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Restore default settings
                        setValues(mDefaultColorStart, mDefaultColorEnd, mDefaultBlendReverse);
                        persistValues();
                        mDialog = null;
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        mDialog = null;
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        mDialog = null;
                    }
                })
                .show();
        mDialogPreviewColorStart = mDialog.findViewById(R.id.color_start);
        mDialogPreviewColorEnd = mDialog.findViewById(R.id.color_end);
        mDialogPreviewColorBetween = mDialog.findViewById(R.id.color_between);
        mDialogColorPreviewSlider = mDialog.findViewById(R.id.color_preview_slider);
        mDialogColorPreviewText = mDialog.findViewById(R.id.color_preview_text);
        mDialogColorPreviewStartText = mDialog.findViewById(R.id.color_preview_start_text);
        mDialogColorPreviewEndText = mDialog.findViewById(R.id.color_preview_end_text);
        mDialogColorPreviewBetweenText = mDialog.findViewById(R.id.color_preview_between_text);
        mDialogPreviewColorStart.setOnClickListener(mDialogPreviewClickListener);
        mDialogPreviewColorEnd.setOnClickListener(mDialogPreviewClickListener);
        mDialogPreviewColorBetween.setOnClickListener(mDialogPreviewClickListener);
        mDialogColorPreviewText.setOnClickListener(mDialogPreviewClickListener);
        mDialogColorPreviewSlider.setOnSeekBarChangeListener(mDialogPreviewSliderChangeListener);
        mDialogColorPreviewStartText.setText(mPreviewStartText);
        mDialogColorPreviewEndText.setText(mPreviewEndText);
        mDialogColorPreviewBetweenText.setText(mPreviewBetweenText);
        updateDialogPreview();
    }

    private void updateDialogPreview() {
        if (mDialog == null) {
            Log.e(TAG, "updateDialogPreview() called without open dialog");
            return;
        }
        mDialogPreviewColorStart.setBackgroundColor(mPreviewColorStart);
        mDialogPreviewColorEnd.setBackgroundColor(mPreviewColorEnd);
        updateDialogSliderPreview();
    }

    private void updateDialogSliderPreview() {
        int currentPreview = mDialogColorPreviewSlider.getProgress();
        mDialogPreviewColorBetween.setBackgroundColor(
                RRUtils.getBlendColorForPercent(mPreviewColorEnd, mPreviewColorStart,
                        mPreviewBlendReverse, currentPreview));
        mDialogColorPreviewText.setText(
                getContext().getString(R.string.color_blend_preview, currentPreview));
    }

    private View.OnClickListener mDialogPreviewClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == mDialogPreviewColorStart) {
                ColorPickerDialog pickerDialog =
                        new ColorPickerDialog(getContext(), mPreviewColorStart);
                pickerDialog.setOnColorChangedListener(mStartColorChangedListener);
                pickerDialog.show();
            } else if (v == mDialogPreviewColorEnd) {
                ColorPickerDialog pickerDialog =
                        new ColorPickerDialog(getContext(), mPreviewColorEnd);
                pickerDialog.setOnColorChangedListener(mEndColorChangedListener);
                pickerDialog.show();
            } else if (v == mDialogPreviewColorBetween) {
                mPreviewBlendReverse = !mPreviewBlendReverse;
                int currentPreview = mDialogColorPreviewSlider.getProgress();
                if (currentPreview < 25 || currentPreview > 75) {
                    // Reset preview slider so change becomes apparent
                    mDialogColorPreviewSlider.setProgress(50);
                }
                updateDialogSliderPreview();
            } else if (v == mDialogColorPreviewText) {
                // Reset preview slider
                mDialogColorPreviewSlider.setProgress(50);
                updateDialogSliderPreview();
            } else {
                Log.e(TAG, "DialogPreviewClickListener not prepared for " + v);
            }
        }
    };

    private SeekBar.OnSeekBarChangeListener mDialogPreviewSliderChangeListener =
            new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    updateDialogSliderPreview();
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            };

    private void updatePreview() {
        if (mViewColorStart != null) {
            mViewColorStart.setBackgroundColor(mColorStart);
        }
        if (mViewColorEnd != null) {
            mViewColorEnd.setBackgroundColor(mColorEnd);
        }
        if (mViewColorBetween != null) {
            mViewColorBetween.setBackgroundColor(
                    RRUtils.getBlendColorForPercent(mColorEnd, mColorStart, mBlendReverse, 50));
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mViewColorStart = holder.findViewById(R.id.color_preview_start);
        mViewColorEnd = holder.findViewById(R.id.color_preview_end);
        mViewColorBetween = holder.findViewById(R.id.color_preview_between);
        updatePreview();
    }

}
