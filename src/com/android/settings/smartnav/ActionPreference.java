/*
 * Copyright (C) 2015 TeamEos project
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
 *
 * Simple preference class implementing ActionHolder interface to assign
 * actions to buttons. It is ABSOLUTELY IMPERITIVE that the preference
 * key is identical to the target ConfigMap tag in ActionConstants 
 */

package com.android.settings.smartnav;

import java.util.Map;

import com.android.internal.utils.ActionConstants.ConfigMap;
import com.android.internal.utils.ActionConstants.Defaults;
import com.android.internal.utils.ActionHolder;
import com.android.internal.utils.Config.ActionConfig;
import com.android.internal.utils.Config.ButtonConfig;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.util.AttributeSet;

public class ActionPreference extends Preference implements ActionHolder {
    private Defaults mDefaults;
    private ConfigMap mMap;
    private ActionConfig mAction;
    private ActionConfig mDefaultAction;

    public ActionPreference(Context context) {
        this(context, null);
    }

    public ActionPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ActionPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ActionPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs);
    }

    @Override
    public String getTag() {
        return this.getKey();
    }

    @Override
    public void setTag(String tag) {
        this.setKey(tag);
    }

    @Override
    public Defaults getDefaults() {
        return mDefaults;
    }

    @Override
    public void setDefaults(Defaults defaults) {
        mDefaults = defaults;
        final String tag = this.getKey();
        for (Map.Entry<String, ConfigMap> entry : defaults.getActionMap().entrySet()) {
            if (((String) entry.getKey()).equals(tag)) {
                mMap = entry.getValue();
                break;
            }
        }
    }

    @Override
    public ConfigMap getConfigMap() {
        return mMap;
    }

    @Override
    public void setConfigMap(ConfigMap map) {
        mMap = map;
    }

    @Override
    public ButtonConfig getButtonConfig() {
        return null;
    }

    @Override
    public void setButtonConfig(ButtonConfig button) {
    }

    @Override
    public ActionConfig getActionConfig() {
        return mAction;
    }

    @Override
    public void setActionConfig(ActionConfig action) {
        mAction = action;
        this.setSummary(action.getLabel());
    }

    @Override
    public ButtonConfig getDefaultButtonConfig() {
        return null;
    }

    @Override
    public void setDefaultButtonConfig(ButtonConfig button) {

    }

    @Override
    public ActionConfig getDefaultActionConfig() {
        return mDefaultAction;
    }

    @Override
    public void setDefaultActionConfig(ActionConfig action) {
        mDefaultAction = action;
    }
}
