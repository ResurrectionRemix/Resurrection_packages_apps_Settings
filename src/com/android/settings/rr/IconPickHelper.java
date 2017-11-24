/**
 * Copyright (C) 2016 The DirtyUnicorns Project
 * Author: Randall Rushing <randall.rushing@gmail.com>
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
 * Wrapper class for delivering icon picker results to activities or fragments
 */

package com.android.settings.rr;

import java.io.File;

import com.android.internal.utils.du.ImageHelper;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.utils.du.DUActionUtils;
import com.android.settings.R;

public class IconPickHelper {
    private static final String TAG = "IconPickHelper";
    public static final int REQUEST_PICK_ICON_PACK = 100;
    public static final int REQUEST_PICK_ICON_GALLERY = 101;
    public static final String INTENT_ACTION_EDIT_CLASS = "com.android.settings";
    public static final String INTENT_ACTION_ICON_PICKER_COMPONENT = "com.android.settings.rr.IconPickerActivity";
    private static final int REQUEST_CODE_ICON_PACK = 420;
    private static final int REQUEST_CODE_GALLERY = 1337;
    private Activity mParent;
    private OnPickListener mListener;
    private int lastFragmentId;
    private int mIconPickType;

    public interface OnPickListener {
        void iconPicked(String iconType, String iconPackage, String iconName);
        void imagePicked(Uri uri);
    }

    public IconPickHelper(Activity parent, OnPickListener listener) {
        mParent = parent;
        mListener = listener;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_ICON_PACK:
                    String iconType = data.getStringExtra("icon_data_type");
                    String iconPackage = data.getStringExtra("icon_data_package");
                    String iconName = data.getStringExtra("icon_data_name");
                    mListener.iconPicked(iconType, iconPackage, iconName);
                    break;
                case REQUEST_CODE_GALLERY:
                    Bitmap b = null;
                    try {
                        if (DUActionUtils.isBitmapAllowedSize(mParent, data.getData(),
                                DUActionUtils.DUI_ICON_MAX_WIDTH, DUActionUtils.DUI_ICON_MAX_HEIGHT)) {
                            b = ImageHelper.getBitmapFromUri(mParent, data.getData());
                            if (b != null) {
                                File dir = new File(Environment.getExternalStorageDirectory()
                                        + File.separator
                                        + "dui_icons");
                                dir.mkdirs();
                                String fileName = "dui_icons_"
                                        + String.valueOf(System.currentTimeMillis());
                                Uri newUri = ImageHelper.addBitmapToStorage(dir, fileName, b);
                                if (newUri == null) {
                                    Toast.makeText(mParent,
                                            mParent.getString(R.string.invalid_icon_from_uri),
                                            Toast.LENGTH_SHORT)
                                            .show();
                                    mListener.imagePicked(null);
                                } else {
                                    mListener.imagePicked(newUri);
                                }
                            } else {
                                Toast.makeText(mParent, mParent.getString(R.string.invalid_icon_from_uri),
                                        Toast.LENGTH_SHORT)
                                        .show();
                                mListener.imagePicked(null);
                            }
                        } else {
                            Toast.makeText(mParent, mParent.getString(R.string.image_exceeds_max_allowed_size),
                                    Toast.LENGTH_SHORT)
                                    .show();
                            mListener.imagePicked(null);
                        }
                    } catch (Exception e) {
                        Toast.makeText(mParent, mParent.getString(R.string.invalid_icon_from_uri),
                                Toast.LENGTH_SHORT)
                                .show();
                        mListener.imagePicked(null);
                    }
                break;
            }
        }
    }

    public void pickIcon(int fragmentId, int requestType) {
        lastFragmentId = fragmentId;
        mIconPickType = requestType;
        if (requestType == REQUEST_PICK_ICON_PACK) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_MAIN);
            intent.setClassName(INTENT_ACTION_EDIT_CLASS, INTENT_ACTION_ICON_PICKER_COMPONENT);
            startFragmentOrActivity(intent, REQUEST_CODE_ICON_PACK);
        } else if (requestType == REQUEST_PICK_ICON_GALLERY) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            startFragmentOrActivity(intent, REQUEST_CODE_GALLERY);
        }
    }

    private void startFragmentOrActivity(Intent pickIntent, int requestCode) {
        if (lastFragmentId == 0) {
            mParent.startActivityForResult(pickIntent, requestCode);
        } else {
            Fragment cFrag = mParent.getFragmentManager().findFragmentById(lastFragmentId);
            if (cFrag != null) {
                mParent.startActivityFromFragment(cFrag, pickIntent, requestCode);
            }
        }
    }
}
