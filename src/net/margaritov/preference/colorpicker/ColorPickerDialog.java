/*
 * Copyright (C) 2010 Daniel Nilsson
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
import android.graphics.PixelFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.android.settings.R;

public class ColorPickerDialog
        extends
        Dialog
        implements
        ColorPickerView.OnColorChangedListener,
        View.OnClickListener {

    private ColorPickerView mColorPicker;

    private ColorPickerPanelView mOldColor;
    private ColorPickerPanelView mNewColor;

    private EditText mHex;
    private Button mSetButton;
    private Button mIcsColor;

    private OnColorChangedListener mListener;

    public interface OnColorChangedListener {
        public void onColorChanged(int color);
    }

    public ColorPickerDialog(Context context, int initialColor) {
        super(context);

        init(initialColor);
    }

    private void init(int color) {
        // To fight color branding.
        getWindow().setFormat(PixelFormat.RGBA_8888);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setUp(color);

    }

    private void setUp(int color) {

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        View layout = inflater.inflate(R.layout.dialog_color_picker, null);

        setContentView(layout);

        setTitle(R.string.dialog_color_picker);

        mColorPicker = (ColorPickerView) layout.findViewById(R.id.color_picker_view);
        mOldColor = (ColorPickerPanelView) layout.findViewById(R.id.old_color_panel);
        mNewColor = (ColorPickerPanelView) layout.findViewById(R.id.new_color_panel);
        mHex = (EditText) layout.findViewById(R.id.hex);
        mSetButton = (Button) layout.findViewById(R.id.enter);
        mIcsColor = (Button) layout.findViewById(R.id.ics_color);

        ((LinearLayout) mOldColor.getParent()).setPadding(
                Math.round(mColorPicker.getDrawingOffset()),
                0,
                Math.round(mColorPicker.getDrawingOffset()),
                0
                );

        mOldColor.setOnClickListener(this);
        mNewColor.setOnClickListener(this);
        mColorPicker.setOnColorChangedListener(this);
        mOldColor.setColor(color);
        mColorPicker.setColor(color, true);
        mHex.setText(ColorPickerPreference.convertToARGB(color));
        mSetButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String text = mHex.getText().toString();
                try {
                    int newColor = ColorPickerPreference.convertToColorInt(text);
                    mColorPicker.setColor(newColor, true);
                } catch (Exception e) {
                }
            }
        });
        mIcsColor.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    int newColor = 0xFF33B5E5;
                    mColorPicker.setColor(newColor, true);
                } catch (Exception e) {
                }
            }
        });

    }

    @Override
    public void onColorChanged(int color) {

        mNewColor.setColor(color);
        try {
            mHex.setText(ColorPickerPreference.convertToARGB(color));
        } catch (Exception e) {

        }
        /*
         * if (mListener != null) { mListener.onColorChanged(color); }
         */

    }

    public void setAlphaSliderVisible(boolean visible) {
        mColorPicker.setAlphaSliderVisible(visible);
    }

    /**
     * Set a OnColorChangedListener to get notified when the color selected by the user has changed.
     *
     * @param listener
     */
    public void setOnColorChangedListener(OnColorChangedListener listener) {
        mListener = listener;
    }

    public int getColor() {
        return mColorPicker.getColor();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.new_color_panel) {
            if (mListener != null) {
                mListener.onColorChanged(mNewColor.getColor());
            }
        }
        dismiss();
    }

}
