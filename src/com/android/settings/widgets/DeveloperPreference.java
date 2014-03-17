package com.android.settings.widgets;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.Display;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.R;
import com.koushikdutta.urlimageviewhelper.UrlImageViewCallback;
import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

import java.io.InputStream;
import java.net.HttpURLConnection;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
public class DeveloperPreference extends Preference {

    private static final String TAG = "DeveloperPreference";
    public static final String GRAVATAR_API = "http://www.gravatar.com/avatar/";
    public static int mDefaultAvatarSize = 200;
    private ImageView twitterButton;
    private ImageView donateButton;
    private ImageView photoView;

    private TextView devName;

    private String nameDev;
    private String twitterName;
    private String donateLink;
    private String devEmail;
    private final Display mDisplay;

    public DeveloperPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray typedArray = null;
        try {
            typedArray = context.obtainStyledAttributes(attrs, R.styleable.DeveloperPreference);
            nameDev = typedArray.getString(R.styleable.DeveloperPreference_nameDev);
            twitterName = typedArray.getString(R.styleable.DeveloperPreference_twitterHandle);
            donateLink = typedArray.getString(R.styleable.DeveloperPreference_donateLink);
            devEmail = typedArray.getString(R.styleable.DeveloperPreference_emailDev);
        } finally {
            if (typedArray != null) {
                typedArray.recycle();
            }
        }
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        mDisplay = wm.getDefaultDisplay();
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        super.onCreateView(parent);

        View layout = View.inflate(getContext(), R.layout.dev_card, null);

        twitterButton = (ImageView) layout.findViewById(R.id.twitter_button);
        donateButton = (ImageView) layout.findViewById(R.id.donate_button);
        devName = (TextView) layout.findViewById(R.id.name);
        photoView = (ImageView) layout.findViewById(R.id.photo);

        return layout;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        if (donateLink != null) {
            final OnClickListener openDonate = new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Uri donateURL = Uri.parse(donateLink);
                    final Intent intent = new Intent(Intent.ACTION_VIEW, donateURL);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    getContext().startActivity(intent);
                }
            };

            donateButton.setOnClickListener(openDonate);
        } else {
            donateButton.setVisibility(View.GONE);
        }


            final OnPreferenceClickListener openTwitter = new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

			if (twitterName != null) {

	                    Uri twitterURL = Uri.parse("http://twitter.com/#!/" + twitterName);
        	            final Intent intent = new Intent(Intent.ACTION_VIEW, twitterURL);
        	            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        	            getContext().startActivity(intent);

			}
        
	            return true;
                }
            };

            // changed to clicking the preference to open twitter
            // it was a hit or miss to click the twitter bird

            this.setOnPreferenceClickListener(openTwitter);
            UrlImageViewHelper.setUrlDrawable(this.photoView,
                    getGravatarUrl(devEmail),
                    R.drawable.ic_null,
                    UrlImageViewHelper.CACHE_DURATION_ONE_WEEK);

        if (twitterName == null)
		twitterButton.setVisibility(View.INVISIBLE);


        devName.setText(nameDev);

    }

    public String getGravatarUrl(String email) {
        try {
            Point point = new Point();
            mDisplay.getSize(point);
            mDefaultAvatarSize = point.x;
            String emailMd5 = getMd5(email.trim().toLowerCase());
            return String.format("%s%s?s=%d",
                    GRAVATAR_API,
                    emailMd5,
                    mDefaultAvatarSize);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private String getMd5(String devEmail) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(devEmail.getBytes());
        byte byteData[] = md.digest();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < byteData.length; i++)
            sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
        return sb.toString();
    }
}
