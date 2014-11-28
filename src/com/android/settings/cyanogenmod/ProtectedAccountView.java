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

package com.android.settings.cyanogenmod;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.text.InputFilter;
import android.text.LoginFilter;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.applications.LockPatternActivity;

import java.io.IOException;

/**
 * When the user forgets their password a bunch of times, we fall back on their
 * account's login/password to unlock protected apps (and reset their lock pattern).
 */
public class ProtectedAccountView extends LinearLayout implements View.OnClickListener {

    public static interface OnNotifyAccountReset {
        void onNotifyAccountReset();
    }

    private EditText mLogin;
    private EditText mPassword;
    private Button mOk;
    private Context mContext;
    private LockPatternUtils mLockPatternUtils;
    private OnNotifyAccountReset mNotifyAccountResetCb;

    /**
     * Shown while making asynchronous check of password.
     */
    private ProgressDialog mCheckingDialog;

    public ProtectedAccountView(Context context) {
        this(context, null);
    }

    public ProtectedAccountView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ProtectedAccountView(Context context, AttributeSet st, int ds) {
        super(context, st, ds);
        mContext = context;
        mLockPatternUtils = new LockPatternUtils(mContext);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mLogin = (EditText) findViewById(R.id.login);
        mLogin.setFilters(new InputFilter[] { new LoginFilter.UsernameFilterGeneric() } );
        mPassword = (EditText) findViewById(R.id.password);

        mOk = (Button) findViewById(R.id.ok);
        mOk.setOnClickListener(this);

        reset();
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction,
            Rect previouslyFocusedRect) {
        // send focus to the login field
        return mLogin.requestFocus(direction, previouslyFocusedRect);
    }

    public boolean needsInput() {
        return true;
    }

    public void setOnNotifyAccountResetCb(OnNotifyAccountReset callback) {
        this.mNotifyAccountResetCb = callback;
    }

    public void clearFocusOnInput() {
        mLogin.clearFocus();
        mPassword.clearFocus();

        // hide keyboard
        final InputMethodManager imm = (InputMethodManager)
                mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mLogin.getWindowToken(), 0);
        imm.hideSoftInputFromWindow(mPassword.getWindowToken(), 0);
    }

    public void reset() {
        mLogin.setText("");
        mPassword.setText("");
        mLogin.requestFocus();
    }

    /** {@inheritDoc} */
    public void cleanUp() {
        if (mCheckingDialog != null) {
            mCheckingDialog.hide();
        }
    }

    public void onClick(View v) {
        if (v == mOk) {
            asyncCheckPassword();
        }
    }

    private void postOnCheckPasswordResult(final boolean success) {
        // ensure this runs on UI thread
        mLogin.post(new Runnable() {
            public void run() {
                if (success) {

                    Activity baseActivity = (Activity) mContext;

                    if (!baseActivity.isFinishing()) {
                        // Remove pattern
                        SharedPreferences prefs = PreferenceManager
                                .getDefaultSharedPreferences(mContext);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.remove(LockPatternActivity.PATTERN_LOCK_PROTECTED_APPS);
                        editor.commit();

                        if (mNotifyAccountResetCb != null) {
                            mNotifyAccountResetCb.onNotifyAccountReset();
                        } else {
                            baseActivity.setResult(Activity.RESULT_OK);
                            baseActivity.finish();
                        }
                    }
                } else {
                    Toast.makeText(mContext,
                            getResources().getString(
                                    R.string.pa_login_incorrect_login),
                            Toast.LENGTH_SHORT).show();
                    mPassword.setText("");
                }
            }
        });
    }

    /**
     * Given the string the user entered in the 'username' field, find
     * the stored account that they probably intended.  Prefer, in order:
     *
     *   - an exact match for what was typed, or
     *   - a case-insensitive match for what was typed, or
     *   - if they didn't include a domain, an exact match of the username, or
     *   - if they didn't include a domain, a case-insensitive
     *     match of the username.
     *
     * If there is a tie for the best match, choose neither --
     * the user needs to be more specific.
     *
     * @return an account name from the database, or null if we can't
     * find a single best match.
     */
    private Account findIntendedAccount(String username) {
        Account[] accounts = AccountManager.get(mContext).getAccountsByTypeAsUser("com.google",
                new UserHandle(mLockPatternUtils.getCurrentUser()));

        // Try to figure out which account they meant if they
        // typed only the username (and not the domain), or got
        // the case wrong.

        Account bestAccount = null;
        int bestScore = 0;
        for (Account a: accounts) {
            int score = 0;
            if (username.equals(a.name)) {
                score = 4;
            } else if (username.equalsIgnoreCase(a.name)) {
                score = 3;
            } else if (username.indexOf('@') < 0) {
                int i = a.name.indexOf('@');
                if (i >= 0) {
                    String aUsername = a.name.substring(0, i);
                    if (username.equals(aUsername)) {
                        score = 2;
                    } else if (username.equalsIgnoreCase(aUsername)) {
                        score = 1;
                    }
                }
            }
            if (score > bestScore) {
                bestAccount = a;
                bestScore = score;
            } else if (score == bestScore) {
                bestAccount = null;
            }
        }
        return bestAccount;
    }

    private void asyncCheckPassword() {
        final String login = mLogin.getText().toString();
        final String password = mPassword.getText().toString();
        Account account = findIntendedAccount(login);
        if (account == null) {
            postOnCheckPasswordResult(false);
            return;
        }
        getProgressDialog().show();
        Bundle options = new Bundle();
        options.putString(AccountManager.KEY_PASSWORD, password);
        AccountManager.get(mContext).confirmCredentialsAsUser(account, options, null /* activity */,
                new AccountManagerCallback<Bundle>() {
            public void run(AccountManagerFuture<Bundle> future) {
                try {
                    final Bundle result = future.getResult();
                    final boolean verified = result.getBoolean(AccountManager.KEY_BOOLEAN_RESULT);
                    postOnCheckPasswordResult(verified);
                } catch (OperationCanceledException e) {
                    postOnCheckPasswordResult(false);
                } catch (IOException e) {
                    postOnCheckPasswordResult(false);
                } catch (AuthenticatorException e) {
                    postOnCheckPasswordResult(false);
                } finally {
                    mLogin.post(new Runnable() {
                        public void run() {
                            getProgressDialog().hide();
                        }
                    });
                }
            }
        }, null /* handler */, new UserHandle(mLockPatternUtils.getCurrentUser()));
    }

    private Dialog getProgressDialog() {
        if (mCheckingDialog == null) {
            mCheckingDialog = new ProgressDialog(mContext);
            mCheckingDialog.setMessage(
                    mContext.getString(R.string.pa_login_checking_password));
            mCheckingDialog.setIndeterminate(true);
            mCheckingDialog.setCancelable(false);
            mCheckingDialog.getWindow().setType(
                    WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        }
        return mCheckingDialog;
    }
}

