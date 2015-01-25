package com.android.settings.widget;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.NumberPicker;

import com.android.settings.R;

public class NumberPickerPreference extends DialogPreference {

    private NumberPicker mPicker;

    private int mMinValue;
    private int mMaxValue;
    private int mCurrentValue;

    public NumberPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.number_picker_pref_dialog);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        mPicker = (NumberPicker)view.findViewById(R.id.pref_num_picker);
        mPicker.setMinValue(mMinValue);
        mPicker.setMaxValue(mMaxValue);
        mPicker.setValue(mCurrentValue);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);
        if ( which == DialogInterface.BUTTON_POSITIVE ) {
            int value = mPicker.getValue();
            if (callChangeListener(value)) {
                mCurrentValue = value;
                persistInt(value);
            }
        }
    }

    public int getValue() {
        return mCurrentValue;
    }

    public void setMinValue(int value) {
        mMinValue = value;
    }

    public void setMaxValue(int value) {
        mMaxValue = value;
    }

    public void setCurrentValue(int value) {
        mCurrentValue = value;
    }
}
