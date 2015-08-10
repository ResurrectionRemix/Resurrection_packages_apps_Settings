/*
 * Copyright (C) 2015 The CyanogenMod Project
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

package com.android.settings.cmstats;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.PersistableBundle;
import android.util.ArrayMap;
import android.util.Log;
import com.android.settings.R;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

public class StatsUploadJobService extends JobService {

    private static final String TAG = StatsUploadJobService.class.getSimpleName();
    private static final boolean DEBUG = false;

    public static final String KEY_JOB_TYPE = "job_type";
    public static final int JOB_TYPE_CYANOGEN = 1;
    public static final int JOB_TYPE_CMORG = 2;

    public static final String KEY_UNIQUE_ID = "uniqueId";
    public static final String KEY_DEVICE_NAME = "deviceName";
    public static final String KEY_VERSION = "version";
    public static final String KEY_COUNTRY = "country";
    public static final String KEY_CARRIER = "carrier";
    public static final String KEY_CARRIER_ID = "carrierId";
    public static final String KEY_TIMESTAMP = "timeStamp";

    private final Map<JobParameters, StatsUploadTask> mCurrentJobs
            = Collections.synchronizedMap(new ArrayMap<JobParameters, StatsUploadTask>());

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        if (DEBUG)
            Log.d(TAG, "onStartJob() called with " + "jobParameters = [" + jobParameters + "]");
        final StatsUploadTask uploadTask = new StatsUploadTask(jobParameters);
        mCurrentJobs.put(jobParameters, uploadTask);
        uploadTask.execute((Void) null);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        if (DEBUG)
            Log.d(TAG, "onStopJob() called with " + "jobParameters = [" + jobParameters + "]");

        final StatsUploadTask cancelledJob;
        cancelledJob = mCurrentJobs.remove(jobParameters);

        if (cancelledJob != null) {
            // cancel the ongoing background task
            cancelledJob.cancel(true);
            return true; // reschedule
        }

        return false;
    }

    private class StatsUploadTask extends AsyncTask<Void, Void, Void> {

        private JobParameters mJobParams;

        public StatsUploadTask(JobParameters jobParams) {
            this.mJobParams = jobParams;
        }

        @Override
        protected Void doInBackground(Void... params) {

            PersistableBundle extras = mJobParams.getExtras();

            String deviceId = extras.getString(KEY_UNIQUE_ID);
            String deviceName = extras.getString(KEY_DEVICE_NAME);
            String deviceVersion = extras.getString(KEY_VERSION);
            String deviceCountry = extras.getString(KEY_COUNTRY);
            String deviceCarrier = extras.getString(KEY_CARRIER);
            String deviceCarrierId = extras.getString(KEY_CARRIER_ID);
            long timeStamp = extras.getLong(KEY_TIMESTAMP);

            boolean success = false;
            if (!isCancelled()) {
                int jobType = extras.getInt(KEY_JOB_TYPE, -1);

                switch (jobType) {
                    case JOB_TYPE_CYANOGEN:
                        try {
                            success = uploadToCyanogen(deviceId, deviceName, deviceVersion,
                                    deviceCountry, deviceCarrier, deviceCarrierId, timeStamp);
                        } catch (IOException | JSONException e) {
                            Log.e(TAG, "Could not upload stats checkin to cyanogen server", e);
                            success = false;
                        }
                        break;

                    case JOB_TYPE_CMORG:
                        try {
                            success = uploadToCM(deviceId, deviceName, deviceVersion, deviceCountry,
                                    deviceCarrier, deviceCarrierId);
                        } catch (IOException e) {
                            Log.e(TAG, "Could not upload stats checkin to commnity server", e);
                            success = false;
                        }
                        break;
                }
            }

            if (success) {
                // we hit the server, succeed either which way.
                mCurrentJobs.remove(mJobParams);
                AnonymousStats.removeJob(StatsUploadJobService.this, mJobParams.getJobId());
            }

            if (DEBUG)
                Log.d(TAG, "job id " + mJobParams.getJobId() + ", has finished with success="
                        + success);
            jobFinished(mJobParams, !success);
            return null;
        }
    }


    private boolean uploadToCM(String deviceId, String deviceName, String deviceVersion,
                               String deviceCountry, String deviceCarrier, String deviceCarrierId)
            throws IOException {

        final Uri uri = Uri.parse(getString(R.string.stats_cm_url)).buildUpon()
                .appendQueryParameter("device_hash", deviceId)
                .appendQueryParameter("device_name", deviceName)
                .appendQueryParameter("device_version", deviceVersion)
                .appendQueryParameter("device_country", deviceCountry)
                .appendQueryParameter("device_carrier", deviceCarrier)
                .appendQueryParameter("device_carrier_id", deviceCarrierId).build();
        URL url = new URL(uri.toString());
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            urlConnection.setInstanceFollowRedirects(true);
            urlConnection.setDoOutput(true);
            urlConnection.connect();

            final int responseCode = urlConnection.getResponseCode();
            if (DEBUG) Log.d(TAG, "cm server response code=" + responseCode);
            final boolean success = responseCode == HttpURLConnection.HTTP_OK;
            if (!success) {
                Log.w(TAG, "failed sending, server returned: " + getResponse(urlConnection,
                        !success));
            }
            return success;
        } finally {
            urlConnection.disconnect();
        }

    }

    private boolean uploadToCyanogen(String deviceId, String deviceName, String deviceVersion,
                                     String deviceCountry, String carrier, String carrierId,
                                     long timeStamp)
            throws IOException, JSONException {
        String authToken = getAuthToken();

        if (authToken.isEmpty()) {
            Log.w(TAG, "no auth token!");
        }

        JSONObject json = new JSONObject();
        json.put("uniqueId", deviceId);
        json.put("deviceName", deviceName);
        json.put("version", deviceVersion);
        json.put("country", deviceCountry);
        json.put("carrier", carrier);
        json.put("carrierId", carrierId);
        json.put("timestamp", timeStamp);

        URL url = new URL(getString(R.string.stats_cyanogen_url));
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            urlConnection.setInstanceFollowRedirects(true);
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);

            urlConnection.setRequestProperty("Accept-Encoding", "identity");
            urlConnection.setRequestProperty("Authorization", authToken);
            urlConnection.setRequestProperty("Content-Type", "application/json");

            OutputStream os = urlConnection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(json.toString());
            writer.flush();
            writer.close();
            os.close();

            urlConnection.connect();

            final int responseCode = urlConnection.getResponseCode();
            final boolean success = responseCode == HttpURLConnection.HTTP_OK;
            if (!success) {
                Log.w(TAG, "failed sending, server returned: " + getResponse(urlConnection,
                        !success));
            }
            return success;
        } finally {
            urlConnection.disconnect();
        }
    }

    private String getAuthToken() {
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(getString(R.string.stats_cyanogen_token_url));
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setInstanceFollowRedirects(true);
            urlConnection.setDoInput(true);

            urlConnection.setRequestProperty("Accept-Encoding", "identity");
            urlConnection.setRequestProperty("Content-Type", "text/plain");

            urlConnection.connect();

            final int responseCode = urlConnection.getResponseCode();
            final boolean success = responseCode == HttpURLConnection.HTTP_OK;
            if (DEBUG) Log.d(TAG, "server auth response code=" + responseCode);
            final String response = getResponse(urlConnection, !success);
            if (DEBUG)
                Log.d(TAG, "server auth response=" + response);

            if (success) {
                return response;
            }
        } catch (IOException e) {
            Log.e(TAG, "error getting auth token", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return "";
    }

    private String getResponse(HttpURLConnection httpUrlConnection, boolean errorStream)
            throws IOException {
        InputStream responseStream = new BufferedInputStream(errorStream
                ? httpUrlConnection.getErrorStream()
                : httpUrlConnection.getInputStream());

        BufferedReader responseStreamReader = new BufferedReader(
                new InputStreamReader(responseStream));
        String line = "";
        StringBuilder stringBuilder = new StringBuilder();
        while ((line = responseStreamReader.readLine()) != null) {
            stringBuilder.append(line).append("\n");
        }
        responseStreamReader.close();
        responseStream.close();

        return stringBuilder.toString();
    }

}
