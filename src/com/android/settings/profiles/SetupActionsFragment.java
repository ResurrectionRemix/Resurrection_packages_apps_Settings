/*
 * Copyright (C) 2014 The CyanogenMod Project
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
package com.android.settings.profiles;

import android.app.Activity;
import android.app.AirplaneModeSettings;
import android.app.AlertDialog;
import android.app.ConnectionSettings;
import android.app.Dialog;
import android.app.Fragment;
import android.app.NotificationGroup;
import android.app.Profile;
import android.app.ProfileGroup;
import android.app.ProfileManager;
import android.app.RingModeSettings;
import android.app.StreamSettings;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.net.wimax.WimaxHelper;
import android.nfc.NfcManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SubSettings;
import com.android.settings.cyanogenmod.DeviceUtils;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.profiles.actions.ItemListAdapter;
import com.android.settings.profiles.actions.item.AirplaneModeItem;
import com.android.settings.profiles.actions.item.AppGroupItem;
import com.android.settings.profiles.actions.item.ConnectionOverrideItem;
import com.android.settings.profiles.actions.item.Header;
import com.android.settings.profiles.actions.item.Item;
import com.android.settings.profiles.actions.item.LockModeItem;
import com.android.settings.profiles.actions.item.ProfileNameItem;
import com.android.settings.profiles.actions.item.RingModeItem;
import com.android.settings.profiles.actions.item.TriggerItem;
import com.android.settings.profiles.actions.item.VolumeStreamItem;

import java.util.ArrayList;
import java.util.List;

import static android.app.ConnectionSettings.PROFILE_CONNECTION_2G3G4G;
import static android.app.ConnectionSettings.PROFILE_CONNECTION_BLUETOOTH;
import static android.app.ConnectionSettings.PROFILE_CONNECTION_GPS;
import static android.app.ConnectionSettings.PROFILE_CONNECTION_MOBILEDATA;
import static android.app.ConnectionSettings.PROFILE_CONNECTION_NFC;
import static android.app.ConnectionSettings.PROFILE_CONNECTION_SYNC;
import static android.app.ConnectionSettings.PROFILE_CONNECTION_WIFI;
import static android.app.ConnectionSettings.PROFILE_CONNECTION_WIFIAP;
import static android.app.ConnectionSettings.PROFILE_CONNECTION_WIMAX;

public class SetupActionsFragment extends SettingsPreferenceFragment
        implements AdapterView.OnItemClickListener {

    private static final int RINGTONE_REQUEST_CODE = 1000;
    private static final int NEW_TRIGGER_REQUEST_CODE = 1001;

    private static final int MENU_REMOVE = Menu.FIRST;
    private static final int MENU_FILL_PROFILE = Menu.FIRST + 1;

    Profile mProfile;
    ItemListAdapter mAdapter;
    ProfileManager mProfileManager;
    ListView mListView;

    boolean mNewProfileMode;

    private static final int[] LOCKMODE_MAPPING = new int[] {
        Profile.LockMode.DEFAULT, Profile.LockMode.INSECURE, Profile.LockMode.DISABLE
    };
    private static final int[] EXPANDED_DESKTOP_MAPPING = new int[] {
        Profile.ExpandedDesktopMode.DEFAULT,
        Profile.ExpandedDesktopMode.ENABLE,
        Profile.ExpandedDesktopMode.DISABLE
    };
    private List<Item> mItems = new ArrayList<Item>();

    public static SetupActionsFragment newInstance(Profile profile, boolean newProfile) {
        SetupActionsFragment fragment = new SetupActionsFragment();
        Bundle args = new Bundle();
        args.putParcelable(ProfilesSettings.EXTRA_PROFILE, profile);
        args.putBoolean(ProfilesSettings.EXTRA_NEW_PROFILE, newProfile);

        fragment.setArguments(args);
        return fragment;
    }

    public SetupActionsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mProfile = getArguments().getParcelable(ProfilesSettings.EXTRA_PROFILE);
            mNewProfileMode = getArguments().getBoolean(ProfilesSettings.EXTRA_NEW_PROFILE, false);
        }

        mProfileManager = (ProfileManager) getActivity().getSystemService(Context.PROFILE_SERVICE);
        mAdapter = new ItemListAdapter(getActivity(), mItems);
        rebuildItemList();

        setHasOptionsMenu(true);
        if (mNewProfileMode) {
            requestFillProfileFromSettingsDialog();
        }
    }

    private void rebuildItemList() {
        mItems.clear();
        // general prefs
        mItems.add(new Header(getString(R.string.profile_name_title)));
        mItems.add(new ProfileNameItem(mProfile));

        if (!mNewProfileMode) {
            // triggers
            mItems.add(new Header(getString(R.string.profile_triggers_header)));
            mItems.add(generateTriggerItem(TriggerItem.WIFI));
            if (DeviceUtils.deviceSupportsBluetooth()) {
                mItems.add(generateTriggerItem(TriggerItem.BLUETOOTH));
            }
            if (DeviceUtils.deviceSupportsNfc(getActivity())) {
                mItems.add(generateTriggerItem(TriggerItem.NFC));
            }
        }

        // connection overrides
        mItems.add(new Header(getString(R.string.wireless_networks_settings_title)));
        if (DeviceUtils.deviceSupportsBluetooth()) {
            mItems.add(new ConnectionOverrideItem(PROFILE_CONNECTION_BLUETOOTH,
                    mProfile.getSettingsForConnection(PROFILE_CONNECTION_BLUETOOTH)));
        }
        mItems.add(generateConnectionOverrideItem(PROFILE_CONNECTION_GPS));
        mItems.add(generateConnectionOverrideItem(PROFILE_CONNECTION_WIFI));
        mItems.add(generateConnectionOverrideItem(PROFILE_CONNECTION_SYNC));
        if (DeviceUtils.deviceSupportsMobileData(getActivity())) {
            mItems.add(generateConnectionOverrideItem(PROFILE_CONNECTION_MOBILEDATA));
            mItems.add(generateConnectionOverrideItem(PROFILE_CONNECTION_WIFIAP));

            final TelephonyManager tm =
                    (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
            if (tm.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM) {
                mItems.add(generateConnectionOverrideItem(PROFILE_CONNECTION_2G3G4G));
            }
        }
        if (WimaxHelper.isWimaxSupported(getActivity())) {
            mItems.add(generateConnectionOverrideItem(PROFILE_CONNECTION_WIMAX));
        }
        if (DeviceUtils.deviceSupportsNfc(getActivity())) {
            mItems.add(generateConnectionOverrideItem(PROFILE_CONNECTION_NFC));
        }

        // add volume streams
        mItems.add(new Header(getString(R.string.profile_volumeoverrides_title)));
        mItems.add(generateVolumeStreamItem(AudioManager.STREAM_ALARM));
        mItems.add(generateVolumeStreamItem(AudioManager.STREAM_MUSIC));
        mItems.add(generateVolumeStreamItem(AudioManager.STREAM_RING));
        mItems.add(generateVolumeStreamItem(AudioManager.STREAM_NOTIFICATION));

        // system settings
        mItems.add(new Header(getString(R.string.profile_system_settings_title)));
        mItems.add(new RingModeItem(mProfile.getRingMode()));
        mItems.add(new AirplaneModeItem(mProfile.getAirplaneMode()));
        mItems.add(new LockModeItem(mProfile));

        // app groups
        if (SettingsActivity.showAdvancedPreferences(getActivity())) {
            mItems.add(new Header(getString(R.string.profile_app_group_category_title)));

            int groupsAdded = 0;
            ProfileGroup[] profileGroups = mProfile.getProfileGroups();
            if (profileGroups != null && profileGroups.length > 1) { // it will always have "other"
                for (ProfileGroup profileGroup : profileGroups) {
                    // only display profile group if there's a matching notification group
                    // and don't' show the wildcard group
                    if (mProfileManager.getNotificationGroup(profileGroup.getUuid()) != null
                            && !mProfile.getDefaultGroup().getUuid().equals(
                                profileGroup.getUuid())) {
                        mItems.add(new AppGroupItem(mProfile, profileGroup));
                        groupsAdded++;
                    }
                }
                if (groupsAdded > 0) {
                    // add "Other" at the end
                    mItems.add(new AppGroupItem(mProfile, mProfile.getDefaultGroup()));
                }
            }
            if (mProfileManager.getNotificationGroups().length > 0) {
                // if there are notification groups available, allow them to be configured
                mItems.add(new AppGroupItem());
            } else if (groupsAdded == 0) {
                // no notification groups available at all, nothing to add/remove
                mItems.remove(mItems.get(mItems.size() - 1));
            }
        }

        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (!mNewProfileMode) {
            menu.add(0, MENU_REMOVE, 0, R.string.profile_menu_delete_title)
                    .setIcon(R.drawable.ic_actionbar_delete)
                    .setAlphabeticShortcut('d')
                    .setEnabled(true)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
                            MenuItem.SHOW_AS_ACTION_WITH_TEXT);

            menu.add(0, MENU_FILL_PROFILE, 0, R.string.profile_menu_fill_from_state)
                    .setAlphabeticShortcut('f')
                    .setEnabled(true)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_REMOVE:
                requestRemoveProfileDialog();
                return true;
            case MENU_FILL_PROFILE:
                requestFillProfileFromSettingsDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private ConnectionOverrideItem generateConnectionOverrideItem(int connectionId) {
        ConnectionSettings settings = mProfile.getSettingsForConnection(connectionId);
        if (settings == null) {
            settings = new ConnectionSettings(connectionId);
            mProfile.setConnectionSettings(settings);
        }
        return new ConnectionOverrideItem(connectionId, settings);
    }

    private VolumeStreamItem generateVolumeStreamItem(int stream) {
        StreamSettings settings = mProfile.getSettingsForStream(stream);
        if (settings == null) {
            settings = new StreamSettings(stream);
            mProfile.setStreamSettings(settings);
        }
        return new VolumeStreamItem(stream, settings);
    }

    private TriggerItem generateTriggerItem(int whichTrigger) {
        return new TriggerItem(mProfile, whichTrigger);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (mNewProfileMode) {
            TextView desc = new TextView(getActivity());
            int descPadding = getResources().getDimensionPixelSize(
                    R.dimen.profile_instruction_padding);
            desc.setPadding(descPadding, descPadding, descPadding, descPadding);
            desc.setText(R.string.profile_setup_actions_description);
            getListView().addHeaderView(desc, null, false);
        }
    }

    private void updateProfile() {
        mProfileManager.updateProfile(mProfile);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mListView.setAdapter(mAdapter);
        if (mNewProfileMode) {
            getActivity().getActionBar().setTitle(R.string.profile_setup_actions_title);
        } else {
            getActivity().getActionBar().setTitle(mProfile.getName());
        }
    }

    private void requestFillProfileFromSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.profile_populate_profile_from_state);
        builder.setNegativeButton(R.string.no, null);
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                fillProfileFromCurrentSettings();
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void fillProfileFromCurrentSettings() {
        new AsyncTask<Profile, Void, Void>() {
            @Override
            protected Void doInBackground(Profile... params) {
                // bt
                if (DeviceUtils.deviceSupportsBluetooth()) {
                    mProfile.setConnectionSettings(
                            new ConnectionSettings(ConnectionSettings.PROFILE_CONNECTION_BLUETOOTH,
                                    BluetoothAdapter.getDefaultAdapter().isEnabled() ? 1 : 0,
                                    true));
                }

                // gps
                LocationManager locationManager = (LocationManager)
                        getSystemService(Context.LOCATION_SERVICE);
                boolean gpsEnabled = locationManager.
                        isProviderEnabled(LocationManager.GPS_PROVIDER);
                mProfile.setConnectionSettings(
                        new ConnectionSettings(ConnectionSettings.PROFILE_CONNECTION_GPS,
                                gpsEnabled ? 1 : 0, true));

                // wifi
                WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                mProfile.setConnectionSettings(
                        new ConnectionSettings(ConnectionSettings.PROFILE_CONNECTION_WIFI,
                                wifiManager.isWifiEnabled() ? 1 : 0, true));

                // auto sync data
                mProfile.setConnectionSettings(
                        new ConnectionSettings(ConnectionSettings.PROFILE_CONNECTION_SYNC,
                                ContentResolver.getMasterSyncAutomatically() ? 1 : 0, true));

                // mobile data
                if (DeviceUtils.deviceSupportsMobileData(getActivity())) {
                    ConnectivityManager cm = (ConnectivityManager)
                            getSystemService(Context.CONNECTIVITY_SERVICE);
                    mProfile.setConnectionSettings(
                            new ConnectionSettings(ConnectionSettings.PROFILE_CONNECTION_MOBILEDATA,
                                    cm.getMobileDataEnabled() ? 1 : 0, true));
                }

                // wifi hotspot
                mProfile.setConnectionSettings(
                        new ConnectionSettings(ConnectionSettings.PROFILE_CONNECTION_WIFIAP,
                                wifiManager.isWifiApEnabled() ? 1 : 0, true));

                // 2g/3g/4g
                // skipping this one

                // nfc
                if (DeviceUtils.deviceSupportsNfc(getActivity())) {
                    NfcManager nfcManager = (NfcManager) getSystemService(Context.NFC_SERVICE);
                    mProfile.setConnectionSettings(
                            new ConnectionSettings(ConnectionSettings.PROFILE_CONNECTION_NFC,
                                    nfcManager.getDefaultAdapter().isEnabled() ? 1 : 0, true));
                }

                // alarm volume
                final AudioManager am = (AudioManager) getActivity()
                        .getSystemService(Context.AUDIO_SERVICE);
                mProfile.setStreamSettings(new StreamSettings(AudioManager.STREAM_ALARM,
                        am.getStreamVolume(AudioManager.STREAM_ALARM), true));

                // media volume
                mProfile.setStreamSettings(new StreamSettings(AudioManager.STREAM_MUSIC,
                        am.getStreamVolume(AudioManager.STREAM_MUSIC), true));

                // ringtone volume
                mProfile.setStreamSettings(new StreamSettings(AudioManager.STREAM_RING,
                        am.getStreamVolume(AudioManager.STREAM_RING), true));

                // notification volume
                mProfile.setStreamSettings(new StreamSettings(AudioManager.STREAM_NOTIFICATION,
                        am.getStreamVolume(AudioManager.STREAM_NOTIFICATION), true));

                // ring mode
                String ringValue;
                switch (am.getRingerMode()) {
                    default:
                    case AudioManager.RINGER_MODE_NORMAL:
                        ringValue = "normal";
                        break;
                    case AudioManager.RINGER_MODE_SILENT:
                        ringValue = "mute";
                        break;
                    case AudioManager.RINGER_MODE_VIBRATE:
                        ringValue = "vibrate";
                        break;
                }
                mProfile.setRingMode(new RingModeSettings(ringValue, true));

                // airplane mode
                boolean airplaneMode = Settings.Global.getInt(getActivity().getContentResolver(),
                        Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
                mProfile.setAirplaneMode(new AirplaneModeSettings(airplaneMode ? 1 : 0, true));

                // lock screen mode
                // populated only from profiles, so we can read the current profile,
                // but let's skip this one

                updateProfile();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                rebuildItemList();

            }
        }.execute(mProfile);
    }

    private void requestRemoveProfileDialog() {
        Profile current = mProfileManager.getActiveProfile();
        if (mProfile.getUuid().equals(current.getUuid())) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(getString(R.string.profile_remove_current_profile));
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getString(R.string.profile_remove_dialog_message, mProfile.getName()));
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                mProfileManager.removeProfile(mProfile);
                finishFragment();
            }
        });
        builder.setNegativeButton(R.string.no, null);
        builder.show();
    }

    private void requestLockscreenModeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final String[] lockEntries =
                getResources().getStringArray(R.array.profile_lockmode_entries);

        int defaultIndex = 0; // no action
        for (int i = 0; i < LOCKMODE_MAPPING.length; i++) {
            if (LOCKMODE_MAPPING[i] == mProfile.getScreenLockMode()) {
                defaultIndex = i;
                break;
            }
        }

        builder.setTitle(R.string.profile_lockmode_title);
        builder.setSingleChoiceItems(lockEntries, defaultIndex,
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                mProfile.setScreenLockMode(LOCKMODE_MAPPING[item]);
                updateProfile();
                mAdapter.notifyDataSetChanged();
                dialog.dismiss();
            }
        });

        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    private void requestAirplaneModeDialog(final AirplaneModeSettings setting) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final String[] connectionNames =
                getResources().getStringArray(R.array.profile_action_generic_connection_entries);

        int defaultIndex = 0; // no action
        if (setting.isOverride()) {
            if (setting.getValue() == 1) {
                defaultIndex = 2; // enabled
            } else {
                defaultIndex = 1; // disabled
            }
        }

        builder.setTitle(R.string.profile_airplanemode_title);
        builder.setSingleChoiceItems(connectionNames, defaultIndex,
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                switch (item) {
                    case 0: // disable override
                        setting.setOverride(false);
                        break;
                    case 1: // enable override, disable
                        setting.setOverride(true);
                        setting.setValue(0);
                        break;
                    case 2: // enable override, enable
                        setting.setOverride(true);
                        setting.setValue(1);
                        break;
                }
                mProfile.setAirplaneMode(setting);
                mAdapter.notifyDataSetChanged();
                updateProfile();
                dialog.dismiss();
            }
        });

        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    private void requestProfileRingMode() {
        // Launch the ringtone picker
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE);
        startActivityForResult(intent, RINGTONE_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == NEW_TRIGGER_REQUEST_CODE) {
            mProfile = mProfileManager.getProfile(mProfile.getUuid());
            rebuildItemList();
        }
    }

    private void requestRingModeDialog(final RingModeSettings setting) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final String[] values = getResources().getStringArray(R.array.ring_mode_values);
        final String[] names = getResources().getStringArray(R.array.ring_mode_entries);

        int defaultIndex = 0; // normal by default
        if (setting.isOverride()) {
            if (setting.getValue().equals(values[0] /* normal */)) {
                defaultIndex = 0;
            } else if (setting.getValue().equals(values[1] /* vibrate */)) {
                defaultIndex = 1; // enabled
            } else if (setting.getValue().equals(values[2] /* mute */)) {
                defaultIndex = 2; // mute
            }
        } else {
            defaultIndex = 3;
        }

        builder.setTitle(R.string.ring_mode_title);
        builder.setSingleChoiceItems(names, defaultIndex,
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                switch (item) {
                    case 0: // enable override, normal
                        setting.setOverride(true);
                        setting.setValue(values[0]);
                        break;
                    case 1: // enable override, vibrate
                        setting.setOverride(true);
                        setting.setValue(values[1]);
                        break;
                    case 2: // enable override, mute
                        setting.setOverride(true);
                        setting.setValue(values[2]);
                        break;
                    case 3:
                        setting.setOverride(false);
                        break;
                }
                mProfile.setRingMode(setting);
                mAdapter.notifyDataSetChanged();
                updateProfile();
                dialog.dismiss();
            }
        });

        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    private void requestConnectionOverrideDialog(final ConnectionSettings setting) {
        if (setting == null) {
            throw new UnsupportedOperationException("connection setting cannot be null yo");
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final String[] connectionNames =
                getResources().getStringArray(R.array.profile_action_generic_connection_entries);

        int defaultIndex = 0; // no action
        if (setting.isOverride()) {
            if (setting.getValue() == 1) {
                defaultIndex = 2; // enabled
            } else {
                defaultIndex = 1; // disabled
            }
        }

        builder.setTitle(ConnectionOverrideItem.getConnectionTitle(setting.getConnectionId()));
        builder.setSingleChoiceItems(connectionNames, defaultIndex,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        switch (item) {
                            case 0: // disable override
                                setting.setOverride(false);
                                break;
                            case 1: // enable override, disable
                                setting.setOverride(true);
                                setting.setValue(0);
                                break;
                            case 2: // enable override, enable
                                setting.setOverride(true);
                                setting.setValue(1);
                                break;
                        }
                        mProfile.setConnectionSettings(setting);
                        mAdapter.notifyDataSetChanged();
                        updateProfile();
                        dialog.dismiss();
                    }
                });

        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    private void requestMobileConnectionOverrideDialog(final ConnectionSettings setting) {
        if (setting == null) {
            throw new UnsupportedOperationException("connection setting cannot be null yo");
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final String[] connectionNames =
                getResources().getStringArray(R.array.profile_networkmode_entries_4g);

        int defaultIndex = ConnectionOverrideItem.CM_MODE_UNCHANGED; // no action
        if (setting.isOverride()) {
            defaultIndex = setting.getValue();
        }

        builder.setTitle(ConnectionOverrideItem.getConnectionTitle(setting.getConnectionId()));
        builder.setSingleChoiceItems(connectionNames, defaultIndex,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        switch (item) {
                            case ConnectionOverrideItem.CM_MODE_UNCHANGED:
                                setting.setOverride(false);
                                break;
                            default:
                                setting.setOverride(true);
                                setting.setValue(item);
                        }
                        mProfile.setConnectionSettings(setting);
                        mAdapter.notifyDataSetChanged();
                        updateProfile();
                        dialog.dismiss();
                    }
                });

        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    public void requestVolumeDialog(int streamId,
                                    final StreamSettings streamSettings) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(VolumeStreamItem.getNameForStream(streamId));

        final AudioManager am = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        final LayoutInflater inflater = LayoutInflater.from(getActivity());
        final View view = inflater.inflate(R.layout.dialog_profiles_volume_override, null);
        final SeekBar seekBar = (SeekBar) view.findViewById(R.id.seekbar);
        final CheckBox override = (CheckBox) view.findViewById(R.id.checkbox);
        override.setChecked(streamSettings.isOverride());
        override.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                streamSettings.setOverride(isChecked);
                seekBar.setEnabled(isChecked);

                mProfile.setStreamSettings(streamSettings);
                mAdapter.notifyDataSetChanged();
                updateProfile();
            }
        });
        seekBar.setEnabled(streamSettings.isOverride());
        seekBar.setMax(am.getStreamMaxVolume(streamId));
        seekBar.setProgress(streamSettings.getValue());
        builder.setView(view);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int value = seekBar.getProgress();
                streamSettings.setValue(value);
                mProfile.setStreamSettings(streamSettings);
                mAdapter.notifyDataSetChanged();
                updateProfile();
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void requestProfileName() {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View dialogView = inflater.inflate(R.layout.profile_name_dialog, null);

        final EditText entry = (EditText) dialogView.findViewById(R.id.name);
        entry.setText(mProfile.getName());
        entry.setSelectAllOnFocus(true);

        final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.rename_dialog_title)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String value = entry.getText().toString();
                        mProfile.setName(value);
                        mAdapter.notifyDataSetChanged();
                        updateProfile();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        entry.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                final String str = s.toString();
                final boolean empty = TextUtils.isEmpty(str)
                        || TextUtils.getTrimmedLength(str) == 0;
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!empty);
            }
        });
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                InputMethodManager imm = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(entry, InputMethodManager.SHOW_IMPLICIT);
            }
        });
        alertDialog.show();
    }

    private void requestActiveAppGroupsDialog() {
        final NotificationGroup[] notificationGroups = mProfileManager.getNotificationGroups();

        CharSequence[] items = new CharSequence[notificationGroups.length];
        boolean[] checked = new boolean[notificationGroups.length];

        for (int i = 0; i < notificationGroups.length; i++) {
            items[i] = notificationGroups[i].getName();
            checked[i] = mProfile.getProfileGroup(notificationGroups[i].getUuid()) != null;
        }
        DialogInterface.OnMultiChoiceClickListener listener =
                new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                if (isChecked) {
                    mProfile.addProfileGroup(new ProfileGroup(notificationGroups[which].getUuid(), false));
                } else {
                    mProfile.removeProfileGroup(notificationGroups[which].getUuid());
                }
                updateProfile();
                rebuildItemList();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setMultiChoiceItems(items, checked, listener)
                .setTitle(R.string.profile_appgroups_title)
                .setPositiveButton(R.string.ok, null);
        builder.show();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setup_actions, container, false);

        mListView = (ListView) view.findViewById(android.R.id.list);
        mListView.setOnItemClickListener(this);
        if (mNewProfileMode) {
            view.findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getActivity().setResult(Activity.RESULT_CANCELED);
                    finishFragment();
                }
            });

            view.findViewById(R.id.finish).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mProfileManager.addProfile(mProfile);

                    getActivity().setResult(Activity.RESULT_OK);
                    finishFragment();
                }
            });
        } else {
            view.findViewById(R.id.bottom_buttons).setVisibility(View.GONE);
        }
        return view;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Item itemAtPosition = (Item) parent.getItemAtPosition(position);

        if (itemAtPosition instanceof AirplaneModeItem) {
            AirplaneModeItem item = (AirplaneModeItem) itemAtPosition;
            requestAirplaneModeDialog(item.getSettings());
        } else if (itemAtPosition instanceof LockModeItem) {
            requestLockscreenModeDialog();
        } else if (itemAtPosition instanceof RingModeItem) {
            RingModeItem item = (RingModeItem) itemAtPosition;
            requestRingModeDialog(item.getSettings());
        } else if (itemAtPosition instanceof ConnectionOverrideItem) {
            ConnectionOverrideItem item = (ConnectionOverrideItem) itemAtPosition;
            if (item.getConnectionType() == ConnectionSettings.PROFILE_CONNECTION_2G3G4G) {
                requestMobileConnectionOverrideDialog(item.getSettings());
            } else {
                requestConnectionOverrideDialog(item.getSettings());
            }
        } else if (itemAtPosition instanceof VolumeStreamItem) {
            VolumeStreamItem item = (VolumeStreamItem) itemAtPosition;
            requestVolumeDialog(item.getStreamType(), item.getSettings());
        } else if (itemAtPosition instanceof ProfileNameItem) {
            requestProfileName();
        } else if (itemAtPosition instanceof TriggerItem) {
            TriggerItem item = (TriggerItem) itemAtPosition;
            openTriggersFragment(item.getTriggerType());
        } else if (itemAtPosition instanceof AppGroupItem) {
            AppGroupItem item = (AppGroupItem) itemAtPosition;
            if (item.getGroupUuid() == null) {
                requestActiveAppGroupsDialog();
            } else {
                startProfileGroupActivity(item);
            }
        }
    }

    private void startProfileGroupActivity(AppGroupItem item) {
            Bundle args = new Bundle();
        args.putString("ProfileGroup", item.getGroupUuid().toString());
        args.putParcelable("Profile", mProfile);

        startFragment(this, ProfileGroupConfig.class.getName(), 0, 0, args);
    }

    private void openTriggersFragment(int openTo) {
        Bundle args = new Bundle();
        args.putParcelable(ProfilesSettings.EXTRA_PROFILE,  mProfile);
        args.putBoolean(ProfilesSettings.EXTRA_NEW_PROFILE, false);
        args.putInt(SetupTriggersFragment.EXTRA_INITIAL_PAGE, openTo);

        SubSettings pa = (SubSettings) getActivity();
        pa.startPreferencePanel(SetupTriggersFragment.class.getCanonicalName(), args,
                R.string.profile_profile_manage, null, this, NEW_TRIGGER_REQUEST_CODE);
    }
}
