package com.sophiemarceauqu.lib_audio.mediaplayer.view.adapter;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import com.sophiemarceauqu.lib_audio.R;
import com.sophiemarceauqu.lib_audio.mediaplayer.core.AudioController;
import com.sophiemarceauqu.lib_audio.mediaplayer.model.AudioBean;
import com.sophiemarceauqu.lib_image_loader.app.ImageLoaderManager;

import androidx.viewpager.widget.PagerAdapter;

import java.util.ArrayList;


/**
 * 播放页面ViewPager Adapter
 */
public class MusicPagerAdapter extends PagerAdapter {
    private Context mContext;
    //data
    private ArrayList<AudioBean> mAudioBeans;
    private SparseArray<ObjectAnimator> mAnims = new SparseArray<>();
    private Callback mCallback;

    public MusicPagerAdapter(ArrayList<AudioBean> audioBeans,
                             Context context, Callback callback) {
        mAudioBeans = audioBeans;
        mContext = context;
        mCallback = callback;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View rootView = LayoutInflater.from(mContext)
                .inflate(R.layout.indictor_item_view, null);
        ImageView imageView = rootView.findViewById(R.id.circle_view);
        container.addView(rootView);
        ImageLoaderManager.getInstance().displayImageForCircle(imageView, mAudioBeans.get(position).albumPic);
        //只在无动画时创建
        mAnims.put(position, createAnim(rootView));//将动画缓存起来
        return rootView;
    }

    @Override
    public int getCount() {
        return mAudioBeans == null ? 0 : mAudioBeans.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    private ObjectAnimator createAnim(View view) {
        view.setRotation(0);
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, View.ROTATION.getName(), 0, 360);
        animator.setDuration(10000);
        animator.setInterpolator(new LinearInterpolator());
        animator.setRepeatCount(-1);
        if (AudioController.getInstance().isStartState()) {
            animator.start();
        }
        return animator;
    }

    public ObjectAnimator getAnim(int pos) {
        return mAnims.get(pos);
    }

    /**
     * 与IndictorView
     */
    public interface Callback {
        void onPlayStatus();

        void onPausestatus();
    }
}
