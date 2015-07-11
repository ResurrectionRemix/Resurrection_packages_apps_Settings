/*
 * Copyright (C) 2015 The CyanogenMod Project
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
package com.android.settings.cmstats;

import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.util.Log;

public class FingerprintStats {
    private static final String TAG = FingerprintStats.class.getSimpleName();
    private static final boolean DEBUG = false;
    private static final String ANALYTIC_INTENT = "com.cyngn.stats.action.SEND_ANALYITC_EVENT";
    private static final String ANALYTIC_PERMISSION = "com.cyngn.stats.SEND_ANALYTICS";
    private static final String TRACKING_ID = "tracking_id";


    /**
     * Failed enrollment due to timeout of enrollment process
     */
    public static final String FAILURE_REASON_TIMEOUT = "timeout";

    /**
     * Failed enrollment due to bad fingerprint scans
     */
    public static final String FAILURE_REASON_BAD_SCAN = "bad_scan";

    /**
     * Failed enrollment due to user canceling enrollment process
     */
    public static final String FAILURE_REASON_CANCELED = "canceled";

    private FingerprintStats() {}

    public static void sendFingerprintEnrollmentFailedEvent(Context context, String category,
            String failureReason) {
        sendEvent(context, new Event(category, Actions.ACTION_ENROLLMENT_FAILED,
                Labels.LABEL_FAILURE_REASON, failureReason));
    }

    public static void sendFingerprintEnrollmentSuccessEvent(Context context, String category) {
        sendEvent(context, new Event(category, Actions.ACTION_ENROLLMENT_SUCCESS,
                null, null));
    }

    private static void sendEvent(Context context, Event event) {

        if (!StatsUtils.isStatsPackageInstalled(context)
                || !StatsUtils.isStatsCollectionEnabled(context)) {
            return;
        }

        // Create new intent
        Intent intent = new Intent();
        intent.setAction(ANALYTIC_INTENT);

        // add tracking id
        intent.putExtra(TRACKING_ID, context.getPackageName());
        // append
        intent.putExtra(Fields.EVENT_CATEGORY, event.category);
        if (DEBUG) Log.d(TAG, Fields.EVENT_CATEGORY + "=" + event.category);
        intent.putExtra(Fields.EVENT_ACTION, event.action);
        if (DEBUG) Log.d(TAG, Fields.EVENT_ACTION + "=" + event.action);
        // check if exist
        if (event.label != null) {
            intent.putExtra(Fields.EVENT_LABEL, event.label);
            if (DEBUG) Log.d(TAG, Fields.EVENT_LABEL + "=" + event.label);
        }

        if (event.value != null) {
            intent.putExtra(Fields.EVENT_VALUE, event.value);
            if (DEBUG) Log.d(TAG, Fields.EVENT_VALUE + "=" + event.value);
        }

        // broadcast for internal package
        context.sendBroadcastAsUser(intent,
                new UserHandle(UserHandle.USER_CURRENT), ANALYTIC_PERMISSION);
    }

    private static final class Event {
        private final String category;
        private final String action;
        private final String label;
        private final String value;

        public Event(String category, String action, String label, String value) {
            this.action = action;
            this.category = category;
            this.label = label;
            this.value = value;
        }
    }

    public static final class Fields {
        public static final String EVENT_CATEGORY = "category";
        public static final String EVENT_ACTION = "action";
        public static final String EVENT_LABEL = "label";
        public static final String EVENT_VALUE = "value";
    }

    public static final class Categories {
        public static final String FINGERPRINT_ENROLLMENT_SETTINGS =
                "fingerprint_enrollment_settings";
        public static final String FINGERPRINT_ENROLLMENT_OOBE =
                "fingerprint_enrollment_oobe";
    }

    public static final class Actions {
        public static final String ACTION_ENROLLMENT_SUCCESS = "enrollment_success";
        public static final String ACTION_ENROLLMENT_FAILED = "enrollment_failed";
    }

    public static final class Labels {
        public static final String LABEL_FAILURE_REASON = "failure_reason";
    }
}
