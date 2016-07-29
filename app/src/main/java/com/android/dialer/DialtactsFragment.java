/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.dialer;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Trace;
import android.provider.CallLog.Calls;
import android.speech.RecognizerIntent;
import android.support.v4.view.ViewPager;
import android.telecom.PhoneAccount;
import android.telecom.TelecomManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnDragListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView.OnScrollListener;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.contacts.common.activity.TransactionSafeActivity;
import com.android.contacts.common.dialog.ClearFrequentsDialog;
import com.android.contacts.common.interactions.ImportExportDialogFragment;
import com.android.contacts.common.list.OnPhoneNumberPickerActionListener;
import com.android.contacts.common.util.PermissionsUtil;
import com.android.contacts.commonbind.analytics.AnalyticsUtil;
import com.android.dialer.bbk.RecyclerViewChangedImpl;
import com.android.dialer.bbk.SelectedCallLogImpl;
import com.android.dialer.bbkwidget.DialtactsActionBarController;
import com.android.dialer.bbkwidget.FloatingActionButtonController;
import com.android.dialer.calllog.CallLogActivity;
import com.android.dialer.calllog.CallLogFragment;
import com.android.dialer.calllog.CallLogQueryHandler;
import com.android.dialer.database.DialerDatabaseHelper;
import com.android.dialer.dialpad.DialpadFragment;
import com.android.dialer.dialpad.SmartDialNameMatcher;
import com.android.dialer.dialpad.SmartDialPrefix;
import com.android.dialer.interactions.PhoneNumberInteraction;
import com.android.dialer.list.DragDropController;
import com.android.dialer.list.ListsFragment;
import com.android.dialer.list.OnDragDropListener;
import com.android.dialer.list.OnListFragmentScrolledListener;
import com.android.dialer.list.PhoneFavoriteSquareTileView;
import com.android.dialer.list.RegularSearchFragment;
import com.android.dialer.list.SearchFragment;
import com.android.dialer.list.SmartDialSearchFragment;
import com.android.dialer.list.SpeedDialFragment;
import com.android.dialer.util.DialerUtils;
import com.android.dialer.util.IntentUtil;
import com.android.dialer.widget.ActionBarController;
import com.android.dialer.widget.SearchEditTextLayout;
import com.android.dialerbind.DatabaseHelperManager;
import com.android.phone.common.animation.AnimUtils;
import com.android.dialer.bbkwidget.DialpadFloatingActionButtonController;
import com.android.phone.common.animation.AnimationListenerAdapter;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

import static android.support.v7.widget.StaggeredGridLayoutManager.TAG;

//import com.android.contacts.common.widget.FloatingActionButtonController;

/**
 * The dialer tab's title is 'phone', a more common name (see strings.xml).
 */
public class DialtactsFragment extends TransactionSafeFragment implements View.OnClickListener,
        DialpadFragment.OnDialpadQueryChangedListener,
        OnListFragmentScrolledListener,
        CallLogFragment.HostInterface,
        DialpadFragment.HostInterface,
        ListsFragment.HostInterface,
        SpeedDialFragment.HostInterface,
        SearchFragment.HostInterface,
        OnDragDropListener,
        OnPhoneNumberPickerActionListener,
        PopupMenu.OnMenuItemClickListener,
        ViewPager.OnPageChangeListener,
        ActionBarController.ActivityUi,
        DialtactsActionBarController.DialtactUi{

    private static final String TAG = "DialtactsFragment";

    public static final boolean DEBUG = false;

    public static final String SHARED_PREFS_NAME = "com.android.dialer_preferences";

    /** @see #getCallOrigin() */
    private static final String CALL_ORIGIN_DIALTACTS =
            "com.android.dialer.DialtactsFragment";

    private static final String KEY_IN_REGULAR_SEARCH_UI = "in_regular_search_ui";
    private static final String KEY_IN_DIALPAD_SEARCH_UI = "in_dialpad_search_ui";
    private static final String KEY_SEARCH_QUERY = "search_query";
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    private static final String KEY_IS_DIALPAD_SHOWN = "is_dialpad_shown";

    private static final String TAG_DIALPAD_FRAGMENT = "dialpad";
    private static final String TAG_REGULAR_SEARCH_FRAGMENT = "search";
    private static final String TAG_SMARTDIAL_SEARCH_FRAGMENT = "smartdial";
    private static final String TAG_FAVORITES_FRAGMENT = "favorites";

    /**
     *BBK-liupengfei for controll searchview hide or show
    */
    private boolean mSearchViewShow = false;
    private CallLogFragment mCalllogList = null;
    /**
     * add editer button
     **/
    private TextView mEditerToCalldetail;
    private Handler reflushEditerViewHandler;

    /**
     *  bbk wangchunhe 2016/07/12 for popupwindow
     */
    private PopupWindow mCallLogSelectPopupWindow;

    /**
     *   add dialtacts bottom menu button
     */
    private ImageButton mMenuButtonCall;
    private ImageButton mMenuButtonContacts;
    private ImageButton mMenuButtonSetting;
    private ImageButton mMenuButtonDelete;


    /**

    /**
     * Just for backward compatibility. Should behave as same as {@link Intent#ACTION_DIAL}.
     */
    private static final String ACTION_TOUCH_DIALER = "com.android.phone.action.TOUCH_DIALER";
    public static final String EXTRA_SHOW_TAB = "EXTRA_SHOW_TAB";

    private static final int ACTIVITY_REQUEST_CODE_VOICE_SEARCH = 1;

    private static final int FAB_SCALE_IN_DELAY_MS = 300;

    private FrameLayout mParentLayout;

    /**
     * Fragment containing the dialpad that slides into view
     */
    public DialpadFragment mDialpadFragment;

    /**
     * Fragment for searching phone numbers using the alphanumeric keyboard.
     */
    public RegularSearchFragment mRegularSearchFragment;

    /**
     * Fragment for searching phone numbers using the dialpad.
     */
    public SmartDialSearchFragment mSmartDialSearchFragment;

    /**
     * Animation that slides in.
     */
    private Animation mSlideIn;

    /**
     * Animation that slides out.
     */
    private Animation mSlideOut;

    AnimationListenerAdapter mSlideInListener = new AnimationListenerAdapter() {
        @Override
        public void onAnimationEnd(Animation animation) {
            maybeEnterSearchUi();
        }
    };

    /**
     * Listener for after slide out animation completes on dialer fragment.
     */
    AnimationListenerAdapter mSlideOutListener = new AnimationListenerAdapter() {
        @Override
        public void onAnimationEnd(Animation animation) {
            commitDialpadFragmentHide();
        }
    };

    /**
     * Fragment containing the speed dial list, recents list, and all contacts list.
     */
    private ListsFragment mListsFragment;

    /**
     * Tracks whether onSaveInstanceState has been called. If true, no fragment transactions can
     * be commited.
     */
    public boolean mStateSaved;
    private boolean mIsRestarting;
    private boolean mInDialpadSearch;
    private boolean mInRegularSearch;
    private boolean mClearSearchOnPause;
    public boolean mIsDialpadShown;
    public boolean mShowDialpadOnResume;
    public boolean isShowMultipleDeleCallLog;

    /**
     * Whether or not the device is in landscape orientation.
     */
    private boolean mIsLandscape;

    /**
     * True if the dialpad is only temporarily showing due to being in call
     */
    public boolean mInCallDialpadUp;

    /**
     * True when this activity has been launched for the first time.
     */
    private boolean mFirstLaunch;

    /**
     * Search query to be applied to the SearchView in the ActionBar once
     * onCreateOptionsMenu has been called.
     */
    private String mPendingSearchViewQuery;

    private PopupMenu mOverflowMenu;
    private EditText mSearchView;
    private View mVoiceSearchButton;



    public String mSearchQuery;

    private DialerDatabaseHelper mDialerDatabaseHelper;
    private DragDropController mDragDropController;
    private ActionBarController mActionBarController;
    private DialtactsActionBarController mDialtactsActionBarController;


    private FloatingActionButtonController mFloatingActionButtonController;

    private int mActionBarHeight;


    /**
     * The text returned from a voice search query.  Set in {@link #onActivityResult} and used in
     * {@link #onResume()} to populate the search box.
     */
    private String mVoiceSearchQuery;

    protected class OptionsPopupMenu extends PopupMenu {
        public OptionsPopupMenu(Context context, View anchor) {
            super(context, anchor, Gravity.END);
        }

        @Override
        public void show() {
            final boolean hasContactsPermission =
                    PermissionsUtil.hasContactsPermissions(DialtactsFragment.this.getContext());
            final Menu menu = getMenu();
            final MenuItem clearFrequents = menu.findItem(R.id.menu_clear_frequents);
            clearFrequents.setVisible(mListsFragment != null &&
                    mListsFragment.getSpeedDialFragment() != null &&
                    mListsFragment.getSpeedDialFragment().hasFrequents() && hasContactsPermission);

            menu.findItem(R.id.menu_import_export).setVisible(hasContactsPermission);
            menu.findItem(R.id.menu_add_contact).setVisible(hasContactsPermission);

            menu.findItem(R.id.menu_history).setVisible(
                    PermissionsUtil.hasPhonePermissions(DialtactsFragment.this.getContext()));
            super.show();
        }
    }

    /**
     * Listener that listens to drag events and sends their x and y coordinates to a
     * {@link DragDropController}.
     */
    private class LayoutOnDragListener implements OnDragListener {
        @Override
        public boolean onDrag(View v, DragEvent event) {
            if (event.getAction() == DragEvent.ACTION_DRAG_LOCATION) {
                mDragDropController.handleDragHovered(v, (int) event.getX(), (int) event.getY());
            }
            return true;
        }
    }

    /**
     * Listener used to send search queries to the phone search fragment.
     */
    private final TextWatcher mPhoneSearchQueryTextListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            final String newText = s.toString();
            if (newText.equals(mSearchQuery)) {
                // If the query hasn't changed (perhaps due to activity being destroyed
                // and restored, or user launching the same DIAL intent twice), then there is
                // no need to do anything here.
                return;
            }
            if (DEBUG) {
                Log.d(TAG, "onTextChange for mSearchView called with new query: " + newText);
                Log.d(TAG, "Previous Query: " + mSearchQuery);
            }
            mSearchQuery = newText;

            // Show search fragment only when the query string is changed to non-empty text.
            if (!TextUtils.isEmpty(newText)) {
                // Call enterSearchUi only if we are switching search modes, or showing a search
                // fragment for the first time.
                final boolean sameSearchMode = (mIsDialpadShown && mInDialpadSearch) ||
                        (!mIsDialpadShown && mInRegularSearch);
                if (!sameSearchMode) {
                    enterSearchUi(mIsDialpadShown, mSearchQuery, true /* animate */);
                }
            }

            if (mSmartDialSearchFragment != null && mSmartDialSearchFragment.isVisible()) {
                mSmartDialSearchFragment.setQueryString(mSearchQuery, false /* delaySelection */);
            } else if (mRegularSearchFragment != null && mRegularSearchFragment.isVisible()) {
                mRegularSearchFragment.setQueryString(mSearchQuery, false /* delaySelection */);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };




    /**
     * Open the search UI when the user clicks on the search box.
     */
    private final View.OnClickListener mSearchViewOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!isInSearchUi()) {
                mActionBarController.onSearchBoxTapped();
                enterSearchUi(false /* smartDialSearch */, mSearchView.getText().toString(),
                        true /* animate */);
            }
        }
    };

    /**
     * Handles the user closing the soft keyboard.
     */
    private final View.OnKeyListener mSearchEditTextLayoutListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
                if (TextUtils.isEmpty(mSearchView.getText().toString())) {
                    // If the search term is empty, close the search UI.
                    maybeExitSearchUi();
                    /// M: end the back key dispatch to avoid activity onBackPressed is called.
                    return true;
                } else {
                    // If the search term is not empty, show the dialpad fab.
                    showFabInSearchUi();
                }
            }
            return false;
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        mFirstLaunch = true;
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.e(TAG, " --- onCreateView(Bundle savedInstanceState) ---  start ");
        Trace.beginSection(TAG + " onCreate");
        View retView = null;
        super.onCreateView(inflater, container, savedInstanceState);

