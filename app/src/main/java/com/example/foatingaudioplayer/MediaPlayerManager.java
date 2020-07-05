package com.example.foatingaudioplayer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.gif.GifDrawable;

import java.io.IOException;
import java.lang.ref.WeakReference;

import static android.content.Context.AUDIO_SERVICE;
import static android.content.Context.TELEPHONY_SERVICE;

/**
 * 语音播放悬浮框
 */
public class MediaPlayerManager implements  MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener,AudioManager.OnAudioFocusChangeListener,MediaPlayer.OnBufferingUpdateListener {

    private final String TAG = "MediaPlayerManager";
    private final String SPEECH_SPEED = "PLAYER_SPEECH_SPEED";
    private Display d;

    private static volatile MediaPlayerManager singleton;
    /***初始化必须传递的资源 start***/
    private WeakReference<Activity> activity;
    private String mSourceUrl;
    private String mAudioUrl;
    private String mId;
    private String mTitle;
    /***初始化必须传递的资源 end***/

    /***view start***/
    private PlayerDragView mVoiceView;
    private SeekBar progressBar;
    private FrameLayout mGifImageLayout;
    private LinearLayout mAllAudioView;
    private ImageView mGifImage, mDrawdownImage, mPlayStopImage, mCloseImage;
    private TextView mTitleTv, mStartTime, mEndTime, mAudioSpeedTv;

    private GifDrawable mDrawable;
    /***view end***/
    private boolean isShowVoiceView;
    public ClickInterface mClickInterface;

    /***播放器相关***/
    private MediaPlayer mPlayer;
    private AudioManager audioManager;

    private boolean hasPrepared;
    private boolean isPlay;//是否在播放
    private boolean bUpdateThreadFLag = false;
    private boolean isError;
    private boolean isLoading = false;
    private boolean errorHappened = false;
    //当前暂停是手动暂停的
    private boolean shoudongStop = false;

    private PlayerSPHelper mSPHelper;

    private Thread mUpdateThread;

    private int progressBarMax = 100;
    private int currentPosition = 0;
    private int bufferPercent = 0;

    private Handler handler = null;

    /***手机通话的监听***/
    private TelephonyManager tm;
    private MyListener listener;
    private boolean isPausedByFocusLossTransient;

    /***初始化语音播放悬浮框 start***/
    private MediaPlayerManager() {
    }
    public static MediaPlayerManager getInstance() {
        if (singleton == null) {
            synchronized (MediaPlayerManager.class) {
                if (singleton == null) {
                    singleton = new MediaPlayerManager();
                }
            }
        }
        return singleton;
    }

    //getInstance 方法的下一步，必须调用此方法
    public MediaPlayerManager setCurrentActivity(Activity activity) {
        if(activity==null) throw new RuntimeException("activity must not be null");
        this.activity = new WeakReference<>(activity);
        d = ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        mSPHelper = new PlayerSPHelper(activity);
        audioManager = (AudioManager) activity.getSystemService(AUDIO_SERVICE);

        registerReceiver();
        return MediaPlayerManager.this;
    }

    public MediaPlayerManager setAudioUrl(String AudioUrl) {
        checkNull();
        this.mAudioUrl = AudioUrl;
        return this;
    }

    public MediaPlayerManager setSourceUrl(String SouceUrl) {
        checkNull();
        this.mSourceUrl = SouceUrl;
        return this;
    }
    public MediaPlayerManager setId(String mId) {
        checkNull();
        this.mId = mId;
        return this;
    }

    public MediaPlayerManager setTitle(String title) {
        checkNull();
        this.mTitle = title;
        return this;
    }

    public String getId() {
        return mId;
    }
    public String getAudioUrl() {
        return mAudioUrl;
    }
    public String getSourceUrl() {
        return mSourceUrl;
    }
    /***初始化语音播放悬浮框 end***/

