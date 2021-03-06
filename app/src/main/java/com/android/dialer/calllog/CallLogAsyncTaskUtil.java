/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.dialer.calllog;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.VoicemailContract.Voicemails;
import android.telecom.PhoneAccountHandle;
import android.text.TextUtils;
import android.util.Log;

import com.android.contacts.common.GeoUtil;
import com.android.dialer.PhoneCallDetails;
import com.android.dialer.util.AsyncTaskExecutor;
import com.android.dialer.util.AsyncTaskExecutors;
import com.android.dialer.util.PhoneNumberUtil;
import com.android.dialer.util.TelecomUtil;
import com.android.incallui.CallerInfo;
import com.google.common.annotations.VisibleForTesting;

import java.io.IOException;

public class CallLogAsyncTaskUtil {
    private static String TAG = CallLogAsyncTaskUtil.class.getSimpleName();

    /** The enumeration of {@link AsyncTask} objects used in this class. */
    public enum Tasks {
        DELETE_VOICEMAIL,
        DELETE_CALL,
        MARK_VOICEMAIL_READ,
        GET_CALL_DETAILS,
        GET_CONTACT_DETAILS,
    }

    private static class CallDetailQuery {
        static final String[] CALL_LOG_PROJECTION = new String[]{
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.COUNTRY_ISO,
                CallLog.Calls.GEOCODED_LOCATION,
                CallLog.Calls.NUMBER_PRESENTATION,
                CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME,
                CallLog.Calls.PHONE_ACCOUNT_ID,
                CallLog.Calls.FEATURES,
                CallLog.Calls.DATA_USAGE,
                CallLog.Calls.TRANSCRIPTION,
                "raw_contact_id",
                CallLog.Calls.CACHED_LOOKUP_URI,
        };

        static final int DATE_COLUMN_INDEX = 0;
        static final int DURATION_COLUMN_INDEX = 1;
        static final int NUMBER_COLUMN_INDEX = 2;
        static final int CALL_TYPE_COLUMN_INDEX = 3;
        static final int COUNTRY_ISO_COLUMN_INDEX = 4;
        static final int GEOCODED_LOCATION_COLUMN_INDEX = 5;
        static final int NUMBER_PRESENTATION_COLUMN_INDEX = 6;
        static final int ACCOUNT_COMPONENT_NAME = 7;
        static final int ACCOUNT_ID = 8;
        static final int FEATURES = 9;
        static final int DATA_USAGE = 10;
        static final int TRANSCRIPTION_COLUMN_INDEX = 11;
        static final int RAW_CONTACT_ID_INDEX = 12;
        static final int CACHED_LOOKUP_URI_INDEX = 13;
    }

    public interface CallLogAsyncTaskListener {
        public void onDeleteCall();

        public void onDeleteVoicemail();

        public void onGetCallDetails(PhoneCallDetails[] details);
    }

    private static AsyncTaskExecutor sAsyncTaskExecutor;

    private static void initTaskExecutor() {
        sAsyncTaskExecutor = AsyncTaskExecutors.createThreadPoolExecutor();
    }

    public static void getCallDetails(
            final Context context,
            final Uri[] callUris,
            final CallLogAsyncTaskListener callLogAsyncTaskListener) {
        if (sAsyncTaskExecutor == null) {
            initTaskExecutor();
        }

        sAsyncTaskExecutor.submit(Tasks.GET_CALL_DETAILS,
                new AsyncTask<Void, Void, PhoneCallDetails[]>() {
                    @Override
                    public PhoneCallDetails[] doInBackground(Void... params) {

                        // TODO: All calls correspond to the same person, so make a single lookup.
                        final int numCalls = callUris.length;
                        PhoneCallDetails[] details = new PhoneCallDetails[numCalls];
                        try {
                            for (int index = 0; index < numCalls; ++index) {
                                details[index] =
                                        getPhoneCallDetailsForUri(context, callUris[index]);
                            }
                            if (numCalls > 0) {
                                getPhoneNumbers(details[0], context);
                            }
                            return details;
                        } catch (IllegalArgumentException e) {
                            // Something went wrong reading in our primary data.
                            Log.w(TAG, "Invalid URI starting call details", e);
                            return null;
                        }
                    }

                    @Override
                    public void onPostExecute(PhoneCallDetails[] phoneCallDetails) {
                        if (callLogAsyncTaskListener != null) {
                            callLogAsyncTaskListener.onGetCallDetails(phoneCallDetails);
                        }
                    }
                });
    }

