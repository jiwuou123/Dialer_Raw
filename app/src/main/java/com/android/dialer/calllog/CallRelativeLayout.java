package com.android.dialer.calllog;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

/**
 * Created by Administrator on 2016/7/23.
 */

public class CallRelativeLayout extends RelativeLayout{
    public CallRelativeLayout(Context context) {
        super(context);
    }

    public CallRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CallRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setPressed(boolean pressed) {
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).setPressed(pressed);
        }
        super.setPressed(pressed);
    }
}
