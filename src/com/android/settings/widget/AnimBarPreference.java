package com.android.settings.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import com.android.settings.R;

public class AnimBarPreference extends Preference
        implements OnSeekBarChangeListener {

    public static int maximum = 100;
    public static int interval = 1;

    private Context mContext;

    private TextView monitorBox;
    private SeekBar bar;

    int defaultValue = 60;

    private OnPreferenceChangeListener changer;

    public AnimBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        mContext = getContext();
        View layout = View.inflate(mContext, R.layout.slider_preference, null);

        monitorBox = (TextView) layout.findViewById(R.id.monitor_box);
        bar = (SeekBar) layout.findViewById(R.id.seek_bar);
        bar.setOnSeekBarChangeListener(this);
        bar.setProgress(defaultValue);

        return layout;
    }

    public void setInitValue(int progress) {
        defaultValue = progress;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        // TODO Auto-generated method stub
        return super.onGetDefaultValue(a, index);
    }

    @Override
    public void setOnPreferenceChangeListener(
            OnPreferenceChangeListener onPreferenceChangeListener) {
        changer = onPreferenceChangeListener;
        super.setOnPreferenceChangeListener(onPreferenceChangeListener);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        progress = Math.round(((float) progress) / interval) * interval;
        seekBar.setProgress(progress);
        if (progress > 0) {
            monitorBox.setText(String.valueOf(progress * 15) + "ms");
        } else {
            monitorBox.setText(mContext.getResources().getString(R.string.animation_duration_default));
        }
        changer.onPreferenceChange(this, Integer.toString(progress));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

}
