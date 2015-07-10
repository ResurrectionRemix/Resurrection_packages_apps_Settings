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
package com.android.settings.profiles.triggers;

import android.app.AlertDialog;
import android.app.ListFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import cyanogenmod.app.Profile;
import cyanogenmod.app.ProfileManager;

import com.android.settings.R;
import com.android.settings.profiles.ProfilesSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class BluetoothTriggerFragment extends ListFragment {

    private BluetoothAdapter mBluetoothAdapter;

    Profile mProfile;
    ProfileManager mProfileManager;

    private View mEmptyView;

    private List<BluetoothTrigger> mTriggers = new ArrayList<BluetoothTrigger>();
    private BluetoothTriggerAdapter mListAdapter;

    public static BluetoothTriggerFragment newInstance(Profile profile) {
        BluetoothTriggerFragment fragment = new BluetoothTriggerFragment();

        Bundle extras = new Bundle();
        extras.putParcelable(ProfilesSettings.EXTRA_PROFILE, profile);

        fragment.setArguments(extras);
        return fragment;
    }

    public BluetoothTriggerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mProfileManager = ProfileManager.getInstance(getActivity());
        if (getArguments() != null) {
            mProfile = getArguments().getParcelable(ProfilesSettings.EXTRA_PROFILE);
        }
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadTriggerListItems();
    }

    private void initPreference(AbstractTriggerItem pref, int state, Resources res, int icon) {
        String[] values = res.getStringArray(R.array.profile_trigger_wifi_options_values);
        for (int i = 0; i < values.length; i++) {
            if (Integer.parseInt(values[i]) == state) {
                pref.setSummary(res.getStringArray(R.array.profile_trigger_wifi_options)[i]);
                break;
            }
        }
        pref.setTriggerState(state);
        pref.setIcon(icon);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        final String triggerId;
        final String triggerName;
        final int triggerType;

        String[] entries = getResources().getStringArray(R.array.profile_trigger_wifi_options);
        String[] values =
                getResources().getStringArray(R.array.profile_trigger_wifi_options_values);

        List<Trigger> triggers = new ArrayList<Trigger>(entries.length);
        for (int i = 0; i < entries.length; i++) {
            Trigger toAdd = new Trigger();
            toAdd.value = Integer.parseInt(values[i]);
            toAdd.name = entries[i];
            triggers.add(toAdd);
        }

        BluetoothTrigger btpref = mListAdapter.getItem(position);
        triggerName = btpref.getTitle();
        triggerId = btpref.getAddress();
        triggerType = Profile.TriggerType.BLUETOOTH;
        BluetoothDevice dev = mBluetoothAdapter.getRemoteDevice(triggerId);
        if (!dev.getBluetoothClass().doesClassMatch(BluetoothClass.PROFILE_A2DP)) {
            removeTrigger(triggers, Profile.TriggerState.ON_A2DP_CONNECT);
            removeTrigger(triggers, Profile.TriggerState.ON_A2DP_DISCONNECT);
        }

        entries = new String[triggers.size()];
        final int[] valueInts = new int[triggers.size()];
        int currentTriggerState = mProfile.getTriggerState(triggerType, triggerId);
        int currentItem = -1;
        for (int i = 0; i < triggers.size(); i++) {
            Trigger t = triggers.get(i);
            entries[i] = t.name;
            valueInts[i] = t.value;
            if (valueInts[i] == currentTriggerState) {
                currentItem = i;
            }
        }

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.profile_trigger_configure)
                .setSingleChoiceItems(entries, currentItem, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mProfile.setTrigger(triggerType, triggerId, valueInts[which], triggerName);
                        mProfileManager.updateProfile(mProfile);
                        reloadTriggerListItems();
                        dialog.dismiss();
                    }
                })
                .show();
    }

    @Override
    public void onStart() {
        super.onStart();
        getListView().setEmptyView(mEmptyView);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mEmptyView = inflater.inflate(R.layout.profile_bluetooth_empty_view, container, false);
        mEmptyView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent bluetoothSettings = new Intent();
                bluetoothSettings.setAction(
                        Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivity(bluetoothSettings);
            }
        });

        ViewGroup view = (ViewGroup) super.onCreateView(inflater, container, savedInstanceState);
        view.addView(mEmptyView);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        reloadTriggerListItems();
        mListAdapter = new BluetoothTriggerAdapter(getActivity());
        setListAdapter(mListAdapter);
    }

    private void removeTrigger(List<Trigger> triggers, int value) {
        for (Trigger t : triggers) {
            if (t.value == value) {
                triggers.remove(t);
                return;
            }
        }
    }

    private void reloadTriggerListItems() {
        mTriggers.clear();
        final Resources res = getResources();

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (!pairedDevices.isEmpty()) {
            for (BluetoothDevice device : pairedDevices) {
                BluetoothTrigger bt =
                        new BluetoothTrigger(device);
                int state = mProfile.getTriggerState(
                        Profile.TriggerType.BLUETOOTH, bt.getAddress());
                initPreference(bt, state, res, R.drawable.ic_settings_bluetooth2);
                mTriggers.add(bt);
            }
        } else {
            final List<Profile.ProfileTrigger> triggers =
                    mProfile.getTriggersFromType(Profile.TriggerType.BLUETOOTH);
            for (Profile.ProfileTrigger trigger : triggers) {
                BluetoothTrigger bt = new BluetoothTrigger(trigger.getName(), trigger.getId());
                initPreference(bt, trigger.getState(), res, R.drawable.ic_settings_bluetooth2);
                mTriggers.add(bt);
            }
        }

        if (mListAdapter != null) {
            mListAdapter.notifyDataSetChanged();
        }
    }

    private class Trigger {
        int value;
        String name;
    }

    private class BluetoothTriggerAdapter extends ArrayAdapter<BluetoothTrigger> {
        public BluetoothTriggerAdapter(Context context) {
            super(context, R.layout.abstract_trigger_row, R.id.title, mTriggers);
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            View rowView = inflater.inflate(R.layout.abstract_trigger_row, viewGroup, false);
            TextView title = (TextView) rowView.findViewById(R.id.title);
            TextView desc = (TextView) rowView.findViewById(R.id.desc);
            ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);

            BluetoothTrigger trigger = getItem(i);

            title.setText(trigger.getTitle());
            desc.setText(trigger.getSummary());
            imageView.setImageResource(trigger.getIcon());

            return rowView;
        }
    }

    public static class BluetoothTrigger extends AbstractTriggerItem {
        private String mAddress;

        public BluetoothTrigger(BluetoothDevice device) {
            mAddress = device.getAddress();
            if (device.getAlias() != null) {
                setTitle(device.getAlias());
            } else {
                setTitle(device.getName());
            }
        }

        public BluetoothTrigger(String name, String address) {
            mAddress = address;
            setTitle(name);
        }

        public String getAddress() {
            return mAddress;
        }
    }
}
