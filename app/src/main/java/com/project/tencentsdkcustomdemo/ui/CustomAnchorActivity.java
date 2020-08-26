package com.project.tencentsdkcustomdemo.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.TextureView;
import android.widget.Button;

import com.project.tencentsdkcustomdemo.R;
import com.project.tencentsdkcustomdemo.constants.Constant;
import com.project.tencentsdkcustomdemo.constants.GenerateTestUserSig;
import com.project.tencentsdkcustomdemo.media.camera.CameraBuilder;
import com.project.tencentsdkcustomdemo.media.egl.CameraEglSurfaceView;
import com.project.tencentsdkcustomdemo.render.TestRenderVideoFrame;
import com.project.tencentsdkcustomdemo.utils.SPUtils;
import com.tencent.liteav.beauty.TXBeautyManager;
import com.tencent.rtmp.ui.TXCloudVideoView;
import com.tencent.trtc.TRTCCloud;
import com.tencent.trtc.TRTCCloudDef;

import androidx.appcompat.widget.Toolbar;

import static com.tencent.trtc.TRTCCloudDef.TRTCRoleAnchor;
import static com.tencent.trtc.TRTCCloudDef.TRTC_APP_SCENE_LIVE;

/**
 * 主播端自定义采集demo
 */
public class CustomAnchorActivity extends BaseActivity {
    //SDK 核心类
    private TRTCCloud mTRTCCloud;
    //用户类型
    private int mRoleType;
    // 房间Id
    private String mRoomId;
    // 用户Id
    private String mUserId;

    protected LiveRoomManager mLiveRoomManager;
    private CameraEglSurfaceView cameraEglSurfaceView;
    private TXCloudVideoView mLocalPreviewView;
    private Button mLiveBtnSwitchCamera;

    public static void openActivity(Activity activity, String roomId) {
        Intent intent = new Intent(activity, CustomAnchorActivity.class);
        intent.putExtra(Constant.ROOM_ID, String.valueOf(roomId));
        intent.putExtra(Constant.USER_ID, String.valueOf(roomId));
        intent.putExtra(Constant.ROLE_TYPE, TRTCRoleAnchor);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_anchor);
        initData();
    }

    private void initData() {
        mTRTCCloud = TRTCCloud.sharedInstance(getApplicationContext());
        mLiveRoomManager = new LiveRoomManager();
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
        initView();
    }

    private void initView() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("自定义采集" + mRoomId);
        bindToolbarWithBack(toolbar);
        cameraEglSurfaceView = findViewById(R.id.CameraEglSurfaceView);
        mLocalPreviewView = findViewById(R.id.trtc_tc_cloud_view_main);
        mLiveBtnSwitchCamera = findViewById(R.id.live_btn_switch_camera);
        mLiveBtnSwitchCamera.setOnClickListener(view -> {
            cameraEglSurfaceView.switchCamera();
            if (CameraBuilder.CAMERA_ID_BACK.equals(cameraEglSurfaceView.getSpecificCameraId())) {
                mLiveBtnSwitchCamera.setBackgroundResource(R.mipmap.live_camera_back);
            } else {
                mLiveBtnSwitchCamera.setBackgroundResource(R.mipmap.live_camera_front);
            }
        });
        int cameraType = SPUtils.getInstance(this).getInt(Constant.CAMERA_TYPE, Constant.CAMERA_1);
        cameraEglSurfaceView.changeCameraInterface(cameraType == Constant.CAMERA_1 ? CameraBuilder.CameraType.Camera1 : CameraBuilder.CameraType.Camera2);
        enterRoom();


    }

    public void enterRoom() {

        // SDK 参数
        TRTCCloudDef.TRTCParams mTRTCParams = new TRTCCloudDef.TRTCParams();
        mTRTCParams.sdkAppId = GenerateTestUserSig.SDKAPPID;
        mTRTCParams.userId = mUserId;
        mTRTCParams.roomId = Integer.parseInt(mRoomId);
        mTRTCParams.role = mRoleType;
        /// userSig是进入房间的用户签名，相当于密码（这里生成的是测试签名，正确做法需要业务服务器来生成，然后下发给客户端）
        mTRTCParams.userSig = GenerateTestUserSig.genTestUserSig(mTRTCParams.userId);
        // 开启本地声音采集并上行
        mTRTCCloud.startLocalAudio();
        mTRTCCloud.enableCustomVideoCapture(true);
        //美颜测试
        TXBeautyManager beautyManager = mTRTCCloud.getBeautyManager();
        beautyManager.setBeautyStyle(0);
        beautyManager.setBeautyLevel(9);
        beautyManager.setWhitenessLevel(9);
        beautyManager.setFilter(BitmapFactory.decodeResource(getResources(), R.drawable.beauty_filter_yuanqi));
        beautyManager.setFilterStrength(1);

        TRTCCloudDef.TRTCVideoEncParam encParam = new TRTCCloudDef.TRTCVideoEncParam();
        encParam.videoResolution = TRTCCloudDef.TRTC_VIDEO_RESOLUTION_1280_720;
        encParam.videoFps = 15;
        encParam.videoBitrate = 2000;
        encParam.videoResolutionMode = TRTCCloudDef.TRTC_VIDEO_RESOLUTION_MODE_PORTRAIT;
        mTRTCCloud.setVideoEncoderParam(encParam);

        mTRTCCloud.enterRoom(mTRTCParams, TRTC_APP_SCENE_LIVE);
        cameraEglSurfaceView.setVideoFrameReadListener(frame -> {
            TRTCCloudDef.TRTCVideoFrame videoFrame = new TRTCCloudDef.TRTCVideoFrame();
            videoFrame.texture = new TRTCCloudDef.TRTCTexture();
            videoFrame.texture.textureId = frame.textureId;
            videoFrame.texture.eglContext10 = frame.eglContext;
            videoFrame.width = frame.width;
            videoFrame.height = frame.height;
            videoFrame.pixelFormat = TRTCCloudDef.TRTC_VIDEO_PIXEL_FORMAT_Texture_2D;
            videoFrame.bufferType = TRTCCloudDef.TRTC_VIDEO_BUFFER_TYPE_TEXTURE;
            mTRTCCloud.sendCustomVideoData(videoFrame);

        });
        //自定义采集+SDK美颜测试
        TestRenderVideoFrame mCustomRender = new TestRenderVideoFrame(mUserId, TRTCCloudDef.TRTC_VIDEO_STREAM_TYPE_BIG);
        mTRTCCloud.setLocalVideoRenderListener(TRTCCloudDef.TRTC_VIDEO_PIXEL_FORMAT_Texture_2D, TRTCCloudDef.TRTC_VIDEO_BUFFER_TYPE_TEXTURE, mCustomRender);
        TextureView textureView = new TextureView(this);
        mLocalPreviewView.addVideoView(textureView);
        mCustomRender.start(textureView);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        exitRoom();
        mLiveRoomManager.destoryLiveRoom(mRoomId);
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
        cameraEglSurfaceView.onDestroy();
    }


}
