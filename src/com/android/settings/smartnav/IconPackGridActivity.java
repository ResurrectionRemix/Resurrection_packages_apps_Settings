/*
 * Copyright (C) 2016 The DirtyUnicorns Project
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
 * GridView displaying all the available icons in a Icon pack.
 */

package com.android.settings.smartnav;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import com.android.internal.utils.ActionUtils;
import com.android.settings.R;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class IconPackGridActivity extends Activity {
    private IconGridAdapter mAdapter;
    private ArrayList<IconInfo> mGridData;
    private GridView mGridView;
    private ProgressBar mProgressBar;
    private LruCache<String, Bitmap> mMemoryCache;
    private String mPackageName;
    private Resources mIconRes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // intent with package name is REQUIRED here
        mPackageName = getIntent().getStringExtra(ActionUtils.INTENT_EXTRA_ICON_PACKAGE_NAME);
        if (mPackageName == null) {
            setResult(RESULT_CANCELED);
            finish();
        }
        try {
            PackageManager pm = getPackageManager();
            String title = pm.getApplicationInfo(mPackageName, 0).loadLabel(pm).toString();
            if (title != null) {
                setTitle(title);
            }
        } catch (Exception e) {
            setTitle(R.string.icon_pack_picker_dialog_title);
        }
        try {
            PackageInfo info = getPackageManager().getPackageInfo(mPackageName, 0);
            String iconApk = info.applicationInfo.publicSourceDir;
            AssetManager assets = new AssetManager();
            assets.addAssetPath(iconApk);
            DisplayMetrics dm = getResources().getDisplayMetrics();
            Configuration config = getResources().getConfiguration();
            mIconRes = new Resources(assets, dm, config);
        } catch (Exception e) {
            setResult(RESULT_CANCELED);
            finish();
        }

        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setContentView(R.layout.icon_picker_grid);

        // Get memory class of this device, exceeding this amount will throw an
        // OutOfMemory exception.
        final int memClass = ((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE))
                .getMemoryClass();
        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = 1024 * 1024 * memClass / 8;
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in bytes rather than number of items.
                return bitmap.getByteCount();
            }
        };

        mGridView = (GridView) findViewById(R.id.icon_grid);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mGridData = new ArrayList<>();
        mAdapter = new IconGridAdapter(this);
        mGridView.setAdapter(mAdapter);
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                IconInfo info = (IconInfo) parent.getItemAtPosition(position);
                Intent resultIntent = new Intent();
                resultIntent.putExtra(ActionUtils.INTENT_EXTRA_ICON_DATA_TYPE, ActionUtils.INTENT_EXTRA_ICON_DATA_TYPE_ICON_PACK);
                resultIntent.putExtra(ActionUtils.INTENT_EXTRA_ICON_DATA_PACKAGE, mPackageName);
                resultIntent.putExtra(ActionUtils.INTENT_EXTRA_ICON_DATA_NAME, info.name);
                IconPackGridActivity.this.setResult(RESULT_OK, resultIntent);
                IconPackGridActivity.this.finish();
            }
        });
        new AsyncIconLoaderTask().execute(mPackageName);
        mProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(RESULT_CANCELED);
            finish();
            return true;
        }
        return false;
    }

    public class AsyncIconLoaderTask extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... params) {
            String packageName = params[0];
            Set<String> set = new HashSet<>();

            //From assets
            try {
                InputStream appfilterstream = mIconRes.getAssets().open("drawable.xml");
                set.addAll(xmlInputStreamToSet(appfilterstream, packageName));
            } catch (Exception e) {
                e.printStackTrace();
            }

            //From res/xml
            try {
                int xmlId = mIconRes.getIdentifier("drawable", "xml", packageName);
                set.addAll(xmlPullParserToSet(mIconRes.getXml(xmlId)));
            } catch (Exception e) {
                e.printStackTrace();
            }

            for (String drawableName : set) {
                int resId = mIconRes.getIdentifier(drawableName, "drawable", packageName);
                if (resId > 0) {
                    Drawable d = mIconRes.getDrawable(resId);
                    if (d != null) {
                        IconInfo info = new IconInfo();
                        info.id = resId;
                        info.name = drawableName;
                        mGridData.add(info);
                    }
                }
            }

            Collections.sort(mGridData, new Comparator<IconInfo>() {
                @Override
                public int compare(IconInfo lhs, IconInfo rhs) {
                    return lhs.name.compareTo(rhs.name);
                }

            });
            return !mGridData.isEmpty();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                mAdapter.setGridData(mGridData);
            } else {
                setResult(RESULT_CANCELED);
                finish();
            }
            mProgressBar.setVisibility(View.GONE);
        }

        private Set<String> xmlInputStreamToSet(InputStream inputStream, String packageName) {

            Set<String> set = new HashSet<>();

            try {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser xpp = factory.newPullParser();
                xpp.setInput(inputStream, "utf-8");
                set.addAll(xmlPullParserToSet(xpp));
            } catch (Exception e) {
                e.printStackTrace();
            }

            return set;
        }

        private Set<String> xmlPullParserToSet(XmlPullParser xpp) {

            Set<String> set = new HashSet<>();
            try {
                if (xpp != null) {
                    int eventType = xpp.getEventType();
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG) {
                            if (xpp.getName().equals("item")) {
                                for (int i = 0; i < xpp.getAttributeCount(); i++) {
                                    if (xpp.getAttributeName(i).startsWith("drawable")) {
                                        String drawableName = xpp.getAttributeValue(i);
                                        set.add(drawableName);
                                    }
                                }
                            }
                        }
                        eventType = xpp.next();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return set;

        }

    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable == null) {
            return null;
        } else if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private class IconGridAdapter extends BaseAdapter {
        private LayoutInflater mInflater;
        private ArrayList<IconInfo> mGridDataInternal = new ArrayList<IconInfo>();

        public IconGridAdapter(Context ctx) {
            mInflater = LayoutInflater.from(ctx);
        }

        void setGridData(ArrayList<IconInfo> gridData) {
            mGridDataInternal = gridData;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mGridDataInternal.size();
        }

        @Override
        public IconInfo getItem(int position) {
            return mGridDataInternal.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView != null) {
                holder = (ViewHolder) convertView.getTag();
            } else {
                convertView = mInflater.inflate(R.layout.icon_picker_item, null, false);
                holder = new ViewHolder();
                convertView.setTag(holder);
                holder.icon = (ImageView) convertView.findViewById(R.id.grid_item_image);
            }
            IconInfo info = (IconInfo) getItem(position);
            Bitmap b = mMemoryCache.get(info.name);
            Drawable d = null;
            if (b == null) {
                try {
                    d = mIconRes.getDrawable(info.id);
                    Bitmap tmp = drawableToBitmap(d);
                    mMemoryCache.put(info.name, tmp);
                } catch (Exception e) {
                }
            } else {
                d = new BitmapDrawable(getResources(), b);
            }
            holder.icon.setImageDrawable(d);

            return convertView;
        }
    }

    private static class ViewHolder {
        ImageView icon;
    }

    private static class IconInfo {
        Integer id;
        String name;
    }
}