//        mFirstLaunch = true;

        final Resources resources = getResources();
        int temp =  resources.getDimensionPixelSize(R.dimen.floating_action_button_width);
        //获取actionbar高度
        mActionBarHeight = resources.getDimensionPixelSize(R.dimen.action_bar_height_large);

        Trace.beginSection(TAG + " setContentView");
        //setContentView(R.layout.dialtacts_activity);
        retView = inflater.inflate(R.layout.dialtacts_activity, container, false);
        Trace.endSection();

        Trace.beginSection(TAG + " setup Views");
        final ActionBar actionBar = getActivity().getActionBar();
        //在actionbar中放入搜索框
        actionBar.setCustomView(R.layout.dialtacts_actionbar);
        actionBar.setDisplayShowCustomEnabled(true);
        TextView actionbarNameTxt = (TextView)actionBar.getCustomView().findViewById(R.id.actionbar_name);
        actionbarNameTxt.setOnClickListener(this);
        ImageView actionbarMenu = (ImageView)actionBar.getCustomView().findViewById(R.id.actionbar_menu);
        actionbarMenu.setOnClickListener(this);
        TextView editerToCalldetail = (TextView)actionBar.getCustomView().findViewById(R.id.actionbar_call_dialtacts_action_editer);
        editerToCalldetail.setOnClickListener(this);
        TextView cancelTxt = (TextView)actionBar.getCustomView().findViewById(R.id.actionbar_call_dialtacts_action_cancel);
        cancelTxt.setOnClickListener(this);
        actionbarNameTxt.setText(getString(R.string.all_calls));
        mSearchView = (EditText) actionBar.getCustomView().findViewById(R.id.edittext);
        mSearchView.setVisibility(View.GONE);
        mDialtactsActionBarController  = new DialtactsActionBarController(actionbarMenu,editerToCalldetail,cancelTxt,actionbarNameTxt,this);
//        actionBar.setBackgroundDrawable(null);

        SearchEditTextLayout searchEditTextLayout =
                (SearchEditTextLayout) actionBar.getCustomView().findViewById(R.id.search_view_container);
//        searchEditTextLayout.setPreImeKeyListener(mSearchEditTextLayoutListener);

//        mActionBarController = new DialtactsActionBarController(this, searchEditTextLayout);

//        mSearchView = (EditText) searchEditTextLayout.findViewById(R.id.search_view);
        mSearchView.addTextChangedListener(mPhoneSearchQueryTextListener);
//        mVoiceSearchButton = searchEditTextLayout.findViewById(R.id.voice_search_button);
//        mSearchView.setOnClickListener(mSearchViewOnClickListener);
//        searchEditTextLayout.findViewById(R.id.search_box_start_search)
//                .setOnClickListener(mSearchViewOnClickListener);
//        searchEditTextLayout.setOnClickListener(mSearchViewOnClickListener);
//        searchEditTextLayout.setCallback(new SearchEditTextLayout.Callback() {
//            @Override
//            public void onBackButtonClicked() {
//                onBackPressed();
//            }
//
//            @Override
//            public void onSearchViewClicked() {
//                // Hide FAB, as the keyboard is shown.
////                mFloatingActionButtonController.scaleOut();
//            }
//        });
        mIsLandscape = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;



//        ImageButton optionsMenuButton =
//                (ImageButton) searchEditTextLayout.findViewById(R.id.dialtacts_options_menu_button);
//        optionsMenuButton.setOnClickListener(this);
//        mOverflowMenu = buildOptionsMenu(searchEditTextLayout);
//        optionsMenuButton.setOnTouchListener(mOverflowMenu.getDragToOpenListener());

        // Add the favorites fragment but only if savedInstanceState is null. Otherwise the
        // fragment manager is responsible for recreating it.
        //恢复状态
        if (savedInstanceState == null) {
            getChildFragmentManager().beginTransaction()
                    .add(R.id.dialtacts_container, new DialpadFragment(getActivity()), TAG_DIALPAD_FRAGMENT)
//                   .add(R.id.dialtacts_frame, new ListsFragment(), TAG_FAVORITES_FRAGMENT)
                    .commit();
        } else {
            mSearchQuery = savedInstanceState.getString(KEY_SEARCH_QUERY);
            mInRegularSearch = savedInstanceState.getBoolean(KEY_IN_REGULAR_SEARCH_UI);
            mInDialpadSearch = savedInstanceState.getBoolean(KEY_IN_DIALPAD_SEARCH_UI);
            mFirstLaunch = savedInstanceState.getBoolean(KEY_FIRST_LAUNCH);
            mShowDialpadOnResume = savedInstanceState.getBoolean(KEY_IS_DIALPAD_SHOWN);
//            mActionBarController.restoreInstanceState(savedInstanceState);
        }

        final boolean isLayoutRtl = DialerUtils.isRtl();
        //拨号界面切换的动画设置。横屏时为上下划出，竖屏时为左右划出
        if (mIsLandscape) {
            //判断横屏方向。
            mSlideIn = AnimationUtils.loadAnimation(getActivity(),
                    isLayoutRtl ? R.anim.dialpad_slide_in_left : R.anim.dialpad_slide_in_right);
            mSlideOut = AnimationUtils.loadAnimation(getActivity(),
                    isLayoutRtl ? R.anim.dialpad_slide_out_left : R.anim.dialpad_slide_out_right);
        } else {
            mSlideIn = AnimationUtils.loadAnimation(getActivity(), R.anim.dialpad_slide_in_bottom);
            mSlideOut = AnimationUtils.loadAnimation(getActivity(), R.anim.dialpad_slide_out_bottom);
        }

