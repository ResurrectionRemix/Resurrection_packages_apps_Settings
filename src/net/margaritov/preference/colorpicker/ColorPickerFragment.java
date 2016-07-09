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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.inputmethod.InputMethodManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.EditText;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.android.internal.util.rr.ColorHelper;

import com.android.settings.R;

public class ColorPickerFragment extends Fragment implements
        ColorPickerView.OnColorChangedListener, TextWatcher, View.OnClickListener,
                View.OnLongClickListener, View.OnFocusChangeListener {

    private static final String PREFERENCE_NAME  =
            "color_picker_fragment";
    private static final String PREFERENCE_FALLBACK_NAME  =
            "color_picker_dialog";
    private static final String SHOW_FAVORITES =
            "show_favorites";
    private static final String SHOW_HELP_SCREEN  =
            "show_help_screen";
    private static final String FAVORITE_COLOR_BUTTON  =
            "favorite_color_button_";

    private static final int PALETTE_DARKKAT  = 0;
    private static final int PALETTE_MATERIAL = 1;
    private static final int PALETTE_RGB      = 2;

    private static final int SHOW = 0;
    private static final int HIDE = 1;
    private static final int NONE = 2;

    private static final int COLOR_TRANSITION       = 0;
    private static final int HEX_BAR_VISIBILITY     = 1;
    private static final int FAVORITES_VISIBILITY   = 2;
    private static final int HELP_SCREEN_VISIBILITY = 3;

    private SharedPreferences mPrefs;
    private SharedPreferences mFallbackPrefs;
    private Resources mResources;

    private ApplyColorView mApplyColorAction;
    private MenuItem mShowEditHexAction;
    private EditText mEditHexValue;

    private View mColorPickerView;
    private ColorPickerView mColorPicker;
    private LinearLayout mColorButtonsLayout;
    private LinearLayout mFavoritesLayout;

    private View mHelpScreen;
    private CheckedTextView mCheckShowHelpScreen;
    private Button mCloseHelpScreen;

	private float mFullTranslationX;
    private int mInitialColor;
    private int mResetColor1;
    private int mResetColor2;
    private CharSequence mResetColor1Title;
    private CharSequence mResetColor2Title;
    private int mOldColorValue;
    private int mNewColorValue;
    private boolean mHideResetColor1 = true;
    private boolean mHideResetColor2 = true;
    private boolean mShowSubMenu = false;
    private boolean mShowFavorites;
    private int mFavoritesLayoutHeight = 0;
    private boolean mShowHelpScreen;
    private int mHelpScreenHeight = 0;
    private boolean mHelpScreenVisible;
    private int mApplyColorIconAnimationType;
    private int mAnimationType;

    private Animator mAnimator;

    private OnColorChangedListener mListener;

    public interface OnColorChangedListener {
        public void onColorChanged(int color);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            getActivity().getFragmentManager().popBackStack();
            return null;
        } else {
            return inflateAndSetupView(inflater, container, savedInstanceState);
        }
    }

    private View inflateAndSetupView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mPrefs = getActivity().getSharedPreferences(PREFERENCE_NAME, Activity.MODE_PRIVATE);
        mFallbackPrefs = getActivity().getSharedPreferences(
                PREFERENCE_FALLBACK_NAME, Activity.MODE_PRIVATE);
        mResources = getActivity().getResources();

		mFullTranslationX = mResources.getDimension(
                R.dimen.color_picker_action_apply_color_translation_x);
        mInitialColor = getArguments().getInt("initial_color");
        mResetColor1 = getArguments().getInt("reset_color_1");
        mResetColor2 = getArguments().getInt("reset_color_2");
        mResetColor1Title = getArguments().getCharSequence("reset_color_1_title");
        mResetColor2Title = getArguments().getCharSequence("reset_color_2_title");
        boolean alphaSliderVisible = getArguments().getBoolean("alpha_slider_visible");
        mNewColorValue = getArguments().getInt("new_color");
        mOldColorValue = getArguments().getInt("old_color");
        mHelpScreenVisible = getArguments().getBoolean("help_screen_visible");

        if (mResetColor1 != 0) {
            mHideResetColor1 = false;
            if (mResetColor2 != 0) {
                mHideResetColor2 = false;
                mShowSubMenu = true;
            }
        }

        final Context contextThemeWrapper = new ContextThemeWrapper(
                getActivity(), R.style.Theme_ColorPickerFragment);
        LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);

        mColorPickerView = localInflater.inflate(R.layout.color_picker_fragment, container, false);
        mColorPicker = (ColorPickerView) mColorPickerView.findViewById(R.id.color_picker_view);
        mColorButtonsLayout = (LinearLayout) mColorPickerView.findViewById(
                R.id.color_picker_color_buttons_layout);
        mFavoritesLayout = (LinearLayout) mColorPickerView.findViewById(R.id.favorite_buttons);
        mHelpScreen = mColorPickerView.findViewById(R.id.color_picker_help_screen);

        mColorPicker.setOnColorChangedListener(this);
        mColorPicker.setColor(mInitialColor);

        if (alphaSliderVisible) {
            mColorPicker.setAlphaSliderVisible(alphaSliderVisible);
        }

        if (mAnimator == null) {
            mAnimator = createAnimator();
        }

        setUpFavoriteColorButtons();
        setUpPaletteColorButtons();
        setUpHelpScreen();

        return mColorPickerView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.color_picker_ab_more, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (mHideResetColor1) {
            menu.removeItem(R.id.reset_colors);
            menu.removeItem(R.id.reset_color);
        } else {
            if (mShowSubMenu) {
                menu.removeItem(R.id.reset_color);
                if (mResetColor1Title != null) {
                    menu.findItem(R.id.reset_colors).getSubMenu()
                            .findItem(R.id.reset_color1).setTitle(mResetColor1Title);
                }
                if (mResetColor2Title != null) {
                    menu.findItem(R.id.reset_colors).getSubMenu()
                    .findItem(R.id.reset_color2).setTitle(mResetColor2Title);
                }
            } else {
                menu.removeItem(R.id.reset_colors);
                if (mResetColor1Title != null) {
                    menu.findItem(R.id.reset_color).setTitle(mResetColor1Title);
                }
            }
        }

        MenuItem applyColor = menu.findItem(R.id.apply_color);
        mApplyColorAction = (ApplyColorView) applyColor.getActionView();
        if (mNewColorValue != mInitialColor) {
            mApplyColorAction.setColor(mNewColorValue);
            mApplyColorAction.setColorPreviewTranslationX(0f);
            mApplyColorAction.showSetIcon(true);
            mApplyColorAction.applySetIconAlpha(1f);
            mApplyColorAction.setOnClickListener(this);
        } else {
            mApplyColorAction.setColor(mNewColorValue);
            mApplyColorAction.setColorPreviewTranslationX(mFullTranslationX);
            mApplyColorAction.showSetIcon(false);
            mApplyColorAction.applySetIconAlpha(0f);
            mApplyColorAction.setOnClickListener(null);
        }

        mShowEditHexAction = menu.findItem(R.id.edit_hex);
        LinearLayout editHexActionView = (LinearLayout) mShowEditHexAction.getActionView();
        mEditHexValue = (EditText) editHexActionView.findViewById(R.id.ab_edit_hex);
        ImageButton setHexValueButton = (ImageButton) editHexActionView.findViewById(R.id.ab_edit_hex_enter);
        mEditHexValue.setText(ColorPickerPreference.convertToARGB(mNewColorValue));
        mEditHexValue.setOnFocusChangeListener(this);
        setHexValueButton.setOnClickListener(this);

        MenuItem showHideFavorites = menu.findItem(R.id.show_hide_favorites);
        int favoritesTitleResId;
        int favoritesIconResId;
        if (mShowFavorites) {
            favoritesTitleResId = R.string.hide_favorites_title;
            favoritesIconResId = R.drawable.ic_hide_favorites;
        } else {
            favoritesTitleResId = R.string.show_favorites_title;
            favoritesIconResId = R.drawable.ic_show_favorites;
        }
        showHideFavorites.setTitle(mResources.getString(favoritesTitleResId));
        showHideFavorites.setIcon(mResources.getDrawable(favoritesIconResId));

        MenuItem showHideHelp = menu.findItem(R.id.show_hide_help);
        int helpTitleResId;
        int iconResId;
        if (mHelpScreenVisible) {
            helpTitleResId = R.string.hide_help_title;
        } else {
            helpTitleResId = R.string.show_help_title;
        }
        showHideHelp.setTitle(mResources.getString(helpTitleResId));
    }

    private ValueAnimator createAnimator() {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(300);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator animation) {
                float position = animation.getAnimatedFraction();
                if (mAnimationType == COLOR_TRANSITION) {
                    int blended = ColorHelper.getBlendColor(mOldColorValue, mNewColorValue, position);
                    mApplyColorAction.setColor(blended);
                    if (mApplyColorIconAnimationType != NONE) {
                        final boolean animateShow = mApplyColorIconAnimationType == SHOW;
                        float currentTranslationX = animateShow ? mFullTranslationX : 0f;
                        float alpha = animateShow ? 0f : 1f;
                        boolean applyAlpha = false;

                        if (animateShow) {
                            currentTranslationX = mFullTranslationX * (1f - position);
                            if (position > 0.5f) {
                                alpha = (position - 0.5f) * 2;
                                applyAlpha = true;
                            }
                        } else {
                            currentTranslationX = mFullTranslationX * position;
                            if (position <= 0.5f && position > 0f) {
                                alpha = 1f - position * 2;
                                applyAlpha = true;
                            }
                        }
                        mApplyColorAction.setColorPreviewTranslationX(currentTranslationX);
                        if (applyAlpha) {
                            mApplyColorAction.applySetIconAlpha(alpha);
                        }
                    }
                } else if (mAnimationType == FAVORITES_VISIBILITY) {
                    int childCount = mColorButtonsLayout.getChildCount();
                    for (int i = 0; i < childCount; i++) {
                        if (i == 0) {
                            mFavoritesLayout.setAlpha(mShowFavorites ? 1f - position : position);
                        } else {
                            View child = mColorButtonsLayout.getChildAt(i);
                            child.setTranslationY(0 - mFavoritesLayoutHeight * (
                                    mShowFavorites ? position : 1f - position));
                        }
                    }
                } else {
                    mHelpScreen.setTranslationY(
                            mHelpScreenHeight * (mHelpScreenVisible ? position  : 1f - position));
                    mHelpScreen.setAlpha(mHelpScreenVisible ? 1f - position : position);
                }
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (mAnimationType == FAVORITES_VISIBILITY) {
                } else if (mAnimationType != COLOR_TRANSITION) {
                    if (!mHelpScreenVisible) {
                        mHelpScreen.setVisibility(View.VISIBLE);
                    }
                }
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mAnimationType == COLOR_TRANSITION) {
                    if (mApplyColorIconAnimationType != NONE) {
                        if (mApplyColorIconAnimationType != SHOW) {
                            mApplyColorAction.showSetIcon(false);
                        } else {
                            mApplyColorAction.setOnClickListener(getFragmentOnClickListener());
                        }
                    }
                    mOldColorValue = mNewColorValue;
                } else if (mAnimationType == FAVORITES_VISIBILITY) {
                    animation.setInterpolator(null);
                    mShowFavorites = !mShowFavorites;
                    writeShowFavorites(mShowFavorites);
                } else {
                    animation.setInterpolator(null);
                    if (mHelpScreenVisible) {
                        mHelpScreen.setVisibility(View.GONE);
                    }
                    mHelpScreenVisible = !mHelpScreenVisible;
                    getActivity().invalidateOptionsMenu();
                }
            }
        });
        return animator;
    }

    private void setUpFavoriteColorButtons() {
        mShowFavorites = getShowFavorites();
        TypedArray ta = mResources.obtainTypedArray(R.array.color_picker_favorite_color_buttons);

        for (int i=0; i<4; i++) {
            int resId = ta.getResourceId(i, 0);
            int buttonNumber = i + 1;
            String tag = String.valueOf(buttonNumber);
            ColorViewButton button = (ColorViewButton) mColorPickerView.findViewById(resId);
            button.setTag(tag);
            button.setOnLongClickListener(this);
            if (getFavoriteButtonValue(button) != 0) {
                button.setColor(getFavoriteButtonValue(button));
                button.setOnClickListener(this);
            } else {
                button.setShowFavoriteIcon(true);
            }
        }

        ta.recycle();

        mFavoritesLayout.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mFavoritesLayoutHeight = mFavoritesLayout.getHeight()
                        + mResources.getDimensionPixelSize(
                                R.dimen.color_picker_color_buttons_container_margin_bottom);
                mFavoritesLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                if (!mShowFavorites) {
                    int childCount = mColorButtonsLayout.getChildCount();
                    for (int i = 0; i < childCount; i++) {
                        if (i == 0) {
                            mFavoritesLayout.setAlpha(0f);
                        } else {
                            View child = mColorButtonsLayout.getChildAt(i);
                            child.setTranslationY(0 - mFavoritesLayoutHeight);
                        }
                    }
                }
            }
        });

    }

    private void setUpPaletteColorButtons() {
        TypedArray layouts = mResources.obtainTypedArray(
                R.array.color_picker_palette_color_buttons_layouts);
        TypedArray buttons = mResources.obtainTypedArray(R.array.color_picker_palette_color_buttons);
        TypedArray colors = mResources.obtainTypedArray(R.array.color_picker_darkkat_palette);

        for (int i=0; i<3; i++) {
            int layoutResId = layouts.getResourceId(i, 0);
            LinearLayout layout = (LinearLayout) mColorPickerView.findViewById(layoutResId);
            TextView paletteTitle = (TextView) layout.findViewById(R.id.palette_color_buttons_title);
            int titleResId = R.string.palette_darkkat_title;
            if (i == PALETTE_MATERIAL) {
                titleResId = R.string.palette_material_title;
                colors = mResources.obtainTypedArray(R.array.color_picker_material_palette);
            } else if (i == PALETTE_RGB) {
                titleResId = R.string.palette_rgb_title;
                colors = mResources.obtainTypedArray(R.array.color_picker_rgb_palette);
            }
            paletteTitle.setText(titleResId);

            for (int j=0; j<8; j++) {
                int buttonResId = buttons.getResourceId(j, 0);
                ColorViewButton button = (ColorViewButton) layout.findViewById(buttonResId);
                button.setColor(mResources.getColor(colors.getResourceId(j, 0)));
                button.setOnClickListener(this);
            }
        }

        layouts.recycle();
        buttons.recycle();
        colors.recycle();
    }

    private void setUpHelpScreen() {
        mShowHelpScreen = getShowHelpScreen();

        mCheckShowHelpScreen = (CheckedTextView) mColorPickerView.findViewById(
                R.id.color_picker_check_show_help_screen);
        mCheckShowHelpScreen.setChecked(!mShowHelpScreen);
        mCheckShowHelpScreen.setOnClickListener(this);
        mCloseHelpScreen = (Button) mColorPickerView.findViewById(
                R.id.color_picker_help_button_ok);
        mCloseHelpScreen.setOnClickListener(this);

        mHelpScreen.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mHelpScreenHeight = mHelpScreen.getHeight();
                mHelpScreen.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                if (!mHelpScreenVisible) {
                    mHelpScreen.setTranslationY(mFavoritesLayoutHeight);
                    mHelpScreen.setAlpha(0f);
                    mHelpScreen.setVisibility(View.GONE);
                }
            }
        });
        mHelpScreen.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
    }

    public void setOnColorChangedListener(OnColorChangedListener listener) {
        mListener = listener;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.reset_color:
            case R.id.reset_color1:
                mColorPicker.setColor(mResetColor1, true);
                return true;
            case R.id.reset_color2:
                mColorPicker.setColor(mResetColor2, true);
                return true;
            case R.id.edit_hex:
                mEditHexValue.setText(ColorPickerPreference.convertToARGB(mNewColorValue));
                return true;
            case R.id.show_hide_favorites:
                mAnimationType = FAVORITES_VISIBILITY;
                mAnimator.setInterpolator(new FastOutSlowInInterpolator());
                mAnimator.setDuration(300);
                mAnimator.start();
                return true;
            case R.id.show_hide_help:
                mAnimationType = HELP_SCREEN_VISIBILITY;
                mAnimator.setInterpolator(new FastOutSlowInInterpolator());
                mAnimator.setDuration(mShowFavorites ? 195 : 225);
                mAnimator.start();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.color_picker_apply_color_action_layout) {
            if (mListener != null) {
                mListener.onColorChanged(mApplyColorAction.getColor());
            }
            getActivity().onBackPressed();
        } else if (v.getId() == R.id.ab_edit_hex_enter) {
			String text = mEditHexValue.getText().toString();
			mShowEditHexAction.collapseActionView();
            try {
                int newColor = ColorPickerPreference.convertToColorInt(text);
                if (newColor != mOldColorValue) {
                    mNewColorValue = newColor;
                    mOldColorValue = mNewColorValue;
                    mColorPicker.setColor(mNewColorValue);
                    if (mNewColorValue != mInitialColor) {
                        mApplyColorAction.setColor(mNewColorValue);
                        mApplyColorAction.setColorPreviewTranslationX(0f);
                        mApplyColorAction.showSetIcon(true);
                        mApplyColorAction.applySetIconAlpha(1f);
                        mApplyColorAction.setOnClickListener(getFragmentOnClickListener());
                    } else {
                        mApplyColorAction.setColor(mNewColorValue);
                        mApplyColorAction.setColorPreviewTranslationX(mFullTranslationX);
                        mApplyColorAction.showSetIcon(false);
                        mApplyColorAction.applySetIconAlpha(0f);
                        mApplyColorAction.setOnClickListener(null);
                    }
                }
            } catch (Exception e) {}
        } else if (v.getId() == R.id.color_picker_check_show_help_screen) {
            mCheckShowHelpScreen.toggle();
            writeShowHelpScreen(!mCheckShowHelpScreen.isChecked());
        } else if (v.getId() == R.id.color_picker_help_button_ok) {
            mAnimationType = HELP_SCREEN_VISIBILITY;
            mAnimator.setInterpolator(new FastOutSlowInInterpolator());
            mAnimator.setDuration(195);
            mAnimator.start();
        } else if (v instanceof ColorViewButton) {
            try {
                int newColor = ((ColorViewButton) v).getColor();
                if (newColor != mOldColorValue) {
                    mColorPicker.setColor(newColor, true);
                }
            } catch (Exception e) {}
        }
    }

    @Override
    public boolean onLongClick(View v) {
        ColorViewButton button = (ColorViewButton) v;
        if (!v.hasOnClickListeners()) {
            button.setOnClickListener(this);
        }
        button.setShowFavoriteIcon(false);
        button.setColor(mApplyColorAction.getColor());
        writeFavoriteButtonValue(button);
        return true;
    }

    @Override
    public void onColorChanged(int color) {
        mApplyColorIconAnimationType = NONE;
        if (color != mOldColorValue) {
            mNewColorValue = color;
            if (mNewColorValue == mInitialColor) {
                if (mOldColorValue != mInitialColor) {
                    mApplyColorIconAnimationType = HIDE;
                    mApplyColorAction.setOnClickListener(null);
                    mApplyColorAction.setClickable(false);
                }
            } else if (mOldColorValue == mInitialColor) {
                mApplyColorIconAnimationType = SHOW;
                mApplyColorAction.showSetIcon(true);
            }
            mAnimationType = COLOR_TRANSITION;
            mAnimator.setDuration(300);
            mAnimator.start();

            try {
                if (mEditHexValue != null) {
                    mEditHexValue.setText(ColorPickerPreference.convertToARGB(color));
                }
            } catch (Exception e) {}
        }

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) {
            mEditHexValue.removeTextChangedListener(this);
            InputMethodManager inputMethodManager = (InputMethodManager) getActivity()
                    .getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
        } else {
            mEditHexValue.addTextChangedListener(this);
        }
    }

    private int getColor() {
        return mColorPicker.getColor();
    }

    private View.OnClickListener getFragmentOnClickListener() {
        return this;
    }

    private void writeShowFavorites(boolean show) {
        mPrefs.edit().putBoolean(SHOW_FAVORITES, show).commit();
        getActivity().invalidateOptionsMenu();
    }

    private boolean getShowFavorites() {
        boolean fallback = mFallbackPrefs.getBoolean("favorites_visible", true);
        return mPrefs.getBoolean(SHOW_FAVORITES, fallback);
    }

    private void writeFavoriteButtonValue(ColorViewButton button) {
        mPrefs.edit().putInt(FAVORITE_COLOR_BUTTON + (String) button.getTag(),
                button.getColor()).commit();
    }

    private int getFavoriteButtonValue(ColorViewButton button) {
        int fallback = mFallbackPrefs.getInt(FAVORITE_COLOR_BUTTON + (String) button.getTag(), 0);
        return mPrefs.getInt(FAVORITE_COLOR_BUTTON + (String) button.getTag(), fallback);
    }

    private void writeShowHelpScreen(boolean show) {
        mPrefs.edit().putBoolean(SHOW_HELP_SCREEN, show).commit();
    }

    private boolean getShowHelpScreen() {
        boolean fallback = mFallbackPrefs.getBoolean(SHOW_HELP_SCREEN, true);
        return mPrefs.getBoolean(SHOW_HELP_SCREEN, fallback);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }


    public Bundle getState() {
        Bundle state = new Bundle();
        state.putInt("new_color", mNewColorValue);
        state.putInt("old_color", mOldColorValue);
        state.putBoolean("help_screen_visible", mHelpScreenVisible);
        return state;
    }
}
