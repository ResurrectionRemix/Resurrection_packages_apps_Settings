/*
 * Copyright (C) 2013 Android Open Kang Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;

import com.android.settings.R;

public class AlarmService extends Service {

    private MediaPlayer mMediaPlayer;
    private AudioManager mAudioManager;
    private int mAlarmVolumeSetting;
    private boolean mPlaying = false;

    @Override
    public void onCreate() {
        mAudioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public void onDestroy() {
        stopAlarm();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            startAlarmSound();
        } catch (Exception e) {
            // Do nothing
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void startAlarmSound()
            throws java.io.IOException, IllegalArgumentException, IllegalStateException {

        Uri alertSound = SmsCallHelper.returnUserRingtone(this);

        if (mPlaying) {
            stopAlarm();
        } else {
            mAlarmVolumeSetting = mAudioManager.getStreamVolume(AudioManager.STREAM_ALARM);
        }

        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnErrorListener(new OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    mp.stop();
                    mp.release();
                    mMediaPlayer = null;
                    return true;
                }
            });
        }

        mAudioManager.setStreamVolume(AudioManager.STREAM_ALARM,
                mAudioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);

        mMediaPlayer.setDataSource(this, alertSound);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);

        if (SmsCallHelper.returnUserRingtoneLoop(this)) {
            mMediaPlayer.setLooping(true);
        } else {
            mMediaPlayer.setLooping(false);
            mMediaPlayer.setOnSeekCompleteListener(stopSelf);
        }
        mMediaPlayer.prepare();
        mMediaPlayer.start();
        mPlaying = true;
    }

    public void stopAlarm() {
        if (mPlaying) {

            if (mMediaPlayer != null) {
                mMediaPlayer.stop();
                mMediaPlayer.release();
                mMediaPlayer = null;
                mAudioManager.setStreamVolume(AudioManager.STREAM_ALARM,
                        mAlarmVolumeSetting, 0);
            }

            mPlaying = false;
        }
    }

    final OnSeekCompleteListener stopSelf = new OnSeekCompleteListener() {
        public void onSeekComplete(MediaPlayer mp) {
            stopSelf();
        }
    };
}
