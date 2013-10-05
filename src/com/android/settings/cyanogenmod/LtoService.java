/*
 * Copyright (C) 2013 The CyanogenMod Project
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

package com.android.settings.cyanogenmod;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.settings.location.LocationSettings;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.cyanogenmod.hardware.LongTermOrbits;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

public class LtoService extends Service {
    private static final String TAG = "LtoService";
    private static final boolean ALOGV = true;

    private static final String KEY_LAST_DOWNLOAD = "lto_last_download";

    public static final String ACTION_NEW_GPS_DATA = "com.cyanogenmod.actions.NEW_GPS_DATA";

    private static final int DOWNLOAD_TIMEOUT = 20000; /* 20 seconds */

    private LtoDownloadTask mTask;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!LongTermOrbits.isSupported()) {
            if (ALOGV) Log.v(TAG, "LTO is not supported by this device");
            return START_NOT_STICKY;
        }
        if (!LocationSettings.isLocationModeEnabled(this)) {
            if (ALOGV) Log.v(TAG, "Location mode not enabled in this device");
            return START_NOT_STICKY;
        }

        if (mTask != null && mTask.getStatus() != AsyncTask.Status.FINISHED) {
            if (ALOGV) Log.v(TAG, "LTO download is still active, not starting new download");
            return START_REDELIVER_INTENT;
        }

        if (!shouldDownload()) {
            Log.d(TAG, "Service started, but shouldn't download ... stopping");
            stopSelf();
            return START_NOT_STICKY;
        }

        mTask = new LtoDownloadTask(
                LongTermOrbits.getSourceLocation(), LongTermOrbits.getDestinationLocation());
        mTask.execute();

        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (mTask != null && mTask.getStatus() != AsyncTask.Status.FINISHED) {
            mTask.cancel(true);
            mTask = null;
        }
    }

    private boolean shouldDownload() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();

        if (info == null || !info.isConnected()) {
            if (ALOGV) Log.v(TAG, "No network connection is available for LTO download");
        } else {
            boolean wifiOnly = prefs.getBoolean(
                    LocationSettings.KEY_GPS_DOWNLOAD_DATA_WIFI_ONLY, true);
            if (wifiOnly && info.getType() != ConnectivityManager.TYPE_WIFI) {
                if (ALOGV) {
                    Log.v(TAG, "Active network is of type " +
                            info.getTypeName() + ", but Wifi only was selected");
                }
                return false;
            }
        }

        long now = System.currentTimeMillis();
        long lastDownload = getLastDownload();
        long due = lastDownload + LongTermOrbits.getDownloadInterval();

        if (ALOGV) {
            Log.v(TAG, "Now " + now + " due " + due + "(" + new Date(due) + ")");
        }

        if (lastDownload != 0 && now < due) {
            if (ALOGV) Log.v(TAG, "LTO download is not due yet");
            return false;
        }

        return true;
    }

    private class LtoDownloadTask extends AsyncTask<Void, Integer, Integer> {
        private String mSource;
        private File mDestination;
        private File mTempFile;
        private WakeLock mWakeLock;

        private static final int RESULT_SUCCESS = 0;
        private static final int RESULT_FAILURE = 1;
        private static final int RESULT_CANCELLED = 2;

        public LtoDownloadTask(String source, File destination) {
            mSource = source;
            mDestination = destination;
            try {
                mTempFile = File.createTempFile("lto-download", null, getCacheDir());
            } catch (IOException e) {
                Log.w(TAG, "Could not create temporary file", e);
            }

            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        }

        @Override
        protected void onPreExecute() {
            mWakeLock.acquire();
        }

        @Override
        protected Integer doInBackground(Void... params) {
            BufferedInputStream in = null;
            BufferedOutputStream out = null;
            int result = RESULT_SUCCESS;

            try {
                HttpParams httpParams = new BasicHttpParams();
                HttpConnectionParams.setConnectionTimeout(httpParams, DOWNLOAD_TIMEOUT);
                HttpConnectionParams.setSoTimeout(httpParams, DOWNLOAD_TIMEOUT);

                HttpClient client = new DefaultHttpClient(httpParams);
                HttpGet request = new HttpGet();
                request.setURI(new URI(mSource));

                HttpResponse response = client.execute(request);
                HttpEntity entity = response.getEntity();
                File outputFile = mTempFile != null ? mTempFile : mDestination;

                in = new BufferedInputStream(entity.getContent());
                out = new BufferedOutputStream(new FileOutputStream(outputFile));

                byte[] buffer = new byte[2048];
                int count, total = 0;
                long length = entity.getContentLength();

                while ((count = in.read(buffer, 0, buffer.length)) != -1) {
                    if (isCancelled()) {
                        result = RESULT_CANCELLED;
                        break;
                    }
                    out.write(buffer, 0, count);
                    total += count;

                    if (length > 0) {
                        float progress = (float) total * 100 / length;
                        publishProgress((int) progress);
                    }
                }

                Log.d(TAG, "Downloaded " + total + "/" + length + " bytes of LTO data");
                if (total == 0 || (length > 0 && total != length)) {
                    result = RESULT_FAILURE;
                }
                in.close();
                out.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed downloading LTO data", e);
                result = RESULT_FAILURE;
            } catch (URISyntaxException e) {
                Log.e(TAG, "URI syntax wrong", e);
                result = RESULT_FAILURE;
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            Log.d(TAG, "return " + result);
            return result;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result != null) {
                finish(result);
            }
        }

        @Override
        protected void onCancelled() {
            finish(RESULT_CANCELLED);
        }

        private void finish(int result) {
            final Context context = LtoService.this;

            if (mTempFile != null) {
                if (result == RESULT_SUCCESS) {
                    mDestination.delete();
                    if (!mTempFile.renameTo(mDestination)) {
                        Log.w(TAG, "Could not move temporary file to destination");
                    } else {
                        mDestination.setReadable(true, false);
                    }
                }
                mTempFile.delete();
            } else if (result != RESULT_SUCCESS) {
                mDestination.delete();
            } else {
                mDestination.setReadable(true, false);
            }

            if (result == RESULT_SUCCESS) {
                long now = System.currentTimeMillis();
                SharedPreferences.Editor editor =
                        PreferenceManager.getDefaultSharedPreferences(context).edit();
                editor.putLong(KEY_LAST_DOWNLOAD, now);
                editor.apply();
                scheduleNextDownload(now);
                notifyNewGpsData();

            } else if (result == RESULT_FAILURE) {
                /* failure, schedule next download in 1 hour */
                long lastDownload = getLastDownload() + (60 * 60 * 1000);
                scheduleNextDownload(lastDownload);
            } else {
                /* cancelled, likely due to lost network - we'll get restarted
                 * when network comes back */
            }

            mWakeLock.release();
            stopSelf();
        }
    }

    private void notifyNewGpsData() {
        Intent intent = new Intent(ACTION_NEW_GPS_DATA);
        sendStickyBroadcast(intent);
    }

    private PendingIntent scheduleNextDownload(long lastDownload) {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, LtoService.class);
        PendingIntent pi = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);

        long nextLtoDownload = lastDownload + LongTermOrbits.getDownloadInterval();
        am.set(AlarmManager.RTC, nextLtoDownload, pi);
        return pi;
    }

    private long getLastDownload() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return prefs.getLong(LtoService.KEY_LAST_DOWNLOAD, 0);
    }
}
