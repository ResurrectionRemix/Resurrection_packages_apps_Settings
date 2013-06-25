/*
 * Copyright (C) 2008 The Android Open Source Project
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
import android.app.Fragment;
import android.content.Intent;
import android.gesture.Gesture;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.preference.PreferenceActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.internal.widget.LinearLayoutWithDefaultTouchRecepient;
import com.android.internal.widget.LockGestureView;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.internal.widget.LockPatternView.Cell;

import java.util.List;

/**
 * Launch this when you want the user to confirm their lock gesture.
 *
 * Sets an activity result of {@link android.app.Activity#RESULT_OK} when the user
 * successfully confirmed their gesture.
 */
public class ConfirmLockGesture extends PreferenceActivity {

    /**
     * Names of {@link CharSequence} fields within the originating {@link android.content.Intent}
     * that are used to configure the keyguard confirmation view's labeling.
     * The view will use the system-defined resource strings for any labels that
     * the caller does not supply.
     */
    public static final String PACKAGE = "com.android.settings";
    public static final String HEADER_TEXT = PACKAGE + ".ConfirmLockGesture.header";
    public static final String FOOTER_TEXT = PACKAGE + ".ConfirmLockGesture.footer";
    public static final String HEADER_WRONG_TEXT = PACKAGE + ".ConfirmLockGesture.header_wrong";
    public static final String FOOTER_WRONG_TEXT = PACKAGE + ".ConfirmLockGesture.footer_wrong";

