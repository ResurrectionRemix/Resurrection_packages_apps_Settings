/*
 * Copyright (C) 2011 Sergey Margaritov
 * Copyright (C) 2013 Slimroms
 * Copyright (C) 2015 The TeamEos Project
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

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.preference.*;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.settings.R;

/**
 * A preference type that allows a user to choose a time
 *
 * @author Sergey Margaritov
 */
public class ColorPickerPreference extends Preference implements
        Preference.OnPreferenceClickListener, ColorPickerDialog.OnColorChangedListener {

    private static final String ANDROIDNS = "http://schemas.android.com/apk/res/android";
    private static final String SETTINGS_NS = "http://schemas.android.com/apk/res/com.android.settings";

    PreferenceViewHolder mView;
    LinearLayout mWidgetFrameView;
    ColorPickerDialog mDialog;
    private int mDefaultValue = Color.BLACK;
    private int mCurrentValue = mDefaultValue;
    private float mDensity = 0;
    private boolean mAlphaSliderEnabled = false;

    private boolean mEnabled = true;

    // if we return -6, button is not enabled
    static final int DEF_VALUE_DEFAULT = -6;
    boolean mUsesDefaultButton = false;
    int mDefValue = -1;

    private boolean mIsLedColorPicker;
    private boolean mShowLedPreview;
    private boolean mShowReset;
    private boolean mShowPreview;
    private EditText mEditText;

    //private boolean mIsCrappyLedDevice;

    public ColorPickerPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ColorPickerPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorPickerPreference(Context context) {
        this(context, null);
    }

    public ColorPickerPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.preference_material_settings);
        init(context, attrs);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray ta, int index) {
        int defaultValue = ta.getInt(index, Color.BLACK);
        return defaultValue;
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        // when using PreferenceDataStore, restorePersistedValue is always true (see Preference class for reference)
        // so we load the persistent value with getPersistedInt if available in the data store, 
        // and use defaultValue as fallback (onGetDefaultValue has been already called and it loaded the android:defaultValue attr from our xml).
        if (defaultValue == null) {
            // if we forgot to add android:defaultValue, default to black color
            defaultValue = Color.BLACK;
        }
        mCurrentValue = getPersistedInt((Integer) defaultValue);
        onColorChanged(mCurrentValue);
    }

    private void init(Context context, AttributeSet attrs) {
        mDensity = getContext().getResources().getDisplayMetrics().density;
        setOnPreferenceClickListener(this);
        if (attrs != null) {
            mAlphaSliderEnabled = attrs.getAttributeBooleanValue(null, "alphaSlider", false);
            mDefaultValue = attrs.getAttributeIntValue(ANDROIDNS, "defaultValue", Color.BLACK);
            mShowReset = attrs.getAttributeBooleanValue(SETTINGS_NS, "showReset", false);
            mShowPreview = attrs.getAttributeBooleanValue(SETTINGS_NS, "showPreview", true);
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        mView = view;
        super.onBindViewHolder(view);

        view.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(null);
            }
        });
        mWidgetFrameView = ((LinearLayout) view
                .findViewById(android.R.id.widget_frame));
        mWidgetFrameView.setOrientation(LinearLayout.HORIZONTAL);
        mWidgetFrameView.setVisibility(View.VISIBLE);
        mWidgetFrameView.setMinimumWidth(0);
        mWidgetFrameView.setPadding(
                mWidgetFrameView.getPaddingLeft(),
                mWidgetFrameView.getPaddingTop(),
                (int) (mDensity * 8),
                mWidgetFrameView.getPaddingBottom()
                );
        setDefaultButton();
        setPreviewColor();
    }

    /**
     * Restore a default value, not necessarily a color
     * For example: Set default value to -1 to remove a color filter
     *
     * @author Randall Rushing aka Bigrushdog
     */
    private void setDefaultButton() {
        if (!mShowReset || mView == null || mWidgetFrameView == null)
            return;

        ImageView defView = new ImageView(getContext());
        // remove already created default button
        int count = mWidgetFrameView.getChildCount();
        if (count > 0) {
            View oldView = mWidgetFrameView.findViewWithTag("default");
            View spacer = mWidgetFrameView.findViewWithTag("spacer");
            if (oldView != null) {
                mWidgetFrameView.removeView(oldView);
            }
            if (spacer != null) {
                mWidgetFrameView.removeView(spacer);
            }
        }
        mWidgetFrameView.addView(defView);
        defView.setImageDrawable(getContext().getDrawable(R.drawable.ic_settings_backup_restore));
        defView.setTag("default");
        defView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onColorChanged(mDefaultValue);
            }
        });
        // sorcery for a linear layout ugh
        View spacer = new View(getContext());
        spacer.setTag("spacer");
        spacer.setLayoutParams(new LinearLayout.LayoutParams((int) (mDensity * 16),
                LayoutParams.MATCH_PARENT));
        mWidgetFrameView.addView(spacer);
    }

    private void setPreviewColor() {
        if (!mShowPreview || mView == null || mWidgetFrameView == null)
            return;

        ImageView iView = new ImageView(getContext());
        // remove already create preview image
        int count = mWidgetFrameView.getChildCount();
        if (count > 0) {
            View preview = mWidgetFrameView.findViewWithTag("preview");
            if (preview != null) {
                mWidgetFrameView.removeView(preview);
            }
        }
        mWidgetFrameView.addView(iView);
        final int size = (int) getContext().getResources().getDimension(R.dimen.oval_notification_size);
        final int imageColor = ((mCurrentValue & 0xF0F0F0) == 0xF0F0F0) ?
                (mCurrentValue - 0x101010) : mCurrentValue;
        iView.setImageDrawable(createOvalShape(size, 0xFF000000 + imageColor));
        iView.setTag("preview");
    }

    @Override
    public void onColorChanged(int color) {
        mCurrentValue = color;
        setPreviewColor();
        persistInt(color);
        try {
            getOnPreferenceChangeListener().onPreferenceChange(this, color);
        } catch (NullPointerException e) {
        }
        try {
            mEditText.setText(Integer.toString(color, 16));
        } catch (NullPointerException e) {
        }
    }

    public boolean onPreferenceClick(Preference preference) {
        return false;
    }

    protected void showDialog(Bundle state) {
        mDialog = new ColorPickerDialog(getContext(), mCurrentValue);
        mDialog.setOnColorChangedListener(this);
        if (mAlphaSliderEnabled) {
            mDialog.setAlphaSliderVisible(true);
        }
        if (state != null) {
            mDialog.onRestoreInstanceState(state);
        }
        mDialog.show();
        mDialog.getWindow().setSoftInputMode(
                android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    /**
     * Toggle Alpha Slider visibility (by default it's disabled)
     *
     * @param enable
     */
    public void setAlphaSliderEnabled(boolean enable) {
        mAlphaSliderEnabled = enable;
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

    public void setDefaultValue(int value) {
        mDefaultValue = value;
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

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (mDialog == null || !mDialog.isShowing()) {
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.dialogBundle = mDialog.onSaveInstanceState();
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !(state instanceof SavedState)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        showDialog(myState.dialogBundle);
    }

    private static class SavedState extends BaseSavedState {
        Bundle dialogBundle;

        public SavedState(Parcel source) {
            super(source);
            dialogBundle = source.readBundle();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeBundle(dialogBundle);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @SuppressWarnings("unused")
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    private static ShapeDrawable createOvalShape(int size, int color) {
        ShapeDrawable shape = new ShapeDrawable(new OvalShape());
        shape.setIntrinsicHeight(size);
        shape.setIntrinsicWidth(size);
        shape.getPaint().setColor(color);
        return shape;
    }
}
