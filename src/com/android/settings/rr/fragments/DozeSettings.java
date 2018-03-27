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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

public class DozeSettings extends SettingsPreferenceFragment {
    private static final String TAG = "DozeSettings";

    private static final String SHARED_PREFERENCES_NAME = "doze_settings";
    private static final String KEY_LIGHT_IDLE_FACTOR = "light_idle_factor";
    private static final String KEY_LOCATION_ACCURACY = "location_accuracy";
    private static final String KEY_IDLE_PENDING_FACTOR = "idle_pending_factor";
    private static final String KEY_IDLE_FACTOR = "idle_factor";
    private static final String KEY_SHOW_ADVANCED = "show_advanced";

    final long LIGHT_IDLE_AFTER_INACTIVE_TIMEOUT = 5 * 60 * 1000L;
    final long LIGHT_PRE_IDLE_TIMEOUT = 10 * 60 * 1000L;
    final long LIGHT_IDLE_TIMEOUT = 5 * 60 * 1000L;
    final float LIGHT_IDLE_FACTOR = 2f;
    final long LIGHT_MAX_IDLE_TIMEOUT = 15 * 60 * 1000L;
    final long LIGHT_IDLE_MAINTENANCE_MIN_BUDGET = 1 * 60 * 1000L;
    final long LIGHT_IDLE_MAINTENANCE_MAX_BUDGET = 5 * 60 * 1000L;
    final long MIN_LIGHT_MAINTENANCE_TIME = 5 * 1000L;
    final long MIN_DEEP_MAINTENANCE_TIME = 30 * 1000L;
    final long INACTIVE_TIMEOUT = 30 * 60 * 1000L;
    final long SENSING_TIMEOUT = 4 * 60 * 1000L;
    final long LOCATING_TIMEOUT = 30 * 1000L;
    final float LOCATION_ACCURACY = 20;
    final long MOTION_INACTIVE_TIMEOUT = 10 * 60 * 1000L;
    final long IDLE_AFTER_INACTIVE_TIMEOUT = 30 * 60 * 1000L;
    final long IDLE_PENDING_TIMEOUT = 5 * 60 * 1000L;
    final long MAX_IDLE_PENDING_TIMEOUT = 10 * 60 * 1000L;
    final float IDLE_PENDING_FACTOR = 2;
    final long IDLE_TIMEOUT = 60 * 60 * 1000L;
    final long MAX_IDLE_TIMEOUT = 6 * 60 * 60 * 1000L;
    final float IDLE_FACTOR = 2;
    final long MIN_TIME_TO_ALARM = 60 * 60 * 1000L;
    final long MAX_TEMP_APP_WHITELIST_DURATION = 5 * 60 * 1000L;
    final long MMS_TEMP_APP_WHITELIST_DURATION = 60 * 1000L;
    final long SMS_TEMP_APP_WHITELIST_DURATION = 20 * 1000L;
    final long NOTIFICATION_WHITELIST_DURATION = 30 * 1000L;

    private int millisecondsInOneSecond = 1000;
    private LinearLayout mContainer;
    private List<String> mIdleConfigKeys = new ArrayList<String>();
    private List<String> mIdleConfigDesc = new ArrayList<String>();
    private List<String> mIdleConfigShort = new ArrayList<String>();
    private List<Object> mIdleConfigValues = new ArrayList<Object>();
    private List<String> mIdleConfigKeysBasic = new ArrayList<String>();
    private List<EditText> mIdleConfigEdit = new ArrayList<EditText>();
    private List<View> mIdleConfigViews = new ArrayList<View>();

    private Context mContext;
    private boolean mShowAdvanced;

    private SharedPreferences getPrefs() {
        return mContext.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        setHasOptionsMenu(true);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, root, savedInstanceState);

        mShowAdvanced = getPrefs().getBoolean(KEY_SHOW_ADVANCED, false);
        View view = inflater.inflate(R.layout.doze_settings, root, false);

        Profiles.initProfiles();
        Profiles.loadUserProfiles(getPrefs());