    public static void getContactDetails(final Context context, final String number, final CallLogAsyncTaskListener callLogAsyncTaskListener) {
        if (sAsyncTaskExecutor == null) {
            initTaskExecutor();
        }
        sAsyncTaskExecutor.submit(Tasks.GET_CONTACT_DETAILS,
                new AsyncTask<Void, Void, PhoneCallDetails[]>() {
                    @Override
                    public PhoneCallDetails[] doInBackground(Void... params) {
//                        Uri[] callLogUris = getCallLogUris(context,number);
                        PhoneCallDetails[] details;
//                        if(callLogUris == null){
                            details = new PhoneCallDetails[1];
                            details[0] = getPhoneDetailsForNumber(context, number, false);
//                        }
//                        else{
//                            details = new PhoneCallDetails[callLogUris.length];
//                            for (int i = 0; i < details.length; i++) {
//                                details[i] = getPhoneCallDetailsForUri(context,callLogUris[i]);
//                            }
//                        }
                        try {
                            getPhoneNumbers(details[0], context);
                            return details;
                        } catch (IllegalArgumentException e) {
                            // Something went wrong reading in our primary data.
                            Log.w(TAG, "Invalid URI starting call details", e);
                            return null;
                        }
                    }

                    @Override
                    public void onPostExecute(PhoneCallDetails[] phoneCallDetails) {
                        if (callLogAsyncTaskListener != null) {
                            callLogAsyncTaskListener.onGetCallDetails(phoneCallDetails);
                        }
                    }
                });
    }

    private static Uri[] getCallLogUris(Context context, String number) {
        Cursor cursor = context.getContentResolver().query(
                CallLog.Calls.CONTENT_URI, new String[]{"_id"}, CallLog.Calls.NUMBER + "=?", new String[]{String.valueOf(number)}, null);
        if(cursor!=null && cursor.moveToFirst()){
            Uri[] uris = new Uri[cursor.getCount()];
            for (int i = 0; i < uris.length; i++) {
                uris[i] = ContentUris.withAppendedId(
                        TelecomUtil.getCallLogUri(context), cursor.getLong(0));
                if(!cursor.moveToNext())
                    break;
            }
            cursor.close();
            return uris;
        }


        return null;
    }
    private static void getPhoneNumbers(PhoneCallDetails details, Context context) {

        Cursor c = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI,
                new String[] {ContactsContract.Data._ID, ContactsContract.CommonDataKinds.Phone.NUMBER,ContactsContract.CommonDataKinds.Phone.STARRED},
                ContactsContract.Data.CONTACT_ID + "=?" + " AND "
                        + ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "'",
                new String[] {String.valueOf(details.contactId)}, null);
        if(c.moveToFirst()){
            details.phoneNumbers = new PhoneCallDetails.PhoneNumberEntity[c.getCount()];
            details.isStarred = c.getInt(2) == 1?true:false;
            int i = 0;
            do{
                details.phoneNumbers[i] = new PhoneCallDetails.PhoneNumberEntity();
                details.phoneNumbers[i].phoneNumber = c.getString(1);
                try {
                    details.phoneNumbers[i].location = CallerInfo.getGeoDescriptionByContext(context, details.phoneNumbers[i].phoneNumber.replaceAll(" ", ""));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                i++;
            }while (c.moveToNext());
        }
        if(c!=null)
            c.close();
    }

