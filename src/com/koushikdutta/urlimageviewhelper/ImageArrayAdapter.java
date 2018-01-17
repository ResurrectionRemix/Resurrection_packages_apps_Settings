package com.koushikdutta.urlimageviewhelper;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;

import com.android.settings.R;

/**
 * The ImageArrayAdapter is the array adapter used for displaying an additional
 * image to a list preference item.
 * 
 * @author Casper Wakkers
 */
public class ImageArrayAdapter extends ArrayAdapter<CharSequence> {
    private int index = 0;
    private int[] resourceIds = null;

    /**
     * ImageArrayAdapter constructor.
     * 
     * @param context the context.
     * @param textViewResourceId resource id of the text view.
     * @param objects to be displayed.
     * @param ids resource id of the images to be displayed.
     * @param i index of the previous selected item.
     */
    public ImageArrayAdapter(Context context, int textViewResourceId,
            CharSequence[] objects, int[] ids, int i) {
        super(context, textViewResourceId, objects);

        index = i;
        resourceIds = ids;
    }

    /**
     * {@inheritDoc}
     */
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
        View row = inflater.inflate(R.layout.image_list_preference, parent, false);

        ImageView imageView = (ImageView) row.findViewById(R.id.image);
        imageView.setImageResource(resourceIds[position]);

        // CheckedTextView checkedTextView = (CheckedTextView)row.findViewById(
        // R.id.check);
        //
        // checkedTextView.setText(getItem(position));

        // if (position == index) {
        // checkedTextView.setChecked(true);
        // }

        return row;
    }
}
