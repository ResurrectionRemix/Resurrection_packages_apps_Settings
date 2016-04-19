package com.android.settings.rr;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.AdapterView.OnItemClickListener;

import com.android.settings.R;

public abstract class HAFRAppChooserDialog extends Dialog {

    final HAFRAppChooserAdapter dAdapter;
    final ProgressBar dProgressBar;
    final ListView dListView;
    final EditText dSearch;
    final ImageButton dButton;

    private int mId;

    public HAFRAppChooserDialog(Context context) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_app_chooser_list);

        dListView = (ListView) findViewById(R.id.listView1);
        dSearch = (EditText) findViewById(R.id.searchText);
        dButton = (ImageButton) findViewById(R.id.searchButton);
        dProgressBar = (ProgressBar) findViewById(R.id.progressBar1);

        dAdapter = new HAFRAppChooserAdapter(context) {
            @Override
            public void onStartUpdate() {
                dProgressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFinishUpdate() {
                dProgressBar.setVisibility(View.GONE);
            }
        };

        dListView.setAdapter(dAdapter);
        dListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> av, View v, int pos, long id) {
                HAFRAppChooserAdapter.AppItem info = (HAFRAppChooserAdapter.AppItem) av
                        .getItemAtPosition(pos);
                onListViewItemClick(info, mId);
                dismiss();
            }
        });

        dButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dAdapter.getFilter().filter(dSearch.getText().toString(), new Filter.FilterListener() {
                    public void onFilterComplete(int count) {
                        dAdapter.update();
                    }
                });
            }
        });

        dAdapter.update();
    }

    public void show(int id) {
        mId = id;
        show();
    }

    public abstract void onListViewItemClick(HAFRAppChooserAdapter.AppItem info, int id);
}
