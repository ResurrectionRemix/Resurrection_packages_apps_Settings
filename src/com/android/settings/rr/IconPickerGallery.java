package com.android.settings.rr;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;

/**
 * So we can capture image selection in DUSystemReceiver
 */
public class IconPickerGallery extends Activity {

    public static final String INTENT_GALLERY_PICKER = "intent_gallery_picker";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        startActivityForResult(intent, 69);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == Activity.RESULT_OK && requestCode == 69) {
            Intent resultIntent = new Intent();
            resultIntent.setAction(INTENT_GALLERY_PICKER);
            resultIntent.putExtra("result", Activity.RESULT_OK);
            resultIntent.putExtra("uri", data.getData().toString());
            sendBroadcastAsUser(resultIntent, UserHandle.CURRENT);
            setResult(RESULT_OK, resultIntent);
            finish();
        } else {
            sendCancelResultAndFinish();
        }


    }


    private void sendCancelResultAndFinish() {
        Intent intent = new Intent(INTENT_GALLERY_PICKER);
        intent.putExtra("result", Activity.RESULT_CANCELED);
        sendBroadcastAsUser(intent, UserHandle.CURRENT);
        setResult(Activity.RESULT_CANCELED);
        finish();
    }

}