    public void play() {
        if (!PlayerNetUtil.hasNetwork(activity.get())){
            Toast.makeText(activity.get(), "无网络连接", Toast.LENGTH_SHORT).show();
        }else if (!PlayerNetUtil.isWifiConnected(activity.get())) {

            final PlayerAlertDialog.Builder builder = new PlayerAlertDialog.Builder(activity.get());
            builder.setTitle("系统提示")
//                    .setMessage("正在使用非WIFI网络，播放将产生流量费用。是否继续播放？")
                    .setPositiveButton("继续播放", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            builder.dismiss();
                            init();
                        }
                    })
                    .setNegativeButton("取消", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            builder.dismiss();
                        }
                    });
            builder.show();
        } else {
            init();
        }
    }

    private void init(){
        initVoiceView();
        setShowVoiceView(true);
        hasPrepared = false; // 开始播放前讲Flag置为不可操作
        initIfNecessary(); // 如果是第一次播放/player已经释放了，就会重新创建、初始化
        try {
            mPlayer.reset();
            mPlayer.setDataSource(activity.get(), Uri.parse(mAudioUrl)); // 设置曲目资源
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mPlayer.prepareAsync(); // 异步的准备方法

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initVoiceView() {
        if (mVoiceView == null) {
            mVoiceView = (PlayerDragView) LayoutInflater.from(activity.get()).inflate(R.layout.audio_player_layout, null);
            progressBar = mVoiceView.findViewById(R.id.seekbar);
            progressBar.setMax(progressBarMax);

            mAllAudioView =  mVoiceView.findViewById(R.id.all_audio_view);
            mGifImageLayout = mVoiceView.findViewById(R.id.gif_image_layout);
            mGifImage = mVoiceView.findViewById(R.id.gif_image);

            mDrawdownImage = mVoiceView.findViewById(R.id.drawdown_image);
            mPlayStopImage = mVoiceView.findViewById(R.id.play_stop_image);
            mCloseImage = mVoiceView.findViewById(R.id.close_image);

            mTitleTv = mVoiceView.findViewById(R.id.title_tv);
            mTitleTv.setText(mTitle);
            mStartTime = mVoiceView.findViewById(R.id.start_time);
            mEndTime = mVoiceView.findViewById(R.id.end_time);
            mAudioSpeedTv = mVoiceView.findViewById(R.id.audio_speed_tv);

            addVoiceView();
            setEvent();
        }
    }

    public void setShowVoiceView(boolean showVoiceView) {
        isShowVoiceView = showVoiceView;
    }

    private void initIfNecessary() {
        if (null == mPlayer) {
            mPlayer = new MediaPlayer();
            mPlayer.setLooping(false);
//            播放出错监听
            mPlayer.setOnErrorListener(this);
//            播放完成监听
            mPlayer.setOnCompletionListener(this);
//            准备Prepared完成监听
            mPlayer.setOnPreparedListener(this);
            //缓存进度监听
            mPlayer.setOnBufferingUpdateListener(this);
        }
    }

    private void setEvent() {

        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(!fromUser) return;

                //手动拖动进度条
                if(bufferPercent>=100){//完全缓存成功以后，才允许拖动
                    try {
                        seekTo((int) (((float)progress/progressBarMax)*getDuration()));
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mGifImageLayout.setOnClickListener(v -> {
            mAllAudioView.setVisibility(View.VISIBLE);
            mGifImageLayout.setVisibility(View.GONE);
        });

        mDrawdownImage.setOnClickListener(v -> {
            mAllAudioView.setVisibility(View.GONE);
            mGifImageLayout.setVisibility(View.VISIBLE);
        });
        mPlayStopImage.setOnClickListener(v -> {

            changeStatus();
        });
        mCloseImage.setOnClickListener(v -> {
            removeVoiceView();
            release();
        });

        //6.0手机才支持设置语音播放速度
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mAudioSpeedTv.setOnClickListener(v -> {
                if(mPlayer==null || !hasPrepared) return;

                float speed = (float) mSPHelper.get(SPEECH_SPEED,1.0f);

                if (mPlayer.isPlaying()) {
                    if( speed == 1.0f){
                        mPlayer.setPlaybackParams(mPlayer.getPlaybackParams().setSpeed(1.25f));
                        mAudioSpeedTv.setText("快");
                    }else{
                        mPlayer.setPlaybackParams(mPlayer.getPlaybackParams().setSpeed(1.0f));
                        mAudioSpeedTv.setText("正常");
                    }
                } else {
                    // 判断是否正在播放，未播放时，要在设置Speed后，暂停音乐播放
                    if(speed == 1.0f){
                        mPlayer.setPlaybackParams(mPlayer.getPlaybackParams().setSpeed(1.25f));
                        mAudioSpeedTv.setText("快");
                    }else{
                        mPlayer.setPlaybackParams(mPlayer.getPlaybackParams().setSpeed(1.0f));
                        mAudioSpeedTv.setText("正常");
                    }

                    start();
                }

                mSPHelper.put(SPEECH_SPEED,mPlayer.getPlaybackParams().getSpeed());

            });
        }

        mTitleTv.setOnClickListener(v -> {
            if(mClickInterface!=null) mClickInterface.titleClick();
        });
    }

    public void addVoiceView() {
        if (mVoiceView != null) {
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(getWidth()-dp2px(10),
                    dp2px(62));
            layoutParams.leftMargin= dp2px(5);
            layoutParams.rightMargin = dp2px(5);
            mAllAudioView.setLayoutParams(layoutParams);

            FrameLayout.LayoutParams layoutParams_p = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);

            ((FrameLayout) activity.get().getWindow().getDecorView()).addView(MediaPlayerManager.getInstance().getVoiceView(), layoutParams_p);
            mVoiceView.setLayoutParams();


            Glide.with(activity.get()).asGif().load(R.drawable.common_audio_gif).into(mGifImage);
            mGifImage.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mDrawable = (GifDrawable) mGifImage.getDrawable();
                    if(isPlay){
                        gifStart();
                    }else{
                        gifStop();
                    }

                }
            },200);
        }
    }
    public void removeVoiceView() {
        if (mVoiceView != null && activity.get()!=null) {
            ((FrameLayout) activity.get().getWindow().getDecorView()).removeView(mVoiceView);
        }
    }

    public void requestAudioFocus() {
        audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
    }

    public void abandonAudioFocus() {
        audioManager.abandonAudioFocus(this);
    }

    public void changeStatus() {
        if (mPlayer!=null &&  isPlay){
            shoudongStop = true;
            pause();
        } else {
            shoudongStop = false;
            if(bufferPercent>=100){
                hasPrepared = true;
            }
            start();
        }
    }



    public void start() {
        // 获取音频焦点
        requestAudioFocus();
        stopLoadingAnimation();
        gifStart();
        mPlayStopImage.setImageDrawable(activity.get().getDrawable(R.drawable.common_audio_playing));
        // release()会释放player、将player置空，所以这里需要判断一下
        if (null != mPlayer && hasPrepared) {
            mPlayer.start();

            isPlay = true;
        }

        initHandler();
    }

    public void pause() {
        abandonAudioFocus();

        gifStop();
        mPlayStopImage.setImageDrawable(activity.get().getDrawable(R.drawable.common_audio_stop));
        if (null != mPlayer && hasPrepared) {
            mPlayer.pause();
            isPlay = false;

        }
    }
    private void initHandler() {
        if(handler == null){
            handler = new Handler(){
                @Override
                public void handleMessage(@NonNull Message msg) {
                    super.handleMessage(msg);
                    switch (msg.what){
                        case 1:
                            if (progressBar != null) {
                                progressBar.setProgress(msg.arg1);
                                mStartTime.setText((String)msg.obj);
                            }
                            if(currentPosition>=bufferPercent-2){
                                loadingAnimation();
                            }else{
                                stopLoadingAnimation();
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                float speed = (float) mSPHelper.get(SPEECH_SPEED,1.0f);
                                if(isPlay && speed!=mPlayer.getPlaybackParams().getSpeed()){

                                    if(speed == 1.0f){
                                        mAudioSpeedTv.setText("正常");
                                    }else{
                                        mAudioSpeedTv.setText("快");
                                    }

                                    if(mPlayer!=null){
                                        mPlayer.setPlaybackParams(mPlayer.getPlaybackParams().setSpeed(speed));
                                    }
                                }
                            }

                            if(errorHappened && PlayerNetUtil.hasNetwork(activity.get())){
                                errorHappened = false;

                                //重新播放
                                MediaPlayerManager.getInstance()
                                        .setCurrentActivity(activity.get())
                                        .setId(mId)
                                        .setTitle(mTitle)
                                        .setAudioUrl(mAudioUrl)
                                        .setSourceUrl(mSourceUrl)
                                        .play();
                            }
                            break;
                        default:
                            break;
                    }
                }
            };
        }
    }


    public void seekTo(int position) {
        if (null != mPlayer && hasPrepared) {
            mPlayer.seekTo(position);
        }
    }

    /**
     * 对于播放视频来说，通过设置SurfaceHolder来设置显示Surface。这个方法不需要判断状态、也不会改变player状态
     * @param holder SurfaceHolder
     */
    public void setDisplay(SurfaceHolder holder) {
        if (null != mPlayer) {
            mPlayer.setDisplay(holder);
        }
    }
    public void release() {
        handler.removeCallbacksAndMessages(null);
        handler = null;

        bUpdateThreadFLag = false;
        if (mUpdateThread != null) {
            mUpdateThread = null;
        }
        hasPrepared = false;
        if(mPlayer!=null){
            mPlayer.stop();
            mPlayer.release();
        }

        mPlayer = null;
        activity.clear();
        mVoiceView = null;
        isPlay = false;
        singleton = null;
        isShowVoiceView = false;
        mGifImage = null;

        unRegisterReceiver();

        abandonAudioFocus();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        hasPrepared = true; // 准备完成后回调到这里

        new Thread() {
            @Override
            public void run() {
                Log.e(TAG,"bufferPercent="+bufferPercent);
                Log.e(TAG,"currentPosition="+currentPosition);
                while (bufferPercent<currentPosition) {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }

                mVoiceView.post(new Runnable() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void run() {
                        int lastPostion = currentPosition;
                        MediaPlayerManager.getInstance().start();
                        updateSeekBar();
                        seekTo((int) (((float)lastPostion/progressBarMax)*getDuration()));
                        mStartTime.setText(durationCalculate(0));
                        mEndTime.setText(durationCalculate(getDuration()));
                    }
                });
            }
        }.start();

    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        hasPrepared = false;
        isPlay = false;
        // 通知调用处，调用play()方法进行下一个曲目的播放
        if (isError) {
            isError = false;
            return;
        }
        seekTo(0);
        progressBar.setProgress(0);
        pause();
        Log.e(TAG,"onCompletion");
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {

        currentPosition = progressBar.getProgress();

        hasPrepared = false;
        isPlay = false;
        isError = true;
        errorHappened = true;
        pause();
        mPlayer.reset();
        mPlayer.release();
        mPlayer = null;

        Toast.makeText(activity.get(),"网络异常！",Toast.LENGTH_SHORT).show();
        return true;
    }

    /**
     * @return 当前正在播放的音频进度节点
     */
    public int getCurrentPosition(){
        try {
            return mPlayer != null ? mPlayer.getCurrentPosition() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     *
     * @return 音频的总时长
     */
    public int getDuration() {
        try {
            return mPlayer != null ? mPlayer.getDuration() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public View getVoiceView() {
        return mVoiceView;
    }
    /**
     * @return 当前是否显示悬浮播放器
     */
    public boolean isShowVoiceView() {
        return isShowVoiceView;
    }

    /**
     * 更新进度
     */
    private void updateSeekBar() {
        bUpdateThreadFLag = true;
        if(mUpdateThread!=null) return;
        //开启线程发送数据
        mUpdateThread = new Thread() {
            @Override
            public void run() {
                while (bUpdateThreadFLag) {

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if(handler==null) return;

                    Message msg = Message.obtain();
                    msg.what = 1;
                    msg.arg1 = hasPrepared?(int) ((float)getCurrentPosition()/(float) getDuration()*100) : progressBar.getProgress();
                    msg.obj =hasPrepared? durationCalculate(getCurrentPosition()) : mStartTime.getText().toString();

                    handler.sendMessage(msg);

                }
            }
        };
        mUpdateThread.start();
    }

    /**
     * @return 播放状态
     */
    public boolean isPlay() {
        return isPlay;
    }

    private void gifStop(){
        if(mDrawable!=null)
            mDrawable.stop();
    }
    private void gifStart(){
        if(mDrawable!=null)
            mDrawable.start();
    }

    //格式化语音时长
    private String durationCalculate(int duration){
        String durationString = "";

        if(duration>=1000){
            int secondDuration = duration/1000;

            int minuteInt = secondDuration/60;
            int secondInt = secondDuration%60;
            String minuteString = "";
            String secondString = "";
            if(minuteInt<10){
                minuteString = "0"+minuteInt;
            }else{
                minuteString = String.valueOf(minuteInt);
            }

            if(secondInt<10){
                secondString = "0"+secondInt;
            }else{
                secondString = String.valueOf(secondInt);
            }

            durationString = minuteString+":"+secondString;
        }else{
            durationString = "00:00";
        }

        return durationString;
    }

    private void loadingAnimation(){
        if(mPlayStopImage==null) return;
        if(isLoading) return;
        mPlayStopImage.setImageDrawable(activity.get().getDrawable(R.drawable.media_loading));
        Animation mAnimation = AnimationUtils.loadAnimation(activity.get(), R.anim.common_loading_rotate);
        LinearInterpolator interpolator = new LinearInterpolator();
        mAnimation.setInterpolator(interpolator);
        mPlayStopImage.startAnimation(mAnimation);
    }
    private void stopLoadingAnimation(){
        if(mPlayStopImage==null) return;
        isLoading = false;
        mPlayStopImage.clearAnimation();
        if(isPlay){
            mPlayStopImage.setImageDrawable(activity.get().getDrawable(R.drawable.common_audio_playing));
        }else{
            mPlayStopImage.setImageDrawable(activity.get().getDrawable(R.drawable.common_audio_stop));
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        Log.e(TAG,focusChange+"");
        switch (focusChange) {
            // 重新获得焦点
            case AudioManager.AUDIOFOCUS_GAIN:
                if (isPausedByFocusLossTransient) {
                    // 通话结束，恢复播放
                    start();
                }

                isPausedByFocusLossTransient = false;
                Log.e(TAG, "重新获得焦点");
                break;
            // 永久丢失焦点，如被其他播放器抢占
            case AudioManager.AUDIOFOCUS_LOSS:
                pause();
                abandonAudioFocus();
                Log.e(TAG, "永久丢失焦点，如被其他播放器抢占");
                break;
            // 短暂丢失焦点，如来电
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                pause();
                isPausedByFocusLossTransient = true;
                Log.e(TAG, "短暂丢失焦点，如来电");
                break;
            // 瞬间丢失焦点，如通知
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // 音量减小为一半
//                AudioPlayer.getInstance().getMediaPlayer().setVolume(0.5f, 0.5f);
                Log.e(TAG, "瞬间丢失焦点，如通知");
                break;
            default:
                break;
        }

    }
    
    private void registerReceiver(){
        if(activity == null || activity.get()==null) return;
        tm = (TelephonyManager) activity.get().getSystemService(TELEPHONY_SERVICE);
        listener = new MyListener();
        tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
    }
    private void unRegisterReceiver(){
        tm.listen(listener, PhoneStateListener.LISTEN_NONE);
        listener = null;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        bufferPercent = percent;

        Log.e(TAG,"Buffer percent="+percent);
    }

    private class MyListener extends PhoneStateListener {

        // 当电话的呼叫状态发生变化的时候调用的方法
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            Log.e(TAG, "state" + state);
            try {
                switch (state) {
                    case TelephonyManager.CALL_STATE_IDLE://空闲状态。
                        if(shoudongStop) return;
                        //继续播放音乐
                        if(!isPlay){
                            start();
                        }
                        Log.e(TAG, "空闲状态");
                        break;
                    case TelephonyManager.CALL_STATE_RINGING://铃响状态。

                        if(isPlay){
                            pause();
                        }
                        //暂停播放音乐
                        Log.e(TAG,  "铃响状态");
                        break;
                    case TelephonyManager.CALL_STATE_OFFHOOK://通话状态
                        if(isPlay){
                            pause();
                        }
                        Log.e(TAG,  "通话状态");
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private int sp2px(float spValue) {
        final float fontScale = activity.get().getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }
    private int dp2px(float dpVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpVal,
                activity.get().getResources().getDisplayMetrics());
    }

    private int getWidth(){
        DisplayMetrics displayMetrics = new DisplayMetrics();
        d.getMetrics(displayMetrics);

        int displayWidth = displayMetrics.widthPixels;

        return displayWidth;
    }

    private int getheight(){
        DisplayMetrics displayMetrics = new DisplayMetrics();
        d.getMetrics(displayMetrics);

        int displayHeight = displayMetrics.heightPixels;

        return displayHeight;
    }

    private void checkNull(){
        if(this.activity==null || this.activity.get()==null) throw new RuntimeException("should call setCurrentActivity method first");
    }

    public void setClickInterface(ClickInterface clickInterface) {
        this.mClickInterface = clickInterface;
    }

    public interface ClickInterface{
        void titleClick();
    }
}
