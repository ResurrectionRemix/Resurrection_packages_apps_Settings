/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Switch;

import com.nitrogen.settings.preferences.CustomSeekBarPreference;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.notification.NotificationBackend.AppRow;
import com.android.settings.widget.EntityHeaderController;
import com.android.settings.widget.MasterSwitchPreference;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.widget.FooterPreference;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

import static android.provider.Settings.System.NOTIFICATION_LIGHT_PULSE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.app.NotificationManager.IMPORTANCE_LOW;
import static android.app.NotificationManager.IMPORTANCE_NONE;
import static android.app.NotificationManager.IMPORTANCE_UNSPECIFIED;

/** These settings are per app, so should not be returned in global search results. */
public class AppNotificationSettings extends NotificationSettingsBase {
    private static final String TAG = "AppNotificationSettings";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static String KEY_GENERAL_CATEGORY = "categories";
    private static String KEY_DELETED = "deleted";

    private static final String KEY_INFO_DESC = "info_desc";
    private static final String KEY_LIGHTS = "lights";
    private static final String KEY_CUSTOM_LIGHT = "custom_light";
    private static final String KEY_LIGHTS_ON_TIME = "custom_light_on_time";
    private static final String KEY_LIGHTS_OFF_TIME = "custom_light_off_time";
    private static final String KEY_LIGHT_ON_ZEN = "show_light_on_zen";

    private List<NotificationChannelGroup> mChannelGroupList;
    private List<PreferenceCategory> mChannelGroups = new ArrayList();
    private FooterPreference mDeletedChannels;

    private PreferenceCategory mLightCategory;
    private RestrictedSwitchPreference mLights;
    private ColorPickerPreference mCustomLight;
    private CustomSeekBarPreference mLightOnTime;
    private CustomSeekBarPreference mLightOffTime;
    private SwitchPreference mLightOnZen;
    
