/**
 * created by jiang, 12/3/15
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
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.android.dialer.R;

import java.util.ArrayList;
import java.util.List;

//import com.jiang.android.indexrecyclerview.adapter.expandRecyclerviewadapter.StickyRecyclerHeadersAdapter;
//import com.jiang.android.indexrecyclerview.model.ContactModel;

/**
 * 根据当前权限进行判断相关的滑动逻辑
 */
public class ContactAdapter extends RecyclerView.Adapter {
    /**
     * 当前处于打开状态的item
     */
    private List<SwipeItemLayout> mOpenedSil = new ArrayList<>();

    //    private List<ContactModel.MembersEntity> mLists;
    private List<Integer> mLists;

    private Context mContext;
    private int mPermission;
    private String createrID;
    private boolean isCreator;


    public static final String OWNER = "1";
    public static final String CREATER = "1";
    public static final String STUDENT = "student";
    private final static String TAG = "ContactAdapter";

    public ContactAdapter(Context ct, List<Integer> mLists) {
        this.mLists = mLists;
        mContext = ct;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(parent.getContext())
//                .inflate(R.layout.item_contact, parent, false);
//        return new MyViewHolder(view);
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        MyViewHolder viewHolder = (MyViewHolder) holder;
        SwipeItemLayout swipeRoot = viewHolder.mRoot;


        swipeRoot.setSwipeAble(true);
        swipeRoot.setDelegate(new SwipeItemLayout.SwipeItemLayoutDelegate() {
            @Override
            public void onSwipeItemLayoutOpened(SwipeItemLayout swipeItemLayout) {
                closeOpenedSwipeItemLayoutWithAnim();
                mOpenedSil.add(swipeItemLayout);
                Log.e(TAG, " ++++++++++++++  ");
            }

            @Override
            public void onSwipeItemLayoutClosed(SwipeItemLayout swipeItemLayout) {
                mOpenedSil.remove(swipeItemLayout);
                Log.e(TAG, " --- mOpenedSil.remove --- ");
            }

            @Override
            public void onSwipeItemLayoutStartOpen(SwipeItemLayout swipeItemLayout) {
                closeOpenedSwipeItemLayoutWithAnim();
                Log.e(TAG, "  StartOpen ");
            }
        });
        viewHolder.mDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                                ((MainActivity) mContext).deleteUser(position);
                removeRecycleItem(position);
                closeOpenedSwipeItemLayout();
            }
        });

        TextView textView = viewHolder.mName;
        textView.setText("第" + String.valueOf(mLists.get(position)) + "个");


    }

    @Override
    public int getItemCount() {
        return mLists.size();
    }


    public void removeRecycleItem(int position) {
        mLists.remove(position);
        notifyDataSetChanged();
        if (mLists.size() == 0) {
            Toast.makeText(mContext, " 已经没数据咯！！！", Toast.LENGTH_SHORT).show();
        }
    }

    public void closeOpenedSwipeItemLayoutWithAnim() {

        for (SwipeItemLayout sil : mOpenedSil) {
            sil.closeWithAnim();
        }
        mOpenedSil.clear();
    }

    public void closeOpenedSwipeItemLayout() {
        for (SwipeItemLayout sil : mOpenedSil) {

            sil.close();
        }
        mOpenedSil.clear();
    }


}
