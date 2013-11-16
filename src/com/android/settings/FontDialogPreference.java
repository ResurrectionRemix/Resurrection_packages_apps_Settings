package com.android.settings;

import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.RemoteException;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

public class FontDialogPreference extends DialogPreference
    implements SeekBar.OnSeekBarChangeListener {

    private TextView mDescriptionText;
    private TextView mPercentageText;
    private IntervalSeekBar mSeekBar;

    private DisplayMetrics mDisplayMetrics;
    private int mLargeTextSp;
    private int mSmallTextSp;

    public FontDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);

        initDisplayMetrics();

        setDialogLayoutResource(R.layout.preference_dialog_fontsize);
        setDialogTitle(null); // Hide the title bar
    }

    @Override
    protected View onCreateDialogView() {
        LayoutInflater inflater =
                (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.preference_dialog_fontsize, null);

        mDescriptionText = (TextView) view.findViewById(R.id.description);
        mPercentageText = (TextView) view.findViewById(R.id.percentage);

        // Calculate original sp sizes for the text views
        mLargeTextSp = Math.round(mDescriptionText.getTextSize() / mDisplayMetrics.scaledDensity);
        mSmallTextSp = Math.round(mPercentageText.getTextSize() / mDisplayMetrics.scaledDensity);

        mSeekBar = (IntervalSeekBar) view.findViewById(R.id.font_size);

        String strFontSize = getPersistedString(String.valueOf(mSeekBar.getDefault()));
        float fontSize = Float.parseFloat(strFontSize);

        mSeekBar.setProgressFloat(fontSize);
        mSeekBar.setOnSeekBarChangeListener(this);

        setPrompt(fontSize);

        return view;
    }

    private void initDisplayMetrics() {
        DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
        mDisplayMetrics = new DisplayMetrics();
        mDisplayMetrics.density = metrics.density;
        mDisplayMetrics.heightPixels = metrics.heightPixels;
        mDisplayMetrics.scaledDensity = metrics.scaledDensity;
        mDisplayMetrics.widthPixels = metrics.widthPixels;
        mDisplayMetrics.xdpi = metrics.xdpi;
        mDisplayMetrics.ydpi = metrics.ydpi;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            // Notify the Display settings screen (parent) that the font size
            // is about to change. This can determine whether to persist the
            // current value
            if (callChangeListener(mSeekBar.getProgressFloat())) {
                // Originally font scaling was a float stored as a String,
                // so using persistFloat raises a ClassCastException
                persistString(Float.toString(mSeekBar.getProgressFloat()));
            }
        }
    }

    @Override
    protected void onClick() {
        // Ignore this until an explicit call to click()
    }

    public void click() {
        super.onClick();
    }

    /**
     * Get an approximate description for the font size scale.
     *  Assumes that the string arrays entries_font_size and
     *  entryvalues_font_size have the same length and correspond to each other
     *  i.e. they are in the same order.
     */
    static String getFontSizeDescription(Resources r, float val) {
        String[] names = r.getStringArray(R.array.entries_font_size);
        String[] indices = r.getStringArray(R.array.entryvalues_font_size);

        float lastVal = Float.parseFloat(indices[0]);
        for (int i = 1; i < indices.length; i++) {
            float thisVal = Float.parseFloat(indices[i]);
            if (val < (lastVal + (thisVal-lastVal)*.5f)) {
                return names[i - 1];
            }
            lastVal = thisVal;
        }
        return names[indices.length - 1];
    }

    /**
     * Set the TextView indicating the font scaling
     */
    private void setPrompt(float fontScaling) {
        // Update the preview text
        String percentage = Math.round(fontScaling * 100) + "%";
        mPercentageText.setText(percentage);

        // Update the preview sizes
        mDisplayMetrics.scaledDensity = mDisplayMetrics.density * fontScaling;
        float largeSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, mLargeTextSp,
                mDisplayMetrics);
        float smallSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, mSmallTextSp,
                mDisplayMetrics);
        mDescriptionText.setTextSize(TypedValue.COMPLEX_UNIT_PX, largeSize);
        mPercentageText.setTextSize(TypedValue.COMPLEX_UNIT_PX, smallSize);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        setPrompt(mSeekBar.getProgressFloat());
    }

    // Not used
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}
