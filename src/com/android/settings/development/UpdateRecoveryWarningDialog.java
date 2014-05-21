/*
 * Copyright (C) 2020 The LineageOS Project
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

package com.android.settings.development;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.SystemProperties;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

public class UpdateRecoveryWarningDialog extends InstrumentedDialogFragment implements
        DialogInterface.OnClickListener, DialogInterface.OnDismissListener {

    public static final String TAG = "UpdateRecoveryWarningDialog";

    private static final String UPDATE_RECOVERY_PROPERTY = "persist.sys.recovery_update";

    public static void show(Fragment host) {
        final FragmentManager manager = host.getActivity().getSupportFragmentManager();
        if (manager.findFragmentByTag(TAG) == null) {
            final UpdateRecoveryWarningDialog dialog =
                    new UpdateRecoveryWarningDialog();
            dialog.setTargetFragment(host, 0 /* requestCode */);
            dialog.show(manager, TAG);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.TYPE_UNKNOWN;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        boolean enabled = SystemProperties.getBoolean(UPDATE_RECOVERY_PROPERTY, false);
        return new AlertDialog.Builder(getActivity())
                .setMessage(enabled ?
                        R.string.update_recovery_off_warning : R.string.update_recovery_on_warning)
                .setPositiveButton(android.R.string.ok, this /* onClickListener */)
                .setNegativeButton(android.R.string.cancel, this /* onClickListener */)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        final UpdateRecoveryDialogHost host = (UpdateRecoveryDialogHost) getTargetFragment();
        if (host == null) {
            return;
        }
        if (which == DialogInterface.BUTTON_POSITIVE) {
            host.onUpdateRecoveryDialogConfirmed();
        } else {
            host.onUpdateRecoveryDialogDismissed();
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        final UpdateRecoveryDialogHost host = (UpdateRecoveryDialogHost) getTargetFragment();
        if (host == null) {
            return;
        }
        host.onUpdateRecoveryDialogDismissed();
    }
}
