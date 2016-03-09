/*
 * Copyright (C) 2016 The DirtyUnicorns Project
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

package com.android.settings.rr;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.utils.du.ActionConstants;
import com.android.internal.utils.du.Config;
import com.android.internal.utils.du.Config.ButtonConfig;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class SmartbarSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {
    private ListPreference mSmartBarContext;
    private ListPreference mImeActions;
    private ListPreference mButtonAnim;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.smartbar_settings);

        int contextVal = Settings.Secure.getIntForUser(mContext.getContentResolver(),
                "smartbar_context_menu_mode", 0, UserHandle.USER_CURRENT);
        mSmartBarContext = (ListPreference) findPreference("smartbar_context_menu_position");
        mSmartBarContext.setValue(String.valueOf(contextVal));
        mSmartBarContext.setOnPreferenceChangeListener(this);

        int imeVal = Settings.Secure.getIntForUser(mContext.getContentResolver(),
                "smartbar_ime_hint_mode", 1, UserHandle.USER_CURRENT);
        mImeActions = (ListPreference) findPreference("smartbar_ime_action");
        mImeActions.setValue(String.valueOf(imeVal));
        mImeActions.setOnPreferenceChangeListener(this);

        int buttonAnimVal = Settings.Secure.getIntForUser(mContext.getContentResolver(),
                "smartbar_button_animation_style", 0, UserHandle.USER_CURRENT);
        mButtonAnim = (ListPreference) findPreference("smartbar_button_animation");
        mButtonAnim.setValue(String.valueOf(buttonAnimVal));
        mButtonAnim.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == findPreference("smartbar_editor_mode")) {
            getActivity().sendBroadcastAsUser(new Intent("intent_navbar_edit"), UserHandle.CURRENT);
            return true;
        } else if (preference == findPreference("smartbar_factory_reset")) {
            new AlertDialog.Builder(getActivity())
                    .setMessage(getString(R.string.smartbar_factory_reset_confirm))
                    .setPositiveButton(android.R.string.ok, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ArrayList<ButtonConfig> buttonConfigs = Config.getDefaultConfig(
                                    mContext,
                                    ActionConstants.getDefaults(ActionConstants.SMARTBAR));
                            Config.setConfig(mContext,
                                    ActionConstants.getDefaults(ActionConstants.SMARTBAR),
                                    buttonConfigs);
                            Intent intent = new Intent("intent_navbar_edit");
                            intent.putExtra("extra_navbar_edit_reset_layout", "resetMePlox");
                            getActivity().sendBroadcastAsUser(intent, UserHandle.CURRENT);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).create().show();
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.equals(mSmartBarContext)) {
            int position = Integer.parseInt(((String) newValue).toString());
            Settings.Secure.putInt(getContentResolver(), "smartbar_context_menu_mode",
                    position);
            return true;
        } else if (preference.equals(mButtonAnim)) {
            int val = Integer.parseInt(((String) newValue).toString());
            Settings.Secure.putInt(getContentResolver(), "smartbar_button_animation_style",
                    val);
            return true;
        } else if (preference.equals(mImeActions)) {
            int val = Integer.parseInt(((String) newValue).toString());
            Settings.Secure.putInt(getContentResolver(), "smartbar_ime_hint_mode",
                    val);
            return true;
        }
        return false;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DONT_TRACK_ME_BRO;
    }
}
