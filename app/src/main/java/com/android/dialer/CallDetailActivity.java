/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.dialer;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.telecom.PhoneAccountHandle;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.QuickContactBadge;
import android.widget.TextView;
import android.widget.Toast;

import com.android.contacts.common.ContactPhotoManager;
import com.android.contacts.common.ContactPhotoManager.DefaultImageRequest;
import com.android.contacts.common.GeoUtil;
import com.android.dialer.calllog.CallDetailPhoneNumberAdapter;
import com.android.dialer.calllog.CallDetailHistoryAdapter;
import com.android.dialer.calllog.CallLogAsyncTaskUtil;
import com.android.dialer.calllog.CallLogAsyncTaskUtil.CallLogAsyncTaskListener;
import com.android.dialer.calllog.CallTypeHelper;
import com.android.dialer.calllog.ContactInfoHelper;
import com.android.dialer.calllog.IntentProvider;
import com.android.dialer.util.DialerUtils;
import com.android.dialer.util.IntentUtil;
import com.android.dialer.util.PhoneNumberUtil;
import com.android.dialer.util.TelecomUtil;

/**
 * Displays the details of a specific call log entry.
 * <p>
 * This activity can be either started with the URI of a single call log entry, or with the
 * {@link #EXTRA_CALL_LOG_IDS} extra to specify a group of call log entries.
 */
public class CallDetailActivity extends Activity implements View.OnClickListener,CallDetailPhoneNumberAdapter.CallDetailCallback {
    private static final String TAG = "CallDetail";

     /** A long array extra containing ids of call log entries to display. */
    public static final String EXTRA_CALL_LOG_IDS = "EXTRA_CALL_LOG_IDS";
    /** If we are started with a voicemail, we'll find the uri to play with this extra. */
    public static final String EXTRA_VOICEMAIL_URI = "EXTRA_VOICEMAIL_URI";
    /** If the activity was triggered from a notification. */
    public static final String EXTRA_FROM_NOTIFICATION = "EXTRA_FROM_NOTIFICATION";

    public static final String VOICEMAIL_FRAGMENT_TAG = "voicemail_fragment";

    private PhoneCallDetails[] detail;

