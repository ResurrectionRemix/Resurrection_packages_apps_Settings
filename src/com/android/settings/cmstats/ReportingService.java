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

import android.app.IntentService;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PersistableBundle;
import android.util.Log;

import java.util.List;

public class ReportingService extends IntentService {
    /* package */ static final String TAG = "CMStats";
    private static final boolean DEBUG = false;

    public ReportingService() {
        super(ReportingService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        JobScheduler js = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);

        if (AnonymousStats.getNextJobId(this) == -1) {
            // if we've filled up to the threshold, we may have some stale job queue ids, purge them
            // then re-add what hasn't executed yet
            AnonymousStats.clearJobQueue(this);

            final List<JobInfo> allPendingJobs = js.getAllPendingJobs();

            // add two extra jobs to the size for what we will schedule below so we *always*
            // have room for both.
            if (js.getAllPendingJobs().size() + 2 >= AnonymousStats.QUEUE_MAX_THRESHOLD) {
                // there are still as many actual pending jobs as our threshold allows.
                // since we are past the threshold we will be losing data if we don't schedule
                // another job here, so just clear out all the old data and start fresh
                js.cancelAll();
            } else {
                for (JobInfo pendingJob : allPendingJobs) {
                    AnonymousStats.addJob(this, pendingJob.getId());
                }
            }
        }

        int cyanogenJobId, cmOrgJobId;
        AnonymousStats.addJob(this, cyanogenJobId = AnonymousStats.getNextJobId(this));
        AnonymousStats.addJob(this, cmOrgJobId = AnonymousStats.getNextJobId(this));

        if (DEBUG) Log.d(TAG, "scheduling jobs id: " + cyanogenJobId + ", " + cmOrgJobId);

        // get snapshot and persist it
        String deviceId = Utilities.getUniqueID(getApplicationContext());
        String deviceName = Utilities.getDevice();
        String deviceVersion = Utilities.getModVersion();
        String deviceCountry = Utilities.getCountryCode(getApplicationContext());
        String deviceCarrier = Utilities.getCarrier(getApplicationContext());
        String deviceCarrierId = Utilities.getCarrierId(getApplicationContext());

        PersistableBundle cyanogenBundle = new PersistableBundle();
        cyanogenBundle.putString(StatsUploadJobService.KEY_DEVICE_NAME, deviceName);
        cyanogenBundle.putString(StatsUploadJobService.KEY_UNIQUE_ID, deviceId);
        cyanogenBundle.putString(StatsUploadJobService.KEY_VERSION, deviceVersion);
        cyanogenBundle.putString(StatsUploadJobService.KEY_COUNTRY, deviceCountry);
        cyanogenBundle.putString(StatsUploadJobService.KEY_CARRIER, deviceCarrier);
        cyanogenBundle.putString(StatsUploadJobService.KEY_CARRIER_ID, deviceCarrierId);
        cyanogenBundle.putLong(StatsUploadJobService.KEY_TIMESTAMP, System.currentTimeMillis());

        PersistableBundle cmBundle = new PersistableBundle(cyanogenBundle);

        // set job types
        cyanogenBundle.putInt(StatsUploadJobService.KEY_JOB_TYPE,
                StatsUploadJobService.JOB_TYPE_CYANOGEN);
        cmBundle.putInt(StatsUploadJobService.KEY_JOB_TYPE,
                StatsUploadJobService.JOB_TYPE_CMORG);

        // schedule cyanogen stats upload
        js.schedule(new JobInfo.Builder(cyanogenJobId, new ComponentName(getPackageName(),
                StatsUploadJobService.class.getName()))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(1000)
                .setExtras(cyanogenBundle)
                .setPersisted(true)
                .build());

        // schedule cmorg stats upload
        js.schedule(new JobInfo.Builder(cmOrgJobId, new ComponentName(getPackageName(),
                StatsUploadJobService.class.getName()))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(1000)
                .setExtras(cmBundle)
                .setPersisted(true)
                .build());

        // reschedule
        final SharedPreferences prefs = AnonymousStats.getPreferences(this);
        prefs.edit().putLong(AnonymousStats.ANONYMOUS_LAST_CHECKED,
                System.currentTimeMillis()).apply();
        ReportingServiceManager.setAlarm(this, 0);
    }

}
