/*
 * Copyright (C) 2010 Daniel Nilsson
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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;

import com.android.settings.R;

public class ColorPickerDialog extends Dialog implements
        ColorPickerView.OnColorChangedListener,
        PopupMenu.OnMenuItemClickListener,
        View.OnClickListener {

    private View mColorPickerView;
    private LinearLayout mActionBarMain;
    private LinearLayout mActionBarEditHex;

    private ImageButton mBackButton;
    private ImageButton mEditHexButton;
    private ImageButton mPaletteButton;
    private ImageButton mResetButton;

    private ImageButton mHexBackButton;
    private EditText mHex;
    private ImageButton mSetButton;
    private View mDivider;

    private ColorPickerView mColorPicker;
    private ColorPickerPanelView[] mPanelViewButtons;
    private ColorPickerPanelView mOldColor;
    private ColorPickerPanelView mNewColor;

    private Animator mEditHexBarFadeInAnimator;
    private Animator mEditHexBarFadeOutAnimator;
    private boolean mHideEditHexBar = false;

    private Animator mColorTransitionAnimator;
    private boolean mAnimateColorTransition = true;
    private boolean mIsPanelButtons = true;

    private final int mInitialColor;
    private final int mAndroidColor;
    private final int mrrColor;
    private int mNewColorValue;
    private boolean mIsResetButtonEnabled = false;

    private static final int PALETTE_RR  = 0;
    private static final int PALETTE_MATERIAL = 1;
    private static final int PALETTE_RGB      = 2;
    private int mPalette = PALETTE_RR;

    private static final int[][] mPaletteColors = {
            { Color.BLACK, 0xff1b1f23, 0xff33b5e5, 0xff009688,
              Color.WHITE, 0xff3f51b5, 0xfff44336, 0xff9c27b0 },
            { 0xfff44336, 0xff4caf50, 0xff2196f3, 0xff009688,
              0xff3f51b5, 0xff9c27b0, 0xffffeb3b, 0xffff9800 },
            { Color.BLACK, Color.RED, Color.GREEN, Color.BLUE,
              Color.WHITE, 0xff800080, Color.YELLOW, 0xffffa500 }
        };

    private OnColorChangedListener mListener;

    public interface OnColorChangedListener {
        public void onColorChanged(int color);
    }

    public ColorPickerDialog(Context context, int theme, int initialColor,
            int androidColor, int rrColor) {
        super(context, theme);
        mInitialColor = initialColor;
        mAndroidColor = androidColor;
        mRrColor = rrColor;
        if (mAndroidColor != 0x00000000 && mRrColor != 0x00000000) {
            mIsResetButtonEnabled = true;
        }

        setUp();
    }

    private void setUp() {
        // To fight color branding.
        getWindow().setFormat(PixelFormat.RGBA_8888);
        getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        mColorPickerView = inflater.inflate(R.layout.dialog_color_picker, null);

        setContentView(mColorPickerView);

        mActionBarMain = (LinearLayout) mColorPickerView.findViewById(R.id.action_bar_main);

        mActionBarEditHex = (LinearLayout) mColorPickerView.findViewById(R.id.action_bar_edit_hex);
        mActionBarEditHex.setVisibility(View.GONE);

        mDivider = mColorPickerView.findViewById(R.id.divider);
        mDivider.setVisibility(View.GONE);

        mBackButton = (ImageButton) mColorPickerView.findViewById(R.id.back);
        mBackButton.setOnClickListener(this);

        mEditHexButton = (ImageButton) mColorPickerView.findViewById(R.id.edit_hex);
        mEditHexButton.setOnClickListener(this);

        mPaletteButton = (ImageButton) mColorPickerView.findViewById(R.id.palette);
        mPaletteButton.setOnClickListener(this);

        mResetButton = (ImageButton) mColorPickerView.findViewById(R.id.reset);
        if (mIsResetButtonEnabled) {
            mResetButton.setOnClickListener(this);
        } else {
            mResetButton.setVisibility(View.GONE);
        }

        mHexBackButton = (ImageButton) mColorPickerView.findViewById(R.id.action_bar_edit_hex_back);
        mHexBackButton.setOnClickListener(this);

        mHex = (EditText) mColorPickerView.findViewById(R.id.hex);
        mHex.setText(ColorPickerPreference.convertToARGB(mInitialColor));

        mSetButton = (ImageButton) mColorPickerView.findViewById(R.id.enter);
        mSetButton.setOnClickListener(this);

        mColorPicker = (ColorPickerView) mColorPickerView.findViewById(R.id.color_picker_view);
        mColorPicker.setOnColorChangedListener(this);

        mPanelViewButtons = new ColorPickerPanelView[8];
        mPanelViewButtons[0] = (ColorPickerPanelView) mColorPickerView.findViewById(R.id.panel_view_1);
        mPanelViewButtons[1] = (ColorPickerPanelView) mColorPickerView.findViewById(R.id.panel_view_2);
        mPanelViewButtons[2] = (ColorPickerPanelView) mColorPickerView.findViewById(R.id.panel_view_3);
        mPanelViewButtons[3] = (ColorPickerPanelView) mColorPickerView.findViewById(R.id.panel_view_4);
        mPanelViewButtons[4] = (ColorPickerPanelView) mColorPickerView.findViewById(R.id.panel_view_5);
        mPanelViewButtons[5] = (ColorPickerPanelView) mColorPickerView.findViewById(R.id.panel_view_6);
        mPanelViewButtons[6] = (ColorPickerPanelView) mColorPickerView.findViewById(R.id.panel_view_7);
        mPanelViewButtons[7] = (ColorPickerPanelView) mColorPickerView.findViewById(R.id.panel_view_8);

        for (int i=0; i<mPanelViewButtons.length; i++) {
            mPanelViewButtons[i].setOnClickListener(this);
        }

        mOldColor = (ColorPickerPanelView) mColorPickerView.findViewById(R.id.old_color_panel);
        mOldColor.setOnClickListener(this);

        mNewColor = (ColorPickerPanelView) mColorPickerView.findViewById(R.id.new_color_panel);
        mNewColor.setOnClickListener(this);

        mNewColorValue = mInitialColor;
        mOldColor.setColor(mInitialColor);

        setPanelViewButtonsColor();
        setupAnimators();

        mAnimateColorTransition = false;
        mColorPicker.setColor(mInitialColor, true);
    }

    public void setAlphaSliderVisible(boolean visible) {
        mColorPicker.setAlphaSliderVisible(visible);
    }

    private void setPanelViewButtonsColor() {
        for (int i=0; i<mPanelViewButtons.length; i++) {
            mPanelViewButtons[i].setColor(mPaletteColors[mPalette][i]);
        }
    }

    /**
     * Set a OnColorChangedListener to get notified when the color selected by the user has changed.
     *
     * @param listener
     */
    public void setOnColorChangedListener(OnColorChangedListener listener) {
        mListener = listener;
    }

    @Override
    public void onColorChanged(int color) {
        mNewColorValue = color;
        if (mAnimateColorTransition == false) {
            mAnimateColorTransition = true;
            mNewColor.setColor(mNewColorValue);
        } else {
            mIsPanelButtons = false;
            mColorTransitionAnimator.start();
        }
        try {
            if (mHex != null) {
                mHex.setText(ColorPickerPreference.convertToARGB(color));
            }
        } catch (Exception e) {

        }
    }

    private int getColor() {
        return mColorPicker.getColor();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.back ||
                v.getId() == R.id.old_color_panel ||
                v.getId() == R.id.new_color_panel) {
            if (mListener != null && v.getId() == R.id.new_color_panel) {
                mListener.onColorChanged(mNewColor.getColor());
            }
            dismiss();
        } else if (v.getId() == R.id.palette) {
            showPalettePopupMenu(v);
        } else if (v.getId() == R.id.edit_hex) {
            showActionBarEditHex();
        } else if (v.getId() == R.id.reset) {
            showResetPopupMenu(v);
        } else if (v.getId() == R.id.action_bar_edit_hex_back) {
            hideActionBarEditHex();
        } else if (v.getId() == R.id.enter) {
            String text = mHex.getText().toString();
            try {
                int newColor = ColorPickerPreference.convertToColorInt(text);
                mColorPicker.setColor(newColor, true);
            } catch (Exception e) {
            }
            hideActionBarEditHex();
        } else {
            for (int i=0; i<mPanelViewButtons.length; i++) {
                int panelViewButtonId = mPanelViewButtons[i].getId();
                if (v.getId() == panelViewButtonId) {
                    try {
                        mColorPicker.setColor(mPaletteColors[mPalette][i], true);
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.palette_rr) {
            mPalette = PALETTE_RR;
            mColorTransitionAnimator.start();
            return true;
        } else if (item.getItemId() == R.id.palette_material) {
            mPalette = PALETTE_MATERIAL;
            mColorTransitionAnimator.start();
            return true;
        } else if (item.getItemId() == R.id.palette_rgb) {
            mPalette = PALETTE_RGB;
            mColorTransitionAnimator.start();
            return true;
        } else if (item.getItemId() == R.id.reset_android) {
            mColorPicker.setColor(mAndroidColor, true);
            return true;
        } else if (item.getItemId() == R.id.reset_rr) {
            mColorPicker.setColor(mRrColor, true);
            return true;
        }
        return false;
    }

    private void showPalettePopupMenu(View v) {
        PopupMenu popup = new PopupMenu(getContext(), v);
        popup.setOnMenuItemClickListener(this);
        popup.inflate(R.menu.palette);
        popup.show();
    }

    private void showResetPopupMenu(View v) {
        PopupMenu popup = new PopupMenu(getContext(), v);
        popup.setOnMenuItemClickListener(this);
        popup.inflate(R.menu.reset);
        popup.show();
    }

    private void showActionBarEditHex() {
        mEditHexBarFadeInAnimator.start();
    }

    private void hideActionBarEditHex() {
        mEditHexBarFadeOutAnimator.start();
    }

    private void setupAnimators() {
        mColorPickerView.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mColorPickerView.getViewTreeObserver().removeOnPreDrawListener(this);
                mHideEditHexBar = false;
                mEditHexBarFadeInAnimator = createAlphaAnimator(0, 100);
                mHideEditHexBar = true;
                mEditHexBarFadeOutAnimator = createAlphaAnimator(100, 0);
                return true;
            }
        });
        mColorTransitionAnimator = createColorTransitionAnimator(0, 1);
    }

    private ValueAnimator createAlphaAnimator(int start, int end) {
        ValueAnimator animator = ValueAnimator.ofInt(start, end);

        animator.setDuration(500);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int value = (Integer) valueAnimator.getAnimatedValue();
                float currentAlpha = value / 100f;
                mActionBarMain.setAlpha(1f - currentAlpha);
                mActionBarEditHex.setAlpha(currentAlpha);
                mDivider.setAlpha(currentAlpha);
            }
        });
        if (mHideEditHexBar) {
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    mActionBarMain.setVisibility(View.VISIBLE);
                    ViewCompat.jumpDrawablesToCurrentState(mActionBarMain);
                }
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mActionBarEditHex.setVisibility(View.GONE);
                    mDivider.setVisibility(View.GONE);
                }
            });
        } else {
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    mActionBarEditHex.setVisibility(View.VISIBLE);
                    ViewCompat.jumpDrawablesToCurrentState(mActionBarEditHex);
                    mDivider.setVisibility(View.VISIBLE);
                }
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mActionBarMain.setVisibility(View.GONE);
                }
            });
        }
        return animator;
    }

    private ValueAnimator createColorTransitionAnimator(float start, float end) {
        ValueAnimator animator = ValueAnimator.ofFloat(start, end);

        animator.setDuration(500);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
            @Override public void onAnimationUpdate(ValueAnimator animation) {
                float position = animation.getAnimatedFraction();
                if (mIsPanelButtons) {
                    int[] blended = new int[8];
                    for (int i=0; i<mPanelViewButtons.length; i++) {
                        blended[i] = blendColors(
                                mPanelViewButtons[i].getColor(),
                                mPaletteColors[mPalette][i],
                                position);
                        mPanelViewButtons[i].setColor(blended[i]);
                    }
                } else {
                    int blended = blendColors(mNewColor.getColor(), mNewColorValue, position);
                    mNewColor.setColor(blended);
                }
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (!mIsPanelButtons) {
                        mIsPanelButtons = true;
                    }
                }
            });
        return animator;
    }

    private int blendColors(int from, int to, float ratio) {
        final float inverseRatio = 1f - ratio;

        final float a = Color.alpha(to) * ratio + Color.alpha(from) * inverseRatio;
        final float r = Color.red(to) * ratio + Color.red(from) * inverseRatio;
        final float g = Color.green(to) * ratio + Color.green(from) * inverseRatio;
        final float b = Color.blue(to) * ratio + Color.blue(from) * inverseRatio;

        return Color.argb((int) a, (int) r, (int) g, (int) b);
    }

    @Override
    public Bundle onSaveInstanceState() {
        Bundle state = super.onSaveInstanceState();
        state.putInt("old_color", mOldColor.getColor());
        state.putInt("new_color", mNewColor.getColor());
        state.putInt("palette", mPalette);
        return state;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mOldColor.setColor(savedInstanceState.getInt("old_color"));
        mColorPicker.setColor(savedInstanceState.getInt("new_color"), true);
        mPalette = savedInstanceState.getInt("palette");
    }
}
