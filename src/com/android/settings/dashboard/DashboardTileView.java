/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.dashboard;

import android.app.Activity;
import android.content.Context;
import android.provider.Settings;
import android.os.UserHandle;
import android.graphics.PorterDuff.Mode;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.android.settings.ProfileSelectDialog;
import com.android.settings.R;
import com.android.settings.Utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import com.android.internal.util.rr.FontHelper;

public class DashboardTileView extends FrameLayout implements View.OnClickListener {

    private static final int DEFAULT_COL_SPAN = 1;

    private ImageView mImageView;
    private TextView mTitleTextView;
    private TextView mStatusTextView;
    private View mDivider;
    private Switch mSwitch;
    private GenericSwitchToggle mSwitchToggle;
    
    private boolean mCustomColors;
    private int mTextcolor;
    private int mIconColor;
    private int mDashTextSize = 14;

    private int mColSpan = DEFAULT_COL_SPAN;

    private DashboardTile mTile;

    public DashboardTileView(Context context) {
        this(context, null);
    }

    public DashboardTileView(Context context, AttributeSet attrs) {
        super(context, attrs);

        final View view = LayoutInflater.from(context).inflate(R.layout.dashboard_tile, this);
        mCustomColors = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.DASHBOARD_CUSTOM_COLORS, 0) == 1;

        mImageView = (ImageView) view.findViewById(R.id.icon);
        mTitleTextView = (TextView) view.findViewById(R.id.title);
 	if (Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.DASHBOARD_TILEVIEW_DOUBLE_LINES, 0) == 1) {
        mTitleTextView.setSingleLine(false);
        } else {
        mTitleTextView.setSingleLine(true);
        }

        mStatusTextView = (TextView) view.findViewById(R.id.status);
        mDivider = view.findViewById(R.id.tile_divider);
	 if (Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.DASHBOARD_TILEVIEW_DIVIDERS, 0) == 1) {
         mDivider.setVisibility(View.GONE);
         } else {
         mDivider.setVisibility(View.VISIBLE);
         }
        mSwitch = (Switch) view.findViewById(R.id.dashboard_switch);

        setOnClickListener(this);
        setBackgroundResource(R.drawable.dashboard_tile_background);
        updatedashboard(context,view);
        setFocusable(true);
        setcolors(view);
    }

    public TextView getTitleTextView() {
         return mTitleTextView;
    }

    public TextView getStatusTextView() { 
        return mStatusTextView;
    }

    public ImageView getImageView() {
        return mImageView;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mSwitchToggle != null) {
            mSwitchToggle.resume(getContext());
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mSwitchToggle != null) {
            mSwitchToggle.pause();
        }
    }

    public void setTile(DashboardTile tile) {
        mTile = tile;

        if (mTile.switchControl != null) {
            try {
                Class<?> clazz = getClass().getClassLoader().loadClass(mTile.switchControl);
                Constructor<?> constructor = clazz.getConstructor(Context.class, Switch.class);
                GenericSwitchToggle sw = (GenericSwitchToggle) constructor.newInstance(
                        getContext(), mSwitch);
                mSwitchToggle = sw;
                mSwitchToggle.resume(getContext());
            } catch (ClassNotFoundException
                    | NoSuchMethodException
                    | InvocationTargetException
                    | InstantiationException
                    | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public void setDividerVisibility(boolean visible) {
        mDivider.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    void setColumnSpan(int span) {
        mColSpan = span;
    }

    int getColumnSpan() {
        return mColSpan;
    }

    @Override
    public void onClick(View v) {
        if (mTile.fragment != null) {
            Utils.startWithFragment(getContext(), mTile.fragment, mTile.fragmentArguments, null, 0,
                    mTile.titleRes, mTile.getTitle(getResources()));
        } else if (mTile.intent != null) {
            int numUserHandles = mTile.userHandle.size();
            if (numUserHandles > 1) {
                ProfileSelectDialog.show(((Activity) getContext()).getFragmentManager(), mTile);
            } else if (numUserHandles == 1) {
                getContext().startActivityAsUser(mTile.intent, mTile.userHandle.get(0));
            } else {
                getContext().startActivity(mTile.intent);
            }
        }
    }

    public Switch getSwitchView() {
        return mSwitch;
    }
  
  public void setcolors(View view) {
        mImageView = (ImageView) view.findViewById(R.id.icon);
        mTitleTextView = (TextView) view.findViewById(R.id.title);
        mCustomColors = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.DASHBOARD_CUSTOM_COLORS, 0) == 1;
        mIconColor = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.DB_ICON_COLOR, 0xFFFFFFFF);         
	mTextcolor = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.DB_TEXT_COLOR, 0xFFFFFFFF); 
        if (mCustomColors) {
		if (mTitleTextView !=null) {
		mTitleTextView.setTextColor(mTextcolor);      
		}		
		if (mImageView != null) {
		mImageView.setColorFilter(mIconColor, Mode.SRC_ATOP);		
		}
        }
    } 
    
    public void updatedashboard(Context context ,View view) {
         mCustomColors = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.DASHBOARD_CUSTOM_COLORS, 0) == 1;
         if(mCustomColors) {       
         setBackgroundColor(Settings.System.getInt(context.getContentResolver(),
            Settings.System.SETTINGS_BG_COLOR, 0xff000000));
        mDivider = (View) view.findViewById(R.id.tile_divider);
        mDivider.setBackgroundResource(R.drawable.dashboard_tile_background);
        mDivider.setBackgroundColor(Settings.System.getInt(context.getContentResolver(),
            Settings.System.SETTINGS_BG_COLOR, 0xff000000));
        mStatusTextView.setTextColor(Settings.System.getInt(context.getContentResolver(),
            Settings.System.SETTINGS_CATEGORY_TEXT_COLOR, 0xff1976D2));
        mStatusTextView.setTextSize(Settings.System.getIntForUser(context.getContentResolver(),
            Settings.System.SETTINGS_TITLE_TEXT_SIZE, 14,
               UserHandle.USER_CURRENT));
        mTitleTextView.setTextSize(Settings.System.getIntForUser(context.getContentResolver(),
            Settings.System.SETTINGS_TITLE_TEXT_SIZE, 18,
               UserHandle.USER_CURRENT));                 
            }
      }

    public void setEnabledTile(boolean enabled) {
        mImageView.setAlpha(enabled ? 1f : Utils.DISABLED_ALPHA);
        mTitleTextView.setEnabled(enabled);
        mStatusTextView.setEnabled(enabled);
        mSwitch.setEnabled(enabled);
        mSwitch.setClickable(enabled);
        setFocusable(enabled);
        setEnabled(enabled);
    }
}