    private enum Stage {
        NeedToUnlock,
        NeedToUnlockWrong,
        LockedOut
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CharSequence msg = getText(R.string.lockpassword_confirm_your_gesture_header);
        showBreadCrumbs(msg, msg);
    }

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, ConfirmLockGestureFragment.class.getName());
        modIntent.putExtra(EXTRA_NO_HEADERS, true);
        return modIntent;
    }

    public static class ConfirmLockGestureFragment extends Fragment {

        // how long we wait to clear a wrong gesture
        private static final int WRONG_GESTURE_CLEAR_TIMEOUT_MS = 2000;

        private static final String KEY_NUM_WRONG_ATTEMPTS = "num_wrong_attempts";

        private LockGestureView mLockGestureView;
        private LockPatternUtils mLockPatternUtils;
        private int mNumWrongConfirmAttempts;
        private CountDownTimer mCountdownTimer;

        private TextView mHeaderTextView;
        private TextView mFooterTextView;

        // caller-supplied text for various prompts
        private CharSequence mHeaderText;
        private CharSequence mFooterText;
        private CharSequence mHeaderWrongText;
        private CharSequence mFooterWrongText;

        // required constructor for fragments
        public ConfirmLockGestureFragment() {

        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mLockPatternUtils = new LockPatternUtils(getActivity());
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.confirm_lock_gesture, null);
            mHeaderTextView = (TextView) view.findViewById(R.id.headerText);
            mLockGestureView = (LockGestureView) view.findViewById(R.id.lockGesture);
            mFooterTextView = (TextView) view.findViewById(R.id.footerText);

            // make it so unhandled touch events within the unlock screen go to the
            // lock gesture view.
            final LinearLayoutWithDefaultTouchRecepient topLayout
                    = (LinearLayoutWithDefaultTouchRecepient) view.findViewById(R.id.topLayout);
            topLayout.setDefaultTouchRecepient(mLockGestureView);

            Intent intent = getActivity().getIntent();
            if (intent != null) {
                mHeaderText = intent.getCharSequenceExtra(HEADER_TEXT);
                mFooterText = intent.getCharSequenceExtra(FOOTER_TEXT);
                mHeaderWrongText = intent.getCharSequenceExtra(HEADER_WRONG_TEXT);
                mFooterWrongText = intent.getCharSequenceExtra(FOOTER_WRONG_TEXT);
            }

            mLockGestureView.setOnGestureListener(mConfirmExistingLockGestureListener);
            updateStage(Stage.NeedToUnlock);

            if (savedInstanceState != null) {
                mNumWrongConfirmAttempts = savedInstanceState.getInt(KEY_NUM_WRONG_ATTEMPTS);
            } else {
                // on first launch, if no lock gesture is set, then finish with
                // success (don't want user to get stuck confirming something that
                // doesn't exist).
                if (!mLockPatternUtils.savedGestureExists()) {
                    getActivity().setResult(Activity.RESULT_OK);
                    getActivity().finish();
                }
            }
            return view;
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            // deliberately not calling super since we are managing this in full
            outState.putInt(KEY_NUM_WRONG_ATTEMPTS, mNumWrongConfirmAttempts);
        }

        @Override
        public void onPause() {
            super.onPause();

            if (mCountdownTimer != null) {
                mCountdownTimer.cancel();
            }
        }

        @Override
        public void onResume() {
            super.onResume();

            // if the user is currently locked out, enforce it.
            long deadline = mLockPatternUtils.getLockoutAttemptDeadline();
            if (deadline != 0) {
                handleAttemptLockout(deadline);
            } else if (!mLockGestureView.isEnabled()) {
                // The deadline has passed, but the timer was cancelled...
                // Need to clean up.
                mNumWrongConfirmAttempts = 0;
                updateStage(Stage.NeedToUnlock);
            }
        }

        private void updateStage(Stage stage) {
            switch (stage) {
                case NeedToUnlock:
                    if (mHeaderText != null) {
                        mHeaderTextView.setText(mHeaderText);
                    } else {
                        mHeaderTextView.setText(R.string.lockgesture_need_to_unlock);
                    }
                    if (mFooterText != null) {
                        mFooterTextView.setText(mFooterText);
                    } else {
                        mFooterTextView.setText(R.string.lockgesture_need_to_unlock_footer);
                    }

                    mLockGestureView.setEnabled(true);
                    mLockGestureView.enableInput();
                    break;
                case NeedToUnlockWrong:
                    if (mHeaderWrongText != null) {
                        mHeaderTextView.setText(mHeaderWrongText);
                    } else {
                        mHeaderTextView.setText(R.string.lockgesture_need_to_unlock_wrong);
                    }
                    if (mFooterWrongText != null) {
                        mFooterTextView.setText(mFooterWrongText);
                    } else {
                        mFooterTextView.setText(R.string.lockgesture_need_to_unlock_wrong_footer);
                    }

                    mLockGestureView.setDisplayMode(LockGestureView.DisplayMode.Wrong);
                    mLockGestureView.setEnabled(true);
                    mLockGestureView.enableInput();
                    break;
                case LockedOut:
                    mLockGestureView.clearGesture();
                    // enabled = false means: disable input, and have the
                    // appearance of being disabled.
                    mLockGestureView.setEnabled(false); // appearance of being disabled
                    break;
            }

            // Always announce the header for accessibility. This is a no-op
            // when accessibility is disabled.
            mHeaderTextView.announceForAccessibility(mHeaderTextView.getText());
        }

        private Runnable mClearGestureRunnable = new Runnable() {
            public void run() {
                mLockGestureView.clearGesture();
            }
        };

        // clear the wrong gesture unless they have started a new one
        // already
        private void postClearPatternRunnable() {
            mLockGestureView.removeCallbacks(mClearGestureRunnable);
            mLockGestureView.postDelayed(mClearGestureRunnable, WRONG_GESTURE_CLEAR_TIMEOUT_MS);
        }

        /**
         * The gesture listener that responds according to a user confirming
         * an existing lock gesture.
         */
        private LockGestureView.OnLockGestureListener mConfirmExistingLockGestureListener
                = new LockGestureView.OnLockGestureListener()  {

            public void onGestureStart() {
                mLockGestureView.removeCallbacks(mClearGestureRunnable);
                mLockGestureView.setDisplayMode(LockGestureView.DisplayMode.Correct);
            }

            public void onGestureCleared() {
                mLockGestureView.removeCallbacks(mClearGestureRunnable);
            }

            public void onGestureDetected(Gesture gesture) {
                if (mLockPatternUtils.checkGesture(gesture)) {

                    Intent intent = new Intent();
                    getActivity().setResult(Activity.RESULT_OK, intent);
                    getActivity().finish();
                } else {
                    if (++mNumWrongConfirmAttempts
                            >= LockPatternUtils.FAILED_ATTEMPTS_BEFORE_TIMEOUT) {
                        long deadline = mLockPatternUtils.setLockoutAttemptDeadline();
                        handleAttemptLockout(deadline);
                    } else {
                        updateStage(Stage.NeedToUnlockWrong);
                        postClearPatternRunnable();
                    }
                }
            }
        };


        private void handleAttemptLockout(long elapsedRealtimeDeadline) {
            updateStage(Stage.LockedOut);
            long elapsedRealtime = SystemClock.elapsedRealtime();
            mCountdownTimer = new CountDownTimer(
                    elapsedRealtimeDeadline - elapsedRealtime,
                    LockPatternUtils.FAILED_ATTEMPT_COUNTDOWN_INTERVAL_MS) {

                @Override
                public void onTick(long millisUntilFinished) {
                    mHeaderTextView.setText(R.string.lockgesture_too_many_failed_confirmation_attempts_header);
                    final int secondsCountdown = (int) (millisUntilFinished / 1000);
                    mFooterTextView.setText(getString(
                            R.string.lockgesture_too_many_failed_confirmation_attempts_footer,
                            secondsCountdown));
                }

                @Override
                public void onFinish() {
                    mNumWrongConfirmAttempts = 0;
                    updateStage(Stage.NeedToUnlock);
                }
            }.start();
        }
    }
}
