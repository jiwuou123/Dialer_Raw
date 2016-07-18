/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.dialer.dialpad;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.text.TextUtils;
import android.util.Log;

import com.android.contacts.common.list.PhoneNumberListAdapter.PhoneQuery;
import com.android.contacts.common.util.PermissionsUtil;
import com.android.dialer.database.DialerDatabaseHelper;
import com.android.dialer.database.DialerDatabaseHelper.ContactNumber;
import com.android.dialer.database.DialerSearchHelper;
import com.android.dialer.list.DialpadSearchListAdapter;
import com.android.dialerbind.DatabaseHelperManager;

import java.util.ArrayList;

/**
 * Implements a Loader<Cursor> class to asynchronously load SmartDial search results.
 */
public class DialpadSearchCursorLoader extends AsyncTaskLoader<Cursor> {

    private final String TAG = DialpadSearchCursorLoader.class.getSimpleName();
    private final boolean DEBUG = false;

    private final Context mContext;

    private Cursor mCursor;

    private String mQuery;
//    private SmartDialNameMatcher mNameMatcher;

    private ForceLoadContentObserver mObserver;

    private DialerSearchHelper dialerSearchHelper;
    public DialpadSearchCursorLoader(Context context) {
        super(context);
        mContext = context;
        dialerSearchHelper = DialerSearchHelper.getInstance(context);
    }

    /**
     * Configures the query string to be used to find SmartDial matches.
     * @param query The query string user typed.
     */
    public void configureQuery(String query) {
        if (DEBUG) {
            Log.v(TAG, "Configure new query to be " + query);
        }
        mQuery = SmartDialNameMatcher.normalizeNumber(query, SmartDialPrefix.getMap());

        /** Constructs a name matcher object for matching names. */
//        mNameMatcher = new SmartDialNameMatcher(mQuery, SmartDialPrefix.getMap());
    }

    private String numberLeftToRight(String origin) {
        return TextUtils.isEmpty(origin) ? origin : '\u202D' + origin + '\u202C';
    }
    /**
     * Queries the SmartDial database and loads results in background.
     * @return Cursor of contacts that matches the SmartDial query.
     */
    @Override
    public Cursor loadInBackground() {
        if (DEBUG) {
            Log.v(TAG, "Load in background " + mQuery);
        }

        if (!PermissionsUtil.hasContactsPermissions(mContext)) {
            return new MatrixCursor(PhoneQuery.PROJECTION_PRIMARY);
        }

        /** Loads results from the database helper. */
        Cursor cursor = dialerSearchHelper.getSmartDialerSearchResults(mQuery);
//        final MatrixCursor matrixCursor = new MatrixCursor(DialpadSearchListAdapter.PhoneQueryEx.PROJECTION_PRIMARY);
//        Object[] row = new Object[DialpadSearchListAdapter.PhoneQueryEx.PROJECTION_PRIMARY.length];
//        if(cursor!=null&&cursor.moveToFirst()){
//            do{
//                row[PhoneQuery.PHONE_ID] = cursor.getInt(DialerSearchHelper.DialerSearchColumn.DATA_ID_INDEX);
//                row[PhoneQuery.PHONE_NUMBER] = cursor.getString(DialerSearchHelper.DialerSearchColumn.SEARCH_PHONE_NUMBER_INDEX);
//                row[PhoneQuery.CONTACT_ID] = cursor.getInt(DialerSearchHelper.DialerSearchColumn.CONTACT_ID_INDEX);
//                row[PhoneQuery.LOOKUP_KEY] = cursor.getString(DialerSearchHelper.DialerSearchColumn.CONTACT_NAME_LOOKUP_INDEX);
//                row[PhoneQuery.PHOTO_ID] =  cursor.getInt(DialerSearchHelper.DialerSearchColumn.PHOTO_ID_INDEX);
//                row[PhoneQuery.DISPLAY_NAME] = cursor.getString(DialerSearchHelper.DialerSearchColumn.NAME_INDEX);
//                row[DialpadSearchListAdapter.PhoneQueryEx.GEOCODED_LOCATION] = cursor.getString(DialerSearchHelper.DialerSearchColumn.CALL_GEOCODED_LOCATION_INDEX);
//                matrixCursor.addRow(row);
//            }while (cursor.moveToNext());
//        }
//        if(cursor!=null){
//            cursor.close();
//            cursor = null;
//        }

        return cursor;
    }

    @Override
    public void deliverResult(Cursor cursor) {
        if (isReset()) {
            /** The Loader has been reset; ignore the result and invalidate the data. */
            releaseResources(cursor);
            return;
        }

        /** Hold a reference to the old data so it doesn't get garbage collected. */
        Cursor oldCursor = mCursor;
        mCursor = cursor;

//        if (mObserver == null) {
//            mObserver = new ForceLoadContentObserver();
//            mContext.getContentResolver().registerContentObserver(
//                    DialerDatabaseHelper.SMART_DIAL_UPDATED_URI, true, mObserver);
//        }

        if (isStarted()) {
            /** If the Loader is in a started state, deliver the results to the client. */
            super.deliverResult(cursor);
        }

        /** Invalidate the old data as we don't need it any more. */
        if (oldCursor != null && oldCursor != cursor) {
            releaseResources(oldCursor);
        }
    }

    @Override
    protected void onStartLoading() {
        if (mCursor != null) {
            /** Deliver any previously loaded data immediately. */
            deliverResult(mCursor);
        }
        if (mCursor == null) {
            /** Force loads every time as our results change with queries. */
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        /** The Loader is in a stopped state, so we should attempt to cancel the current load. */
        cancelLoad();
    }

    @Override
    protected void onReset() {
        /** Ensure the loader has been stopped. */
        onStopLoading();

        if (mObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(mObserver);
            mObserver = null;
        }

        /** Release all previously saved query results. */
        if (mCursor != null) {
            releaseResources(mCursor);
            mCursor = null;
        }
    }

    @Override
    public void onCanceled(Cursor cursor) {
        super.onCanceled(cursor);

        if (mObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(mObserver);
            mObserver = null;
        }

        /** The load has been canceled, so we should release the resources associated with 'data'.*/
        releaseResources(cursor);
    }

    private void releaseResources(Cursor cursor) {
        if (cursor != null) {
            cursor.close();
        }
    }
}
