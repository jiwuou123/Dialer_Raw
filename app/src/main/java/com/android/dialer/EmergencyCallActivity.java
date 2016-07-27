package com.android.dialer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.contacts.common.CallUtil;
import com.android.contacts.common.activity.TransactionSafeActivity;
import com.android.dialer.dialpad.UnicodeDialerKeyListener;
import com.android.dialer.util.DialerUtils;
import com.android.phone.common.HapticFeedback;
import com.android.phone.common.dialpad.DialpadKeyButton;
import com.android.phone.common.dialpad.DialpadView;

import java.util.HashSet;

/**
 * Created by Administrator on 2016/7/11.
 */
public class EmergencyCallActivity extends TransactionSafeActivity implements View.OnClickListener, DialpadKeyButton.OnPressedListener, TextWatcher, View.OnLongClickListener {


    private DialpadView mDialpadView;
    private EditText mDigits;
    private ToneGenerator mToneGenerator;
    private View mDelete;


    /**
     * Set of dialpad keys that are currently being pressed
     */
    private final HashSet<View> mPressedDialpadKeys = new HashSet<View>(12);

    // Vibration (haptic feedback) for dialer key presses.
    private final HapticFeedback mHaptic = new HapticFeedback();

    // determines if we want to playback local DTMF tones.
    private boolean mDTMFToneEnabled;
    private final Object mToneGeneratorLock = new Object();

    /** Remembers if we need to clear digits field when the screen is completely gone. */
    private boolean mClearDigitsOnStop;


    /** The DTMF tone volume relative to other sounds in the stream */
    private static final int TONE_RELATIVE_VOLUME = 80;

    /** Stream type used to play the DTMF tones off call, and mapped to the volume control keys */
    private static final int DIAL_TONE_STREAM_TYPE = AudioManager.STREAM_DTMF;

    /** The length of DTMF tones in milliseconds */
    private static final int TONE_LENGTH_MS = 150;
    private static final int TONE_LENGTH_INFINITE = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.flags |= WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;

        // When no proximity sensor is available, use a shorter timeout.
        // TODO: Do we enable this for non proximity devices any more?
        // lp.userActivityTimeout = USER_ACTIVITY_TIMEOUT_WHEN_NO_PROX_SENSOR;

        getWindow().setAttributes(lp);

        setContentView(R.layout.emergency_call_surface);

