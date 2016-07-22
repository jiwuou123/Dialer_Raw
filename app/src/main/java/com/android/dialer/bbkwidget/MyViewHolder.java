package com.android.dialer.bbkwidget;

import android.view.View;
import android.widget.TextView;

public class MyViewHolder extends android.support.v7.widget.RecyclerView.ViewHolder {

    public TextView mName;
    public SwipeItemLayout mRoot;
    public TextView mDelete;

    public MyViewHolder(View itemView) {
        super(itemView);
        mName = (TextView) itemView.findViewById(R.id.item_contact_title);
        mRoot = (SwipeItemLayout) itemView.findViewById(R.id.item_contact_swipe_root);
        mDelete = (TextView) itemView.findViewById(R.id.item_contact_delete);


    }

    public void setmName(TextView mName) {
        this.mName = mName;
    }

    public void setmDelete(TextView mDelete) {
        this.mDelete = mDelete;
    }

    public void setmRoot(SwipeItemLayout mRoot) {
        this.mRoot = mRoot;
    }

    public SwipeItemLayout getmRoot() {
        return mRoot;
    }

    public TextView getmDelete() {
        return mDelete;
    }

    public TextView getmName() {
        return mName;
    }

//    private TextView txt;
//
//    private LinearLayout layout;
//    public static MyViewHolder create(View rootView){
//        return new MyViewHolder(rootView);
//    }
//
//    private MyViewHolder(View itemView) {
//        super(itemView);
//        txt= (TextView) itemView.findViewById(R.id.item_recycler_txt);
//        layout= (LinearLayout) itemView.findViewById(R.id.item_recycler_ll);
//    }
//
//    public TextView getTxt() {
//        return txt;
//    }
//
//    public void setTxt(TextView txt) {
//        this.txt = txt;
//    }
//
//    public LinearLayout getLayout() {
//        return layout;
//    }
//
//    public void setLayout(LinearLayout layout) {
//        this.layout = layout;
//    }
}
