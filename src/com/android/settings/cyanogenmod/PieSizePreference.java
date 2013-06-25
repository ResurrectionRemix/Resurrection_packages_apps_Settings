package com.android.settings.cyanogenmod;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.SeekBarDialogPreference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;

public class PieSizePreference extends SeekBarDialogPreference
                               implements SeekBar.OnSeekBarChangeListener {

    private SeekBar mSeekBar;

    private float mOldSize;

    private boolean mRestoredOldState = true;

    private static final int SEEK_BAR_MIN = 500;
    private static final int SEEK_BAR_MAX = 1500;

    public PieSizePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        mRestoredOldState = false;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mSeekBar = getSeekBar(view);
        mSeekBar.setMax(SEEK_BAR_MAX);
        mOldSize = Settings.System.getFloat(getContext().getContentResolver(),
                Settings.System.PIE_SIZE, 1.0f);

        mSeekBar.setProgress((int) (mOldSize * 1000 - SEEK_BAR_MIN));
        mSeekBar.setEnabled(true);
        mSeekBar.setOnSeekBarChangeListener(this);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
        Settings.System.putFloat(getContext().getContentResolver(),
                Settings.System.PIE_SIZE, (SEEK_BAR_MIN + progress) / 1000f);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            onProgressChanged(mSeekBar, mSeekBar.getProgress(), true);
        } else {
            restoreOldState();
        }
    }

    private void restoreOldState() {
        if (mRestoredOldState) {
            return;
        }

        Settings.System.putFloat(getContext().getContentResolver(),
                Settings.System.PIE_SIZE, mOldSize);

        mRestoredOldState = true;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (getDialog() == null || !getDialog().isShowing()) {
            return superState;
        }

        // Save the dialog state
        final SavedState myState = new SavedState(superState);
        myState.oldSize = mOldSize;

        // Restore the old state when the activity or dialog is being paused
        restoreOldState();
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
        mOldSize = myState.oldSize;
    }

    private static class SavedState extends BaseSavedState {

        float oldSize;

        public SavedState(Parcel source) {
            super(source);
            oldSize = source.readFloat();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeFloat(oldSize);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {

            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // nothing to do
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // nothing to do
    }

}
