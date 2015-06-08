/*
 * Copyright (C) 2014 SlimRoms
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import com.android.internal.util.slim.ActionHelper;
import com.android.internal.util.slim.DeviceUtils;
import com.android.internal.util.slim.DeviceUtils.FilteredDeviceFeaturesArray;
import com.android.settings.slim.SlimActionShortcut;

import com.android.settings.R;

public class CreateSlimShortcut extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        FilteredDeviceFeaturesArray
                finalActionDialogArray = new FilteredDeviceFeaturesArray();
        finalActionDialogArray =
            DeviceUtils.filterUnsupportedDeviceFeatures(this,
            getResources().getStringArray(getResources().getIdentifier(
            "shortcut_action_launcher_values", "array", "com.android.settings")),
            getResources().getStringArray(getResources().getIdentifier(
            "shortcut_action_launcher_entries", "array", "com.android.settings")));
        final String[] dialogValues = finalActionDialogArray.values;
        final String[] dialogEntries = finalActionDialogArray.entries;
        AlertDialog.Builder action = new AlertDialog.Builder(this);
        action.setTitle(R.string.shortcut_action_select_action)
        .setNegativeButton(R.string.cancel,
            new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                CreateSlimShortcut.this.finish();
            }
        })
        .setItems(dialogEntries,
            new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                Intent shortcutIntent = new Intent(
                        CreateSlimShortcut.this, SlimActionShortcut.class);
                shortcutIntent.putExtra("value", dialogValues[item]);

                Drawable icon = ActionHelper.getActionIconImage(
                        CreateSlimShortcut.this, dialogValues[item], null);
                BitmapDrawable bitmap = (BitmapDrawable) icon;
                Intent intent = new Intent();
                intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap.getBitmap());
                intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, dialogEntries[item]);
                intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
                setResult(RESULT_OK, intent);
                CreateSlimShortcut.this.finish();
            }
        });
        action.show();
    }
}