    private static PhoneCallDetails getPhoneDetailsForNumber(Context context, String phoneNumber,boolean isVoicemail) {
        String countryIso = GeoUtil.getCurrentCountryIso(context);
        ContactInfoHelper contactInfoHelper =
                new ContactInfoHelper(context, GeoUtil.getCurrentCountryIso(context));
        ContactInfo info = contactInfoHelper.lookupNumber(phoneNumber, countryIso);
        PhoneCallDetails details = new PhoneCallDetails(
                context, phoneNumber, 0, info.formattedNumber, isVoicemail);

//        details.accountHandle = accountHandle;
        details.contactUri = info.lookupUri;
        details.name = info.name;
        details.numberType = info.type;
        details.numberLabel = info.label;
        details.photoUri = info.photoUri;
        details.sourceType = info.sourceType;
        details.objectId = info.objectId;
        details.lookUpKey = info.lookupKey;
        details.contactId = info.contactId;
        return  details;
    }
    /**
     * Return the phone call details for a given call log URI.
     */
    private static PhoneCallDetails getPhoneCallDetailsForUri(Context context, Uri callUri) {

        Cursor cursor = context.getContentResolver().query(
                callUri, CallDetailQuery.CALL_LOG_PROJECTION, null, null, null);
        try {
            if (cursor == null || !cursor.moveToFirst()) {
                throw new IllegalArgumentException("Cannot find content: " + callUri);
            }

            // Read call log.
            final String countryIso = cursor.getString(CallDetailQuery.COUNTRY_ISO_COLUMN_INDEX);
            final String number = cursor.getString(CallDetailQuery.NUMBER_COLUMN_INDEX);
            final int numberPresentation =
                    cursor.getInt(CallDetailQuery.NUMBER_PRESENTATION_COLUMN_INDEX);

            final PhoneAccountHandle accountHandle = PhoneAccountUtils.getAccount(
                    cursor.getString(CallDetailQuery.ACCOUNT_COMPONENT_NAME),
                    cursor.getString(CallDetailQuery.ACCOUNT_ID));

            // If this is not a regular number, there is no point in looking it up in the contacts.
            ContactInfoHelper contactInfoHelper =
                    new ContactInfoHelper(context, GeoUtil.getCurrentCountryIso(context));
            boolean isVoicemail = PhoneNumberUtil.isVoicemailNumber(context, accountHandle, number);
            boolean shouldLookupNumber =
                    PhoneNumberUtil.canPlaceCallsTo(number, numberPresentation) && !isVoicemail;
            ContactInfo info = shouldLookupNumber
                            ? contactInfoHelper.lookupNumber(number, countryIso)
                            : ContactInfo.EMPTY;
            PhoneCallDetails details = new PhoneCallDetails(
                    context, number, numberPresentation, info.formattedNumber, isVoicemail);

            details.accountHandle = accountHandle;
            details.contactUri = info.lookupUri;
            details.name = info.name;
            details.numberType = info.type;
            details.numberLabel = info.label;
            details.photoUri = info.photoUri;
            details.sourceType = info.sourceType;
            details.objectId = info.objectId;
            details.lookUpKey = info.lookupKey;

            details.callTypes = new int[] {
                cursor.getInt(CallDetailQuery.CALL_TYPE_COLUMN_INDEX)
            };
            details.date = cursor.getLong(CallDetailQuery.DATE_COLUMN_INDEX);
            details.duration = cursor.getLong(CallDetailQuery.DURATION_COLUMN_INDEX);
            details.features = cursor.getInt(CallDetailQuery.FEATURES);
            details.geocode = cursor.getString(CallDetailQuery.GEOCODED_LOCATION_COLUMN_INDEX);
            details.transcription = cursor.getString(CallDetailQuery.TRANSCRIPTION_COLUMN_INDEX);

            details.countryIso = !TextUtils.isEmpty(countryIso) ? countryIso
                    : GeoUtil.getCurrentCountryIso(context);
            details.contactId = cursor.getLong(CallDetailQuery.RAW_CONTACT_ID_INDEX);

            return details;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }


    /**
     * Delete specified calls from the call log.
     *
     * @param context The context.
     * @param callIds String of the callIds to delete from the call log, delimited by commas (",").
     * @param callLogAsyncTaskListenerg The listener to invoke after the entries have been deleted.
     */
    public static void deleteCalls(
            final Context context,
            final String callIds,
            final CallLogAsyncTaskListener callLogAsyncTaskListener) {
        if (sAsyncTaskExecutor == null) {
            initTaskExecutor();
        }

        sAsyncTaskExecutor.submit(Tasks.DELETE_CALL,
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    public Void doInBackground(Void... params) {
                        context.getContentResolver().delete(
                                TelecomUtil.getCallLogUri(context),
                                CallLog.Calls._ID + " IN (" + callIds + ")", null);
                        return null;
                    }

                    @Override
                    public void onPostExecute(Void result) {
                        if (callLogAsyncTaskListener != null) {
                            callLogAsyncTaskListener.onDeleteCall();
                        }
                    }
                });

    }

    public static void markVoicemailAsRead(final Context context, final Uri voicemailUri) {
        if (sAsyncTaskExecutor == null) {
            initTaskExecutor();
        }

        sAsyncTaskExecutor.submit(Tasks.MARK_VOICEMAIL_READ, new AsyncTask<Void, Void, Void>() {
            @Override
            public Void doInBackground(Void... params) {
                ContentValues values = new ContentValues();
                values.put(Voicemails.IS_READ, true);
                context.getContentResolver().update(
                        voicemailUri, values, Voicemails.IS_READ + " = 0", null);

                Intent intent = new Intent(context, CallLogNotificationsService.class);
                intent.setAction(CallLogNotificationsService.ACTION_MARK_NEW_VOICEMAILS_AS_OLD);
                context.startService(intent);
                return null;
            }
        });
    }

    public static void deleteVoicemail(
            final Context context,
            final Uri voicemailUri,
            final CallLogAsyncTaskListener callLogAsyncTaskListener) {
        if (sAsyncTaskExecutor == null) {
            initTaskExecutor();
        }

        sAsyncTaskExecutor.submit(Tasks.DELETE_VOICEMAIL,
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    public Void doInBackground(Void... params) {
                        context.getContentResolver().delete(voicemailUri, null, null);
                        return null;
                    }

                    @Override
                    public void onPostExecute(Void result) {
                        if (callLogAsyncTaskListener != null) {
                            callLogAsyncTaskListener.onDeleteVoicemail();
                        }
                    }
                });
    }

    @VisibleForTesting
    public static void resetForTest() {
        sAsyncTaskExecutor = null;
    }
}
