package com.sophiemarceauqu.lib_audio.mediaplayer.view;

import android.animation.Animator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.viewpager.widget.ViewPager;

import com.sophiemarceauqu.lib_audio.R;
import com.sophiemarceauqu.lib_audio.mediaplayer.core.AudioController;
import com.sophiemarceauqu.lib_audio.mediaplayer.events.AudioLoadEvent;
import com.sophiemarceauqu.lib_audio.mediaplayer.events.AudioPauseEvent;
import com.sophiemarceauqu.lib_audio.mediaplayer.events.AudioStartEvent;
import com.sophiemarceauqu.lib_audio.mediaplayer.model.AudioBean;
import com.sophiemarceauqu.lib_audio.mediaplayer.view.adapter.MusicPagerAdapter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

/**
 * 音乐播放器页面唱针布局
 */
public class IndictorView extends RelativeLayout implements ViewPager.OnPageChangeListener {
    private Context mContext;
    //View相关
    private ImageView mImageView;
    private ViewPager mViewPager;
    private MusicPagerAdapter mMusicPagerAdapter;
    //data
    private AudioBean mAudioBean;//当前播放歌曲
    private ArrayList<AudioBean> mQueue;//播放队列

    public IndictorView(Context context) {
        this(context, null);
    }

    public IndictorView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IndictorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        EventBus.getDefault().register(this);
        initData();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initView();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        //指定要播放的position
        AudioController.getInstance().setPlayIndex(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        switch (state) {
            case ViewPager.SCROLL_STATE_IDLE:
                showPlayView();
                break;
            case ViewPager.SCROLL_STATE_DRAGGING:
                showPauseView();
                break;
            case ViewPager.SCROLL_STATE_SETTLING:
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioLoadEvent(AudioLoadEvent event) {
        //更新Viewpageer为load状态
        mAudioBean = event.mAudioBean;
        showLoadView(true);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioPauseEvent(AudioPauseEvent event) {
        //更新Activity为暂停状态
        showPauseView();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioStartEvent(AudioStartEvent event) {
        //更新Activity为播放状态
        showPlayView();
    }

    private void initData() {
        mQueue = AudioController.getInstance().getQueue();
        mAudioBean = AudioController.getInstance().getNowPlaying();
    }

    private void initView() {
        View rootView = LayoutInflater.from(mContext).inflate(R.layout.indictor_view, this);
        mImageView = rootView.findViewById(R.id.tip_view);
        mViewPager = rootView.findViewById(R.id.view_pager);
        mViewPager.setOverScrollMode(View.OVER_SCROLL_NEVER);
        mMusicPagerAdapter = new MusicPagerAdapter(mQueue, mContext, null);
        mViewPager.setAdapter(mMusicPagerAdapter);
        showLoadView(false);
        //要在UI初始化完，否则会多一次listen响应
        mViewPager.addOnPageChangeListener(this);
    }

    private void showLoadView(boolean isSmooth) {
        mViewPager.setCurrentItem(mQueue.indexOf(mAudioBean), isSmooth);
    }


    private void showPlayView() {
        Animator anim = mMusicPagerAdapter.getAnim(mViewPager.getCurrentItem());
        if (anim != null) {
            anim.pause();
        }
    }

    private void showPauseView() {
        Animator anim = mMusicPagerAdapter.getAnim(mViewPager.getCurrentItem());
        if (anim != null) {
            if (anim.isPaused()) {
                anim.resume();
            } else {
                anim.start();
            }
        }
    }
}
