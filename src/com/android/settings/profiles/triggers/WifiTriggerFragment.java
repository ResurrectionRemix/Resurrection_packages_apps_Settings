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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
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

public class WifiTriggerFragment extends ListFragment {
    WifiManager mWifiManager;
    Profile mProfile;
    private ProfileManager mProfileManager;

    private View mEmptyView;

    private List<WifiTrigger> mTriggers = new ArrayList<WifiTrigger>();
    private WifiTriggerAdapter mListAdapter;

    public static WifiTriggerFragment newInstance(Profile profile) {
        WifiTriggerFragment fragment = new WifiTriggerFragment();

        Bundle extras = new Bundle();
        extras.putParcelable(ProfilesSettings.EXTRA_PROFILE, profile);

        fragment.setArguments(extras);
        return fragment;
    }

    public WifiTriggerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mProfile = getArguments().getParcelable(ProfilesSettings.EXTRA_PROFILE);
        } else {
            throw new UnsupportedOperationException("no profile!");
        }
        mProfileManager = ProfileManager.getInstance(getActivity());
        mWifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
    }


    @Override
    public void onStart() {
        super.onStart();
        getListView().setEmptyView(mEmptyView);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mEmptyView = inflater.inflate(R.layout.profile_wifi_empty_view, container, false);
        mEmptyView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent wifiSettings = new Intent();
                wifiSettings.setAction(
                        Settings.ACTION_WIFI_SETTINGS);
                startActivity(wifiSettings);
            }
        });

        ViewGroup view = (ViewGroup) super.onCreateView(inflater, container, savedInstanceState);
        view.addView(mEmptyView);
        return view;
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

        WifiTrigger pref = (WifiTrigger) l.getAdapter().getItem(position);
        triggerName = pref.getTitle();
        triggerId = pref.getSSID();
        triggerType = Profile.TriggerType.WIFI;
        removeTrigger(triggers, Profile.TriggerState.ON_A2DP_CONNECT);
        removeTrigger(triggers, Profile.TriggerState.ON_A2DP_DISCONNECT);

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

    private void removeTrigger(List<Trigger> triggers, int value) {
        for (Trigger t : triggers) {
            if (t.value == value) {
                triggers.remove(t);
                return;
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        reloadTriggerListItems();
        mListAdapter = new WifiTriggerAdapter(getActivity());
        setListAdapter(mListAdapter);
    }

    private void reloadTriggerListItems() {
        mTriggers.clear();
        final Resources res = getResources();
        final List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();

        if (configs != null) {
            for (WifiConfiguration config : configs) {
                WifiTrigger accessPoint = new WifiTrigger(config);
                int state = mProfile.getTriggerState(
                        Profile.TriggerType.WIFI, accessPoint.getSSID());
                initPreference(accessPoint, state, res, R.drawable.ic_wifi_signal_4_teal);
                mTriggers.add(accessPoint);
            }
        } else {
            final List<Profile.ProfileTrigger> triggers =
                    mProfile.getTriggersFromType(Profile.TriggerType.WIFI);
            for (Profile.ProfileTrigger trigger : triggers) {
                WifiTrigger accessPoint = new WifiTrigger(trigger.getName());
                initPreference(accessPoint, trigger.getState(), res,
                        R.drawable.ic_wifi_signal_4_teal);
                mTriggers.add(accessPoint);
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

    private class WifiTriggerAdapter extends ArrayAdapter<WifiTrigger> {
        public WifiTriggerAdapter(Context context) {
            super(context, R.layout.abstract_trigger_row, R.id.title, mTriggers);
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            View rowView = inflater.inflate(R.layout.abstract_trigger_row, viewGroup, false);
            TextView title = (TextView) rowView.findViewById(R.id.title);
            TextView desc = (TextView) rowView.findViewById(R.id.desc);
            ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);

            WifiTrigger trigger = getItem(i);

            title.setText(trigger.getTitle());
            desc.setText(trigger.getSummary());
            imageView.setImageResource(trigger.getIcon());

            return rowView;
        }
    }

    public static class WifiTrigger extends AbstractTriggerItem {
        public String mSSID;
        public WifiConfiguration mConfig;

        public WifiTrigger(WifiConfiguration config) {
            mConfig = config;
            loadConfig(config);
        }

        public WifiTrigger(String ssid) {
            mSSID = ssid;
        }

        public String getSSID() {
            return mSSID;
        }

        @Override
        public String getTitle() {
            return mSSID;
        }

        private void loadConfig(WifiConfiguration config) {
            mSSID = (config.SSID == null ? "" : removeDoubleQuotes(config.SSID));
            mConfig = config;
        }

        public static String removeDoubleQuotes(String string) {
            final int length = string.length();
            if (length >= 2) {
                if (string.startsWith("\"") && string.endsWith("\"")) {
                    return string.substring(1, length - 1);
                }
            }
            return string;
        }
    }
}
