package com.android.dialer;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.android.contacts.common.activity.TransactionSafeActivity;
import com.android.contacts.common.interactions.TouchPointManager;

/**
 * Created by Administrator on 2016/7/26.
 */
public class TestFragmentActivity  extends TransactionSafeActivity {
    private static final String TAG_Dialtacts_Fragment = "dialtacts_fragment";
    private DialtactsFragment mDlf = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_fragment_layout);

        //绑定fragment
        final FragmentTransaction transaction = getFragmentManager().beginTransaction();
        DialtactsFragment fragment = new DialtactsFragment();

        mDlf = fragment;

        transaction.add(R.id.test_fragment_back, fragment);
        transaction.commitAllowingStateLoss();

    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(false);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            TouchPointManager.getInstance().setPoint((int) ev.getRawX(), (int) ev.getRawY());
        }
        return super.dispatchTouchEvent(ev);

    }

    @Override
    protected void onNewIntent(Intent newIntent) {
        super.onNewIntent(newIntent);

        if (mDlf != null && mDlf.isVisible()){
            setIntent(newIntent);

            mDlf.mStateSaved = false;
            mDlf.displayFragment(newIntent);

        }
    }

    @Override
    public void onBackPressed() {
        if (mDlf != null && mDlf.isVisible()){
            if (mDlf.mStateSaved) {
                return;
            }
            if (mDlf.mIsDialpadShown) {
                if(!mDlf.isInSearchUi()) {
                    finish();
                    return;
                }
                if(mDlf.mSmartDialSearchFragment!=null && mDlf.mSmartDialSearchFragment.popupWindowIsShowing()){
                    mDlf.mSmartDialSearchFragment.popupWindowDismiss();
                }else {
                    if (TextUtils.isEmpty(mDlf.mSearchQuery) ||
                            (mDlf.mSmartDialSearchFragment != null && mDlf.mSmartDialSearchFragment.isVisible()
                                    && mDlf.mSmartDialSearchFragment.getAdapter().getCount() == 0)) {
                        mDlf.exitSearchUi();
                    }
                    mDlf.hideDialpadFragment(true, true);
                }
//            hideDialpadFragment(true, true);
//        } else if (isInSearchUi()) {
//            exitSearchUi();
//            DialerUtils.hideInputMethod(mParentLayout);
            } else {
                super.onBackPressed();
            }
        }


    }
}
