/*
 * Copyright (C) 2015 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.cyanogenmod;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.settings.R;

public class NavRing extends Fragment implements View.OnClickListener {
    private LinearLayout mRestore, mSave, mEdit;
    private final static Intent TRIGGER_INTENT =
            new Intent("android.intent.action.NAVBAR_RING_EDIT");

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.nav_bar, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        TextView message = (TextView) view.findViewById(R.id.message);
        message.setText(R.string.navigation_ring_message);
        mEdit = (LinearLayout) view.findViewById(R.id.navbar_edit);
        mEdit.setOnClickListener(this);
        mSave = (LinearLayout) view.findViewById(R.id.navbar_save);
        mSave.setOnClickListener(this);
        mRestore = (LinearLayout) view.findViewById(R.id.navbar_restore);
        mRestore.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == mEdit) {
            getActivity().sendBroadcast(TRIGGER_INTENT);
        } else if (v == mRestore) {
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.profile_reset_title)
                    .setIcon(R.drawable.ic_navbar_restore)
                    .setMessage(R.string.navigation_bar_reset_message)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            for (int i = 0; i < 3; i++) {
                                Settings.Secure.putString(getActivity().getContentResolver(),
                                        Settings.Secure.NAVIGATION_RING_TARGETS[i], null);
                            }
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }
    }
}
