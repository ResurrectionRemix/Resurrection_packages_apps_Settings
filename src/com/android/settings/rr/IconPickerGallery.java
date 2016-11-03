/**
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
 *
 */

package com.android.settings.rr;

import java.io.File;

import com.android.internal.utils.du.DUActionUtils;
import com.android.internal.utils.du.ImageHelper;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.UserHandle;
import android.widget.Toast;

import com.android.settings.R;

/**
 * So we can capture image selection in DUSystemReceiver
 */
public class IconPickerGallery extends Activity {
    public static String TAG = IconPickerGallery.class.getSimpleName();
    public static final String INTENT_GALLERY_PICKER = "intent_gallery_picker";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        startActivityForResult(intent, 69);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == 69) {
            Bitmap b = null;
            try {
                if (DUActionUtils.isBitmapAllowedSize(this, data.getData(),
                        DUActionUtils.DUI_ICON_MAX_WIDTH, DUActionUtils.DUI_ICON_MAX_HEIGHT)) {
                    b = ImageHelper.getBitmapFromUri(this, data.getData());
                    if (b != null) {
                        File dir = new File(Environment.getExternalStorageDirectory() + File.separator
                                + "dui_icons");
                        dir.mkdirs();
                        String fileName = "dui_icons_"
                                + String.valueOf(System.currentTimeMillis());
                        ;
                        Uri newUri = ImageHelper.addBitmapToStorage(dir, fileName, b);
                        if (newUri == null) {
                            Toast.makeText(this, getString(R.string.invalid_icon_from_uri),
                                    Toast.LENGTH_SHORT)
                                    .show();
                            sendCancelResultAndFinish();
                        } else {
                            Intent resultIntent = new Intent();
                            resultIntent.setAction(INTENT_GALLERY_PICKER);
                            resultIntent.putExtra("result", Activity.RESULT_OK);
                            resultIntent.putExtra("uri", newUri.toString());
                            sendBroadcastAsUser(resultIntent, UserHandle.CURRENT);
                            setResult(RESULT_OK, resultIntent);
                            finish();
                        }
                    } else {
                        Toast.makeText(this, getString(R.string.invalid_icon_from_uri), Toast.LENGTH_SHORT)
                                .show();
                        sendCancelResultAndFinish();
                    }
                } else {
                    Toast.makeText(this, getString(R.string.image_exceeds_max_allowed_size),
                            Toast.LENGTH_SHORT)
                            .show();
                    sendCancelResultAndFinish();
                }
            } catch (Exception e) {
                Toast.makeText(this, getString(R.string.invalid_icon_from_uri), Toast.LENGTH_SHORT)
                        .show();
                sendCancelResultAndFinish();
            }
        } else {
            sendCancelResultAndFinish();
        }
    }

    private void sendCancelResultAndFinish() {
        Intent intent = new Intent(INTENT_GALLERY_PICKER);
        intent.putExtra("result", Activity.RESULT_CANCELED);
        sendBroadcastAsUser(intent, UserHandle.CURRENT);
        setResult(Activity.RESULT_CANCELED);
        finish();
    }

}
