/**
 * Copyright (c) 2015, The CyanogenMod Project
 * Copyright (c) 2020, RR
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.rr.utils;

import android.annotation.SdkConstant;

/**
 * @hide
 * TODO: We need to somehow make these managers accessible via getSystemService
 */
public final class RRContextConstants {

    /**
     * @hide
     */
    private RRContextConstants() {
        // Empty constructor
    }

    /**
     * Features supported by RR
     */
    public static class Features {
        /**
         * Feature for {@link PackageManager#getSystemAvailableFeatures} and
         * {@link PackageManager#hasSystemFeature}: The device includes the Lineage
         * in-screen fingerprint.
         */
        @SdkConstant(SdkConstant.SdkConstantType.FEATURE)
        public static final String FOD = "vendor.lineage.biometrics.fingerprint.inscreen";
    }
}
