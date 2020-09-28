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
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
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
import com.android.internal.util.rr.RRFontHelper;
public class SettingsHomepageActivity extends FragmentActivity {

    Context context;
    ImageView avatarView;
    UserManager mUserManager;
    View homepageSpacer;
    View homepageMainLayout;
    ImageView iv;
    Drawable mStockDrawable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.settings_homepage_container);
        final View root = findViewById(R.id.settings_homepage_container);
        root.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        setHomepageContainerPaddingTop();

        Context context = getApplicationContext();

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

        homepageSpacer = findViewById(R.id.settings_homepage_spacer);
        homepageMainLayout = findViewById(R.id.main_content_scrollable_container);
        LottieAnimationView view = homepageSpacer.findViewById(R.id.home_animation);
        TextView tv = homepageSpacer.findViewById(R.id.spacer_text);
        iv = homepageSpacer.findViewById(R.id.spacer_image);
        mStockDrawable = context.getDrawable(R.drawable.rr_spacer);
        try {
            RRFontHelper.setFontType(tv, getFontStyle());
            tv.setTextSize(getFontSize());
            if (configAnim() == 0) {
                 iv.setVisibility(View.VISIBLE);
                 if (isProfileAvatar()) {
                     iv.setImageDrawable(getCircularUserIcon(context));
                     iv.setOnClickListener(new View.OnClickListener() {
                         @Override
                         public void onClick(View v) {
                            Intent intent = new Intent(Intent.ACTION_MAIN);
                            intent.setComponent(new ComponentName("com.android.settings","com.android.settings.Settings$UserSettingsActivity"));
                            startActivity(intent);
                         }
                     });
                 } else {
                     if (mStockDrawable != null) {
                         iv.setImageDrawable(mStockDrawable);
                     }
                 }
                 tv.setVisibility(View.GONE);
                 view.setVisibility(View.GONE);
            } else if (configAnim() == 1) {
                 iv.setVisibility(View.GONE);
                 tv.setVisibility(View.GONE);
                 view.setVisibility(View.VISIBLE);
            } else if (configAnim() == 2) {
                 iv.setVisibility(View.GONE);
                 tv.setVisibility(View.VISIBLE);
                 view.setVisibility(View.GONE);
            } else if (configAnim() == 3) {
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

    private boolean isHomepageSpacerEnabled() {
        return Settings.System.getInt(this.getContentResolver(),
        Settings.System.SETTINGS_SPACER, 0) != 0;
    }

    private boolean isProfileAvatar() {
        return Settings.System.getInt(this.getContentResolver(),
        Settings.System.SETTINGS_SPACER_IMAGE_STYLE, 0) == 1;
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
        if (iv != null & configAnim() == 0) {
           if (isProfileAvatar()) {
               iv.setImageDrawable(getCircularUserIcon(getApplicationContext()));
           } else {
               if (mStockDrawable != null) {
                  iv.setImageDrawable(mStockDrawable);
               }
           }
       }
    }
}
