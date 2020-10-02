/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.homepage;

import android.animation.LayoutTransition;
import android.app.ActivityManager;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.database.ContentObserver;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.os.UserManager;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toolbar;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.internal.util.UserIcons;

import com.android.settings.R;
import com.android.settings.accounts.AvatarViewMixin;
import com.android.settings.core.HideNonSystemOverlayMixin;
import com.android.settings.homepage.contextualcards.ContextualCardsFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.drawable.CircleFramedDrawable;
import com.airbnb.lottie.LottieAnimationView;

import android.provider.Settings;
import com.android.internal.util.rr.ImageHelperQS;
import com.android.internal.util.rr.RRFontHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class SettingsHomepageActivity extends FragmentActivity {
    private static final String TAG = "SettingsHomepageActivity";
    Context context;
    ImageView avatarView;
    UserManager mUserManager;
    View homepageSpacer;
    View homepageMainLayout;
    ImageView iv;
    ImageView mCustomImage;
    Drawable mStockDrawable;
    private static final String SPACER_IMAGE = "custom_spacer_image";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.settings_homepage_container);
        final View root = findViewById(R.id.settings_homepage_container);
        root.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        setHomepageContainerPaddingTop();

        context = getApplicationContext();

        mUserManager = context.getSystemService(UserManager.class);

        final Toolbar toolbar = findViewById(R.id.search_action_bar);
        FeatureFactory.getFactory(this).getSearchFeatureProvider()
                .initSearchToolbar(this /* activity */, toolbar, SettingsEnums.SETTINGS_HOMEPAGE);

        avatarView = root.findViewById(R.id.account_avatar);
        //final AvatarViewMixin avatarViewMixin = new AvatarViewMixin(this, avatarView);
        avatarView.setImageDrawable(getCircularUserIcon(context));
        avatarView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setComponent(new ComponentName("com.android.settings","com.android.settings.Settings$UserSettingsActivity"));
                startActivity(intent);
            }
        });
        //getLifecycle().addObserver(avatarViewMixin);

        if (!getSystemService(ActivityManager.class).isLowRamDevice()) {
            // Only allow contextual feature on high ram devices.
            showFragment(new ContextualCardsFragment(), R.id.contextual_cards_content);
        }
        showFragment(new TopLevelSettings(), R.id.main_content);
        ((FrameLayout) findViewById(R.id.main_content))
                .getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

        SettingsObserver observer = new SettingsObserver(new Handler(Looper.getMainLooper()));
        observer.observe();
        recreateSpacer();
    }

    private void recreateSpacer() {
       homepageSpacer = findViewById(R.id.settings_homepage_spacer);
        homepageMainLayout = findViewById(R.id.main_content_scrollable_container);
        LottieAnimationView view = homepageSpacer.findViewById(R.id.home_animation);
        TextView tv = homepageSpacer.findViewById(R.id.spacer_text);
        iv = homepageSpacer.findViewById(R.id.spacer_image);
        mCustomImage = homepageSpacer.findViewById(R.id.custom_image);
        mStockDrawable = context.getDrawable(R.drawable.rr_spacer);
        Drawable rrDrawable = context.getDrawable(R.drawable.rr_spacer_main_icon);
        Drawable rrDrawable2 = context.getDrawable(R.drawable.rr_main_conf_shortcut_icon_primay);
        Drawable rrDrawable3 = context.getDrawable(R.drawable.rr_main_conf_shortcut_icon);

        try {
            RRFontHelper.setFontType(tv, getFontStyle());
            tv.setTextSize(getFontSize());
            if (configAnim() == 0) {
                 if (isProfileAvatar() == 1) {
                     homepageSpacer.setBackground(null);
                     mCustomImage.setImageDrawable(null);
                     mCustomImage.setVisibility(View.GONE);
                     iv.setVisibility(View.VISIBLE);
                     iv.setImageDrawable(getCircularUserIcon(context));
                     iv.setOnClickListener(new View.OnClickListener() {
                         @Override
                         public void onClick(View v) {
                            Intent intent = new Intent(Intent.ACTION_MAIN);
                            intent.setComponent(new ComponentName("com.android.settings","com.android.settings.Settings$UserSettingsActivity"));
                            startActivity(intent);
                         }
                     });
                 } else if (isProfileAvatar() == 0) {
                     homepageSpacer.setBackground(null);
                     mCustomImage.setImageDrawable(null);
                     mCustomImage.setVisibility(View.GONE);
                     iv.setVisibility(View.VISIBLE);
                     iv.setClickable(false);
                     if (mStockDrawable != null) {
                         iv.setImageDrawable(mStockDrawable);
                     }
                 } else if (isProfileAvatar() == 2) {
                     homepageSpacer.setBackground(null);
                     mCustomImage.setImageDrawable(null);
                     mCustomImage.setVisibility(View.GONE);
                     iv.setVisibility(View.VISIBLE);
                     if (rrDrawable != null) {
                         iv.setImageDrawable(rrDrawable);
                     }
                     iv.setOnClickListener(new View.OnClickListener() {
                         @Override
                         public void onClick(View v) {
                            Intent intent = new Intent(Intent.ACTION_MAIN);
                            intent.setComponent(new ComponentName("com.android.settings","com.android.settings.Settings$MainSettingsLayoutActivity"));
                            startActivity(intent);
                         }
                     });
                 } else if (isProfileAvatar() == 3) {

                     iv.setVisibility(View.GONE);
                     BitmapDrawable bp = getCustomImageFromString(SPACER_IMAGE, context);
                     if (bp == null) Log.d(TAG,"Bitmap is null!!");
                     if (bp != null)  {
                         if (isCrop() == 0){             
                             homepageSpacer.setBackground(bp);
                             mCustomImage.setVisibility(View.GONE);
                          } else {
                             homepageSpacer.setBackground(null);
                             mCustomImage.setImageDrawable(bp);
                             mCustomImage.setVisibility(View.VISIBLE);
                          }
                     }
                 } else if (isProfileAvatar() == 4) {
                     homepageSpacer.setBackground(null);
                     mCustomImage.setImageDrawable(null);
                     mCustomImage.setVisibility(View.GONE);
                     iv.setVisibility(View.VISIBLE);
                     if (rrDrawable2 != null) {
                         iv.setImageDrawable(rrDrawable2);
                     }
                     iv.setOnClickListener(new View.OnClickListener() {
                         @Override
                         public void onClick(View v) {
                            Intent intent = new Intent(Intent.ACTION_MAIN);
                            intent.setComponent(new ComponentName("com.android.settings","com.android.settings.Settings$MainSettingsLayoutActivity"));
                            startActivity(intent);
                         }
                     });
                 } else if (isProfileAvatar() == 5) {
                     homepageSpacer.setBackground(null);
                     mCustomImage.setImageDrawable(null);
                     mCustomImage.setVisibility(View.GONE);
                     iv.setVisibility(View.VISIBLE);
                     if (rrDrawable3 != null) {
                         iv.setImageDrawable(rrDrawable3);
                     }
                     iv.setOnClickListener(new View.OnClickListener() {
                         @Override
                         public void onClick(View v) {
                            Intent intent = new Intent(Intent.ACTION_MAIN);
                            intent.setComponent(new ComponentName("com.android.settings","com.android.settings.Settings$MainSettingsLayoutActivity"));
                            startActivity(intent);
                         }
                     });
                 }
                 tv.setVisibility(View.GONE);
                 view.setVisibility(View.GONE);
            } else if (configAnim() == 1) {
                 homepageSpacer.setBackground(null);
                 mCustomImage.setImageDrawable(null);
                 mCustomImage.setVisibility(View.GONE);
                 iv.setVisibility(View.GONE);
                 tv.setVisibility(View.GONE);
                 view.setVisibility(View.VISIBLE);
            } else if (configAnim() == 2) {
                 mCustomImage.setImageDrawable(null);
                 homepageSpacer.setBackground(null);
                 mCustomImage.setVisibility(View.GONE);
                 iv.setVisibility(View.GONE);
                 tv.setVisibility(View.VISIBLE);
                 view.setVisibility(View.GONE);
            } else if (configAnim() == 3) {
                 homepageSpacer.setBackground(null);
                 mCustomImage.setImageDrawable(null);
                 mCustomImage.setVisibility(View.GONE);
                 iv.setVisibility(View.GONE);
                 tv.setVisibility(View.GONE);
                 view.setVisibility(View.GONE);
            }
            if (avatarView != null) {
                avatarView.setVisibility(isSearchDisabled()? View.GONE: View.VISIBLE);
             }
        } catch (Exception e) {}
        if (!isHomepageSpacerEnabled() && homepageSpacer != null && homepageMainLayout != null) {
            homepageSpacer.setVisibility(View.GONE);
            setMargins(homepageMainLayout, 0,0,0,0);
        }
    }



    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = context.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.SETTINGS_SPACER), false, this,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.SETTINGS_SPACER_IMAGE_CROP), false, this,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.SETTINGS_SPACER_IMAGE_STYLE), false, this,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.SETTINGS_DISPLAY_ANIM), false, this,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.SETTINGS_SPACER_FONT_STYLE), false, this,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.SETTINGS_SPACER_STYLE), false, this,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.SETTINGS_SPACER_IMAGE_SEARCHBAR), false, this,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.SETTINGS_SPACER_CUSTOM), false, this,
                    UserHandle.USER_ALL);
        }

        @Override
        public void onChange(boolean selfChange) {
            recreateSpacer();
        }
    }


    private void showFragment(Fragment fragment, int id) {
        final FragmentManager fragmentManager = getSupportFragmentManager();
        final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        final Fragment showFragment = fragmentManager.findFragmentById(id);

        if (showFragment == null) {
            fragmentTransaction.add(id, fragment);
        } else {
            fragmentTransaction.show(showFragment);
        }
        fragmentTransaction.commit();
    }

    public void saveCustomFileFromString(Uri fileUri, String fileName, Context mContext) {
        try {
            final InputStream fileStream = mContext.getContentResolver().openInputStream(fileUri);
            File file = new File(mContext.getFilesDir(), fileName);
            if (file.exists()) {
                file.delete();
            }
            FileOutputStream output = new FileOutputStream(file);
            byte[] buffer = new byte[8 * 1024];
            int read;
            while ((read = fileStream.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            output.flush();
        } catch (IOException e) {
        }
    }

    public BitmapDrawable getCustomImageFromString(String fileName, Context mContext) {
        String imageUri = Settings.System.getStringForUser(mContext.getContentResolver(),
                Settings.System.SETTINGS_SPACER_CUSTOM,
                UserHandle.USER_CURRENT);
        if (imageUri != null) {
            saveCustomFileFromString(Uri.parse(imageUri), SPACER_IMAGE, mContext);
        }
        BitmapDrawable mImage = null;
        File file = new File(mContext.getFilesDir(), fileName);
        if (file.exists()) {
            final Bitmap image = BitmapFactory.decodeFile(file.getAbsolutePath());
            mImage = new BitmapDrawable(mContext.getResources(), ImageHelperQS.resizeMaxDeviceSize(mContext, image));
        }
        return mImage;
    }

    private boolean isHomepageSpacerEnabled() {
        return Settings.System.getInt(this.getContentResolver(),
        Settings.System.SETTINGS_SPACER, 0) != 0;
    }

    private int isCrop() {
        return Settings.System.getInt(this.getContentResolver(),
        Settings.System.SETTINGS_SPACER_IMAGE_CROP, 1);
    }

    private int isProfileAvatar() {
        return Settings.System.getInt(this.getContentResolver(),
        Settings.System.SETTINGS_SPACER_IMAGE_STYLE, 0);
    }

    private boolean isSearchDisabled() {
        return Settings.System.getInt(this.getContentResolver(),
        Settings.System.SETTINGS_SPACER_IMAGE_SEARCHBAR, 0) == 1;
    }

    private int configAnim() {
         return Settings.System.getInt(this.getContentResolver(),
                Settings.System.SETTINGS_SPACER_STYLE, 0);
    }

    private int getFontStyle() {
         return Settings.System.getInt(this.getContentResolver(),
                Settings.System.SETTINGS_SPACER_FONT_STYLE, 0);
    }

    private int getFontSize() {
         return Settings.System.getInt(this.getContentResolver(),
                Settings.System.SETTINGS_DISPLAY_ANIM, 40);
    }

    private static void setMargins (View v, int l, int t, int r, int b) {
        if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            p.setMargins(l, t, r, b);
            v.requestLayout();
        }
    }


    @VisibleForTesting
    void setHomepageContainerPaddingTop() {
        final View view = this.findViewById(R.id.homepage_container);

        final int searchBarHeight = getResources().getDimensionPixelSize(R.dimen.search_bar_height);
        final int searchBarMargin = getResources().getDimensionPixelSize(R.dimen.search_bar_margin);

        // The top padding is the height of action bar(48dp) + top/bottom margins(16dp)
        final int paddingTop = searchBarHeight + searchBarMargin * 2;
        view.setPadding(0 /* left */, paddingTop, 0 /* right */, 0 /* bottom */);
    }

    private Drawable getCircularUserIcon(Context context) {
        Bitmap bitmapUserIcon = mUserManager.getUserIcon(UserHandle.myUserId());

        if (bitmapUserIcon == null) {
            // get default user icon.
            final Drawable defaultUserIcon = UserIcons.getDefaultUserIcon(
                    context.getResources(), UserHandle.myUserId(), false);
            bitmapUserIcon = UserIcons.convertToBitmap(defaultUserIcon);
        }
        Drawable drawableUserIcon = new CircleFramedDrawable(bitmapUserIcon,
                (int) context.getResources().getDimension(R.dimen.circle_avatar_size));

        return drawableUserIcon;
    }

    @Override
    public void onResume() {
        super.onResume();
        avatarView.setImageDrawable(getCircularUserIcon(getApplicationContext()));
        if (configAnim() == 0 && isProfileAvatar() == 1 && isHomepageSpacerEnabled()) {
            if (iv != null) {
                iv.setImageDrawable(getCircularUserIcon(getApplicationContext()));
            }
        }
    }
}