    private CallLogAsyncTaskListener mCallLogAsyncTaskListener = new CallLogAsyncTaskListener() {

        @Override
        public void onDeleteCall() {
            finish();
        }

        @Override
        public void onDeleteVoicemail() {
            finish();
        }

        @Override
        public void onGetCallDetails(PhoneCallDetails[] details) {
            detail = details;
            if (details == null) {
                // Somewhere went wrong: we're going to bail out and show error to users.
                Toast.makeText(mContext, R.string.toast_call_detail_error,
                        Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // We know that all calls are from the same number and the same contact, so pick the
            // first.
            PhoneCallDetails firstDetails = details[0];
            mNumber = TextUtils.isEmpty(firstDetails.number) ?
                    null : firstDetails.number.toString();
            final int numberPresentation = firstDetails.numberPresentation;
            final Uri contactUri = firstDetails.contactUri;
            final Uri photoUri = firstDetails.photoUri;
            final PhoneAccountHandle accountHandle = firstDetails.accountHandle;

            // Cache the details about the phone number.
            final boolean canPlaceCallsTo =
                    PhoneNumberUtil.canPlaceCallsTo(mNumber, numberPresentation);
            mIsVoicemailNumber =
                    PhoneNumberUtil.isVoicemailNumber(mContext, accountHandle, mNumber);
            final boolean isSipNumber = PhoneNumberUtil.isSipNumber(mNumber);

            final CharSequence callLocationOrType = getNumberTypeOrLocation(firstDetails);

            final CharSequence displayNumber = firstDetails.displayNumber;
            final String displayNumberStr = mBidiFormatter.unicodeWrap(
                    displayNumber.toString(), TextDirectionHeuristics.LTR);

            if (!TextUtils.isEmpty(firstDetails.name)) {
                mCallerName.setText(firstDetails.name);
                mCallerNumber.setText(callLocationOrType + " " + displayNumberStr);
            } else {
                mCallerName.setText(displayNumberStr);
                if (!TextUtils.isEmpty(callLocationOrType)) {
                    mCallerNumber.setText(callLocationOrType);
                    mCallerNumber.setVisibility(View.VISIBLE);
                } else {
                    mCallerNumber.setVisibility(View.GONE);
                }
            }

            mHasEditNumberBeforeCallOption =
                    canPlaceCallsTo && !isSipNumber && !mIsVoicemailNumber;
            mHasReportMenuOption = mContactInfoHelper.canReportAsInvalid(
                    firstDetails.sourceType, firstDetails.objectId);
            invalidateOptionsMenu();

            ListView historyList = (ListView) findViewById(R.id.history);
            historyList.setAdapter(
                    new CallDetailHistoryAdapter(mContext, mInflater, mCallTypeHelper, details));
            ListView contactPhoneNumbers = (ListView) findViewById(R.id.contact_phone_numbers);
            if(details.length>=5){
                View view = findViewById(R.id.call_detail_more_call_log_item);
                if(view!=null){
                    view.setVisibility(View.VISIBLE);
                    setCallDetailListener();
                }
            }
            if(firstDetails.phoneNumbers!=null){
                contactPhoneNumbers.setVisibility(View.VISIBLE);
                CallDetailPhoneNumberAdapter adater = new CallDetailPhoneNumberAdapter(mContext,firstDetails.phoneNumbers,displayNumber.toString());
                contactPhoneNumbers.setAdapter(adater);
                adater.setCallDetailCallback(CallDetailActivity.this);
                View view =  findViewById(R.id.call_detail_exist_contact);
                if(view !=null){
                    view.setVisibility(View.VISIBLE);
                    setCallExistItemListener();
                }
            }else {
                View view = findViewById(R.id.call_detail_no_exist_contact);
                if(view !=null){
                    view.setVisibility(View.VISIBLE);
                    setCallNotExistItemListener();
                }
            }
            String lookupKey = contactUri == null ? null
                    : ContactInfoHelper.getLookupKeyFromUri(contactUri);


            final boolean isBusiness = mContactInfoHelper.isBusiness(firstDetails.sourceType);

            final int contactType =
                    mIsVoicemailNumber ? ContactPhotoManager.TYPE_VOICEMAIL :
                    isBusiness ? ContactPhotoManager.TYPE_BUSINESS :
                    ContactPhotoManager.TYPE_DEFAULT;

            String nameForDefaultImage;
            if (TextUtils.isEmpty(firstDetails.name)) {
                nameForDefaultImage = firstDetails.displayNumber;
            } else {
                nameForDefaultImage = firstDetails.name.toString();
            }

            loadContactPhotos(
                    contactUri, photoUri, nameForDefaultImage, lookupKey, contactType);
            findViewById(R.id.call_detail).setVisibility(View.VISIBLE);
        }

        /**
         * Determines the location geocode text for a call, or the phone number type
         * (if available).
         *
         * @param details The call details.
         * @return The phone number type or location.
         */
        private CharSequence getNumberTypeOrLocation(PhoneCallDetails details) {
            if (!TextUtils.isEmpty(details.name)) {
                return Phone.getTypeLabel(mResources, details.numberType,
                        details.numberLabel);
            } else {
                return details.geocode;
            }
        }
    };

    private void setCallNotExistItemListener() {
        findViewById(R.id.call_contact).setOnClickListener(this);
        findViewById(R.id.send_message).setOnClickListener(this);
        findViewById(R.id.add_new_contacts).setOnClickListener(this);
        findViewById(R.id.add_new_exits_contacts).setOnClickListener(this);
        findViewById(R.id.block_contact).setOnClickListener(this);
    }

    private void setCallExistItemListener() {
        findViewById(R.id.send_contact).setOnClickListener(this);
        findViewById(R.id.add_to_the_personal_collection).setOnClickListener(this);
        findViewById(R.id.block_contact).setOnClickListener(this);
    }

    private void setCallDetailListener() {
        findViewById(R.id.call_detail_more_call_log).setOnClickListener(this);
    }

    private Context mContext;
    private CallTypeHelper mCallTypeHelper;
    private QuickContactBadge mQuickContactBadge;
    private TextView mCallerName;
    private TextView mCallerNumber;
//    private TextView mAccountLabel;
//    private View mCallButton;
    private ContactInfoHelper mContactInfoHelper;

    protected String mNumber;
    private boolean mIsVoicemailNumber;
    private String mDefaultCountryIso;

    /* package */ LayoutInflater mInflater;
    /* package */ Resources mResources;
    /** Helper to load contact photos. */
    private ContactPhotoManager mContactPhotoManager;

    private Uri mVoicemailUri;
    private BidiFormatter mBidiFormatter = BidiFormatter.getInstance();

    /** Whether we should show "edit number before call" in the options menu. */
    private boolean mHasEditNumberBeforeCallOption;
    private boolean mHasReportMenuOption;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mContext = this;

        setContentView(R.layout.call_detail_instead);

        mInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        mResources = getResources();

        mCallTypeHelper = new CallTypeHelper(getResources());

        mVoicemailUri = getIntent().getParcelableExtra(EXTRA_VOICEMAIL_URI);

        mQuickContactBadge = (QuickContactBadge) findViewById(R.id.quick_contact_photo);
        mQuickContactBadge.setOverlay(null);
        mQuickContactBadge.setPrioritizedMimeType(Phone.CONTENT_ITEM_TYPE);
        mCallerName = (TextView) findViewById(R.id.caller_name);
        mCallerNumber = (TextView) findViewById(R.id.caller_number);
//        mAccountLabel = (TextView) findViewById(R.id.phone_account_label);
        mDefaultCountryIso = GeoUtil.getCurrentCountryIso(this);
        mContactPhotoManager = ContactPhotoManager.getInstance(this);
        mContactInfoHelper = new ContactInfoHelper(this, GeoUtil.getCurrentCountryIso(this));
        addTextToActionBar();
        if (getIntent().getBooleanExtra(EXTRA_FROM_NOTIFICATION, false)) {
            closeSystemDialogs();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getCallDetails();
    }

    public void getCallDetails() {
        CallLogAsyncTaskUtil.getCallDetails(this, getCallLogEntryUris(), mCallLogAsyncTaskListener);
    }

    private boolean hasVoicemail() {
        return mVoicemailUri != null;
    }
    private IntentProvider toTotalDetailsIntent = null;
    /**
     * Returns the list of URIs to show.
     * <p>
     * There are two ways the URIs can be provided to the activity: as the data on the intent, or as
     * a list of ids in the call log added as an extra on the URI.
     * <p>
     * If both are available, the data on the intent takes precedence.
     */
    private Uri[] getCallLogEntryUris() {
        final Uri uri = getIntent().getData();
        if (uri != null) {
            // If there is a data on the intent, it takes precedence over the extra.
            toTotalDetailsIntent = IntentProvider.getCallTotalDetailIntentProvider(uri, null, null);
            return new Uri[]{ uri };
        }
        final long[] ids = getIntent().getLongArrayExtra(EXTRA_CALL_LOG_IDS);
        final int numIds = ids == null ? 0 : ids.length;
        final Uri[] uris = new Uri[numIds];
        for (int index = 0; index < numIds; ++index) {
            uris[index] = ContentUris.withAppendedId(
                    TelecomUtil.getCallLogUri(CallDetailActivity.this), ids[index]);
        }
        toTotalDetailsIntent = IntentProvider.getCallTotalDetailIntentProvider(uri,ids, null);
        return uris;
    }

    /** Load the contact photos and places them in the corresponding views. */
    private void loadContactPhotos(Uri contactUri, Uri photoUri, String displayName,
            String lookupKey, int contactType) {

        final DefaultImageRequest request = new DefaultImageRequest(displayName, lookupKey,
                contactType, true /* isCircular */);

        mQuickContactBadge.assignContactUri(contactUri);
        mQuickContactBadge.setContentDescription(
                mResources.getString(R.string.description_contact_details, displayName));

        mContactPhotoManager.loadDirectoryPhoto(mQuickContactBadge, photoUri,
                false /* darkTheme */, true /* isCircular */, request);
    }

    private void closeSystemDialogs() {
        sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
    }

    /**
     * 自适应Actionbar
     */
    public void addTextToActionBar()
    {
        ActionBar mActionbar = getActionBar();
//        LayoutParams layout = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT,Gravity.CENTER);
//        mActionbar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
//        mActionbar.setCustomView(getLayoutInflater().inflate(R.layout.call_detail_actionbar_view,null),layout);
        mActionbar.setDisplayShowCustomEnabled( true );

        // Inflate the custom view
        LayoutInflater inflater = LayoutInflater.from( this );
        View header = inflater.inflate( R.layout.call_detail_actionbar_view, null );

        // Magic happens to center it.

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        getActionBar().setHomeAsUpIndicator(R.drawable.ic_menu_back);
//        tv.measure( 0, 0 );
//        int tvSize = tv.getMeasuredWidth();
        int leftSpace = 0;
        try
        {
            View homeButton = findViewById( android.R.id.home );
            final ViewGroup holder = (ViewGroup) homeButton.getParent();

            View firstChild =  holder.getChildAt( 0 );
            View secondChild =  holder.getChildAt( 1 );

            leftSpace = firstChild.getWidth()+secondChild.getWidth();
        }
        catch ( Exception ignored )
        {}

        mActionbar.setCustomView( header );

        if ( null != header )
        {
            ActionBar.LayoutParams params = (ActionBar.LayoutParams) header.getLayoutParams();

            if ( null != params )
            {
                int leftMargin = leftSpace;

                params.leftMargin = 0 >= leftMargin ? 0 : leftMargin;
            }
            mActionbar.setCustomView(header,params);
        }

    }

    @Override
    public void onClick(View v) {
        String number = detail[0].displayNumber;
        Intent intent = null;
        if(TextUtils.isEmpty(number))
            return;
        switch (v.getId()){
            case R.id.call_detail_more_call_log:
                if (toTotalDetailsIntent != null) {
                    intent = toTotalDetailsIntent.getIntent(mContext);
                    // See IntentProvider.getCallDetailIntentProvider() for why this may be null.
                    if (intent != null) {
                        DialerUtils.startActivityWithErrorToast(mContext, intent);
                    }
                }
                break;
            case R.id.send_contact:
                //TODO 发送联系人 提供了联系人 cantactId与号码
                break;
            case R.id.add_to_the_personal_collection:
                break;
            case R.id.block_contact:
                //TODO 阻止联系人
                break;
            case R.id.call_contact:
                callContact(number);
                break;
            case R.id.send_message:
                sendMessage(number);
                break;
            case R.id.add_new_contacts:
                intent = IntentUtil.getNewContactIntent(number);
                DialerUtils.startActivityWithErrorToast(this, intent);
                break;
            case R.id.add_new_exits_contacts:
                intent = IntentUtil.getAddToExistingContactIntent(number);
                DialerUtils.startActivityWithErrorToast(this,intent,
                            R.string.add_contact_not_available);
                break;
        }
    }

    private void callContact(String number) {
        Intent intent;
        intent = IntentUtil.getCallIntent(number);
        DialerUtils.startActivityWithErrorToast(this, intent);
    }

    private void sendMessage(String number) {
        Intent intent;
        intent = IntentUtil.getSendSmsIntent(number);
        DialerUtils.startActivityWithErrorToast(this, intent);
    }

    @Override
    public void call(View v, int position) {
        String number = detail[0].displayNumber;
        if(!TextUtils.isEmpty(number))
            callContact(number);
    }

    @Override
    public void senMessage(View v, int position) {
        String number = detail[0].displayNumber;
        if(!TextUtils.isEmpty(number))
            sendMessage(number);
    }
}
