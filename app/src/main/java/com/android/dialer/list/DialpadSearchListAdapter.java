package com.android.dialer.list;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.telephony.PhoneNumberUtils;
import android.text.LoginFilter;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.android.common.widget.CompositeCursorAdapter;
import com.android.contacts.common.GeoUtil;
import com.android.contacts.common.format.TextHighlighter;
import com.android.contacts.common.list.ContactListAdapter;
import com.android.contacts.common.list.ContactListItemView;
import com.android.contacts.common.list.DirectoryPartition;
import com.android.contacts.common.list.PhoneNumberListAdapter;
import com.android.contacts.common.util.PhoneNumberFormatter;
import com.android.dialer.R;
import com.android.dialer.database.DialerSearchHelper;
import com.android.dialer.dialpad.DialpadSearchCursorLoader;
import com.android.dialer.dialpad.SmartDialCursorLoader;
import com.android.dialer.util.DialerSearchUtils;
import com.google.i18n.phonenumbers.PhoneNumberUtil;

import java.util.ArrayList;

/**
 * Created by Administrator on 2016/7/13.
 */

public class DialpadSearchListAdapter extends DialerPhoneNumberListAdapter{
    private final String TAG = "DialpadSearchListAdapter";

    private final int DS_MATCHED_DATA_INIT_POS    = 3;
    private final int DS_MATCHED_DATA_DIVIDER     = 3;

    private MoreFetcher moreFetcher;
    private String mCountryIso;
    public DialpadSearchListAdapter(Context context) {
        super(context);
        mCountryIso = GeoUtil.getCurrentCountryIso(context);
    }
    public void configureLoader(DialpadSearchCursorLoader loader) {
        if (getQueryString() == null) {
            loader.configureQuery("");
        } else {
            loader.configureQuery(getQueryString());
        }
    }

    @Override
    public Uri getDataUri(int position) {
        Cursor cursor = ((Cursor)getItem(position));
        if (cursor != null) {
            if(cursor.getLong(DialerSearchHelper.DialerSearchColumn.CALL_LOG_ID_INDEX)>0)
                return null;
            else {
                long id = cursor.getLong(DialerSearchHelper.DialerSearchColumn.DATA_ID_INDEX);
                if(id<=0)
                    return null;
                return ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, id);
            }
        } else {
            return null;
        }
    }
    @Override
    public String getLookupKey(int position) {
        final Cursor item = (Cursor)getItem(position);
        return item != null ? item.getString(DialerSearchHelper.DialerSearchColumn.CONTACT_NAME_LOOKUP_INDEX) : null;
    }

    public Uri getContactUri(int position) {
        int partitionIndex = getPartitionForPosition(position);
        Cursor item = (Cursor)getItem(position);
        return item != null ? getContactUri(partitionIndex, item) : null;
    }

    public Uri getContactUri(int partitionIndex,Cursor cursor) {
        long contactId = cursor.getLong(DialerSearchHelper.DialerSearchColumn.CONTACT_ID_INDEX);
        String lookupKey = cursor.getString(DialerSearchHelper.DialerSearchColumn.CONTACT_NAME_LOOKUP_INDEX);
        Uri uri = ContactsContract.Contacts.getLookupUri(contactId, lookupKey);
        long directoryId = ((DirectoryPartition)getPartition(partitionIndex)).getDirectoryId();
        if (uri != null && directoryId != ContactsContract.Directory.DEFAULT) {
            uri = uri.buildUpon().appendQueryParameter(
                    ContactsContract.DIRECTORY_PARAM_KEY, String.valueOf(directoryId)).build();
        }
        return uri;
    }
    /**
     * 监听器
     * @param moreFetcher
     */
    public void setMoreFetcher(MoreFetcher moreFetcher) {
        this.moreFetcher = moreFetcher;
    }

    /**
     * 返回对应item的电话号码
     * @param position item位置
     * @return 号码
     */
    @Override
    public String getPhoneNumber(int position) {
        return ((Cursor)getItem(position)).getString(DialerSearchHelper.DialerSearchColumn.SEARCH_PHONE_NUMBER_INDEX);
    }
    public int getCallLogId(int position) {
        return ((Cursor)getItem(position)).getInt(DialerSearchHelper.DialerSearchColumn.CALL_LOG_ID_INDEX);
    }
    @Override
    public void changeCursor(int partitionIndex, Cursor cursor) {
        boolean change = cursor!=null&&cursor.getCount()==0&&!TextUtils.isEmpty(getFormattedQueryString());
        setShortcutEnabled(SHORTCUT_DIRECT_CALL,change);
        super.changeCursor(partitionIndex,cursor);
    }


    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if(getShortcutTypeFromPosition(position)==SHORTCUT_DIRECT_CALL){
            if(convertView == null){
                convertView =  newView(mContext,parent);
            }
            ViewHolder viewHolder = (ViewHolder) convertView.getTag();
            viewHolder.primaryCallInfo.setText(getQueryString());
            viewHolder.secondaryCallInfo.setVisibility(View.GONE);
            viewHolder.callDetail.setImageDrawable(mContext.getDrawable(R.drawable.add_contact));
            bindListener(NEW_CONTACT,position, viewHolder);
            return convertView;
        }
        return super.getView(position, convertView, parent);
    }

    private void bindListener(final int type, final int position, final ViewHolder viewHolder) {
        if(moreFetcher!=null){
            viewHolder.callDetail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    moreFetcher.fetchMore(type,position,viewHolder.callDetail);
                }
            });
        }
    }

    @Override
    protected View newView(Context context, int partition, Cursor cursor, int position, ViewGroup parent) {
        View view = newView(context, parent);
        return view;
    }

    @NonNull
    private View newView(Context context, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialpad_search_list_item,parent,false);
        ViewHolder viewHolder = new ViewHolder();
        viewHolder.primaryCallInfo = (TextView) view.findViewById(R.id.dialpad_search_primary_call_info);
        viewHolder.secondaryCallInfo = (TextView) view.findViewById(R.id.dialpad_search_secondary_call_info);
        viewHolder.callDetail = (ImageButton) view.findViewById(R.id.dialpad_search_call_detail);
        PhoneNumberFormatter.setPhoneNumberFormattingTextWatcher(context, viewHolder.secondaryCallInfo);
