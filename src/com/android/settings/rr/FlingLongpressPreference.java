/*
 * Copyright (C) 2014 TeamEos
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

import com.android.settings.R;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;
import android.preference.DialogPreference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;

public class FlingLongpressPreference extends DialogPreference implements
        SeekBar.OnSeekBarChangeListener {
    private static final String TAG = FlingLongpressPreference.class.getSimpleName();

    private static final String SETTINGSNS = "http://schemas.android.com/apk/res/com.android.settings";
    private static final String ATTR_URI = "observedUri";
    private String mActionUri;

    // just a hair about tap timeout
    private static final int MIN_VAL = 25;
    // 500, but everything shifts by 25
    private static final int MAX_VAL = ViewConfiguration.getLongPressTimeout() - MIN_VAL;

    private SeekBar mLongPress;
    private ContentResolver mResolver;

    // what settings provider holds
    private int mStoredVal;

    // track the instance state of the seekbar
    private int mTempVal;

    public FlingLongpressPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mResolver = context.getContentResolver();

        // this was hard coded for NX, but we can use for anything that
        // takes a longpress timeout configuration. So try to load
        // the uri from attr, if not, it's NX
        int uriRes = attrs.getAttributeResourceValue(SETTINGSNS, ATTR_URI, -1);
        mActionUri = uriRes != -1 ? context.getResources().getString(uriRes) : Settings.Secure.FLING_LONGPRESS_TIMEOUT;

        mStoredVal = Settings.Secure.getIntForUser(mResolver, mActionUri, MAX_VAL, UserHandle.USER_CURRENT);
        mTempVal = mStoredVal - MIN_VAL;
        setDialogLayoutResource(R.layout.fling_longpress_timeout);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        ViewGroup container = (ViewGroup) view
                .findViewById(R.id.fling_longpress_container);
        mLongPress = (SeekBar) container.findViewById(R.id.seekbar);
        mLongPress.setMax(MAX_VAL);
        mLongPress.setProgress(mStoredVal - MIN_VAL);
        mLongPress.setOnSeekBarChangeListener(this);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        builder.setNeutralButton(R.string.long_swipe_reset,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        AlertDialog d = (AlertDialog) getDialog();
        Button defaultsButton = d.getButton(DialogInterface.BUTTON_NEUTRAL);
        defaultsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLongPress.setProgress(MAX_VAL);
            }
        });
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
            boolean fromUser) {
        // always holds seekbar value, not adjusted settings value
        mTempVal = progress;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub

    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (!positiveResult) {
            // user canceled, set Temp values back to
            // last saved settings vals
            mTempVal = mStoredVal - MIN_VAL;
            return;
        }
        mStoredVal = mTempVal + MIN_VAL;
        Settings.Secure.putIntForUser(mResolver, mActionUri, mStoredVal, UserHandle.USER_CURRENT);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (getDialog() == null || !getDialog().isShowing()) {
            return superState;
        }

        // Save the dialog state
        final SavedState myState = new SavedState(superState);
        myState.mLongpressState = mTempVal;

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

        mLongPress.setProgress(myState.mLongpressState);
    }

    private static class SavedState extends BaseSavedState {
        int mLongpressState;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);
            mLongpressState = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
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
