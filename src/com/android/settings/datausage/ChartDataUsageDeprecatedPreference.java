/*
 * Copyright (c) 2016, The Linux Foundation. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.
    * Neither the name of The Linux Foundation nor the names of its
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package com.android.settings.datausage;

import android.animation.LayoutTransition;

import android.content.Context;

import android.net.NetworkPolicy;
import android.net.NetworkStats;
import android.net.NetworkStatsHistory;

import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;

import android.util.AttributeSet;
import android.util.Log;

import android.view.View;

import com.android.settings.R;
import com.android.settings.widget.ChartDataUsageView;
import com.android.settings.widget.ChartDataUsageView.DataUsageChartListener;

public class ChartDataUsageDeprecatedPreference extends Preference {
    private static final String TAG = "ChartDataUsageDeprecatedPreference";
    private Context mContext = null;

    private ChartDataUsageView mChartDataUsageView = null;
    private DataUsageChartListener mChartListener = null;

    private NetworkStatsHistory mNetwork;
    private NetworkStatsHistory mDetail;
    private NetworkPolicy mPolicy;

    private long mInspectStart;
    private long mInspectEnd;
    private long mSelectLeft;
    private long mSelectRight;

    private boolean mInspectRangeChanged = false;


    public ChartDataUsageDeprecatedPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setLayoutResource(R.layout.data_usage_chart);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        mChartDataUsageView = (ChartDataUsageView)holder.itemView;
        // No need to update chart again if just get InspectRangeChanged event
        if (!mInspectRangeChanged) {
            updateChart();
        }

        Log.d(TAG, "onBindViewHolder mChartDataUsageView = " + mChartDataUsageView);
    }

    public long getInspectStart() {
        if (mChartDataUsageView != null) {
            return mChartDataUsageView.getInspectStart();
        } else {
            return mInspectStart;
        }
    }

    public long getInspectEnd() {
        if (mChartDataUsageView != null) {
            return mChartDataUsageView.getInspectEnd();
        } else {
            return mInspectEnd;
        }
    }

    public long getInspectLeft() {
        if (mChartDataUsageView != null) {
            return mChartDataUsageView.getInspectLeft();
        } else {
            return mSelectLeft;
        }
    }

    public long getInspectRight() {
        if (mChartDataUsageView != null) {
            return mChartDataUsageView.getInspectRight();
        } else {
            return mSelectRight;
        }
    }

    public long getWarningBytes() {
        return mChartDataUsageView.getWarningBytes();
    }

    public long getLimitBytes() {
        return mChartDataUsageView.getLimitBytes();
    }

    public void setVisibleRange(long visibleStart, long visibleEnd, long selectLeft,
            long selectRight) {
        mInspectStart = visibleStart;
        mInspectEnd = visibleEnd;
        mSelectLeft = selectLeft;
        mSelectRight = selectRight;
        notifyChanged();
    }

    public void bindNetworkPolicy(NetworkPolicy policy) {
        mPolicy = policy;
    }

    public void bindNetworkStats(NetworkStatsHistory stats) {
        mNetwork = stats;
        notifyChanged();
    }

    public void bindDetailNetworkStats(NetworkStatsHistory stats) {
        mDetail = stats;
        notifyChanged();
    }

    public void setListener(DataUsageChartListener listener) {
        mChartListener = listener;
    }

    private void updateChart() {
        mChartDataUsageView.setListener(mChartListener);
        mChartDataUsageView.bindNetworkPolicy(mPolicy);
        mChartDataUsageView.bindNetworkStats(mNetwork);
        mChartDataUsageView.bindDetailNetworkStats(mDetail);
        mChartDataUsageView.setVisibleRange(mInspectStart, mInspectEnd, mSelectLeft, mSelectRight);

        boolean isVisible = BillingCycleSettings.isShowDataUsage(mContext);
        int visible = isVisible ? View.VISIBLE : View.GONE;
        mChartDataUsageView.setDateSelectionSweepVisible(visible);
    }

    public void setInspectRangeChanged() {
        mInspectRangeChanged = true;
    }
}