//        mSlideIn.setInterpolator(AnimUtils.EASE_IN);
        mSlideOut.setInterpolator(AnimUtils.EASE_OUT);

        mSlideIn.setAnimationListener(mSlideInListener);
        mSlideOut.setAnimationListener(mSlideOutListener);



        Trace.endSection();

        Trace.beginSection(TAG + " initialize smart dialing");
        mDialerDatabaseHelper = DatabaseHelperManager.getDatabaseHelper(getActivity());
        SmartDialPrefix.initializeNanpSettings(getActivity());
        Trace.endSection();
        Trace.endSection();
        initFlushHandler();

        Log.e(TAG, " onCreateView()  dialerFragment  " +  mIsDialpadShown  );
        return retView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        final View MenuButtonContainer = view.findViewById(
                R.id.dialtacts_bottom_menu_container);
        mMenuButtonCall = (ImageButton) view.findViewById(R.id.dialtacts_bottom_menu_button_call);
        mMenuButtonCall.setOnClickListener(this);
        mMenuButtonContacts = (ImageButton) view.findViewById(R.id.dialtacts_bottom_menu_button_contacts);
        mMenuButtonContacts.setOnClickListener(this);
        mMenuButtonSetting = (ImageButton) view.findViewById(R.id.dialtacts_bottom_menu_button_group);
        mMenuButtonSetting.setOnClickListener(this);
        mMenuButtonDelete = (ImageButton)view.findViewById(R.id.dialtacts_bottom_menu_button_delete);
        mMenuButtonDelete.setOnClickListener(this);
//        mFloatingActionButtonController = new FloatingActionButtonController(this,
//                MenuButtonContainer, MenuButtonCall);

        final View floatingActionButtonContainer = view.findViewById(
                R.id.floating_action_button_container);
        //底部拨号按钮
        ImageButton floatingActionButton = (ImageButton) view.findViewById(R.id.floating_action_button);
        floatingActionButton.setOnClickListener(this);

        mFloatingActionButtonController = new FloatingActionButtonController(getActivity(),
                floatingActionButtonContainer, floatingActionButton);

        mParentLayout = (FrameLayout) view.findViewById(R.id.dialtacts_mainlayout);
        mParentLayout.setOnDragListener(new LayoutOnDragListener());
        floatingActionButtonContainer.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        final ViewTreeObserver observer =
                                floatingActionButtonContainer.getViewTreeObserver();
                        if (!observer.isAlive()) {
                            return;
                        }
                        observer.removeOnGlobalLayoutListener(this);
                        int screenWidth = mParentLayout.getWidth();
                        mFloatingActionButtonController.setScreenWidth(screenWidth);
                        mFloatingActionButtonController.align(
                                getFabAlignment(), false /* animate */);

                    }
                });
    }

    @Override
    public void onResume() {
        Trace.beginSection(TAG + " onResume");
        super.onResume();

        if (!isInSearchUi()) {
            //if(mCalllogList != null)
            //   mCalllogList.scrollToTop();
            showDialpadFragment(false);
            Log.e(TAG," ------ onResume() ----    hide 1" );
        } else {
            showSearchFragment();
            showDialpadFragment(true);
            mFloatingActionButtonController.setVisible(true);
            Log.e(TAG, " ----- onResume() -----  show 2");
        }


        mStateSaved = false;
        if (mFirstLaunch) {
            displayFragment(getActivity().getIntent());
            Log.e(TAG," onResume show ");
        } else if (!phoneIsInUse() && mInCallDialpadUp) {
            hideDialpadFragment(false, true);
            mInCallDialpadUp = false;
            Log.e(TAG, " ---- onResume() --- show 2");
        } else if (mShowDialpadOnResume) {
            showDialpadFragment(false);
            mShowDialpadOnResume = true;
            Log.e(TAG, " ---- onResume() ---- 3 ");
            if(getCallLogFragment() != null){
                mCalllogList = (CallLogFragment)getCallLogFragment();
                mCalllogList.setRecyclerViewChangedImpl(mRecyclerViewChangedImpl);
            }else {
                Log.d(TAG,"getCallLogFragment == null importent log ********");
            }
        }



        // If there was a voice query result returned in the {@link #onActivityResult} callback, it
        // will have been stashed in mVoiceSearchQuery since the search results fragment cannot be
        // shown until onResume has completed.  Active the search UI and set the search term now.
        if (!TextUtils.isEmpty(mVoiceSearchQuery)) {
//            mActionBarController.onSearchBoxTapped();
            mSearchView.setText(mVoiceSearchQuery);
//            mVoiceSearchQuery = null;
        }

        mFirstLaunch = false;

//        if (mIsRestarting) {
//            // This is only called when the activity goes from resumed -> paused -> resumed, so it
//            // will not cause an extra view to be sent out on rotation
//            if (mIsDialpadShown) {
//                AnalyticsUtil.sendScreenView(mDialpadFragment, getActivity());
//                Log.e(TAG," ----- onResume() ----- 4   mIsDialpadShown is true ");
//            }
//            mIsRestarting = false;
//        }

        prepareVoiceSearchButton();
        mDialerDatabaseHelper.startSmartDialUpdateThread();
        mFloatingActionButtonController.align(getFabAlignment(), false /* animate */);

        if (getActivity().getIntent().hasExtra(EXTRA_SHOW_TAB)) {
            int index = getActivity().getIntent().getIntExtra(EXTRA_SHOW_TAB, ListsFragment.TAB_INDEX_SPEED_DIAL);
            if (index < mListsFragment.getTabCount()) {
                mListsFragment.showTab(index);
            }
        } else if (Calls.CONTENT_TYPE.equals(getActivity().getIntent().getType())) {
            if (mListsFragment != null) {
                mListsFragment.showTab(ListsFragment.TAB_INDEX_RECENTS);
            }
        }

        actionbarMenuOpen();
        Trace.endSection();
        sendMessageToReflushEditerView();


        Log.e(TAG," onResume mFloatingActionButtonController "+ mFloatingActionButtonController.isVisible());

    }

    /**
     *send message to get calllog list after 200ms as it should need a little time to fetches.
     */
    private void sendMessageToReflushEditerView(){
        //Log.e(TAG,"reflushEditerViewHandler = "+reflushEditerViewHandler);
        if(reflushEditerViewHandler != null)
            reflushEditerViewHandler.sendMessageDelayed(reflushEditerViewHandler.obtainMessage(1), 200);
    }
    private void initFlushHandler() {
        reflushEditerViewHandler = new Handler(){
            public void handleMessage(Message msg) {
                showEditerViewIfNeed();
            };
        };
    }
    /**
     *show editerview if calllog list is not empty.
     */
    private void showEditerViewIfNeed(){
//        Log.e(TAG,"showEditerViewIfNeed mCalllogList = "+ mCalllogList);
        if(mCalllogList != null) {
            int calllogCount = mCalllogList.getListItemCount();
            Log.e(TAG," calllogCount = "+calllogCount);
            if(mEditerToCalldetail != null)
                mEditerToCalldetail.setVisibility(calllogCount == 0 ? View.INVISIBLE : View.VISIBLE);
        }
    }

    @Override
    public void onStart() {
        mIsRestarting = true;
        Log.e(TAG,"  ----- onStart() ----- " + " mIsDialpadShown is:  " + mIsDialpadShown);
        super.onStart();
    }

    //    @Override
