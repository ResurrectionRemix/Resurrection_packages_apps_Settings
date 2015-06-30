/*
 * Copyright (C) 2015 The CyanogenMod Project
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

package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.hardware.fingerprint.Fingerprint;
import android.os.Bundle;
import android.service.fingerprint.FingerprintManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.internal.widget.LockPatternUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ManageFingerprints extends SettingsActivity {
    private static final String TAG = ManageFingerprints.class.getSimpleName();
    private static final String PASSWORD_CONFIRMED = "password_confirmed";
    private static final String WAITING_FOR_CONFIRMATION = "waiting_for_confirmation";
    public static final String CONFIRM_CREDENTIALS = "confirm_credentials";

    private static final int CONFIRM_EXISTING_REQUEST = 100;
    private static final int SET_FALLBACK = 99;

    private LockPatternUtils mLockPatternUtils;
    private boolean mPasswordConfirmed = false;
    private boolean mWaitingForConfirmation = false;

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, getFragmentClass().getName());
        return modIntent;
    }

    public static Intent createIntent(Context context, final boolean isFallback,
                                      boolean requirePassword, boolean confirmCredentials) {
        Intent intent = new Intent().setClass(context, ManageFingerprints.class);
        intent.putExtra(ChooseLockGeneric.CONFIRM_CREDENTIALS, confirmCredentials);
        intent.putExtra(LockPatternUtils.LOCKSCREEN_BIOMETRIC_WEAK_FALLBACK, isFallback);
        intent.putExtra(LockPatternUtils.LOCKSCREEN_FINGERPRINT_FALLBACK, isFallback);
        intent.putExtra(EncryptionInterstitial.EXTRA_REQUIRE_PASSWORD, requirePassword);
        return intent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (FingerprintListFragment.class.getName().equals(fragmentName)) return true;
        return false;
    }

    Class<? extends Fragment> getFragmentClass() {
        return FingerprintListFragment.class;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CharSequence msg = getText(R.string.lockpassword_choose_your_password_header);
        setTitle(msg);

        final boolean confirmCredentials = getIntent().getBooleanExtra(CONFIRM_CREDENTIALS, true);
        mPasswordConfirmed = !confirmCredentials;

        if (savedInstanceState != null) {
            mPasswordConfirmed = savedInstanceState.getBoolean(PASSWORD_CONFIRMED);
            mWaitingForConfirmation = savedInstanceState.getBoolean(WAITING_FOR_CONFIRMATION);
        }

        if (!mPasswordConfirmed && !mWaitingForConfirmation) {
            ChooseLockSettingsHelper helper =
                    new ChooseLockSettingsHelper(this, null);
            if (!helper.launchConfirmationActivity(CONFIRM_EXISTING_REQUEST, null, null)) {
                mPasswordConfirmed = true; // no password set, so no need to confirm
            } else {
                mWaitingForConfirmation = true;
            }
        }

        // Set alternate pin
        boolean setFallback = getIntent().getBooleanExtra(
                LockPatternUtils.LOCKSCREEN_FINGERPRINT_FALLBACK, false);
        if (setFallback) {
            Intent fallBackIntent = new Intent().setClass(this,
                    ChooseLockGeneric.InternalActivity.class);
            fallBackIntent.putExtra(LockPatternUtils.LOCKSCREEN_BIOMETRIC_WEAK_FALLBACK, true);
            fallBackIntent.putExtra(CONFIRM_CREDENTIALS, false);
            fallBackIntent.putExtra(EXTRA_SHOW_FRAGMENT_TITLE,
                    R.string.backup_lock_settings_picker_title);
            fallBackIntent.putExtra(LockPatternUtils.LOCKSCREEN_FINGERPRINT_FALLBACK, true);
            startActivityForResult(fallBackIntent, SET_FALLBACK);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Saved so we don't force user to re-enter their password if configuration changes
        outState.putBoolean(PASSWORD_CONFIRMED, mPasswordConfirmed);
        outState.putBoolean(WAITING_FOR_CONFIRMATION, mWaitingForConfirmation);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CONFIRM_EXISTING_REQUEST && resultCode == Activity.RESULT_OK) {
            mWaitingForConfirmation = false;
            mPasswordConfirmed = true;
        } else if (requestCode == SET_FALLBACK &&
                (resultCode == Activity.RESULT_OK || resultCode == Activity.RESULT_FIRST_USER)) {
            Log.d("TAG", "fallback password set");
            mLockPatternUtils = new LockPatternUtils(this);
            mLockPatternUtils.setUseFingerprint();
        } else {
            finish();
            Log.d("TAG", "fallback password NOT set");
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public static class FingerprintListFragment extends Fragment {
        public static final int MAX_NUM_FINGERPRINTS = 5;

        private FingerprintAdapter mAdapter;
        private ListView mFingerList;
        private FingerprintManager mFpManager;

        // required constructor for fragments
        public FingerprintListFragment() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (!(getActivity() instanceof ManageFingerprints)) {
                throw new SecurityException("Fragment contained in wrong activity");
            }

            mFpManager = (FingerprintManager) getActivity()
                    .getSystemService(Context.FINGERPRINT_SERVICE);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fingerprint_list, container, false);
            mFingerList = (ListView) v.findViewById(R.id.list);
            mAdapter = new FingerprintAdapter(getActivity());
            mFingerList.setAdapter(mAdapter);
            mFingerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if(FingerprintAdapter.TYPE_ADD_FINGER == mAdapter.getItemViewType(position)) {
                        addFinger();
                    } else {
                        Fingerprint fingerprint = (Fingerprint) mAdapter.getItem(position);
                        showRenameDeleteDialog(fingerprint);
                    }
                }
            });
            return v;
        }

        private void addFinger() {
            // Check if we can actually add more fingerprints
            if (mAdapter.getCount() - 1 == MAX_NUM_FINGERPRINTS)  {
                DialogFragment dialogFragment = MaxNumFingerprintsDialog.newInstance();
                dialogFragment.show(getChildFragmentManager(), "MaxFingers");
            } else {

            }
        }
        public void doRename(Fingerprint fingerprint, String name) {
            mFpManager.setFingerprintName(fingerprint.getFingerId(), name);
            mAdapter.notifyDataSetChanged();
        }

        public void doDelete(Fingerprint fingerprint) {
            mFpManager.remove(fingerprint.getFingerId());
            mAdapter.notifyDataSetChanged();
        }
        private void showRenameDeleteDialog(Fingerprint fingerprint) {
            DialogFragment dialogFragment = RenameAndDeleteFragment.newInstance(fingerprint);
            dialogFragment.show(getChildFragmentManager(), "Rename");
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode,
                Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public static class FingerprintAdapter extends BaseAdapter {
        public static final int TYPE_FINGERPRINT = 0;
        public static final int TYPE_ADD_FINGER = 1;

        private LayoutInflater mInflater;
        private Resources mRes;
        private List<Fingerprint> mFingerprints = Collections.EMPTY_LIST;
        private FingerprintManager mFpManager;

        public FingerprintAdapter(Context context) {
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mFpManager =
                    (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
            mFingerprints = mFpManager.getEnrolledFingerprints();
            mRes = context.getResources();
        }

        @Override
        public void notifyDataSetChanged() {
            mFingerprints = mFpManager.getEnrolledFingerprints();
            super.notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mFingerprints.size() + 1;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == mFingerprints.size()) {
                return TYPE_ADD_FINGER;
            } else {
                return TYPE_FINGERPRINT;
            }
        }

        @Override
        public Object getItem(int position) {
            if (position < mFingerprints.size()) {
                return mFingerprints.get(position);
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            if (position < mFingerprints.size()) {
                return mFingerprints.get(position).getFingerId();
            } else {
                return -1;
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            int viewType = getItemViewType(position);
            if (viewType == TYPE_FINGERPRINT) {
                Fingerprint fingerprintInfo = (Fingerprint) getItem(position);
                if (convertView == null) {
                    convertView = createFingerprintView(parent);
                }
                Holder holder = (Holder) convertView.getTag();
                holder.mName.setText(fingerprintInfo.getName());
                holder.mImage.setImageResource(R.drawable.ic_fingerprint);
            } else {
                if (convertView == null) {
                    convertView = createAddFingerprintView(parent);
                }
                Holder holder = (Holder) convertView.getTag();
                holder.mName.setText(R.string.fingerprint_item_add_new_fingerprint);
                holder.mImage.setImageResource(R.drawable.ic_add_black_18dp);

                if (mFingerprints.size() == FingerprintListFragment.MAX_NUM_FINGERPRINTS) {
                    holder.mImage.setAlpha(0.5f);
                    holder.mName.setAlpha(0.5f);
                } else {
                    holder.mImage.setAlpha(1f);
                    holder.mName.setAlpha(1f);
                }

            }

            return convertView;
        }

        private View createFingerprintView(ViewGroup parent) {
            View v = mInflater.inflate(R.layout.fingerprint_item, parent, false);
            Holder holder = new Holder();
            v.setTag(holder);
            holder.mName = (TextView) v.findViewById(R.id.name);
            holder.mImage = (ImageView) v.findViewById(R.id.image);
            return v;
        }

        private View createAddFingerprintView(ViewGroup parent) {
            View v = mInflater.inflate(R.layout.fingerprint_item, parent, false);
            Holder holder = new Holder();
            v.setTag(holder);
            holder.mName = (TextView) v.findViewById(R.id.name);
            holder.mImage = (ImageView) v.findViewById(R.id.image);
            return v;
        }

        public static class Holder {
            public TextView mName;
            public ImageView mImage;
        }
    }


    public static class RenameAndDeleteFragment extends DialogFragment {
        private static final String ARG_FINGERPRINT = "fingerprint";

        private Fingerprint mFingerprint;
        private EditText mFingerNameEditText;

        static RenameAndDeleteFragment newInstance(Fingerprint fingerprint) {
            RenameAndDeleteFragment fragment = new RenameAndDeleteFragment();
            Bundle args = new Bundle();
            args.putParcelable("fingerprint", fingerprint);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            mFingerprint = getArguments().getParcelable(ARG_FINGERPRINT);

            LayoutInflater inflater = getActivity().getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.fingerprint_rename_dialog, null, false);
            mFingerNameEditText = (EditText) dialogView.findViewById(R.id.name);
            mFingerNameEditText.setText(mFingerprint.getName());

            AlertDialog dialog = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.fingerprint_dialog_title_rename)
                    .setPositiveButton(R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    String name = mFingerNameEditText.getText().toString();
                                    ((FingerprintListFragment)getParentFragment())
                                            .doRename(mFingerprint, name);
                                }
                            }
                    )
                    .setNegativeButton(R.string.dialog_delete_title,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ((FingerprintListFragment)getParentFragment())
                                            .doDelete(mFingerprint);
                                }
                            }
                    )
                    .setView(dialogView)
                    .create();
            return dialog;
        }
    }

    public static class MaxNumFingerprintsDialog extends DialogFragment {
        static MaxNumFingerprintsDialog newInstance() {
            return new MaxNumFingerprintsDialog();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            String msg = getString(R.string.fingerprint_dialog_msg_max_fingers_reached,
                    FingerprintListFragment.MAX_NUM_FINGERPRINTS);

            AlertDialog dialog = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.fingerprint_dialog_title_max_fingers_reached)
                    .setPositiveButton(R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dismiss();
                                }
                            }
                    )
                    .setMessage(msg)
                    .create();
            return dialog;
        }
    }
}
