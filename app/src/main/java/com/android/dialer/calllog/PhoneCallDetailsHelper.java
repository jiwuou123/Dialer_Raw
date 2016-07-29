/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.telecom.PhoneAccount;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.android.contacts.common.testing.NeededForTesting;
import com.android.contacts.common.util.PhoneNumberHelper;
import com.android.dialer.PhoneCallDetails;
import com.android.dialer.R;
import com.android.dialer.util.DialerUtils;
import com.android.dialer.util.PhoneNumberUtil;

import com.google.common.collect.Lists;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * Helper class to fill in the views in {@link PhoneCallDetailsViews}.
 */
public class PhoneCallDetailsHelper {
    /** The maximum number of icons will be shown to represent the call types in a group. */
    private static final int MAX_CALL_TYPE_ICONS = 3;

    private final Context mContext;
    private final Resources mResources;
    /** The injected current time in milliseconds since the epoch. Used only by tests. */
    private Long mCurrentTimeMillisForTest;
    private final TelecomCallLogCache mTelecomCallLogCache;

    /**
     * List of items to be concatenated together for accessibility descriptions
     */
    private ArrayList<CharSequence> mDescriptionItems = Lists.newArrayList();

    private final boolean DEG = false;
    private final static String TAG  = "PhoneCallDetailsHelper";

    /**
     * Creates a new instance of the helper.
     * <p>
     * Generally you should have a single instance of this helper in any context.
     *
     * @param resources used to look up strings
     */
    public PhoneCallDetailsHelper(
            Context context,
            Resources resources,
            TelecomCallLogCache telecomCallLogCache) {
        mContext = context;
        mResources = resources;
        mTelecomCallLogCache = telecomCallLogCache;
    }

    /** Fills the call details views with content. */
    public void setPhoneCallDetails(PhoneCallDetailsViews views, PhoneCallDetails details) {
        // Display up to a given number of icons.
        int type = -1;
        views.callTypeIcons.clear();
        int count = details.callTypes.length;
        type = details.callTypes[0];
        boolean isVoicemail = false;
//        for (int index = 0; index < count && index < MAX_CALL_TYPE_ICONS; ++index) {
//            views.callTypeIcons.add(details.callTypes[index]);
//            if (index == 0) {
//                isVoicemail = details.callTypes[index] == Calls.VOICEMAIL_TYPE;
//            }
//
//        }
        for (int index = 0; index < count && index < MAX_CALL_TYPE_ICONS; ++index) {
            //拒接类型
            if (details.duration == 0){
                if (Calls.INCOMING_TYPE == details.callTypes[index]){
                    views.callTypeIcons.add(CallTypeIconsView.ALL_REJECT);
                    type = CallTypeIconsView.ALL_REJECT;
                    Log.d(TAG, " --- setPhoneCallDetails ---   INCOMING_TYPE");
                } else {
                    views.callTypeIcons.add(details.callTypes[index]);
                }
            } else {
                views.callTypeIcons.add(details.callTypes[index]);
            }


            if (index == 0) {
                isVoicemail = details.callTypes[index] == Calls.VOICEMAIL_TYPE;
            }
            break;
        }

        // Show the video icon if the call had video enabled.
        views.callTypeIcons.setShowVideo(
                (details.features & Calls.FEATURES_VIDEO) == Calls.FEATURES_VIDEO);
        views.callTypeIcons.requestLayout();
        views.callTypeIcons.setVisibility(View.VISIBLE);

        // Show the total call count only if there are more than the maximum number of icons.
        final Integer callCount;
        if (count > MAX_CALL_TYPE_ICONS) {
            callCount = count;
        } else {
            callCount = null;
        }

//      CharSequence callLocationAndDate = getCallLocationAndDate(details);
        CharSequence callLocationAndDate = getCallLocation(details);

        // BBK liupengfei set date and time
        setCallDataAndTime(views,getCallDate(details));

        // Set the call count, location and date.
        setCallCountAndDate(views, null, callLocationAndDate);

        // Set the account label if it exists.
        String accountLabel = mTelecomCallLogCache.getAccountLabel(details.accountHandle);

        if (accountLabel != null) {
            views.callAccountLabel.setVisibility(View.VISIBLE);
            views.callAccountLabel.setText(accountLabel);
            int color = PhoneAccountUtils.getAccountColor(mContext, details.accountHandle);
            if (color == PhoneAccount.NO_HIGHLIGHT_COLOR) {
                int defaultColor = R.color.dialtacts_secondary_text_color;
                views.callAccountLabel.setTextColor(mContext.getResources().getColor(defaultColor));
            } else {
                views.callAccountLabel.setTextColor(color);
            }
        } else {
            views.callAccountLabel.setVisibility(View.GONE);
        }



        final CharSequence nameText;
        final CharSequence displayNumber = details.displayNumber;
        if (TextUtils.isEmpty(details.name)) {
            nameText = displayNumber;
            // We have a real phone number as "nameView" so make it always LTR
            views.nameView.setTextDirection(View.TEXT_DIRECTION_LTR);
        } else {
            nameText = details.name;
        }
        int missColor  = R.color.bbk_miss_call_color;
        int callColor = R.color.bbk_call_color;
        if(type == Calls.MISSED_TYPE||type == CallTypeIconsView.ALL_REJECT)views.nameView.setTextColor(mResources.getColor(missColor));
        else views.nameView.setTextColor(mResources.getColor(callColor));

        views.nameView.setText(nameText);

        if (isVoicemail && !TextUtils.isEmpty(details.transcription)) {
            views.voicemailTranscriptionView.setText(details.transcription);
            views.voicemailTranscriptionView.setVisibility(View.VISIBLE);
        } else {
            views.voicemailTranscriptionView.setText(null);
            views.voicemailTranscriptionView.setVisibility(View.GONE);
        }

        // Bold if not read
        Typeface typeface = details.isRead ? Typeface.SANS_SERIF : Typeface.DEFAULT_BOLD;
        views.nameView.setTypeface(typeface);
        views.voicemailTranscriptionView.setTypeface(typeface);
        views.callLocationAndDate.setTypeface(typeface);
    }