//    protected void onRestart() {
//        super.onRestart();
//        mIsRestarting = true;
//        Log.e(TAG, "  ----- onRestart() ----- " + " mIsDialpadShown is:  " + mIsDialpadShown);
//    }

    @Override
    public void onPause() {
        if (mClearSearchOnPause) {
            hideDialpadAndSearchUi();
            mClearSearchOnPause = false;
        }
        if (mSlideOut.hasStarted() && !mSlideOut.hasEnded()) {
            commitDialpadFragmentHide();
        }
        Log.e(TAG, "  ----- onPause() ----- " + " mIsDialpadShown is:  " + mIsDialpadShown);
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_SEARCH_QUERY, mSearchQuery);
        outState.putBoolean(KEY_IN_REGULAR_SEARCH_UI, mInRegularSearch);
        outState.putBoolean(KEY_IN_DIALPAD_SEARCH_UI, mInDialpadSearch);
        outState.putBoolean(KEY_FIRST_LAUNCH, mFirstLaunch);
        outState.putBoolean(KEY_IS_DIALPAD_SHOWN, mIsDialpadShown);
//        mActionBarController.saveInstanceState(outState);
        mStateSaved = true;
    }





    protected void handleMenuSettings() {
//        final Intent intent = new Intent(this, DialerSettingsActivity.class);
//        startActivity(intent);
    }

    @Override
    public void onClick(View view) {
        int i = view.getId();
        if (i == R.id.floating_action_button) {//                if (mListsFragment.getCurrentTabIndex()
//                        == ListsFragment.TAB_INDEX_ALL_CONTACTS && !mInRegularSearch) {
//                    DialerUtils.startActivityWithErrorToast(
//                            this,
//                            IntentUtil.getNewContactIntent(),
//                            R.string.add_contact_not_available);
//                } else if (!mIsDialpadShown) {
//                    mInCallDialpadUp = false;
//                    showDialpadFragment(true);
//                }
            Log.d(TAG, " onClic mIsDialpadShown "+ mIsDialpadShown);
            if (!mIsDialpadShown) {
                mInCallDialpadUp = false;
                showDialpadFragment(true);
            }


        } else if (i == R.id.voice_search_button) {
            try {
                startActivityForResult(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH),
                        ACTIVITY_REQUEST_CODE_VOICE_SEARCH);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(DialtactsFragment.this.getContext(), R.string.voice_search_not_available,
                        Toast.LENGTH_SHORT).show();
            }

        } else if (i == R.id.dialtacts_options_menu_button) {
            mOverflowMenu.show();

        } else if (i == R.id.dialtacts_bottom_menu_button_call) {
            if (!mIsDialpadShown) {
                mInCallDialpadUp = false;
                showDialpadFragment(true);
            } else {
                if (TextUtils.isEmpty(mSearchQuery) ||
                        (mSmartDialSearchFragment != null && mSmartDialSearchFragment.isVisible()
                                && mSmartDialSearchFragment.getAdapter().getCount() == 0)) {
                    exitSearchUi();
                }
                hideDialpadFragment(true, true);
            }

        } else if (i == R.id.actionbar_menu) {
            initCallLogSelectPopupWindow();
            Log.e(TAG, " onClick actionbar_menu");


        } else if (i == R.id.actionbar_name) {
            initCallLogSelectPopupWindow();
            Log.e(TAG, " ---- onClick actionbarName ----");

        } else if (i == R.id.actionbar_call_dialtacts_action_editer) {
            StringBuilder sb = new StringBuilder("已选");
            if (mDialtactsActionBarController.getEditerText().equals(getResources().getString(R.string.call_detail_editer))){
                showMultipleEditer();
                mDialtactsActionBarController.setEditerTxt(getString(R.string.call_delete_select_all));
            } else if (mDialtactsActionBarController.getEditerText().equals(getResources().getString(R.string.call_delete_select_all))){
                mCalllogList.allSelectLog(true);
                mDialtactsActionBarController.setEditerTxt(getString(R.string.call_delete_cansel_all));
                sb.append(mCalllogList.getSelectLogCount()).append("项通话记录");
                mDialtactsActionBarController.setActionName(new String(sb));
            } else if (mDialtactsActionBarController.getEditerText().equals(getResources().getString(R.string.call_delete_cansel_all))){
                mCalllogList.allSelectLog(false);
                mDialtactsActionBarController.setEditerTxt(getString(R.string.call_delete_select_all));
                mDialtactsActionBarController.setActionName(getString(R.string.select_call_log));
            }
            mFloatingActionButtonController.setVisible(false);
            mCalllogList.setmSelectCallLogImpl(mSeletectCallLogImpl);




        } else if (i == R.id.actionbar_call_dialtacts_action_cancel) {
            hideMultipleEditer();


        } else if (i == R.id.dialtacts_bottom_menu_button_delete) {
            if (mCalllogList != null)
                mCalllogList.deleteSelectedCallItems();


        } else {
            Log.wtf(TAG, "Unexpected onClick event from " + view);
        }
    }

    private void showMultipleEditer() {

        mDialtactsActionBarController.setActionName(getString(R.string.select_call_log));
        mDialtactsActionBarController.getmActionMenu().setVisibility(View.GONE);
        mDialtactsActionBarController.showCanselTxt(true);
        bottomMenuButtonSlideOut();
        bottomMenuButtonDeleteSlideIn();
        mCalllogList.showMultipleDelete(true);
        if (mIsDialpadShown){

            hideDialpadFragment(true,true);
        }


    }

    private void hideMultipleEditer(){
        if (mCalllogList.getmCallTypeFilter() == CallLogQueryHandler.CALL_TYPE_ALL){
            mDialtactsActionBarController.setActionName(getString(R.string.all_calls));
        }else {
            mDialtactsActionBarController.setActionName(getString(R.string.call_log_missed));
        }

        mDialtactsActionBarController.showActionMenu(true);
        mDialtactsActionBarController.showCanselTxt(false);
        mDialtactsActionBarController.setEditerTxt(getString(R.string.call_detail_editer));

        bottomMenuButtonSlideIn();
        bottomMenuButtonDeleteSlideOut();
        mCalllogList.showMultipleDelete(false);
        mCalllogList.allSelectLog(false);
        if (!mIsDialpadShown) {
            mInCallDialpadUp = false;
            showDialpadFragment(true);
        }
        mFloatingActionButtonController.setVisible(true);

    }



    private void bottomMenuButtonSlideOut(){
        mMenuButtonCall.setVisibility(View.GONE);
        mMenuButtonContacts.setVisibility(View.GONE);
        mMenuButtonSetting.setVisibility(View.GONE);
    }
    private void bottomMenuButtonSlideIn() {
        mMenuButtonCall.setVisibility(View.VISIBLE);
        mMenuButtonContacts.setVisibility(View.VISIBLE);
        mMenuButtonSetting.setVisibility(View.VISIBLE);
    }
    private void bottomMenuButtonDeleteSlideOut(){
        mMenuButtonDelete.setVisibility(View.GONE);
    }
    private void bottomMenuButtonDeleteSlideIn() {
        mMenuButtonDelete.setVisibility(View.VISIBLE);
    }





    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int i = item.getItemId();
        if (i == R.id.menu_history) {// Use explicit CallLogActivity intent instead of ACTION_VIEW +
            // CONTENT_TYPE, so that we always open our call log from our dialer
            final Intent intent = new Intent(this.getContext(), CallLogActivity.class);
            startActivity(intent);

        } else if (i == R.id.menu_add_contact) {
            DialerUtils.startActivityWithErrorToast(
                    this.getContext(),
                    IntentUtil.getNewContactIntent(),
                    R.string.add_contact_not_available);

        } else if (i == R.id.menu_import_export) {// We hard-code the "contactsAreAvailable" argument because doing it properly would
            // involve querying a {@link ProviderStatusLoader}, which we don't want to do right
            // now in Dialtacts for (potential) performance reasons. Compare with how it is
            // done in {@link PeopleActivity}.
            ImportExportDialogFragment.show(getChildFragmentManager(), true,
                    DialtactsFragment.class);
            return true;
        } else if (i == R.id.menu_clear_frequents) {
            ClearFrequentsDialog.show(getChildFragmentManager());
            return true;
        } else if (i == R.id.menu_call_settings) {
            handleMenuSettings();
            return true;
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ACTIVITY_REQUEST_CODE_VOICE_SEARCH) {
            if (resultCode == getActivity().RESULT_OK) {
                final ArrayList<String> matches = data.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS);
                if (matches.size() > 0) {
                    final String match = matches.get(0);
                    mVoiceSearchQuery = match;
                } else {
                    Log.e(TAG, "Voice search - nothing heard");
                }
            } else {
                Log.e(TAG, "Voice search failed");
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Initiates a fragment transaction to show the dialpad fragment. Animations and other visual
     * updates are handled by a callback which is invoked after the dialpad fragment is shown.
     * @see #onDialpadShown
     */
    public void showDialpadFragment(boolean animate) {

//        if (mIsDialpadShown || mStateSaved) {
//            return;
//        }
        if (mIsDialpadShown || mStateSaved ||mDialpadFragment == null) {
            Log.e("showDialpadFragment "," return; ");
            return;
        }
        mIsDialpadShown = true;
        animate = false;
        mDialpadFragment.setAnimate(animate);
//        mListsFragment.setUserVisibleHint(false);
        AnalyticsUtil.sendScreenView(mDialpadFragment);


        final FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        //if (mDialpadFragment == null) {
            //mDialpadFragment = new DialpadFragment();
          //  ft.add(R.id.dialtacts_container, mDialpadFragment, TAG_DIALPAD_FRAGMENT);
        //} else {
            ft.show(mDialpadFragment);
        //}

//        mDialpadFragment.setAnimate(animate);

//        ft.commit();

//        if (animate) {
////            mFloatingActionButtonController.scaleOut();
//        } else {
////            mFloatingActionButtonController.setVisible(false);
//            maybeEnterSearchUi();
//        }
        if (!isInSearchUi() && TextUtils.isEmpty(mSearchQuery)) {
            if(getCallLogFragment() == null){
                mCalllogList  = new CallLogFragment(CallLogQueryHandler.CALL_TYPE_ALL);
                mCalllogList.setRecyclerViewChangedImpl(mRecyclerViewChangedImpl);
                ft.add(R.id.dialtacts_frame, mCalllogList,"mCalllogList");
                ft.show(mCalllogList);
            }else {
                if(mSearchViewShow)
                    mDialpadFragment.showSearchView();
                else
                    mDialpadFragment.hideSearchView();
            }
        }
        ft.commitAllowingStateLoss();
        sendMessageToReflushEditerView();
        Log.e(TAG," showDialpadFragment mIsDialpadShown:"+mIsDialpadShown + "  mInDialpadSearch:" + mInDialpadSearch  + "  isDialpadShown() :" + isDialpadShown() );


//        mListsFragment.getView().animate().alpha(0).withLayer();
    }

    /**
     * Callback from child DialpadFragment when the dialpad is shown.
     */
    public void onDialpadShown() {
        Assert.assertNotNull(mDialpadFragment);
        if (mDialpadFragment.getAnimate()) {
            mDialpadFragment.getView().startAnimation(mSlideIn);
        } else {
            mDialpadFragment.setYFraction(0);
        }

        updateSearchFragmentPosition();
    }

    /**
     * Initiates animations and other visual updates to hide the dialpad. The fragment is hidden in
     * a callback after the hide animation ends.
     * @see #commitDialpadFragmentHide
     */
    public void hideDialpadFragment(boolean animate, boolean clearDialpad) {
        if (mDialpadFragment == null ) {
            return;
        }
        if (clearDialpad) {
            mDialpadFragment.clearDialpad();
        }
        if (!mIsDialpadShown) {
            return;
        }
        mIsDialpadShown = false;
        mDialpadFragment.setAnimate(animate);
//        mListsFragment.setUserVisibleHint(true);
//        mListsFragment.sendScreenViewForCurrentPosition();

        updateSearchFragmentPosition();


        mFloatingActionButtonController.align(getFabAlignment(), animate);
        if (animate) {
            mDialpadFragment.getView().startAnimation(mSlideOut);
        } else {
            commitDialpadFragmentHide();
        }

//        mActionBarController.onDialpadDown();


        if (isInSearchUi()) {
            if (TextUtils.isEmpty(mSearchQuery)) {
                exitSearchUi();
            }
        }

        Log.e(TAG, " hideDialpadFragment  mIsDialpadShown " + mIsDialpadShown);

    }

    /**
     * Finishes hiding the dialpad fragment after any animations are completed.
     */
    private void commitDialpadFragmentHide() {
        if (!mStateSaved && mDialpadFragment != null && !mDialpadFragment.isHidden()) {
            final FragmentTransaction ft = getChildFragmentManager().beginTransaction();
            ft.hide(mDialpadFragment);
            ft.commit();
        }
        mFloatingActionButtonController.scaleIn(AnimUtils.NO_DELAY);

    }

    private void updateSearchFragmentPosition() {
//        SearchFragment fragment = null;
//        if (mSmartDialSearchFragment != null && mSmartDialSearchFragment.isVisible()) {
//            fragment = mSmartDialSearchFragment;
//        } else if (mRegularSearchFragment != null && mRegularSearchFragment.isVisible()) {
//            fragment = mRegularSearchFragment;
//        }
//        if (fragment != null && fragment.isVisible()) {
////            fragment.updatePosition(true /* animate */);
//            fragment.updatePosition(false /* animate */);
//        }
    }

    @Override
    public boolean isInSearchUi() {
        return mInDialpadSearch || mInRegularSearch;
    }

    @Override
    public boolean hasSearchQuery() {
        return !TextUtils.isEmpty(mSearchQuery);
    }

    @Override
    public boolean shouldShowActionBar() {
//        return mListsFragment.shouldShowActionBar();
        return true;
    }

    private void setNotInSearchUi() {
        mInDialpadSearch = false;
        mInRegularSearch = false;
    }

    private void hideDialpadAndSearchUi() {
        if (mIsDialpadShown) {
            hideDialpadFragment(false, true);
        } else {
            exitSearchUi();
        }
    }

    private void prepareVoiceSearchButton() {
        final Intent voiceIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        boolean canBeHandled = canIntentBeHandled(voiceIntent);
//        if (canIntentBeHandled(voiceIntent)) {
//            mVoiceSearchButton.setVisibility(View.VISIBLE);
//            mVoiceSearchButton.setOnClickListener(this);
//        } else {
//            mVoiceSearchButton.setVisibility(View.GONE);
//        }
    }



    /**
     * Returns true if the intent is due to hitting the green send key (hardware call button:
     * KEYCODE_CALL) while in a call.
     *
     * @param intent the intent that launched this activity
     * @return true if the intent is due to hitting the green send key while in a call
     */
    private boolean isSendKeyWhileInCall(Intent intent) {
        // If there is a call in progress and the user launched the dialer by hitting the call
        // button, go straight to the in-call screen.
        final boolean callKey = Intent.ACTION_CALL_BUTTON.equals(intent.getAction());

        if (callKey) {
            getTelecomManager().showInCallScreen(false);
            return true;
        }

        return false;
    }

    /**
     * Sets the current tab based on the intent's request type
     *
     * @param intent Intent that contains information about which tab should be selected
     */
    public void displayFragment(Intent intent) {
        // If we got here by hitting send and we're in call forward along to the in-call activity
        if (isSendKeyWhileInCall(intent)) {
            getActivity().finish();
            return;
        }

//        final boolean phoneIsInUse = phoneIsInUse();
//        if (phoneIsInUse || (intent.getData() !=  null && isDialIntent(intent))) {
//            showDialpadFragment(false);
//            mDialpadFragment.setStartedFromNewIntent(true);
//            if (phoneIsInUse && !mDialpadFragment.isVisible()) {
//                mInCallDialpadUp = true;
//            }
        if (mDialpadFragment != null) {
            final boolean phoneIsInUse = phoneIsInUse();
            if (phoneIsInUse || (intent.getData() !=  null && isDialIntent(intent))) {

                mDialpadFragment.setStartedFromNewIntent(true);
                if (phoneIsInUse && !mDialpadFragment.isVisible()) {
                    mInCallDialpadUp = true;
                }
                showDialpadFragment(false);
            }
            Log.e(TAG, "        --- displayFragment(Intent intent) ---  mDialpadFragment != null ");
        }
        Log.e(TAG, " --- displayFragment(Intent intent) --- " + " mIsDialpadShown  is " + mIsDialpadShown);
    }

//    @Override
//    public void onNewIntent(Intent newIntent) {
//        getActivity().setIntent(newIntent);
//
//        mStateSaved = false;
//        displayFragment(newIntent);
//
//        getActivity().invalidateOptionsMenu();
//    }

    /** Returns true if the given intent contains a phone number to populate the dialer with */
    private boolean isDialIntent(Intent intent) {
        final String action = intent.getAction();
        if (Intent.ACTION_DIAL.equals(action) || ACTION_TOUCH_DIALER.equals(action)) {
            return true;
        }
        if (Intent.ACTION_VIEW.equals(action)) {
            final Uri data = intent.getData();
            if (data != null && PhoneAccount.SCHEME_TEL.equals(data.getScheme())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns an appropriate call origin for this Activity. May return null when no call origin
     * should be used (e.g. when some 3rd party application launched the screen. Call origin is
     * for remembering the tab in which the user made a phone call, so the external app's DIAL
     * request should not be counted.)
     */
    public String getCallOrigin() {
        return !isDialIntent(getActivity().getIntent()) ? CALL_ORIGIN_DIALTACTS : null;
    }

    /**
     * Shows the search fragment
     */
    private void enterSearchUi(boolean smartDialSearch, String query, boolean animate) {
        if (mStateSaved || getChildFragmentManager().isDestroyed()) {
            // Weird race condition where fragment is doing work after the activity is destroyed
            // due to talkback being on (b/10209937). Just return since we can't do any
            // constructive here.
            return;
        }
        mDialpadFragment.showSearchView();

        if (DEBUG) {
            Log.d(TAG, "Entering search UI - smart dial " + smartDialSearch);
        }

        final FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        if (mInDialpadSearch && mSmartDialSearchFragment != null) {
            transaction.remove(mSmartDialSearchFragment);
        } else if (mInRegularSearch && mRegularSearchFragment != null) {
            transaction.remove(mRegularSearchFragment);
        }

        final String tag;
        if (smartDialSearch) {
            tag = TAG_SMARTDIAL_SEARCH_FRAGMENT;
        } else {
            tag = TAG_REGULAR_SEARCH_FRAGMENT;
        }
        mInDialpadSearch = smartDialSearch;
        mInRegularSearch = !smartDialSearch;

        mFloatingActionButtonController.scaleOut();

        SearchFragment fragment = (SearchFragment) getChildFragmentManager().findFragmentByTag(tag);
        if (animate) {
            transaction.setCustomAnimations(android.R.animator.fade_in, 0);
        } else {
            transaction.setTransition(FragmentTransaction.TRANSIT_NONE);
        }
        if (fragment == null) {

            Log.e(TAG, " --- enterSearchUi --- null" );
            if (smartDialSearch) {
                fragment = new SmartDialSearchFragment();
                transaction.add(R.id.dialtacts_frame, fragment, tag);
            } else {
                fragment = new RegularSearchFragment();
                fragment.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        // Show the FAB when the user touches the lists fragment and the soft
                        // keyboard is hidden.
                        showFabInSearchUi();
                        return false;
                    }
                });
            }

        } else {
            transaction.show(fragment);

        }
        // DialtactsFragment will provide the options menu
        fragment.setHasOptionsMenu(false);
        fragment.setShowEmptyListForNullQuery(true);
        fragment.setQueryString(query, false /* delaySelection */);
        transaction.commitAllowingStateLoss();
//        transaction.commit();

//        if (animate) {
//            mListsFragment.getView().animate().alpha(0).withLayer();
//        }
//        mListsFragment.setUserVisibleHint(false);
    }

    /**
     * Hides the search fragment
     */
    private void exitSartDialpadFragment() {
        // See related bug in enterSearchUI();
        if (getChildFragmentManager().isDestroyed() || mStateSaved) {
            return;
        }
        setNotInSearchUi();
        final FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        if (mSmartDialSearchFragment != null) {
            transaction.remove(mSmartDialSearchFragment);
        }
        if (mRegularSearchFragment != null) {
            transaction.remove(mRegularSearchFragment);
        }
        transaction.commit();

        addCallLogFragmentInList();
    }



    /**
     * Hides the search fragment
     */
    public void exitSearchUi() {
        // See related bug in enterSearchUI();
        if (getChildFragmentManager().isDestroyed() || mStateSaved) {
            return;
        }

//        mSearchView.setText(null);

//        if (mDialpadFragment != null) {
            mDialpadFragment.clearDialpad();
//        }

        setNotInSearchUi();

        // Restore the FAB for the lists fragment.
//        if (getFabAlignment() != FloatingActionButtonController.ALIGN_END) {
//            mFloatingActionButtonController.setVisible(false);
//        }
        mFloatingActionButtonController.scaleIn(FAB_SCALE_IN_DELAY_MS);
//        onPageScrolled(mListsFragment.getCurrentTabIndex(), 0 /* offset */, 0 /* pixelOffset */);
//        onPageSelected(mListsFragment.getCurrentTabIndex());

        final FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        if (mSmartDialSearchFragment != null) {
            transaction.remove(mSmartDialSearchFragment);
        }
        if (mRegularSearchFragment != null) {
            transaction.remove(mRegularSearchFragment);
        }
        transaction.commit();

//        mListsFragment.getView().animate().alpha(1).withLayer();

        if (mDialpadFragment == null || !mDialpadFragment.isVisible()) {
            // If the dialpad fragment wasn't previously visible, then send a screen view because
            // we are exiting regular search. Otherwise, the screen view will be sent by
            // {@link #hideDialpadFragment}.
//            mListsFragment.sendScreenViewForCurrentPosition();
//            mListsFragment.setUserVisibleHint(true);
        }
        addCallLogFragmentInList();
//        mActionBarController.onSearchUiExited();
//        getActionBar().show();
    }


//    @Override
//    public void onBackPressed() {
//        if (mStateSaved) {
//            return;
//        }
//        if (mIsDialpadShown) {
//            if(!isInSearchUi()) {
//                getActivity().finish();
//                return;
//            }
//            if(mSmartDialSearchFragment!=null && mSmartDialSearchFragment.popupWindowIsShowing()){
//                mSmartDialSearchFragment.popupWindowDismiss();
//            }else {
//                if (TextUtils.isEmpty(mSearchQuery) ||
//                        (mSmartDialSearchFragment != null && mSmartDialSearchFragment.isVisible()
//                                && mSmartDialSearchFragment.getAdapter().getCount() == 0)) {
//                    exitSearchUi();
//                }
//                hideDialpadFragment(true, true);
//            }
////            hideDialpadFragment(true, true);
////        } else if (isInSearchUi()) {
////            exitSearchUi();
////            DialerUtils.hideInputMethod(mParentLayout);
//        } else {
//            getActivity().onBackPressed();
//        }
//    }

    /**
     *添加新的通话记录
     **/
    private void addCallLogFragmentInList(){
        final FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        if(getCallLogFragment() == null){
            mCalllogList = null;
            mCalllogList  = new CallLogFragment(CallLogQueryHandler.CALL_TYPE_ALL);
            transaction.add(R.id.dialtacts_frame, mCalllogList,"mCalllogList");
        }else {
            mCalllogList = (CallLogFragment)getCallLogFragment();
        }
        mCalllogList.setRecyclerViewChangedImpl(mRecyclerViewChangedImpl);
        transaction.show(mCalllogList);
        transaction.commitAllowingStateLoss();
    }



    private void maybeEnterSearchUi() {
        if (!isInSearchUi()) {
            enterSearchUi(true /* isSmartDial */, mSearchQuery, false);
//            enterSearchUi(false,mSearchQuery,false);
        }

    }

    /**
     * @return True if the search UI was exited, false otherwise
     */
    private boolean maybeExitSearchUi() {
        if (isInSearchUi() && TextUtils.isEmpty(mSearchQuery)) {
            exitSearchUi();
            DialerUtils.hideInputMethod(mParentLayout);
            return true;
        }
        return false;
    }

    private void hideSearchFragment(){
        mSearchViewShow = false;
        getActivity().getActionBar().show();
        mDialpadFragment.hideSearchView();
        //maybeExitSearchUi();
        exitSartDialpadFragment();
    }
    private void showSearchFragment(){
        final FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        getActivity().getActionBar().hide();
        mSearchViewShow = true;
        mDialpadFragment.showSearchView();
        if (mCalllogList != null) {
            //Log.e(TAG,"SHOWSearchFragment");
            transaction.hide(mCalllogList);
        }
        if(getCallLogFragment() != null) {
            transaction.hide(getCallLogFragment());
        }
        transaction.commitAllowingStateLoss();
    }



    private void showFabInSearchUi() {
        mFloatingActionButtonController.changeIcon(
                getResources().getDrawable(R.drawable.fab_ic_dial),
                getResources().getString(R.string.action_menu_dialpad_button));
        mFloatingActionButtonController.align(getFabAlignment(), false /* animate */);
        mFloatingActionButtonController.scaleIn(FAB_SCALE_IN_DELAY_MS);
    }

    @Override
    public void onDialpadQueryChanged(String query) {
        //Log.d(TAG, "---query---:" + query);
        if(query.length() == 8){
            if(query.equals("*#5858#*"))
                getActivity().sendBroadcast(new Intent("com.android.dialer.DialtactsFragment.recovery.hgc"));
        }
        if(query.length() == 9){
            if(query.equals("*#58058#*")) {
        	   /*start self test app*/
                Intent selftestIntent = new Intent();
                selftestIntent.setClassName("com.eebbk.selftest","com.eebbk.selftest.ui.STHomeActivity");
                selftestIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(selftestIntent);
            }
        }
        if(query == null || query.length() == 0){
//            mSearchQuery = query;
            hideSearchFragment();
        }
        else{
            if (!isInSearchUi()) {
//                mSearchQuery = query;
                enterSearchUi(false /* isSmartDial */, query, true);
            }

            showSearchFragment();
        }
        if (mSmartDialSearchFragment != null) {
            mSmartDialSearchFragment.setAddToContactNumber(query);
        }
        final String normalizedQuery = SmartDialNameMatcher.normalizeNumber(query,
                SmartDialNameMatcher.LATIN_SMART_DIAL_MAP);

        if (!TextUtils.equals(mSearchView.getText(), normalizedQuery)) {
            if (DEBUG) {
                Log.d(TAG, "onDialpadQueryChanged - new query: " + query);
            }
            if (mDialpadFragment == null || !mDialpadFragment.isVisible()) {
                // This callback can happen if the dialpad fragment is recreated because of
                // activity destruction. In that case, don't update the search view because
                // that would bring the user back to the search fragment regardless of the
                // previous state of the application. Instead, just return here and let the
                // fragment manager correctly figure out whatever fragment was last displayed.
                if (!TextUtils.isEmpty(normalizedQuery)) {
                    mPendingSearchViewQuery = normalizedQuery;
                }
                return;
            }
            mSearchView.setText(normalizedQuery);
        }
        try {
            if (mDialpadFragment != null && mDialpadFragment.isVisible()) {
                mDialpadFragment.process_quote_emergency_unquote(normalizedQuery);
            }
        } catch (Exception ignored) {
            // Skip any exceptions for this piece of code
        }
    }

    @Override
    public boolean onDialpadSpacerTouchWithEmptyQuery() {
        if (mInDialpadSearch && mSmartDialSearchFragment != null
                && !mSmartDialSearchFragment.isShowingPermissionRequest()) {
            hideDialpadFragment(true /* animate */, true /* clearDialpad */);
            return true;
        }
        return false;
    }

    @Override
    public void onListFragmentScrollStateChange(int scrollState) {
        if (scrollState == OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
            Log.e(TAG,"onListFragmentScrollStateChange");
            FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
            transaction.hide( mCalllogList);
            transaction.commitAllowingStateLoss();
            hideDialpadFragment(true, false);
            DialerUtils.hideInputMethod(mParentLayout);
        }
    }

    @Override
    public void onListFragmentScroll(int firstVisibleItem, int visibleItemCount,
                                     int totalItemCount) {
        // TODO: No-op for now. This should eventually show/hide the actionBar based on
        // interactions with the ListsFragments.on
    }

    private boolean phoneIsInUse() {
        return getTelecomManager().isInCall();
    }

    private boolean canIntentBeHandled(Intent intent) {
        final PackageManager packageManager = getActivity().getPackageManager();
        final List<ResolveInfo> resolveInfo = packageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo != null && resolveInfo.size() > 0;
    }

    /**
     * Called when the user has long-pressed a contact tile to start a drag operation.
     */
    @Override
    public void onDragStarted(int x, int y, PhoneFavoriteSquareTileView view) {
//        mListsFragment.showRemoveView(true);
    }

    @Override
    public void onDragHovered(int x, int y, PhoneFavoriteSquareTileView view) {
    }

    /**
     * Called when the user has released a contact tile after long-pressing it.
     */
    @Override
    public void onDragFinished(int x, int y) {
//        mListsFragment.showRemoveView(false);
    }

    @Override
    public void onDroppedOnRemove() {}

    /**
     * Allows the SpeedDialFragment to attach the drag controller to mRemoveViewContainer
     * once it has been attached to the activity.
     */
    @Override
    public void setDragDropController(DragDropController dragController) {
        mDragDropController = dragController;
//        mListsFragment.getRemoveView().setDragDropController(dragController);
    }

    /**
     * Implemented to satisfy {@link SpeedDialFragment.HostInterface}
     */
    @Override
    public void showAllContactsTab() {
        if (mListsFragment != null) {
            mListsFragment.showTab(ListsFragment.TAB_INDEX_ALL_CONTACTS);
        }
    }

    /**
     * Implemented to satisfy {@link CallLogFragment.HostInterface}
     */
    @Override
    public void showDialpad() {
        showDialpadFragment(true);
    }

    @Override
    public void onPickPhoneNumberAction(Uri dataUri) {
        // Specify call-origin so that users will see the previous tab instead of
        // CallLog screen (search UI will be automatically exited).
        if (getActivity() instanceof TransactionSafeActivity){
            PhoneNumberInteraction.startInteractionForPhoneCall(
                    (TransactionSafeActivity)getActivity(), dataUri, getCallOrigin());
        }

        mClearSearchOnPause = true;
    }

    @Override
    public void onCallNumberDirectly(String phoneNumber) {
        onCallNumberDirectly(phoneNumber, false /* isVideoCall */);
    }

    @Override
    public void onCallNumberDirectly(String phoneNumber, boolean isVideoCall) {
        if (phoneNumber == null) {
            // Invalid phone number, but let the call go through so that InCallUI can show
            // an error message.
            phoneNumber = "";
        }
        Intent intent = isVideoCall ?
                IntentUtil.getVideoCallIntent(phoneNumber, getCallOrigin()) :
                IntentUtil.getCallIntent(phoneNumber, getCallOrigin());
        DialerUtils.startActivityWithErrorToast(this.getActivity(), intent);
        mClearSearchOnPause = true;
    }

    @Override
    public void onShortcutIntentCreated(Intent intent) {
        Log.w(TAG, "Unsupported intent has come (" + intent + "). Ignoring.");
    }

    @Override
    public void onHomeInActionBarSelected() {
        exitSearchUi();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
//        int tabIndex = mListsFragment.getCurrentTabIndex();

        // Scroll the button from center to end when moving from the Speed Dial to Recents tab.
        // In RTL, scroll when the current tab is Recents instead of Speed Dial, because the order
        // of the tabs is reversed and the ViewPager returns the left tab position during scroll.
//        boolean isRtl = DialerUtils.isRtl();
//        if (!isRtl && tabIndex == ListsFragment.TAB_INDEX_SPEED_DIAL && !mIsLandscape) {
//            mFloatingActionButtonController.onPageScrolled(positionOffset);
//        } else if (isRtl && tabIndex == ListsFragment.TAB_INDEX_RECENTS && !mIsLandscape) {
//            mFloatingActionButtonController.onPageScrolled(1 - positionOffset);
//        } else if (tabIndex != ListsFragment.TAB_INDEX_SPEED_DIAL) {
//            mFloatingActionButtonController.onPageScrolled(1);
//        }
    }

    @Override
    public void onPageSelected(int position) {
//        int tabIndex = mListsFragment.getCurrentTabIndex();
//        if (tabIndex == ListsFragment.TAB_INDEX_ALL_CONTACTS) {
//            mFloatingActionButtonController.changeIcon(
//                    getResources().getDrawable(R.drawable.ic_person_add_24dp),
//                    getResources().getString(R.string.search_shortcut_create_new_contact));
//        } else {
//            mFloatingActionButtonController.changeIcon(
//                    getResources().getDrawable(R.drawable.fab_ic_dial),
//                    getResources().getString(R.string.action_menu_dialpad_button));
//        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    private TelecomManager getTelecomManager() {
        return (TelecomManager) getActivity().getSystemService(Context.TELECOM_SERVICE);
    }

    @Override
    public boolean isActionBarShowing() {
//        return mActionBarController.isActionBarShowing();
        return getActivity().getActionBar().isShowing();
    }

    @Override
    public ActionBarController getActionBarController() {
//        return mActionBarController;
        return null;
    }

    @Override
    public boolean isDialpadShown() {
        return mIsDialpadShown;
    }

    @Override
    public int getDialpadHeight() {
        if (mDialpadFragment != null) {
            return mDialpadFragment.getDialpadHeight();
        }
        return 0;
    }

    @Override
    public int getActionBarHideOffset() {
        return getActivity().getActionBar().getHideOffset();
    }

    @Override
    public void setActionBarHideOffset(int offset) {
        getActivity().getActionBar().setHideOffset(offset);
    }

    @Override
    public int getActionBarHeight() {
        return mActionBarHeight;
    }

    private int getFabAlignment() {
//        if (!mIsLandscape && !isInSearchUi() &&
//                mListsFragment.getCurrentTabIndex() == ListsFragment.TAB_INDEX_SPEED_DIAL) {
//            return FloatingActionButtonController.ALIGN_MIDDLE;
//        }
//        return FloatingActionButtonController.ALIGN_END;
        return FloatingActionButtonController.ALIGN_MIDDLE;
    }


    private Fragment getCallLogFragment(){

        return getChildFragmentManager().findFragmentByTag("mCalllogList");

    }

    //bbk wangchunhe 2016/07/13

    /**
     * @author bbk wangchunhe
     * @Date 2016/07/13
     * initialize CallLogSelectPopupWindow
     */
    private void initCallLogSelectPopupWindow() {
        if (mCallLogSelectPopupWindow == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(this.getContext());
            View contentView  = layoutInflater.inflate(R.layout.dialtacts_call_log_select_popupwindow,mParentLayout,false);
            mCallLogSelectPopupWindow = new PopupWindow(contentView, ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);
            contentView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCallLogSelectPopupWindow.dismiss();
                }
            });

        }

        mCallLogSelectPopupWindow.setTouchable(true);
        mCallLogSelectPopupWindow.setFocusable(true);
        mCallLogSelectPopupWindow.setOutsideTouchable(true);
        mCallLogSelectPopupWindow.setAnimationStyle(R.style.PopupWindowAinm);
        mCallLogSelectPopupWindow.setBackgroundDrawable(new BitmapDrawable());
        mCallLogSelectPopupWindow.showAsDropDown(getActivity().getActionBar().getCustomView());
