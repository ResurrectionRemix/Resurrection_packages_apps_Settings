/*
 * Performance Control - An Android CPU Control application Copyright (C)
 * Brandon Valosek, 2011 <bvalosek@gmail.com> Copyright (C) Modified by 2012
 * James Roberts
 * Copyright (C) 2017 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.android.settings.rr.fragments;

import android.os.SystemClock;
import android.util.Log;

import java.io.*;
import java.util.*;

public class CPUStateMonitor {
    public static final String TIME_IN_STATE_PATH = "/sys/devices/system/cpu/cpu0/cpufreq/stats/time_in_state";
    public static final String PREF_OFFSETS = "pref_offsets";
    public static final String TIME_IN_STATE_OVERALL_PATH = "/sys/devices/system/cpu/cpufreq/overall_stats/overall_time_in_state";

    private Map<Integer, ArrayList<CpuState>> mStates;
    private Map<Integer, Map<Integer, Long>> mOffsets;
    private boolean mOverallStats;
    private int mCpuNum;
    private List<Integer> mFrequencies;
    private List<Integer> mShowCpus;
    private boolean mStatsInMsecs;

    public CPUStateMonitor(List<Integer> showCpus, boolean statsInMsecs) {
        mStatsInMsecs = statsInMsecs;
        mShowCpus = showCpus;
        mCpuNum = Helpers.getNumOfCpus();
        mFrequencies = new ArrayList<Integer>();
        mStates = new HashMap<Integer, ArrayList<CpuState>>();
        mOffsets = new HashMap<Integer, Map<Integer, Long>>();
        for (int i = 0; i < mCpuNum; i++) {
            ArrayList<CpuState> cpuStates = new ArrayList<CpuState>();
            mStates.put(i, cpuStates);

            Map<Integer, Long> cpuOffsets = new HashMap<Integer, Long>();
            mOffsets.put(i, cpuOffsets);
        }
        mOverallStats = Helpers.hasOverallStats();
    }

    public boolean hasOverallStats() {
        return mOverallStats;
    }

    @SuppressWarnings("serial")
    public class CPUStateMonitorException extends Exception {
        public CPUStateMonitorException(String s) {
            super(s);
        }
    }

    // @SuppressLint({"UseValueOf", "UseValueOf"})
    public class CpuState implements Comparable<CpuState> {
        public CpuState(int cpu, int a, long b, boolean msecs) {
            mCpu = cpu;
            freq = a;
            duration = msecs ? b : (b * 10);
        }

        public int freq = 0;
        public long duration = 0;
        public int mCpu = 0;

        @Override
        public String toString() {
            return mCpu + ":" + freq + ":" + duration;
        }

        public int compareTo(CpuState state) {
            Integer a = freq;
            Integer b = state.freq;
            return a.compareTo(b);
        }

        public long getDuration() {
            Map<Integer, Long> offsets = getOffsets(mCpu);
            Long offset = offsets.get(freq);
            if (offset != null) {
                return duration - offset;
            }
            return duration;
        }
    }

    public List<CpuState> getStates(int cpu) {
        return mStates.get(cpu);
    }

    public List<Integer> getFrequencies() {
        return mFrequencies;
    }

    public CpuState getFreqState(int cpu, int freq) {
        List<CpuState> cpuStates = mStates.get(cpu);
        for (CpuState state : cpuStates) {
            if (state.freq == freq) {
                return state;
            }
        }
        return null;
    }

    public CpuState getDeepSleepState() {
        List<CpuState> cpuStates = mStates.get(0);
        for (CpuState state : cpuStates) {
            if (state.freq == 0) {
                return state;
            }
        }
        return null;
    }

    public long getTotalStateTime(int cpu, boolean withOffset) {
        long sum = 0;
        long offset = 0;

        List<CpuState> cpuStates = mStates.get(cpu);
        for (CpuState state : cpuStates) {
            if (withOffset) {
                sum += state.getDuration();
            } else {
                sum += state.duration;
            }
        }
        return sum;
    }

    public Map<Integer, Long> getOffsets(int cpu) {
        Map<Integer, Long> cpuOffsets = mOffsets.get(cpu);
        return cpuOffsets;
    }

    public void setOffsets(int cpu, Map<Integer, Long> offsets) {
        mOffsets.put(cpu, offsets);
    }

    public void setOffsets() throws CPUStateMonitorException {
        updateStates();
        for (int i = 0; i < mCpuNum; i++) {
            if (mShowCpus != null) {
                if (!mShowCpus.contains(i)) {
                    continue;
                }
            }
            setOffsets(i);
        }
    }

    private void setOffsets(int cpu) throws CPUStateMonitorException {
        Map<Integer, Long> cpuOffsets = mOffsets.get(cpu);
        cpuOffsets.clear();

        List<CpuState> cpuStates = mStates.get(cpu);
        for (CpuState state : cpuStates) {
            cpuOffsets.put(state.freq, state.duration);
        }
    }

    public void removeOffsets() {
        for (int i = 0; i < mCpuNum; i++) {
            removeOffsets(i);
        }
    }

    private void removeOffsets(int cpu) {
        Map<Integer, Long> cpuOffsets = mOffsets.get(cpu);
        cpuOffsets.clear();
    }

    public void clear() {
        for (int i = 0; i < mCpuNum; i++) {
            List<CpuState> cpuStates = mStates.get(i);
            cpuStates.clear();
        }
    }

    public void updateStates() throws CPUStateMonitorException {
        mFrequencies.clear();
        if (mOverallStats) {
            try {
                InputStream is = new FileInputStream(TIME_IN_STATE_OVERALL_PATH);
                InputStreamReader ir = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(ir);
                clear();
                readInOverallStates(br);
                is.close();
            } catch (IOException e) {
                throw new CPUStateMonitorException(
                        "Problem opening time-in-states file");
            }
        } else {
            try {
                clear();
                for (int i = 0; i < mCpuNum; i++) {
                    if (mShowCpus != null) {
                        if (!mShowCpus.contains(i)) {
                            continue;
                        }
                    }
                    List<CpuState> cpuStates = mStates.get(i);
                    InputStream is = new FileInputStream(getCpuFreqPathFor(i));
                    InputStreamReader ir = new InputStreamReader(is);
                    BufferedReader br = new BufferedReader(ir);
                    readInStates(br, i, cpuStates);
                    is.close();
                }
            } catch (IOException e) {
                throw new CPUStateMonitorException(
                        "Problem opening time-in-states file");
            }
        }

        List<CpuState> cpuStates = mStates.get(0);
        long sleepTime = Math.max((SystemClock.elapsedRealtime() - SystemClock
                .uptimeMillis()), 0);
        cpuStates.add(new CpuState(0, 0, sleepTime, true));
        Collections.sort(mFrequencies);
    }

    private void readInStates(BufferedReader br, int cpu,
            List<CpuState> cpuStates) throws CPUStateMonitorException {
        try {
            String line;
            while ((line = br.readLine()) != null) {
                String[] nums = line.split(" ");
                int freq = Integer.parseInt(nums[0]);
                cpuStates.add(new CpuState(cpu, freq, Long
                        .parseLong(nums[1]), mStatsInMsecs));
                if (!mFrequencies.contains(freq)) {
                    mFrequencies.add(freq);
                }
            }
            Collections.sort(cpuStates);
        } catch (IOException e) {
            throw new CPUStateMonitorException(
                    "Problem processing time-in-states file");
        }
    }

    private void readInOverallStates(BufferedReader br)
            throws CPUStateMonitorException {
        int cpu = 0;
        List<CpuState> cpuStates = null;
        ;
        int firstFreq = 0;
        try {
            String line;
            while ((line = br.readLine()) != null) {
                String[] nums = line.split(" ");
                int freq = Integer.parseInt(nums[0]);
                if (firstFreq == 0) {
                    firstFreq = freq;
                } else if (freq == firstFreq) {
                    cpu++;
                    if (cpuStates != null) {
                        Collections.sort(cpuStates);
                    }
                }
                cpuStates = mStates.get(cpu);
                cpuStates.add(new CpuState(cpu, freq, Long.parseLong(nums[1]), mStatsInMsecs));
                if (!mFrequencies.contains(freq)) {
                    mFrequencies.add(freq);
                }
            }
        } catch (IOException e) {
            throw new CPUStateMonitorException(
                    "Problem processing time-in-states file");
        }
    }

    public void dump() {
        Log.d("PC", "states = " + mStates + "\noffsets = " + mOffsets);
    }

    private String getCpuFreqPathFor(int cpu) {
        return TIME_IN_STATE_PATH.replace("cpu0", "cpu" + cpu);
    }
}
