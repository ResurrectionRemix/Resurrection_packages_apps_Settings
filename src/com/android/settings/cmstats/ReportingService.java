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
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.util.Log;
import cyanogenmod.providers.CMSettings;

import java.util.List;

public class ReportingService extends IntentService {
    /* package */ static final String TAG = "CMStats";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    public static final String EXTRA_OPTING_OUT = "cmstats::opt_out";

    public ReportingService() {
        super(ReportingService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        JobScheduler js = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);

        String deviceId = Utilities.getUniqueID(getApplicationContext());
        String deviceName = Utilities.getDevice();
        String deviceVersion = Utilities.getModVersion();
        String deviceCountry = Utilities.getCountryCode(getApplicationContext());
        String deviceCarrier = Utilities.getCarrier(getApplicationContext());
        String deviceCarrierId = Utilities.getCarrierId(getApplicationContext());
        boolean optOut = intent.getBooleanExtra(EXTRA_OPTING_OUT, false);

        final int cyanogenJobId = AnonymousStats.getNextJobId(getApplicationContext());
        final int cmOrgJobId = AnonymousStats.getNextJobId(getApplicationContext());

        if (DEBUG) Log.d(TAG, "scheduling jobs id: " + cyanogenJobId + ", " + cmOrgJobId);

        PersistableBundle cyanogenBundle = new PersistableBundle();
        cyanogenBundle.putBoolean(StatsUploadJobService.KEY_OPT_OUT, optOut);
        cyanogenBundle.putString(StatsUploadJobService.KEY_DEVICE_NAME, deviceName);
        cyanogenBundle.putString(StatsUploadJobService.KEY_UNIQUE_ID, deviceId);
        cyanogenBundle.putString(StatsUploadJobService.KEY_VERSION, deviceVersion);
        cyanogenBundle.putString(StatsUploadJobService.KEY_COUNTRY, deviceCountry);
        cyanogenBundle.putString(StatsUploadJobService.KEY_CARRIER, deviceCarrier);
        cyanogenBundle.putString(StatsUploadJobService.KEY_CARRIER_ID, deviceCarrierId);
        cyanogenBundle.putLong(StatsUploadJobService.KEY_TIMESTAMP, System.currentTimeMillis());

        // get snapshot and persist it
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

        if (optOut) {
            // we've successfully scheduled the opt out.
            CMSettings.Secure.putIntForUser(getContentResolver(),
                    CMSettings.Secure.STATS_COLLECTION_REPORTED, 1, UserHandle.USER_OWNER);
        }

        // reschedule
        AnonymousStats.updateLastSynced(this);
        ReportingServiceManager.setAlarm(this);
    }
}
