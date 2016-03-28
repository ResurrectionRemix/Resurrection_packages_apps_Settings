/*
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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.LayerDrawable;
import android.graphics.PorterDuff.Mode;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.util.AttributeSet;

import com.android.settings.R;

public class ColorPickerColorButton extends LinearLayout {

	private ImageView mColorView;
	private TextView mHexView;

	private int mBorderColor = 0xff6E6E6E;
	private int mColor = Color.WHITE;

	public ColorPickerColorButton(Context context) {
		this(context, null);
	}

	public ColorPickerColorButton(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ColorPickerColorButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

	    mColorView = (ImageView) findViewById(R.id.color_button_color);
	    mHexView = (TextView) findViewById(R.id.color_button_hex);
    }

	public void setColor(int color) {
		mColor = color;
        if (mColorView == null || mHexView == null) {
            return;
        }
        if (mColorView.getDrawable() != null
                && mColorView.getDrawable() instanceof LayerDrawable) {
            ((LayerDrawable) mColorView.getDrawable()).findDrawableByLayerId(R.id.color_fill)
                    .setColorFilter(mColor, Mode.MULTIPLY);
        }
        mHexView.setText(ColorPickerPreference.convertToARGB(mColor));

	}

	public int getColor() {
		return mColor;
	}

	public void setBorderColor(int color) {
		mBorderColor = color;
        if (mColorView == null) {
            return;
        }
        if (mColorView.getDrawable() != null
                && mColorView.getDrawable() instanceof LayerDrawable) {
            ((LayerDrawable) mColorView.getDrawable()).findDrawableByLayerId(R.id.boarder)
                    .setColorFilter(mBorderColor, Mode.MULTIPLY);
        }
	}

	public int getBorderColor() {
		return mBorderColor;
	}

    public void setImageResource(int resId) {
        if (mColorView == null) {
            return;
        }

        if (resId > 0) {
            mColorView.setImageResource(resId);
        }
    }

}
