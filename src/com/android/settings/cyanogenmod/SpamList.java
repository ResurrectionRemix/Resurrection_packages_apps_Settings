package com.android.settings.cyanogenmod;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.internal.util.cm.SpamFilter;
import static com.android.internal.util.cm.SpamFilter.*;
import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.Settings.NotificationStationActivity;

public class SpamList extends ListFragment {

    private static final int MENU_NOTIFICATIONS = Menu.FIRST;
    private static final Uri PACKAGES_URI;
    private static final Uri MESSAGES_URI;

    static {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(ContentResolver.SCHEME_CONTENT);
        builder.authority(SpamFilter.AUTHORITY);
        builder.encodedPath("packages");
        PACKAGES_URI = builder.build();

        MESSAGES_URI = builder
                .encodedPath("messages")
                .build();
    }

    private SpamAdapter mAdapter;
    private FetchFilters mTask;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mTask = new FetchFilters();
        mTask.execute();
        getListView().setDividerHeight(0);
        addEmptyView();
        setHasOptionsMenu(true);
        getActivity().getContentResolver().registerContentObserver(
                SpamFilter.NOTIFICATION_URI, true, mObserver);
    }

    private void addEmptyView() {
        TextView v = new TextView(getActivity());
        v.setText(R.string.no_filters_title);
        v.setGravity(Gravity.CENTER);
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        getActivity().addContentView(v, params);
        getListView().setEmptyView(v);
    }

    @Override
    public void onListItemClick(ListView l, View v, final int position, long id) {
        if (mAdapter.getItemViewType(position) == SpamAdapter.HEADER_TYPE) {
            return;
        }
        NotificationInfo info = (NotificationInfo) mAdapter.getItem(position);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(info.appLabel);
        int baseTitleId = info.count == 0 ? R.string.spam_added_title : R.string.spam_last_blocked_title;
        String baseTitle = getActivity().getString(baseTitleId);
        StringBuilder msg = new StringBuilder();
        msg.append(String.format(baseTitle, DateUtils.getRelativeTimeSpanString(info.date))).append("\n\n");
        msg.append(getActivity().getString(R.string.app_ops_ignored_count, info.count));
        builder.setMessage(msg.toString());
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setNeutralButton(R.string.blacklist_button_delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mAdapter.removeItem(position);
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.show();
        TextView textView = (TextView) dialog.findViewById(android.R.id.message);
        textView.setTextSize(17);
    }

    private ContentObserver mObserver = new ContentObserver(null) {
        @Override
        public void onChange(boolean selfChange) {
            if (mTask != null) {
                mTask.cancel(true);
            }
            mTask = new FetchFilters();
            mTask.execute();
        }
    };

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_NOTIFICATIONS, 0, R.string.volume_notification_description)
                .setIcon(R.drawable.ic_settings_notifications)
                .setAlphabeticShortcut('n')
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
                MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_NOTIFICATIONS:
                Intent i = new Intent(getActivity(), NotificationStationActivity.class);
                startActivity(i);
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().getContentResolver().unregisterContentObserver(mObserver);
    }

    private static class ItemInfo {
        String id;
    }

    private static final class PackageInfo extends ItemInfo {
        String packageName;
        CharSequence applicationLabel;
    }

    private static final class NotificationInfo extends ItemInfo {
        String messageText;
        CharSequence appLabel;
        long date;
        int count;
    }

    private class FetchFilters extends AsyncTask<Void, Void, List<ItemInfo>> {

        private void addNotificationsForPackage(PackageInfo pInfo, List<ItemInfo> items) {
            String selection = SpamContract.NotificationTable.PACKAGE_ID + "=?";
            String[] selectionArgs = new String[] {pInfo.id};
            Cursor c = getActivity().getContentResolver().query(MESSAGES_URI, null, selection,
                    selectionArgs, null);
            if (c != null) {
                int notificationIdIndex = c.getColumnIndex(SpamContract.NotificationTable.ID);
                int notificationMessageIndex = c.getColumnIndex(SpamContract.NotificationTable.MESSAGE_TEXT);
                int notificationBlockedIndex = c.getColumnIndex(SpamContract.NotificationTable.LAST_BLOCKED);
                int notificationCountIndex = c.getColumnIndex(SpamContract.NotificationTable.COUNT);
                while (c.moveToNext()) {
                    NotificationInfo nInfo = new NotificationInfo();
                    nInfo.messageText = c.getString(notificationMessageIndex);
                    nInfo.id = c.getString(notificationIdIndex);
                    nInfo.date = c.getLong(notificationBlockedIndex);
                    nInfo.count = c.getInt(notificationCountIndex);
                    nInfo.appLabel = pInfo.applicationLabel;
                    items.add(nInfo);
                }
                c.close();
            }
        }

        @Override
        protected List<ItemInfo> doInBackground(Void... params) {
            List<ItemInfo> items = new ArrayList<ItemInfo>();
            Cursor c = getActivity().getContentResolver().query(
                    PACKAGES_URI, null, null, null, null);
            if (c != null) {
                int packageIdIndex = c.getColumnIndex(SpamContract.PackageTable.ID);
                int packageNameIndex = c.getColumnIndex(SpamContract.PackageTable.PACKAGE_NAME);
                while (c.moveToNext()) {
                    PackageInfo pInfo = new PackageInfo();
                    pInfo.packageName = c.getString(packageNameIndex);
                    getAppInfo(pInfo);
                    pInfo.id = c.getString(packageIdIndex);
                    items.add(pInfo);
                    addNotificationsForPackage(pInfo, items);
                }
                c.close();
            }
            return items;
        }

        private void getAppInfo(PackageInfo info) {
            ApplicationInfo appInfo = null;
            PackageManager pm = getActivity().getPackageManager();
            try {
                appInfo = pm.getApplicationInfo(info.packageName, 0);
                info.applicationLabel = appInfo.loadLabel(pm);
            } catch (PackageManager.NameNotFoundException e) {
                info.applicationLabel = info.packageName;
            }
        }

        @Override
        protected void onPostExecute(List<ItemInfo> result) {
            mAdapter = new SpamAdapter(result);
            setListAdapter(mAdapter);
            mTask = null;
        }
    }

    private class SpamAdapter extends BaseAdapter {

        private static final int HEADER_TYPE = 0;
        private static final int ENTRY_TYPE = 1;
        private List<ItemInfo> mItems;

        SpamAdapter(List<ItemInfo> items) {
            mItems = items;
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public ItemInfo getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public int getItemViewType(int position) {
            return getItem(position) instanceof
                    PackageInfo ? HEADER_TYPE : ENTRY_TYPE;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        public void removeItem(int position) {
            ItemInfo item = mItems.get(position);
            Uri.Builder builder = new Uri.Builder();
            builder.scheme(ContentResolver.SCHEME_CONTENT);
            builder.authority(SpamFilter.AUTHORITY);
            builder.encodedPath(SpamFilter.MESSAGE_PATH);
            builder.appendEncodedPath(((NotificationInfo) item).id);
            getActivity().getContentResolver().delete(builder.build(), null, null);
            notifyDataSetChanged();
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return getItemViewType(position) == ENTRY_TYPE;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            int viewType = getItemViewType(position);
            TextView titleView = null;
            ItemInfo info = getItem(position);
            String text;
            if (viewType == HEADER_TYPE) {
                if (convertView == null) {
                    convertView = new TextView(getActivity(), null,
                            android.R.attr.listSeparatorTextViewStyle);
                }
                titleView = (TextView) convertView;
                text = (String) ((PackageInfo) info).applicationLabel;
            } else {
                if (convertView == null) {
                    convertView = View.inflate(getActivity(), R.layout.item_row, null);
                }
                titleView = ((TextView) convertView.findViewById(R.id.label));
                text = ((NotificationInfo) info).messageText;
            }
            titleView.setText(text);
            return convertView;
        }
    }
}