    private int mLedColor = 0;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.NOTIFICATION_APP_NOTIFICATION;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mUid < 0 || TextUtils.isEmpty(mPkg) || mPkgInfo == null) {
            Log.w(TAG, "Missing package or uid or packageinfo");
            finish();
            return;
        }

        if (getPreferenceScreen() != null) {
            getPreferenceScreen().removeAll();
            mChannelGroups.clear();
            mDeletedChannels = null;
            mShowLegacyChannelConfig = false;
        }

        addPreferencesFromResource(R.xml.notification_settings);
        getPreferenceScreen().setOrderingAsAdded(true);
        setupBlock();
        addHeaderPref();

        mShowLegacyChannelConfig = mBackend.onlyHasDefaultChannel(mAppRow.pkg, mAppRow.uid);
        if (mShowLegacyChannelConfig) {
            mChannel = mBackend.getChannel(
                    mAppRow.pkg, mAppRow.uid, NotificationChannel.DEFAULT_CHANNEL_ID);
            populateDefaultChannelPrefs();
            mLightCategory = (PreferenceCategory) findPreference("light_customization");
            //setup lights for legacy app default channel
            setupLights();
        } else {
            addPreferencesFromResource(R.xml.upgraded_app_notification_settings);
            setupBadge();
            // Load channel settings
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... unused) {
                    mChannelGroupList = mBackend.getChannelGroups(mPkg, mUid).getList();
                    Collections.sort(mChannelGroupList, mChannelGroupComparator);
                    return null;
                }

                @Override
                protected void onPostExecute(Void unused) {
                    if (getHost() == null) {
                        return;
                    }
                    populateChannelList();
                    addAppLinkPref();
                    setupInfoDesc(R.string.app_notifications_info_desc);
                }
            }.execute();
        }

        updateDependents(mAppRow.banned);
    }

    private void setupLights() {
        //find light prefs
        mLights = (RestrictedSwitchPreference) findPreference(KEY_LIGHTS);
        mCustomLight = (ColorPickerPreference) findPreference(KEY_CUSTOM_LIGHT);
        mLightOnTime =(CustomSeekBarPreference) findPreference(KEY_LIGHTS_ON_TIME);
        mLightOffTime = (CustomSeekBarPreference) findPreference(KEY_LIGHTS_OFF_TIME);
        mLightOnZen = (SwitchPreference) findPreference(KEY_LIGHT_ON_ZEN);
        mLights.setDisabledByAdmin(mSuspendedAppsAdmin);
        mLights.setChecked(mChannel.shouldShowLights());
        //enable custom light prefs is light is enabled
        mCustomLight.setEnabled(!mLights.isDisabledByAdmin() && mChannel.shouldShowLights());
        mLightOnTime.setEnabled(!mLights.isDisabledByAdmin() && mChannel.shouldShowLights());
        mLightOffTime.setEnabled(!mLights.isDisabledByAdmin() && mChannel.shouldShowLights());
        mLightOnZen.setEnabled(!mLights.isDisabledByAdmin() && mChannel.shouldShowLights());

        //light pref
        mLights.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean lights = (Boolean) newValue;
                mChannel.enableLights(lights);
                mChannel.lockFields(NotificationChannel.USER_LOCKED_LIGHTS);
                mBackend.updateChannel(mPkg, mUid, mChannel);
                showLedPreview();
                if (!lights) {
                    mNm.forcePulseLedLight(-1, -1, -1);
                }
                mCustomLight.setEnabled(lights);
                mLightOnTime.setEnabled(lights);
                mLightOffTime.setEnabled(lights);
                mLightOnZen.setEnabled(lights);
                //enable NOTIFICATION_LIGHT_PULSE if the user wants to enable notification light for an app
                //if he disables mLights, don't do anything (other apps may have it still enabled)
                if (lights && Settings.System.getInt(mContext.getContentResolver(),
                        NOTIFICATION_LIGHT_PULSE, 1) == 0) {
                    Settings.System.putInt(mContext.getContentResolver(),
                        NOTIFICATION_LIGHT_PULSE, 1);
                }
                return true;
            }
        });
        //light color pref
        int defaultLightColor = getResources().getColor(com.android.internal.R.color.config_defaultNotificationColor);
        mCustomLight.setDefaultColor(defaultLightColor);
        mLedColor = (mChannel.getLightColor() != 0 ? mChannel.getLightColor() : defaultLightColor);
        mCustomLight.setAlphaSliderEnabled(false);
        mCustomLight.setNewPreviewColor(mLedColor);
        mCustomLight.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mLedColor = ((Integer) newValue).intValue();
                mChannel.setLightColor(mLedColor);
                mBackend.updateChannel(mPkg, mUid, mChannel);
                showLedPreview();
                return true;
            }
        });
        //light on time pref
        int lightOn = mChannel.getLightOnTime();
        int defaultLightOn = getResources().getInteger(
                com.android.internal.R.integer.config_defaultNotificationLedOn);
        mLightOnTime.setDefaultValue(defaultLightOn);
        lightOn = lightOn == 0 ? defaultLightOn : lightOn;
        mLightOnTime.setValue(lightOn);
        mLightOnTime.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int val = (Integer) newValue;
                mChannel.setLightOnTime(val);
                mBackend.updateChannel(mPkg, mUid, mChannel);
                showLedPreview();
                return true;
            }
        });
        //light off time pref
        int lightOff = mChannel.getLightOffTime();
        int defaultLightOff = getResources().getInteger(
                com.android.internal.R.integer.config_defaultNotificationLedOff);
        mLightOffTime.setDefaultValue(defaultLightOff);
        lightOff = lightOff == 0 ? defaultLightOff : lightOff;
        mLightOffTime.setValue(lightOff);
        mLightOffTime.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int val = (Integer) newValue;
                mChannel.setLightOffTime(val);
                mBackend.updateChannel(mPkg, mUid, mChannel);
                showLedPreview();
                return true;
            }
        });
        //light on zen pref
        mLightOnZen.setChecked(mChannel.shouldLightOnZen());
        mLightOnZen.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean show = (Boolean) newValue;
                mChannel.setLightOnZen(show);
                mBackend.updateChannel(mPkg, mUid, mChannel);
                return true;
            }
        });

        showLedPreview();
    }

    private void showLedPreview() {
        if (mChannel.shouldShowLights()) {
            if (mLedColor == 0xFFFFFFFF) {
                // i've no idea why atm but this is needed 
                mLedColor = 0xffffff;
            }
            mNm.forcePulseLedLight(
                    mLedColor, mChannel.getLightOnTime(), mChannel.getLightOffTime());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mNm.forcePulseLedLight(-1, -1, -1);
    }

    @Override
    public void onStop() {
        super.onStop();
        mNm.forcePulseLedLight(-1, -1, -1);
    }

    @Override
    public void onPause() {
        super.onPause();
        mNm.forcePulseLedLight(-1, -1, -1);
    }

    private void addHeaderPref() {
        ArrayMap<String, AppRow> rows = new ArrayMap<>();
        rows.put(mAppRow.pkg, mAppRow);
        collectConfigActivities(rows);
        final Activity activity = getActivity();
        final Preference pref = EntityHeaderController
                .newInstance(activity, this /* fragment */, null /* header */)
                .setRecyclerView(getListView(), getLifecycle())
                .setIcon(mAppRow.icon)
                .setLabel(mAppRow.label)
                .setPackageName(mAppRow.pkg)
                .setUid(mAppRow.uid)
                .setHasAppInfoLink(true)
                .setButtonActions(EntityHeaderController.ActionType.ACTION_NONE,
                        EntityHeaderController.ActionType.ACTION_NOTIF_PREFERENCE)
                .done(activity, getPrefContext());
        pref.setKey(KEY_HEADER);
        getPreferenceScreen().addPreference(pref);
    }

    private void populateChannelList() {
        if (!mChannelGroups.isEmpty()) {
            // If there's anything in mChannelGroups, we've called populateChannelList twice.
            // Clear out existing channels and log.
            Log.w(TAG, "Notification channel group posted twice to settings - old size " +
                    mChannelGroups.size() + ", new size " + mChannelGroupList.size());
            for (Preference p : mChannelGroups) {
                getPreferenceScreen().removePreference(p);
            }
        }
        if (mChannelGroupList.isEmpty()) {
            PreferenceCategory groupCategory = new PreferenceCategory(getPrefContext());
            groupCategory.setTitle(R.string.notification_channels);
            groupCategory.setKey(KEY_GENERAL_CATEGORY);
            getPreferenceScreen().addPreference(groupCategory);
            mChannelGroups.add(groupCategory);

            Preference empty = new Preference(getPrefContext());
            empty.setTitle(R.string.no_channels);
            empty.setEnabled(false);
            groupCategory.addPreference(empty);
        } else {
            for (NotificationChannelGroup group : mChannelGroupList) {
                PreferenceCategory groupCategory = new PreferenceCategory(getPrefContext());
                if (group.getId() == null) {
                    groupCategory.setTitle(mChannelGroupList.size() > 1
                            ? R.string.notification_channels_other
                            : R.string.notification_channels);
                    groupCategory.setKey(KEY_GENERAL_CATEGORY);
                } else {
                    groupCategory.setTitle(group.getName());
                    groupCategory.setKey(group.getId());
                }
                groupCategory.setOrderingAsAdded(true);
                getPreferenceScreen().addPreference(groupCategory);
                mChannelGroups.add(groupCategory);

                final List<NotificationChannel> channels = group.getChannels();
                Collections.sort(channels, mChannelComparator);
                int N = channels.size();
                for (int i = 0; i < N; i++) {
                    final NotificationChannel channel = channels.get(i);
                    populateSingleChannelPrefs(groupCategory, channel);
                }
            }

            int deletedChannelCount = mBackend.getDeletedChannelCount(mAppRow.pkg, mAppRow.uid);
            if (deletedChannelCount > 0 &&
                    getPreferenceScreen().findPreference(KEY_DELETED) == null) {
                mDeletedChannels = new FooterPreference(getPrefContext());
                mDeletedChannels.setSelectable(false);
                mDeletedChannels.setTitle(getResources().getQuantityString(
                        R.plurals.deleted_channels, deletedChannelCount, deletedChannelCount));
                mDeletedChannels.setEnabled(false);
                mDeletedChannels.setKey(KEY_DELETED);
                mDeletedChannels.setOrder(ORDER_LAST);
                getPreferenceScreen().addPreference(mDeletedChannels);
            }
        }

        updateDependents(mAppRow.banned);
    }

    private void populateSingleChannelPrefs(PreferenceCategory groupCategory,
            final NotificationChannel channel) {
        MasterSwitchPreference channelPref = new MasterSwitchPreference(
                getPrefContext());
        channelPref.setSwitchEnabled(mSuspendedAppsAdmin == null
                && isChannelBlockable(mAppRow.systemApp, channel)
                && isChannelConfigurable(channel));
        channelPref.setKey(channel.getId());
        channelPref.setTitle(channel.getName());
        channelPref.setChecked(channel.getImportance() != IMPORTANCE_NONE);
        channelPref.setSummary(getImportanceSummary(channel));
        Bundle channelArgs = new Bundle();
        channelArgs.putInt(AppInfoBase.ARG_PACKAGE_UID, mUid);
        channelArgs.putString(AppInfoBase.ARG_PACKAGE_NAME, mPkg);
        channelArgs.putString(Settings.EXTRA_CHANNEL_ID, channel.getId());
        Intent channelIntent = Utils.onBuildStartFragmentIntent(getActivity(),
                ChannelNotificationSettings.class.getName(),
                channelArgs, null, R.string.notification_channel_title, null, false,
                getMetricsCategory());
        channelPref.setIntent(channelIntent);

        channelPref.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference,
                            Object o) {
                        boolean value = (Boolean) o;
                        int importance = value ?  IMPORTANCE_LOW : IMPORTANCE_NONE;
                        channel.setImportance(importance);
                        channel.lockFields(
                                NotificationChannel.USER_LOCKED_IMPORTANCE);
                        channelPref.setSummary(getImportanceSummary(channel));
                        mBackend.updateChannel(mPkg, mUid, channel);

                        return true;
                    }
                });
        groupCategory.addPreference(channelPref);
    }

    void setupBadge() {
        mBadge = (RestrictedSwitchPreference) getPreferenceScreen().findPreference(KEY_BADGE);
        mBadge.setDisabledByAdmin(mSuspendedAppsAdmin);
        if (mChannel == null) {
            mBadge.setChecked(mAppRow.showBadge);
        } else {
            mBadge.setChecked(mChannel.canShowBadge());
        }
        mBadge.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean value = (Boolean) newValue;
                if (mChannel == null) {
                    mBackend.setShowBadge(mPkg, mUid, value);
                } else {
                    mChannel.setShowBadge(value);
                    mChannel.lockFields(NotificationChannel.USER_LOCKED_SHOW_BADGE);
                    mBackend.updateChannel(mPkg, mUid, mChannel);
                }
                return true;
            }
        });
    }

    protected void setupBlock() {
        View switchBarContainer = LayoutInflater.from(
                getPrefContext()).inflate(R.layout.styled_switch_bar, null);
        mSwitchBar = switchBarContainer.findViewById(R.id.switch_bar);
        mSwitchBar.show();
        mSwitchBar.setDisabledByAdmin(mSuspendedAppsAdmin);
        mSwitchBar.setChecked(!mAppRow.banned);
        mSwitchBar.addOnSwitchChangeListener(new SwitchBar.OnSwitchChangeListener() {
            @Override
            public void onSwitchChanged(Switch switchView, boolean isChecked) {
                if (mShowLegacyChannelConfig && mChannel != null) {
                    final int importance = isChecked ? IMPORTANCE_UNSPECIFIED : IMPORTANCE_NONE;
                    mImportanceToggle.setChecked(importance == IMPORTANCE_UNSPECIFIED);
                    mChannel.setImportance(importance);
                    mChannel.lockFields(NotificationChannel.USER_LOCKED_IMPORTANCE);
                    mBackend.updateChannel(mPkg, mUid, mChannel);
                }
                mBackend.setNotificationsEnabledForPackage(mPkgInfo.packageName, mUid, isChecked);
                mAppRow.banned = true;
                updateDependents(!isChecked);
            }
        });

        mBlockBar = new LayoutPreference(getPrefContext(), switchBarContainer);
        mBlockBar.setOrder(ORDER_FIRST);
        mBlockBar.setKey(KEY_BLOCK);
        getPreferenceScreen().addPreference(mBlockBar);

        if (mAppRow.systemApp && !mAppRow.banned) {
            setVisible(mBlockBar, false);
        }

        setupBlockDesc(R.string.app_notifications_off_desc);
    }

    protected void setupInfoDesc(int summaryResId) {
        FooterPreference infoDesc = (FooterPreference) getPreferenceScreen().findPreference(
                KEY_INFO_DESC);
        infoDesc = new FooterPreference(getPrefContext());
        infoDesc.setSelectable(false);
        infoDesc.setTitle(summaryResId);
        infoDesc.setEnabled(false);
        infoDesc.setOrder(50);
        getPreferenceScreen().addPreference(infoDesc);
    }

    protected void updateDependents(boolean banned) {
        for (PreferenceCategory category : mChannelGroups) {
            setVisible(category, !banned);
        }
        if (mDeletedChannels != null) {
            setVisible(mDeletedChannels, !banned);
        }
        setVisible(mBlockedDesc, banned);
        setVisible(mBadge, !banned);
        if (mShowLegacyChannelConfig) {
            setVisible(mImportanceToggle, !banned);
            setVisible(mPriority, checkCanBeVisible(NotificationManager.IMPORTANCE_DEFAULT)
                    || (checkCanBeVisible(NotificationManager.IMPORTANCE_LOW)
                    && mDndVisualEffectsSuppressed));
            setVisible(mVisibilityOverride, !banned &&
                    checkCanBeVisible(NotificationManager.IMPORTANCE_LOW) && isLockScreenSecure());
            setVisible(mLightCategory, !banned);
        }
        if (mAppLink != null) {
            setVisible(mAppLink, !banned);
        }
        if (mAppRow.systemApp && !mAppRow.banned) {
            setVisible(mBlockBar, false);
        }
    }

    private String getImportanceSummary(NotificationChannel channel) {
        switch (channel.getImportance()) {
            case NotificationManager.IMPORTANCE_UNSPECIFIED:
                return getContext().getString(R.string.notification_importance_unspecified);
            case NotificationManager.IMPORTANCE_NONE:
                return getContext().getString(R.string.notification_toggle_off);
            case NotificationManager.IMPORTANCE_MIN:
                return getContext().getString(R.string.notification_importance_min);
            case NotificationManager.IMPORTANCE_LOW:
                return getContext().getString(R.string.notification_importance_low);
            case NotificationManager.IMPORTANCE_DEFAULT:
                if (hasValidSound(channel)) {
                    return getContext().getString(R.string.notification_importance_default);
                } else { // Silent
                    return getContext().getString(R.string.notification_importance_low);
                }
            case NotificationManager.IMPORTANCE_HIGH:
            case NotificationManager.IMPORTANCE_MAX:
            default:
                if (hasValidSound(channel)) {
                    return getContext().getString(R.string.notification_importance_high);
                } else { // Silent
                    return getContext().getString(R.string.notification_importance_high_silent);
                }
        }
    }

    private Comparator<NotificationChannel> mChannelComparator =
            new Comparator<NotificationChannel>() {

        @Override
        public int compare(NotificationChannel left, NotificationChannel right) {
            if (left.isDeleted() != right.isDeleted()) {
                return Boolean.compare(left.isDeleted(), right.isDeleted());
            }
            return left.getId().compareTo(right.getId());
        }
    };

    private Comparator<NotificationChannelGroup> mChannelGroupComparator =
            new Comparator<NotificationChannelGroup>() {

                @Override
                public int compare(NotificationChannelGroup left, NotificationChannelGroup right) {
                    // Non-grouped channels (in placeholder group with a null id) come last
                    if (left.getId() == null && right.getId() != null) {
                        return 1;
                    } else if (right.getId() == null && left.getId() != null) {
                        return -1;
                    }
                    return left.getId().compareTo(right.getId());
                }
            };
}