        mIdleConfigKeys.addAll(Arrays.asList(getResources().getStringArray(R.array.idle_config_keys)));
        mIdleConfigDesc.addAll(Arrays.asList(getResources().getStringArray(R.array.idle_config_desc)));
        mIdleConfigShort.addAll(Arrays.asList(getResources().getStringArray(R.array.idle_config_short)));
        mIdleConfigKeysBasic.addAll(Arrays.asList(getResources().getStringArray(R.array.idle_config_keys_basic)));

        mIdleConfigValues.add(LIGHT_IDLE_AFTER_INACTIVE_TIMEOUT);
        mIdleConfigValues.add(LIGHT_PRE_IDLE_TIMEOUT);
        mIdleConfigValues.add(LIGHT_IDLE_TIMEOUT);
        mIdleConfigValues.add(LIGHT_IDLE_FACTOR);
        mIdleConfigValues.add(LIGHT_MAX_IDLE_TIMEOUT);
        mIdleConfigValues.add(LIGHT_IDLE_MAINTENANCE_MIN_BUDGET);
        mIdleConfigValues.add(LIGHT_IDLE_MAINTENANCE_MAX_BUDGET);
        mIdleConfigValues.add(MIN_LIGHT_MAINTENANCE_TIME);
        mIdleConfigValues.add(MIN_DEEP_MAINTENANCE_TIME);
        mIdleConfigValues.add(INACTIVE_TIMEOUT);
        mIdleConfigValues.add(SENSING_TIMEOUT);
        mIdleConfigValues.add(LOCATING_TIMEOUT);
        mIdleConfigValues.add(LOCATION_ACCURACY);
        mIdleConfigValues.add(MOTION_INACTIVE_TIMEOUT);
        mIdleConfigValues.add(IDLE_AFTER_INACTIVE_TIMEOUT);
        mIdleConfigValues.add(IDLE_PENDING_TIMEOUT);
        mIdleConfigValues.add(MAX_IDLE_PENDING_TIMEOUT);
        mIdleConfigValues.add(IDLE_PENDING_FACTOR);
        mIdleConfigValues.add(IDLE_TIMEOUT);
        mIdleConfigValues.add(MAX_IDLE_TIMEOUT);
        mIdleConfigValues.add(IDLE_FACTOR);
        mIdleConfigValues.add(MIN_TIME_TO_ALARM);
        mIdleConfigValues.add(MAX_TEMP_APP_WHITELIST_DURATION);
        mIdleConfigValues.add(MMS_TEMP_APP_WHITELIST_DURATION);
        mIdleConfigValues.add(SMS_TEMP_APP_WHITELIST_DURATION);
        mIdleConfigValues.add(NOTIFICATION_WHITELIST_DURATION);

        mContainer = (LinearLayout) view.findViewById(R.id.idle_config_container);
        createOptionsList(inflater);

