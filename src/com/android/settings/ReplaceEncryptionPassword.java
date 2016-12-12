/*
 * Copyright (C) 2016 The CyanogenMod Project
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

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternUtils.RequestThrottledException;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.os.UserHandle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ReplaceEncryptionPassword extends SettingsActivity {
    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, getFragmentClass().getName());
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (ReplaceEncryptionPasswordFragment.class.getName().equals(fragmentName)) return true;
        return false;
    }

    /* package */ Class<? extends Fragment> getFragmentClass() {
        return ReplaceEncryptionPasswordFragment.class;
    }

    public static class ReplaceEncryptionPasswordFragment extends Fragment {
        private static final int KEYGUARD_REQUEST = 55;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (!(getActivity() instanceof ReplaceEncryptionPassword)) {
                throw new SecurityException("Fragment contained in wrong activity");
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
            Resources res = getActivity().getResources();
            ChooseLockSettingsHelper helper = new ChooseLockSettingsHelper(getActivity(), this);

            helper.launchConfirmationActivity(KEYGUARD_REQUEST,
                    res.getText(R.string.unlock_set_unlock_password_title),
                    true);

            return null;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            if (requestCode != KEYGUARD_REQUEST) {
                return;
            }

            // If the user entered a valid keyguard trace, present the final
            // confirmation prompt; otherwise, go back to the initial state.
            if (resultCode == Activity.RESULT_OK && data != null) {
                LockPatternUtils utils = new LockPatternUtils(getActivity());
                int type = data.getIntExtra(ChooseLockSettingsHelper.EXTRA_KEY_TYPE, -1);
                String password = data.getStringExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD);
                if (type == StorageManager.CRYPT_TYPE_PATTERN) {
                    byte size = data.getByteExtra("pattern_size",
                            LockPatternUtils.PATTERN_SIZE_DEFAULT);
                    utils.replaceSeparateEncryptionPasswordWithPattern(
                            utils.stringToPattern(password, size), size);
                } else {
                    utils.replaceSeparateEncryptionPassword(password);
                }
            }

            getActivity().finish();
        }
    }
}
