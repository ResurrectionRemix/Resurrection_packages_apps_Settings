/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.settings.inputmethod;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.UserDictionary;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.android.settings.R;
import com.android.settings.UserDictionarySettings;
import com.android.settings.Utils;

import java.util.ArrayList;
import java.util.Locale;
import java.util.TreeSet;

/**
 * A container class to factor common code to UserDictionaryAddWordFragment
 * and UserDictionaryAddWordActivity.
 */
public class UserDictionaryAddWordContents {
    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_WORD = "word";
    public static final String EXTRA_SHORTCUT = "shortcut";
    public static final String EXTRA_LOCALE = "locale";
    public static final String EXTRA_ORIGINAL_WORD = "originalWord";
    public static final String EXTRA_ORIGINAL_SHORTCUT = "originalShortcut";

    public static final int MODE_EDIT = 0;
    public static final int MODE_INSERT = 1;

    private static final int FREQUENCY_FOR_USER_DICTIONARY_ADDS = 250;

    private final int mMode; // Either MODE_EDIT or MODE_INSERT
    private final EditText mWordEditText;
    private final EditText mShortcutEditText;
    private String mLocale;
    private final String mOldWord;
    private final String mOldShortcut;
    private String mSavedWord;
    private String mSavedShortcut;

    /* package */ UserDictionaryAddWordContents(final View view, final Bundle args) {
        mWordEditText = (EditText)view.findViewById(R.id.user_dictionary_add_word_text);
        mShortcutEditText = (EditText)view.findViewById(R.id.user_dictionary_add_shortcut);
        final String word = args.getString(EXTRA_WORD);
        if (null != word) {
            mWordEditText.setText(word);
            // Use getText in case the edit text modified the text we set. This happens when
            // it's too long to be edited.
            mWordEditText.setSelection(mWordEditText.getText().length());
        }
        final String shortcut = args.getString(EXTRA_SHORTCUT);
        if (null != shortcut && null != mShortcutEditText) {
            mShortcutEditText.setText(shortcut);
        }
        mMode = args.getInt(EXTRA_MODE); // default return value for #getInt() is 0 = MODE_EDIT
        mOldWord = args.getString(EXTRA_WORD);
        mOldShortcut = args.getString(EXTRA_SHORTCUT);
        updateLocale(args.getString(EXTRA_LOCALE));
    }

    /* package */ UserDictionaryAddWordContents(final View view,
            final UserDictionaryAddWordContents oldInstanceToBeEdited) {
        mWordEditText = (EditText)view.findViewById(R.id.user_dictionary_add_word_text);
        mShortcutEditText = (EditText)view.findViewById(R.id.user_dictionary_add_shortcut);
        mMode = MODE_EDIT;
        mOldWord = oldInstanceToBeEdited.mSavedWord;
        mOldShortcut = oldInstanceToBeEdited.mSavedShortcut;
        updateLocale(oldInstanceToBeEdited.getCurrentUserDictionaryLocale());
    }

    // locale may be null, this means default locale
    // It may also be the empty string, which means "all locales"
    /* package */ void updateLocale(final String locale) {
        mLocale = null == locale ? Locale.getDefault().toString() : locale;
    }

    /* package */ void saveStateIntoBundle(final Bundle outState) {
        outState.putString(EXTRA_WORD, mWordEditText.getText().toString());
        outState.putString(EXTRA_ORIGINAL_WORD, mOldWord);
        if (null != mShortcutEditText) {
            outState.putString(EXTRA_SHORTCUT, mShortcutEditText.getText().toString());
        }
        if (null != mOldShortcut) {
            outState.putString(EXTRA_ORIGINAL_SHORTCUT, mOldShortcut);
        }
        outState.putString(EXTRA_LOCALE, mLocale);
    }

    /* package */ void delete(final Context context) {
        if (MODE_EDIT == mMode && !TextUtils.isEmpty(mOldWord)) {
            // Mode edit: remove the old entry.
            final ContentResolver resolver = context.getContentResolver();
            UserDictionarySettings.deleteWord(mOldWord, mOldShortcut, resolver);
        }
        // If we are in add mode, nothing was added, so we don't need to do anything.
    }

