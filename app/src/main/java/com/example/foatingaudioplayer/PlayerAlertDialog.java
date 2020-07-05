package com.example.foatingaudioplayer;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatDialog;
import androidx.constraintlayout.widget.ConstraintLayout;

public class PlayerAlertDialog extends AppCompatDialog {
    protected LinearLayout vLayoutContent;
    protected LinearLayout vLayoutContentSub; //增加一个sub content

    protected TextView titleTxt;

    protected ConstraintLayout vLayoutButtons;
    protected Button vConfirmBtn;
    protected Button vCancelBtn;


    protected Context mContext;

    private View.OnClickListener onDefaultClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            cancel();
        }

    };

    protected CharSequence title;

    protected View contentView;
    protected View contentViewSub;

    protected CharSequence positiveText;

    protected CharSequence negativeText;

    protected  @DrawableRes int positiveBackgroundRes;

    protected  @DrawableRes int negativeBackgroundRes;

//    protected int mOrientation = LinearLayout.HORIZONTAL;//默认的button的方向
    protected boolean showNegativeButton;
    /**
     * 点击Positive按钮，是否关闭框，默认是关闭
     */
    protected boolean isPositiveDismissed;

    protected View.OnClickListener onPositiveListener = onDefaultClickListener;
    protected View.OnClickListener onNegativeListener = onDefaultClickListener;


    public PlayerAlertDialog(@NonNull Context context) {
        this(context, R.style.playerDialog);
    }

    public PlayerAlertDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
        this.mContext = context;

    }

    protected PlayerAlertDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        this.mContext = context;

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_alert_dialog);
        setCanceledOnTouchOutside(false);
        setCancelable(false);
        initView();
    }

    private void initView() {
        vLayoutContent = findViewById(R.id.layout_content);
        vLayoutContentSub = findViewById(R.id.layout_content_sub);
        titleTxt = findViewById(R.id.text_title);
        vLayoutButtons = findViewById(R.id.layout_buttons);
        vConfirmBtn = findViewById(R.id.confirm_btn);
        vCancelBtn = findViewById(R.id.cancel_btn);

    }

    @Override
    public void show() {
        super.show();
        setTitle(title);
        setContent(contentView);
        setSubContent(contentViewSub);
        setPositiveButton(positiveText, onPositiveListener);
        if(positiveBackgroundRes!=0){
            setPositiveBackground(positiveBackgroundRes);
        }
        if (showNegativeButton) {
            setNegativeButton(negativeText, onNegativeListener);
            if(negativeBackgroundRes!=0){
                setNegativeBackground(negativeBackgroundRes);
            }

        }
    }

    public void setTitle(CharSequence title) {
        if (!TextUtils.isEmpty(title)) {
            titleTxt.setVisibility(View.VISIBLE);
            titleTxt.setText(title);
        }else{
            titleTxt.setVisibility(View.GONE);
        }
    }

    public void setTitle(CharSequence title,int gravity){
        if (!TextUtils.isEmpty(title)) {
            titleTxt.setVisibility(View.VISIBLE);
            titleTxt.setText(title);
        }else{
            titleTxt.setVisibility(View.GONE);
        }
        titleTxt.setGravity(gravity);
    }


    public void setContent(View view) {
        if (view != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if(parent!=null){
                parent.removeAllViews();
            }

            vLayoutContent.addView(view);

        }

    }

    public void setSubContent(View view) {
        if (view != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if(parent!=null){
                parent.removeAllViews();
            }
            vLayoutContentSub.addView(view);

        }

    }

    public void setContentMargin(int left,int right){
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) vLayoutContent.getLayoutParams();
        params.leftMargin = left;
        params.rightMargin = right;
    }


    // 确定按钮一定可见
    public void setPositiveButton(CharSequence text, final View.OnClickListener listener) {
        if (!TextUtils.isEmpty(text)) {
            vConfirmBtn.setText(text);
        }
        vConfirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onClick(v);
                }
                if(isPositiveDismissed){
                    dismiss();
                }
            }
        });
    }

    //取消按钮 默认是隐藏
    public void setNegativeButton(CharSequence text, final View.OnClickListener listener) {
        if (!TextUtils.isEmpty(text)) {
            vCancelBtn.setText(text);
        }

        vCancelBtn.setVisibility(View.VISIBLE);

        vCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onClick(v);

                }
                dismiss();
            }
        });
    }

    public void setPositiveBackground(@DrawableRes int drawableId){
        vConfirmBtn.setBackgroundResource(drawableId);
    }

    public void setNegativeBackground(@DrawableRes int drawableId){
        vCancelBtn.setBackgroundResource(drawableId);
    }

    public static class Builder<T extends PlayerAlertDialog, K extends Builder<T, K>> {
        protected T dialog;     //子类必须实例化 不然空指针
        protected Context context;

        public Builder(Context context) {
            this.context = context;
            dialog = (T) new PlayerAlertDialog(context);
        }

        public K setTitle(@StringRes int titleId) {
            dialog.title = context.getText(titleId);
            return (K) this;
        }

        public K setTitle(CharSequence title) {
            dialog.title = title;
            return (K) this;
        }


        public K setContentView(View contentView) {
            dialog.contentView = contentView;
            return (K) this;
        }

        public K setSubContentView(View contentViewSub) {
            dialog.contentViewSub = contentViewSub;
            return (K) this;
        }

        public K setPositiveButton(@StringRes int textId, final View.OnClickListener listener) {
            return  setPositiveButton(textId,true,listener);
        }

        public K setPositiveButton(@StringRes int textId, boolean isPositiveDismissed,final View.OnClickListener listener) {
            dialog.positiveText = context.getText(textId);
            dialog.onPositiveListener = listener;
            dialog.isPositiveDismissed = isPositiveDismissed;
            return (K) this;
        }

        public K setPositiveButton(CharSequence text, final View.OnClickListener listener) {
            return setPositiveButton(text,true,listener);
        }

        public K setPositiveButton(CharSequence text, boolean isPositiveDismissed,final View.OnClickListener listener) {
            dialog.positiveText = text;
            dialog.onPositiveListener = listener;
            dialog.isPositiveDismissed = isPositiveDismissed;
            return (K) this;
        }

        public K setPositiveBackground(@DrawableRes int drawableRes) {
            dialog.positiveBackgroundRes = drawableRes;
            return (K) this;
        }


        public K setNegativeButton(@StringRes int textId, final View.OnClickListener listener) {
            dialog.negativeText = context.getText(textId);
            dialog.onNegativeListener = listener;
            dialog.showNegativeButton = true;
            return (K) this;
        }

        public K setNegativeButton(CharSequence text, final View.OnClickListener listener) {
            dialog.negativeText = text;
            dialog.onNegativeListener = listener;
            dialog.showNegativeButton = true;
            return (K) this;
        }

        public K ssetNegativeBackground(@DrawableRes int drawableRes) {
            dialog.negativeBackgroundRes = drawableRes;
            return (K) this;
        }


        public K setCancelable(boolean cancelable) {
            dialog.setCancelable(cancelable);
            return (K) this;
        }

        /*public K setOrientation(int orientation){
            dialog.mOrientation = orientation;
            return (K) this;
        }*/

        public void dismiss(){
            dialog.dismiss();
        }


        public T create() {
            return dialog;
        }

        /**
         * Creates an {@link PlayerAlertDialog} with the arguments supplied to this
         * builder and immediately displays the dialog.
         * <p>
         * Calling this method is functionally identical to:
         * <pre>
         *     CommonAlertDialog dialog = builder.create();
         *     dialog.show();
         * </pre>
         */
        public T show() {
            final T dialog = create();
            dialog.show();
            return dialog;
        }


    }
}
