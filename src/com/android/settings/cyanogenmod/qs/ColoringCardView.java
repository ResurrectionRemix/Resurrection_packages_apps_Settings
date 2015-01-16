/*
 * Copyright (C) 2015 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.cyanogenmod.qs;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;

public class ColoringCardView extends CardView {
    private PorterDuffColorFilter mColorFilter;
    private Paint mPaint;

    public ColoringCardView(Context context) {
        super(context);
        init();
    }

    public ColoringCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ColoringCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setColor(int color) {
        if (color != mColorFilter.getColor()) {
            mColorFilter.setColor(color);
            mPaint.setColorFilter(color == Color.TRANSPARENT ? null : mColorFilter);
            setLayerPaint(mPaint);
        }
    }

    private void init() {
        mPaint = new Paint();
        mColorFilter = new PorterDuffColorFilter(Color.TRANSPARENT, PorterDuff.Mode.MULTIPLY);
        setLayerType(LAYER_TYPE_HARDWARE, mPaint);
    }
}

