/*
 * Copyright (C) 2015 The CyanogenMod Project
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

package com.android.settings.cyanogenmod;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;

import com.android.settings.R;

public class FingerprintProgressBar extends ProgressBar {
    private static final int PROGRESS_ANIMATION_DURATION = 250;

    private Drawable mFingerprintMask;
    private Paint mNormalPaint;
    private Paint mSuccessPaint;
    private Paint mErrorPaint;
    private Bitmap mMaskedProgress;
    private Canvas mMaskedCanvas;
    private boolean mShowError;
    ObjectAnimator mProgressAnimation;

    private int mTotalEnrollmentSteps;
    private int mCurrentEnrollmentStep;

    public FingerprintProgressBar(Context context) {
        this(context, null);
    }

    public FingerprintProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FingerprintProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final Resources res = context.getResources();
        mFingerprintMask = context.getDrawable(R.drawable.fingerprint_mask);

        mNormalPaint = new Paint();
        mNormalPaint.setColor(res.getColor(R.color.fingerprint_progress_normal_color));
        mNormalPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
        mSuccessPaint = new Paint();
        mSuccessPaint.setColor(res.getColor(R.color.fingerprint_progress_success_color));
        mSuccessPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
        mErrorPaint = new Paint();
        mErrorPaint.setColor(res.getColor(R.color.fingerprint_progress_error_color));
        mErrorPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mFingerprintMask.setBounds(0, 0, w, h);
        mMaskedProgress = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mMaskedCanvas = new Canvas(mMaskedProgress);
        drawProgressOffscreen();
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        canvas.drawBitmap(mMaskedProgress, 0, 0, null);
    }

    @Override
    public synchronized void setProgress(int progress) {
        super.setProgress(progress);
        if (mMaskedCanvas != null) {
            drawProgressOffscreen();
            invalidate();
        }
    }

    private void drawProgressOffscreen() {
        final int width = getWidth();
        final int height = getHeight();
        final float progress = getMax() != 0 ? (float) getProgress() / getMax() : 0;

        final float splitAt = height - height * progress;
        final RectF normalFillRect = new RectF(0, 0, width, splitAt);
        final RectF progressFillRect = new RectF(0, splitAt, width, height);
        final Paint progressPaint = mShowError ? mErrorPaint : mSuccessPaint;

        mMaskedCanvas.drawColor(0, PorterDuff.Mode.DST_ATOP);
        mFingerprintMask.draw(mMaskedCanvas);
        mMaskedCanvas.drawRect(progressFillRect, progressPaint);
        mMaskedCanvas.drawRect(normalFillRect, mNormalPaint);
    }

    public void setProgressAnimated(int progress) {
        if (mProgressAnimation == null) {
            mProgressAnimation = ObjectAnimator.ofInt(this, "progress", progress);
            mProgressAnimation.setDuration(PROGRESS_ANIMATION_DURATION);
            mProgressAnimation.setInterpolator(new DecelerateInterpolator());
        } else {
            mProgressAnimation.setIntValues(progress);
        }
        mProgressAnimation.start();
    }

    public void showError(boolean show) {
        if (mShowError != show) {
            mShowError = show;
            if (mMaskedCanvas != null) {
                drawProgressOffscreen();
                invalidate();
            }
        }
    }

    public boolean isShowingError() {
        return mShowError;
    }
}