    /* package */ int apply(final Context context, final Bundle outParameters) {
        if (null != outParameters) saveStateIntoBundle(outParameters);
        final ContentResolver resolver = context.getContentResolver();
        if (MODE_EDIT == mMode && !TextUtils.isEmpty(mOldWord)) {
            // Mode edit: remove the old entry.
            UserDictionarySettings.deleteWord(mOldWord, mOldShortcut, resolver);
        }
        final String newWord = mWordEditText.getText().toString();
        final String newShortcut;
        if (null == mShortcutEditText) {
            newShortcut = null;
        } else {
            final String tmpShortcut = mShortcutEditText.getText().toString();
            if (TextUtils.isEmpty(tmpShortcut)) {
                newShortcut = null;
            } else {
                newShortcut = tmpShortcut;
            }
        }
        if (TextUtils.isEmpty(newWord)) {
            // If the word is somehow empty, don't insert it.
            return UserDictionaryAddWordActivity.CODE_CANCEL;
        }
        mSavedWord = newWord;
        mSavedShortcut = newShortcut;
        // If there is no shortcut, and the word already exists in the database, then we
        // should not insert, because either A. the word exists with no shortcut, in which
        // case the exact same thing we want to insert is already there, or B. the word
        // exists with at least one shortcut, in which case it has priority on our word.
        if (TextUtils.isEmpty(newShortcut) && hasWord(newWord, context)) {
            return UserDictionaryAddWordActivity.CODE_ALREADY_PRESENT;
        }

        // Disallow duplicates. If the same word with no shortcut is defined, remove it; if
        // the same word with the same shortcut is defined, remove it; but we don't mind if
        // there is the same word with a different, non-empty shortcut.
        UserDictionarySettings.deleteWord(newWord, null, resolver);
        if (!TextUtils.isEmpty(newShortcut)) {
            // If newShortcut is empty we just deleted this, no need to do it again
            UserDictionarySettings.deleteWord(newWord, newShortcut, resolver);
        }

        // In this class we use the empty string to represent 'all locales' and mLocale cannot
        // be null. However the addWord method takes null to mean 'all locales'.
        UserDictionary.Words.addWord(context, newWord.toString(),
                FREQUENCY_FOR_USER_DICTIONARY_ADDS, newShortcut,
                TextUtils.isEmpty(mLocale) ? null : Utils.createLocaleFromString(mLocale));

        return UserDictionaryAddWordActivity.CODE_WORD_ADDED;
    }

    private static final String[] HAS_WORD_PROJECTION = { UserDictionary.Words.WORD };
    private static final String HAS_WORD_SELECTION_ONE_LOCALE = UserDictionary.Words.WORD
            + "=? AND " + UserDictionary.Words.LOCALE + "=?";
    private static final String HAS_WORD_SELECTION_ALL_LOCALES = UserDictionary.Words.WORD
            + "=? AND " + UserDictionary.Words.LOCALE + " is null";
    private boolean hasWord(final String word, final Context context) {
        final Cursor cursor;
        // mLocale == "" indicates this is an entry for all languages. Here, mLocale can't
        // be null at all (it's ensured by the updateLocale method).
        if ("".equals(mLocale)) {
            cursor = context.getContentResolver().query(UserDictionary.Words.CONTENT_URI,
                      HAS_WORD_PROJECTION, HAS_WORD_SELECTION_ALL_LOCALES,
                      new String[] { word }, null /* sort order */);
        } else {
            cursor = context.getContentResolver().query(UserDictionary.Words.CONTENT_URI,
                      HAS_WORD_PROJECTION, HAS_WORD_SELECTION_ONE_LOCALE,
                      new String[] { word, mLocale }, null /* sort order */);
        }
        try {
            if (null == cursor) return false;
            return cursor.getCount() > 0;
        } finally {
            if (null != cursor) cursor.close();
        }
    }

    public static class LocaleRenderer {
        private final String mLocaleString;
        private final String mDescription;
        // LocaleString may NOT be null.
        public LocaleRenderer(final Context context, final String localeString) {
            mLocaleString = localeString;
            if (null == localeString) {
                mDescription = context.getString(R.string.user_dict_settings_more_languages);
            } else if ("".equals(localeString)) {
                mDescription = context.getString(R.string.user_dict_settings_all_languages);
            } else {
                mDescription = Utils.createLocaleFromString(localeString).getDisplayName();
            }
        }
        @Override
        public String toString() {
            return mDescription;
        }
        public String getLocaleString() {
            return mLocaleString;
        }
        // "More languages..." is null ; "All languages" is the empty string.
        public boolean isMoreLanguages() {
            return null == mLocaleString;
        }
    }

    private static void addLocaleDisplayNameToList(final Context context,
            final ArrayList<LocaleRenderer> list, final String locale) {
        if (null != locale) {
            list.add(new LocaleRenderer(context, locale));
        }
    }

    // Helper method to get the list of locales to display for this word
    public ArrayList<LocaleRenderer> getLocalesList(final Activity activity) {
        final TreeSet<String> locales = UserDictionaryList.getUserDictionaryLocalesSet(activity);
        // Remove our locale if it's in, because we're always gonna put it at the top
        locales.remove(mLocale); // mLocale may not be null
        final String systemLocale = Locale.getDefault().toString();
        // The system locale should be inside. We want it at the 2nd spot.
        locales.remove(systemLocale); // system locale may not be null
        locales.remove(""); // Remove the empty string if it's there
        final ArrayList<LocaleRenderer> localesList = new ArrayList<LocaleRenderer>();
        // Add the passed locale, then the system locale at the top of the list. Add an
        // "all languages" entry at the bottom of the list.
        addLocaleDisplayNameToList(activity, localesList, mLocale);
        if (!systemLocale.equals(mLocale)) {
            addLocaleDisplayNameToList(activity, localesList, systemLocale);
        }
        for (final String l : locales) {
            // TODO: sort in unicode order
            addLocaleDisplayNameToList(activity, localesList, l);
        }
        if (!"".equals(mLocale)) {
            // If mLocale is "", then we already inserted the "all languages" item, so don't do it
            addLocaleDisplayNameToList(activity, localesList, ""); // meaning: all languages
        }
        localesList.add(new LocaleRenderer(activity, null)); // meaning: select another locale
        return localesList;
    }

    public String getCurrentUserDictionaryLocale() {
        return mLocale;
    }
}
