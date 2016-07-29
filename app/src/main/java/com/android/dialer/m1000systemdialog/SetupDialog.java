package com.android.dialer.m1000systemdialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout.LayoutParams;

import com.android.dialer.R;

	/**
	*add by liupengfei 20160423.
	*自定义dialog
	*样式：R.style.SetupDialog :透明背景，无标题栏，无黑色边框。
	*布局文件：R.layout.round_alert_dialog_layout : 标题栏、警告图标、内容、左边确定和右边取消。
	*背景：R.drwable.round_alert_dialog_title_bg 等 ：圆角半径:9px，颜色：纯白，点击：cc纯白。
	**/
 public class SetupDialog extends Dialog {
	  
    public SetupDialog(Context context, int theme) {
        super(context, theme);
    }
  
    public SetupDialog(Context context) {
        super(context);
    }
  
    /**
     * Helper class for creating a custom dialog
     */
    public static class Builder {
  
        private Context context;
        private View contentView;
  
        private OnClickListener negativeButtonClickListener;
  
        public Builder(Context context) {
            this.context = context;
        }

  


        /**
         * Set a custom content view for the Dialog.
         * If a message is set, the contentView is not
         * added to the Dialog...
         * @param v
         * @return
         */
        public Builder setContentView(View v) {
            this.contentView = v;
            return this;
        }
  

        /**
         * Set the negative button resource and it's listener
         * @param listener
         * @return
         */
        public Builder setNegativeButton(OnClickListener listener) {
            this.negativeButtonClickListener = listener;
            return this;
        }
  
        /**
         * Create the custom dialog
         */
        public SetupDialog create() {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            // instantiate the dialog with the custom Theme
            final SetupDialog dialog = new SetupDialog(context,R.style.SetupDialog);
            dialog.setCanceledOnTouchOutside(false);
            View layout = inflater.inflate(R.layout.setup_dialog_layout, null);
            dialog.addContentView(layout, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

            //点击其它区域dismiss掉
            (layout.findViewById(R.id.container)).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });

            //点击设置
            if (negativeButtonClickListener != null) {
                (layout.findViewById(R.id.setup_back)).setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        negativeButtonClickListener.onClick(dialog, DialogInterface.BUTTON_NEGATIVE);
                    }
                });
            }

            Window window = dialog.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(dialog.getContext().getResources().getColor(R.color.setup_dialog_startbar_color));

            dialog.setContentView(layout);
            return dialog;
        }
  
    }
  
}