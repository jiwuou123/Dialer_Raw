/**
 * created by jiang, 15/12/23
 * Copyright (c) 2015, jyuesong@gmail.com All Rights Reserved.
 * *                #                                                   #
 * #                       _oo0oo_                     #
 * #                      o8888888o                    #
 * #                      88" . "88                    #
 * #                      (| -_- |)                    #
 * #                      0\  =  /0                    #
 * #                    ___/`---'\___                  #
 * #                  .' \\|     |# '.                 #
 * #                 / \\|||  :  |||# \                #
 * #                / _||||| -:- |||||- \              #
 * #               |   | \\\  -  #/ |   |              #
 * #               | \_|  ''\---/''  |_/ |             #
 * #               \  .-\__  '-'  ___/-. /             #
 * #             ___'. .'  /--.--\  `. .'___           #
 * #          ."" '<  `.___\_<|>_/___.' >' "".         #
 * #         | | :  `- \`.;`\ _ /`;.`/ - ` : | |       #
 * #         \  \ `_.   \_ __\ /__ _/   .-` /  /       #
 * #     =====`-.____`.___ \_____/___.-`___.-'=====    #
 * #                       `=---='                     #
 * #     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~   #
 * #                                                   #
 * #               佛祖保佑         永无BUG              #
 * #                                                   #
 */

package com.android.dialer.bbkwidget;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import java.util.List;


/**
 * Created by jiang on 15/12/23.
 * 判断当前recyclerview的滑动事件，判断需不需要让划出来的按钮回去
 */
public class TouchableRecyclerView extends RecyclerView {

    private Context mContext;
    private int position;
    private Rect mTouchFrame;
    private int Slop;
    private SwipeItemLayout itemLayout;
    private final static String TAG = "TouchableRecyclerView";
    private List<Integer> mList;


    public TouchableRecyclerView(Context context) {
        this(context, null);
    }

    public TouchableRecyclerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TouchableRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initEvent(context);
    }

    private void initEvent(Context context) {
        mContext = context;
        Slop = ViewConfiguration.get(mContext).getScaledEdgeSlop();
    }


    /**
     * 判断 当前手势触摸的距离是否为拖动的最小距离
     *
     * @param e
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent e) {

        int dx = 0;
        int dy = 0;

        switch (e.getAction()) {

                case MotionEvent.ACTION_DOWN:{
//                    int x = (int) e.getX();
//                    int y = (int) e.getY();
//
//
//                int firstVisibleItemPosition = ((LinearLayoutManager) getLayoutManager()).findFirstVisibleItemPosition();
//                Rect fame = mTouchFrame;
//                if (fame == null) {
//                    mTouchFrame = new Rect();
//                    fame = mTouchFrame;
//                }
//                int count = getChildCount();
//                for (int i = count - 1; i >= 0; i--) {
//                    final View child = getChildAt(i);
//                    if (child.getVisibility() == View.VISIBLE) {
//                        child.getHitRect(fame);
//                        if (fame.contains(x, y)) {
//                            position = firstVisibleItemPosition + i;
//                            Log.d(TAG," --- pos --- " + position);
//                        }
//                    }
//
//                }
//                // 通过position得到Item的viewHolder
//                View view = getChildAt(position - firstVisibleItemPosition);
//                MyViewHolder viewHolder = (MyViewHolder) getChildViewHolder(view);
//                itemLayout = viewHolder.getmRoot();
//                    itemLayout.setSwipeAble(true);
                }

                    break;
                case MotionEvent.ACTION_MOVE:
                    int tempX = (int) e.getX();
                    int tempY = (int) e.getY();
                    if (Math.abs(dx - tempX) > Slop && Math.abs(tempY - dy) > Slop) {
                        closeAllOpenedItem();
                    }
                    break;



        }


        return super.onTouchEvent(e);

    }




    public void closeAllOpenedItem() {
        if (getAdapter() != null)
            ((ContactAdapter) getAdapter()).closeOpenedSwipeItemLayoutWithAnim();
    }

}
