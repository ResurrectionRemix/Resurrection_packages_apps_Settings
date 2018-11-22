/*
 * Copyright (C) 2014 The LiquidSmooth Project
 * Copyright (C) 2017 faust93 at monumentum@gmail.com
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

package com.android.settings.rr.fragments;

import android.content.Context;
import android.os.Bundle;
import android.app.AlarmManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.widget.Switch;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.AdapterView;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.app.AlertDialog;
import android.content.DialogInterface;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import com.android.internal.logging.nano.MetricsProto;

public class AlarmBlocker extends SettingsPreferenceFragment {

    private static final String TAG = "AlarmBlocker";

    private Switch mBlockerEnabled;
    private ListView mAlarmList;
    private List<String> mSeenAlarms;
    private List<String> mBlockedAlarms;
    private LayoutInflater mInflater;
    private Map<String, Boolean> mAlarmState;
    private AlarmListAdapter mListAdapter;
    private boolean mEnabled;
    private AlertDialog mAlertDialog;
    private boolean mAlertShown = false;
    private TextView mAlarmListHeader;

    private static final int MENU_RELOAD = Menu.FIRST;
    private static final int MENU_SAVE = Menu.FIRST + 1;

    public class AlarmListAdapter extends ArrayAdapter<String> {

        public AlarmListAdapter(Context context, int resource, List<String> values) {
            super(context, R.layout.alarm_item, resource, values);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = mInflater.inflate(R.layout.alarm_item, parent, false);
            final CheckBox check = (CheckBox)rowView.findViewById(R.id.alarm_blocked);
            check.setText(mSeenAlarms.get(position));

            Boolean checked = mAlarmState.get(check.getText().toString());
            check.setChecked(checked.booleanValue());

            if(checked.booleanValue()){
                check.setTextColor(getResources().getColor(android.R.color.holo_red_light));
            }

            check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton v, boolean checked) {
                        mAlarmState.put(v.getText().toString(), new Boolean(checked));
                        if(checked){
                            check.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                            mListAdapter.notifyDataSetChanged();
                        } else {
                            check.setTextColor(getResources().getColor(android.R.color.primary_text_dark));
                            mListAdapter.notifyDataSetChanged();
                        }
                    }
            });
            return rowView;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("maxwen", "running");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mInflater = inflater;
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.alarm_blocker, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAlarmState = new HashMap<String, Boolean>();
        updateSeenAlarmsList();
        updateBlockedAlarmsList();

        mBlockerEnabled = (Switch) getActivity().findViewById(
                R.id.alarm_blocker_switch);
        mAlarmList = (ListView) getActivity().findViewById(
                R.id.alarm_list);
        mAlarmListHeader = (TextView) getActivity().findViewById(
                R.id.alarm_list_header);

        mListAdapter = new AlarmListAdapter(getActivity(), android.R.layout.simple_list_item_multiple_choice,
                mSeenAlarms);
        mAlarmList.setAdapter(mListAdapter);

        updateSwitches();

        // after updateSwitches!!!
        mBlockerEnabled
                .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton v, boolean checked) {
                        if (checked && isFirstEnable() && !mAlertShown){
                            showAlert();
                            mAlertShown = true;
                        }

                        Settings.System.putInt(getActivity().getContentResolver(),
                                Settings.System.ALARM_BLOCKING_ENABLED,
                                checked?1:0);

                        updateSwitches();
                    }
                });
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.RESURRECTED;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private boolean isFirstEnable() {
        return Settings.System.getString(getActivity().getContentResolver(),
                Settings.System.ALARM_BLOCKING_ENABLED) == null;
    }

    private void updateSwitches() {
        mBlockerEnabled.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.ALARM_BLOCKING_ENABLED, 0)==1?true:false);

        mEnabled = mBlockerEnabled.isChecked();
        mAlarmList.setVisibility(mEnabled ?View.VISIBLE : View.INVISIBLE);
        mAlarmListHeader.setVisibility(mEnabled ?View.VISIBLE : View.INVISIBLE);
    }

    private void updateSeenAlarmsList() {
        AlarmManager pm = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
        Log.d("AlarmBlocker", pm.getSeenAlarms());

        String seenAlarms =  pm.getSeenAlarms();
        mSeenAlarms = new ArrayList<String>();

        if (seenAlarms!=null && seenAlarms.length()!=0){
            String[] parts = seenAlarms.split("\\|");
            for(int i = 0; i < parts.length; i++){
                mSeenAlarms.add(parts[i]);
                mAlarmState.put(parts[i], new Boolean(false));
            }
        }
    }

    private void updateBlockedAlarmsList() {
        String blockedAlarmList = Settings.System.getString(getActivity().getContentResolver(),
                Settings.System.ALARM_BLOCKING_LIST);

        mBlockedAlarms = new ArrayList<String>();

        if (blockedAlarmList!=null && blockedAlarmList.length()!=0){
            String[] parts = blockedAlarmList.split("\\|");
            for(int i = 0; i < parts.length; i++){
                mBlockedAlarms.add(parts[i]);

                // add all blocked but not seen so far
                if(!mSeenAlarms.contains(parts[i])){
                    mSeenAlarms.add(parts[i]);
                }
                mAlarmState.put(parts[i], new Boolean(true));
            }
        }

        Collections.sort(mSeenAlarms);
    }

    private void save(){
        StringBuffer buffer = new StringBuffer();
        Iterator<String> nextState = mAlarmState.keySet().iterator();
        while(nextState.hasNext()){
            String name = nextState.next();
            Boolean state=mAlarmState.get(name);
            if(state.booleanValue()){
                buffer.append(name + "|");
            }
        }
        if(buffer.length()>0){
            buffer.deleteCharAt(buffer.length() - 1);
        }
        Settings.System.putString(getActivity().getContentResolver(),
                Settings.System.ALARM_BLOCKING_LIST, buffer.toString());
    }

    private void reload(){
        mAlarmState = new HashMap<String, Boolean>();
        updateSeenAlarmsList();
        updateBlockedAlarmsList();

        mListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RELOAD, 0, R.string.alarm_blocker_reload)
                .setIcon(com.android.internal.R.drawable.ic_menu_refresh)
                .setAlphabeticShortcut('r')
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
                        MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        menu.add(0, MENU_SAVE, 0, R.string.alarm_blocker_save)
                .setIcon(R.drawable.ic_wakelockblocker_save)
                .setAlphabeticShortcut('s')
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
                        MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RELOAD:
                if (mEnabled){
                    reload();
                }
                return true;
            case MENU_SAVE:
                if (mEnabled){
                    save();
                }
                return true;
            default:
                return false;
        }
    }

    private void showAlert() {
        /* Display the warning dialog */
        mAlertDialog = new AlertDialog.Builder(getActivity()).create();
        mAlertDialog.setTitle(R.string.alarm_blocker_warning_title);
        mAlertDialog.setMessage(getResources().getString(R.string.alarm_blocker_warning));
        mAlertDialog.setButton(DialogInterface.BUTTON_POSITIVE,
                getResources().getString(com.android.internal.R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        return;
                    }
                });
        mAlertDialog.show();
    }
}
