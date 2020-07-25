/*
 * Copyright (C) 2019 Paranoid Android
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

package com.android.settings.widget;

import android.app.AppLockManager;
import android.content.Context;
import android.widget.ImageView;
import android.view.View;

import androidx.preference.CheckBoxPreference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

public class AppLockPreference extends CheckBoxPreference {

    private final AppLockManager mAppLockManager;
    private final String mPackageName;

    private ImageView mNotificationImage;
    private View mNotifFrame;

    public AppLockPreference(Context context, AppLockManager am, String pkg) {
        super(context, null);
        mAppLockManager = am;
        mPackageName = pkg;

        setLayoutResource(R.layout.preference_applock);
        setWidgetLayoutResource(R.layout.preference_widget_applock);

        setSummaryOn(R.string.applock_locked);
        setSummaryOff(R.string.applock_unlocked);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);

        mNotificationImage = (ImageView) view.findViewById(R.id.notification_img);
        mNotifFrame = (View) view.findViewById(R.id.notification_frame);
        mNotifFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean hide = mAppLockManager.getAppNotificationHide(mPackageName);
                mAppLockManager.setAppNotificationHide(mPackageName, !hide);
                mNotificationImage.setImageResource(hide ? R.drawable.ic_audio_notifications
                        : R.drawable.ic_audio_notifications_off_24dp);
                String toolTip = getContext().getString(hide ? R.string.applock_show_notif
                        : R.string.applock_hide_notif);
                v.setTooltipText(toolTip);
                v.performLongClick();
            }
        });
        boolean hiding = mAppLockManager.getAppNotificationHide(mPackageName);
        String toolTip = getContext().getString(hiding ? R.string.applock_hide_notif
                : R.string.applock_show_notif);
        mNotifFrame.setTooltipText(toolTip);
        mNotificationImage.setImageResource(hiding ? R.drawable.ic_audio_notifications_off_24dp
                : R.drawable.ic_audio_notifications);
        mNotifFrame.setVisibility(isChecked() ? View.VISIBLE : View.GONE);
    }

    public void startHintAnimation() {
        mNotifFrame.postOnAnimationDelayed(
                getSinglePressFor(mNotifFrame), 200);
    }

    private Runnable getSinglePressFor(View v) {
        return () -> {
            v.setPressed(true);
            v.postOnAnimationDelayed(getSingleUnpressFor(v), 200);
        };
    }

    private Runnable getSingleUnpressFor(View v) {
        return () -> {
            v.setPressed(false);
        };
    }
}
