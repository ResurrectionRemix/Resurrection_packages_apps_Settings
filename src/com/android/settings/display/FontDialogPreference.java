/*
 * Copyright (C) 2018 The Dirty Unicorns Project
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
 * preference for managing custom fonts
 */
 package com.android.settings.display;
 import com.android.settingslib.CustomDialogPreference;
 import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.FontInfo;
import android.content.IFontService;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.AttributeSet;
import android.widget.ListView;
 import com.android.settings.R;
 public class FontDialogPreference extends CustomDialogPreference {
    private static final String TAG = "FontDialogPreference";
    private Context mContext;
    private IFontService mFontService;
     public FontDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mFontService = IFontService.Stub.asInterface(
                ServiceManager.getService("dufont"));
    }
     @Override
    protected void onPrepareDialogBuilder(Builder builder,
            DialogInterface.OnClickListener listener) {
        super.onPrepareDialogBuilder(builder, listener);
        FontListAdapter adapter = new FontListAdapter(mContext);
        DialogInterface.OnClickListener l = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                FontInfo info = adapter.getItem(which);
                try {
                    mFontService.applyFont(info);
                } catch (RemoteException e) {
                }
            }
        };
        builder.setIcon(R.drawable.font_dialog_icon);
        builder.setAdapter(adapter, l);
        builder.setCancelable(false);
        builder.setNegativeButton(mContext.getString(com.android.internal.R.string.cancel),
                listener);
    }
     @Override
    protected void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_NEGATIVE) {
            dialog.dismiss();
        }
    }
}
