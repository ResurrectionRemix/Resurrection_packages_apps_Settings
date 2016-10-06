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
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.view.View;

import com.android.settings.R;

import net.margaritov.preference.colorpicker.drawable.ColorViewCircleDrawable;

public class ApplyColorView extends FrameLayout {

	private ImageView mColorView;
	private ImageView mColorSet;

	private ColorViewCircleDrawable mColorPreview;

	private int mBorderColor;

	public ApplyColorView(Context context) {
		this(context, null);
	}

	public ApplyColorView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ApplyColorView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        final Resources res = getContext().getResources();
        final int drawableSize = (int) res.getDimension(R.dimen.color_picker_button_drawable_size);
        mBorderColor = res.getColor(R.color.color_picker_color_view_drawable_border);

	    mColorView = (ImageView) findViewById(R.id.apply_color_action_color);
	    mColorSet = (ImageView) findViewById(R.id.apply_color_action_set);
        mColorPreview = new ColorViewCircleDrawable(getContext(), drawableSize);

        mColorPreview.setBorderColor(mBorderColor);
        mColorView.setImageDrawable(mColorPreview);
    }

	public void setColor(int color) {
        if (mColorPreview == null) {
            return;
        }
        mColorPreview.setColor(color);
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
		mBorderColor = color;
        mColorPreview.setBorderColor(mBorderColor);
	}

	public int getBorderColor() {
		return mBorderColor;
	}

    public void showSetIcon(boolean show) {
        if (mColorSet == null) {
            return;
        }
        mColorSet.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
    }

    public void applySetIconAlpha(float alpha) {
        if (mColorSet == null) {
            return;
        }
        mColorSet.setAlpha(alpha);
    }

    public void setColorPreviewTranslationX(float x) {
        if (mColorSet == null) {
            return;
        }
        mColorView.setTranslationX(x);
    }
}
