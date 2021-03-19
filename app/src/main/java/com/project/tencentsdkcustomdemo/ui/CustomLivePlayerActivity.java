package com.project.tencentsdkcustomdemo.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.TextureView;

import androidx.appcompat.widget.Toolbar;

import com.project.tencentsdkcustomdemo.R;
import com.project.tencentsdkcustomdemo.render.LiveRenderVideoFrame;
import com.tencent.rtmp.TXLiveConstants;
import com.tencent.rtmp.TXLivePlayer;
import com.tencent.rtmp.ui.TXCloudVideoView;

public class CustomLivePlayerActivity extends BaseActivity {
    private TXCloudVideoView mTXCloudVideoView;
    private TXLivePlayer mTXLivePlayer;
    private String playerUrl = "http://liteavapp.qcloud.com/live/liteavdemoplayerstreamid.flv";
    private LiveRenderVideoFrame liveRenderVideoFrame;

    public static void openActivity(Activity activity) {
        Intent intent = new Intent(activity, CustomLivePlayerActivity.class);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_live_player);
        Toolbar mToolbar = findViewById(R.id.toolbar);
        bindToolbarWithBack(mToolbar);
        mTXCloudVideoView = findViewById(R.id.trtc_tc_cloud_view_main);
        mTXLivePlayer = new TXLivePlayer(this);
        mTXLivePlayer.setRenderMode(TXLiveConstants.RENDER_ROTATION_LANDSCAPE);
        mTXLivePlayer.startPlay(playerUrl, TXLivePlayer.PLAY_TYPE_LIVE_FLV);
        liveRenderVideoFrame = new LiveRenderVideoFrame();
        mTXLivePlayer.setVideoRenderListener(liveRenderVideoFrame, null);
        TextureView textureView = new TextureView(this);
        mTXCloudVideoView.addVideoView(textureView);
        liveRenderVideoFrame.start(textureView);

    }

    @Override
    protected void onDestroy() {
        if (mTXLivePlayer != null) {
            mTXLivePlayer.stopPlay(false);
        }
        if (liveRenderVideoFrame != null) {
            liveRenderVideoFrame.stop();
        }
        super.onDestroy();

    }
}
