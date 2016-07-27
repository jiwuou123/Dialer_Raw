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
import android.app.Dialog;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.telecom.PhoneAccountHandle;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.QuickContactBadge;
import android.widget.TextView;
import android.widget.Toast;

import com.android.contacts.common.ContactPhotoManager;
import com.android.contacts.common.ContactPhotoManager.DefaultImageRequest;
import com.android.contacts.common.GeoUtil;
import com.android.dialer.calllog.CallDetailHistoryAdapter;
import com.android.dialer.calllog.CallDetailPhoneNumberAdapter;
import com.android.dialer.calllog.CallLogAsyncTaskUtil;
import com.android.dialer.calllog.CallLogAsyncTaskUtil.CallLogAsyncTaskListener;
import com.android.dialer.calllog.CallTypeHelper;
import com.android.dialer.calllog.ContactInfoHelper;
import com.android.dialer.calllog.ContactSaveService;
import com.android.dialer.calllog.IntentProvider;
import com.android.dialer.m1000systemdialog.RoundAlertDialog;
import com.android.dialer.util.DialerUtils;
import com.android.dialer.util.IntentUtil;
import com.android.dialer.util.PhoneNumberUtil;
import com.android.dialer.util.TelecomUtil;
import com.eebbk.bbksafe.module.psfilter.exinterface.Iexinterface;

/**
 * Displays the details of a specific call log entry.
 * <p>
 * This activity can be either started with the URI of a single call log entry, or with the
 * {@link #EXTRA_CALL_LOG_IDS} extra to specify a group of call log entries.
 */
public class CallDetailActivity extends Activity implements View.OnClickListener,CallDetailPhoneNumberAdapter.CallDetailCallback,View.OnLongClickListener{
    private static final String TAG = "CallDetail";

     /** A long array extra containing ids of call log entries to display. */
    public static final String EXTRA_CALL_LOG_IDS = "EXTRA_CALL_LOG_IDS";
    /** If we are started with a voicemail, we'll find the uri to play with this extra. */
    public static final String EXTRA_VOICEMAIL_URI = "EXTRA_VOICEMAIL_URI";
    /** If the activity was triggered from a notification. */
    public static final String EXTRA_FROM_NOTIFICATION = "EXTRA_FROM_NOTIFICATION";

    public static final String VOICEMAIL_FRAGMENT_TAG = "voicemail_fragment";