    /**
     *modify by liupengfei to get call location only!
     * Builds a string containing the call location .
     *
     * @param details The call details.
     * @return The call location and date string.
     */
    private CharSequence getCallLocation(PhoneCallDetails details) {
        // Get type of call (ie mobile, home, etc) if known, or the caller's location.
        //CharSequence callTypeOrLocation = getCallTypeOrLocation(details);//不再显示手机存储状态
        CharSequence callTypeOrLocation = details.geocode;
        if (!TextUtils.isEmpty(details.name)){
            callTypeOrLocation = details.number;
        } else {
            if(TextUtils.isEmpty(callTypeOrLocation))callTypeOrLocation = mResources.getString(R.string.unkown_location);
        }

        return callTypeOrLocation;
    }

    /**
     * Builds a string containing the call location and date.
     *
     * @param details The call details.
     * @return The call location and date string.
     */
    private CharSequence getCallLocationAndDate(PhoneCallDetails details) {
        mDescriptionItems.clear();

        // Get type of call (ie mobile, home, etc) if known, or the caller's location.
        CharSequence callTypeOrLocation = getCallTypeOrLocation(details);

        // Only add the call type or location if its not empty.  It will be empty for unknown
        // callers.
        if (!TextUtils.isEmpty(callTypeOrLocation)) {
            mDescriptionItems.add(callTypeOrLocation);
        }
        // The date of this call, relative to the current time.
        mDescriptionItems.add(getCallDate(details));

        // Create a comma separated list from the call type or location, and call date.
        return DialerUtils.join(mResources, mDescriptionItems);
    }

