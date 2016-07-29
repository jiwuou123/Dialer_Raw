package com.android.dialer.bbkwidget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.view.View;
import android.widget.RelativeLayout;

/**
 * Created by Administrator on 2016/7/28.
 */

public class CallLogEmptyView extends RelativeLayout implements View.OnClickListener {

    public CallLogEmptyView(Context context) {
        super(context);
    }

    public CallLogEmptyView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public CallLogEmptyView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CallLogEmptyView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onClick(View v) {

    }
}
