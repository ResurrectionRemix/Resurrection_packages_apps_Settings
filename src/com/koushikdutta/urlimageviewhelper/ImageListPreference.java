package com.koushikdutta.urlimageviewhelper;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.res.TypedArray;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;

import com.android.settings.R;

public class ImageListPreference extends ListPreference {
    private int[] resourceIds = null;

    private int mSummaryImageResourceId;
    /**
     * Constructor of the ImageListPreference. Initializes the custom images.
     * 
     * @param context application context.
     * @param attrs custom xml attributes.
     */
    public ImageListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray typedArray = context.obtainStyledAttributes(attrs,
                R.styleable.ImageListPreference);
        mSummaryImageResourceId = typedArray.getResourceId(R.styleable.ImageListPreference_summaryImage, 0);

        String[] imageNames = context.getResources().getStringArray(
                typedArray.getResourceId(typedArray.getIndexCount() - 1, -1));

        resourceIds = new int[imageNames.length];

        for (int i = 0; i < imageNames.length; i++) {
            String imageName = imageNames[i].substring(
                    imageNames[i].indexOf('/') + 1,
                    imageNames[i].lastIndexOf('.'));

            resourceIds[i] = context.getResources().getIdentifier(imageName,
                    null, context.getPackageName());
        }

        typedArray.recycle();
    }

    @Override
    protected View onCreateView(ViewGroup parent) {

        View layout = View.inflate(getContext(), R.layout.summary_image_preference, null);

        ImageView mSummaryImage = (ImageView) layout.findViewById(R.id.summary_image);
        mSummaryImage.setImageResource(mSummaryImageResourceId);

        return layout;
    }

    /**
     * {@inheritDoc}
     */
    protected void onPrepareDialogBuilder(Builder builder) {
        int index = findIndexOfValue(getSharedPreferences().getString(
                getKey(), "1"));

        ListAdapter listAdapter = new ImageArrayAdapter(getContext(),
                R.layout.image_list_preference, getEntries(), resourceIds, index);

        // Order matters.
        builder.setAdapter(listAdapter, this);
        super.onPrepareDialogBuilder(builder);
    }
}
