/*
 *  Copyright (C) 2017 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
*/
package com.android.settings.rr.fragments;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import android.widget.*;
import android.widget.CompoundButton.OnCheckedChangeListener;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import com.android.settings.rr.fragments.CPUStateMonitor.CpuState;
import com.android.settings.rr.fragments.CPUStateMonitor.CPUStateMonitorException;

public class TimeInState extends SettingsPreferenceFragment {
    private static final String TAG = "TimeInState";
    public static final String PREF_OFFSETS = "pref_offsets";
    public static final String PREF_STATE_MODE = "pref_state_mode";
    public static final String PREF_CORE_MODE = "pref_core_mode";

    private LinearLayout mStatesView;
    private TextView mTotalStateTime;
    private TextView mStatesWarning;
    private CheckBox mStateMode;
    private boolean mUpdatingData;
    private CPUStateMonitor monitor;
    private Context mContext;
    private int mCpuNum;
    private boolean mActiveStateMode;
    private boolean mActiveCoreMode;
    private Spinner mPeriodTypeSelect;
    private LinearLayout mProgress;
    private CheckBox mCoreMode;
    private int mPeriodType = 1;
    private boolean sHasRefData;
    private Intent mShareIntent;
    private List<Integer> mShowCpus;
    private static boolean sResetStats;

    private static final int MENU_REFRESH = Menu.FIRST;
    private static final int MENU_SHARE = MENU_REFRESH + 1;
    private static final String SHARED_PREFERENCES_NAME = "time_in_state";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        boolean statsInMsecs = getResources().getBoolean(R.bool.config_cpufreq_msecs);

        mShowCpus = new ArrayList<Integer>();
        String showCpus = getResources().getString(R.string.config_cpufreq_show_cpus);
        if (!TextUtils.isEmpty(showCpus)) {
            String[] parts = showCpus.split(",");
            for (String cpu : parts) {
                mShowCpus.add(Integer.valueOf(cpu));
            }
        }
        monitor = new CPUStateMonitor(mShowCpus, statsInMsecs);
        mActiveCoreMode = mShowCpus.size() > 1;

        mCpuNum = Helpers.getNumOfCpus();
        mPeriodType = getPrefs().getInt("which", 1);
        if (savedInstanceState != null) {
            mPeriodType = savedInstanceState.getInt("which");
        }

        if (sResetStats) {
            sResetStats = false;
            clearOffsets();
        }
        loadOffsets();

