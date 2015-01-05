package com.android.settings.dashboard;

import android.content.Context;
import android.widget.CompoundButton;
import android.widget.Switch;
import com.android.settings.widget.SwitchBar;

public abstract class GenericSwitchToggle implements SwitchBar.OnSwitchChangeListener,
        CompoundButton.OnCheckedChangeListener {

    protected Context mContext;
    protected Switch mSwitch;
    protected SwitchBar mSwitchBar;

    protected boolean mStateMachineEvent;
    protected boolean mListeningToOnSwitchChange = false;

    public GenericSwitchToggle(Context context, Switch switch_) {
        mContext = context;
        mSwitch = switch_;
    }

    public GenericSwitchToggle(Context context, SwitchBar switch_) {
        mContext = context;
        mSwitchBar = switch_;
    }

    public void pause() {
        if (mListeningToOnSwitchChange) {
            if (mSwitchBar != null) {
                mSwitchBar.removeOnSwitchChangeListener(this);
            }
            if (mSwitch != null) {
                mSwitch.setOnCheckedChangeListener(null);
            }
            mListeningToOnSwitchChange = false;
        }
    }

    public void resume(Context context) {
        mContext = context;

        if (!mListeningToOnSwitchChange) {
            if (mSwitchBar != null) {
                mSwitchBar.addOnSwitchChangeListener(this);
                mListeningToOnSwitchChange = true;
            }
            if (mSwitch != null) {
                mSwitch.setOnCheckedChangeListener(this);
                mListeningToOnSwitchChange = true;
            }
        }
    }

    public void teardownSwitchBar() {
        if (mSwitchBar == null) {
            return;
        }
        if (mListeningToOnSwitchChange) {
            mSwitchBar.removeOnSwitchChangeListener(this);
            mListeningToOnSwitchChange = false;
        }
        mSwitchBar.hide();
    }

    protected void setChecked(boolean checked) {
        mStateMachineEvent = true;
        if (mSwitchBar != null) {
            mSwitchBar.setChecked(checked);
        }
        if (mSwitch != null) {
            mSwitch.setChecked(checked);
        }
        mStateMachineEvent = false;
    }

    protected void setEnabled(boolean enabled) {
        if (mSwitchBar != null) {
            mSwitchBar.setEnabled(enabled);
        }
        if (mSwitch != null) {
            mSwitch.setEnabled(enabled);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        onSwitchChanged(mSwitch, isChecked);
    }

    public abstract void onSwitchChanged(Switch switchView, boolean isChecked);
}
