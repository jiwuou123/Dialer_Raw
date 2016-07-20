package com.android.dialer.calllog;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.dialer.PhoneCallDetails;
import com.android.dialer.R;

/**
 * Created by Administrator on 2016/7/20.
 */

public class CallDetailPhoneNumberAdapter extends BaseAdapter{
    private final Context mContext;
    private final LayoutInflater mLayoutInflater;
    private final PhoneCallDetails.PhoneNumberEntity[] phoneNumberEntities;
    private CallDetailCallback callDetailCallback;
    public CallDetailPhoneNumberAdapter(Context context, PhoneCallDetails.PhoneNumberEntity[] phoneNumberEntities) {
        mContext = context;
        mLayoutInflater = LayoutInflater.from(context);
        this.phoneNumberEntities = phoneNumberEntities;
    }

    public void setCallDetailCallback(CallDetailCallback callDetailCallback) {
        this.callDetailCallback = callDetailCallback;
    }

    @Override
    public int getCount() {
        return phoneNumberEntities.length;
    }

    @Override
    public Object getItem(int position) {
        return phoneNumberEntities[position];
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if(convertView == null){
            convertView = mLayoutInflater.inflate(R.layout.call_detail_contact_number_item,null);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        }else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        if(callDetailCallback!=null) {
            viewHolder.call.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    callDetailCallback.call(v, position);
                }
            });
            viewHolder.sendMessage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    callDetailCallback.senMessage(v, position);
                }
            });
        }
        String location = phoneNumberEntities[position].location;
        if(!TextUtils.isEmpty(location))
            viewHolder.location.setText(location);
        viewHolder.phoneNumber.setText(phoneNumberEntities[position].phoneNumber);
        return convertView;
    }


    public static class ViewHolder{
        public TextView phoneNumber;
        public TextView location;
        public ImageView call;
        public ImageView sendMessage;
        public ViewHolder(View itemView){
            phoneNumber = (TextView) itemView.findViewById(R.id.cd_phonel);
            location = (TextView) itemView.findViewById(R.id.cd_local);
            call = (ImageView) itemView.findViewById(R.id.bt_phone);
            sendMessage = (ImageView) itemView.findViewById(R.id.bt_message);
        }
    }
    public interface CallDetailCallback{
        void call(View v, int position);
        void senMessage(View v, int position);
    }
}