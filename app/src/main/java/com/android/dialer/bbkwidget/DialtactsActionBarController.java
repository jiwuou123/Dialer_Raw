package com.android.dialer.bbkwidget;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by Administrator on 2016/7/27.
 */

public class DialtactsActionBarController {

    private ImageView mActionMenu;
    private TextView mActionNameTxt;
    private TextView mCanselTxt;
    private TextView mEditerTxt;
    private DialtactUi mDialtactUi;
    private static final String TAG = "DialtactsActionBarContr";

    public interface DialtactUi {


    }

    public DialtactsActionBarController(ImageView mActionMenu, TextView mEditerTxt, TextView mCanselTxt, TextView mActionNameTxt, DialtactUi dialtactUi) {
        this.mActionMenu = mActionMenu;
        this.mEditerTxt = mEditerTxt;
        this.mCanselTxt = mCanselTxt;
        this.mActionNameTxt = mActionNameTxt;
        this.mDialtactUi = dialtactUi;
    }

    public void setActionName(String s){
        mActionNameTxt.setText(s);
        Log.d(TAG, " actionName : "+ s);

    }


    public void setActionMenuIcon(int res){

            mActionMenu.setImageResource(res);

    }


    public String getEditerText(){
        return mEditerTxt.getText().toString().trim();
    }


    public void showCanselTxt(boolean b){
        if (b) mCanselTxt.setVisibility(View.VISIBLE);
        else mCanselTxt.setVisibility(View.GONE);
    }





    public void setEditerTxt(String s){
        mEditerTxt.setText(s);
    }

    public void showActionMenu(boolean b){
        if (b) mActionMenu.setVisibility(View.VISIBLE);
        else mActionMenu.setVisibility(View.GONE);
    }


    public ImageView getmActionMenu() {
        return mActionMenu;
    }

    public TextView getmActionNameTxt() {
        return mActionNameTxt;
    }
}