    /**
     * For a call, if there is an associated contact for the caller, return the known call type
     * (e.g. mobile, home, work).  If there is no associated contact, attempt to use the caller's
     * location if known.
     * @param details Call details to use.
     * @return Type of call (mobile/home) if known, or the location of the caller (if known).
     */
    public CharSequence getCallTypeOrLocation(PhoneCallDetails details) {
        CharSequence numberFormattedLabel = null;
        // Only show a label if the number is shown and it is not a SIP address.
        if (!TextUtils.isEmpty(details.number)
                && !PhoneNumberHelper.isUriNumber(details.number.toString())
                && !mTelecomCallLogCache.isVoicemailNumber(details.accountHandle, details.number)) {

            if (TextUtils.isEmpty(details.name) && !TextUtils.isEmpty(details.geocode)) {
                numberFormattedLabel = details.geocode;
            } else if (!(details.numberType == Phone.TYPE_CUSTOM
                    && TextUtils.isEmpty(details.numberLabel))) {
                // Get type label only if it will not be "Custom" because of an empty number label.
                numberFormattedLabel = Phone.getTypeLabel(
                        mResources, details.numberType, details.numberLabel);
            }
        }

        if (!TextUtils.isEmpty(details.name) && TextUtils.isEmpty(numberFormattedLabel)) {
            numberFormattedLabel = details.displayNumber;
        }
        return numberFormattedLabel;
    }

    /**
     * Get the call date/time of the call, relative to the current time.
     * e.g. 3 minutes ago
     * @param details Call details to use.
     * @return String representing when the call occurred.
     */
    public CharSequence getCallDate(PhoneCallDetails details) {
        /*return DateUtils.getRelativeTimeSpanString(details.date,
                getCurrentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE);*/
        boolean isSameDay = false;
        CharSequence month,time,time1;
        month = getRelativeTimeSpanStringForIphone(details.date,System.currentTimeMillis());
        time = DateFormat.format("HH:mm", details.date);
        time1 = getTime(details);
        isSameDay = DateUtils.isToday(details.date);
        if(isSameDay)return time1;
        else  {

            return  month+"::"+time1;
        }

    }

