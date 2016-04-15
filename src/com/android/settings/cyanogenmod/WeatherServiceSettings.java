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

package com.android.settings.cyanogenmod;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.content.PackageMonitor;
import com.android.internal.os.BackgroundThread;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import cyanogenmod.providers.CMSettings;
import cyanogenmod.weatherservice.WeatherProviderService;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import static org.cyanogenmod.internal.logging.CMMetricsLogger.WEATHER_SETTINGS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WeatherServiceSettings extends SettingsPreferenceFragment {

    private Context mContext;
    private WeatherProviderServiceInfoAdapter mAdapter;
    private Handler mHandler;
    private static final String TAG = WeatherServiceSettings.class.getSimpleName();

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
        mHandler = new Handler(mContext.getMainLooper());
    }

    @Override
    protected int getMetricsCategory() {
        return WEATHER_SETTINGS;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateAdapter();
        registerPackageMonitor();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterPackageMonitor();
    }

    private void registerPackageMonitor() {
        mPackageMonitor.register(mContext, BackgroundThread.getHandler().getLooper(),
                UserHandle.ALL, true);
    }

    private void unregisterPackageMonitor() {
        mPackageMonitor.unregister();
    }

    private PackageMonitor mPackageMonitor = new PackageMonitor() {
        @Override
        public void onPackageAdded(String packageName, int uid) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateAdapter();
                }
            });
        }

        @Override
        public void onPackageRemoved(String packageName, int uid) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateAdapter();
                }
            });
        }
    };

    private void updateAdapter() {
        final PackageManager pm = getContext().getPackageManager();
        final Intent intent = new Intent(WeatherProviderService.SERVICE_INTERFACE);
        List<ResolveInfo> resolveInfoList = pm.queryIntentServices(intent,
                PackageManager.GET_SERVICES | PackageManager.GET_META_DATA);
        List<WeatherProviderServiceInfo> weatherProviderServiceInfos
                = new ArrayList<>(resolveInfoList.size());
        ComponentName activeService = getEnabledWeatherServiceProvider();
        for (ResolveInfo resolveInfo : resolveInfoList) {
            if (resolveInfo.serviceInfo == null) continue;

            if (resolveInfo.serviceInfo.packageName == null
                    || resolveInfo.serviceInfo.name == null) {
                //Really?
                continue;
            }

            if (!resolveInfo.serviceInfo.permission.equals(
                    cyanogenmod.platform.Manifest.permission.BIND_WEATHER_PROVIDER_SERVICE)) {
                continue;
            }
            WeatherProviderServiceInfo serviceInfo = new WeatherProviderServiceInfo();
            serviceInfo.componentName = new ComponentName(resolveInfo.serviceInfo.packageName,
                    resolveInfo.serviceInfo.name);
            serviceInfo.isActive = serviceInfo.componentName.equals(activeService);
            serviceInfo.caption = resolveInfo.loadLabel(pm);
            serviceInfo.icon = resolveInfo.loadIcon(pm);
            serviceInfo.settingsComponentName = getSettingsComponent(pm, resolveInfo);

            weatherProviderServiceInfos.add(serviceInfo);
        }
        mAdapter.clear();
        mAdapter.addAll(weatherProviderServiceInfos);

    }

    private ComponentName getSettingsComponent(PackageManager pm, ResolveInfo resolveInfo) {
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
                    WeatherProviderService.SERVICE_META_DATA);
            if (parser == null) {
                Log.w(TAG, "Can't find " + WeatherProviderService.SERVICE_META_DATA + " meta-data");
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
            if (!"weather-provider-service".equals(nodeName)) {
                Log.w(TAG, "Meta-data does not start with weather-provider-service tag");
                return null;
            }
            //Will use Dream styleable for now, it has the attribute we need
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

    private ComponentName getEnabledWeatherServiceProvider() {
        String activeWeatherServiceProvider = CMSettings.Secure.getString(
                mContext.getContentResolver(), CMSettings.Secure.WEATHER_PROVIDER_SERVICE);
        if (activeWeatherServiceProvider == null) return null;
        return ComponentName.unflattenFromString(activeWeatherServiceProvider);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ListView listView = getListView();
        ViewGroup contentRoot = (ViewGroup) listView.getParent();
        listView.setItemsCanFocus(true);

        View emptyView = getActivity().getLayoutInflater().inflate(
                R.layout.empty_weather_state, contentRoot, false);
        TextView emptyTextView = (TextView) emptyView.findViewById(R.id.message);
        emptyTextView.setText(R.string.weather_settings_no_services_prompt);

        listView.setEmptyView(emptyView);

        contentRoot.addView(emptyView);
        mAdapter = new WeatherProviderServiceInfoAdapter(mContext);
        listView.setAdapter(mAdapter);
    }

    private class WeatherProviderServiceInfo {
        CharSequence caption;
        Drawable icon;
        boolean isActive;
        ComponentName componentName;
        public ComponentName settingsComponentName;
    }

    private class WeatherProviderServiceInfoAdapter
            extends ArrayAdapter<WeatherProviderServiceInfo> {

        private final LayoutInflater mInflater;

        public WeatherProviderServiceInfoAdapter(Context context) {
            super(context, 0);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            WeatherProviderServiceInfo info = getItem(position);
            final View row = convertView != null ? convertView :
                    buildRow(parent);
            row.setTag(info);

            ((ImageView) row.findViewById(android.R.id.icon))
                    .setImageDrawable(info.icon);

            ((TextView) row.findViewById(android.R.id.title)).setText(info.caption);

            RadioButton radioButton = (RadioButton) row.findViewById(android.R.id.button1);
            radioButton.setChecked(info.isActive);
            radioButton.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    row.onTouchEvent(event);
                    return false;
                }
            });

            boolean showSettings = info.settingsComponentName != null;
            View settingsDivider = row.findViewById(R.id.divider);
            settingsDivider.setVisibility(showSettings ? View.VISIBLE : View.INVISIBLE);
            ImageView settingsButton = (ImageView) row.findViewById(android.R.id.button2);
            settingsButton.setVisibility(showSettings ? View.VISIBLE : View.INVISIBLE);
            settingsButton.setAlpha(info.isActive ? 1f : Utils.DISABLED_ALPHA);
            settingsButton.setEnabled(info.isActive);
            settingsButton.setFocusable(info.isActive);
            settingsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    launchSettingsActivity((WeatherProviderServiceInfo)row.getTag());
                }
            });

            return row;
        }

        private void launchSettingsActivity(WeatherProviderServiceInfo info) {
            if (info != null && info.settingsComponentName != null) {
                try {
                    mContext.startActivity(new Intent().setComponent(info.settingsComponentName));
                } catch (ActivityNotFoundException e) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast t = Toast.makeText(mContext,
                                    R.string.weather_settings_activity_not_found,
                                        Toast.LENGTH_LONG);
                            TextView v = (TextView) t.getView().findViewById(android.R.id.message);
                            if (v != null) v.setGravity(Gravity.CENTER);
                            t.show();
                        }
                    });
                    Log.w(TAG, info.settingsComponentName + " not found");
                }
            }
        }

        private View buildRow(ViewGroup parent) {
            final View row =  mInflater.inflate(R.layout.weather_service_provider_info_row,
                    parent, false);
            final View header = row.findViewById(android.R.id.widget_frame);
            header.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    v.setPressed(true);
                    setActiveWeatherProviderService((WeatherProviderServiceInfo) row.getTag());
                }
            });

            return row;
        }

        private void setActiveWeatherProviderService(WeatherProviderServiceInfo info) {
            WeatherProviderServiceInfo currentSelection = getCurrentSelection();
            if (info.equals(currentSelection)) return;
            if (currentSelection != null) {
                currentSelection.isActive = false;
            }
            info.isActive = true;
            CMSettings.Secure.putString(mContext.getContentResolver(),
                    CMSettings.Secure.WEATHER_PROVIDER_SERVICE,
                        info.componentName.flattenToString());
            launchSettingsActivity(info);
            notifyDataSetChanged();
        }

        private WeatherProviderServiceInfo getCurrentSelection() {
            for (int indx = 0; indx < getCount(); indx++) {
                WeatherProviderServiceInfo info = getItem(indx);
                if (info.isActive) {
                    return info;
                }
            }
            return null;
        }
    }
}
