/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.settings.cyanogenmod;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.widget.SwitchBar;
import cyanogenmod.externalviews.KeyguardExternalViewProviderService;
import cyanogenmod.providers.CMSettings;
import org.cyanogenmod.internal.util.CmLockPatternUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static cyanogenmod.providers.CMSettings.Secure.LIVE_LOCK_SCREEN_ENABLED;

public class LiveLockScreenSettings extends SettingsPreferenceFragment implements
        SwitchBar.OnSwitchChangeListener {
    private static final String TAG = LiveLockScreenSettings.class.getSimpleName();
    static final boolean DEBUG = false;
    private static final String PACKAGE_SCHEME = "package";

    private final PackageReceiver mPackageReceiver = new PackageReceiver();

    private Context mContext;
    private LiveLockScreenBackend mBackend;
    private LiveLockScreenInfoAdapter mAdapter;
    private SwitchBar mSwitchBar;
    private boolean mRefreshing;

    @Override
    public void onAttach(Activity activity) {
        logd("onAttach(%s)", activity.getClass().getSimpleName());
        super.onAttach(activity);
        mContext = activity;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.LOCKSCREEN;
    }

    @Override
    public void onCreate(Bundle icicle) {
        logd("onCreate(%s)", icicle);
        super.onCreate(icicle);

        mBackend = new LiveLockScreenBackend(getActivity());
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        if (!mRefreshing) {
            mBackend.setEnabled(isChecked);
            refreshFromBackend();
        }
    }

    @Override
    public void onStart() {
        logd("onStart()");
        super.onStart();
    }

    @Override
    public void onDestroyView() {
        logd("onDestroyView()");
        super.onDestroyView();

        mSwitchBar.removeOnSwitchChangeListener(this);
        mSwitchBar.hide();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        logd("onActivityCreated(%s)", savedInstanceState);
        super.onActivityCreated(savedInstanceState);

        ListView listView = getListView();
        listView.setItemsCanFocus(true);

        TextView emptyView = (TextView) getView().findViewById(android.R.id.empty);
        emptyView.setText(R.string.live_lock_screen_settings_disabled_prompt);
        listView.setEmptyView(emptyView);

        mAdapter = new LiveLockScreenInfoAdapter(mContext);
        listView.setAdapter(mAdapter);

        final SettingsActivity sa = (SettingsActivity) getActivity();
        mSwitchBar = sa.getSwitchBar();
        mSwitchBar.addOnSwitchChangeListener(this);
        mSwitchBar.show();
    }

    @Override
    public void onPause() {
        logd("onPause()");
        super.onPause();

        mContext.unregisterReceiver(mPackageReceiver);
    }

    @Override
    public void onResume() {
        logd("onResume()");
        super.onResume();
        refreshFromBackend();

        // listen for package changes
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme(PACKAGE_SCHEME);
        mContext.registerReceiver(mPackageReceiver , filter);
    }

    private void refreshFromBackend() {
        logd("refreshFromBackend()");
        mRefreshing = true;
        boolean liveLockScreenEnabled = mBackend.isEnabled();
        if (mSwitchBar.isChecked() != liveLockScreenEnabled) {
            mSwitchBar.setChecked(liveLockScreenEnabled);
        }

        mAdapter.clear();
        if (liveLockScreenEnabled) {
            List<LiveLockScreenBackend.LiveLockScreenInfo> liveLockScreenInfos =
                    mBackend.getLiveLockScreenInfos();
            mAdapter.addAll(liveLockScreenInfos);
        }
        mRefreshing = false;
    }

    private static void logd(String msg, Object... args) {
        if (DEBUG) {
            Log.d(TAG, args == null || args.length == 0 ? msg : String.format(msg, args));
        }
    }

    private class LiveLockScreenInfoAdapter extends ArrayAdapter<LiveLockScreenBackend.LiveLockScreenInfo> {
        private final LayoutInflater mInflater;

        public LiveLockScreenInfoAdapter(Context context) {
            super(context, 0);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LiveLockScreenBackend.LiveLockScreenInfo liveLockScreenInfo = getItem(position);
            logd("getView(%s)", liveLockScreenInfo.caption);
            final View row = convertView != null ? convertView :
                    createLiveLockScreenInfoRow(parent);
            row.setTag(liveLockScreenInfo);

            // bind icon
            ((ImageView) row.findViewById(android.R.id.icon))
                    .setImageDrawable(liveLockScreenInfo.icon);

            // bind caption
            ((TextView) row.findViewById(android.R.id.title)).setText(liveLockScreenInfo.caption);

            // bind radio button
            RadioButton radioButton = (RadioButton) row.findViewById(android.R.id.button1);
            radioButton.setChecked(liveLockScreenInfo.isActive);
            radioButton.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    row.onTouchEvent(event);
                    return false;
                }});

            // bind settings button + divider
            boolean showSettings = liveLockScreenInfo.settingsComponentName != null;
            View settingsDivider = row.findViewById(R.id.divider);
            settingsDivider.setVisibility(showSettings ? View.VISIBLE : View.INVISIBLE);

            ImageView settingsButton = (ImageView) row.findViewById(android.R.id.button2);
            settingsButton.setVisibility(showSettings ? View.VISIBLE : View.INVISIBLE);
            settingsButton.setAlpha(liveLockScreenInfo.isActive ? 1f : Utils.DISABLED_ALPHA);
            settingsButton.setEnabled(liveLockScreenInfo.isActive);
            settingsButton.setFocusable(liveLockScreenInfo.isActive);
            settingsButton.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    mBackend.launchSettings(
                            (LiveLockScreenBackend.LiveLockScreenInfo) row.getTag());
                }});

            return row;
        }

        private View createLiveLockScreenInfoRow(ViewGroup parent) {
            final View row =  mInflater.inflate(R.layout.live_lock_screen_info_row, parent, false);
            final View header = row.findViewById(android.R.id.widget_frame);
            header.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    v.setPressed(true);
                    activate((LiveLockScreenBackend.LiveLockScreenInfo) row.getTag());
                }});
            return row;
        }

        private LiveLockScreenBackend.LiveLockScreenInfo getCurrentSelection() {
            for (int i = 0; i < getCount(); i++) {
                LiveLockScreenBackend.LiveLockScreenInfo liveLockScreenInfo = getItem(i);
                if (liveLockScreenInfo.isActive) {
                    return liveLockScreenInfo;
                }
            }
            return null;
        }
        private void activate(LiveLockScreenBackend.LiveLockScreenInfo liveLockScreenInfo) {
            if (liveLockScreenInfo.equals(getCurrentSelection())) {
                return;
            }
            for (int i = 0; i < getCount(); i++) {
                getItem(i).isActive = false;
            }
            liveLockScreenInfo.isActive = true;
            mBackend.setActiveLiveLockScreen(liveLockScreenInfo.componentName);
            if (liveLockScreenInfo.settingsComponentName != null) {
                mBackend.launchSettings(liveLockScreenInfo);
            }
            notifyDataSetChanged();
        }
    }

    private class PackageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            logd("PackageReceiver.onReceive");
            refreshFromBackend();
        }
    }

    public static class LiveLockScreenBackend {
        private static final String TAG = LiveLockScreenSettings.class.getSimpleName() + ".Backend";

        public static class LiveLockScreenInfo {
            CharSequence caption;
            Drawable icon;
            boolean isActive;
            public ComponentName componentName;
            public ComponentName settingsComponentName;

            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder(LiveLockScreenInfo.class.getSimpleName());
                sb.append('[').append(caption);
                if (isActive) {
                    sb.append(",active");
                }
                sb.append(',').append(componentName);
                if (settingsComponentName != null) {
                    sb.append("settings=").append(settingsComponentName);
                }
                return sb.append(']').toString();
            }
        }

        private final Context mContext;
        private final LiveLockScreenInfoComparator mComparator;
        private CmLockPatternUtils mLockPatternUtils;

        public LiveLockScreenBackend(Context context) {
            mContext = context;
            mComparator = new LiveLockScreenInfoComparator(null);
            mLockPatternUtils = new CmLockPatternUtils(context);
        }

        public List<LiveLockScreenInfo> getLiveLockScreenInfos() {
            logd("getLiveLockScreenInfos()");
            ComponentName activeLiveLockScreen = getActiveLiveLockScreen();
            PackageManager pm = mContext.getPackageManager();
            Intent liveLockScreenIntent =
                    new Intent(KeyguardExternalViewProviderService.SERVICE_INTERFACE);
            List<ResolveInfo> resolveInfos = pm.queryIntentServices(liveLockScreenIntent,
                    PackageManager.GET_META_DATA);
            List<LiveLockScreenInfo> liveLockScreenInfos = new ArrayList<>(resolveInfos.size());
            for (ResolveInfo resolveInfo : resolveInfos) {
                if (resolveInfo.serviceInfo == null) {
                    continue;
                }
                LiveLockScreenInfo liveLockScreenInfo = new LiveLockScreenInfo();
                liveLockScreenInfo.caption = resolveInfo.loadLabel(pm);
                liveLockScreenInfo.icon = resolveInfo.loadIcon(pm);
                liveLockScreenInfo.componentName = getLiveLockScreenComponentName(resolveInfo);
                liveLockScreenInfo.isActive = liveLockScreenInfo.componentName.equals(
                        activeLiveLockScreen);
                liveLockScreenInfo.settingsComponentName =
                        getSettingsComponentName(pm, resolveInfo);
                liveLockScreenInfos.add(liveLockScreenInfo);
            }
            Collections.sort(liveLockScreenInfos, mComparator);
            return liveLockScreenInfos;
        }

        public CharSequence getActiveLiveLockScreenName() {
            ComponentName cn = getActiveLiveLockScreen();
            if (cn != null) {
                PackageManager pm = mContext.getPackageManager();
                try {
                    ServiceInfo ri = pm.getServiceInfo(cn, 0);
                    if (ri != null) {
                        return ri.loadLabel(pm);
                    }
                } catch (PackageManager.NameNotFoundException exc) {
                    return null; // uninstalled?
                }
            }
            return null;
        }

        public boolean isEnabled() {
            return getBoolean(LIVE_LOCK_SCREEN_ENABLED, false);
        }

        public void setEnabled(boolean value) {
            logd("setEnabled(%s)", value);
            setBoolean(LIVE_LOCK_SCREEN_ENABLED, value);
        }

        private boolean getBoolean(String key, boolean def) {
            return CMSettings.Secure.getInt(mContext.getContentResolver(), key, def ? 1 : 0) == 1;
        }

        private void setBoolean(String key, boolean value) {
            CMSettings.Secure.putInt(mContext.getContentResolver(), key, value ? 1 : 0);
        }

        public void setActiveLiveLockScreen(ComponentName liveLockScreen) {
            logd("setActiveLiveLockScreen(%s)", liveLockScreen);
            if (mLockPatternUtils == null) {
                return;
            }
            try {
                mLockPatternUtils.setThirdPartyKeyguard(liveLockScreen);
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "Failed to set active live lock screen to " + liveLockScreen, e);
            }
        }

        public ComponentName getActiveLiveLockScreen() {
            if (mLockPatternUtils == null) {
                return null;
            }
            return mLockPatternUtils.getThirdPartyKeyguardComponent();
        }

        public void launchSettings(LiveLockScreenInfo liveLockScreenInfo) {
            logd("launchSettings(%s)", liveLockScreenInfo);
            if (liveLockScreenInfo == null || liveLockScreenInfo.settingsComponentName == null) {
                return;
            }
            mContext.startActivity(new Intent().setComponent(
                    liveLockScreenInfo.settingsComponentName));
        }

        private static ComponentName getLiveLockScreenComponentName(ResolveInfo resolveInfo) {
            if (resolveInfo == null || resolveInfo.serviceInfo == null) {
                return null;
            }
            return new ComponentName(resolveInfo.serviceInfo.packageName,
                    resolveInfo.serviceInfo.name);
        }

        private static ComponentName getSettingsComponentName(PackageManager pm,
                ResolveInfo resolveInfo) {
            if (resolveInfo == null
                    || resolveInfo.serviceInfo == null
                    || resolveInfo.serviceInfo.metaData == null) {
                return null;
            }
            String cn = null;
            XmlResourceParser parser = null;
            Exception caughtException = null;
            try {
                parser = resolveInfo.serviceInfo.loadXmlMetaData(pm,
                        KeyguardExternalViewProviderService.META_DATA);
                if (parser == null) {
                    Log.w(TAG, "No " + KeyguardExternalViewProviderService.META_DATA
                            + " meta-data");
                    return null;
                }
                Resources res =
                        pm.getResourcesForApplication(resolveInfo.serviceInfo.applicationInfo);
                AttributeSet attrs = Xml.asAttributeSet(parser);
                int type;
                while ((type=parser.next()) != XmlPullParser.END_DOCUMENT
                        && type != XmlPullParser.START_TAG) {
                }
                String nodeName = parser.getName();
                if (!"lockscreen".equals(nodeName)) {
                    Log.w(TAG, "Meta-data does not start with lockscreen tag");
                    return null;
                }
                // Dream styleable has the attributes we need so we'll piggy back off of that
                TypedArray sa = res.obtainAttributes(attrs, com.android.internal.R.styleable.Dream);
                cn = sa.getString(com.android.internal.R.styleable.Dream_settingsActivity);
                sa.recycle();
            } catch (PackageManager.NameNotFoundException e) {
                caughtException = e;
            } catch (IOException e) {
                caughtException = e;
            } catch (XmlPullParserException e) {
                caughtException = e;
            } finally {
                if (parser != null) parser.close();
            }
            if (caughtException != null) {
                Log.w(TAG, "Error parsing : " + resolveInfo.serviceInfo.packageName,
                        caughtException);
                return null;
            }
            if (cn != null && cn.indexOf('/') < 0) {
                cn = resolveInfo.serviceInfo.packageName + "/" + cn;
            }
            return cn == null ? null : ComponentName.unflattenFromString(cn);
        }

        private static void logd(String msg, Object... args) {
            if (LiveLockScreenSettings.DEBUG) {
                Log.d(TAG, args == null || args.length == 0 ? msg : String.format(msg, args));
            }
        }

        private static class LiveLockScreenInfoComparator implements
                Comparator<LiveLockScreenInfo> {
            private final ComponentName mDefaultLiveLockScreen;

            public LiveLockScreenInfoComparator(ComponentName defaultLiveLockScreen) {
                mDefaultLiveLockScreen = defaultLiveLockScreen;
            }

            @Override
            public int compare(LiveLockScreenInfo lhs, LiveLockScreenInfo rhs) {
                return sortKey(lhs).compareTo(sortKey(rhs));
            }

            private String sortKey(LiveLockScreenInfo di) {
                StringBuilder sb = new StringBuilder();
                sb.append(di.componentName.equals(mDefaultLiveLockScreen) ? '0' : '1');
                sb.append(di.caption);
                return sb.toString();
            }
        }
    }
}