//        mActionbarMenu.setImageResource(R.drawable.actionbar_menu_down_up);
        mDialtactsActionBarController.setActionMenuIcon(R.drawable.actionbar_menu_down_up);
        popupWindowItemSelect(mCallLogSelectPopupWindow);
        mCallLogSelectPopupWindow.update();
        mCallLogSelectPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
//                mActionbarMenu.setImageResource(R.drawable.actionbar_menu_down);

                mDialtactsActionBarController.setActionMenuIcon(R.drawable.actionbar_menu_down);
            }
        });




    }

    /**
     * @author bbk wangchunhe
     * @Date 2016/07/15
     * show CallLogFragment of filterType
     * @param filterType
     */
    private void showCallLogFragment(int filterType){

        final FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        if (mCalllogList != null){
            transaction.remove(mCalllogList);
        }

        mCalllogList  = new CallLogFragment(filterType);
        transaction.add(R.id.dialtacts_frame, mCalllogList,"mCalllogList");
        mCalllogList.setRecyclerViewChangedImpl(mRecyclerViewChangedImpl);
        transaction.show(mCalllogList);
        transaction.commitAllowingStateLoss();

    }

    /**
     * @author bbk wangchunhe
     * @Date 2016/07/15
     * add PopupWindow OnClickListener
     * @param popupWindow
     */
    private void popupWindowItemSelect(final PopupWindow popupWindow) {
        View contentView = popupWindow.getContentView();
        RelativeLayout callLogsContainer = (RelativeLayout)contentView.findViewById(R.id.dialtacts_popupwindow_all_calls_cantainer);
        final TextView  callLogsTxt = (TextView) contentView.findViewById(R.id.dialtacts_popupwindow_all_calls_txt);
        final ImageView callLogsImage = (ImageView) contentView.findViewById(R.id.dialtacts_popupwindow_all_calls_image);

        RelativeLayout callLogMissedContainer = (RelativeLayout)contentView.findViewById(R.id.dialtacts_popupwindow_call_log_missed_container);
        final TextView  callLogMissedTxt = (TextView) contentView.findViewById(R.id.dialtacts_popupwindow_call_log_missed_txt);
        final ImageView callLogMissedImage = (ImageView) contentView.findViewById(R.id.dialtacts_popupwindow_call_log_missed_image);


        ColorStateList colorStateListTxt = getActivity().getColorStateList(R.color.popupwindows_item_text_color);
        ColorStateList colorStateListImage = getActivity().getColorStateList(R.color.popupwindows_item_image_color);

        callLogsImage.setImageTintList(colorStateListImage);
        callLogMissedImage.setImageTintList(colorStateListImage);

        callLogsTxt.setTextColor(colorStateListTxt);
        callLogMissedTxt.setTextColor(colorStateListTxt);


            if (!callLogMissedImage.isSelected()){
                callLogsImage.setSelected(true);
                callLogsTxt.setSelected(true);
            }





        callLogsContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callLogsImage.setSelected(true);
                callLogsTxt.setSelected(true);
                callLogMissedImage.setSelected(false);
                callLogMissedTxt.setSelected(false);
                showCallLogFragment(CallLogQueryHandler.CALL_TYPE_ALL);
//                mActionbarNameTxt.setText(getString(R.string.all_calls));
//                mDialtactsActionBarController.setActionNameAndMenu(R.string.all_calls,R.string.call_log_missed,R.string.select_call_log);
                Log.e(TAG," action Name  --1" + mDialtactsActionBarController.getmActionNameTxt().getText());
                mDialtactsActionBarController.setActionName(getString(R.string.all_calls));
                Log.e(TAG," action Name  --2" + mDialtactsActionBarController.getmActionNameTxt().getText());
                mCallLogSelectPopupWindow.dismiss();


            }
        });

        callLogMissedContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                callLogsImage.setSelected(false);
                callLogsTxt.setSelected(false);
                callLogMissedImage.setSelected(true);
                callLogMissedTxt.setSelected(true);
                showCallLogFragment(Calls.MISSED_TYPE);
