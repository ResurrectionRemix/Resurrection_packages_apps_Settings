package com.android.settings.rr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.R;

public abstract class HAFRAppChooserAdapter extends BaseAdapter implements Filterable {

    final Context mContext;
    final Handler mHandler;
    final PackageManager mPackageManager;
    final LayoutInflater mLayoutInflater;

    protected List<PackageInfo> mInstalledAppInfo;
    protected List<AppItem> mInstalledApps = new LinkedList<AppItem>();
    protected List<PackageInfo> mTemporarylist;

    boolean isUpdating;

    public HAFRAppChooserAdapter(Context context) {
        mContext = context;
        mHandler = new Handler();
        mPackageManager = mContext.getPackageManager();
        mLayoutInflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mInstalledAppInfo = mPackageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS);
        mTemporarylist = mInstalledAppInfo;
    }

    public synchronized void update() {
        onStartUpdate();

        new Thread(new Runnable() {
            @Override
            public void run() {
                isUpdating = true;
                final List<AppItem> temp = new LinkedList<AppItem>();
                for (PackageInfo info : mTemporarylist) {
                    final AppItem item = new AppItem();
                    item.title = info.applicationInfo.loadLabel(mPackageManager);
                    item.icon = info.applicationInfo.loadIcon(mPackageManager);
                    item.packageName = info.packageName;
                    final int index = Collections.binarySearch(temp, item);
                    if (index < 0) {
                        temp.add((-index - 1), item);
                    } else {
                        temp.add((index + 1), item);
                    }
                }
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mInstalledApps = temp;
                        notifyDataSetChanged();
                        isUpdating = false;
                        onFinishUpdate();
                    }
                });
            }
        }).start();
    }

    public abstract void onStartUpdate();

    public abstract void onFinishUpdate();

    @Override
    public int getCount() {
        return mInstalledApps.size();
    }

    @Override
    public AppItem getItem(int position) {
        if (position >= mInstalledApps.size()) {
            return mInstalledApps.get(mInstalledApps.size());
        } else if (position < 0) {
            return mInstalledApps.get(0);
        }

        return mInstalledApps.get(position);
    }

    @Override
    public long getItemId(int position) {
        if (position < 0 || position >= mInstalledApps.size()) {
            return -1;
        }

        return getItem(position).hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView != null) {
            holder = (ViewHolder) convertView.getTag();
        } else {
            convertView = mLayoutInflater.inflate(R.layout.view_app_list, parent, false);
            holder = new ViewHolder();
            holder.name = (TextView) convertView.findViewById(android.R.id.title);
            holder.icon = (ImageView) convertView.findViewById(android.R.id.icon);
            holder.pkg = (TextView) convertView.findViewById(android.R.id.message);
            convertView.setTag(holder);
        }
        AppItem appInfo = getItem(position);

        holder.name.setText(appInfo.title);
        holder.pkg.setText(appInfo.packageName);
        holder.icon.setImageDrawable(appInfo.icon);
        return convertView;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                if (TextUtils.isEmpty(constraint)) {
                    // No filter implemented we return all the list
                    mTemporarylist = mInstalledAppInfo;
                    return new FilterResults();
                }

                ArrayList<PackageInfo> FilteredList = new ArrayList<PackageInfo>();
                for (PackageInfo data : mInstalledAppInfo) {
                    final String filterText = constraint.toString().toLowerCase(Locale.ENGLISH);
                    try {
                        if (data.applicationInfo.loadLabel(mPackageManager).toString()
                                .toLowerCase(Locale.ENGLISH).contains(filterText)) {
                            FilteredList.add(data);
                        } else if (data.packageName.contains(filterText)) {
                            FilteredList.add(data);
                        }
                    } catch (Exception e) {
                    }
                }
                mTemporarylist = FilteredList;
                return new FilterResults();
            }
        };
    }

    public class AppItem implements Comparable<AppItem> {
        public CharSequence title;
        public String packageName;
        public Drawable icon;

        @Override
        public int compareTo(AppItem another) {
            return this.title.toString().compareTo(another.title.toString());
        }
    }

    static class ViewHolder {
        TextView name;
        ImageView icon;
        TextView pkg;
    }
}
