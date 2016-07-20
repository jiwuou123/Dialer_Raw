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
package com.android.dialer.list;

import android.app.Activity;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.android.contacts.common.list.ContactEntryListAdapter;
import com.android.contacts.common.list.PhoneNumberListAdapter;
import com.android.contacts.common.util.PermissionsUtil;
import com.android.dialer.R;
import com.android.dialer.calllog.IntentProvider;
import com.android.dialer.database.DialerSearchHelper;
import com.android.dialer.dialpad.DialpadSearchCursorLoader;
import com.android.dialer.dialpad.SmartDialCursorLoader;
import com.android.dialer.util.DialerUtils;
import com.android.dialer.util.IntentUtil;
import com.android.dialer.widget.EmptyContentView;

import static android.Manifest.permission.CALL_PHONE;

/**
 * Implements a fragment to load and display SmartDial search results.
 */
public class SmartDialSearchFragment extends SearchFragment
        implements EmptyContentView.OnEmptyViewActionButtonClickedListener,DialpadSearchListAdapter.MoreFetcher,View.OnClickListener{
    private static final String TAG = SmartDialSearchFragment.class.getSimpleName();

    private static final int CALL_PHONE_PERMISSION_REQUEST_CODE = 1;

    private PopupWindow addContactPopupWindow = null;
    private TextView popupWindowTitle = null;
    /**
     * Creates a SmartDialListAdapter to display and operate on search results.
     */
    @Override
    protected ContactEntryListAdapter createListAdapter() {
        PhoneNumberListAdapter adapter = null;
        //TODO 修改相应规则
        if(DialerUtils.canUseDialpadSearch()){
            adapter = new DialpadSearchListAdapter(getActivity());
            ((DialpadSearchListAdapter)adapter).setMoreFetcher(this);
        }else {
            adapter = new SmartDialNumberListAdapter(getActivity());
        }
        adapter.setUseCallableUri(super.usesCallableUri());
        adapter.setQuickContactEnabled(true);
        // Set adapter's query string to restore previous instance state.
        adapter.setQueryString(getQueryString());
        return adapter;
    }

    @Override
    protected void onCreateView(LayoutInflater inflater, ViewGroup container) {
        super.onCreateView(inflater, container);
        getListView().setDividerHeight(1);
        int padding = (int) getResources().getDimension(R.dimen.dialpad_search_list_item_padding);
        getView().setPadding(padding,padding,padding,padding);
    }

    /**
     * Creates a SmartDialCursorLoader object to load query results.
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Smart dialing does not support Directory Load, falls back to normal search instead.
        if (id == getDirectoryLoaderId()) {
            return super.onCreateLoader(id, args);
        } else {

            if(DialerUtils.canUseDialpadSearch()){
                //TODO 修改相应规则
                DialpadSearchListAdapter adapter = (DialpadSearchListAdapter) getAdapter();
                DialpadSearchCursorLoader loader = new DialpadSearchCursorLoader(super.getContext());
                adapter.configureLoader(loader);
                return loader;
            }else {
                SmartDialNumberListAdapter adapter = (SmartDialNumberListAdapter) getAdapter();
                SmartDialCursorLoader loader = new SmartDialCursorLoader(super.getContext());
                adapter.configureLoader(loader);
                return loader;
            }
        }
    }

    /**
     * Gets the Phone Uri of an entry for calling.
     * @param position Location of the data of interest.
     * @return Phone Uri to establish a phone call.
     */
    @Override
    protected Uri getPhoneUri(int position) {
        if(DialerUtils.canUseDialpadSearch()){
            final DialpadSearchListAdapter adapter = (DialpadSearchListAdapter) getAdapter();
            return adapter.getDataUri(position);
        }
        final SmartDialNumberListAdapter adapter = (SmartDialNumberListAdapter) getAdapter();
        return adapter.getDataUri(position);
    }

    @Override
    protected void setupEmptyView() {
        if (mEmptyView != null && getActivity() != null) {
            if (!PermissionsUtil.hasPermission(getActivity(), CALL_PHONE)) {
                mEmptyView.setImage(R.drawable.empty_contacts);
                mEmptyView.setActionLabel(R.string.permission_single_turn_on);
                mEmptyView.setDescription(R.string.permission_place_call);
                mEmptyView.setActionClickedListener(this);
            } else {
                mEmptyView.setImage(EmptyContentView.NO_IMAGE);
                mEmptyView.setActionLabel(EmptyContentView.NO_LABEL);
                mEmptyView.setDescription(EmptyContentView.NO_LABEL);
            }
        }
    }

    @Override
    public void onEmptyViewActionButtonClicked() {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        requestPermissions(new String[] {CALL_PHONE}, CALL_PHONE_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
            int[] grantResults) {
        if (requestCode == CALL_PHONE_PERMISSION_REQUEST_CODE) {
            setupEmptyView();
        }
    }

    public boolean isShowingPermissionRequest() {
        return mEmptyView != null && mEmptyView.isShowingContent();
    }

    @Override
    public void fetchMore(int type, int position, View view) {
        DialpadSearchListAdapter adapter = (DialpadSearchListAdapter) getAdapter();
        switch (type){
            case DialpadSearchListAdapter.CALLLOG_DETAIL:
//                cursor.moveToPosition(position);
                int callId = adapter.getCallLogId(position);
               Intent intent = IntentProvider.getCallDetailIntentProvider(callId,null,null).getIntent(getContext());
                DialerUtils.startActivityWithErrorToast(getContext(), intent);
                break;
            case DialpadSearchListAdapter.CONTACT_DETAIL:
                final Uri uri = adapter.getContactUri(position);
                if (uri != null) {
                    adapter.getDataUri(position);
                    ContactsContract.QuickContact.showQuickContact(getContext(), view, uri, null,
                            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
                }
                break;
            case DialpadSearchListAdapter.NEW_CONTACT:
//                cursor.moveToFirst();
                Cursor cursor = (Cursor) adapter.getItem(position);
                String showNumber = cursor!=null?cursor.getString(DialerSearchHelper.DialerSearchColumn.SEARCH_PHONE_NUMBER_INDEX):getQueryString();
                showPopupWindow(showNumber);
//                number = TextUtils.isEmpty(mAddToContactNumber) ?
//                        adapter.getFormattedQueryString() : mAddToContactNumber;
//                intent = IntentUtil.getNewContactIntent(number);
//                DialerUtils.startActivityWithErrorToast(getActivity(), intent);
                break;
        }
    }
    private void initPopupWindow(){
        View contentView = LayoutInflater.from(getContext()).inflate(R.layout.dialpad_search_add_contact,null);
        contentView.setOnClickListener(this);
        addContactPopupWindow = new PopupWindow(contentView, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT);
        popupWindowTitle = (TextView) contentView.findViewById(R.id.contact_phone_number);
        contentView.findViewById(R.id.create_new_contact_action).setOnClickListener(this);
        contentView.findViewById(R.id.add_to_existing_contact_action).setOnClickListener(this);
//        addContactPopupWindow.setContentView(contentView);
        addContactPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        addContactPopupWindow.setOutsideTouchable(true);
        addContactPopupWindow.setAnimationStyle(R.style.DialpadSearchPopupWindowAnim);
        addContactPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                backgroundAlpha(1f);
            }
        });
    }
    private void showPopupWindow(String text){
        if(addContactPopupWindow==null)
            initPopupWindow();
        popupWindowTitle.setText(text);
        backgroundAlpha(0.5f);
        addContactPopupWindow.showAtLocation(getActivity().getWindow().findViewById(android.R.id.content), Gravity.BOTTOM,0,0);
    }
    @Override
    public void onClick(View v) {
        DialpadSearchListAdapter adapter = (DialpadSearchListAdapter) getAdapter();
        final Intent intent;
        final String number;
        switch (v.getId()){
            case R.id.create_new_contact_action:
                number = adapter.getFormattedQueryString();
                intent = IntentUtil.getNewContactIntent(number);
                DialerUtils.startActivityWithErrorToast(getActivity(), intent,
                        R.string.add_contact_not_available);
                break;
            case R.id.add_to_existing_contact_action:
                number = adapter.getFormattedQueryString() ;
                intent = IntentUtil.getAddToExistingContactIntent(number);
                DialerUtils.startActivityWithErrorToast(getActivity(), intent,
                        R.string.add_contact_not_available);
                break;
        }
    }

    public void backgroundAlpha(float bgAlpha)
    {
        WindowManager.LayoutParams lp = getActivity().getWindow().getAttributes();
        lp.alpha = bgAlpha; //0.0-1.0
        getActivity().getWindow().setAttributes(lp);
    }
    public boolean popupWindowIsShowing(){
        return addContactPopupWindow!=null&&addContactPopupWindow.isShowing();
    }
    public void popupWindowDismiss(){
         addContactPopupWindow.dismiss();
    }
}