//                mActionbarNameTxt.setText(getString(R.string.call_log_missed));
//                mDialtactsActionBarController.setActionNameAndMenu(R.string.all_calls,R.string.call_log_missed,R.string.select_call_log);
                Log.e(TAG," action Name  --3" + mDialtactsActionBarController.getmActionNameTxt().getText());
                mDialtactsActionBarController.setActionName(getString(R.string.call_log_missed));
                Log.e(TAG," action Name  --4" + mDialtactsActionBarController.getmActionNameTxt().getText());
                mCallLogSelectPopupWindow.dismiss();

            }
        });


    }




    /**
     * @Date 2016/07/13
     * change actionbarMenu icon
     */
    private void actionbarMenuOpen() {
        if(mCallLogSelectPopupWindow != null && mCallLogSelectPopupWindow.isShowing()){
//            mActionbarMenu.setImageResource(R.drawable.actionbar_menu_down_up);
            mDialtactsActionBarController.setActionMenuIcon(R.drawable.actionbar_menu_down_up);

        }
    }

    ////////////BBK liupengfei add 2015/12/22///////////////////
    /**
    the method of com.android.dialer.bbk.RecyclerViewChangedImpl
    */

    private RecyclerViewChangedImpl mRecyclerViewChangedImpl = new RecyclerViewChangedImpl() {

        @Override
        public void onScrollStateChanged(int newState) {
            // TODO Auto-generated method stub
            Log.e(TAG,"onScrollStateChanged newState = "+newState+",mIsDialpadShown = "+mIsDialpadShown);
            if (mIsDialpadShown) {
                if (TextUtils.isEmpty(mSearchQuery) ||
                        (mSmartDialSearchFragment != null && mSmartDialSearchFragment.isVisible()
                                && mSmartDialSearchFragment.getAdapter().getCount() == 0)) {
                    exitSearchUi();
                }
                hideDialpadFragment(true, true);
                Log.e(TAG," mIsDialpadShown is" + true);
            } else if (isInSearchUi()) {
                //exitSearchUi();
                //DialerUtils.hideInputMethod(mParentLayout);
            }
        }
    };

    private SelectedCallLogImpl mSeletectCallLogImpl = new SelectedCallLogImpl() {
        @Override
        public void selectCallLogToDelete(int count) {
            StringBuilder sb = new StringBuilder("已选").append(count).append("项通话记录");

            if (count>0){
                mMenuButtonDelete.setEnabled(true);
//                mActionbarNameTxt.setText(sb);
                mDialtactsActionBarController.setActionName(new String(sb));
            }else {
                mMenuButtonDelete.setEnabled(false);

            }

        }
    };


}






