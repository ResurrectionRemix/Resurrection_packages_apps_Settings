/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.settings;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.res.Resources;
import android.gesture.Gesture;
import android.gesture.GestureStore;
import android.gesture.Prediction;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.internal.widget.LinearLayoutWithDefaultTouchRecepient;
import com.android.internal.widget.LockGestureView;
import com.android.internal.widget.LockPatternUtils;

import java.util.ArrayList;

import static com.android.internal.widget.LockGestureView.DisplayMode;

/**
 * If the user has a lock gesture set already, makes them confirm the existing one.
 *
 * Then, prompts the user to choose a lock gesture:
 * - prompts for initial gesture
 * - asks for confirmation / restart
 * - saves chosen password when confirmed
 */
public class ChooseLockGesture extends PreferenceActivity {
    /**
     * Used by the choose lock gesture wizard to indicate the wizard is
     * finished, and each activity in the wizard should finish.
     * <p>
     * Previously, each activity in the wizard would finish itself after
     * starting the next activity. However, this leads to broken 'Back'
     * behavior. So, now an activity does not finish itself until it gets this
     * result.
     */
    static final int RESULT_FINISHED = RESULT_FIRST_USER;

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, ChooseLockGestureFragment.class.getName());
        modIntent.putExtra(EXTRA_NO_HEADERS, true);
        return modIntent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CharSequence msg = getText(R.string.lockpassword_choose_your_gesture_header);
        showBreadCrumbs(msg, msg);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // *** TODO ***
        return super.onKeyDown(keyCode, event);
    }

    public static class ChooseLockGestureFragment extends Fragment
            implements View.OnClickListener {

        public static final int CONFIRM_EXISTING_REQUEST = 55;

        // how long after a confirmation message is shown before moving on
        static final int INFORMATION_MSG_TIMEOUT_MS = 3000;

        // how long we wait to clear a wrong gesture
        private static final int WRONG_GESTURE_CLEAR_TIMEOUT_MS = 2000;

        private static final int ID_EMPTY_MESSAGE = -1;

        protected TextView mHeaderText;
        protected LockGestureView mLockGestureView;
        protected TextView mFooterText;
        private TextView mFooterLeftButton;
        private TextView mFooterRightButton;
        protected Gesture mChosenGesture = null;
        protected GestureStore mGestureStore;
        protected int mMinPredictionScore;

        @Override
        public void onActivityResult(int requestCode, int resultCode,
                Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            switch (requestCode) {
                case CONFIRM_EXISTING_REQUEST:
                    if (resultCode != Activity.RESULT_OK) {
                        getActivity().setResult(RESULT_FINISHED);
                        getActivity().finish();
                    }
                    updateStage(Stage.Introduction);
                    break;
            }
        }

        /**
         * The gesture listener that responds according to a user choosing a new
         * lock gesture.
         */
        protected LockGestureView.OnLockGestureListener mChooseNewLockGestureListener =
                new LockGestureView.OnLockGestureListener() {

                public void onGestureStart() {
                    mLockGestureView.removeCallbacks(mClearGestureRunnable);
                    mLockGestureView.setDisplayMode(DisplayMode.Correct);
                    gestureInProgress();
                }

                public void onGestureCleared() {
                    mLockGestureView.removeCallbacks(mClearGestureRunnable);
                }

                public void onGestureDetected(Gesture gesture) {
                    if (mUiStage == Stage.NeedToConfirm || mUiStage == Stage.ConfirmWrong) {
                        if (mChosenGesture == null) throw new IllegalStateException(
                                "null chosen pattern in stage 'need to confirm");
                        if (gestureMatch(gesture)) {
                            updateStage(Stage.ChoiceConfirmed);
                        } else {
                            updateStage(Stage.ConfirmWrong);
                        }
                    } else if (mUiStage == Stage.Introduction){
                        mChosenGesture = gesture;
                        if (mGestureStore.getGestures("lock_gesture") != null)
                            mGestureStore.removeEntry("lock_gesture");
                        mGestureStore.addGesture("lock_gesture", gesture);
                        updateStage(Stage.FirstChoiceValid);
                    } else {
                        throw new IllegalStateException("Unexpected stage " + mUiStage + " when "
                                + "entering the gesture.");
                    }
                }

                private void gestureInProgress() {
                    mHeaderText.setText(R.string.lockgesture_recording_inprogress);
                    mFooterText.setText("");
                    mFooterLeftButton.setEnabled(false);
                    mFooterRightButton.setEnabled(false);
                }
         };

        protected boolean gestureMatch(Gesture gesture) {
            ArrayList<Prediction> predictions = mGestureStore.recognize(gesture);
            if (predictions.size() > 0) {
                Prediction prediction = predictions.get(0);
                if (prediction.score > mMinPredictionScore) {
                    if (prediction.name.equals("lock_gesture")) {
                        Gesture foundGesture = mGestureStore.getGestures("lock_gesture").get(0);
                        if (foundGesture.getStrokesCount() == gesture.getStrokesCount())
                            return true;
                        else
                            return false;
                    }
                }
            }
            return false;
        }

        /**
         * The states of the left footer button.
         */
        enum LeftButtonMode {
            Cancel(R.string.cancel, true),
            CancelDisabled(R.string.cancel, false),
            Retry(R.string.lockgesture_retry_button_text, true),
            RetryDisabled(R.string.lockgesture_retry_button_text, false),
            Gone(ID_EMPTY_MESSAGE, false);


            /**
             * @param text The displayed text for this mode.
             * @param enabled Whether the button should be enabled.
             */
            LeftButtonMode(int text, boolean enabled) {
                this.text = text;
                this.enabled = enabled;
            }

            final int text;
            final boolean enabled;
        }

        /**
         * The states of the right button.
         */
        enum RightButtonMode {
            Continue(R.string.lockgesture_continue_button_text, true),
            ContinueDisabled(R.string.lockgesture_continue_button_text, false),
            Confirm(R.string.lockgesture_confirm_button_text, true),
            ConfirmDisabled(R.string.lockgesture_confirm_button_text, false),
            Ok(android.R.string.ok, true);

            /**
             * @param text The displayed text for this mode.
             * @param enabled Whether the button should be enabled.
             */
            RightButtonMode(int text, boolean enabled) {
                this.text = text;
                this.enabled = enabled;
            }

            final int text;
            final boolean enabled;
        }

        /**
         * Keep track internally of where the user is in choosing a gesture.
         */
        protected enum Stage {

            Introduction(
                    R.string.lockgesture_recording_intro_header,
                    LeftButtonMode.Cancel, RightButtonMode.ContinueDisabled,
                    ID_EMPTY_MESSAGE, true),
            HelpScreen(
                    R.string.lockgesture_settings_help_how_to_record,
                    LeftButtonMode.Gone, RightButtonMode.Ok, ID_EMPTY_MESSAGE, false),
            FirstChoiceValid(
                    R.string.lockgesture_pattern_entered_header,
                    LeftButtonMode.Retry, RightButtonMode.Continue, ID_EMPTY_MESSAGE, false),
            NeedToConfirm(
                    R.string.lockgesture_need_to_confirm,
                    LeftButtonMode.Cancel, RightButtonMode.ConfirmDisabled,
                    ID_EMPTY_MESSAGE, true),
            ConfirmWrong(
                    R.string.lockgesture_need_to_unlock_wrong,
                    LeftButtonMode.Cancel, RightButtonMode.ConfirmDisabled,
                    ID_EMPTY_MESSAGE, true),
            ChoiceConfirmed(
                    R.string.lockgesture_pattern_confirmed_header,
                    LeftButtonMode.Cancel, RightButtonMode.Confirm, ID_EMPTY_MESSAGE, false);


            /**
             * @param headerMessage The message displayed at the top.
             * @param leftMode The mode of the left button.
             * @param rightMode The mode of the right button.
             * @param footerMessage The footer message.
             * @param gestureEnabled Whether the pattern widget is enabled.
             */
            Stage(int headerMessage,
                    LeftButtonMode leftMode,
                    RightButtonMode rightMode,
                    int footerMessage, boolean gestureEnabled) {
                this.headerMessage = headerMessage;
                this.leftMode = leftMode;
                this.rightMode = rightMode;
                this.footerMessage = footerMessage;
                this.gestureEnabled = gestureEnabled;
            }

            final int headerMessage;
            final LeftButtonMode leftMode;
            final RightButtonMode rightMode;
            final int footerMessage;
            final boolean gestureEnabled;
        }

        private Stage mUiStage = Stage.Introduction;

        private Runnable mClearGestureRunnable = new Runnable() {
            public void run() {
                mLockGestureView.clearGesture();
            }
        };

        private ChooseLockSettingsHelper mChooseLockSettingsHelper;

        private static final String KEY_UI_STAGE = "uiStage";
        private static final String KEY_GESTURE_CHOICE = "chosenGesture";

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());
            mGestureStore = new GestureStore();
            mGestureStore.setOrientationStyle(GestureStore.ORIENTATION_SENSITIVE);
            try {
                mMinPredictionScore = getActivity().getResources().getInteger(
                        com.android.internal.R.integer.min_gesture_prediction_score);
            } catch (Resources.NotFoundException e) {
                mMinPredictionScore = 2;
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {

            // setupViews()
            View view = inflater.inflate(R.layout.choose_lock_gesture, null);
            mHeaderText = (TextView) view.findViewById(R.id.headerText);
            mLockGestureView = (LockGestureView) view.findViewById(R.id.lockGesture);
            mLockGestureView.setOnGestureListener(mChooseNewLockGestureListener);
            mFooterText = (TextView) view.findViewById(R.id.footerText);

            mFooterLeftButton = (TextView) view.findViewById(R.id.footerLeftButton);
            mFooterRightButton = (TextView) view.findViewById(R.id.footerRightButton);

            mFooterLeftButton.setOnClickListener(this);
            mFooterRightButton.setOnClickListener(this);

            // make it so unhandled touch events within the unlock screen go to the
            // lock gesture view.
            final LinearLayoutWithDefaultTouchRecepient topLayout
                    = (LinearLayoutWithDefaultTouchRecepient) view.findViewById(
                    R.id.topLayout);
            topLayout.setDefaultTouchRecepient(mLockGestureView);

            final boolean confirmCredentials = getActivity().getIntent()
                    .getBooleanExtra("confirm_credentials", false);

            if (savedInstanceState == null) {
                if (confirmCredentials) {
                    // first launch. As a security measure, we're in NeedToConfirm mode until we
                    // know there isn't an existing password or the user confirms their password.
                    updateStage(Stage.NeedToConfirm);
                    boolean launchedConfirmationActivity =
                        mChooseLockSettingsHelper.launchConfirmationActivity(
                                CONFIRM_EXISTING_REQUEST, null, null);
                    if (!launchedConfirmationActivity) {
                        updateStage(Stage.Introduction);
                    }
                } else {
                    updateStage(Stage.Introduction);
                }
            } else {
                // restore from previous state
                final Gesture gesture = savedInstanceState.getParcelable(KEY_GESTURE_CHOICE);
                if (gesture != null) {
                    mChosenGesture = gesture;
                }
                updateStage(Stage.values()[savedInstanceState.getInt(KEY_UI_STAGE)]);
            }
            return view;
        }

        public void onClick(View v) {
            if (v == mFooterLeftButton) {
                if (mUiStage.leftMode == LeftButtonMode.Retry) {
                    mChosenGesture = null;
                    mLockGestureView.clearGesture();
                    updateStage(Stage.Introduction);
                } else if (mUiStage.leftMode == LeftButtonMode.Cancel) {
                    // They are canceling the entire wizard
                    getActivity().setResult(RESULT_FINISHED);
                    getActivity().finish();
                } else {
                    throw new IllegalStateException("left footer button pressed, but stage of " +
                        mUiStage + " doesn't make sense");
                }
            } else if (v == mFooterRightButton) {

                if (mUiStage.rightMode == RightButtonMode.Continue) {
                    if (mUiStage != Stage.FirstChoiceValid) {
                        throw new IllegalStateException("expected ui stage " + Stage.FirstChoiceValid
                                + " when button is " + RightButtonMode.Continue);
                    }
                    updateStage(Stage.NeedToConfirm);
                } else if (mUiStage.rightMode == RightButtonMode.Confirm) {
                    if (mUiStage != Stage.ChoiceConfirmed) {
                        throw new IllegalStateException("expected ui stage " + Stage.ChoiceConfirmed
                                + " when button is " + RightButtonMode.Confirm);
                    }
                    saveChosenGestureAndFinish();
                } else if (mUiStage.rightMode == RightButtonMode.Ok) {
                    if (mUiStage != Stage.HelpScreen) {
                        throw new IllegalStateException("Help screen is only mode with ok button, but " +
                                "stage is " + mUiStage);
                    }
                    mLockGestureView.clearGesture();
                    mLockGestureView.setDisplayMode(DisplayMode.Correct);
                    updateStage(Stage.Introduction);
                }
            }
        }

        public boolean onKeyDown(int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
                if (mUiStage == Stage.HelpScreen) {
                    updateStage(Stage.Introduction);
                    return true;
                }
            }
            if (keyCode == KeyEvent.KEYCODE_MENU && mUiStage == Stage.Introduction) {
                updateStage(Stage.HelpScreen);
                return true;
            }
            return false;
        }

        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);

            outState.putInt(KEY_UI_STAGE, mUiStage.ordinal());
            if (mChosenGesture != null) {
                outState.putParcelable(KEY_GESTURE_CHOICE, mChosenGesture);
            }
        }

        /**
         * Updates the messages and buttons appropriate to what stage the user
         * is at in choosing a view.  This doesn't handle clearing out the gesture;
         * the gesture is expected to be in the right state.
         * @param stage
         */
        protected void updateStage(Stage stage) {
            final Stage previousStage = mUiStage;

            mUiStage = stage;

            // header text, footer text, visibility and
            // enabled state all known from the stage
            mHeaderText.setText(stage.headerMessage);
            if (stage.footerMessage == ID_EMPTY_MESSAGE) {
                mFooterText.setText("");
            } else {
                mFooterText.setText(stage.footerMessage);
            }

            if (stage.leftMode == LeftButtonMode.Gone) {
                mFooterLeftButton.setVisibility(View.GONE);
            } else {
                mFooterLeftButton.setVisibility(View.VISIBLE);
                mFooterLeftButton.setText(stage.leftMode.text);
                mFooterLeftButton.setEnabled(stage.leftMode.enabled);
            }

            mFooterRightButton.setText(stage.rightMode.text);
            mFooterRightButton.setEnabled(stage.rightMode.enabled);

            // same for whether the patten is enabled
            if (stage.gestureEnabled) {
                mLockGestureView.enableInput();
            } else {
                mLockGestureView.disableInput();
            }

            // the rest of the stuff varies enough that it is easier just to handle
            // on a case by case basis.
            mLockGestureView.setDisplayMode(DisplayMode.Correct);

            switch (mUiStage) {
                case Introduction:
                    mLockGestureView.clearGesture();
                    break;
                case HelpScreen:
                    break;
                case FirstChoiceValid:
                    break;
                case NeedToConfirm:
                    mLockGestureView.clearGesture();
                    break;
                case ConfirmWrong:
                    mLockGestureView.setDisplayMode(DisplayMode.Wrong);
                    postClearGestureRunnable();
                    break;
                case ChoiceConfirmed:
                    break;
            }

            // If the stage changed, announce the header for accessibility. This
            // is a no-op when accessibility is disabled.
            if (previousStage != stage) {
                mHeaderText.announceForAccessibility(mHeaderText.getText());
            }
        }


        // clear the wrong gesture unless they have started a new one
        // already
        private void postClearGestureRunnable() {
            mLockGestureView.removeCallbacks(mClearGestureRunnable);
            mLockGestureView.postDelayed(mClearGestureRunnable, WRONG_GESTURE_CLEAR_TIMEOUT_MS);
        }

        private void saveChosenGestureAndFinish() {
            LockPatternUtils utils = mChooseLockSettingsHelper.utils();
            final boolean lockVirgin = !utils.isGestureEverChosen();

            final boolean isFallback = getActivity().getIntent()
                .getBooleanExtra(LockPatternUtils.LOCKSCREEN_BIOMETRIC_WEAK_FALLBACK, false);
            utils.saveLockGesture(mChosenGesture, isFallback);
            utils.setLockGestureEnabled(true);

            if (lockVirgin) {
                utils.setVisibleGestureEnabled(true);
            }

            getActivity().setResult(RESULT_FINISHED);
            getActivity().finish();
        }
    }
}