        getSettings();
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.doze_settings_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_profile:
                displayProfiles();
                break;
            case R.id.action_save:
                save();
                break;
            case R.id.action_save_as_profile:
                saveAsProfile();
                break;
            case R.id.action_restoredefault:
                restoreDefaults();
                break;
            case R.id.action_filter:
                mShowAdvanced = !mShowAdvanced;
                getPrefs().edit().putBoolean(KEY_SHOW_ADVANCED, mShowAdvanced).commit();
                int i = 0;
                for (View idleConfig : mIdleConfigViews) {
                    final String configKey = mIdleConfigKeys.get(i);
                    if (mShowAdvanced) {
                        idleConfig.setVisibility(View.VISIBLE);
                    } else {
                        idleConfig.setVisibility(!mIdleConfigKeysBasic.contains(configKey) ? View.GONE : View.VISIBLE);
                    }
                    i++;
                }
                break;
            }

        return super.onOptionsItemSelected(item);
    }

    private String getMessage(String desc, String value) {
        return desc +
                "\n\n" + getResources().getString(R.string.string_default) +
                ": " +
                value +
                " " +
                getResources().getString(R.string.string_seconds);
    }


    private void getSettings() {
        String line = Settings.Global.getString(getContentResolver(), Settings.Global.DEVICE_IDLE_CONSTANTS_USER);

        KeyValueListParser parser = new KeyValueListParser(',');
        if ("null".equals(line)) {
            parser.setString(line + "=0");
        } else {
            parser.setString(line);
        }
        int divideBy = getDisplayValueFix();
        for (int i = 0; i < mIdleConfigKeys.size(); i++) {
            EditText editText = mIdleConfigEdit.get(i);
            String key = mIdleConfigKeys.get(i);
            Object defaultValue = mIdleConfigValues.get(i);

            if (!key.equals(KEY_IDLE_FACTOR) && !key.equals(KEY_IDLE_PENDING_FACTOR) && !key.equals(KEY_IDLE_PENDING_FACTOR) && !key.equals(KEY_LOCATION_ACCURACY) && !key.equals(KEY_LIGHT_IDLE_FACTOR)) {
                Long value = parser.getLong(key, (Long) defaultValue);
                value = value / divideBy;
                editText.setText(String.valueOf(value));
            } else {
                Float value = parser.getFloat(key, (Float) defaultValue);
                editText.setText(String.valueOf(value));
            }
        }
    }


    private int getDisplayValueFix() {
        return millisecondsInOneSecond;
    }

    private void save() {
        int multiplyBy = getDisplayValueFix();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mIdleConfigKeys.size(); i++) {
            EditText editText = mIdleConfigEdit.get(i);
            String key = mIdleConfigKeys.get(i);

            if (!key.equals(KEY_IDLE_FACTOR) && !key.equals(KEY_IDLE_PENDING_FACTOR) && !key.equals(KEY_IDLE_PENDING_FACTOR) && !key.equals(KEY_LOCATION_ACCURACY) && !key.equals(KEY_LIGHT_IDLE_FACTOR)) {
                Long value = Long.valueOf(editText.getText().toString()) * multiplyBy;
                sb.append(key + "=" + value + ",");
            } else {
                Float value = Float.valueOf(editText.getText().toString());
                sb.append(key + "=" + value + ",");
            }
        }
        if (sb.length() != 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        Settings.Global.putString(getContentResolver(), Settings.Global.DEVICE_IDLE_CONSTANTS_USER, sb.toString());
        showApplyToast();
    }

    private void restoreDefaults() {
        Settings.Global.putString(getContentResolver(), Settings.Global.DEVICE_IDLE_CONSTANTS_USER, null);
        getSettings();
        showApplyToast();
    }

    private void applyProfile(String settings) {
        Settings.Global.putString(getContentResolver(), Settings.Global.DEVICE_IDLE_CONSTANTS_USER, settings);
        getSettings();
        showApplyToast();
    }

    private void displayProfiles() {
        final ArrayList<Profiles.Profile> combinedProfileList = new ArrayList<>();
        combinedProfileList.addAll(Profiles.getSystemProfileList());
        combinedProfileList.addAll(Profiles.getUserProfileList());
        Collections.sort(combinedProfileList);
        int profileCount = 0;
        String[] names = new String[combinedProfileList.size()];
        for (Profiles.Profile profile : combinedProfileList) {
            if (Profiles.getSystemProfileList().contains(profile)) {
                names[profileCount] = profile.getName() + " *";
            } else {
                names[profileCount] = profile.getName();
            }
            profileCount++;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(getResources().getString(R.string.action_profile));
        builder.setItems(names, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                applyProfile(combinedProfileList.get(item).getSettings());
            }
        });
        final AlertDialog alert = builder.create();
        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                ListView lv = alert.getListView();
                lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                        Profiles.Profile p = combinedProfileList.get(position);
                        if (Profiles.getUserProfileList().contains(p)) {
                            AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
                            dialog.setTitle(getResources().getString(R.string.delete_profile));
                            dialog.setMessage(p.getName());
                            dialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Profiles.removeProfile(p);
                                    combinedProfileList.remove(p);
                                    Profiles.saveUserProfiles(getPrefs());
                                    alert.hide();
                                }
                            });
                            dialog.setNegativeButton(android.R.string.cancel, null);
                            dialog.create().show();
                        }
                        return true;
                    }
                });
            }
        });
        alert.show();
    }

    private void saveAsProfile() {
        AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
        final EditText edittext = new EditText(mContext);
        alert.setTitle(getResources().getString(R.string.profile_name));

        alert.setView(edittext);

        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String name = edittext.getText().toString();
                int multiplyBy = getDisplayValueFix();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < mIdleConfigKeys.size(); i++) {
                    EditText editText = mIdleConfigEdit.get(i);
                    String key = mIdleConfigKeys.get(i);

                    if (!key.equals(KEY_IDLE_FACTOR) && !key.equals(KEY_IDLE_PENDING_FACTOR) && !key.equals(KEY_IDLE_PENDING_FACTOR) && !key.equals(KEY_LOCATION_ACCURACY) && !key.equals(KEY_LIGHT_IDLE_FACTOR)) {
                        Long value = Long.valueOf(editText.getText().toString()) * multiplyBy;
                        sb.append(key + "=" + value + ",");
                    } else {
                        Float value = Float.valueOf(editText.getText().toString());
                        sb.append(key + "=" + value + ",");
                    }
                }
                if (sb.length() != 0) {
                    sb.deleteCharAt(sb.length() - 1);
                }
                Profiles.Profile profile = new Profiles.Profile(name, sb.toString());
                Profiles.addProfile(profile);
                Profiles.saveUserProfiles(getPrefs());
            }
        });

        alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // what ever you want to do with No option.
            }
        });

        alert.show();
    }

    private void createOptionsList(LayoutInflater inflater) {
        mContainer.removeAllViews();
        for (int i = 0; i < mIdleConfigKeys.size(); i++) {
            final String configKey = mIdleConfigKeys.get(i);
            final int idx = i;
            View idleConfig = inflater.inflate(R.layout.doze_item, null, false);
            TextView idleConfigText = (TextView) idleConfig.findViewById(R.id.idle_config_text);
            idleConfigText.setText(mIdleConfigShort.get(i));
            EditText idleConfigValue = (EditText) idleConfig.findViewById(R.id.idle_config_edit);
            idleConfigValue.setSaveEnabled(false);
            mIdleConfigEdit.add(idleConfigValue);
            ImageView idleConfigImage = (ImageView) idleConfig.findViewById(R.id.idle_config_image);
            idleConfig.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String key = mIdleConfigKeys.get(idx);
                    int divideBy = getDisplayValueFix();
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setTitle(key);
                    Object value = mIdleConfigValues.get(idx);
                    if (!key.equals(KEY_IDLE_FACTOR) && !key.equals(KEY_IDLE_PENDING_FACTOR) && !key.equals(KEY_IDLE_PENDING_FACTOR) && !key.equals(KEY_LOCATION_ACCURACY) && !key.equals(KEY_LIGHT_IDLE_FACTOR)) {
                        if (value instanceof Long) {
                            value = ((Long) value) / divideBy;
                        }
                        builder.setMessage(getMessage(mIdleConfigDesc.get(idx), String.valueOf(value)));
                    } else {
                        builder.setMessage(mIdleConfigDesc.get(idx) +
                                "\n\n" +
                                getResources().getString(R.string.string_default) +
                                ": " +
                                String.valueOf(value));
                    }
                    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            });
            mContainer.addView(idleConfig);
            mIdleConfigViews.add(idleConfig);
            if (mShowAdvanced) {
                idleConfig.setVisibility(View.VISIBLE);
            } else {
                idleConfig.setVisibility(!mIdleConfigKeysBasic.contains(configKey) ? View.GONE : View.VISIBLE);
            }
        }
    }

    private void showApplyToast() {
        Toast.makeText(mContext, getResources().getString(R.string.doze_settings_applied), Toast.LENGTH_SHORT).show();
    }
}

