/*
 * Copyright (C) 2015 The CyanogenMod Project
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
package com.android.settings;

import android.annotation.Nullable;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.fingerprint.Fingerprint;
import android.os.Bundle;
import android.service.fingerprint.FingerprintManager;
import android.service.fingerprint.FingerprintManagerReceiver;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.settings.cyanogenmod.FingerprintProgressBar;
import com.android.setupwizard.navigationbar.SetupWizardNavBar;

import java.util.List;

public class EnrollFingerprint extends SettingsActivity
        implements SetupWizardNavBar.NavigationBarListener {

    private SetupWizardNavBar mSetupBar;
    private SetupWizardNavBar.NavigationBarListener mFragmentListener;

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, getFragmentClass().getName());
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (EnrollFingerprintFragment.class.getName().equals(fragmentName)) return true;
        return false;
    }

    /* package */ Class<? extends Fragment> getFragmentClass() {
        return EnrollFingerprintFragment.class;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public static Intent createIntent(Context context) {
        Intent intent = new Intent().setClass(context, EnrollFingerprint.class);
        return intent;
    }

    @Override
    public void onNavigationBarCreated(SetupWizardNavBar bar) {
        mSetupBar = bar;
        bar.setUseImmersiveMode(false);
        // hide next button for now
        bar.getNextButton().setVisibility(View.INVISIBLE);
        if (mFragmentListener != null) {
            mFragmentListener.onNavigationBarCreated(bar);
        }
    }

    @Override
    public void onNavigateBack() {
        if (mFragmentListener != null) {
            mFragmentListener.onNavigateBack();
        } else {
            onBackPressed();
        }
    }

    @Override
    public void onNavigateNext() {
        if (mFragmentListener != null) {
            mFragmentListener.onNavigateNext();
        }
    }

    public SetupWizardNavBar getSetupBar() {
        return mSetupBar;
    }

    public void setFragmentBarListener(SetupWizardNavBar.NavigationBarListener listener) {
        mFragmentListener = listener;
    }

    public static class EnrollFingerprintFragment extends Fragment
            implements SetupWizardNavBar.NavigationBarListener {
        private static final int ID_EMPTY_MESSAGE = -1;

        /**
         * Total time the user has to complete enrollment of a fingerprint.
         */
        private static final long TIME_TO_COMPLETE_ENROLLMENT = 10 * DateUtils.MINUTE_IN_MILLIS;

        /**
         * Time before we show the user an error state if an enrollment step is not completed
         * in this time.
         */
        private static final long ENROLLMENT_STEP_TIMEOUT_DURATION =
                10 * DateUtils.SECOND_IN_MILLIS;

        /**
         * Minimum number steps required to show the reminder screen half way through
         */
        private static final int STEPS_REQUIRED_TO_SHOW_HELP = 8;

        /**
         * Value used to indicate # of enrollment steps is currently unknown
         */
        private static final int NUM_ENROLLMENT_STEPS_UNKNOWN = -1;

        private static final int NUM_FAILED_ENROLLMENTS_FOR_FAILURE = 5;

        /**
         * String for formatting the enrollment step progress
         */
        private static final String STEPS_FORMAT_STRING = "%d/%d";

        /**
         * Tag used for error dialogs
         */
        private static final String ERROR_DIALOG_TAG = "error_dialog";

        private TextView mTitle;
        private TextView mStepsCompleted;
        private TextView mInstructions;
        private ProgressBar mProcessingProgress;
        private FingerprintProgressBar mStepProgress;
        private ImageView mInfoGraphic;
        private View mCompletedCheckmark;

        private FingerprintManager mFpM;
        protected Stage mUiStage = Stage.Introduction;
        private int mNumEnrollmentSteps;
        private int mCurrentEnrollmentStep;
        private String[] mSuccessfulScanTitles;
        private String[] mErrorTitles;
        private int mScanSuccessTextIndex = 0;
        private int mConsecutiveErrorCount = 0;

        /**
         * The states of the left footer button.
         */
        enum BackButtonMode {
            Cancel(R.string.cancel, true),
            AddAnother(R.string.fingerprint_enrollment_add_another_label, true),
            Back(ID_EMPTY_MESSAGE, true),
            Gone(ID_EMPTY_MESSAGE, false);


            /**
             * @param text The displayed text for this mode.
             * @param enabled Whether the button should be enabled.
             */
            BackButtonMode(int text, boolean enabled) {
                this.text = text;
                this.enabled = enabled;
            }

            final int text;
            final boolean enabled;
        }

        /**
         * The states of the right button.
         */
        enum NextButtonMode {
            FindSensor(R.string.fingerprint_enrollment_found_it_label, true),
            Done(R.string.fingerprint_enrollment_done_label, true),
            Ok(android.R.string.ok, true),
            Gone(ID_EMPTY_MESSAGE, false);


            /**
             * @param text The displayed text for this mode.
             * @param enabled Whether the button should be enabled.
             */
            NextButtonMode(int text, boolean enabled) {
                this.text = text;
                this.enabled = enabled;
            }

            final int text;
            final boolean enabled;
        }

        /**
         * Keep track internally of where the user is in enrolling a fingerprint.
         */
        protected enum Stage {
            Introduction(
                    R.string.fingerprint_enrollment_introduction_title,
                    R.string.fingerprint_enrollment_introduction_instructions,
                    BackButtonMode.Gone, NextButtonMode.FindSensor,
                    ID_EMPTY_MESSAGE, R.drawable.front_back_on_grey),
            HelpScreen(
                    R.string.fingerprint_enrollment_break_title,
                    R.string.fingerprint_enrollment_break_instructions,
                    BackButtonMode.Gone, NextButtonMode.Gone,
                    ID_EMPTY_MESSAGE, R.drawable.front_back_on_grey),
            WrongSensor(
                    R.string.fingerprint_enrollment_error_title_wrong_sensor,
                    R.string.fingerprint_enrollment_error_message,
                    BackButtonMode.Gone, NextButtonMode.Gone,
                    ID_EMPTY_MESSAGE, 0),
            EnrollmentStep(
                    R.string.fingerprint_enrollment_scan_title,
                    R.string.fingerprint_enrollment_scan_instructions,
                    BackButtonMode.Gone, NextButtonMode.Gone,
                    ID_EMPTY_MESSAGE, 0),
            EnrollmentError(
                    R.string.fingerprint_enrollment_error_title_1,
                    R.string.fingerprint_enrollment_error_message,
                    BackButtonMode.Gone, NextButtonMode.Gone,
                    ID_EMPTY_MESSAGE, 0),
            EnrollmentFinished(
                    R.string.fingerprint_enrollment_finished_title,
                    R.string.fingerprint_enrollment_finished_message,
                    BackButtonMode.AddAnother, NextButtonMode.Done,
                    ID_EMPTY_MESSAGE, 0);

            /**
             * @param titleMessage The message displayed at the top.
             * @param backMode The mode of the left button.
             * @param nextMode The mode of the right button.
             * @param footerMessage The footer message.
             * @param infoGraphic Resource to use for the info graphic
             */
            Stage(int titleMessage,
                    int instructionMessage,
                    BackButtonMode backMode,
                    NextButtonMode nextMode,
                    int footerMessage, int infoGraphic) {
                this.titleMessage = titleMessage;
                this.instructionMessage = instructionMessage;
                this.backMode = backMode;
                this.nextMode = nextMode;
                this.footerMessage = footerMessage;
                this.infoGraphic = infoGraphic;
            }

            final int titleMessage;
            final int instructionMessage;
            final BackButtonMode backMode;
            final NextButtonMode nextMode;
            final int footerMessage;
            final int infoGraphic;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (!(getActivity() instanceof EnrollFingerprint)) {
                throw new SecurityException("Fragment contained in wrong activity");
            }
            getEnrollmentActivity().setFragmentBarListener(this);
            mFpM = (FingerprintManager) getActivity().getSystemService(FINGERPRINT_SERVICE);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fingerprint_enrollment, container, false);
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            mTitle = (TextView) view.findViewById(R.id.title);
            mStepsCompleted = (TextView) view.findViewById(R.id.steps);
            mInstructions = (TextView) view.findViewById(R.id.instructions);
            mInfoGraphic = (ImageView) view.findViewById(R.id.infographic);
            mProcessingProgress = (ProgressBar) view.findViewById(R.id.processing_progress);
            mStepProgress = (FingerprintProgressBar) view.findViewById(R.id.enrollment_progress);
            mCompletedCheckmark = view.findViewById(R.id.completed_checkmark);
            mNumEnrollmentSteps = -1;
            mCurrentEnrollmentStep = 0;
            mSuccessfulScanTitles =
                    getResources().getStringArray(R.array.fingerprint_successful_scan_titles);
            mErrorTitles =
                    getResources().getStringArray(R.array.fingerprint_enrollment_error_titles);
            updateStage(Stage.Introduction);
        }

        @Override
        public void onDestroy() {
            if (mCurrentEnrollmentStep != mNumEnrollmentSteps) {
                mFpM.cancel();
            }
            mFpM.stopListening();
            super.onDestroy();
        }

        @Override
        public void onNavigationBarCreated(SetupWizardNavBar bar) {
        }

        @Override
        public void onNavigateBack() {
            switch (mUiStage) {
                case EnrollmentStep:
                    break;
                case EnrollmentError:
                    break;
                case EnrollmentFinished:
                    updateStage(Stage.Introduction);
                    break;
                case Introduction:
                default:
                    getActivity().onBackPressed();
            }
        }

        @Override
        public void onNavigateNext() {
            switch (mUiStage) {
                case Introduction:
                    updateStage(Stage.EnrollmentStep);
                    break;
                case EnrollmentStep:
                    break;
                case EnrollmentError:
                    break;
                case EnrollmentFinished:
                    getActivity().onBackPressed();
                    break;
                case HelpScreen:
                    break;
            }
        }

        /**
         * Updates the messages and buttons appropriate to what stage the user
         * is at in enrolling a fingerprint.
         * @param stage
         */
        protected void updateStage(Stage stage) {
            final Stage previousStage = mUiStage;

            mUiStage = stage;

            mTitle.setText(stage.titleMessage);
            mInstructions.setText(stage.instructionMessage);
            mInfoGraphic.setImageResource(stage.infoGraphic);
            final SetupWizardNavBar setupBar = getEnrollmentActivity().getSetupBar();

            if (stage.backMode.enabled) {
                final Button backButton = setupBar.getBackButton();
                backButton.setVisibility(View.VISIBLE);
                backButton.setText(stage.backMode.text);
            } else {
                setupBar.getBackButton().setVisibility(View.INVISIBLE);
            }

            if (stage.nextMode.enabled) {
                final Button nextButton = setupBar.getNextButton();
                nextButton.setVisibility(View.VISIBLE);
                nextButton.setText(stage.nextMode.text);
            } else {
                setupBar.getNextButton().setVisibility(View.INVISIBLE);
            }

            if (mUiStage == Stage.EnrollmentStep || mUiStage == Stage.EnrollmentError) {
                mStepProgress.setVisibility(View.VISIBLE);
            } else {
                mStepProgress.setVisibility(View.GONE);
            }

            switch (mUiStage) {
                case Introduction:
                    mCompletedCheckmark.setVisibility(View.GONE);
                    mStepsCompleted.setVisibility(View.GONE);
                    mStepProgress.setProgress(0);
                    mStepProgress.showError(false);
                    mCurrentEnrollmentStep = 0;
                    mNumEnrollmentSteps = NUM_ENROLLMENT_STEPS_UNKNOWN;
                    break;
                case HelpScreen:
                    mStepsCompleted.setText(
                            String.format(STEPS_FORMAT_STRING, mCurrentEnrollmentStep,
                                    mNumEnrollmentSteps));
                    break;
                case WrongSensor:
                    // show fingerprint progress
                    mStepProgress.setVisibility(View.VISIBLE);
                    break;
                case EnrollmentStep:
                    float stepPercentage = mNumEnrollmentSteps == NUM_ENROLLMENT_STEPS_UNKNOWN ? 0
                            : 100.0f * mCurrentEnrollmentStep / mNumEnrollmentSteps;
                    mStepProgress.setProgressAnimated((int)(stepPercentage));
                    if (mCurrentEnrollmentStep >= 1) {
                        // If the last step was an error, set back to success color in progress
                        if (previousStage == Stage.EnrollmentError) {
                            mStepProgress.showError(false);
                            mConsecutiveErrorCount = 0;
                        }
                        mStepsCompleted.setVisibility(View.VISIBLE);
                        mStepsCompleted.setText(
                                String.format(STEPS_FORMAT_STRING, mCurrentEnrollmentStep,
                                        mNumEnrollmentSteps));
                        if (mCurrentEnrollmentStep > 1
                                && mCurrentEnrollmentStep < mNumEnrollmentSteps - 1) {
                            int titleIdx = mScanSuccessTextIndex++ % mSuccessfulScanTitles.length;
                            mTitle.setText(mSuccessfulScanTitles[titleIdx]);
                        }
                    }
                    // We need to start the enrollment process if previous stage was Introduction
                    if (previousStage == Stage.Introduction) {
                        mFpM.startListening(mFingerprintReceiver);
                        mFpM.enroll(TIME_TO_COMPLETE_ENROLLMENT);
                        mProcessingProgress.setVisibility(View.VISIBLE);
                        mStepProgress.setOnTouchListener(mWrongSensorTouchListener);
                        startEnrollmentStepTimeout();
                    } else if (mCurrentEnrollmentStep == 1) {
                        mStepProgress.setOnTouchListener(null);
                        mTitle.setText(R.string.fingerprint_successful_scan_title_first);
                    } else if (mCurrentEnrollmentStep == mNumEnrollmentSteps - 1) {
                        // about to perform last enrollment step, set title to final scan text
                        mTitle.setText(R.string.fingerprint_successful_scan_final);
                    }
                    break;
                case EnrollmentError:
                    mStepProgress.showError(true);
                    mTitle.setText(mErrorTitles[mConsecutiveErrorCount % mErrorTitles.length]);
                    mConsecutiveErrorCount++;
                    if (mConsecutiveErrorCount >= NUM_FAILED_ENROLLMENTS_FOR_FAILURE) {
                        mFpM.cancel();
                        mFpM.stopListening();
                        cancelEnrollmentStepTimeout();
                        showFailedEnrollmentDialog();
                    }
                    break;
                case EnrollmentFinished:
                    mStepProgress.setVisibility(View.VISIBLE);
                    mStepProgress.setProgress(100);
                    mCompletedCheckmark.setVisibility(View.VISIBLE);
                    mStepsCompleted.setText(R.string.fingerprint_enrollment_done_label);
                    mProcessingProgress.setVisibility(View.GONE);

                    // Hide add another if at max number of enrolled fingerprints
                    List<Fingerprint> enrolled = mFpM.getEnrolledFingerprints();
                    if (enrolled != null &&
                            ManageFingerprints.FingerprintListFragment.MAX_NUM_FINGERPRINTS ==
                            enrolled.size()) {
                        setupBar.getBackButton().setVisibility(View.INVISIBLE);
                    }
                    break;
            }
        }

        protected EnrollFingerprint getEnrollmentActivity() {
            return (EnrollFingerprint) getActivity();
        }

        private void showWrongSensorDialog() {
            DialogFragment dialogFragment = WrongSensorDialogFragment.newInstance();
            dialogFragment.show(getChildFragmentManager(), ERROR_DIALOG_TAG);
        }

        private void showFailedEnrollmentDialog() {
            DialogFragment dialogFragment = FailedEnrollmentDialogFragment.newInstance(this);
            dialogFragment.show(getChildFragmentManager(), ERROR_DIALOG_TAG);
        }

        private void dismissErrorDialogIfShowing() {
            Fragment prev = getChildFragmentManager().findFragmentByTag(ERROR_DIALOG_TAG);
            if (prev instanceof DialogFragment) {
                ((DialogFragment) prev).dismiss();
            }
        }

        private View.OnTouchListener mWrongSensorTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    updateStage(Stage.WrongSensor);
                    showWrongSensorDialog();
                }
                return false;
            }
        };

        private Runnable mEnrollmentStepTimeoutRunnable = new Runnable() {
            @Override
            public void run() {
                startEnrollmentStepTimeout();
                updateStage(Stage.EnrollmentError);
            }
        };

        private void cancelEnrollmentStepTimeout() {
            mTitle.removeCallbacks(mEnrollmentStepTimeoutRunnable);
        }

        private void startEnrollmentStepTimeout() {
            cancelEnrollmentStepTimeout();
            mTitle.postDelayed(mEnrollmentStepTimeoutRunnable, ENROLLMENT_STEP_TIMEOUT_DURATION);
        }

        private FingerprintManagerReceiver mFingerprintReceiver = new FingerprintManagerReceiver() {
            @Override
            public void onEnrollResult(int fingerprintId, int remaining) {
                super.onEnrollResult(fingerprintId, remaining);
                // If mNumEnrollmentSteps is unknown, this is the first time this callback has been
                // called so we need to set mNumEnrollmentSteps to the remaining steps plus one
                if (mNumEnrollmentSteps == NUM_ENROLLMENT_STEPS_UNKNOWN) {
                    mNumEnrollmentSteps = remaining + 1;
                }
                mCurrentEnrollmentStep = mNumEnrollmentSteps - remaining;
                if (remaining == 0) {
                    mCurrentEnrollmentStep = mNumEnrollmentSteps;
                    updateStage(Stage.EnrollmentFinished);
                } else {
                    startEnrollmentStepTimeout();
                    if (mUiStage == Stage.EnrollmentError || mUiStage == Stage.WrongSensor) {
                        dismissErrorDialogIfShowing();
                    }
                    if (mNumEnrollmentSteps >= STEPS_REQUIRED_TO_SHOW_HELP &&
                            mCurrentEnrollmentStep == mNumEnrollmentSteps / 2) {
                        updateStage(Stage.HelpScreen);
                    } else {
                        updateStage(Stage.EnrollmentStep);
                    }
                }
            }

            @Override
            public void onError(int error) {
                super.onError(error);
                if (error == FingerprintManager.FINGERPRINT_ERROR) {
                    updateStage(Stage.EnrollmentError);
                } else if (error == FingerprintManager.FINGERPRINT_ERROR_TIMEOUT) {
                    mFpM.cancel();
                    mFpM.stopListening();
                    showFailedEnrollmentDialog();
                    updateStage(Stage.EnrollmentError);
                }
            }
        };

        public static class WrongSensorDialogFragment extends DialogFragment {
            static WrongSensorDialogFragment newInstance() {
                return new WrongSensorDialogFragment();
            }

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                AlertDialog dialog = new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.fingerprint_dialog_title_wrong_sensor)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dismiss();
                            }
                        })
                        .setMessage(R.string.fingerprint_dialog_msg_wrong_sensor)
                        .create();
                return dialog;
            }
        }

        public static class FailedEnrollmentDialogFragment extends DialogFragment {
            private EnrollFingerprintFragment mFragment;
            static FailedEnrollmentDialogFragment newInstance(EnrollFingerprintFragment fragment) {
                return new FailedEnrollmentDialogFragment(fragment);
            }

            @SuppressLint("ValidFragment")
            public FailedEnrollmentDialogFragment(EnrollFingerprintFragment fragment) {
                super();
                mFragment = fragment;
            }

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                AlertDialog dialog = new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.fingerprint_dialog_title_failed_enrollment)
                        .setPositiveButton(R.string.fingerprint_enrollment_restart_label,
                                new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dismiss();
                            }
                        })
                        .setMessage(R.string.fingerprint_dialog_msg_failed_enrollment)
                        .create();
                return dialog;
            }

            @Override
            public void onDismiss(DialogInterface dialog) {
                super.onDismiss(dialog);
                if (mFragment != null) {
                    mFragment.updateStage(Stage.Introduction);
                }
            }
        }
    }
}
