/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.settings.fuelgauge;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.BatteryStats;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;
import com.android.internal.os.PowerProfile;
import com.android.settings.HelpUtils;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import java.util.List;

/**
 * Displays a list of apps and subsystems that consume power, ordered by how much power was
 * consumed since the last time it was unplugged.
 */
public class PowerUsageSummary extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final boolean DEBUG = false;

    static final String TAG = "PowerUsageSummary";

    private static final String KEY_APP_LIST = "app_list";

    private static final String KEY_PERF_PROFILE = "pref_perf_profile";

    private static final String KEY_BATTERY_SAVER = "low_power";

    private static final String KEY_PER_APP_PROFILES = "app_perf_profiles_enabled";

    private static final String BATTERY_HISTORY_FILE = "tmp_bat_history.bin";
    private static final String DOCK_BATTERY_HISTORY_FILE = "tmp_dock_bat_history.bin";

    private static final int MENU_STATS_TYPE = Menu.FIRST;
    private static final int MENU_STATS_REFRESH = Menu.FIRST + 1;
    private static final int MENU_STATS_RESET = Menu.FIRST + 2;
    private static final int MENU_BATTERY_SAVER = Menu.FIRST + 3;
    private static final int MENU_HELP = Menu.FIRST + 4;

    private UserManager mUm;

    private BatteryHistoryPreference mHistPref;
    private PreferenceGroup mAppListGroup;
    private String mBatteryLevel;
    private String mBatteryStatus;
    private boolean mBatteryPluggedIn;

    private int mStatsType = BatteryStats.STATS_SINCE_CHARGED;

    private static final int MIN_POWER_THRESHOLD_MILLI_AMP = 5;
    private static final int MAX_ITEMS_TO_LIST = 10;
    private static final int MIN_AVERAGE_POWER_THRESHOLD_MILLI_AMP = 10;
    private static final int SECONDS_IN_HOUR = 60 * 60;

    private BatteryStatsHelper mStatsHelper;

    private BatteryManager mBatteryManager;
    private PowerManager mPowerManager;
    private ListPreference mPerfProfilePref;
    private SwitchPreference mBatterySaverPref;
    private String[] mPerfProfileEntries;
    private String[] mPerfProfileValues;
    private String mPerfProfileDefaultEntry;
    private PerformanceProfileObserver mPerformanceProfileObserver = null;
    private SwitchPreference mPerAppProfiles;

    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)
                    && updateBatteryStatus(intent)) {
                if (!mHandler.hasMessages(MSG_REFRESH_STATS)) {
                    mHandler.sendEmptyMessageDelayed(MSG_REFRESH_STATS, 500);
                }
            }
        }
    };

    private class PerformanceProfileObserver extends ContentObserver {
        public PerformanceProfileObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            updatePerformanceValue();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mUm = (UserManager) activity.getSystemService(Context.USER_SERVICE);
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mStatsHelper = new BatteryStatsHelper(activity, true);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mStatsHelper.create(icicle);

        mBatteryManager = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);

        mPerfProfileEntries = getResources().getStringArray(
                com.android.internal.R.array.perf_profile_entries);
        mPerfProfileValues = getResources().getStringArray(
                com.android.internal.R.array.perf_profile_values);

        addPreferencesFromResource(R.xml.power_usage_summary);
        mAppListGroup = (PreferenceGroup) findPreference(KEY_APP_LIST);
        setHasOptionsMenu(true);

        mPerfProfilePref = (ListPreference) findPreference(KEY_PERF_PROFILE);
        mBatterySaverPref = (SwitchPreference) findPreference(KEY_BATTERY_SAVER);
        mPerAppProfiles = (SwitchPreference) findPreference(KEY_PER_APP_PROFILES);
        if (mPerfProfilePref != null && !mPowerManager.hasPowerProfiles()) {
            removePreference(KEY_PERF_PROFILE);
            removePreference(KEY_PER_APP_PROFILES);
            mPerfProfilePref = null;
            mPerAppProfiles = null;
        } else if (mPerfProfilePref != null) {
            // Remove the battery saver switch, power profiles have 3 modes
            removePreference(KEY_BATTERY_SAVER);
            mBatterySaverPref = null;
            mPerfProfilePref.setOrder(-1);
            mPerfProfilePref.setEntries(mPerfProfileEntries);
            mPerfProfilePref.setEntryValues(mPerfProfileValues);
            updatePerformanceValue();
            mPerfProfilePref.setOnPreferenceChangeListener(this);
        }

        mPerformanceProfileObserver = new PerformanceProfileObserver(new Handler());
    }

    @Override
    public void onStart() {
        super.onStart();
        mStatsHelper.clearStats();
    }

    @Override
    public void onResume() {
        super.onResume();
        BatteryStatsHelper.dropFile(getActivity(), BATTERY_HISTORY_FILE);
        updateBatteryStatus(getActivity().registerReceiver(mBatteryInfoReceiver,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED)));
        if (mHandler.hasMessages(MSG_REFRESH_STATS)) {
            mHandler.removeMessages(MSG_REFRESH_STATS);
            mStatsHelper.clearStats();
        }
        refreshStats();

        if (mPerfProfilePref != null) {
            updatePerformanceValue();
            ContentResolver resolver = getActivity().getContentResolver();
            resolver.registerContentObserver(Settings.Secure.getUriFor(
                    Settings.Secure.PERFORMANCE_PROFILE), false, mPerformanceProfileObserver);
        }
        if (mBatterySaverPref != null) {
            refreshBatterySaverOptions();
        }
    }

    @Override
    public void onPause() {
        BatteryEntry.stopRequestQueue();
        mHandler.removeMessages(BatteryEntry.MSG_UPDATE_NAME_ICON);
        getActivity().unregisterReceiver(mBatteryInfoReceiver);
        super.onPause();

        if (mPerfProfilePref != null) {
            ContentResolver resolver = getActivity().getContentResolver();
            resolver.unregisterContentObserver(mPerformanceProfileObserver);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mHandler.removeMessages(MSG_REFRESH_STATS);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getActivity().isChangingConfigurations()) {
            mStatsHelper.storeState();
            BatteryEntry.clearUidCache();
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference instanceof BatteryHistoryPreference) {
            mStatsHelper.storeStatsHistoryInFile(BATTERY_HISTORY_FILE);
            Bundle args = new Bundle();
            args.putString(BatteryHistoryDetail.EXTRA_STATS, BATTERY_HISTORY_FILE);
            if (mBatteryManager.isDockBatterySupported()) {
                mStatsHelper.storeDockStatsHistoryInFile(DOCK_BATTERY_HISTORY_FILE);
                args.putString(BatteryHistoryDetail.EXTRA_DOCK_STATS, DOCK_BATTERY_HISTORY_FILE);
            }
            args.putParcelable(BatteryHistoryDetail.EXTRA_BROADCAST,
                    mStatsHelper.getBatteryBroadcast());
            SettingsActivity sa = (SettingsActivity) getActivity();
            sa.startPreferencePanel(BatteryHistoryDetail.class.getName(), args,
                    R.string.history_details_title, null, null, 0);
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        if (!(preference instanceof PowerGaugePreference)) {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        PowerGaugePreference pgp = (PowerGaugePreference) preference;
        BatteryEntry entry = pgp.getInfo();
        PowerUsageDetail.startBatteryDetailPage((SettingsActivity) getActivity(), mStatsHelper,
                mStatsType, entry, true);
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (newValue != null) {
            if (preference == mPerfProfilePref) {
                mPowerManager.setPowerProfile(String.valueOf(newValue));
                updatePerformanceSummary();
                return true;
            }
        }
        return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (DEBUG) {
            menu.add(0, MENU_STATS_TYPE, 0, R.string.menu_stats_total)
                    .setIcon(com.android.internal.R.drawable.ic_menu_info_details)
                    .setAlphabeticShortcut('t');
        }
        MenuItem refresh = menu.add(0, MENU_STATS_REFRESH, 0, R.string.menu_stats_refresh)
                .setIcon(com.android.internal.R.drawable.ic_menu_refresh)
                .setAlphabeticShortcut('r');
        refresh.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
                MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        MenuItem reset = menu.add(0, MENU_STATS_RESET, 0, R.string.menu_stats_reset)
                .setIcon(R.drawable.ic_actionbar_delete)
                .setAlphabeticShortcut('d');
        reset.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
                MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        MenuItem batterySaver = menu.add(0, MENU_BATTERY_SAVER, 0,
                R.string.battery_saver_threshold);
        batterySaver.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        String helpUrl;
        if (!TextUtils.isEmpty(helpUrl = getResources().getString(R.string.help_url_battery))) {
            final MenuItem help = menu.add(0, MENU_HELP, 0, R.string.help_label);
            HelpUtils.prepareHelpMenuItem(getActivity(), help, helpUrl);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_STATS_TYPE:
                if (mStatsType == BatteryStats.STATS_SINCE_CHARGED) {
                    mStatsType = BatteryStats.STATS_SINCE_UNPLUGGED;
                } else {
                    mStatsType = BatteryStats.STATS_SINCE_CHARGED;
                }
                refreshStats();
                return true;
            case MENU_STATS_RESET:
                resetStats();
                return true;
            case MENU_STATS_REFRESH:
                mStatsHelper.clearStats();
                refreshStats();
                mHandler.removeMessages(MSG_REFRESH_STATS);
                return true;
            case MENU_BATTERY_SAVER:
                Resources res = getResources();

                final int defWarnLevel = res.getInteger(
                        com.android.internal.R.integer.config_lowBatteryWarningLevel);
                final int value = Settings.Global.getInt(getContentResolver(),
                        Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL, defWarnLevel);

                int selectedIndex = -1;
                final int[] intVals = res.getIntArray(R.array.battery_saver_trigger_values);
                String[] strVals = new String[intVals.length];
                for (int i = 0; i < intVals.length; i++) {
                    if (intVals[i] == value) {
                        selectedIndex = i;
                    }
                    if (intVals[i] > 0 && intVals[i] < 100) {
                        strVals[i] = res.getString(R.string.battery_saver_turn_on_automatically_pct,
                                Utils.formatPercentage(intVals[i]));
                    } else {
                        strVals[i] =
                                res.getString(R.string.battery_saver_turn_on_automatically_never);
                    }
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.battery_saver_turn_on_automatically_title)
                        .setSingleChoiceItems(strVals,
                                selectedIndex,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Settings.Global.putInt(getContentResolver(),
                                                Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL,
                                                intVals[which]);
                                    }
                                })
                        .setPositiveButton(R.string.ok, null);
                builder.create().show();

                return true;
            default:
                return false;
        }
    }

    private void resetStats() {
        AlertDialog dialog = new AlertDialog.Builder(getActivity())
            .setTitle(R.string.menu_stats_reset)
            .setMessage(R.string.reset_stats_msg)
            .setPositiveButton(android.R.string.ok, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Reset stats and request a refresh to initialize vars
                    mStatsHelper.resetStatistics();
                    refreshStats();
                    mHandler.removeMessages(MSG_REFRESH_STATS);
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .create();
        dialog.show();
    }

    private void refreshBatterySaverOptions() {
        if (mBatterySaverPref != null) {
            mBatterySaverPref.setEnabled(!mBatteryPluggedIn);
            mBatterySaverPref.setChecked(!mBatteryPluggedIn && mPowerManager.isPowerSaveMode());
            mBatterySaverPref.setSummary(mBatteryPluggedIn
                    ? R.string.battery_saver_summary_unavailable
                    : R.string.battery_saver_summary);
        }
    }

    private void addNotAvailableMessage() {
        Preference notAvailable = new Preference(getActivity());
        notAvailable.setTitle(R.string.power_usage_not_available);
        mHistPref.setHideLabels(true);
        mAppListGroup.addPreference(notAvailable);
    }

    private boolean updateBatteryStatus(Intent intent) {
        if (intent != null) {
            String batteryLevel = com.android.settings.Utils.getBatteryPercentage(intent);
            String batteryStatus = com.android.settings.Utils.getBatteryStatus(getResources(),
                    intent);
            if (!batteryLevel.equals(mBatteryLevel) || !batteryStatus.equals(mBatteryStatus)) {
                mBatteryLevel = batteryLevel;
                mBatteryStatus = batteryStatus;
                mBatteryPluggedIn = isBatteryPluggedIn(intent);
                return true;
            }
        }
        return false;
    }

    private boolean isBatteryPluggedIn(Intent intent) {
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
                BatteryManager.BATTERY_STATUS_UNKNOWN);
        return status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL;
    }

    private void updatePerformanceSummary() {
        String value = mPowerManager.getPowerProfile();
        String summary = "";
        int count = mPerfProfileValues.length;
        for (int i = 0; i < count; i++) {
            try {
                if (mPerfProfileValues[i].equals(value)) {
                    summary = mPerfProfileEntries[i];
                }
            } catch (IndexOutOfBoundsException ex) {
                // Ignore
            }
        }
        mPerfProfilePref.setSummary(String.format("%s", summary));
    }

    private void updatePerformanceValue() {
        if (mPerfProfilePref == null) {
            return;
        }
        mPerfProfilePref.setValue(mPowerManager.getPowerProfile());
        updatePerformanceSummary();
    }

    private void refreshStats() {
        final BatteryStats stats = mStatsHelper.getStats();
        final BatteryStats dockStats = mStatsHelper.getDockStats();

        mAppListGroup.removeAll();
        mAppListGroup.setOrderingAsAdded(false);
        mHistPref = new BatteryHistoryPreference(getActivity(), stats, dockStats,
                mStatsHelper.getBatteryBroadcast());
        mHistPref.setOrder(-1);
        mAppListGroup.addPreference(mHistPref);
        boolean addedSome = false;
        final PowerProfile powerProfile = mStatsHelper.getPowerProfile();
        final double averagePower = powerProfile.getAveragePower(PowerProfile.POWER_SCREEN_FULL);
        if (averagePower >= MIN_AVERAGE_POWER_THRESHOLD_MILLI_AMP) {
            final List<UserHandle> profiles = mUm.getUserProfiles();

            mStatsHelper.refreshStats(BatteryStats.STATS_SINCE_CHARGED, profiles);

            final List<BatterySipper> usageList = mStatsHelper.getUsageList();

            final int dischargeAmount = stats != null ? stats.getDischargeAmount(mStatsType) : 0;
            final int numSippers = usageList.size();
            for (int i = 0; i < numSippers; i++) {
                final BatterySipper sipper = usageList.get(i);
                if ((sipper.value * SECONDS_IN_HOUR) < MIN_POWER_THRESHOLD_MILLI_AMP) {
                    continue;
                }
                final double percentOfTotal =
                        ((sipper.value / mStatsHelper.getTotalPower()) * dischargeAmount);
                if (((int) (percentOfTotal + .5)) < 1) {
                    continue;
                }
                if (sipper.drainType == BatterySipper.DrainType.OVERCOUNTED) {
                    // Don't show over-counted unless it is at least 2/3 the size of
                    // the largest real entry, and its percent of total is more significant
                    if (sipper.value < ((mStatsHelper.getMaxRealPower()*2)/3)) {
                        continue;
                    }
                    if (percentOfTotal < 10) {
                        continue;
                    }
                    if ("user".equals(Build.TYPE) || "userdebug".equals(Build.TYPE)) {
                        continue;
                    }
                }
                if (sipper.drainType == BatterySipper.DrainType.UNACCOUNTED) {
                    // Don't show over-counted unless it is at least 1/2 the size of
                    // the largest real entry, and its percent of total is more significant
                    if (sipper.value < (mStatsHelper.getMaxRealPower()/2)) {
                        continue;
                    }
                    if (percentOfTotal < 5) {
                        continue;
                    }
                    if ("user".equals(Build.TYPE) || "userdebug".equals(Build.TYPE)) {
                        continue;
                    }
                }
                final UserHandle userHandle = new UserHandle(UserHandle.getUserId(sipper.getUid()));
                final BatteryEntry entry = new BatteryEntry(getActivity(), mHandler, mUm, sipper);
                final Drawable badgedIcon = mUm.getBadgedIconForUser(entry.getIcon(),
                        userHandle);
                final CharSequence contentDescription = mUm.getBadgedLabelForUser(entry.getLabel(),
                        userHandle);
                final PowerGaugePreference pref = new PowerGaugePreference(getActivity(),
                        badgedIcon, contentDescription, entry);

                final double percentOfMax = (sipper.value * 100) / mStatsHelper.getMaxPower();
                sipper.percent = percentOfTotal;
                pref.setTitle(entry.getLabel());
                pref.setOrder(i + 1);
                pref.setPercent(percentOfMax, percentOfTotal);
                if (sipper.uidObj != null) {
                    pref.setKey(Integer.toString(sipper.uidObj.getUid()));
                }
                addedSome = true;
                mAppListGroup.addPreference(pref);
                if (mAppListGroup.getPreferenceCount() > (MAX_ITEMS_TO_LIST + 1)) {
                    break;
                }
            }
        }
        if (!addedSome) {
            addNotAvailableMessage();
        }

        BatteryEntry.startRequestQueue();
    }

    static final int MSG_REFRESH_STATS = 100;

    Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BatteryEntry.MSG_UPDATE_NAME_ICON:
                    BatteryEntry entry = (BatteryEntry) msg.obj;
                    PowerGaugePreference pgp =
                            (PowerGaugePreference) findPreference(
                                    Integer.toString(entry.sipper.uidObj.getUid()));
                    if (pgp != null) {
                        final int userId = UserHandle.getUserId(entry.sipper.getUid());
                        final UserHandle userHandle = new UserHandle(userId);
                        pgp.setIcon(mUm.getBadgedIconForUser(entry.getIcon(), userHandle));
                        pgp.setTitle(entry.name);
                    }
                    break;
                case BatteryEntry.MSG_REPORT_FULLY_DRAWN:
                    Activity activity = getActivity();
                    if (activity != null) {
                        activity.reportFullyDrawn();
                    }
                    break;
                case MSG_REFRESH_STATS:
                    mStatsHelper.clearStats();
                    refreshStats();
                    if (mBatterySaverPref != null) {
                        refreshBatterySaverOptions();
                    }
            }
            super.handleMessage(msg);
        }
    };
}
