/*
 * Copyright (C) 2019 ExtendedUI
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

package com.android.settings.rr.preview;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.FrameLayout;

import androidx.core.content.res.TypedArrayUtils;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import com.android.settings.R;
import com.android.settingslib.widget.LayoutPreference;
import android.graphics.Color;


public class AmbientLightSettingsPreview extends LayoutPreference {

    private boolean mAllowDividerAbove;
    private boolean mAllowDividerBelow;
    private View mRootView;
    static ImageView LeftAmbientLightImageView;
    static ImageView RightAmbientLightImageView;

    private static final String TAG = "AmbientLightSettingsPreview";

    public AmbientLightSettingsPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0 /* defStyleAttr */);
    }

    public AmbientLightSettingsPreview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Preference);
        mAllowDividerAbove = TypedArrayUtils.getBoolean(a, R.styleable.Preference_allowDividerAbove,
                R.styleable.Preference_allowDividerAbove, false);
        mAllowDividerBelow = TypedArrayUtils.getBoolean(a, R.styleable.Preference_allowDividerBelow,
                R.styleable.Preference_allowDividerBelow, false);
        a.recycle();

        a = context.obtainStyledAttributes(
                attrs, R.styleable.Preference, defStyleAttr, 0);
        int layoutResource = a.getResourceId(R.styleable.Preference_android_layout, 0);
        if (layoutResource == 0) {
            throw new IllegalArgumentException("LayoutPreference requires a layout to be defined");
        }
        a.recycle();

        // Need to create view now so that findViewById can be called immediately.
        final View view = LayoutInflater.from(getContext())
                .inflate(layoutResource, null, false);
        setView(view);
    }

    private void setView(View view) {
        setLayoutResource(R.layout.layout_preference_frame);
        mRootView = view;
        setShouldDisableView(false);
        try {
            LeftAmbientLightImageView = findViewById(R.id.ambient_light_preview_imageview_left);
            RightAmbientLightImageView = findViewById(R.id.ambient_light_preview_imageview_right);
        } catch (NullPointerException e) {
            Log.e(TAG, "NullPointerException");
        }
    }

    public static void setAmbientLightPreviewColor(int color) {
        try {
            LeftAmbientLightImageView.setColorFilter(color);
            RightAmbientLightImageView.setColorFilter(color);
        } catch (NullPointerException e) {
            Log.e(TAG, "NullPointerException");
        }
    }
}
