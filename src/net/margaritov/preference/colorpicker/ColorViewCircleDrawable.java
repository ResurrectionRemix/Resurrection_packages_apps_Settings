/*
 * Copyright (C) 2013 The Android Open Source Project
 *
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
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import com.android.internal.util.darkkat.ImageHelper;

import com.android.settings.R;

public class ColorViewCircleDrawable extends Drawable {
	private static final int sNumRectangles = 6;

    private final int mSize;
    private final float mBorderWidth;

    private int mColor;
    private int mBorderColor;

    private Paint mPaint;
	private Paint mPaintWhite;
	private Paint mPaintGray;
	private Paint mBorderPaint;

    private Bitmap mPatternBitmap;
    private Bitmap mBitmap;
    private Bitmap mFavoriteBitmap;

    private boolean mShowFavoriteIcon;

    public ColorViewCircleDrawable(Context context, int size) {
        super();

        Resources res = context.getResources();

        mSize = size;
		mBorderWidth = res.getDimension(R.dimen.color_picker_color_view_drawable_boarder_width);

        mColor = Color.BLACK;
        mBorderColor = res.getColor(R.color.color_picker_color_view_drawable_boarder);

		mPaintWhite = new Paint();
		mPaintWhite.setAntiAlias(true);
		mPaintWhite.setColor(Color.WHITE);

		mPaintGray = new Paint();
		mPaintGray.setAntiAlias(true);
		mPaintGray.setColor(Color.LTGRAY);

		mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.BLACK);
        mPaint.setStyle(Paint.Style.FILL);

		mBorderPaint = new Paint();
		mBorderPaint.setAntiAlias(true);
		mBorderPaint.setColor(mBorderColor);
		mBorderPaint.setStrokeCap(Paint.Cap.BUTT);
		mBorderPaint.setDither(true);
		mBorderPaint.setStrokeWidth(mBorderWidth);
		mBorderPaint.setStyle(Paint.Style.STROKE);

        mFavoriteBitmap = ImageHelper.drawableToBitmap(res.getDrawable(R.drawable.ic_favorite_emty));
        mShowFavoriteIcon = false;
    }

    @Override
    public void draw(Canvas canvas) {
        generateCirclePatternBitmap();
        canvas.drawBitmap(mBitmap, 0, 0, null);
        if (mShowFavoriteIcon) {
            canvas.drawBitmap(mFavoriteBitmap, mBorderWidth, mBorderWidth, null);
        }
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
    }
    
    @Override
    public int getIntrinsicWidth() {
        return mSize;
    }

    @Override
    public int getIntrinsicHeight() {
        return mSize;
    }

    public void setColor(int color) {
        mColor = color;
        invalidateSelf();
    }

    public void setBorderColor(int color) {
        mBorderColor = color;
		mBorderPaint.setColor(mBorderColor);
        invalidateSelf();
    }

    public void setShowFavoriteIcon(boolean show) {
        mShowFavoriteIcon = show;
        invalidateSelf();
    }

	private void generateCirclePatternBitmap() {
		generatePatternBitmap();

        mBitmap = Bitmap.createBitmap(mSize, mSize, Config.ARGB_8888);
        final Canvas canvas = new Canvas(mBitmap);

        final int width = mPatternBitmap.getWidth();
        final int height = mPatternBitmap.getHeight();
        final int square = Math.min(width, height);

        final Rect cropRect = new Rect((width - square) / 2, (height - square) / 2, square, square);
        final RectF circleRect = new RectF(0f, 0f, mSize, mSize);

        final Path fillPath = new Path();
        fillPath.addArc(circleRect, 0f, 360f);

        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        canvas.drawPath(fillPath, mPaint);

        // mask in the icon where the bitmap is opaque
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(mPatternBitmap, cropRect, circleRect, mPaint);

        // prepare paint for frame drawing
        mPaint.setXfermode(null);
        canvas.drawCircle(mSize / 2, mSize / 2, (mSize - mBorderWidth) / 2, mBorderPaint);
    }

	/**
     * @author Daniel Nilsson
     * (AlphaPatternDrawable.generatePatternBitmap())
     */

	private void generatePatternBitmap() {
        if (mShowFavoriteIcon) {
            mPaintWhite.setColorFilter(null);
            mPaintGray.setColorFilter(null);
        } else {
            PorterDuffColorFilter Filter = new PorterDuffColorFilter(mColor, Mode.SRC_ATOP);
            mPaintWhite.setColorFilter(Filter);
            mPaintGray.setColorFilter(Filter);
        }

		mPatternBitmap = Bitmap.createBitmap(mSize, mSize, Config.ARGB_8888);
		Canvas canvas = new Canvas(mPatternBitmap);

        final float rectangleSize = mSize / sNumRectangles;

		RectF r = new RectF();
		boolean verticalStartWhite = true;
		for (int i = 0; i <= sNumRectangles; i++) {
			boolean isWhite = verticalStartWhite;
			for (int j = 0; j <= sNumRectangles; j++) {
				r.top = i * rectangleSize;
				r.left = j * rectangleSize;
				r.bottom = r.top + rectangleSize;
				r.right = r.left + rectangleSize;

				canvas.drawRect(r, isWhite ? mPaintWhite : mPaintGray);
				isWhite = !isWhite;
			}
			verticalStartWhite = !verticalStartWhite;
		}
	}
}
