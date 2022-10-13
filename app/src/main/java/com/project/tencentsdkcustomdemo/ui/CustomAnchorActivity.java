package com.project.tencentsdkcustomdemo.ui;

import android.app.Activity;
import android.content.Intent;

import android.os.Bundle;
import android.view.TextureView;
import android.widget.Button;

import com.project.tencentsdkcustomdemo.R;
import com.project.tencentsdkcustomdemo.constants.Constant;
import com.project.tencentsdkcustomdemo.constants.GenerateTestUserSig;
import com.project.tencentsdkcustomdemo.media.audio.RecordConfig;
import com.project.tencentsdkcustomdemo.media.audio.RecordHelper;
import com.project.tencentsdkcustomdemo.render.TRTCRenderVideoFrame;
import com.project.tencentsdkcustomdemo.utils.SPUtils;
import com.tencent.rtmp.ui.TXCloudVideoView;
import com.tencent.trtc.TRTCCloud;
import com.tencent.trtc.TRTCCloudDef;
import com.trtc.customcamera.capture.CameraBuilder;
import com.trtc.customcamera.capture.CameraCapture;

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
    private TXCloudVideoView mLocalPreviewView;
    private Button mLiveBtnSwitchCamera;
    private CameraCapture mCustomCameraCapture;
    private int cameraID = 0;

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
        mLocalPreviewView = findViewById(R.id.trtc_tc_cloud_view_main);
        mLiveBtnSwitchCamera = findViewById(R.id.live_btn_switch_camera);
        mLiveBtnSwitchCamera.setOnClickListener(view -> {
            if (cameraID == 0) {
                cameraID = 1;
                mLiveBtnSwitchCamera.setBackgroundResource(R.mipmap.live_camera_back);
            } else {
                cameraID = 0;
                mLiveBtnSwitchCamera.setBackgroundResource(R.mipmap.live_camera_front);
            }
            mCustomCameraCapture.changeCameraID(cameraID);
        });
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
       /* // 开启本地声音采集并上行
        mTRTCCloud.startLocalAudio();*/
        //设置16k采样率
        mTRTCCloud.setAudioQuality(TRTCCloudDef.TRTC_AUDIO_QUALITY_SPEECH);
        mTRTCCloud.enableCustomAudioCapture(true);
        mTRTCCloud.enableCustomVideoCapture(TRTCCloudDef.TRTC_VIDEO_STREAM_TYPE_BIG, true);

        TRTCCloudDef.TRTCVideoEncParam encParam = new TRTCCloudDef.TRTCVideoEncParam();
        encParam.videoResolution = TRTCCloudDef.TRTC_VIDEO_RESOLUTION_1280_720;
        encParam.videoFps = 15;
        encParam.videoBitrate = 2000;
        encParam.videoResolutionMode = TRTCCloudDef.TRTC_VIDEO_RESOLUTION_MODE_PORTRAIT;
        mTRTCCloud.setVideoEncoderParam(encParam);
        mTRTCCloud.enterRoom(mTRTCParams, TRTC_APP_SCENE_LIVE);
        customVideo();
        customVoice();
    }

    /*
    视频自定义采集
     */
    private void customVideo() {
        int cameraType = SPUtils.getInstance(this).getInt(Constant.CAMERA_TYPE, Constant.CAMERA_1);
        mCustomCameraCapture = new CameraBuilder(this).setCameraID(cameraID).setCameraType(cameraType == Constant.CAMERA_1 ? CameraBuilder.CameraType.Camera1 : CameraBuilder.CameraType.Camera2).build();
        mCustomCameraCapture.startCameraCapture((eglContext, textureId, width, height) -> {
            TRTCCloudDef.TRTCVideoFrame videoFrame = new TRTCCloudDef.TRTCVideoFrame();
            videoFrame.texture = new TRTCCloudDef.TRTCTexture();
            videoFrame.texture.textureId = textureId;
            videoFrame.texture.eglContext14 = eglContext;
            videoFrame.width = width;
            videoFrame.height = height;
            videoFrame.pixelFormat = TRTCCloudDef.TRTC_VIDEO_PIXEL_FORMAT_Texture_2D;
            videoFrame.bufferType = TRTCCloudDef.TRTC_VIDEO_BUFFER_TYPE_TEXTURE;
            if (mTRTCCloud != null)
                mTRTCCloud.sendCustomVideoData(TRTCCloudDef.TRTC_VIDEO_STREAM_TYPE_BIG, videoFrame);
        });

        //自定义采集+SDK美颜测试
        TRTCRenderVideoFrame mCustomRender = new TRTCRenderVideoFrame(mUserId, TRTCCloudDef.TRTC_VIDEO_STREAM_TYPE_BIG);
        mTRTCCloud.setLocalVideoRenderListener(TRTCCloudDef.TRTC_VIDEO_PIXEL_FORMAT_Texture_2D, TRTCCloudDef.TRTC_VIDEO_BUFFER_TYPE_TEXTURE, mCustomRender);
        TextureView textureView = new TextureView(this);
        mLocalPreviewView.addVideoView(textureView);
        mCustomRender.start(textureView);
    }

    /**
     * 音频自定义采集
     */
    private void customVoice() {
        RecordConfig config = new RecordConfig();
        RecordHelper.getInstance().setRecordDataListener(data -> {
            //给队列添加PCM音频数据包

            TRTCCloudDef.TRTCAudioFrame trtcAudioFrame = new TRTCCloudDef.TRTCAudioFrame();
            //音频数据
            trtcAudioFrame.data = data;
            //声道数
            trtcAudioFrame.channel = RecordHelper.getInstance().getCurrentConfig().getChannelCount();
            //采样率
            trtcAudioFrame.sampleRate = RecordHelper.getInstance().getCurrentConfig().getSampleRate();
            if (mTRTCCloud != null) {

                mTRTCCloud.sendCustomAudioData(trtcAudioFrame);
            }
        });
        //开始录音
        RecordHelper.getInstance().start(config);
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
        mCustomCameraCapture.destroy();
    }

    public void exitRoom() {
        mTRTCCloud.stopLocalAudio();
        mTRTCCloud.stopLocalPreview();
        mTRTCCloud.exitRoom();
        //cameraEglSurfaceView.onDestroy();
    }


}
