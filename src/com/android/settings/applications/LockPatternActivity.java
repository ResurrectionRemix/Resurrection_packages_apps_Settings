/*
 * Copyright (C) 2014 The CyanogenMod Project
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

package com.android.settings.applications;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import android.widget.Toast;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.settings.R;
import com.android.settings.cyanogenmod.ProtectedAccountView;
import com.android.settings.cyanogenmod.ProtectedAccountView.OnNotifyAccountReset;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

public class LockPatternActivity extends Activity implements OnNotifyAccountReset {
    public static final String PATTERN_LOCK_PROTECTED_APPS = "pattern_lock_protected_apps";
    public static final String RECREATE_PATTERN = "recreate_pattern_lock";

    private static final int MIN_PATTERN_SIZE = 4;
    private static final int MAX_PATTERN_RETRY = 5;
    private static final int PATTERN_CLEAR_TIMEOUT_MS = 2000;

    private static final int MENU_RESET = 0;

    LockPatternView mLockPatternView;
    ProtectedAccountView mAccountView;

    TextView mPatternLockHeader;
    MenuItem mItem;
    Button mCancel;
    Button mContinue;
    byte[] mPatternHash;

    int mRetry = 0;

    boolean mCreate;
    boolean mRetryPattern = true;
    boolean mConfirming = false;

    Runnable mCancelPatternRunnable = new Runnable() {
        public void run() {
            mLockPatternView.clearPattern();
            mContinue.setEnabled(false);

            if (mCreate) {
                if (mConfirming) {
                    mPatternLockHeader.setText(getResources()
                            .getString(R.string.lockpattern_need_to_confirm));
                } else {
                    mPatternLockHeader.setText(getResources()
                            .getString(R.string.lockpattern_recording_intro_header));
                    mCancel.setText(getResources().getString(R.string.cancel));
                }
            } else {
                mPatternLockHeader.setText(getResources()
                        .getString(R.string.lockpattern_settings_enable_summary));
            }
        }
    };

    View.OnClickListener mCancelOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mCreate && !mConfirming && !mRetryPattern) {
                // Retry
                mRetryPattern = true;
                resetPatternState(true);
                return;
            }
            setResult(RESULT_CANCELED);
            finish();
        }
    };

    View.OnClickListener mContinueOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Button btn = (Button) v;
            if (mConfirming) {
                SharedPreferences prefs = PreferenceManager
                        .getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(PATTERN_LOCK_PROTECTED_APPS,
                        Base64.encodeToString(mPatternHash, Base64.DEFAULT));
                editor.commit();
                setResult(RESULT_OK);
                finish();
            } else {
                mConfirming = true;
                mCancel.setText(getResources().getString(R.string.cancel));
                mLockPatternView.clearPattern();

                mPatternLockHeader.setText(getResources().getString(
                        R.string.lockpattern_need_to_confirm));
                btn.setText(getResources().getString(R.string.lockpattern_confirm_button_text));
                btn.setEnabled(false);
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        if (!mCreate) {
            menu.add(0, MENU_RESET, 0, R.string.lockpattern_reset_button)
                    .setIcon(R.drawable.ic_lockscreen_ime)
                    .setAlphabeticShortcut('r')
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
                            MenuItem.SHOW_AS_ACTION_WITH_TEXT);
            mItem = menu.findItem(0);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                if (mAccountView.getVisibility() == View.VISIBLE) {
                    switchToPattern(false);
                } else {
                    switchToAccount();
                }
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onNotifyAccountReset() {
        switchToPattern(true);
    }

    private void switchToPattern(boolean reset) {
        if (reset) {
            resetPatternState(false);
        }
        mItem.setIcon(R.drawable.ic_lockscreen_ime);
        mAccountView.clearFocusOnInput();
        mAccountView.setVisibility(View.GONE);
        mLockPatternView.setVisibility(View.VISIBLE);
    }

    private void switchToAccount() {
        mPatternLockHeader.setText(getResources()
                .getString(R.string.lockpattern_settings_reset_summary));
        mItem.setIcon(R.drawable.ic_settings_lockscreen);
        mAccountView.setVisibility(View.VISIBLE);
        mLockPatternView.setVisibility(View.GONE);
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.patternlock);

        mPatternLockHeader = (TextView) findViewById(R.id.pattern_lock_header);
        mCancel = (Button) findViewById(R.id.pattern_lock_btn_cancel);
        mCancel.setOnClickListener(mCancelOnClickListener);
        mContinue = (Button) findViewById(R.id.pattern_lock_btn_continue);
        mContinue.setOnClickListener(mContinueOnClickListener);

        mAccountView = (ProtectedAccountView) findViewById(R.id.lock_account_view);
        mAccountView.setOnNotifyAccountResetCb(this);
        mLockPatternView = (LockPatternView) findViewById(R.id.lock_pattern_view);

        resetPatternState(false);

        //Setup Pattern Lock View
        mLockPatternView.setSaveEnabled(false);
        mLockPatternView.setFocusable(false);
        mLockPatternView.setOnPatternListener(new UnlockPatternListener());

    }

    private void resetPatternState(boolean clear) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String pattern = prefs.getString(PATTERN_LOCK_PROTECTED_APPS, null);
        mCreate = pattern == null || RECREATE_PATTERN.equals(getIntent().getAction())
                || clear;

        mPatternHash = null;
        if (pattern != null) {
            mPatternHash = Base64.decode(pattern, Base64.DEFAULT);
        }

        mContinue.setEnabled(!mCreate);
        mCancel.setVisibility(mCreate ? View.VISIBLE : View.GONE);
        mCancel.setText(getResources().getString(R.string.cancel));
        mContinue.setVisibility(mCreate ? View.VISIBLE : View.GONE);
        mPatternLockHeader.setText(mCreate
                ? getResources().getString(R.string.lockpattern_recording_intro_header)
                : getResources().getString(R.string.lockpattern_settings_enable_summary));
        mLockPatternView.clearPattern();

        invalidateOptionsMenu();
    }

    private class UnlockPatternListener implements LockPatternView.OnPatternListener {

        public void onPatternStart() {
            mLockPatternView.removeCallbacks(mCancelPatternRunnable);

            mPatternLockHeader.setText(getResources().getText(
                    R.string.lockpattern_recording_inprogress));
            mContinue.setEnabled(false);
        }

        public void onPatternCleared() {
        }

        public void onPatternDetected(List<LockPatternView.Cell> pattern) {
            //Check inserted Pattern
            if (mCreate) {
                if (pattern.size() < MIN_PATTERN_SIZE) {
                    mPatternLockHeader.setText(getResources().getString(
                            R.string.lockpattern_recording_incorrect_too_short,
                            LockPatternUtils.MIN_LOCK_PATTERN_SIZE));

                    mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
                    mLockPatternView.postDelayed(mCancelPatternRunnable, PATTERN_CLEAR_TIMEOUT_MS);
                    mCancel.setText(getResources()
                            .getString(R.string.lockpattern_retry_button_text));
                    mRetryPattern = false;
                    return;
                }

                if (mConfirming) {
                    if (Arrays.equals(mPatternHash, patternToHash(pattern))) {
                        mContinue.setText(getResources()
                                .getString(R.string.lockpattern_confirm_button_text));
                        mContinue.setEnabled(true);
                        mPatternLockHeader.setText(getResources().getString(
                                R.string.lockpattern_pattern_confirmed_header));
                    } else {
                        mContinue.setEnabled(false);

                        mPatternLockHeader.setText(getResources().getString(
                                R.string.lockpattern_need_to_unlock_wrong));
                        mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
                        mLockPatternView.postDelayed(mCancelPatternRunnable,
                                PATTERN_CLEAR_TIMEOUT_MS);
                    }
                } else {
                    //Save pattern, user needs to redraw to confirm
                    mCancel.setText(getResources()
                            .getString(R.string.lockpattern_retry_button_text));
                    mRetryPattern = false;

                    mPatternHash = patternToHash(pattern);

                    mPatternLockHeader.setText(getResources().getString(
                            R.string.lockpattern_pattern_entered_header));
                    mContinue.setEnabled(true);
                }
            } else {
                //Check against existing pattern
                if (Arrays.equals(mPatternHash, patternToHash(pattern))) {
                    setResult(RESULT_OK);
                    finish();
                } else {
                    mRetry++;
                    mPatternLockHeader.setText(getResources().getString(
                            R.string.lockpattern_need_to_unlock_wrong));

                    mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
                    mLockPatternView.postDelayed(mCancelPatternRunnable, PATTERN_CLEAR_TIMEOUT_MS);

                    if (mRetry >= MAX_PATTERN_RETRY) {
                        mLockPatternView.removeCallbacks(mCancelPatternRunnable);
                        Toast.makeText(getApplicationContext(),
                                getResources().getString(
                                        R.string.lockpattern_too_many_failed_confirmation_attempts_header),
                                Toast.LENGTH_SHORT).show();
                        switchToAccount();
                    }
                }
            }
        }

        public void onPatternCellAdded(List<LockPatternView.Cell> pattern) {}
    }

    /*
     * Generate an SHA-1 hash for the pattern. Not the most secure, but it is
     * at least a second level of protection. First level is that the file
     * is in a location only readable by the system process.
     * @param pattern the gesture pattern.
     * @return the hash of the pattern in a byte array.
     */
    public byte[] patternToHash(List<LockPatternView.Cell> pattern) {
        if (pattern == null) {
            return null;
        }

        final int patternSize = pattern.size();
        byte[] res = new byte[patternSize];
        for (int i = 0; i < patternSize; i++) {
            LockPatternView.Cell cell = pattern.get(i);
            res[i] = (byte) (cell.getRow() * 3 + cell.getColumn());
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(res);
            return hash;
        } catch (NoSuchAlgorithmException nsa) {
            return res;
        }
    }
}
