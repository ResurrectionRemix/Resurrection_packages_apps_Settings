/*
 * Copyright (C) 2010 The Android Open Source Project
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
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceGroupAdapter;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for Settings fragments, with some helper functions and dialog management.
 */
public class SettingsPreferenceFragment extends PreferenceFragment implements DialogCreatable {

    private static final String TAG = "SettingsPreferenceFragment";

    private static final int MENU_HELP = Menu.FIRST + 100;
    private static final int DELAY_HIGHLIGHT_DURATION_MILLIS = 600;

    private static final String SAVE_HIGHLIGHTED_KEY = "android:preference_highlighted";
    private static final String SAVE_HIGHLIGHTED_KEY_KEY = "android:preference_highlighted_key";
    public static final String NODE_PREFERENCE_CATEGORY = "PreferenceCategory";

    private SettingsDialogFragment mDialogFragment;

    private String mHelpUrl;

    protected Context mContext;

    protected ContentResolver mContentRes;

    // Cache the content resolver for async callbacks
    private ContentResolver mContentResolver;

    private String mPreferenceKey;
    private boolean mPreferenceHighlighted = false;
    private Drawable mHighlightDrawable;

    private ListAdapter mCurrentRootAdapter;
    private boolean mIsDataSetObserverRegistered = false;
    private DataSetObserver mDataSetObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            highlightPreferenceIfNeeded();
        }

        @Override
        public void onInvalidated() {
            highlightPreferenceIfNeeded();
        }
    };

    private ViewGroup mPinnedHeaderFrameLayout;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mContext = getActivity().getApplicationContext();

        mContentRes = getActivity().getContentResolver();

        // Prepare help url and enable menu if necessary
        int helpResource = getHelpResource();
        if (helpResource != 0) {
            mHelpUrl = getResources().getString(helpResource);
        }

        final Bundle args = getArguments();
        if (args != null) {
            mPreferenceKey = args.getString(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY);
        }

        if (icicle != null) {
            if (icicle.getString(SAVE_HIGHLIGHTED_KEY_KEY, "").equals(mPreferenceKey)) {
                // highlighting same thing as before, restore
                mPreferenceHighlighted = icicle.getBoolean(SAVE_HIGHLIGHTED_KEY);
            } else {
                // highlighting a different preference
                mPreferenceHighlighted = false;
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);
        mPinnedHeaderFrameLayout = (ViewGroup) root.findViewById(R.id.pinned_header);
        return root;
    }

    public void setPinnedHeaderView(View pinnedHeader) {
        mPinnedHeaderFrameLayout.addView(pinnedHeader);
        mPinnedHeaderFrameLayout.setVisibility(View.VISIBLE);
    }

    public void clearPinnedHeaderView() {
        mPinnedHeaderFrameLayout.removeAllViews();
        mPinnedHeaderFrameLayout.setVisibility(View.GONE);
    }

    @Override
    public void addPreferencesFromResource(int preferencesResId) {
        super.addPreferencesFromResource(preferencesResId);
        maybeStripAdvancedPreferences(preferencesResId);
    }

    private void maybeStripAdvancedPreferences(int preferenceResId) {
        List<PreferenceNodeInfo> prefsToRemove = findKeysToRemove(preferenceResId);
        for (PreferenceNodeInfo nodeInfo : prefsToRemove) {
            PreferenceGroup c = (PreferenceGroup) findPreference(nodeInfo.category);
            if (c != null) {
                Preference p = c.findPreference(nodeInfo.key);
                if (p != null) {
                    c.removePreference(p);
                }
                if (c.getPreferenceCount() == 0) {
                    getPreferenceScreen().removePreference(c);
                }
            }
        }
    }

    private List<PreferenceNodeInfo> findKeysToRemove(int xmlResId) {
        final boolean advancedModeEnabled = SettingsActivity.showAdvancedPreferences(getActivity());
        List<PreferenceNodeInfo> preferencesToRemove = new ArrayList<>();
        XmlResourceParser parser = null;
        try {
            parser = getResources().getXml(xmlResId);

            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && type != XmlPullParser.START_TAG) {
                // Parse next until start tag is found
            }

            String key, lastCategoryKey = null;
            Boolean advanced;
            final int preferenceScreenDepth = parser.getDepth();
            final AttributeSet attrs = Xml.asAttributeSet(parser);

            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && (type != XmlPullParser.END_TAG || parser.getDepth()
                    > preferenceScreenDepth)) {
                if (type == XmlPullParser.TEXT) {
                    continue;
                }

                String nodeName = parser.getName();
                if (type == XmlPullParser.END_TAG) {
                    if (nodeName.equals(NODE_PREFERENCE_CATEGORY)) {
                        lastCategoryKey = null;
                    }
                    continue;
                }

                key = getDataKey(getActivity(), attrs);
                advanced = getAdvancedBoolean(getActivity(), attrs);

                if (advanced != null) {
                    if (!advancedModeEnabled && advanced) {
                        // advanced mode is OFF and this preference is advanced
                        preferencesToRemove.add(new PreferenceNodeInfo(lastCategoryKey, key));
                    } else if (advancedModeEnabled && !advanced) {
                        // advanced mode is ON and this preference is not advanced
                        preferencesToRemove.add(new PreferenceNodeInfo(lastCategoryKey, key));
                    }
                }

                if (nodeName.equals(NODE_PREFERENCE_CATEGORY)) {
                    lastCategoryKey = getDataKey(getActivity(), attrs);
                }
            }
        } catch (XmlPullParserException | IOException e) {
            throw new RuntimeException("Error parsing PreferenceScreen", e);
        } finally {
            if (parser != null) parser.close();
        }
        return preferencesToRemove;
    }

    private static class PreferenceNodeInfo {
        String category;
        String key;

        public PreferenceNodeInfo(String category, String key) {
            this.category = category;
            this.key = key;
        }

        @Override
        public String toString() {
            return "category: " + category + ", key: " + key;
        }
    }

    private String getDataKey(Context context, AttributeSet attrs) {
        return getData(context, attrs,
                com.android.internal.R.styleable.Preference,
                com.android.internal.R.styleable.Preference_key);
    }

    private String getData(Context context, AttributeSet set, int[] attrs, int resId) {
        final TypedArray sa = context.obtainStyledAttributes(set, attrs);
        final TypedValue tv = sa.peekValue(resId);

        CharSequence data = null;
        if (tv != null && tv.type == TypedValue.TYPE_STRING) {
            if (tv.resourceId != 0) {
                data = context.getText(tv.resourceId);
            } else {
                data = tv.string;
            }
        }
        return (data != null) ? data.toString() : null;
    }

    private Boolean getAdvancedBoolean(Context context, AttributeSet attrs) {
        return getBoolean(context, attrs,
                R.styleable.Preference, R.styleable.Preference_advanced);
    }

    private Boolean getBoolean(Context context, AttributeSet set, int[] attrs, int resId) {
        final TypedArray sa = context.obtainStyledAttributes(set, attrs);
        final TypedValue tv = sa.peekValue(resId);

        if (tv != null && tv.type == TypedValue.TYPE_INT_BOOLEAN) {
            if (tv.resourceId != 0) {
                return context.getResources().getBoolean(tv.resourceId);
            } else {
                return tv.data != 0;
            }
        }
        return null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(SAVE_HIGHLIGHTED_KEY, mPreferenceHighlighted);
        if (mPreferenceHighlighted) {
            outState.putString(SAVE_HIGHLIGHTED_KEY_KEY, mPreferenceKey);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (!TextUtils.isEmpty(mHelpUrl)) {
            setHasOptionsMenu(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        final Bundle args = getArguments();
        if (args != null) {
            if (!TextUtils.equals(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY, mPreferenceKey)) {
                mPreferenceKey = args.getString(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY);
                mPreferenceHighlighted = false;
            }
        }
        highlightPreferenceIfNeeded();
    }

    @Override
    protected void onBindPreferences() {
        registerObserverIfNeeded();
    }

    @Override
    protected void onUnbindPreferences() {
        unregisterObserverIfNeeded();
    }

    @Override
    public void onStop() {
        super.onStop();

        unregisterObserverIfNeeded();
    }

    public void registerObserverIfNeeded() {
        if (!mIsDataSetObserverRegistered) {
            if (mCurrentRootAdapter != null) {
                mCurrentRootAdapter.unregisterDataSetObserver(mDataSetObserver);
            }
            mCurrentRootAdapter = getPreferenceScreen().getRootAdapter();
            mCurrentRootAdapter.registerDataSetObserver(mDataSetObserver);
            mIsDataSetObserverRegistered = true;
        }
    }

    public void unregisterObserverIfNeeded() {
        if (mIsDataSetObserverRegistered) {
            if (mCurrentRootAdapter != null) {
                mCurrentRootAdapter.unregisterDataSetObserver(mDataSetObserver);
                mCurrentRootAdapter = null;
            }
            mIsDataSetObserverRegistered = false;
        }
    }

    public void highlightPreferenceIfNeeded() {
        getView().post(new Runnable() {
            @Override
            public void run() {
                if (isAdded() && !mPreferenceHighlighted &&!TextUtils.isEmpty(mPreferenceKey)) {
                    highlightPreference(mPreferenceKey);
                }
            }
        });
    }

    private Drawable getHighlightDrawable() {
        if (mHighlightDrawable == null) {
            mHighlightDrawable = getActivity().getDrawable(R.drawable.preference_highlight);
        }
        return mHighlightDrawable;
    }

    /**
     * Return a valid ListView position or -1 if none is found
     */
    private int canUseListViewForHighLighting(String key) {
        if (!hasListView()) {
            return -1;
        }

        ListView listView = getListView();
        ListAdapter adapter = listView.getAdapter();

        if (adapter != null && adapter instanceof PreferenceGroupAdapter) {
            return findListPositionFromKey(adapter, key);
        }

        return -1;
    }

    private void highlightPreference(String key) {
        final Drawable highlight = getHighlightDrawable();

        final int position = canUseListViewForHighLighting(key);
        if (position >= 0) {
            mPreferenceHighlighted = true;

            final ListView listView = getListView();
            final ListAdapter adapter = listView.getAdapter();

            ((PreferenceGroupAdapter) adapter).setHighlightedDrawable(highlight);
            ((PreferenceGroupAdapter) adapter).setHighlighted(position);

            listView.post(new Runnable() {
                @Override
                public void run() {
                    listView.setSelection(position);
                    listView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            final int index = position - listView.getFirstVisiblePosition();
                            if (index >= 0 && index < listView.getChildCount()) {
                                final View v = listView.getChildAt(index);
                                final int centerX = v.getWidth() / 2;
                                final int centerY = v.getHeight() / 2;
                                highlight.setHotspot(centerX, centerY);
                                v.setPressed(true);
                                v.setPressed(false);
                            }
                        }
                    }, DELAY_HIGHLIGHT_DURATION_MILLIS);
                }
            });
        }
    }

    private int findListPositionFromKey(ListAdapter adapter, String key) {
        final int count = adapter.getCount();
        for (int n = 0; n < count; n++) {
            final Object item = adapter.getItem(n);
            if (item instanceof Preference) {
                Preference preference = (Preference) item;
                final String preferenceKey = preference.getKey();
                if (preferenceKey != null && preferenceKey.equals(key)) {
                    return n;
                }
            }
        }
        return -1;
    }

    protected void removePreference(String key) {
        Preference pref = findPreference(key);
        if (pref != null) {
            getPreferenceScreen().removePreference(pref);
        }
    }

    /**
     * Override this if you want to show a help item in the menu, by returning the resource id.
     * @return the resource id for the help url
     */
    protected int getHelpResource() {
        return 0;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mHelpUrl != null && getActivity() != null) {
            MenuItem helpItem = menu.add(0, MENU_HELP, 0, R.string.help_label);
            HelpUtils.prepareHelpMenuItem(getActivity(), helpItem, mHelpUrl);
        }
    }

    /*
     * The name is intentionally made different from Activity#finish(), so that
     * users won't misunderstand its meaning.
     */
    public final void finishFragment() {
        getActivity().onBackPressed();
    }

    // Some helpers for functions used by the settings fragments when they were activities

    /**
     * Returns the ContentResolver from the owning Activity.
     */
    protected ContentResolver getContentResolver() {
        Context context = getActivity();
        if (context != null) {
            mContentResolver = context.getContentResolver();
        }
        return mContentResolver;
    }

    /**
     * Returns the specified system service from the owning Activity.
     */
    protected Object getSystemService(final String name) {
        return getActivity().getSystemService(name);
    }

    /**
     * Returns the PackageManager from the owning Activity.
     */
    protected PackageManager getPackageManager() {
        return getActivity().getPackageManager();
    }

    @Override
    public void onDetach() {
        if (isRemoving()) {
            if (mDialogFragment != null) {
                mDialogFragment.dismiss();
                mDialogFragment = null;
            }
        }
        super.onDetach();
    }

    // Dialog management

    protected void showDialog(int dialogId) {
        if (mDialogFragment != null) {
            Log.e(TAG, "Old dialog fragment not null!");
        }
        mDialogFragment = new SettingsDialogFragment(this, dialogId);
        mDialogFragment.show(getChildFragmentManager(), Integer.toString(dialogId));
    }

    public Dialog onCreateDialog(int dialogId) {
        return null;
    }

    protected void removeDialog(int dialogId) {
        // mDialogFragment may not be visible yet in parent fragment's onResume().
        // To be able to dismiss dialog at that time, don't check
        // mDialogFragment.isVisible().
        if (mDialogFragment != null && mDialogFragment.getDialogId() == dialogId) {
            mDialogFragment.dismiss();
        }
        mDialogFragment = null;
    }

    /**
     * Sets the OnCancelListener of the dialog shown. This method can only be
     * called after showDialog(int) and before removeDialog(int). The method
     * does nothing otherwise.
     */
    protected void setOnCancelListener(DialogInterface.OnCancelListener listener) {
        if (mDialogFragment != null) {
            mDialogFragment.mOnCancelListener = listener;
        }
    }

    /**
     * Sets the OnDismissListener of the dialog shown. This method can only be
     * called after showDialog(int) and before removeDialog(int). The method
     * does nothing otherwise.
     */
    protected void setOnDismissListener(DialogInterface.OnDismissListener listener) {
        if (mDialogFragment != null) {
            mDialogFragment.mOnDismissListener = listener;
        }
    }

    public void onDialogShowing() {
        // override in subclass to attach a dismiss listener, for instance
    }

    public static class SettingsDialogFragment extends DialogFragment {
        private static final String KEY_DIALOG_ID = "key_dialog_id";
        private static final String KEY_PARENT_FRAGMENT_ID = "key_parent_fragment_id";

        private int mDialogId;

        private Fragment mParentFragment;

        private DialogInterface.OnCancelListener mOnCancelListener;
        private DialogInterface.OnDismissListener mOnDismissListener;

        public SettingsDialogFragment() {
            /* do nothing */
        }

        public SettingsDialogFragment(DialogCreatable fragment, int dialogId) {
            mDialogId = dialogId;
            if (!(fragment instanceof Fragment)) {
                throw new IllegalArgumentException("fragment argument must be an instance of "
                        + Fragment.class.getName());
            }
            mParentFragment = (Fragment) fragment;
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            if (mParentFragment != null) {
                outState.putInt(KEY_DIALOG_ID, mDialogId);
                outState.putInt(KEY_PARENT_FRAGMENT_ID, mParentFragment.getId());
            }
        }

        @Override
        public void onStart() {
            super.onStart();

            if (mParentFragment != null && mParentFragment instanceof SettingsPreferenceFragment) {
                ((SettingsPreferenceFragment) mParentFragment).onDialogShowing();
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            if (savedInstanceState != null) {
                mDialogId = savedInstanceState.getInt(KEY_DIALOG_ID, 0);
                mParentFragment = getParentFragment();
                int mParentFragmentId = savedInstanceState.getInt(KEY_PARENT_FRAGMENT_ID, -1);
                if (mParentFragment == null) {
                    mParentFragment = getFragmentManager().findFragmentById(mParentFragmentId);
                }
                if (!(mParentFragment instanceof DialogCreatable)) {
                    throw new IllegalArgumentException(
                            (mParentFragment != null
                                    ? mParentFragment.getClass().getName()
                                    : mParentFragmentId)
                                    + " must implement "
                                    + DialogCreatable.class.getName());
                }
                // This dialog fragment could be created from non-SettingsPreferenceFragment
                if (mParentFragment instanceof SettingsPreferenceFragment) {
                    // restore mDialogFragment in mParentFragment
                    ((SettingsPreferenceFragment) mParentFragment).mDialogFragment = this;
                }
            }
            return ((DialogCreatable) mParentFragment).onCreateDialog(mDialogId);
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);
            if (mOnCancelListener != null) {
                mOnCancelListener.onCancel(dialog);
            }
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            if (mOnDismissListener != null) {
                mOnDismissListener.onDismiss(dialog);
            }
        }

        public int getDialogId() {
            return mDialogId;
        }

        @Override
        public void onDetach() {
            super.onDetach();

            // This dialog fragment could be created from non-SettingsPreferenceFragment
            if (mParentFragment instanceof SettingsPreferenceFragment) {
                // in case the dialog is not explicitly removed by removeDialog()
                if (((SettingsPreferenceFragment) mParentFragment).mDialogFragment == this) {
                    ((SettingsPreferenceFragment) mParentFragment).mDialogFragment = null;
                }
            }
        }
    }

    protected boolean hasNextButton() {
        return ((ButtonBarHandler)getActivity()).hasNextButton();
    }

    protected Button getNextButton() {
        return ((ButtonBarHandler)getActivity()).getNextButton();
    }

    public void finish() {
        getActivity().onBackPressed();
    }

    public boolean startFragment(Fragment caller, String fragmentClass, int titleRes,
            int requestCode, Bundle extras) {
        final Activity activity = getActivity();
        if (activity instanceof SettingsActivity) {
            SettingsActivity sa = (SettingsActivity) activity;
            sa.startPreferencePanel(fragmentClass, extras, titleRes, null, caller, requestCode);
            return true;
        } else if (activity instanceof PreferenceActivity) {
            PreferenceActivity sa = (PreferenceActivity) activity;
            sa.startPreferencePanel(fragmentClass, extras, titleRes, null, caller, requestCode);
            return true;
        } else {
            Log.w(TAG,
                    "Parent isn't SettingsActivity nor PreferenceActivity, thus there's no way to "
                    + "launch the given Fragment (name: " + fragmentClass
                    + ", requestCode: " + requestCode + ")");
            return false;
        }
    }
    
    public void setTitle(int resId) {
        getActivity().setTitle(resId);
    }
}
