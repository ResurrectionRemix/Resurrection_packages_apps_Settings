/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.deviceinfo;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserManager;
import android.os.storage.DiskInfo;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.os.storage.VolumeRecord;
import android.provider.DocumentsContract;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.text.format.Formatter.BytesResult;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.util.Preconditions;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.deviceinfo.StorageSettings.MountTask;
import com.android.settings.deviceinfo.StorageSettings.UnmountTask;

import java.io.File;
import java.util.Objects;

/**
 * Panel showing summary and actions for a {@link VolumeInfo#TYPE_PUBLIC}
 * storage volume.
 */
public class PublicVolumeSettings extends SettingsPreferenceFragment {
    // TODO: disable unmount when providing over MTP/PTP

    private StorageManager mStorageManager;

    private String mVolumeId;
    private VolumeInfo mVolume;
    private DiskInfo mDisk;

    private StorageSummaryPreference mSummary;

    private Preference mMount;
    private Preference mFormatPublic;
    private Preference mFormatPrivate;
    private Button mUnmount;

    private boolean mIsPermittedToAdopt;

    private boolean isVolumeValid() {
        return (mVolume != null) && (mVolume.getType() == VolumeInfo.TYPE_PUBLIC)
                && mVolume.isMountedReadable();
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.DEVICEINFO_STORAGE;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        final Context context = getActivity();

        mIsPermittedToAdopt = UserManager.get(context).isAdminUser()
                && !ActivityManager.isUserAMonkey();

        mStorageManager = context.getSystemService(StorageManager.class);

        if (DocumentsContract.ACTION_DOCUMENT_ROOT_SETTINGS.equals(
                getActivity().getIntent().getAction())) {
            final Uri rootUri = getActivity().getIntent().getData();
            final String fsUuid = DocumentsContract.getRootId(rootUri);
            mVolume = mStorageManager.findVolumeByUuid(fsUuid);
        } else {
            final String volId = getArguments().getString(VolumeInfo.EXTRA_VOLUME_ID);
            mVolume = mStorageManager.findVolumeById(volId);
        }

        if (!isVolumeValid()) {
            getActivity().finish();
            return;
        }

        mDisk = mStorageManager.findDiskById(mVolume.getDiskId());
        Preconditions.checkNotNull(mDisk);

        mVolumeId = mVolume.getId();

        addPreferencesFromResource(R.xml.device_info_storage_volume);
        getPreferenceScreen().setOrderingAsAdded(true);

        mSummary = new StorageSummaryPreference(getPrefContext());

        mMount = buildAction(R.string.storage_menu_mount);
        mUnmount = new Button(getActivity());
        mUnmount.setText(R.string.storage_menu_unmount);
        mUnmount.setOnClickListener(mUnmountListener);
        mFormatPublic = buildAction(R.string.storage_menu_format);
        if (mIsPermittedToAdopt) {
            mFormatPrivate = buildAction(R.string.storage_menu_format_private);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (!isVolumeValid()) {
            return;
        }

        final Resources resources = getResources();
        final int padding = resources.getDimensionPixelSize(
                R.dimen.unmount_button_padding);
        final ViewGroup buttonBar = getButtonBar();
        buttonBar.removeAllViews();
        buttonBar.setPadding(padding, padding, padding, padding);

        //Fixed monkey test issue
        //Settings crash caused by null pointer parameter
        //Add check with mUnmount's values
        if (null == mUnmount) {
            mUnmount = new Button(getActivity());
            mUnmount.setText(R.string.storage_menu_unmount);
            mUnmount.setOnClickListener(mUnmountListener);
        }

        buttonBar.addView(mUnmount, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    public void update() {
        if (!isVolumeValid()) {
            getActivity().finish();
            return;
        }

        getActivity().setTitle(mStorageManager.getBestVolumeDescription(mVolume));

        final Context context = getActivity();
        final PreferenceScreen screen = getPreferenceScreen();

        screen.removeAll();

        if (mVolume.isMountedReadable()) {
            addPreference(mSummary);

            final File file = mVolume.getPath();
            final long totalBytes = file.getTotalSpace();
            final long freeBytes = file.getFreeSpace();
            final long usedBytes = totalBytes - freeBytes;

            final BytesResult result = Formatter.formatBytes(getResources(), usedBytes, 0);
            mSummary.setTitle(TextUtils.expandTemplate(getText(R.string.storage_size_large),
                    result.value, result.units));
            mSummary.setSummary(getString(R.string.storage_volume_used,
                    Formatter.formatFileSize(context, totalBytes)));
            mSummary.setPercent((int) ((usedBytes * 100) / totalBytes));
        }

        if (mVolume.getState() == VolumeInfo.STATE_UNMOUNTED) {
            addPreference(mMount);
        }
        if (!mDisk.isNonRemovable() && mVolume.isMountedReadable()) {
            getButtonBar().setVisibility(View.VISIBLE);
        }
        addPreference(mFormatPublic);
        if (mDisk.isAdoptable() && mIsPermittedToAdopt) {
            addPreference(mFormatPrivate);
        }
    }

    private void addPreference(Preference pref) {
        pref.setOrder(Preference.DEFAULT_ORDER);
        getPreferenceScreen().addPreference(pref);
    }

    private Preference buildAction(int titleRes) {
        final Preference pref = new Preference(getPrefContext());
        pref.setTitle(titleRes);
        return pref;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Refresh to verify that we haven't been formatted away
        mVolume = mStorageManager.findVolumeById(mVolumeId);
        if (!isVolumeValid()) {
            getActivity().finish();
            return;
        }

        mStorageManager.registerListener(mStorageListener);
        update();
    }

    @Override
    public void onPause() {
        super.onPause();
        mStorageManager.unregisterListener(mStorageListener);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference pref) {
        final Context context = getActivity();
        if (pref == mMount) {
            new MountTask(context, mVolume).execute();
        } else if (pref == mFormatPublic) {
            final Intent intent = new Intent(context, StorageWizardFormatConfirm.class);
            intent.putExtra(DiskInfo.EXTRA_DISK_ID, mDisk.getId());
            intent.putExtra(StorageWizardFormatConfirm.EXTRA_FORMAT_PRIVATE, false);
            startActivity(intent);
        } else if (pref == mFormatPrivate) {
            final Intent intent = new Intent(context, StorageWizardFormatConfirm.class);
            intent.putExtra(DiskInfo.EXTRA_DISK_ID, mDisk.getId());
            intent.putExtra(StorageWizardFormatConfirm.EXTRA_FORMAT_PRIVATE, true);
            startActivity(intent);
        }

        return super.onPreferenceTreeClick(pref);
    }

    private final View.OnClickListener mUnmountListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            new UnmountTask(getActivity(), mVolume).execute();
        }
    };

    private final StorageEventListener mStorageListener = new StorageEventListener() {
        @Override
        public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
            if (Objects.equals(mVolume.getId(), vol.getId())) {
                mVolume = vol;
                update();
            }
        }

        @Override
        public void onVolumeRecordChanged(VolumeRecord rec) {
            if (Objects.equals(mVolume.getFsUuid(), rec.getFsUuid())) {
                mVolume = mStorageManager.findVolumeById(mVolumeId);
                update();
            }
        }
    };
}
