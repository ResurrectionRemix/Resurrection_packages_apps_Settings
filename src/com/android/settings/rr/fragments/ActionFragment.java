/*
 * Copyright (C) 2015 TeamEos project
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
 *
 * Handle assignable action dialogs and instances of the ActionPreference
 * class that holds target widget state
 */

package com.android.settings.rr.fragments;

import java.util.ArrayList;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.utils.du.ActionConstants.Defaults;
import com.android.internal.utils.du.ActionHandler;
import com.android.internal.utils.du.Config;
import com.android.internal.utils.du.Config.ActionConfig;
import com.android.internal.utils.du.Config.ButtonConfig;

import com.android.settings.rr.ShortcutPickHelper;
import com.android.settings.rr.Preferences.ActionPreference;
import com.android.settings.rr.CustomActionListAdapter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class ActionFragment extends SettingsPreferenceFragment implements
        ShortcutPickHelper.OnPickListener {

    private static final int DIALOG_CATEGORY = 69;
    private static final int DIALOG_CUSTOM_ACTIONS = 70;
    private static final String KEY_FOCUSED_PREFERENCE = "key_focused_preference";

    private ShortcutPickHelper mPicker;
    protected ArrayList<ActionPreference> mPrefHolder;
    private String mHolderTag;
    private Defaults mDefaults;
    private ArrayList<ButtonConfig> mButtons;
    private ArrayList<ButtonConfig> mDefaultButtons;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (icicle != null) {
            String holderTag = icicle.getString(KEY_FOCUSED_PREFERENCE);
            if (holderTag != null) {
                mHolderTag = holderTag;
            }
        }
        mPicker = new ShortcutPickHelper(getActivity(), this);
        mPrefHolder = new ArrayList<ActionPreference>();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mPicker.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void shortcutPicked(String uri, String friendlyName, boolean isApplication) {
        // activity dialogs pass null here if they are dismissed
        // if null, do nothing, no harm
        if (uri == null) {
            return;
        }
        findAndUpdatePreference(new ActionConfig(getActivity(), uri), mHolderTag);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference instanceof ActionPreference) {
            mHolderTag = ((ActionPreference)preference).getTag();
            showDialog(DIALOG_CATEGORY);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mDefaults != null) {
            loadAndSetConfigs();
            onActionPolicyEnforced(mPrefHolder);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mHolderTag != null) {
            outState.putString(KEY_FOCUSED_PREFERENCE, mHolderTag);
        }
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case DIALOG_CATEGORY: {
                Dialog dialog;
                final DialogInterface.OnClickListener categoryClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        onTargetChange(getResources().getStringArray(R.array.action_dialog_values)[item]);
                        dialog.dismiss();
                    }
                };
                dialog = new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.choose_action_title)
                        .setItems(getResources().getStringArray(R.array.action_dialog_entries),
                                categoryClickListener)
                        .setNegativeButton(getString(android.R.string.cancel), null)
                        .create();
                return dialog;
            }
            case DIALOG_CUSTOM_ACTIONS: {
                Dialog dialog;
                final CustomActionListAdapter adapter = new CustomActionListAdapter(getActivity());
                if (!usesExtendedActionsList()) {
                    adapter.removeAction(ActionHandler.SYSTEMUI_TASK_HOME);
                    adapter.removeAction(ActionHandler.SYSTEMUI_TASK_BACK);
                }
                final DialogInterface.OnClickListener customActionClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        findAndUpdatePreference(adapter.getItem(item), mHolderTag);
                        dialog.dismiss();
                    }
                };
                dialog = new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.action_entry_custom_action))
                        .setAdapter(adapter, customActionClickListener)
                        .setNegativeButton(getString(android.R.string.cancel), null)
                        .create();
                return dialog;
            }
        }
        return super.onCreateDialog(dialogId);
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        switch (dialogId) {
            case DIALOG_CATEGORY:
            case DIALOG_CUSTOM_ACTIONS:
                return MetricsEvent.RESURRECTED;
            default:
                return 0;
        }
    }

    // subclass overrides to include back and home actions
    protected boolean usesExtendedActionsList() {
        return false;
    }

    protected void onActionPolicyEnforced(ArrayList<ActionPreference> prefs) {
    }

    protected void setActionPreferencesEnabled(boolean enabled) {
        for (ActionPreference pref : mPrefHolder) {
            pref.setEnabled(enabled);
        }
    }

    /**
     * load our button lists and ActionPreferences map button action targets from preference keys
     * and defaults config maps subclass is required to set desired Defaults interface int
     * ActionContants
     */
    protected void onPreferenceScreenLoaded(Defaults defaults) {
        mDefaults = defaults;
        final PreferenceScreen prefScreen = getPreferenceScreen();
        for (int i = 0; i < prefScreen.getPreferenceCount(); i++) {
            Preference pref = prefScreen.getPreference(i);
            if (pref instanceof PreferenceCategory) {
                PreferenceCategory cat = (PreferenceCategory) pref;
                for (int j = 0; j < cat.getPreferenceCount(); j++) {
                    Preference child = cat.getPreference(j);
                    if (child instanceof ActionPreference) {
                        mPrefHolder.add((ActionPreference) child);
                    }
                }
            } else if (pref instanceof ActionPreference) {
                mPrefHolder.add((ActionPreference) pref);
            }
        }
        loadAndSetConfigs();
    }

    protected void loadAndSetConfigs() {
        mButtons = Config.getConfig(getActivity(), mDefaults);
        mDefaultButtons = Config.getDefaultConfig(getActivity(), mDefaults);
        for (ActionPreference pref : mPrefHolder) {
            pref.setDefaults(mDefaults);
            ButtonConfig button = mButtons.get(pref.getConfigMap().button);
            ActionConfig action = button.getActionConfig(pref.getConfigMap().action);
            pref.setActionConfig(action);
            ButtonConfig defButton = mDefaultButtons.get(pref.getConfigMap().button);
            ActionConfig defAction = defButton.getActionConfig(pref.getConfigMap().action);
            pref.setDefaultActionConfig(defAction);
        }
    }

    private void onTargetChange(String uri) {
        if (uri == null) {
            return;
        } else if (uri.equals(getString(R.string.action_value_default_action))) {
            findAndUpdatePreference(null, mHolderTag);
        } else if (uri.equals(getString(R.string.action_value_select_app))) {
            mPicker.pickShortcut(null, null, getId());
        } else if (uri.equals(getString(R.string.action_value_custom_action))) {
            showDialog(DIALOG_CUSTOM_ACTIONS);
        }
    }

    protected void findAndUpdatePreference(ActionConfig action, String tag) {
        for (ActionPreference pref : mPrefHolder) {
            if (pref.getTag().equals(mHolderTag)) {
                if (action == null) {
                    action = pref.getDefaultActionConfig();
                }
                pref.setActionConfig(action);
                ButtonConfig button = mButtons.get(pref.getConfigMap().button);
                ActionConfig newAction = pref.getActionConfig();
                button.setActionConfig(newAction, pref.getConfigMap().action);
                mButtons = Config.replaceButtonAtPosition(mButtons, button, pref.getConfigMap());
                Config.setConfig(getActivity(), mDefaults, mButtons);
                onActionPolicyEnforced(mPrefHolder);
                break;
            }
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.RESURRECTED;
    }
}