    private PhoneCallDetails[] detail;
    private boolean isBlack;//检验是否为黑名单联系人
    private boolean isStarted;//判断联系人是否收藏。
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
            Uri photoUri = firstDetails.photoUri;
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

//                mCallerNumber.setText(callLocationOrType + " " + displayNumberStr);
            } else {
                mCallerName.setText(displayNumberStr);
                if (!TextUtils.isEmpty(callLocationOrType)) {
                    mCallerNumber.setText(callLocationOrType);
                    mCallerNumber.setVisibility(View.VISIBLE);
                } else {
                    if(firstDetails.isVoicemail)
                        mCallerNumber.setText(firstDetails.number);
                    else
                        mCallerNumber.setVisibility(View.INVISIBLE);
                }
            }

            mHasEditNumberBeforeCallOption =
                    canPlaceCallsTo && !isSipNumber && !mIsVoicemailNumber;
            mHasReportMenuOption = mContactInfoHelper.canReportAsInvalid(
                    firstDetails.sourceType, firstDetails.objectId);
            invalidateOptionsMenu();

            ListView historyList = (ListView) findViewById(R.id.history);
            PhoneCallDetails[] showDetails = null;
            ListView contactPhoneNumbers = (ListView) findViewById(R.id.contact_phone_numbers);
            showDetails = details;
            if(details.length>=5){
                showDetails = new PhoneCallDetails[5];
                for (int i = 0; i < 5; i++) {
                    showDetails[i] = details[i];
                }
                View view = findViewById(R.id.call_detail_more_call_log_item);
                findViewById(R.id.call_detail_more_call_log_item_divider).setVisibility(View.VISIBLE);
                if(view!=null){
                    view.setVisibility(View.VISIBLE);
                    setCallDetailListener();
                }
            }
            historyList.setAdapter(
                    new CallDetailHistoryAdapter(mContext, mInflater, mCallTypeHelper, showDetails));
            findViewById(R.id.history_divide).setVisibility(View.VISIBLE);
            if(firstDetails.isVoicemail){
                View view =  findViewById(R.id.call_detail_voice_mail);
                if(view != null){
                    view.setVisibility(View.VISIBLE);
                    initViewMailItemView();
                }
            }else if(firstDetails.phoneNumbers!=null){
                contactPhoneNumbers.setVisibility(View.VISIBLE);
                CallDetailPhoneNumberAdapter adater = new CallDetailPhoneNumberAdapter(mContext,firstDetails.phoneNumbers,displayNumber.toString());
                contactPhoneNumbers.setAdapter(adater);
                findViewById(R.id.contact_phone_numbers_divide).setVisibility(View.VISIBLE);
                adater.setCallDetailCallback(CallDetailActivity.this);
                isStarted = firstDetails.isStarred;
                View view =  findViewById(R.id.call_detail_exist_contact);
                if(view !=null){
                    view.setVisibility(View.VISIBLE);
                    initCallExistItemView();
                }
                checkStarted();
            }else {
                View view = findViewById(R.id.call_detail_no_exist_contact);
                if(view !=null){
                    view.setVisibility(View.VISIBLE);
                    initCallNotExistItemView();
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
            if(photoUri==null)
                photoUri = Uri.parse("android.resource://"+getPackageName()+"/"+R.drawable.call_detail_photo_normal);
            loadContactPhotos(
                    contactUri, photoUri, nameForDefaultImage, lookupKey, contactType);
            findViewById(R.id.call_detail).setVisibility(View.VISIBLE);
            //检查联系人是否为黑名单
            currentNumber = firstDetails.number.toString();
            checkBlackList();
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

    private void checkStarted() {
        TextView textView = (TextView) findViewById(R.id.add_to_the_personal_favorites);
        if(isStarted)
            textView.setText(R.string.unfavourite);
        else
            textView.setText(R.string.favorite);
    }


    private void checkBlackList() {
        if(isConnection&&detail!=null){
            try {
                TextView blockContact = (TextView)findViewById(R.id.block_contact);
                if(exInterface.isBlacklist(currentNumber)){
                    blockContact.setText(R.string.unblock_contact);
                    isBlack = true;
                }else {
                    blockContact.setText(R.string.block_contact);
                    isBlack = false;
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
    private void initViewMailItemView() {
        findViewById(R.id.call_contact).setOnClickListener(this);
        findViewById(R.id.send_message).setOnClickListener(this);
        findViewById(R.id.block_contact).setOnClickListener(this);
    }

    private void initCallNotExistItemView() {
        findViewById(R.id.call_contact).setOnClickListener(this);
        findViewById(R.id.send_message).setOnClickListener(this);
        findViewById(R.id.add_new_contacts).setOnClickListener(this);
        findViewById(R.id.add_new_exits_contacts).setOnClickListener(this);
        findViewById(R.id.block_contact).setOnClickListener(this);
        findViewById(R.id.caller_name).setOnLongClickListener(this);
    }

    private void initCallExistItemView() {
        findViewById(R.id.send_contact).setOnClickListener(this);
        findViewById(R.id.add_to_the_personal_favorites).setOnClickListener(this);
        findViewById(R.id.block_contact).setOnClickListener(this);
        actionBarEdit.setOnClickListener(this);
        actionBarEdit.setVisibility(View.VISIBLE);
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
    private Button actionBarEdit;

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
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(R.layout.call_detail_actionbar_view);

        actionBarEdit = (Button) actionBar.getCustomView().findViewById(R.id.call_detail_action_editer);
        //指定返回键回调
        ImageView backBtn = (ImageView)actionBar.getCustomView().findViewById(R.id.backBtn);
        if (null != backBtn){
            backBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
        if (getIntent().getBooleanExtra(EXTRA_FROM_NOTIFICATION, false)) {
            closeSystemDialogs();
        }
        bindService();
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


    private void showAddBlack(){
        if(addBlackDialog == null)
        addBlackDialog = new RoundAlertDialog.Builder(this).setTitle(R.string.block).setMessage(getResources().getString(R.string.block_caller_tips))
                .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            exInterface.addBlacklist(currentNumber);
                            checkBlackList();
                            dialog.dismiss();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }).setPositiveButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();
        addBlackDialog.show();
    }
    RoundAlertDialog addBlackDialog ;
    String currentNumber;
    @Override
    public void onClick(View v) {
        currentNumber = detail[0].number.toString();
        String lookUpKey = detail[0].lookUpKey;
        long contactId = detail[0].contactId;
        Uri ContactUri = detail[0].contactUri;
        Intent intent = null;
        if(TextUtils.isEmpty(currentNumber))
            return;
        int i = v.getId();
        if (i == R.id.call_detail_more_call_log) {
            if (toTotalDetailsIntent != null) {
                intent = toTotalDetailsIntent.getIntent(mContext);
                // See IntentProvider.getCallDetailIntentProvider() for why this may be null.
                if (intent != null) {
                    DialerUtils.startActivityWithErrorToast(mContext, intent);
                }
            }

        } else if (i == R.id.send_contact) {
            shareContact(lookUpKey, contactId);

        } else if (i == R.id.add_to_the_personal_favorites) {
            intent = ContactSaveService.createSetStarredIntent(
                    getApplicationContext(), ContactUri, !isStarted);
            startService(intent);
            isStarted = !isStarted;
            checkStarted();

        } else if (i == R.id.block_contact) {
            if (isConnection) {
                if (isBlack)
                    try {
                        exInterface.removeBlacklist(currentNumber);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                else
                    showAddBlack();
            }

        } else if (i == R.id.call_contact) {
            callContact(currentNumber);

        } else if (i == R.id.send_message) {
            sendMessage(currentNumber);

        } else if (i == R.id.add_new_contacts) {
            intent = IntentUtil.getNewContactIntent(currentNumber);
            DialerUtils.startActivityWithErrorToast(this, intent);

        } else if (i == R.id.add_new_exits_contacts) {
            intent = IntentUtil.getAddToExistingContactIntent(currentNumber);
            DialerUtils.startActivityWithErrorToast(this, intent,
                    R.string.add_contact_not_available);

        } else if (i == R.id.call_detail_action_editer) {
            intent = new Intent(Intent.ACTION_EDIT);
            intent.setDataAndType(ContactUri, ContactsContract.Contacts.CONTENT_ITEM_TYPE);
            startActivity(intent);

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
    public void sendMessage(View v, int position) {
        String number = detail[0].displayNumber;
        if(!TextUtils.isEmpty(number))
            sendMessage(number);
    }

    @Override
    public void onCallContentLongClick(View v, int position) {
        vibrator();
        showCopyDialog(detail[0].phoneNumbers[position].phoneNumber);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService();
    }
    private Iexinterface exInterface;
    private boolean isConnection = false;
    private void bindService(){
        Intent intent = new Intent("com.eebbk.bbksafe.module.psfilter.exinterface.InterfaceService");
        intent.setPackage("com.eebbk.bbksafe");
        mContext.bindService(intent, mConn, Service.BIND_AUTO_CREATE);
    }
    private void unbindService(){
        mContext.unbindService(mConn);
    }

    private ServiceConnection mConn = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            exInterface = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            exInterface = Iexinterface.Stub.asInterface(service);
            isConnection = true;
            //检查联系人是否为黑名单
            checkBlackList();
        }
    };

    private void shareContact(String lookupKey, long contactId) {
        Uri shareUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey);

        final Intent intent = new Intent(Intent.ACTION_SEND);
//        if (mContactData.isUserProfile()) {
//            // User is sharing the profile.  We don't want to force the receiver to have
//            // the highly-privileged READ_PROFILE permission, so we need to request a
//            // pre-authorized URI from the provider.
//            shareUri = getPreAuthorizedUri(shareUri);
//            /** M for ALPS01752410 @{*/
//            intent.setType(ContactsContract.Contacts.CONTENT_VCARD_TYPE);
//            intent.putExtra("userProfile", "true");
//        } else {
            intent.setType(ContactsContract.Contacts.CONTENT_VCARD_TYPE);
            intent.putExtra("contactId", String.valueOf(contactId));
            /** @} */
//        }

        intent.setType(ContactsContract.Contacts.CONTENT_VCARD_TYPE);
        intent.putExtra(Intent.EXTRA_STREAM, shareUri);
        // Launch chooser to share contact via
        final CharSequence chooseTitle = getText(R.string.share_via);
        final Intent chooseIntent = Intent.createChooser(intent, chooseTitle);

        try {
            this.startActivity(chooseIntent);
        } catch (final ActivityNotFoundException ex) {
            Toast.makeText(this, R.string.share_error, Toast.LENGTH_SHORT).show();
        }
    }
    private Dialog copyDialog;
    private ListView copyListView;
    private void initCopyDialog(){
        copyDialog = new Dialog(this, R.style.Theme_Light_Dialog);
        final View dialogView = LayoutInflater.from(this).inflate(R.layout.call_detail_copy_layout,null);
        //获得dialog的window窗口
        Window window = copyDialog.getWindow();
        //设置dialog在屏幕底部
        window.setGravity(Gravity.BOTTOM);
        window.setWindowAnimations(R.style.DialpadSearchPopupWindowAnim);
        window.getDecorView().setPadding(0, 0, 0, 0);
        android.view.WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setAttributes(lp);
        copyDialog.setContentView(dialogView);
        copyListView = (ListView) dialogView.findViewById(R.id.call_detail_copy_content);
        String[] copyList = null;
        if(detail[0].phoneNumbers!=null&&detail[0].phoneNumbers.length>1)
            copyList = new String[]{"复制","设置为默认号码"};
        else
            copyList = new String[]{"复制"};
        copyListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position){
                    case 0:
                        //获取剪贴板管理服务
                        ClipboardManager cm =(ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        //将文本数据复制到剪贴板
                        cm.setText(currentCopyString);
                        copyDialog.dismiss();
                        break;
                }
            }
        });
        copyListView.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,copyList));
    }
    private String currentCopyString;
    private void showCopyDialog(String number){
        if(copyDialog == null)
            initCopyDialog();
        currentCopyString = number;
        copyDialog.show();
    }
    @Override
    public boolean onLongClick(View v) {
        int i = v.getId();
        if (i == R.id.caller_name) {
            vibrator();
            showCopyDialog(currentNumber);
            return true;
        }
        return false;
    }

    private void vibrator() {
        Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(300);
    }
}
