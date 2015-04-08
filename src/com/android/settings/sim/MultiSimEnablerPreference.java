/**
 * Copyright (c) 2014, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */


package com.android.settings.sim;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.TelephonyIntents;
import com.android.settings.R;
import com.android.settings.Utils;

import java.util.List;

/**
 * SimEnabler is a helper to manage the slot on/off checkbox preference. It is
 * turns on/off slot and ensures the summary of the preference reflects the current state.
 */
public class MultiSimEnablerPreference extends SwitchPreference implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "MultiSimEnablerPreference";
    private static final boolean DBG = true;

    private static final int EVT_UPDATE = 1;
    private static final int EVT_SHOW_RESULT_DLG = 2;
    private static final int EVT_PROGRESS_DLG_TIME_OUT = 3;

    private int mSlotId;
    private SubscriptionInfo mSir;

    private Boolean mTargetState = null;
    private Handler mParentHandler = null;
    private AlertDialog mConfirmationDialog = null;
    private ProgressDialog mProgressDialog = null;
    private boolean mClicking;
    private Switch mSwitch;

    //Delay for progress dialog to dismiss
    private static final int PROGRESS_DLG_TIME_OUT = 30000;
    private static final int MSG_DELAY_TIME = 2000;

    private SubscriptionManager mSubscriptionManager;

    private int[] mTintArr;
    private String[] mColorStrings;
    private int mTintSelectorPos;

    public MultiSimEnablerPreference(Context context, SubscriptionInfo sir, Handler handler,
            int slotId) {
        super(context);

        mSlotId = slotId;
        mSir = sir;
        mParentHandler = handler;
        mSubscriptionManager = SubscriptionManager.from(context);
        mTintArr = context.getResources().getIntArray(com.android.internal.R.array.sim_colors);
        mColorStrings = context.getResources().getStringArray(R.array.color_picker);
        mTintSelectorPos = 0;

        setOnPreferenceChangeListener(this);
        setPersistent(false);
        setTitle(context.getString(R.string.sim_card_number_title, mSlotId + 1));
    }

    public void update() {
        logd("update()" + mSir);

        boolean isSubValid = isCurrentSubValid();
        setEnabled(isSubValid);

        logd("update() isSubValid "  + isSubValid);
        if (isSubValid) {
            String number = TextUtils.isEmpty(mSir.getNumber())
                    ? getContext().getString(R.string.sim_no_number_set)
                    : mSir.getNumber();
            setSummary(getContext().getString(R.string.sim_settings_summary,
                    getSimDisplayName(), number));
            setIcon(new BitmapDrawable(getContext().getResources(),
                    mSir.createIconBitmap(getContext())));

            boolean isActivated = mSir.mStatus == SubscriptionManager.ACTIVE;
            updateCheckedState(isActivated);
        } else {
            setSummary(R.string.sim_slot_empty);
            setIcon(null);
        }
    }

    public void cleanUp() {
         mSubscriptionManager.removeOnSubscriptionsChangedListener(mSubscriptionListener);
        dismissProgressDialog();
        if (mConfirmationDialog != null) {
            mConfirmationDialog.dismiss();
            mConfirmationDialog = null;
        }
    }

    public void createEditDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        final LayoutInflater inflater = LayoutInflater.from(getContext());

        final View dialogLayout = inflater.inflate(R.layout.multi_sim_dialog, null);
        builder.setView(dialogLayout);

        final EditText nameText = (EditText)dialogLayout.findViewById(R.id.sim_name);
        nameText.setText(mSir.getDisplayName());

        final Spinner tintSpinner = (Spinner) dialogLayout.findViewById(R.id.spinner);
        SelectColorAdapter adapter = new SelectColorAdapter(getContext(),
                R.layout.settings_color_picker_item, mColorStrings);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tintSpinner.setAdapter(adapter);
        for (int i = 0; i < mTintArr.length; i++) {
            if (mTintArr[i] == mSir.getIconTint()) {
                tintSpinner.setSelection(i);
                mTintSelectorPos = i;
                break;
            }
        }
        tintSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                tintSpinner.setSelection(pos);
                mTintSelectorPos = pos;
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        final TextView numberView = (TextView)dialogLayout.findViewById(R.id.number);
        numberView.setText(mSir.getNumber());

        final TextView carrierView = (TextView)dialogLayout.findViewById(R.id.carrier);
        carrierView.setText(mSir.getCarrierName());

        builder.setTitle(getContext().getString(R.string.sim_editor_title,
                mSir.getSimSlotIndex() + 1));

        builder.setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                mSir.setDisplayName(nameText.getText());
                mSubscriptionManager.setDisplayName(mSir.getDisplayName().toString(),
                        mSir.getSubscriptionId());

                final int tintSelected = tintSpinner.getSelectedItemPosition();
                int subscriptionId = mSir.getSubscriptionId();
                int tint = mTintArr[tintSelected];
                mSir.setIconTint(tint);
                mSubscriptionManager.setIconTint(tint, subscriptionId);

                update();
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        mSwitch = (Switch) view.findViewById(com.android.internal.R.id.switchWidget);
        mSwitch.setClickable(true);

        update();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        if (mClicking) {
            return false;
        }

        boolean checked = (Boolean) newValue;
        logd("onClick: " + checked);

        return configureSubscription(checked);
    }

    @Override
    public void onClick() {
        mClicking = true;
        super.onClick();
        mClicking = false;
    }

    private void sendMessage(int event, Handler handler, int delay) {
        Message message = handler.obtainMessage(event);
        handler.sendMessageDelayed(message, delay);
    }

    private boolean hasCard() {
        return TelephonyManager.getDefault().hasIccCard(mSlotId);
    }

    private boolean isAirplaneModeOn() {
        return (Settings.Global.getInt(getContext().getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0);
    }

    private boolean isCurrentSubValid() {
        if (isAirplaneModeOn() || !hasCard()) {
            return false;
        }

        List<SubscriptionInfo> sirList = mSubscriptionManager.getActiveSubscriptionInfoList();
        if (sirList == null) {
            return false;
        }

        for (SubscriptionInfo sir : sirList) {
            if (sir != null && mSlotId == sir.getSimSlotIndex()) {
                mSir = sir;
                break;
            }
        }

        return mSir != null && mSir.getSubscriptionId() > 0 && mSir.getSimSlotIndex() >= 0
                && mSir.mStatus != SubscriptionManager.SUB_CONFIGURATION_IN_PROGRESS;
    }

    private CharSequence getSimDisplayName() {
        if (mSir != null) {
            return mSir.getDisplayName();
        }

        return getContext().getString(R.string.sim_enabler_sim);
    }

    private void updateCheckedState(boolean checked) {
        setOnPreferenceChangeListener(null);
        if (mSwitch != null) {
            mSwitch.setChecked(checked);
        }
        setOnPreferenceChangeListener(this);
    }

    private int getActivatedSubInfoCount() {
        int activeSubInfoCount = 0;
        List<SubscriptionInfo> subInfoLists = mSubscriptionManager.getActiveSubscriptionInfoList();
        SubscriptionController subController = SubscriptionController.getInstance();
        if (subInfoLists != null) {
            for (SubscriptionInfo subInfo : subInfoLists) {
                if (subInfo.getStatus() == SubscriptionManager.ACTIVE) activeSubInfoCount++;
            }
        }
        return activeSubInfoCount;
    }

    private boolean configureSubscription(boolean enable) {
        if (isAirplaneModeOn()) {
            // do nothing but warning
            logd("APM is on, EXIT!");
            showToast(R.string.sim_enabler_airplane_on);
            return false;
        }
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            int[] subId = SubscriptionManager.getSubId(i);
            if (TelephonyManager.getDefault().getCallState(subId[0])
                    != TelephonyManager.CALL_STATE_IDLE) {
                logd("Call state for phoneId: " + i + " is not idle, EXIT!");
                showToast(R.string.sim_enabler_in_call);
                return false;
            }
        }

        if (!enable) {
            if (getActivatedSubInfoCount() > 1) {
                logd("More than one sub is active, Deactivation possible.");
                showDisableConfirmationDialog();
            } else {
                logd("Only one sub is active. Deactivation not possible.");
                showToast(R.string.sim_enabler_both_inactive);
                return false;
            }
        } else {
            logd("Activate the sub");
            sendSubConfigurationRequest(true);
        }

        return true;
    }

    private void showToast(int textResId) {
        Toast.makeText(getContext(), textResId, Toast.LENGTH_LONG).show();
    }

    private void sendSubConfigurationRequest(boolean enable) {
        if (mParentHandler == null) {
            return;
        }
        mTargetState = enable;

        showProgressDialog(enable);
        if (enable) {
            SubscriptionManager.activateSubId(mSir.getSubscriptionId());
        } else {
            SubscriptionManager.deactivateSubId(mSir.getSubscriptionId());
        }

        mSubscriptionManager.addOnSubscriptionsChangedListener(mSubscriptionListener);
    }

    private void processSetUiccDone() {
        sendMessage(EVT_UPDATE, mParentHandler, MSG_DELAY_TIME);
        sendMessage(EVT_SHOW_RESULT_DLG, mHandler, MSG_DELAY_TIME);
        mTargetState = null;
        mSubscriptionManager.removeOnSubscriptionsChangedListener(mSubscriptionListener);
    }

    private void showDisableConfirmationDialog() {
        if (mConfirmationDialog != null) {
            mConfirmationDialog.dismiss();
        }

        mConfirmationDialog = new AlertDialog.Builder(getContext())
                .setTitle(getSimDisplayName())
                .setMessage(R.string.sim_enabler_need_disable_sim)
                .setPositiveButton(R.string.sim_enabler_deactivate, mConfirmDialogListener)
                .setNegativeButton(android.R.string.no, mConfirmDialogListener)
                .setOnCancelListener(mConfirmDialogCancelListener)
                .create();

        mConfirmationDialog.setCanceledOnTouchOutside(false);
        mConfirmationDialog.show();
    }

    private void showProgressDialog(boolean enabling) {
        int stateTextResId = enabling
                ? R.string.sim_enabler_enabling : R.string.sim_enabler_disabling;

        dismissProgressDialog();

        mProgressDialog = new ProgressDialog(getContext());
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setMessage(getContext().getString(stateTextResId, getSimDisplayName()));
        mProgressDialog.setCancelable(false);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();

        sendMessage(EVT_PROGRESS_DLG_TIME_OUT, mHandler, PROGRESS_DLG_TIME_OUT);
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    private void logd(String msg) {
        if (DBG) Log.d(TAG + "(" + mSlotId + ")", msg);
    }

    private DialogInterface.OnClickListener mConfirmDialogListener =
            new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                sendSubConfigurationRequest(false);
            } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                updateCheckedState(true);
            }
        }
    };

    private DialogInterface.OnCancelListener mConfirmDialogCancelListener =
            new DialogInterface.OnCancelListener() {
        @Override
        public void onCancel(DialogInterface dialog) {
            mConfirmationDialog = null;
            update();
        }
    };

    private final SubscriptionManager.OnSubscriptionsChangedListener mSubscriptionListener =
            new SubscriptionManager.OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            logd("Received onSubscriptionChanged");
            SubscriptionInfo sir = Utils.findRecordBySubId(getContext(), mSir.getSubscriptionId());
            if (mTargetState == (sir != null)) {
                processSetUiccDone();
            }
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVT_SHOW_RESULT_DLG:
                    logd("EVT_SHOW_RESULT_DLG");
                    update();
                    dismissProgressDialog();
                    mHandler.removeMessages(EVT_PROGRESS_DLG_TIME_OUT);
                    break;
                case EVT_PROGRESS_DLG_TIME_OUT:
                    logd("EVT_PROGRESS_DLG_TIME_OUT");
                    dismissProgressDialog();
                    break;
                default:
                    break;
            }
        }
    };

    private class SelectColorAdapter extends ArrayAdapter<CharSequence> {
        private final Context mContext;
        private final int mResId;

        public SelectColorAdapter(Context context, int resource, String[] arr) {
            super(context, resource, arr);
            mContext = context;
            mResId = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View rowView;
            final ViewHolder holder;
            final Resources res = mContext.getResources();
            int iconSize = res.getDimensionPixelSize(R.dimen.color_swatch_size);
            int strokeWidth = res.getDimensionPixelSize(R.dimen.color_swatch_stroke_width);

            if (convertView == null) {
                // Cache views for faster scrolling
                rowView = inflater.inflate(mResId, parent, false);
                holder = new ViewHolder();
                ShapeDrawable drawable = new ShapeDrawable(new OvalShape());
                drawable.setIntrinsicHeight(iconSize);
                drawable.setIntrinsicWidth(iconSize);
                drawable.getPaint().setStrokeWidth(strokeWidth);
                holder.label = (TextView) rowView.findViewById(R.id.color_text);
                holder.icon = (ImageView) rowView.findViewById(R.id.color_icon);
                holder.swatch = drawable;
                rowView.setTag(holder);
            } else {
                rowView = convertView;
                holder = (ViewHolder) rowView.getTag();
            }

            holder.label.setText(getItem(position));
            holder.swatch.getPaint().setColor(mTintArr[position]);
            holder.swatch.getPaint().setStyle(Paint.Style.FILL_AND_STROKE);
            holder.icon.setVisibility(View.VISIBLE);
            holder.icon.setImageDrawable(holder.swatch);
            return rowView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View rowView = getView(position, convertView, parent);
            final ViewHolder holder = (ViewHolder) rowView.getTag();

            if (mTintSelectorPos == position) {
                holder.swatch.getPaint().setStyle(Paint.Style.FILL_AND_STROKE);
            } else {
                holder.swatch.getPaint().setStyle(Paint.Style.STROKE);
            }
            holder.icon.setVisibility(View.VISIBLE);
            return rowView;
        }

        private class ViewHolder {
            TextView label;
            ImageView icon;
            ShapeDrawable swatch;
        }
    }
}