        setHasOptionsMenu(true);
    }

    private SharedPreferences getPrefs() {
        return mContext.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, root, savedInstanceState);

        View view = inflater.inflate(R.layout.time_in_state, root, false);

        mStatesView = (LinearLayout) view.findViewById(R.id.ui_states_view);
        mStatesWarning = (TextView) view.findViewById(R.id.ui_states_warning);
        mTotalStateTime = (TextView) view
                .findViewById(R.id.ui_total_state_time);

        mStateMode = (CheckBox) view.findViewById(R.id.ui_mode_switch);
        mActiveStateMode = getPrefs().getBoolean(PREF_STATE_MODE, false);
        mStateMode.setChecked(mActiveStateMode);
        mStateMode.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                mActiveStateMode = isChecked;
                SharedPreferences.Editor editor = getPrefs().edit();
                editor.putBoolean(PREF_STATE_MODE, mActiveStateMode).commit();
                updateView();
            }
        });

        mCoreMode = (CheckBox) view.findViewById(R.id.ui_core_switch);
        mCoreMode.setVisibility(View.GONE);

        mPeriodTypeSelect = (Spinner) view
                .findViewById(R.id.period_type_select);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                mContext, R.array.period_type_entries, R.layout.spinner_item);
        mPeriodTypeSelect.setAdapter(adapter);
        mPeriodTypeSelect
                .setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent,
                            View view, int position, long id) {
                        mPeriodType = position;
                        if (position == 0) {
                            loadOffsets();
                        } else if (position == 1) {
                            monitor.removeOffsets();
                        }
                        refreshData();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {
                    }
                });
        mPeriodTypeSelect.setSelection(mPeriodType);
        mProgress = (LinearLayout) view.findViewById(R.id.ui_progress);
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("which", mPeriodType);
    }

    @Override
    public void onResume() {
        mUpdatingData = false;
        refreshData();
        super.onResume();
    }

    @Override
    public void onPause() {
        getPrefs().edit().putInt("which", mPeriodType).commit();
        super.onPause();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.time_in_state_menu, menu);

        menu.add(0, MENU_REFRESH, 0, R.string.mt_refresh)
                .setIcon(R.drawable.ic_menu_refresh_new)
                .setAlphabeticShortcut('r')
                .setShowAsAction(
                        MenuItem.SHOW_AS_ACTION_IF_ROOM
                                | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        menu.add(1, MENU_SHARE, 0, R.string.mt_share)
                .setIcon(R.drawable.ic_menu_share_material)
                .setAlphabeticShortcut('s')
                .setShowAsAction(
                        MenuItem.SHOW_AS_ACTION_IF_ROOM
                                | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_REFRESH:
            refreshData();
            break;
        case R.id.reset:
            createResetPoint();
            break;
        case MENU_SHARE:
            if (mShareIntent != null) {
                Intent intent = Intent.createChooser(mShareIntent, null);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void createResetPoint() {
        try {
            monitor.setOffsets();
        } catch (Exception e) {
            // not good
        }
        saveOffsets();
        if (mPeriodType == 1) {
            monitor.removeOffsets();
        }
        refreshData();
    }

    public void updateView() {
        if (mUpdatingData) {
            return;
        }

        StringBuffer data = new StringBuffer();
        mStatesView.removeAllViews();

        if (monitor.getStates(0).size() == 0) {
            mStatesWarning.setVisibility(View.VISIBLE);
            mTotalStateTime.setText(getResources().getString(R.string.total_time)
                    + " " + toString(0));
            mStatesView.setVisibility(View.GONE);
        } else {
            if (mPeriodType == 0 && !sHasRefData) {
                mStatesWarning.setVisibility(View.VISIBLE);
                mStatesWarning.setText(getResources().getString(R.string.no_stat_because_reset_state));
                mTotalStateTime.setText(getResources().getString(R.string.total_time)
                        + " " + toString(0));
                mStatesView.setVisibility(View.VISIBLE);
            } else {
                mStatesWarning.setVisibility(View.GONE);
                mStatesView.setVisibility(View.VISIBLE);
                long totTime = getStateTime(mActiveStateMode);
                data.append(totTime + "\n");
                totTime = totTime / 1000;
                if (!mActiveStateMode) {
                    CpuState deepSleepState = monitor.getDeepSleepState();
                    if (deepSleepState != null) {
                        generateStateRowHeader(deepSleepState, mStatesView);
                        generateStateRow(deepSleepState, mStatesView);
                        data.append(deepSleepState.freq + " "
                                + deepSleepState.getDuration() + "\n");
                    }
                }
                if (mActiveCoreMode) {
                    int cpu = 0;
                    for (int freq : monitor.getFrequencies()) {
                        boolean headerCreated = false;
                        for (cpu = 0; cpu < mCpuNum; cpu++) {
                            if (mShowCpus != null) {
                                if (!mShowCpus.contains(cpu)) {
                                    continue;
                                }
                            }
                            CpuState state = monitor.getFreqState(cpu, freq);
                            if (state == null) {
                                continue;
                            }
                            if (!headerCreated) {
                                generateStateRowHeader(state, mStatesView);
                                headerCreated = true;
                            }
                            generateStateRow(state, mStatesView);
                            data.append(state.mCpu + " " + state.freq + " "
                                    + state.getDuration() + "\n");
                        }
                    }
                } else {
                    for (CpuState state : monitor.getStates(0)) {
                        if (state.freq == 0) {
                            continue;
                        }
                        generateStateRowHeader(state, mStatesView);
                        generateStateRow(state, mStatesView);
                        data.append(state.freq + " " + state.getDuration() + "\n");
                    }
                }

                mTotalStateTime.setText(getResources().getString(R.string.total_time)
                        + " " + toString(totTime));
            }
        }
        updateShareIntent(data.toString());
    }

    public void refreshData() {
        if (!mUpdatingData) {
            new RefreshStateDataTask().execute((Void) null);
        }
    }

    private static String toString(long tSec) {
        long h = (long) Math.max(0, Math.floor(tSec / (60 * 60)));
        long m = (long) Math.max(0, Math.floor((tSec - h * 60 * 60) / 60));
        long s = Math.max(0, tSec % 60);
        String sDur;
        sDur = h + ":";
        if (m < 10)
            sDur += "0";
        sDur += m + ":";
        if (s < 10)
            sDur += "0";
        sDur += s;

        return sDur;
    }

    private View generateStateRow(CpuState state, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        LinearLayout view = (LinearLayout) inflater.inflate(
                R.layout.state_row_line, parent, false);

        float per = 0f;
        String sPer = "";
        String sDur = "";
        String sCpu = " ";
        long tSec = 0;

        if (state != null) {
            long duration = state.getDuration();
            if (duration != 0) {
                per = (float) duration * 100 / getStateTime(mActiveStateMode);
                if (per > 100f) {
                    per = 0f;
                }
                if (per < 0f) {
                    per = 0f;
                }
                tSec = duration / 1000;
            }
            sPer = String.format("%3d", (int) per) + "%";
            sDur = toString(tSec);
            if (state.freq != 0 && mActiveCoreMode) {
                sCpu = String.valueOf(state.mCpu);
            }
        }

        TextView cpuText = (TextView) view.findViewById(R.id.ui_cpu_text);
        TextView durText = (TextView) view.findViewById(R.id.ui_duration_text);
        TextView perText = (TextView) view
                .findViewById(R.id.ui_percentage_text);
        ProgressBar bar = (ProgressBar) view.findViewById(R.id.ui_bar);

        cpuText.setText(sCpu);
        perText.setText(sPer);
        durText.setText(sDur);
        bar.setProgress((int) per);

        parent.addView(view);
        return view;
    }

    private View generateStateRowHeader(CpuState state, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        LinearLayout view = (LinearLayout) inflater.inflate(
                R.layout.state_row_header, parent, false);

        String sFreq;
        if (state.freq == 0) {
            sFreq = getString(R.string.deep_sleep);
        } else {
            sFreq = state.freq / 1000 + " MHz";
        }

        TextView freqText = (TextView) view.findViewById(R.id.ui_freq_text);
        freqText.setText(sFreq);

        parent.addView(view);
        return view;
    }

    protected class RefreshStateDataTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... v) {
            try {
                monitor.updateStates();
            } catch (CPUStateMonitorException e) {
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            mProgress.setVisibility(View.VISIBLE);
            mUpdatingData = true;
        }

        @Override
        protected void onPostExecute(Void v) {
            try {
                mProgress.setVisibility(View.GONE);
                mUpdatingData = false;
                updateView();
            } catch(Exception e) {
            }
        }
    }

    private void loadOffsets() {
        String prefs = getPrefs().getString(PREF_OFFSETS, "");
        if (TextUtils.isEmpty(prefs)) {
            return;
        }
        String[] cpus = prefs.split(":");
        if (cpus.length != mCpuNum) {
            return;
        }
        try {
            for (int cpu = 0; cpu < mCpuNum; cpu++) {
                if (mShowCpus != null) {
                    if (!mShowCpus.contains(cpu)) {
                        continue;
                    }
                }
                String cpuData = cpus[cpu];
                Map<Integer, Long> offsets = new HashMap<Integer, Long>();
                String[] sOffsets = cpuData.split(",");
                for (String offset : sOffsets) {
                    String[] parts = offset.split(";");
                    offsets.put(Integer.parseInt(parts[0]),
                            Long.parseLong(parts[1]));
                }
                monitor.setOffsets(cpu, offsets);
            }
        } catch (NumberFormatException e) {
        }
        sHasRefData = true;
    }

    private void saveOffsets() {
        StringBuffer str = new StringBuffer();
        for (int cpu = 0; cpu < mCpuNum; cpu++) {
            boolean saveCpu = true;
            if (mShowCpus != null) {
                if (!mShowCpus.contains(cpu)) {
                    // just placeholder
                    str.append("cpu");
                    saveCpu = false;
                }
            }
            if (saveCpu) {
                int size = monitor.getOffsets(cpu).entrySet().size();
                int i = 0;
                for (Map.Entry<Integer, Long> entry : monitor.getOffsets(cpu)
                        .entrySet()) {
                    str.append(entry.getKey() + ";" + entry.getValue());
                    if (i < size - 1) {
                        str.append(",");
                    }
                    i++;
                }
            }
            if (cpu < mCpuNum - 1) {
                str.append(":");
            }
        }
        getPrefs().edit().putString(PREF_OFFSETS, str.toString()).commit();
        sHasRefData = true;
    }

    private void clearOffsets() {
        getPrefs().edit().putString(PREF_OFFSETS, "").commit();
        sHasRefData = false;
    }

    private long getStateTime(boolean activeMode) {
        long total = monitor.getTotalStateTime(0, true);
        if (activeMode) {
            CpuState deepSleepState = monitor.getDeepSleepState();
            return total - deepSleepState.getDuration();
        }
        return total;
    }

    private void updateShareIntent(String data) {
        mShareIntent = new Intent();
        mShareIntent.setAction(Intent.ACTION_SEND);
        mShareIntent.setType("text/plain");
        mShareIntent.putExtra(Intent.EXTRA_TEXT, data);
    }

    public static void triggerResetStats() {
        // on reboot
        sResetStats = true;
    }
}