//        viewHolder.callDate = (TextView) view.findViewById(R.id.dialpad_search_call_date);
        view.setTag(viewHolder);
        return view;
    }
    @Override
    protected void bindView(View itemView, int partition, Cursor cursor, int position) {
        ViewHolder viewHolder = (ViewHolder) itemView.getTag();
        if(getCount() == 1&&cursor.getCount()==0){
                return;
        }
        if((cursor.getInt(DialerSearchHelper.DialerSearchColumn.CONTACT_ID_INDEX))==0){
            bindCallLog(viewHolder,mContext,cursor);
            if(cursor.getCount()==1){
                bindListener(NEW_CONTACT,position, viewHolder);
            }else
                bindListener(CALLLOG_DETAIL,position, viewHolder);
        }else {
            bindContactView(viewHolder,mContext,cursor);
            bindListener(CONTACT_DETAIL,position, viewHolder);
        }
    }
    private void bindCallLog(ViewHolder viewHolder, Context context, Cursor cursor){
        if(cursor.getCount() == 1){
            viewHolder.callDetail.setImageDrawable(context.getDrawable(R.drawable.add_contact));
        }
        else{
            viewHolder.callDetail.setImageDrawable(context.getDrawable(R.drawable.detail_button));

        }
        bindPhoneNumber(viewHolder.primaryCallInfo,cursor);
        String callGeocodeLocation = cursor.getString(DialerSearchHelper.DialerSearchColumn.CALL_GEOCODED_LOCATION_INDEX);
        if(!TextUtils.isEmpty(callGeocodeLocation)){
            viewHolder.secondaryCallInfo.setVisibility(View.VISIBLE);
            viewHolder.secondaryCallInfo.setText(callGeocodeLocation);
        }
        else
            viewHolder.secondaryCallInfo.setVisibility(View.GONE);

    }
    private void bindContactView(ViewHolder viewHolder, Context context, Cursor cursor){
        viewHolder.callDetail.setImageDrawable(context.getDrawable(R.drawable.detail_button));
        bindName(viewHolder.primaryCallInfo,cursor);
        bindPhoneNumber(viewHolder.secondaryCallInfo, cursor);
        viewHolder.secondaryCallInfo.setVisibility(View.VISIBLE);
    }

    private void bindPhoneNumber(TextView textView, Cursor cursor) {
        String number = cursor.getString(DialerSearchHelper.DialerSearchColumn.SEARCH_PHONE_NUMBER_INDEX);
        String formatNumber = numberLeftToRight(PhoneNumberUtils.formatNumber(number,mCountryIso));
        if (!TextUtils.isEmpty(formatNumber)) {
            String highlight = getNumberHighlight(cursor);
            if (!TextUtils.isEmpty(highlight)) {
                SpannableStringBuilder style = highlightHyphen(highlight, formatNumber,
                        number);
                textView.setText(style);
            } else {
                textView.setText(number);
            }
        } else {
            textView.setText(number);
        }
    }

    private void bindName(TextView textView, Cursor cursor) {
        String displayName = cursor.getString(DialerSearchHelper.DialerSearchColumn.NAME_INDEX);
        String highlight = getNameHighlight(cursor);
        if (!TextUtils.isEmpty(highlight)) {
            SpannableStringBuilder style = highlightString(highlight, displayName);
            textView.setText(style);
            if (isRegularSearch(cursor)) {
                textView.setText(highlightName(highlight, displayName));
            }
        } else {
            textView.setText(displayName);
        }
    }

    private String getNameHighlight(Cursor cursor) {
        return cursor.getString(DialerSearchHelper.DialerSearchColumn.DS_MATCHED_NAME_OFFSETS);
    }


    private String getNumberHighlight(Cursor cursor) {
        return  cursor.getString(DialerSearchHelper.DialerSearchColumn.DS_MATCHED_DATA_OFFSETS);
    }


    private CharSequence highlightName(String highlight, CharSequence target) {
        String highlightedPrefix = getUpperCaseQueryString();
        if (highlightedPrefix != null) {
            TextHighlighter mTextHighlighter = new TextHighlighter(Typeface.BOLD);
            target =  mTextHighlighter.applyPrefixHighlight(target, highlightedPrefix);
        }
        return target;
    }

    private SpannableStringBuilder highlightString(String highlight, CharSequence target) {
        SpannableStringBuilder style = new SpannableStringBuilder(target);
        int length = highlight.length();
        final int styleLength = style.length();
        int start = -1;
        int end = -1;
        for (int i = DS_MATCHED_DATA_INIT_POS; i + 1 < length; i += DS_MATCHED_DATA_DIVIDER) {
            start = (int) highlight.charAt(i);
            end = (int) highlight.charAt(i + 1) + 1;
            /// M: If highlight area is invalid, just skip it.
            if (start > styleLength || end > styleLength || start > end) {
                Log.d(TAG, "highlightString, start: " + start + " ,end: " + end
                        + " ,styleLength: " + styleLength);
                break;
            }
            style.setSpan(new ForegroundColorSpan(mContext.getColor(R.color.phone_number_text_select_color)), start, end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return style;
    }
    private boolean isRegularSearch(Cursor cursor) {
        String regularSearch  = cursor.getString(DialerSearchHelper.DialerSearchColumn.DS_MATCHED_DATA_OFFSETS);
        Log.d(TAG, "" + regularSearch);

        return Boolean.valueOf(regularSearch);
    }

    private SpannableStringBuilder highlightHyphen(String highlight, String target, String origin) {
        if (target == null) {
            return null;
        }
        SpannableStringBuilder style = new SpannableStringBuilder(target);
        ArrayList<Integer> numberHighlightOffset = DialerSearchUtils
                .adjustHighlitePositionForHyphen(target, highlight
                        .substring(DS_MATCHED_DATA_INIT_POS), origin);
        if (numberHighlightOffset != null && numberHighlightOffset.size() > 1) {
//            style.setSpan(new StyleSpan(Typeface.BOLD), numberHighlightOffset.get(0),
//                    numberHighlightOffset.get(1) + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            style.setSpan(new ForegroundColorSpan(mContext.getColor(R.color.phone_number_text_select_color)), numberHighlightOffset.get(0),
                    numberHighlightOffset.get(1) + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return style;
    }


    private String numberLeftToRight(String origin) {
        return TextUtils.isEmpty(origin) ? origin : '\u202D' + origin + '\u202C';
    }

    private static class ViewHolder {
        public TextView primaryCallInfo;
        public TextView secondaryCallInfo;
        public ImageButton callDetail;
//        public TextView callDate;
    }


    public static final int CALLLOG_DETAIL = 0;
    public static final int CONTACT_DETAIL = 1;
    public static final int NEW_CONTACT = 2;
    //register a listener for detail item.
    interface MoreFetcher{
        public void fetchMore(int type,int position,View view);
    }
}
