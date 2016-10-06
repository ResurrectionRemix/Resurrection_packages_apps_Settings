/*
 * Copyright (C) 2016 DarkKat
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

package net.margaritov.preference.colorpicker.widget;

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

import net.margaritov.preference.colorpicker.drawable.ColorViewCircleDrawable;
import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class ColorViewButton extends LinearLayout {

	private ImageView mColorView;
	private TextView mHexView;

	private ColorViewCircleDrawable mColorPreview;

	public ColorViewButton(Context context) {
		this(context, null);
	}

	public ColorViewButton(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ColorViewButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        final Resources res = getContext().getResources();
        final int drawableSize = (int) res.getDimension(R.dimen.color_picker_button_drawable_size);

	    mColorView = (ImageView) findViewById(R.id.color_button_color);
	    mHexView = (TextView) findViewById(R.id.color_button_hex);
        mColorPreview = new ColorViewCircleDrawable(getContext(), drawableSize);

        mColorView.setImageDrawable(mColorPreview);
    }

	public void setColor(int color) {
        if (mColorView == null || mHexView == null || mColorPreview == null) {
            return;
        }
        mColorPreview.setColor(color);
        mHexView.setText(ColorPickerPreference.convertToARGB(color));
	}

	public int getColor() {
        if (mColorPreview == null) {
            return Color.BLACK;
        } else {
		    return mColorPreview.getColor();
        }
	}

	public void setBorderColor(int color) {
        if (mColorPreview == null) {
            return;
        }
        mColorPreview.setBorderColor(color);
	}

	public int getBorderColor() {
        if (mColorPreview == null) {
            return 0xff6E6E6E;
        } else {
		    return mColorPreview.getBorderColor();
        }
	}

    public void setShowFavoriteIcon(boolean show) {
        if (mColorView == null || mHexView == null || mColorPreview == null) {
            return;
        }
        mColorPreview.setShowFavoriteIcon(show);
        if (show) {
            mHexView.setText(getContext().getResources().getString(R.string.empty_title));
        }
    }
}
