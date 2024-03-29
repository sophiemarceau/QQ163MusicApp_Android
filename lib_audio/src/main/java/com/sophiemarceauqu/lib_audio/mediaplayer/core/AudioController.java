package com.sophiemarceauqu.lib_audio.mediaplayer.core;

import com.sophiemarceauqu.lib_audio.mediaplayer.db.GreenDaoHelper;
import com.sophiemarceauqu.lib_audio.mediaplayer.events.AudioCompleteEvent;
import com.sophiemarceauqu.lib_audio.mediaplayer.events.AudioErrorEvent;
import com.sophiemarceauqu.lib_audio.mediaplayer.events.AudioFavouriteEvent;
import com.sophiemarceauqu.lib_audio.mediaplayer.events.AudioPlayModeEvent;
import com.sophiemarceauqu.lib_audio.mediaplayer.exception.AudioQueueEmptyException;
import com.sophiemarceauqu.lib_audio.mediaplayer.model.AudioBean;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Random;

/**
 *控制播放逻辑类，注意添加一个控制方法时，要考虑是否需要添加event，来更新UI
 */
public class AudioController {
    /**
     * 播放方式
     */
    public enum PlayMode {
        //列表循环，随机，单曲循环
        LOOP, RANDOM, REPEAT
    }

    private AudioPlayer mAudioPlayer;//核心播放器
    //播放队列，不能为空，不设置主动抛错
    private ArrayList<AudioBean> mQueue = new ArrayList<>();//歌曲队列
    private PlayMode mPlayMode = PlayMode.LOOP;//当前播放歌曲索引
    private int mQueueIndex = 0;//循环模式

    //单例方法
    private static class SingletonHolder {
        private static AudioController instance = new AudioController();
    }

    public static AudioController getInstance() {
        return AudioController.SingletonHolder.instance;
    }

    private AudioController() {
        EventBus.getDefault().register(this);
        mAudioPlayer = new AudioPlayer();
    }

    private void addCustomAudio(int index, AudioBean bean) {
        if (mQueue == null) {
            throw new AudioQueueEmptyException("当前播放队列为空，请先设置播放队列.");
        }
        mQueue.add(index, bean);
    }

    private int queryAudio(AudioBean bean) {
        return mQueue.indexOf(bean);
    }

    private void load(AudioBean bean) {
        mAudioPlayer.load(bean);
    }

    /**
     * 获取播放器当前状态
     *
     * @return
     */
    private CustomMediaPlayer.Status getStatus() {
        return mAudioPlayer.getStatus();
    }

    private AudioBean getNextPlaying() {
        switch (mPlayMode) {
            case LOOP:
                mQueueIndex = (mQueueIndex + 1) % mQueue.size();
                return getPlaying(mQueueIndex);
            case RANDOM:
                mQueueIndex = new Random().nextInt(mQueue.size()) % mQueue.size();
                return getPlaying(mQueueIndex);
            case REPEAT:
                return getPlaying(mQueueIndex);
        }
        return null;
    }

    private AudioBean getPreviousPlaying() {
        switch (mPlayMode) {
            case LOOP:
                mQueueIndex = (mQueueIndex + mQueue.size() - 1) % mQueue.size();
                return getPlaying(mQueueIndex);
            case RANDOM:
                mQueueIndex = new Random().nextInt(mQueue.size()) % mQueue.size();
                return getPlaying(mQueueIndex);
            case REPEAT:
                return getPlaying(mQueueIndex);
        }
        return null;
    }

    private AudioBean getPlaying(int index) {
        if (mQueue != null && !mQueue.isEmpty() && index >= 0 && index < mQueue.size()) {
            return mQueue.get(index);
        } else {
            throw new AudioQueueEmptyException("当前播放队列为空，请先设置播放队列。");
        }
    }

    /**
     * 对外提供是否播放中状态
     *
     * @return
     */
    public boolean isStartState() {
        return CustomMediaPlayer.Status.STATED == getStatus();
    }