        initViews();
    }
    private static View getRootView(Activity context)
    {
        return ((ViewGroup)context.findViewById(android.R.id.content)).getChildAt(0);
    }
    void initViews() {

        //初始化震动
        try {
            mHaptic.init(this,
                    getResources().getBoolean(R.bool.config_enable_dialer_key_vibration));
        } catch (Resources.NotFoundException nfe) {
            Log.e("f", "Vibrate control bool missing.", nfe);
        }

        mDialpadView = (DialpadView) findViewById(R.id.dialpad_view);
        mDialpadView.setCanDigitsBeEdited(true);

        mDigits = mDialpadView.getDigits();
        mDigits.setKeyListener(UnicodeDialerKeyListener.INSTANCE);
        mDigits.setOnClickListener(this);
        mDigits.addTextChangedListener(this);
        mDigits.setElegantTextHeight(false);

        mDigits.setCursorVisible(false);

        mDelete = mDialpadView.getDeleteButton();

        if (mDelete != null) {
            mDelete.setOnClickListener(this);
            mDelete.setOnLongClickListener(this);
        }

        //不可见
        findViewById(R.id.dialpad_overflow).setVisibility(View.INVISIBLE);

        //取消按钮
        TextView mCancelPic = (TextView)findViewById(R.id.cancel_pic);
        mCancelPic.setOnClickListener(this);


        //拨号按钮
        ImageView dial = (ImageView)findViewById(R.id.dial_pic);
        dial.setOnClickListener(this);


        //每个按键相应点击
        configureKeypadListeners(getRootView(this));
    }
    public void clearDialpad() {
        if (mDigits != null) {
            mDigits.getText().clear();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        synchronized (mToneGeneratorLock) {
            if (mToneGenerator != null) {
                mToneGenerator.release();
                mToneGenerator = null;
            }
        }

        if (mClearDigitsOnStop) {
            mClearDigitsOnStop = false;
            clearDialpad();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Make sure we don't leave this activity with a tone still playing.
        stopTone();
        mPressedDialpadKeys.clear();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // if the mToneGenerator creation fails, just continue without it.  It is
        // a local audio signal, and is not as important as the dtmf tone itself.
        final long start = System.currentTimeMillis();
        synchronized (mToneGeneratorLock) {
            if (mToneGenerator == null) {
                try {
                    mToneGenerator = new ToneGenerator(DIAL_TONE_STREAM_TYPE, TONE_RELATIVE_VOLUME);
                } catch (RuntimeException e) {
                    Log.w("f", "Exception caught while creating local tone generator: " + e);
                    mToneGenerator = null;
                }
            }
        }
        final long total = System.currentTimeMillis() - start;
        if (total > 50) {
            Log.i("f", "Time for ToneGenerator creation: " + total);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateDeleteButtonEnabledState();

        // Retrieve the haptic feedback setting.
        mHaptic.checkSystemSetting();

        final ContentResolver contentResolver = this.getContentResolver();
        // retrieve the DTMF tone play back setting.
        mDTMFToneEnabled = Settings.System.getInt(contentResolver,
                Settings.System.DTMF_TONE_WHEN_DIALING, 1) == 1;

        mPressedDialpadKeys.clear();
    }
    /**
     * @return true if the widget with the phone number digits is empty.
     */
    private boolean isDigitsEmpty() {
        return mDigits.length() == 0;
    }

    public static class ErrorDialogFragment extends DialogFragment {
        private int mTitleResId;
        private int mMessageResId;

        private static String titleStr = null;
        private static String msgStr = null;

        private static final String ARG_TITLE_RES_ID = "argTitleResId";
        private static final String ARG_MESSAGE_RES_ID = "argMessageResId";

        public static ErrorDialogFragment newInstance(int messageResId) {
            return newInstance(0, messageResId);
        }

        public static ErrorDialogFragment newInstance(String title, String msg) {
            final ErrorDialogFragment fragment = new ErrorDialogFragment();
            ErrorDialogFragment.titleStr = title;
            ErrorDialogFragment.msgStr = msg;
            final Bundle args = new Bundle();
            args.putInt(ARG_TITLE_RES_ID, 0);
            args.putInt(ARG_MESSAGE_RES_ID, 0);
            fragment.setArguments(args);
            return fragment;
        }

        public static ErrorDialogFragment newInstance(int titleResId, int messageResId) {
            final ErrorDialogFragment fragment = new ErrorDialogFragment();
            final Bundle args = new Bundle();
            args.putInt(ARG_TITLE_RES_ID, titleResId);
            args.putInt(ARG_MESSAGE_RES_ID, messageResId);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mTitleResId = getArguments().getInt(ARG_TITLE_RES_ID);
            mMessageResId = getArguments().getInt(ARG_MESSAGE_RES_ID);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            if (null != ErrorDialogFragment.titleStr){
                builder.setTitle(ErrorDialogFragment.titleStr);
                ErrorDialogFragment.titleStr = null;
            }else{
                if (mTitleResId != 0) {
                    builder.setTitle(mTitleResId);
                }
            }

            if (null != ErrorDialogFragment.msgStr){
                builder.setMessage(ErrorDialogFragment.msgStr);
                ErrorDialogFragment.msgStr = null;
            }else{
                if (mMessageResId != 0) {
                    builder.setMessage(mMessageResId);
                }
            }

            builder.setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dismiss();
                        }
                    });
            return builder.create();
        }
    }

    void handleDialButtonPressed(){
        Log.i("f","handleDialButtonPressed");

        final String number = mDigits.getText().toString();
        if ( number != null) {
            if (TextUtils.isEmpty(number)) {
//                输入为空的处理
                DialogFragment dialogFragment = ErrorDialogFragment.newInstance(
                        R.string.emergency_call_title, R.string.emergency_input_empty);
                dialogFragment.show(getFragmentManager(),
                        "1");
            } else {
//                检测是否为紧急呼叫号码
                boolean isEmergencyCall = PhoneNumberUtils.isEmergencyNumber(number);
                if (!isEmergencyCall) {
//                    不是紧急呼叫号码
                    String start = getResources().getString(R.string.emergency_input_wrong_start);
                    start += number;
                    start += getResources().getString(R.string.emergency_input_wrong_end);

                    DialogFragment dialogFragment = ErrorDialogFragment.newInstance(
                            getResources().getString(R.string.emergency_call_title), start);
                    dialogFragment.show(getFragmentManager(),
                            "2");
                }else{
//                    拨打紧急呼叫号码
                    final Intent intent = new Intent(Intent.ACTION_CALL,
                            CallUtil.getCallUri(number));
                    intent.setClass(this, EmergencyCallActivity.class);
                    DialerUtils.startActivityWithErrorToast(this, intent);
                }
            }
        }

    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.dial_pic) {//                拨打
            mHaptic.vibrate();
            handleDialButtonPressed();


        } else if (i == R.id.deleteButton) {
            keyPressed(KeyEvent.KEYCODE_DEL);
        } else if (i == R.id.digits) {
            if (!isDigitsEmpty()) {
                mDigits.setCursorVisible(true);
            }
        } else if (i == R.id.cancel_pic) {
            finish();

        } else {
            Log.wtf("f", "Unexpected onClick() event from: " + v);
            return;
        }
    }
    /**
     * Play the specified tone for the specified milliseconds
     *
     * The tone is played locally, using the audio stream for phone calls.
     * Tones are played only if the "Audible touch tones" user preference
     * is checked, and are NOT played if the device is in silent mode.
     *
     * The tone length can be -1, meaning "keep playing the tone." If the caller does so, it should
     * call stopTone() afterward.
     *
     * @param tone a tone code from {@link ToneGenerator}
     * @param durationMs tone length.
     */
    private void playTone(int tone, int durationMs) {
        // if local tone playback is disabled, just return.
        if (!mDTMFToneEnabled) {
            return;
        }

        // Also do nothing if the phone is in silent mode.
        // We need to re-check the ringer mode for *every* playTone()
        // call, rather than keeping a local flag that's updated in
        // onResume(), since it's possible to toggle silent mode without
        // leaving the current activity (via the ENDCALL-longpress menu.)
        AudioManager audioManager =
                (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int ringerMode = audioManager.getRingerMode();
        if ((ringerMode == AudioManager.RINGER_MODE_SILENT)
                || (ringerMode == AudioManager.RINGER_MODE_VIBRATE)) {
            return;
        }

        synchronized (mToneGeneratorLock) {
            if (mToneGenerator == null) {
                Log.w("f", "playTone: mToneGenerator == null, tone: " + tone);
                return;
            }

            // Start the new tone (will stop any playing tone)
            mToneGenerator.startTone(tone, durationMs);
        }
    }

    /**
     * Stop the tone if it is played.
     */
    private void stopTone() {
        // if local tone playback is disabled, just return.
        if (!mDTMFToneEnabled) {
            return;
        }
        synchronized (mToneGeneratorLock) {
            if (mToneGenerator == null) {
                Log.w("f", "stopTone: mToneGenerator == null");
                return;
            }
            mToneGenerator.stopTone();
        }
    }

    private void keyPressed(int keyCode) {

        switch (keyCode) {
            case KeyEvent.KEYCODE_1:
                playTone(ToneGenerator.TONE_DTMF_1, TONE_LENGTH_INFINITE);
                break;
            case KeyEvent.KEYCODE_2:
                playTone(ToneGenerator.TONE_DTMF_2, TONE_LENGTH_INFINITE);
                break;
            case KeyEvent.KEYCODE_3:
                playTone(ToneGenerator.TONE_DTMF_3, TONE_LENGTH_INFINITE);
                break;
            case KeyEvent.KEYCODE_4:
                playTone(ToneGenerator.TONE_DTMF_4, TONE_LENGTH_INFINITE);
                break;
            case KeyEvent.KEYCODE_5:
                playTone(ToneGenerator.TONE_DTMF_5, TONE_LENGTH_INFINITE);
                break;
            case KeyEvent.KEYCODE_6:
                playTone(ToneGenerator.TONE_DTMF_6, TONE_LENGTH_INFINITE);
                break;
            case KeyEvent.KEYCODE_7:
                playTone(ToneGenerator.TONE_DTMF_7, TONE_LENGTH_INFINITE);
                break;
            case KeyEvent.KEYCODE_8:
                playTone(ToneGenerator.TONE_DTMF_8, TONE_LENGTH_INFINITE);
                break;
            case KeyEvent.KEYCODE_9:
                playTone(ToneGenerator.TONE_DTMF_9, TONE_LENGTH_INFINITE);
                break;
            case KeyEvent.KEYCODE_0:
                playTone(ToneGenerator.TONE_DTMF_0, TONE_LENGTH_INFINITE);
                break;
            case KeyEvent.KEYCODE_POUND:
                playTone(ToneGenerator.TONE_DTMF_P, TONE_LENGTH_INFINITE);
                break;
            case KeyEvent.KEYCODE_STAR:
                playTone(ToneGenerator.TONE_DTMF_S, TONE_LENGTH_INFINITE);
                break;
            default:
                break;
        }

        mHaptic.vibrate();
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
        mDigits.onKeyDown(keyCode, event);

        // If the cursor is at the end of the text we hide it.
        final int length = mDigits.length();
        if (length == mDigits.getSelectionStart() && length == mDigits.getSelectionEnd()) {
            mDigits.setCursorVisible(false);
        }
    }

    private void configureKeypadListeners(View view) {
        final int[] buttonIds = new int[] {R.id.one, R.id.two, R.id.three, R.id.four, R.id.five,
                R.id.six, R.id.seven, R.id.eight, R.id.nine, R.id.star, R.id.zero, R.id.pound};

        DialpadKeyButton dialpadKey;

        for (int i = 0; i < buttonIds.length; i++) {
            dialpadKey = (DialpadKeyButton) view.findViewById(buttonIds[i]);
            dialpadKey.setOnPressedListener(this);
        }


    }

    /**
     * When a key is pressed, we start playing DTMF tone, do vibration, and enter the digit
     * immediately. When a key is released, we stop the tone. Note that the "key press" event will
     * be delivered by the system with certain amount of delay, it won't be synced with user's
     * actual "touch-down" behavior.
     */
    @Override
    public void onPressed(View view, boolean pressed) {
        Log.d("f", "onPressed(). view: " + view + ", pressed: " + pressed);
        if (pressed) {
            int i = view.getId();
            if (i == R.id.one) {
                keyPressed(KeyEvent.KEYCODE_1);
            } else if (i == R.id.two) {
                keyPressed(KeyEvent.KEYCODE_2);
            } else if (i == R.id.three) {
                keyPressed(KeyEvent.KEYCODE_3);
            } else if (i == R.id.four) {
                keyPressed(KeyEvent.KEYCODE_4);
            } else if (i == R.id.five) {
                keyPressed(KeyEvent.KEYCODE_5);
            } else if (i == R.id.six) {
                keyPressed(KeyEvent.KEYCODE_6);
            } else if (i == R.id.seven) {
                keyPressed(KeyEvent.KEYCODE_7);
            } else if (i == R.id.eight) {
                keyPressed(KeyEvent.KEYCODE_8);
            } else if (i == R.id.nine) {
                keyPressed(KeyEvent.KEYCODE_9);
            } else if (i == R.id.zero) {
                keyPressed(KeyEvent.KEYCODE_0);
            } else if (i == R.id.pound) {
                keyPressed(KeyEvent.KEYCODE_POUND);
            } else if (i == R.id.star) {
                keyPressed(KeyEvent.KEYCODE_STAR);
            } else {
                Log.wtf("f", "Unexpected onTouch(ACTION_DOWN) event from: " + view);
            }
            mPressedDialpadKeys.add(view);
        } else {
            mPressedDialpadKeys.remove(view);
            if (mPressedDialpadKeys.isEmpty()) {
                stopTone();
            }
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        updateDeleteButtonEnabledState();
    }

    /**
     * Update the enabledness of the "Dial" and "Backspace" buttons if applicable.
     */
    private void updateDeleteButtonEnabledState() {

        final boolean digitsNotEmpty = !isDigitsEmpty();
        mDelete.setEnabled(digitsNotEmpty);
    }



    @Override
    public boolean onLongClick(View view) {
        final Editable digits = mDigits.getText();
        final int id = view.getId();
        if (id == R.id.deleteButton) {
            digits.clear();
            return true;
        } else {
        }
        return false;
    }
}

