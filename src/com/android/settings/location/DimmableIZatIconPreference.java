/* Copyright (c) 2016, The Linux Foundation. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.
    * Neither the name of The Linux Foundation nor the names of its
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package com.android.settings.location;

import android.annotation.Nullable;
import android.content.Context;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.util.Log;
import com.android.settings.DimmableIconPreference;
import com.qti.izat.XTProxy;
import com.qti.izat.IXTSrvCb;

public class DimmableIZatIconPreference extends DimmableIconPreference {
    private static final String TAG = "DimmableIZatIconPreference";
    boolean mChecked;
    private XTProxy mXT;

    public static DimmableIconPreference newInstance(Context context,
            @Nullable CharSequence contentDescription, InjectedSetting info) {
        if (XTProxy.IZAT_XT_PACKAGE.equals(info.packageName)) {
            return new DimmableIZatIconPreference(context, null,
                           com.android.internal.R.attr.checkBoxPreferenceStyle);
        } else {
            return new DimmableIconPreference(context, contentDescription);
        }
    }

    public DimmableIZatIconPreference(Context context, AttributeSet attrs,
                                          int defStyleAttr) {
        super(context, attrs, defStyleAttr, null);

        mXT = XTProxy.getXTProxy(context, new IXTSrvCb.Stub() {
            @Override
            public void userConsentNotify(boolean consent) {
                if (mChecked != mXT.getUserConsent()) {
                    mChecked = !mChecked;
                    dimIcon(!isEnabled() || !mChecked);
                }
            }
        });
        mChecked = mXT.getUserConsent();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        dimIcon(!isEnabled() || !mChecked);
    }
}
