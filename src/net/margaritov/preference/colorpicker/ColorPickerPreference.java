/*
 * Copyright (C) 2011 Sergey Margaritov
 * Copyright (C) 2013 Slimroms
 * Copyright (C) 2015 DarkKat
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

package net.margaritov.preference.colorpicker;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.android.settings.R;

/**
 * A preference type that allows a user to choose a color
 * 
 * @author Sergey Margaritov
 */
public class ColorPickerPreference extends DialogPreference implements
        ColorPickerDialog.OnColorChangedListener {
    public static final String TAG = "ColorPickerPreference";

    private static final String sAndroidns = "http://schemas.android.com/apk/res/android";

    private View mView;
    private View mPreview;

    private final Resources mResources;
    private final float mDensity;
    private int mDefaultValue = Color.BLACK;
    private int mResetColor1 = Color.TRANSPARENT;
    private int mResetColor2 = Color.TRANSPARENT;
    private String mResetColor1Title = null;
    private String mResetColor2Title = null;
    private int mValue;
    private boolean mAlphaSliderVisible = false;

    public ColorPickerPreference(Context context) {
        this(context, null);
    }

    public ColorPickerPreference(Context context, AttributeSet attrs) {
        this(context, attrs, com.android.internal.R.attr.dialogPreferenceStyle);
    }

    public ColorPickerPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ColorPickerPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        mResources = context.getResources();
        mDensity = mResources.getDisplayMetrics().density;

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(
                    attrs, R.styleable.ColorPickerPreference, defStyleAttr, defStyleRes);
            mDefaultValue = a.getColor(R.styleable.ColorPickerPreference_defaultColor,
                    Color.TRANSPARENT);
            mResetColor1 = a.getColor(R.styleable.ColorPickerPreference_resetColor1,
                    Color.TRANSPARENT);
            mResetColor2 = a.getColor(R.styleable.ColorPickerPreference_resetColor2,
                    Color.TRANSPARENT);
            mResetColor1Title = a.getString(R.styleable.ColorPickerPreference_resetColor1Title);
            mResetColor2Title = a.getString(R.styleable.ColorPickerPreference_resetColor2Title);
            mAlphaSliderVisible = a.getBoolean(
                    R.styleable.ColorPickerPreference_alphaSliderVisible, false);
            a.recycle();

            if (mDefaultValue == Color.TRANSPARENT) {
                String defaultValue = attrs.getAttributeValue(sAndroidns, "defaultValue");
                if (defaultValue != null) {
                    if (defaultValue.startsWith("#")) {
                        try {
                            mDefaultValue = convertToColorInt(defaultValue);
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Wrong color: " + defaultValue);
                        }
                    } else {
                        int resourceId = attrs.getAttributeResourceValue(sAndroidns, "defaultValue", 0);
                        if (resourceId != 0) {
                            mDefaultValue = mResources.getInteger(resourceId);
                        }
                    }
                }
            }
            if (mDefaultValue == Color.TRANSPARENT) {
                mDefaultValue = Color.BLACK;
            }

            mValue = mDefaultValue;
        }
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        onColorChanged(restoreValue ? getValue() : (Integer) defaultValue);
    }

    @Override
    protected void onBindView(View view) {
        mView = view;
        super.onBindView(view);

        setPreview();
    }

    private void setPreview() {
        if (mView == null)
            return;

        LinearLayout widgetFrameView = ((LinearLayout) mView
                .findViewById(android.R.id.widget_frame));
        if (widgetFrameView == null) {
            return;
        }

        widgetFrameView.setVisibility(View.VISIBLE);
        widgetFrameView.setPadding(
                widgetFrameView.getPaddingLeft(),
                widgetFrameView.getPaddingTop(),
                (int) (mDensity * 8),
                widgetFrameView.getPaddingBottom()
                );

        final int size = (int) mResources.getDimension(
                R.dimen.color_picker_preference_preview_width_height);

        mPreview = new View(getContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        mPreview.setLayoutParams(lp);
        mPreview.setBackground(new ColorViewCircleDrawable(getContext(), size));
        ((ColorViewCircleDrawable) mPreview.getBackground()).setColor(getValue());

        widgetFrameView.addView(mPreview);
        widgetFrameView.setMinimumWidth(0);

    }

    private int getValue() {
        try {
            if (isPersistent()) {
                mValue = getPersistedInt(mDefaultValue);
            }
        } catch (ClassCastException e) {
            mValue = mDefaultValue;
        }

        return mValue;
    }

    @Override
    public void onColorChanged(int color) {
        if (isPersistent()) {
            persistInt(color);
        }
        mValue = color;
        if (mPreview != null) {
            ((ColorViewCircleDrawable) mPreview.getBackground()).setColor(color);
        }
        try {
            getOnPreferenceChangeListener().onPreferenceChange(this, color);
        } catch (NullPointerException e) {
        }
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        final ColorPickerDialog pickerDialog = (ColorPickerDialog) getDialog();
    }

    @Override
    protected Dialog createDialog() {
        final ColorPickerDialog pickerDialog = new ColorPickerDialog(
                getContext(), R.style.Theme_ColorPickerDialog);

        if (mResetColor1 == Color.TRANSPARENT) {
            if (mResetColor2 != Color.TRANSPARENT) {
                mResetColor2 = Color.TRANSPARENT;
                Log.w(TAG + ".createDialog()",
                        "Reset color 1 has not been set, ignore reset color 2 value");
            }
            if (mResetColor1Title != null) {
                mResetColor1Title = null;
                Log.w(TAG + ".createDialog()",
                        "Reset color 1 has not been set, ignore reset color 1 title");
            }
            if (mResetColor2Title != null) {
                mResetColor2Title = null;
                Log.w(TAG + ".createDialog()",
                        "Reset color 1 has not been set, ignore reset color 2 title");
            }
        } else if (mResetColor2 == Color.TRANSPARENT) {
            if (mResetColor2Title != null) {
                mResetColor2Title = null;
                Log.w(TAG + ".createDialog()",
                        "Reset color 2 has not been set, ignore reset color 2 title");
            }
        }

        pickerDialog.setUp(getValue(), mResetColor1, mResetColor2, mResetColor1Title,
                mResetColor2Title, mAlphaSliderVisible);
        pickerDialog.setOnColorChangedListener(this);

        return pickerDialog;
    }

    public void setDefaultColors(int androidColor, int darkKatColor) {
        setResetColors(androidColor, darkKatColor);
    }

    public void setResetColors(int resetColor1, int resetColor2) {
        mResetColor1 = resetColor1;
        mResetColor2 = resetColor2;
    }

    public void setResetColor(int color) {
        mResetColor1 = color;
    }

    public void setResetColorsTitle(int title1ResId, int title2ResId) {
        mResetColor1Title = mResources.getString(title1ResId);
        mResetColor2Title = mResources.getString(title2ResId);
    }

    public void setResetColorsTitle(String title1, String title2) {
        mResetColor1Title = title1;
        mResetColor2Title = title2;
    }

    public void setResetColorTitle(int titleResId) {
        mResetColor1Title = mResources.getString(titleResId);
    }

    public void setResetColorTitle(String title) {
        mResetColor1Title = title;
    }

    /**
     * Toggle Alpha Slider visibility (by default it's disabled)
     * 
     * @param enable
     */
    public void setAlphaSliderVisible(boolean visible) {
        mAlphaSliderVisible = visible;
    }

    /**
     * Toggle Alpha Slider visibility (by default it's disabled)
     * 
     * @param enable
     */
    public void setAlphaSliderEnabled(boolean enabled) {
        setAlphaSliderVisible(enabled);
    }

    /**
     * For custom purposes. Not used by ColorPickerPreferrence
     *
     * set color preview value from outside
     * @author kufikugel
     */
    public void setNewPreviewColor(int color) {
        onColorChanged(color);
    }

    /**
     * For custom purposes. Not used by ColorPickerPreferrence
     * 
     * @param color
     * @author Unknown
     */
    public static String convertToARGB(int color) {
        String alpha = Integer.toHexString(Color.alpha(color));
        String red = Integer.toHexString(Color.red(color));
        String green = Integer.toHexString(Color.green(color));
        String blue = Integer.toHexString(Color.blue(color));

        if (alpha.length() == 1) {
            alpha = "0" + alpha;
        }

        if (red.length() == 1) {
            red = "0" + red;
        }

        if (green.length() == 1) {
            green = "0" + green;
        }

        if (blue.length() == 1) {
            blue = "0" + blue;
        }

        return "#" + alpha + red + green + blue;
    }

    /**
     * Converts a aarrggbb- or rrggbb color string to a color int
     * 
     * @param argb
     * @throws NumberFormatException
     * @author Unknown
     */
    public static int convertToColorInt(String argb) throws NumberFormatException {

        if (argb.startsWith("#")) {
            argb = argb.replace("#", "");
        }

        int alpha = -1, red = -1, green = -1, blue = -1;

        if (argb.length() == 8) {
            alpha = Integer.parseInt(argb.substring(0, 2), 16);
            red = Integer.parseInt(argb.substring(2, 4), 16);
            green = Integer.parseInt(argb.substring(4, 6), 16);
            blue = Integer.parseInt(argb.substring(6, 8), 16);
        }
        else if (argb.length() == 6) {
            alpha = 255;
            red = Integer.parseInt(argb.substring(0, 2), 16);
            green = Integer.parseInt(argb.substring(2, 4), 16);
            blue = Integer.parseInt(argb.substring(4, 6), 16);
        }

        return Color.argb(alpha, red, green, blue);
    }
}


