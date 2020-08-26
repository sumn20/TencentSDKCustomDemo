package com.project.tencentsdkcustomdemo.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.widget.Toast;

import com.project.tencentsdkcustomdemo.R;
import com.project.tencentsdkcustomdemo.constants.Constant;
import com.project.tencentsdkcustomdemo.constants.GenerateTestUserSig;
import com.project.tencentsdkcustomdemo.render.TestRenderVideoFrame;
import com.tencent.rtmp.ui.TXCloudVideoView;
import com.tencent.trtc.TRTCCloud;
import com.tencent.trtc.TRTCCloudDef;
import com.tencent.trtc.TRTCCloudListener;

import androidx.appcompat.widget.Toolbar;

import static com.tencent.trtc.TRTCCloudDef.TRTCRoleAudience;
import static com.tencent.trtc.TRTCCloudDef.TRTC_APP_SCENE_LIVE;

/**
 * 观众端自定义渲染demo
 */
public class CustomAudienceActivity extends BaseActivity {

    private TXCloudVideoView mRemotePreviewView;
    //SDK 核心类
    private TRTCCloud mTRTCCloud;
    //用户类型
    private int mRoleType;
    // 房间Id
    private String mRoomId;
    // 用户Id
    private String mUserId;
    private TestRenderVideoFrame mCustomRender;

    public static void openActivity(Activity activity, String roomId) {
        Intent intent = new Intent(activity, CustomAudienceActivity.class);
        intent.putExtra(Constant.ROOM_ID, String.valueOf(roomId));
        intent.putExtra(Constant.USER_ID, "user_" + System.currentTimeMillis());
        intent.putExtra(Constant.ROLE_TYPE, TRTCRoleAudience);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_audience);
        Toolbar mToolbar = findViewById(R.id.toolbar);
        mRemotePreviewView = findViewById(R.id.trtc_tc_cloud_view_main);

        bindToolbarWithBack(mToolbar);
        initData();

    }

    private void initData() {
        mTRTCCloud = TRTCCloud.sharedInstance(this);

        initListener();
        Intent intent = getIntent();
        if (null != intent) {
            if (intent.getStringExtra(Constant.USER_ID) != null) {
                mUserId = intent.getStringExtra(Constant.USER_ID);
            }
            if (intent.getStringExtra(Constant.ROOM_ID) != null) {
                mRoomId = intent.getStringExtra(Constant.ROOM_ID);
            }
            if (intent.getIntExtra(Constant.ROLE_TYPE, 0) != 0) {
                mRoleType = intent.getIntExtra(Constant.ROLE_TYPE, 0);
            }
        }
        mCustomRender = new TestRenderVideoFrame(mRoomId, TRTCCloudDef.TRTC_VIDEO_STREAM_TYPE_BIG);

        // SDK 参数
        TRTCCloudDef.TRTCParams mTRTCParams = new TRTCCloudDef.TRTCParams();
        mTRTCParams.sdkAppId = GenerateTestUserSig.SDKAPPID;
        mTRTCParams.userId = mUserId;
        mTRTCParams.roomId = Integer.parseInt(mRoomId);
        /// userSig是进入房间的用户签名，相当于密码（这里生成的是测试签名，正确做法需要业务服务器来生成，然后下发给客户端）
        mTRTCParams.userSig = GenerateTestUserSig.genTestUserSig(mTRTCParams.userId);
        mTRTCParams.role = mRoleType;
        // 进入直播间
        mTRTCCloud.enterRoom(mTRTCParams, TRTC_APP_SCENE_LIVE);
    }

    private void initListener() {
        mTRTCCloud.setListener(new TRTCCloudListener() {
            @Override
            public void onUserVideoAvailable(String userId, boolean available) {
                if (available) {
                    if (mRoomId.equals(userId)) {
                        mTRTCCloud.startRemoteView(userId, null);
                        mTRTCCloud.setRemoteVideoRenderListener(userId, TRTCCloudDef.TRTC_VIDEO_PIXEL_FORMAT_Texture_2D, TRTCCloudDef.TRTC_VIDEO_BUFFER_TYPE_TEXTURE, mCustomRender);
                        TextureView textureView = new TextureView(CustomAudienceActivity.this);
                        mRemotePreviewView.addVideoView(textureView);
                        mCustomRender.start(textureView);
                    } else {
                        if (mCustomRender != null) {
                            mCustomRender.stop();
                        }
                    }
                } else {
                    if (mRoomId.equals(userId)) {
                        Toast.makeText(CustomAudienceActivity.this, "主播已经离开", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        exitRoom();
        //销毁 trtc 实例
        if (mTRTCCloud != null) {
            mTRTCCloud.setListener(null);
        }
        mTRTCCloud = null;
        TRTCCloud.destroySharedInstance();
    }


    public void exitRoom() {
        mTRTCCloud.stopLocalAudio();
        mTRTCCloud.stopLocalPreview();
        mTRTCCloud.exitRoom();
        if (mCustomRender != null) {
            mCustomRender.stop();
        }
    }
}
