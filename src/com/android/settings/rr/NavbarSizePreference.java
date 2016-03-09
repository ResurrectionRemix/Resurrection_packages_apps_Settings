/*
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

package com.android.settings.rr;

import com.android.internal.utils.du.DUActionUtils;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.settings.R;

public class NavbarSizePreference extends DialogPreference implements
        SeekBar.OnSeekBarChangeListener {
    private static final int DEVICE_NORMAL_SCREEN = 1;
    private static final int DEVICE_LARGE_SCREEN = 2;
    private static final int DEVICE_XLARGE_SCREEN = 3;

    private SeekBar mNavigationBarHeight;
    private SeekBar mNavigationBarHeightLandscape;
    private SeekBar mNavigationBarWidth;
    private TextView mBarHeightValue;
    private TextView mBarHeightLandscapeValue;
    private TextView mBarWidthValue;
    private int mHValue;
    private int mLValue;
    private int mWValue;
    private int mDefaultHeight;
    private int mDefaultHeightLandscape;
    private int mDefaultWidth;
    private int mMinHeightPercent;
    private int mMinWidthPercent;
    private int mMaxHeightPercent;
    private int mMaxWidthPercent;

    private Context ctx;
    private int mScreenSize;

    public NavbarSizePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        ctx = context;
        final Resources res = context.getResources();
        final ContentResolver cr = context.getContentResolver();
        mMinHeightPercent = res.getInteger(R.integer.navigation_bar_height_min_percent);
        mMinWidthPercent = res.getInteger(R.integer.navigation_bar_width_min_percent);
        mMaxHeightPercent = res.getInteger(R.integer.navigation_bar_height_max_percent);
        mMaxWidthPercent = res.getInteger(R.integer.navigation_bar_width_max_percent);
        mDefaultHeight = res.getDimensionPixelSize(com.android.internal.R.dimen.navigation_bar_height);
        mDefaultHeightLandscape = res.getDimensionPixelSize(com.android.internal.R.dimen.navigation_bar_height_landscape);
        mDefaultWidth = res.getDimensionPixelSize(com.android.internal.R.dimen.navigation_bar_width);

        // load user settings
        mHValue = Settings.Secure.getInt(cr, Settings.Secure.NAVIGATION_BAR_HEIGHT, mDefaultHeight);
        mWValue = Settings.Secure.getInt(cr, Settings.Secure.NAVIGATION_BAR_WIDTH, mDefaultWidth);
        mLValue = Settings.Secure.getInt(cr, Settings.Secure.NAVIGATION_BAR_HEIGHT_LANDSCAPE, mDefaultHeightLandscape);

        // hide settings that don't apply to device
        if (DUActionUtils.isNormalScreen()) {
            mScreenSize = DEVICE_NORMAL_SCREEN;
        } else if (DUActionUtils.isLargeScreen()) {
            mScreenSize = DEVICE_LARGE_SCREEN;
        } else if (DUActionUtils.isXLargeScreen()) {
            mScreenSize = DEVICE_XLARGE_SCREEN;
        } else {
            mScreenSize = DEVICE_NORMAL_SCREEN;
        }

        setDialogLayoutResource(R.layout.navbar_size);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        final int currentHeightPercent =
                getSharedPreferenceValue("heightPercent", mDefaultHeight, mHValue, mMinHeightPercent, mMaxHeightPercent);
        final int currentWidthPercent =
                getSharedPreferenceValue("widthPercent", mDefaultWidth, mWValue, mMinWidthPercent, mMaxWidthPercent);
        final int currentHeightLandscapePercent =
                getSharedPreferenceValue("heightLandscapePercent", mDefaultHeightLandscape, mLValue, mMinHeightPercent, mMaxHeightPercent);

        // Navbar height in portrait
        ViewGroup port = (ViewGroup) view.findViewById(R.id.port_container);
        mNavigationBarHeight = (SeekBar) port.findViewById(R.id.seekbar);
        mBarHeightValue = (TextView) port.findViewById(R.id.value);
        mNavigationBarHeight.setMax(mMaxHeightPercent - mMinHeightPercent);
        mNavigationBarHeight.setProgress(currentHeightPercent);
        mBarHeightValue.setText(String.valueOf(currentHeightPercent + mMinHeightPercent) + "%");
        mNavigationBarHeight.setOnSeekBarChangeListener(this);

        // Navbar width in landscape (phones only)
        ViewGroup land_vert = (ViewGroup) view.findViewById(R.id.land_vert_container);
        mNavigationBarWidth = (SeekBar) land_vert.findViewById(R.id.seekbar);
        mBarWidthValue = (TextView) land_vert.findViewById(R.id.value);
        if (mScreenSize != DEVICE_NORMAL_SCREEN) {
            land_vert.setVisibility(View.GONE);
        } else {
            mNavigationBarWidth.setMax(mMaxWidthPercent - mMinWidthPercent);
            mNavigationBarWidth.setProgress(currentWidthPercent);
            mBarWidthValue.setText(String.valueOf(currentWidthPercent + mMinWidthPercent) + "%");
            mNavigationBarWidth.setOnSeekBarChangeListener(this);
        }

        // Navbar height in landscape (tablets only)
        ViewGroup land_hor = (ViewGroup) view.findViewById(R.id.land_hor_container);
        mNavigationBarHeightLandscape = (SeekBar) land_hor.findViewById(R.id.seekbar);
        mBarHeightLandscapeValue = (TextView) land_hor.findViewById(R.id.value);
        if (mScreenSize == DEVICE_NORMAL_SCREEN) {
            land_hor.setVisibility(View.GONE);
        } else {
            mNavigationBarHeightLandscape.setMax(mMaxHeightPercent - mMinHeightPercent);
            mNavigationBarHeightLandscape.setProgress(currentHeightLandscapePercent);
            mBarHeightLandscapeValue.setText(String.valueOf(currentHeightLandscapePercent + mMinHeightPercent) + "%");
            mNavigationBarHeightLandscape.setOnSeekBarChangeListener(this);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekbar, int rawprogress, boolean fromUser) {
        ContentResolver cr = ctx.getContentResolver();
        double proportion = 1.0;
        if (fromUser) {
            if (seekbar == mNavigationBarHeight) {
                final int progress = rawprogress + mMinHeightPercent;
                proportion = ((double) progress / 100.0);
                mBarHeightValue.setText(String.valueOf(progress) + "%");
                mHValue = (int) (proportion * mDefaultHeight);
                Settings.Secure.putInt(cr,
                        Settings.Secure.NAVIGATION_BAR_HEIGHT, mHValue);
            } else if (seekbar == mNavigationBarWidth) {
                final int progress = rawprogress + mMinWidthPercent;
                proportion = ((double) progress / 100.0);
                mBarWidthValue.setText(String.valueOf(progress) + "%");
                mWValue = (int) (proportion * mDefaultWidth);
                Settings.Secure.putInt(cr,
                        Settings.Secure.NAVIGATION_BAR_WIDTH, mWValue);
            } else if (seekbar == mNavigationBarHeightLandscape) {
                final int progress = rawprogress + mMinHeightPercent;
                proportion = ((double) progress / 100.0);
                mBarHeightLandscapeValue.setText(String.valueOf(progress) + "%");
                mLValue = (int) (proportion * mDefaultHeightLandscape);
                Settings.Secure.putInt(cr,
                        Settings.Secure.NAVIGATION_BAR_HEIGHT_LANDSCAPE, mLValue);
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        SharedPreferences prefs = ctx.getSharedPreferences("navbar_size_values",
                Context.MODE_PRIVATE);
        prefs.edit().putInt("heightPercent", mNavigationBarHeight.getProgress())
                .putInt("widthPercent", mNavigationBarWidth.getProgress())
                .putInt("heightLandscapePercent", mNavigationBarHeightLandscape.getProgress()).commit();
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        builder.setPositiveButton(R.string.positive_done, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        AlertDialog d = (AlertDialog) getDialog();
        Button cancelButton = d.getButton(DialogInterface.BUTTON_NEGATIVE);
        cancelButton.setVisibility(View.GONE);
    }

    private int getSharedPreferenceValue(String string, int value, int storedValue, int minPercent, int maxPercent) {
        // loads previous bar states per type
        SharedPreferences prefs = ctx.getSharedPreferences("navbar_size_values",
                Context.MODE_PRIVATE);
        return prefs.getInt(string,
                (int) (100.0 * (storedValue - (minPercent / 100.0) * value) /
                ((maxPercent / 100.0) * value - (minPercent / 100.0) * value)));
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (getDialog() == null || !getDialog().isShowing()) {
            return superState;
        }

        // Save the dialog state
        final SavedState myState = new SavedState(superState);
        myState.mSavedStateHeight = getSharedPreferenceValue("heightPercent", mDefaultHeight, mHValue, mMinHeightPercent, mMaxHeightPercent);
        myState.mSavedStateWidth = getSharedPreferenceValue("widthPercent", mDefaultWidth, mWValue, mMinWidthPercent, mMaxWidthPercent);
        myState.mSavedStateHeightLand = getSharedPreferenceValue("heightLandscapePercent", mDefaultHeightLandscape, mLValue, mMinHeightPercent, mMaxHeightPercent);

        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());

        mNavigationBarHeight.setProgress(myState.mSavedStateHeight);
        mNavigationBarWidth.setProgress(myState.mSavedStateWidth);
        mNavigationBarHeightLandscape.setProgress(myState.mSavedStateHeightLand);
    }

    private static class SavedState extends BaseSavedState {
        int mSavedStateHeight;
        int mSavedStateWidth;
        int mSavedStateHeightLand;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);
            mSavedStateHeight = source.readInt();
            mSavedStateWidth = source.readInt();
            mSavedStateHeightLand = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mSavedStateHeight);
            dest.writeInt(mSavedStateWidth);
            dest.writeInt(mSavedStateHeightLand);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {

            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
