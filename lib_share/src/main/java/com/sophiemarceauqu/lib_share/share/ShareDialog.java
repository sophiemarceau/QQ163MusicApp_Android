package com.sophiemarceauqu.lib_share.share;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.sophiemarceauqu.lib_share.R;

import java.util.HashMap;

import cn.sharesdk.framework.Platform;

public class ShareDialog extends Dialog {
    private Context mContext;
    private DisplayMetrics dm;

    //UI
    private RelativeLayout mWeixinLayout;
    private RelativeLayout mWeixinMomentLayout;
    private RelativeLayout mQQLayout;
    private RelativeLayout mQZoneLayout;

    /**
     * share relative
     */
    private int mShareType;//指定分享类型
    private String mShareTitle;//指定分享内容标题
    private String mShareText;//指定分享内容文本
    private String mSharePhoto; //指定分享本地图片
    private String mShareTitleUrl;
    private String mShareSiteUrl;
    private String mShareSite;
    private String mUrl;
    private String mResourceUrl;//构建模式的实体类来去优化封装

    private boolean isShowDownload;

    public ShareDialog(Context context, boolean isShowDownload) {
        super(context, R.style.SheetDialogStyle);
        mContext = context;
        dm = mContext.getResources().getDisplayMetrics();
        this.isShowDownload = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_share_layout);
        initView();
    }

    private void initView() {
        /**
         * 通过获取到dialog的window来控制dialog的宽高及位置
         */
        Window dialogWindow = getWindow();
        dialogWindow.setGravity(Gravity.BOTTOM);
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.width = dm.widthPixels;//设置宽度
        dialogWindow.setAttributes(lp);
        dialogWindow.setBackgroundDrawableResource(android.R.color.transparent);

        mWeixinLayout = findViewById(R.id.weixin_layout);
        mWeixinLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareData(ShareManager.PlatformType.WeChat);
            }
        });
        mWeixinMomentLayout = findViewById(R.id.moment_layout);
        mWeixinMomentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareData(ShareManager.PlatformType.WechatMoments);
            }
        });
        mQQLayout = findViewById(R.id.qq_layout);
        mQQLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareData(ShareManager.PlatformType.QQ);
            }
        });
        mQZoneLayout = findViewById(R.id.qzone_layout);
        mQZoneLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareData(ShareManager.PlatformType.QZone);
            }
        });
    }

    public void setResourceUrl(String resourceUrl) {
        mResourceUrl = resourceUrl;
    }

    public void setShareTitle(String title) {
        mShareTitle = title;
    }

    public void setImagePhoto(String photo) {
        mSharePhoto = photo;
    }

    public void setShareType(int type) {
        mShareType = type;
    }

    public void setShareSite(String site) {
        mShareSite = site;
    }

    public void setShareTitleUrl(String titleUrl) {
        mShareTitleUrl = titleUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    public void setShareSiteUrl(String siteUrl) {
        mShareSiteUrl = siteUrl;
    }

    public void setShareText(String text) {
        mShareText = text;
    }

    private ShareManager.PlatformShareListener mListener = new ShareManager.PlatformShareListener() {
        @Override
        public void onComplete(int var2, HashMap<String, Object> var3) {

        }

        @Override
        public void onError(int var2, Throwable var3) {

        }

        @Override
        public void onCancel(int var2) {

        }
    };

    private void shareData(ShareManager.PlatformType platformType) {
        ShareData mData = new ShareData();
        Platform.ShareParams params = new Platform.ShareParams();
        params.setShareType(mShareType);
        params.setTitle(mShareTitle);
        params.setTitleUrl(mShareTitleUrl);
        params.setSite(mShareSite);
        params.setSiteUrl(mShareSiteUrl);
        params.setText(mShareText);
        params.setImagePath(mSharePhoto);
        params.setUrl(mUrl);
        mData.mPlatformType = platformType;
        mData.mShareParams = params;
        ShareManager.getInstance().shareData(mData, mListener);
    }
}
