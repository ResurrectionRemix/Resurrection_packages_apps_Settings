/*
 * Copyright (C) 2010 Daniel Nilsson
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.android.settings.notificationlight;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.InputFilter;
import android.text.InputFilter.LengthFilter;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.notificationlight.ColorPickerView.OnColorChangedListener;

import java.util.ArrayList;
import java.util.IllegalFormatException;
import java.util.Locale;

public class LightSettingsDialog extends AlertDialog implements
        ColorPickerView.OnColorChangedListener, TextWatcher, OnFocusChangeListener {

    private final static String STATE_KEY_COLOR = "LightSettingsDialog:color";
    // Minimum delay between LED notification updates
    private final static long LED_UPDATE_DELAY_MS = 250;

    private ColorPickerView mColorPicker;
    private LinearLayout mColorPanel;
    private View mLightsDialogDivider;

    private EditText mHexColorInput;
    private ColorPanelView mNewColor;
    private Spinner mPulseSpeedOn;
    private Spinner mPulseSpeedOff;
    private LayoutInflater mInflater;

    private OnColorChangedListener mListener;

    private NotificationManager mNotificationManager;

    private boolean mReadyForLed;
    private int mLedLastColor;
    private int mLedLastSpeedOn;
    private int mLedLastSpeedOff;

    /**
     * @param context
     * @param initialColor
     * @param initialSpeedOn
     * @param initialSpeedOff
     */
    protected LightSettingsDialog(Context context, int initialColor, int initialSpeedOn,
            int initialSpeedOff) {
        super(context);

        init(context, initialColor, initialSpeedOn, initialSpeedOff, true);
    }

    /**
     * @param context
     * @param initialColor
     * @param initialSpeedOn
     * @param initialSpeedOff
     * @param onOffChangeable
     */
    protected LightSettingsDialog(Context context, int initialColor, int initialSpeedOn,
            int initialSpeedOff, boolean onOffChangeable) {
        super(context);

        init(context, initialColor, initialSpeedOn, initialSpeedOff, onOffChangeable);
    }

    private void init(Context context, int color, int speedOn, int speedOff,
            boolean onOffChangeable) {
        mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        mReadyForLed = false;
        mLedLastColor = 0;

        // To fight color banding.
        getWindow().setFormat(PixelFormat.RGBA_8888);
        setUp(color, speedOn, speedOff, onOffChangeable);
    }

    /**
     * This function sets up the dialog with the proper values.  If the speedOff parameters
     * has a -1 value disable both spinners
     *
     * @param color - the color to set
     * @param speedOn - the flash time in ms
     * @param speedOff - the flash length in ms
     */
    private void setUp(int color, int speedOn, int speedOff, boolean onOffChangeable) {
        mInflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = mInflater.inflate(R.layout.dialog_light_settings, null);

        mColorPicker = (ColorPickerView) layout.findViewById(R.id.color_picker_view);
        mColorPanel = (LinearLayout) layout.findViewById(R.id.color_panel_view);
        mHexColorInput = (EditText) layout.findViewById(R.id.hex_color_input);
        mNewColor = (ColorPanelView) layout.findViewById(R.id.color_panel);
        mLightsDialogDivider = (View) layout.findViewById(R.id.lights_dialog_divider);
        mPulseSpeedOn = (Spinner) layout.findViewById(R.id.on_spinner);
        mPulseSpeedOff = (Spinner) layout.findViewById(R.id.off_spinner);

        mColorPicker.setOnColorChangedListener(this);
        mColorPicker.setColor(color, true);

        mHexColorInput.setOnFocusChangeListener(this);

        if (onOffChangeable) {
            PulseSpeedAdapter pulseSpeedAdapter = new PulseSpeedAdapter(
                    R.array.notification_pulse_length_entries,
                    R.array.notification_pulse_length_values,
                    speedOn);
            mPulseSpeedOn.setAdapter(pulseSpeedAdapter);
            mPulseSpeedOn.setSelection(pulseSpeedAdapter.getTimePosition(speedOn));
            mPulseSpeedOn.setOnItemSelectedListener(mPulseSelectionListener);

            pulseSpeedAdapter = new PulseSpeedAdapter(R.array.notification_pulse_speed_entries,
                    R.array.notification_pulse_speed_values,
                    speedOff);
            mPulseSpeedOff.setAdapter(pulseSpeedAdapter);
            mPulseSpeedOff.setSelection(pulseSpeedAdapter.getTimePosition(speedOff));
            mPulseSpeedOff.setOnItemSelectedListener(mPulseSelectionListener);
        } else {
            View speedSettingsGroup = layout.findViewById(R.id.speed_title_view);
            speedSettingsGroup.setVisibility(View.GONE);
        }

        mPulseSpeedOn.setEnabled(onOffChangeable);
        mPulseSpeedOff.setEnabled((speedOn != 1) && onOffChangeable);

        setView(layout);
        setTitle(R.string.edit_light_settings);

        if (!getContext().getResources().getBoolean(
                com.android.internal.R.bool.config_multiColorNotificationLed)) {
            mColorPicker.setVisibility(View.GONE);
            mColorPanel.setVisibility(View.GONE);
            mLightsDialogDivider.setVisibility(View.GONE);
        }

        mReadyForLed = true;
        updateLed();
    }

    private AdapterView.OnItemSelectedListener mPulseSelectionListener =
            new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (parent == mPulseSpeedOn) {
                mPulseSpeedOff.setEnabled(mPulseSpeedOn.isEnabled() && getPulseSpeedOn() != 1);
            }
            updateLed();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    @Override
    public Bundle onSaveInstanceState() {
        Bundle state = super.onSaveInstanceState();
        state.putInt(STATE_KEY_COLOR, getColor());
        return state;
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        mColorPicker.setColor(state.getInt(STATE_KEY_COLOR), true);
    }

    @Override
    public void onStop() {
        super.onStop();
        dismissLed();
    }

    @Override
    public void onStart() {
        super.onStart();
        updateLed();
    }

    @Override
    public void onColorChanged(int color) {
        final boolean hasAlpha = mColorPicker.isAlphaSliderVisible();
        final String format = hasAlpha ? "%08x" : "%06x";
        final int mask = hasAlpha ? 0xFFFFFFFF : 0x00FFFFFF;

        mNewColor.setColor(color);
        mHexColorInput.setText(String.format(Locale.US, format, color & mask));

        if (mListener != null) {
            mListener.onColorChanged(color);
        }

        updateLed();
    }

    public void setAlphaSliderVisible(boolean visible) {
        mHexColorInput.setFilters(new InputFilter[] { new InputFilter.LengthFilter(visible ? 8 : 6) } );
        mColorPicker.setAlphaSliderVisible(visible);
    }

    public int getColor() {
        return mColorPicker.getColor();
    }

    @SuppressWarnings("unchecked")
    public int getPulseSpeedOn() {
        if (mPulseSpeedOn.isEnabled()) {
            return ((Pair<String, Integer>) mPulseSpeedOn.getSelectedItem()).second;
        } else {
            return 1;
        }
    }

    @SuppressWarnings("unchecked")
    public int getPulseSpeedOff() {
        // return 0 if 'Always on' is selected
        return getPulseSpeedOn() == 1 ? 0 : ((Pair<String, Integer>) mPulseSpeedOff.getSelectedItem()).second;
    }

    private Handler mLedHandler = new Handler() {
        public void handleMessage(Message msg) {
            updateLed();
        }
    };

    private void updateLed() {
        if (!mReadyForLed) {
            return;
        }

        final int color = getColor() & 0xFFFFFF;
        final int speedOn, speedOff;
        if (mPulseSpeedOn.isEnabled()) {
            speedOn = getPulseSpeedOn();
            speedOff = getPulseSpeedOff();
        } else {
            speedOn = 1;
            speedOff = 0;
        }

        if (mLedLastColor == color && mLedLastSpeedOn == speedOn
                && mLedLastSpeedOff == speedOff) {
            return;
        }

        // Dampen rate of consecutive LED changes
        if (mLedHandler.hasMessages(0)) {
            return;
        }
        mLedHandler.sendEmptyMessageDelayed(0, LED_UPDATE_DELAY_MS);

        mLedLastColor = color;
        mLedLastSpeedOn = speedOn;
        mLedLastSpeedOff = speedOff;

        final Bundle b = new Bundle();
        b.putBoolean(Notification.EXTRA_FORCE_SHOW_LIGHTS, true);

        final Notification.Builder builder = new Notification.Builder(getContext());
        builder.setLights(color, speedOn, speedOff);
        builder.setExtras(b);

        mNotificationManager.notify(1, builder.build());
    }

    public void dismissLed() {
        mNotificationManager.cancel(1);
        // ensure we later reset LED if dialog is
        // hidden and then made visible
        mLedLastColor = 0;
    }

    class PulseSpeedAdapter extends BaseAdapter implements SpinnerAdapter {
        private ArrayList<Pair<String, Integer>> times;

        public PulseSpeedAdapter(int timeNamesResource, int timeValuesResource) {
            times = new ArrayList<Pair<String, Integer>>();

            String[] time_names = getContext().getResources().getStringArray(timeNamesResource);
            String[] time_values = getContext().getResources().getStringArray(timeValuesResource);

            for(int i = 0; i < time_values.length; ++i) {
                times.add(new Pair<String, Integer>(time_names[i], Integer.decode(time_values[i])));
            }

        }

        /**
         * This constructor apart from taking a usual time entry array takes the
         * currently configured time value which might cause the addition of a
         * "Custom" time entry in the spinner in case this time value does not
         * match any of the predefined ones in the array.
         *
         * @param timeNamesResource The time entry names array
         * @param timeValuesResource The time entry values array
         * @param customTime Current time value that might be one of the
         *            predefined values or a totally custom value
         */
        public PulseSpeedAdapter(int timeNamesResource, int timeValuesResource, Integer customTime) {
            this(timeNamesResource, timeValuesResource);

            // Check if we also need to add the custom value entry
            if (getTimePosition(customTime) == -1) {
                times.add(new Pair<String, Integer>(getContext().getResources()
                        .getString(R.string.custom_time), customTime));
            }
        }

        /**
         * Will return the position of the spinner entry with the specified
         * time. Returns -1 if there is no such entry.
         *
         * @param time Time in ms
         * @return Position of entry with given time or -1 if not found.
         */
        public int getTimePosition(Integer time) {
            for (int position = 0; position < getCount(); ++position) {
                if (getItem(position).second.equals(time)) {
                    return position;
                }
            }

            return -1;
        }

        @Override
        public int getCount() {
            return times.size();
        }

        @Override
        public Pair<String, Integer> getItem(int position) {
            return times.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            if (view == null) {
                view = mInflater.inflate(R.layout.pulse_time_item, parent, false);
            }

            Pair<String, Integer> entry = getItem(position);
            ((TextView) view.findViewById(R.id.textViewName)).setText(entry.first);

            return view;
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
        String hexColor = mHexColorInput.getText().toString();
        if (!hexColor.isEmpty()) {
            try {
                int color = Color.parseColor('#' + hexColor);
                if (!mColorPicker.isAlphaSliderVisible()) {
                    color |= 0xFF000000; // set opaque
                }
                mColorPicker.setColor(color);
                mNewColor.setColor(color);
                updateLed();
                if (mListener != null) {
                    mListener.onColorChanged(color);
                }
            } catch (IllegalArgumentException ex) {
                // Number format is incorrect, ignore
            }
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) {
            mHexColorInput.removeTextChangedListener(this);
            InputMethodManager inputMethodManager = (InputMethodManager) getContext()
                    .getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
        } else {
            mHexColorInput.addTextChangedListener(this);
        }
    }
}