    /**
     * 对外提供是否暂停状态
     *
     * @return
     */
    public boolean isPauseStatue() {
        return CustomMediaPlayer.Status.PAUSED == getStatus();
    }

    public ArrayList<AudioBean> getQueue() {
        return mQueue == null ? new ArrayList<AudioBean>() : mQueue;
    }

    /**
     * 设置播放队列
     *
     * @param queue
     */
    public void setQueue(ArrayList<AudioBean> queue) {
        this.setQueue(queue, 0);
    }

    public void setQueue(ArrayList<AudioBean> queue, int queueIndex) {
        mQueue.addAll(queue);
        mQueueIndex = queueIndex;
    }

    /**
     * 队列头添加播放歌曲
     */
    public void addAudio(AudioBean bean) {
        this.addAudio(0, bean);
    }

    public void addAudio(int index, AudioBean bean) {
        if (mQueue == null) {
            throw new AudioQueueEmptyException("当前播放队列为空，请先设置播放队列。");
        }
        int query = queryAudio(bean);
        if (query <= -1) {
            //没添加过此id的歌曲，添加且直接播放
            addCustomAudio(index, bean);
            setPlayIndex(index);
        } else {
            AudioBean currentBean = getNowPlaying();
            if (!currentBean.id.equals(bean.id)) {
                //添加过且不是当前播放，否则什么也不干
                setPlayIndex(query);
            }
        }
    }

    public void setPlayIndex(int index) {
        if (mQueue == null) {
            throw new AudioQueueEmptyException("当前播放队列为空，请先设置队列！");
        }
        mQueueIndex = index;
        play();
    }

    public PlayMode getPlayMode() {
        return mPlayMode;
    }

    /**
     * 对外提供设置播放模式
     *
     * @param playMode
     */
    public void setPlayMode(PlayMode playMode) {
        mPlayMode = playMode;
        //还要对外发送切换事件，更新UI
        EventBus.getDefault().post(new AudioPlayModeEvent(mPlayMode));
    }

    public int getQueueIndex() {
        return mQueueIndex;
    }

    /**
     * 添加/移除到收藏
     */
    public void changeFavourite() {
        if (null!= GreenDaoHelper.selectFavourite(getNowPlaying())){
            //已收藏，移除
            GreenDaoHelper.removeFavourite(getNowPlaying());
            EventBus.getDefault().post(new AudioFavouriteEvent(false));
        }else{
            //未收藏，添加收藏
            GreenDaoHelper.addFavourite(getNowPlaying());
            EventBus.getDefault().post(new AudioFavouriteEvent(true));
        }
    }

    /**
     * 播放/暂停切换
     */
    public void playOrPause() {
        if (isStartState()) {
            pause();
        } else if (isPauseStatue()) {
            resume();
        }
    }

    /**
     * 加载当前index歌曲
     */
    public void play() {
        AudioBean bean = getPlaying(mQueueIndex);
        load(bean);
    }

    /**
     * 加载next index歌曲
     */
    public void next() {
        AudioBean bean = getNextPlaying();
        load(bean);
    }

    /**
     * 加载previous index歌曲
     */
    public void previous() {
        AudioBean bean = getPreviousPlaying();
        load(bean);
    }

    /**
     * 对外提供获取当前播放时间
     */
    public int getNowPlayTime() {
        return mAudioPlayer.getCurrentPosition();
    }

    /**
     * 对外提供获取总播放时间
     */
    public int getTotalPlayTime() {
        return mAudioPlayer.getCurrentPosition();
    }

    /**
     * 对外提供的获取当前歌曲信息
     *
     * @return
     */
    public AudioBean getNowPlaying() {
        return getPlaying(mQueueIndex);
    }

    public void resume() {
        mAudioPlayer.resume();
    }

    public void pause() {
        mAudioPlayer.pause();
    }

    public void release() {
        mAudioPlayer.release();
        EventBus.getDefault().unregister(this);
    }

    //播放完毕时间处理
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioCompleteEvent(AudioCompleteEvent event) {
        next();
    }

    //播放出错事件处理
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioErrorEvent(AudioErrorEvent event) {
        next();
    }

}
