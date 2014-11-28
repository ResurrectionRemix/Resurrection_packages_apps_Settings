/*
 * Copyright (C) 2014 CyanogenMod Project
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

package com.android.settings.notification;

import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.preference.PreferenceManager;
import android.preference.Preference;
import android.provider.Settings;
import android.text.format.Formatter;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.settings.R;

public class IncreasingRingVolumePreference extends Preference implements
        PreferenceManager.OnActivityStopListener, Handler.Callback,
        SeekBar.OnSeekBarChangeListener {
    private static final String TAG = "IncreasingRingMinVolumePreference";

    public interface Callback {
        void onStartingSample();
    }

    private SeekBar mStartVolumeSeekBar;
    private SeekBar mRampUpTimeSeekBar;
    private TextView mRampUpTimeValue;

    private Ringtone mRingtone;
    private Callback mCallback;

    private Handler mHandler;
    private final Handler mMainHandler = new Handler(this);

    private static final int MSG_START_SAMPLE = 1;
    private static final int MSG_STOP_SAMPLE = 2;
    private static final int MSG_INIT_SAMPLE = 3;
    private static final int CHECK_RINGTONE_PLAYBACK_DELAY_MS = 1000;

    public IncreasingRingVolumePreference(Context context) {
        this(context, null);
    }

    public IncreasingRingVolumePreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IncreasingRingVolumePreference(Context context, AttributeSet attrs,
            int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public IncreasingRingVolumePreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.preference_increasing_ring);
        initHandler();
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    @Override
    public void onActivityStop() {
        if (mHandler != null) {
            postStopSample();
            mHandler.getLooper().quitSafely();
            mHandler = null;
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_START_SAMPLE:
                onStartSample();
                break;
            case MSG_STOP_SAMPLE:
                onStopSample();
                break;
            case MSG_INIT_SAMPLE:
                onInitSample();
                break;
        }
        return true;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        getPreferenceManager().registerOnActivityStopListener(this);

        initHandler();

        final SeekBar seekBar = (SeekBar) view.findViewById(R.id.start_volume);
        if (seekBar == mStartVolumeSeekBar) return;

        mStartVolumeSeekBar = seekBar;
        mRampUpTimeSeekBar = (SeekBar) view.findViewById(R.id.ramp_up_time);
        mRampUpTimeValue = (TextView) view.findViewById(R.id.ramp_up_time_value);

        final ContentResolver cr = getContext().getContentResolver();
        float startVolume = Settings.System.getFloat(cr,
                Settings.System.INCREASING_RING_START_VOLUME, 0.1f);
        int rampUpTime = Settings.System.getInt(cr,
                Settings.System.INCREASING_RING_RAMP_UP_TIME, 10);

        mStartVolumeSeekBar.setProgress(Math.round(startVolume * 1000F));
        mStartVolumeSeekBar.setOnSeekBarChangeListener(this);
        mRampUpTimeSeekBar.setOnSeekBarChangeListener(this);
        mRampUpTimeSeekBar.setProgress((rampUpTime / 5) - 1);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        if (seekBar == mStartVolumeSeekBar && mCallback != null) {
            mCallback.onStartingSample();
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (seekBar == mStartVolumeSeekBar) {
            postStartSample();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
        ContentResolver cr = getContext().getContentResolver();
        if (fromTouch && seekBar == mStartVolumeSeekBar) {
            Settings.System.putFloat(cr, Settings.System.INCREASING_RING_START_VOLUME,
                        (float) progress / 1000F);
        } else if (seekBar == mRampUpTimeSeekBar) {
            int seconds = (progress + 1) * 5;
            mRampUpTimeValue.setText(
                    Formatter.formatShortElapsedTime(getContext(), seconds * 1000));
            if (fromTouch) {
                Settings.System.putInt(cr, Settings.System.INCREASING_RING_RAMP_UP_TIME, seconds);
            }
        }
    }

    private void initHandler() {
        if (mHandler != null) return;

        HandlerThread thread = new HandlerThread(TAG + ".CallbackHandler");
        thread.start();

        mHandler = new Handler(thread.getLooper(), this);
        mHandler.sendEmptyMessage(MSG_INIT_SAMPLE);
    }

    private void onInitSample() {
        mRingtone = RingtoneManager.getRingtone(getContext(),
                Settings.System.DEFAULT_RINGTONE_URI);
        if (mRingtone != null) {
            mRingtone.setStreamType(AudioManager.STREAM_RING);
        }
    }

    private void postStartSample() {
        mHandler.removeMessages(MSG_START_SAMPLE);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_START_SAMPLE),
                isSamplePlaying() ? CHECK_RINGTONE_PLAYBACK_DELAY_MS : 0);
    }

    private void onStartSample() {
        if (!isSamplePlaying() && mRingtone != null) {
            try {
                mRingtone.play();
            } catch (Throwable e) {
                Log.w(TAG, "Error playing ringtone", e);
            }
            mHandler.removeMessages(MSG_STOP_SAMPLE);
            mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_STOP_SAMPLE), 2000);
        }
    }

    private boolean isSamplePlaying() {
        return mRingtone != null && mRingtone.isPlaying();
    }

    public void stopSample() {
        if (mHandler != null) {
            postStopSample();
        }
    }

    private void postStopSample() {
        // remove pending delayed start messages
        mHandler.removeMessages(MSG_START_SAMPLE);
        mHandler.removeMessages(MSG_STOP_SAMPLE);
        mHandler.sendEmptyMessage(MSG_STOP_SAMPLE);
    }

    private void onStopSample() {
        if (mRingtone != null) {
            mRingtone.stop();
        }
    }
}
