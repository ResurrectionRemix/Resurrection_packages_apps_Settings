/*
 * Copyright (C) 2015-2016 DirtyUnicorns Project
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
 * Dialog allowing to pick a system action or a intent compatible with
 * ActionHandler framework
 */

package com.android.settings.rr;

import com.android.internal.utils.du.Config.ActionConfig;
import com.android.settings.R;
import com.android.settings.rr.widget.CustomActionListAdapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;

public class ActionPickerDialogActivity extends Activity implements
        ShortcutPickHelper.OnPickListener {

    private static final int DIALOG_ROOT = 1;
    private static final int DIALOG_SYSTEM = 2;
    private ShortcutPickHelper mPicker;
    private CustomActionListAdapter mCustomActionListAdapter;
    private boolean mHasDefault;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPicker = new ShortcutPickHelper(this, this);
        mCustomActionListAdapter = new CustomActionListAdapter(this);
        mHasDefault = getIntent().getBooleanExtra("has_defaults", false);
        String[] excludedActions = getIntent().getStringArrayExtra("excluded_actions");
        if (excludedActions != null) {
            for (int i = 0; i < excludedActions.length; i ++) {
                mCustomActionListAdapter.removeAction(excludedActions[i]);
            }
        }
        createDialog(this, DIALOG_ROOT).show();
    }

    public Dialog createDialog(final Context context, final int id) {

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final Dialog dialog;

        final DialogInterface.OnClickListener l = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                switch (id) {
                    case DIALOG_ROOT:
                        onTargetChange(getResources().getStringArray(
                                mHasDefault ? R.array.action_dialog_values
                                        : R.array.action_dialog_no_default_values)[item]);
                        break;
                    case DIALOG_SYSTEM:
                        sendResultAndFinish(mCustomActionListAdapter.getItem(item).getAction());
                        finish();
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid dialog type "
                                + id + " in ActionPicker dialog.");
                }
            }
        };

        final DialogInterface.OnCancelListener cancel = new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
                onTargetChange(null);
            }
        };

        final DialogInterface.OnClickListener cancelClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                cancel.onCancel(dialog);
            }
        };

        switch (id) {
            case DIALOG_ROOT:
                dialog = builder
                        .setTitle(R.string.choose_action_title)
                        .setItems(
                                getResources().getStringArray(
                                        mHasDefault ? R.array.action_dialog_entries
                                                : R.array.action_dialog_no_default_entries), l)
                        .setOnCancelListener(cancel)
                        .setNegativeButton(getString(android.R.string.cancel), cancelClickListener)
                        .create();
                break;
            case DIALOG_SYSTEM:
                dialog = builder.setTitle(getString(R.string.action_entry_custom_action))
                        .setAdapter(mCustomActionListAdapter, l)
                        .setOnCancelListener(cancel)
                        .setNegativeButton(getString(android.R.string.cancel), cancelClickListener)
                        .create();
                break;
            default:
                throw new IllegalArgumentException("Invalid dialog type "
                        + id + " in ActionPicker dialog.");
        }
        return dialog;
    }

    private void sendResultAndFinish(String result) {
        Intent intent = new Intent("intent_action_action_picker");
        intent.putExtra("result", Activity.RESULT_OK);
        intent.putExtra("action_string", result);
        ActionConfig actionConfig = new ActionConfig(this, result);
        intent.putExtra("action_config", actionConfig);
        sendBroadcastAsUser(intent, UserHandle.CURRENT);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    private void sendCancelResultAndFinish() {
        Intent intent = new Intent("intent_action_action_picker");
        intent.putExtra("result", Activity.RESULT_CANCELED);
        sendBroadcastAsUser(intent, UserHandle.CURRENT);
        setResult(Activity.RESULT_CANCELED);
        finish();
    }

    @Override
    public void shortcutPicked(String uri, String friendlyName, boolean isApplication) {
        if (uri == null) {
            sendCancelResultAndFinish();
        } else {
            sendResultAndFinish(uri);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mPicker.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void onTargetChange(String uri) {
        if (uri == null) {
            sendCancelResultAndFinish();
        } else if (uri.equals(getString(R.string.action_value_default_action))) {
            sendResultAndFinish("default");
        } else if (uri.equals(getString(R.string.action_value_select_app))) {
            mPicker.pickShortcut(null, null, 0);
        } else if (uri.equals(getString(R.string.action_value_custom_action))) {
            createDialog(this, DIALOG_SYSTEM).show();
        }
    }
}
