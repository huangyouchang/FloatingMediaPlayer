package com.example.foatingaudioplayer;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.LinearLayout;

public class PlayerDragView extends LinearLayout{
  private int screenWidth;
  private int screenHeight;
  private Context mContext;
  private int lastX, lastY;
  private int left ,top;
  public static MarginLayoutParams layoutParams = null;
  private int startX;
  private int endX;
  private boolean isMoved = false;
  private int mStatusBarHeight;
  private DragViewStatusCallbcak statusCallbcak;
  public PlayerDragView(Context context) {
    this(context,null);
  }
  public PlayerDragView(Context context, AttributeSet attrs) {
    super(context, attrs);
    mContext = context;
    mStatusBarHeight = getStatusBarHeight();

    DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
    screenWidth = displayMetrics.widthPixels;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      screenHeight = displayMetrics.heightPixels;
    } else {
      screenHeight = displayMetrics.heightPixels-mStatusBarHeight;
    }
    setLayoutParams();
  }
  public void setLayoutParams(){
    post(new Runnable() {
      @Override
      public void run() {
        if (layoutParams == null) {
          layoutParams = (MarginLayoutParams)getLayoutParams();
          layoutParams.topMargin = screenHeight/2;
          layoutParams.leftMargin = 0;
        }
        setLayoutParams(layoutParams);
      }
    });
  }

  private int rawX;
  private int rawY;
  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    switch (ev.getAction()) {
      case MotionEvent.ACTION_DOWN:
        rawX = (int) ev.getRawX();
        rawY = (int) ev.getRawY();
        onTouchEvent(ev);
      break;
      case MotionEvent.ACTION_UP:
        Log.e("ly", "onInterceptTouchEvent: up");
        onTouchEvent(ev);
        break;

      case MotionEvent.ACTION_MOVE:
        if (Math.abs(ev.getRawX() - rawX) >20 || Math.abs(ev.getRawY() - rawY) >20) {
          return onTouchEvent(ev);
        }
        break;
    }
    return super.onInterceptTouchEvent(ev);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    return dealTouch(event);
  }
  public void onTouchEventEx(MotionEvent event) {
    dealTouch(event);
  }

  private boolean dealTouch(MotionEvent event) {
    switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN:
        lastX = (int) event.getRawX();
        lastY = (int) event.getRawY();
        startX = lastX;
        break;
      case MotionEvent.ACTION_MOVE:
        isMoved = true;
        int dx = (int) event.getRawX() - lastX;
        int dy = (int) event.getRawY() - lastY;
//        if (Math.abs(event.getRawX() - startX) > 30) {
//          statusCallbcak.move();
//        }
        left = getLeft() + dx;
        top = getTop() + dy;
        int right = getRight() + dx;
        int bottom = getBottom() + dy;
        // 设置不能出界
        if (left < 0) {
          left = 0;
          right = left + getWidth();
        }
        if (right > screenWidth) {
          right = screenWidth;
          left = right - getWidth();
        }
        if (top < mStatusBarHeight) {
          top = mStatusBarHeight;
          bottom = top + getHeight();
        }
        if (bottom > screenHeight + mStatusBarHeight) {
          bottom = screenHeight + mStatusBarHeight;
          top = bottom - getHeight();
        }
        layoutParams = (MarginLayoutParams)getLayoutParams();
        layoutParams.topMargin = top;
        layoutParams.leftMargin = left;
        setLayoutParams(layoutParams);
        Log.e("ly", "onTouch: "+left+"----"+top+"----"+right+"----"+bottom+"----");
        lastX = (int) event.getRawX();
        lastY = (int) event.getRawY();
        break;
      case MotionEvent.ACTION_UP:
        //只有滑动改变上边距时，抬起才进行设置
        Log.e("ly", "onTouch: up");
        if (isMoved) {
          layoutParams = (MarginLayoutParams)getLayoutParams();
          layoutParams.topMargin = top;
          setLayoutParams(layoutParams);
          isMoved = false;
        }
        endX = (int) event.getRawX();
        //滑动距离比较小，当作点击事件处理
        if (Math.abs(startX - endX) < 20) {
//          performClick();
          return false;
        }

        startScroll(left,screenWidth/2,true);
        break;
    }
    return true;
  }
  public void startScroll(final int start, int end, final boolean isLeft){
    ValueAnimator valueAnimator = ValueAnimator.ofFloat(start,end).setDuration(200);
    valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      @Override
      public void onAnimationUpdate(ValueAnimator animation) {
        if (isLeft) {
          layoutParams.leftMargin = (int) (start*(1-animation.getAnimatedFraction()));
        } else {
          layoutParams.leftMargin = (int) (start + (screenWidth - start - getWidth())*(animation.getAnimatedFraction()));
        }
        setLayoutParams(layoutParams);
      }
    });
    valueAnimator.start();
  }
  /**
   * 获取状态栏的高度
   * @return 状态栏高度
   */
  public int getStatusBarHeight() {
    int result = 0;
    int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
    if (resourceId > 0) {
      result = getResources().getDimensionPixelSize(resourceId);
    }
    return result;
  }

  public void setStatusCallbcak(DragViewStatusCallbcak statusCallbcak) {
    this.statusCallbcak = statusCallbcak;
  }

  public interface DragViewStatusCallbcak{
    void move();
  }
}
