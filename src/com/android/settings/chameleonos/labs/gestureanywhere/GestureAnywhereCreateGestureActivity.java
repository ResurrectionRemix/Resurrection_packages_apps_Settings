/*
 * Copyright (C) 2013 The ChameleonOS Open Source Project
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

package com.android.settings.chameleonos.labs.gestureanywhere;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.MotionEvent;
import android.gesture.GestureOverlayView;
import android.gesture.Gesture;
import android.gesture.GestureLibrary;
import android.widget.TextView;
import com.android.settings.R;

public class GestureAnywhereCreateGestureActivity extends Activity {
    private static final float LENGTH_THRESHOLD = 60.0f;

    private Gesture mGesture;
    private View mDoneButton;
    private String mUri;
    private String mName;
    private boolean mIsExistingGesture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.ga_create_gesture);

        mDoneButton = findViewById(R.id.done);

        final Intent intent = getIntent();
        mUri = intent.getStringExtra("uri");
        mName = intent.getStringExtra("name");

        ((TextView) findViewById(R.id.gesture_name)).setText(mName);

        GestureOverlayView overlay = (GestureOverlayView) findViewById(R.id.gestures_overlay);
        overlay.addOnGestureListener(new GesturesProcessor());

        final GestureLibrary store = GestureAnywhereBuilderActivity.getStore();
        final String gestureName = mName + '|' + mUri;
        for (String entry : store.getGestureEntries()) {
            if (gestureName.equals(entry)) {
                mGesture = store.getGestures(entry).get(0);
                mIsExistingGesture = true;
                break;
            }
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mGesture != null) {
            final GestureOverlayView overlay =
                    (GestureOverlayView) findViewById(R.id.gestures_overlay);
            overlay.post(new Runnable() {
                @Override
                public void run() {
                    overlay.setGesture(mGesture);
                    mDoneButton.setEnabled(true);
                }
            });
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        
        if (mGesture != null) {
            outState.putParcelable("gesture", mGesture);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        
        mGesture = savedInstanceState.getParcelable("gesture");
        if (mGesture != null) {
            final GestureOverlayView overlay =
                    (GestureOverlayView) findViewById(R.id.gestures_overlay);
            overlay.post(new Runnable() {
                public void run() {
                    overlay.setGesture(mGesture);
                }
            });

            mDoneButton.setEnabled(true);
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void addGesture(View v) {
        if (mGesture != null) {
            if (TextUtils.isEmpty(mName) || TextUtils.isEmpty(mUri)) {
                return;
            }

            final GestureLibrary store = GestureAnywhereBuilderActivity.getStore();
            final String gestureName = mName + "|" +mUri;
            if (mIsExistingGesture) {
                store.removeEntry(gestureName);
            }
            store.addGesture(gestureName, mGesture);
            store.save();

            setResult(RESULT_OK);
        } else {
            setResult(RESULT_CANCELED);
        }

        finish();
        
    }
    
    @SuppressWarnings({"UnusedDeclaration"})
    public void cancelGesture(View v) {
        setResult(RESULT_CANCELED);
        finish();
    }
    
    private class GesturesProcessor implements GestureOverlayView.OnGestureListener {
        public void onGestureStarted(GestureOverlayView overlay, MotionEvent event) {
            mDoneButton.setEnabled(false);
            mGesture = null;
        }

        public void onGesture(GestureOverlayView overlay, MotionEvent event) {
        }

        public void onGestureEnded(GestureOverlayView overlay, MotionEvent event) {
            mGesture = overlay.getGesture();
            if (mGesture.getLength() < LENGTH_THRESHOLD) {
                overlay.clear(false);
            }
            mDoneButton.setEnabled(true);
        }

        public void onGestureCancelled(GestureOverlayView overlay, MotionEvent event) {
        }
    }
}
