/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.rr.navigation;

import static androidx.annotation.RestrictTo.Scope.GROUP_ID;

import android.content.Context;
import androidx.annotation.RestrictTo;
import android.os.Parcelable;
import android.view.ViewGroup;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuItemImpl;
import androidx.appcompat.view.menu.MenuPresenter;
import androidx.appcompat.view.menu.MenuView;
import androidx.appcompat.view.menu.SubMenuBuilder;

/**
 * @hide
 */
@RestrictTo(GROUP_ID)
public class BottomNavigationPresenter implements MenuPresenter {
    private MenuBuilder mMenu;
    private BottomNavigationMenuCustom mMenuView;
    private boolean mUpdateSuspended = false;

    public void setBottomNavigationMenuView(BottomNavigationMenuCustom menuView) {
        mMenuView = menuView;
    }

    @Override
    public void initForMenu(Context context, MenuBuilder menu) {
        mMenuView.initialize(mMenu);
        mMenu = menu;
    }

    @Override
    public MenuView getMenuView(ViewGroup root) {
        return mMenuView;
    }

    @Override
    public void updateMenuView(boolean cleared) {
        if (mUpdateSuspended) return;
        if (cleared) {
            mMenuView.buildMenuView();
        } else {
            mMenuView.updateMenuView();
        }
    }

    @Override
    public void setCallback(Callback cb) {}

    @Override
    public boolean onSubMenuSelected(SubMenuBuilder subMenu) {
        return false;
    }

    @Override
    public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {}

    @Override
    public boolean flagActionItems() {
        return false;
    }

    @Override
    public boolean expandItemActionView(MenuBuilder menu, MenuItemImpl item) {
        return false;
    }

    @Override
    public boolean collapseItemActionView(MenuBuilder menu, MenuItemImpl item) {
        return false;
    }

    @Override
    public int getId() {
        return -1;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        return null;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {}

    public void setUpdateSuspended(boolean updateSuspended) {
        mUpdateSuspended = updateSuspended;
    }
}
