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
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.settings.R;

/**
 * A preference type that allows a user to choose a color
 * 
 * @author Sergey Margaritov
 */
public class ColorPickerPreference extends DialogPreference implements
        ColorPickerDialog.OnColorChangedListener {

    View mView;
    LinearLayout widgetFrameView;
    ColorPickerDialog mDialog;

    int mDefaultValue = Color.BLACK;
    int mAndroidColor = 0x00000000;
    int mDarkKatColor = mAndroidColor;
    private int mValue = Color.BLACK;
    private float mDensity = 0;
    private boolean mAlphaSliderEnabled = false;

    private static final String androidns = "http://schemas.android.com/apk/res/android";

    private EditText mEditText;

    public ColorPickerPreference(Context context) {
        super(context);
        init(context, null);
    }

    public ColorPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ColorPickerPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        onColorChanged(restoreValue ? getValue() : (Integer) defaultValue);
    }

    private void init(Context context, AttributeSet attrs) {
        mDensity = getContext().getResources().getDisplayMetrics().density;
        if (attrs != null) {
            String defaultValue = attrs.getAttributeValue(androidns, "defaultValue");
            if (defaultValue.startsWith("#")) {
                try {
                    mDefaultValue = convertToColorInt(defaultValue);
                } catch (NumberFormatException e) {
                    Log.e("ColorPickerPreference", "Wrong color: " + defaultValue);
                    mDefaultValue = convertToColorInt("#FF000000");
                }
            } else {
                int resourceId = attrs.getAttributeResourceValue(androidns, "defaultValue", 0);
                if (resourceId != 0) {
                    mDefaultValue = context.getResources().getInteger(resourceId);
                }
            }
            mAlphaSliderEnabled = attrs.getAttributeBooleanValue(null, "alphaSlider", false);
        }
        mValue = mDefaultValue;
    }

    @Override
    protected void onBindView(View view) {
        mView = view;
        super.onBindView(view);

        widgetFrameView = ((LinearLayout) view
                .findViewById(android.R.id.widget_frame));

        setPreviewColor();
    }

    private void setPreviewColor() {
        if (mView == null)
            return;

        ImageView iView = new ImageView(getContext());
        LinearLayout widgetFrameView = ((LinearLayout) mView
                .findViewById(android.R.id.widget_frame));
        if (widgetFrameView == null)
            return;

        widgetFrameView.setVisibility(View.VISIBLE);
        widgetFrameView.setPadding(
                widgetFrameView.getPaddingLeft(),
                widgetFrameView.getPaddingTop(),
                (int) (mDensity * 8),
                widgetFrameView.getPaddingBottom()
                );
        // remove already create preview image
        int count = widgetFrameView.getChildCount();
        if (count > 0) {
            widgetFrameView.removeViews(0, count);
        }
        widgetFrameView.addView(iView);
        widgetFrameView.setMinimumWidth(0);
        iView.setBackgroundDrawable(new AlphaPatternDrawable((int) (5 * mDensity)));
        iView.setImageBitmap(getPreviewBitmap());
    }

    private Bitmap getPreviewBitmap() {
        int d = (int) (mDensity * 31); // 30dip
        int color = getValue();
        Bitmap bm = Bitmap.createBitmap(d, d, Config.ARGB_8888);
        int w = bm.getWidth();
        int h = bm.getHeight();
        int c = color;
        for (int i = 0; i < w; i++) {
            for (int j = i; j < h; j++) {
                c = (i <= 1 || j <= 1 || i >= w - 2 || j >= h - 2) ? Color.GRAY : color;
                bm.setPixel(i, j, c);
                if (i != j) {
                    bm.setPixel(j, i, c);
                }
            }
        }

        return bm;
    }

    public int getValue() {
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
        setPreviewColor();
        try {
            getOnPreferenceChangeListener().onPreferenceChange(this, color);
        } catch (NullPointerException e) {
        }
        try {
            mEditText.setText(Integer.toString(color, 16));
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
                getContext(), R.style.Theme_ColorPickerDialog,
                getValue(), mAndroidColor, mDarkKatColor);

        if (mAlphaSliderEnabled) {
            pickerDialog.setAlphaSliderVisible(true);
        }
        pickerDialog.setOnColorChangedListener(this);

        return pickerDialog;
    }

    /**
     * Toggle Alpha Slider visibility (by default it's disabled)
     * 
     * @param enable
     */
    public void setAlphaSliderEnabled(boolean enable) {
        mAlphaSliderEnabled = enable;
    }

    public void setDefaultColors(int androidColor, int darkkatColor) {
        mAndroidColor = androidColor;
        mDarkKatColor = darkkatColor;
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
     * For custom purposes. Not used by ColorPickerPreferrence
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

