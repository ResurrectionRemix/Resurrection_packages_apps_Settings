/*
 * Copyright (C) 2014 TeamEos
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

package com.android.settings.rr;

import com.android.internal.utils.du.ActionConstants;
import com.android.internal.utils.du.ActionConstants.Fling;
import com.android.internal.utils.du.DUActionUtils;

import com.android.settings.R;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.preference.Preference.BaseSavedState;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

public class LongSwipePreference extends DialogPreference implements
		SeekBar.OnSeekBarChangeListener {
	private static final String TAG = LongSwipePreference.class.getSimpleName();

	// phablets and tablets
	private static final String LONG_SWIPE_URI_LEFT_H = Settings.Secure.FLING_LONGSWIPE_THRESHOLD_LEFT_LAND;
	private static final String LONG_SWIPE_URI_RIGHT_H = Settings.Secure.FLING_LONGSWIPE_THRESHOLD_RIGHT_LAND;
	private static final String LONG_SWIPE_URI_LEFT_V = Settings.Secure.FLING_LONGSWIPE_THRESHOLD_LEFT_PORT;
	private static final String LONG_SWIPE_URI_RIGHT_V = Settings.Secure.FLING_LONGSWIPE_THRESHOLD_RIGHT_PORT;

	// normal screens - bar goes vertical
	private static final String LONG_SWIPE_URI_UP = Settings.Secure.FLING_LONGSWIPE_THRESHOLD_UP_LAND;
	private static final String LONG_SWIPE_URI_DOWN = Settings.Secure.FLING_LONGSWIPE_THRESHOLD_DOWN_LAND;

	// same for all devices
	private SeekBar mRightPort;
	private TextView mRightPortVal;
	private SeekBar mLeftPort;
	private TextView mLeftPortVal;

	// this group will double as right/left or up/down
	// depending on screen size
	private SeekBar mRightLand;
	private TextView mRightLandVal;
	private SeekBar mLeftLand;
	private TextView mLeftLandVal;

	// long swipe thresholds user set values
	// as read from settings or being committed
	private int leftLandSetting;
	private int rightLandSetting;
	private int leftPortSetting;
	private int rightPortSetting;

	// hold default values for reset
	private int leftLandDef;
	private int rightLandDef;
	private int leftPortDef;
	private int rightPortDef;

	// min/max values for seekbar math
	private int leftLandMin;
	private int rightLandMin;
	private int leftPortMin;
	private int rightPortMin;
	private int leftLandMax;
	private int rightLandMax;
	private int leftPortMax;
	private int rightPortMax;

	// track the instance state of the seekbars
	private int leftLandTemp;
	private int rightLandTemp;
	private int leftPortTemp;
	private int rightPortTemp;

	private Context mContext;
	private ContentResolver mResolver;

	public LongSwipePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		mResolver = context.getContentResolver();
		loadDefaults(getConfigs(context));
		updateValues();
		setDialogLayoutResource(R.layout.long_swipe_threshold);
	}

    private void loadDefaults(Bundle b) {
        leftLandDef = Math.round(b.getFloat(DUActionUtils.isNormalScreen() ? Fling.CONFIG_FlingLongSwipeVerticalDown
                        : Fling.CONFIG_FlingLongSwipeLandscapeLeft) * 100);
        rightLandDef = Math.round(b.getFloat(DUActionUtils.isNormalScreen() ? Fling.CONFIG_FlingLongSwipeVerticalUp
                        : Fling.CONFIG_FlingLongSwipeLandscapeRight) * 100);
        leftPortDef = Math.round(b.getFloat(Fling.CONFIG_FlingLongSwipePortraitLeft) * 100);
        rightPortDef = Math.round(b.getFloat(Fling.CONFIG_FlingLongSwipePortraitRight) * 100);

        String pkg = mContext.getPackageName();
        int min = Math.round((Float) DUActionUtils.getValue(mContext, "config_flingMinLongSwipeThreshold",
                DUActionUtils.DIMEN, DUActionUtils.FORMAT_FLOAT, pkg) * 100);
        int max = Math.round((Float) DUActionUtils.getValue(mContext, "config_flingMaxLongSwipeThreshold",
                DUActionUtils.DIMEN, DUActionUtils.FORMAT_FLOAT, pkg) * 100);

        Log.i(TAG,
                "Min/Max Fling swipe thresholds min/max: " + String.valueOf(min) + "/"
                        + String.valueOf(max));

        leftLandMax = max;
        rightLandMax = max;
        leftPortMax = max;
        rightPortMax = max;
        leftLandMin = min;
        rightLandMin = min;
        leftPortMin = min;
        rightPortMin = min;
    }

    private Bundle getConfigs(Context ctx) {
        try {
            Bundle b = ActionConstants.getDefaults(ActionConstants.FLING).getConfigs(ctx);
            Log.i(TAG, "Got config bundle! dump: " + b.toString());
            return b;
        } catch (Exception e) {
            return null; // it's all over anyways
        }
    }

	@Override
	protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
		builder.setNeutralButton(R.string.long_swipe_reset,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				});
	}

	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);
		ViewGroup portRight = (ViewGroup) view
				.findViewById(R.id.right_swipe_port_container);
		mRightPortVal = (TextView) portRight.findViewById(R.id.value);
		mRightPortVal.setText(getPercentString(rightPortTemp));
		mRightPort = (SeekBar) portRight.findViewById(R.id.seekbar);
		mRightPort.setMax(rightPortMax - rightPortMin);
		mRightPort.setProgress(rightPortTemp - rightPortMin);
		mRightPort.setOnSeekBarChangeListener(this);

		ViewGroup portLeft = (ViewGroup) view
				.findViewById(R.id.left_swipe_port_container);
		mLeftPortVal = (TextView) portLeft.findViewById(R.id.value);
		mLeftPortVal.setText(getPercentString(leftPortTemp));
		mLeftPort = (SeekBar) portLeft.findViewById(R.id.seekbar);
		mLeftPort.setMax(leftPortMax - leftPortMin);
		mLeftPort.setProgress(leftPortTemp - leftPortMin);
		mLeftPort.setOnSeekBarChangeListener(this);

		ViewGroup landRight = (ViewGroup) view
				.findViewById(R.id.right_swipe_land_container);
		TextView labelRight = (TextView) landRight.findViewById(R.id.text);
		if (DUActionUtils.isNormalScreen()) {
			labelRight.setText(mContext.getString(R.string.up_swipe_title));
		}
		mRightLandVal = (TextView) landRight.findViewById(R.id.value);
		mRightLandVal.setText(getPercentString(rightLandTemp));
		mRightLand = (SeekBar) landRight.findViewById(R.id.seekbar);
		mRightLand.setMax(rightLandMax - rightLandMin);
		mRightLand.setProgress(rightLandTemp - rightLandMin);
		mRightLand.setOnSeekBarChangeListener(this);

		ViewGroup landLeft = (ViewGroup) view
				.findViewById(R.id.left_swipe_land_container);
		TextView labelLeft = (TextView) landLeft.findViewById(R.id.text);
		if (DUActionUtils.isNormalScreen()) {
			labelLeft.setText(mContext.getString(R.string.down_swipe_title));
		}
		mLeftLandVal = (TextView) landLeft.findViewById(R.id.value);
		mLeftLandVal.setText(getPercentString(leftLandTemp));
		mLeftLand = (SeekBar) landLeft.findViewById(R.id.seekbar);
		mLeftLand.setMax(leftLandMax - leftLandMin);
		mLeftLand.setProgress(leftLandTemp - leftLandMin);
		mLeftLand.setOnSeekBarChangeListener(this);

	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		if (seekBar.equals(mRightPort)) {
			rightPortTemp = progress + rightPortMin;
			mRightPortVal.setText(getPercentString(rightPortTemp));
		} else if (seekBar.equals(mLeftPort)) {
			leftPortTemp = progress + leftPortMin;
			mLeftPortVal.setText(getPercentString(leftPortTemp));
		} else if (seekBar.equals(mRightLand)) {
			rightLandTemp = progress + rightLandMin;
			mRightLandVal.setText(getPercentString(rightLandTemp));
		} else if (seekBar.equals(mLeftLand)) {
			leftLandTemp = progress + leftLandMin;
			mLeftLandVal.setText(getPercentString(leftLandTemp));
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void showDialog(Bundle state) {
		super.showDialog(state);
		AlertDialog d = (AlertDialog) getDialog();
		Button defaultsButton = d.getButton(DialogInterface.BUTTON_NEUTRAL);
		defaultsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mRightPort.setProgress(rightPortDef - rightPortMin);
				mLeftPort.setProgress(leftPortDef - leftPortMin);
				mRightLand.setProgress(rightLandDef - rightLandMin);
				mLeftLand.setProgress(leftLandDef - leftLandMin);
			}
		});
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		if (!positiveResult) {
			// user canceled, set Temp values back to
			// last saved settings vals
			leftPortTemp = leftPortSetting;
			rightPortTemp = rightPortSetting;
			leftLandTemp = leftLandSetting;
			rightLandTemp = rightLandSetting;
			return;
		}
		String rightUri;
		String leftUri;

		if (DUActionUtils.isNormalScreen()) {
			rightUri = LONG_SWIPE_URI_UP;
			leftUri = LONG_SWIPE_URI_DOWN;
		} else {
			rightUri = LONG_SWIPE_URI_RIGHT_H;
			leftUri = LONG_SWIPE_URI_LEFT_H;
		}

		commitValue(LONG_SWIPE_URI_LEFT_V, leftPortTemp);
		commitValue(LONG_SWIPE_URI_RIGHT_V, rightPortTemp);
		commitValue(leftUri, leftLandTemp);
		commitValue(rightUri, rightLandTemp);

		// all the settings are equal since we committed
		leftPortSetting = leftPortTemp;
		rightPortSetting = rightPortTemp;
		leftLandSetting = leftLandTemp;
		rightLandSetting = rightLandTemp;
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		final Parcelable superState = super.onSaveInstanceState();
		if (getDialog() == null || !getDialog().isShowing()) {
			return superState;
		}

		// Save the dialog state
		final SavedState myState = new SavedState(superState);
		myState.mLSTleftPort = leftPortTemp;
		myState.mLSTrightPort = rightPortTemp;
		myState.mLSTleftLand = leftLandTemp;
		myState.mLSTrightLand = rightLandTemp;

		return myState;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		if (state == null || !state.getClass().equals(SavedState.class)) {
			// Didn't save state for us in onSaveInstanceState
			super.onRestoreInstanceState(state);
			return;
		}

		SavedState myState = (SavedState) state;
		super.onRestoreInstanceState(myState.getSuperState());

		mLeftPort.setProgress(myState.mLSTleftPort - leftPortMin);
		mRightPort.setProgress(myState.mLSTrightPort - rightPortMin);
		mLeftLand.setProgress(myState.mLSTleftLand - leftLandMin);
		mRightLand.setProgress(myState.mLSTrightLand - rightLandMin);
	}

	private void updateValues() {
		String rightUri;
		String leftUri;

		if (DUActionUtils.isNormalScreen()) {
			rightUri = LONG_SWIPE_URI_UP;
			leftUri = LONG_SWIPE_URI_DOWN;
		} else {
			rightUri = LONG_SWIPE_URI_RIGHT_H;
			leftUri = LONG_SWIPE_URI_LEFT_H;
		}

		leftLandSetting = float2int(Settings.Secure.getFloat(mResolver,
				leftUri, int2float(leftLandDef)));
		leftLandTemp = leftLandSetting;

		rightLandSetting = float2int(Settings.Secure.getFloat(mResolver,
				rightUri, int2float(rightLandDef)));
		rightLandTemp = rightLandSetting;

		leftPortSetting = float2int(Settings.Secure.getFloat(mResolver,
				LONG_SWIPE_URI_LEFT_V, int2float(leftPortDef)));
		leftPortTemp = leftPortSetting;

		rightPortSetting = float2int(Settings.Secure.getFloat(mResolver,
				LONG_SWIPE_URI_RIGHT_V, int2float(rightPortDef)));
		rightPortTemp = rightPortSetting;

	}

	private void commitValue(String uri, int val) {
		Settings.Secure.putFloat(mResolver, uri, int2float(val));
	}

	private String getPercentString(int val) {
		return String.format("%d%%", val);
	}

	private int float2int(float f) {
		return Math.round(f * 100);
	}

	private float int2float(int i) {
		return ((float) (i * 100) / 10000);
	}

	private static class SavedState extends BaseSavedState {
		int mLSTleftPort;
		int mLSTrightPort;
		int mLSTleftLand;
		int mLSTrightLand;

		public SavedState(Parcelable superState) {
			super(superState);
		}

		public SavedState(Parcel source) {
			super(source);
			mLSTleftPort = source.readInt();
			mLSTrightPort = source.readInt();
			mLSTleftLand = source.readInt();
			mLSTrightLand = source.readInt();
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeInt(mLSTleftPort);
			dest.writeInt(mLSTrightPort);
			dest.writeInt(mLSTleftLand);
			dest.writeInt(mLSTrightLand);
		}

		public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {

			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}

			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
	}
}