    /**
     *获取时间
     **/
    private CharSequence getTime(PhoneCallDetails details){
        // Set the date.
        CharSequence dateValue = DateUtils.formatDateRange(mContext, details.date, details.date,
                DateUtils.FORMAT_SHOW_TIME);
        return dateValue;
    }
    //根据iPhone规则来获取时间格式
    public String getRelativeTimeSpanStringForIphone(long time,long now){
        SimpleDateFormat formatter = null;//时间标准格式生成器
        Resources res = mContext.getResources();

        if(DEG)
            Log.e("liupengfei","time = "+time+",now = "+now);

        formatter = new SimpleDateFormat("yyyy-MM-dd");//获取输入时间年月日格式
        String yearMonthDay = formatter.format(time);
        if(time>now){//如果输入时间是未来时间，则显示年月日
            return yearMonthDay;
        }

        formatter = new SimpleDateFormat("E");
        String dayOfWeek = formatter.format(time);//获取输入时间星期

        if(DEG)
            Log.e("liupengfei","dayOfWeek = "+dayOfWeek);

        formatter = new SimpleDateFormat("kk:mm");
        String hourMinuOfTime = formatter.format(time);//获取输入时间与分

        if(DEG)
            Log.e("liupengfei","hourMinuOfTime = "+hourMinuOfTime);

        formatter = new SimpleDateFormat("kk:mm:ss");
        String hourMinuSecOfNow = formatter.format(now);//获取当前时间时分秒
        long millisecOfNow = 0;
        try {
            millisecOfNow = formatter.parse(hourMinuSecOfNow).getTime();//获取当前时间时分秒的毫秒数
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if(DEG)
            Log.e("liupengfei","hourMinuSecOfNow = "+hourMinuSecOfNow+",millisecOfNow = "+millisecOfNow);

        if(DateUtils.isToday(time)){

            String timeOfCurrentDay = hourMinuOfTime;
            String[] hourAndminute = timeOfCurrentDay.split(":");
            int hour =Integer.parseInt(hourAndminute[0]);

            if(DEG){
                Log.e("liupengfei","timeOfCurrentDay = "+timeOfCurrentDay);
                Log.e("liupengfei","hourAndminute = "+hourAndminute);
                Log.e("liupengfei","hour = "+hour);
            }

            ContentResolver cv = mContext.getContentResolver();
            String strTimeFormat = android.provider.Settings.System.getString(cv,android.provider.Settings.System.TIME_12_24);
            //判断是否是12小时制
            if(DEG)
                Log.e("liupengfei","strTimeFormat = "+strTimeFormat);
            if(strTimeFormat!=null){
                if(strTimeFormat.equals("12")){
                    if(hour>12){

                        return  res.getString(R.string.call_log_pm)+hour%12+":"+hourAndminute[1];
                    }else{

                        return  res.getString(R.string.call_log_am)+hour%12+":"+hourAndminute[1];
                    }
                }else{

                    return hour%24+":"+hourAndminute[1];
                }
            }else{

                return hour%24+":"+hourAndminute[1];
            }
        }else{
//            if(now-518400000l-millisecOfNow>time){       //此时为一周以前
//
//                return yearMonthDay;
//            }else{
//
//                if(now-millisecOfNow-86400000l<time){		     //此时为昨天
//
//                    return res.getString(R.string.call_log_header_yesterday);
//                }else{
//
//                    return dayOfWeek;
//                }
//            }
//            if(now-518400000l-millisecOfNow>time){       //此时为一周以前
//
//                return yearMonthDay;
//            }else{

                if(now-millisecOfNow-86400000l<time){		     //此时为昨天

                    return res.getString(R.string.call_log_header_yesterday);
                }else{

                    return yearMonthDay;
                }
//            }

        }
    }

    private void setCallDataAndTime(PhoneCallDetailsViews views,
                                    CharSequence dateText){
        String dateTestString = dateText.toString();
        String date[] = dateTestString.split("::");
        if(DEG)
            Log.e("liupengfei","setCallDataAndTime date.length ="+date.length+",dateTestString = "+dateTestString);
        if(date.length > 1){
//            views.dataAndTime2.setVisibility(View.VISIBLE);
//            views.dataAndTime.setText(date[0]);
//            views.dataAndTime2.setText(date[1]);
            views.dataAndTime2.setVisibility(View.VISIBLE);
            views.dataAndTime2.setText(date[0]);
            views.dataAndTime.setVisibility(View.GONE);
            if(DEG)
                Log.e("liupengfei","setCallDataAndTime date[0] ="+date[0]+",date[1] = "+date[1]);
        }else{
//            views.dataAndTime2.setVisibility(View.GONE);
//            views.dataAndTime.setText(dateText);
            views.dataAndTime2.setText(dateText);
            views.dataAndTime.setVisibility(View.GONE);
        }
    }

    /** Sets the text of the header view for the details page of a phone call. */
    @NeededForTesting
    public void setCallDetailsHeader(TextView nameView, PhoneCallDetails details) {
        final CharSequence nameText;
        if (!TextUtils.isEmpty(details.name)) {
            nameText = details.name;
        } else if (!TextUtils.isEmpty(details.displayNumber)) {
            nameText = details.displayNumber;
        } else {
            nameText = mResources.getString(R.string.unknown);
        }

        nameView.setText(nameText);
    }

    @NeededForTesting
    public void setCurrentTimeForTest(long currentTimeMillis) {
        mCurrentTimeMillisForTest = currentTimeMillis;
    }

    /**
     * Returns the current time in milliseconds since the epoch.
     * <p>
     * It can be injected in tests using {@link #setCurrentTimeForTest(long)}.
     */
    private long getCurrentTimeMillis() {
        if (mCurrentTimeMillisForTest == null) {
            return System.currentTimeMillis();
        } else {
            return mCurrentTimeMillisForTest;
        }
    }

    /** Sets the call count and date. */
    private void setCallCountAndDate(PhoneCallDetailsViews views, Integer callCount,
            CharSequence dateText) {
        // Combine the count (if present) and the date.
        final CharSequence text;
        if (callCount != null) {
            text = mResources.getString(
                    R.string.call_log_item_count_and_date, callCount.intValue(), dateText);
        } else {
            text = dateText;
        }

        views.callLocationAndDate.setText(text);
    }


}
