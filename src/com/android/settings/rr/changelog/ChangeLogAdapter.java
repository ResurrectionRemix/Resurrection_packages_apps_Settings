package com.android.settings.rr.changelog;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import com.android.settings.R;

class ChangeLogAdapter extends RecyclerView.Adapter<ChangeLogAdapter.ViewHolder> {

    private List<ChangelogItem> mChangelogItems;
    private Context mContext;

    @Override
    public ChangeLogAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View changeLogView = inflater.inflate(R.layout.changelog_item, parent, false);

        return new ViewHolder(changeLogView);
    }

    @Override
    public void onBindViewHolder(ChangeLogAdapter.ViewHolder viewHolder, int position) {
        ChangelogItem changelogItem = mChangelogItems.get(position);
        TextView commitID = viewHolder.commitID;
        TextView commitMessage = viewHolder.commitMessage;

        commitID.setText(changelogItem.getCommit_id());
        if (changelogItem.getCommit_message() != null) {
            commitID.setTextAlignment(View.TEXT_ALIGNMENT_INHERIT);
            commitMessage.setVisibility(View.VISIBLE);
            commitMessage.setText(changelogItem.getCommit_message());
            commitID.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getContext().getResources().getDimension(R.dimen.changelog_header_small));
            commitID.setTypeface(Typeface.DEFAULT_BOLD);

            int[] attrs = new int[] {
                    android.R.attr.textColorPrimary,
            };
            TypedArray ta = getContext().getTheme().obtainStyledAttributes(attrs);
            commitID.setTextColor(ta.getColor(0, Color.GRAY));
            ta.recycle();
        } else {
            commitID.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            commitMessage.setVisibility(View.GONE);
            commitID.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getContext().getResources().getDimension(R.dimen.changelog_header_big));
            commitID.setTypeface(Typeface.DEFAULT);

            int[] attrs = new int[] {
                    R.attr.colorAccent,
            };
            TypedArray ta = getContext().getTheme().obtainStyledAttributes(attrs);
            commitID.setTextColor(ta.getColor(0, Color.GRAY));
            ta.recycle();
        }
    }

    @Override
    public int getItemCount() {
        return mChangelogItems.size();
    }

    ChangeLogAdapter(Context context, List<ChangelogItem> changelogItems) {
        mChangelogItems = changelogItems;
        mContext = context;
    }

    private Context getContext() {
        return mContext;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView commitID;
        TextView commitMessage;

        ViewHolder(View itemView) {
            super(itemView);
            commitID = (TextView) itemView.findViewById(R.id.commit_id);
            commitMessage = (TextView) itemView.findViewById(R.id.commit_message);
        }
    }
}
