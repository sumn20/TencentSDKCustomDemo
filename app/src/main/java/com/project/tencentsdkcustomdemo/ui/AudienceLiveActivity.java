package com.project.tencentsdkcustomdemo.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.project.tencentsdkcustomdemo.R;
import com.project.tencentsdkcustomdemo.constants.Constant;
import com.project.tencentsdkcustomdemo.constants.GenerateTestUserSig;
import com.tencent.liteav.TXLiteAVCode;
import com.tencent.rtmp.ui.TXCloudVideoView;
import com.tencent.trtc.TRTCCloud;
import com.tencent.trtc.TRTCCloudDef;
import com.tencent.trtc.TRTCCloudListener;
import com.tencent.trtc.TRTCStatistics;

import java.util.ArrayList;

import androidx.appcompat.widget.Toolbar;

import static com.tencent.trtc.TRTCCloudDef.TRTCRoleAnchor;
import static com.tencent.trtc.TRTCCloudDef.TRTCRoleAudience;
import static com.tencent.trtc.TRTCCloudDef.TRTC_APP_SCENE_LIVE;

public class AudienceLiveActivity extends BaseActivity {
    private static final String TAG = AudienceLiveActivity.class.getSimpleName();
    private TXCloudVideoView mTrtcVideoView;
    //SDK 核心类
    private TRTCCloud mTRTCCloud;
    //用户类型
    private int mRoleType;
    // 房间Id
    private String mRoomId;
    // 用户Id
    private String mUserId;

    public static void openActivity(Activity activity, String roomId) {
        Intent intent = new Intent(activity, AudienceLiveActivity.class);
        intent.putExtra(Constant.ROOM_ID, String.valueOf(roomId));
        intent.putExtra(Constant.USER_ID, "user_" + System.currentTimeMillis());
        intent.putExtra(Constant.ROLE_TYPE, TRTCRoleAudience);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audience_live);
        Toolbar mToolbar = findViewById(R.id.toolbar);
        mTrtcVideoView = findViewById(R.id.trtc_video_view);
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
            /**
             * 错误回调：SDK 不可恢复的错误，一定要监听，并分情况给用户适当的界面提示。
             *
             * @param errCode   错误码
             * @param errMsg    错误信息
             * @param extraInfo 扩展信息字段，个别错误码可能会带额外的信息帮助定位问题
             */
            @Override
            public void onError(int errCode, String errMsg, Bundle extraInfo) {
                Log.d(TAG, "onError errCode:" + errCode + "errMsg:" + errMsg);

                Toast.makeText(AudienceLiveActivity.this, "onError: " + errMsg + "[" + errCode + "]", Toast.LENGTH_SHORT).show();
                if (errCode == TXLiteAVCode.ERR_ROOM_ENTER_FAIL) {
                    exitRoom();
                }

            }

            /**
             * 警告回调：用于告知您一些非严重性问题，例如出现卡顿或者可恢复的解码失败。
             *
             * @param warningCode 错误码
             * @param warningMsg
             * @param bundle
             */
            @Override
            public void onWarning(int warningCode, String warningMsg, Bundle bundle) {
                Log.d(TAG, "onWarning warningCode:" + warningCode + "warningMsg:" + warningMsg);
            }

            /**
             * 已加入房间的回调
             *
             * @param result result > 0 时为进房耗时（ms），result < 0 时为进房错误码。
             */
            @Override
            public void onEnterRoom(long result) {
                Log.d(TAG, "onEnterRoom 耗时:" + result);

            }

            /**
             * 离开房间的事件回调
             *
             * @param reason 离开房间原因，0：主动调用 exitRoom 退房；1：被服务器踢出当前房间；2：当前房间整个被解散。
             */
            @Override
            public void onExitRoom(int reason) {
                Log.d(TAG, "onExitRoom 离开原因：" + reason);
            }

            /**
             * 切换角色的事件回调
             * 调用 TRTCCloud 中的 switchRole() 接口会切换主播和观众的角色，该操作会伴随一个线路切换的过程， 待 SDK 切换完成后，会抛出 onSwitchRole() 事件回调。
             *
             * @param errCode
             * @param errMsg
             */
            @Override
            public void onSwitchRole(int errCode, String errMsg) {
                Log.d(TAG, "onSwitchRole");

            }

            /**
             * 请求跨房通话（主播 PK）的结果回调
             *
             * @param userId  要 PK 的目标主播 userid。
             * @param errCode 错误码，ERR_NULL 代表切换成功，其他请参见错误码。
             * @param errMsg  错误信息。
             */

            @Override
            public void onConnectOtherRoom(String userId, int errCode, String errMsg) {
                Log.d(TAG, "onConnectOtherRoom");

            }

            /**
             * 结束跨房通话（主播 PK）的结果回调
             *
             * @param errCode 错误码
             * @param errMsg  错误信息
             */

            @Override
            public void onDisConnectOtherRoom(int errCode, String errMsg) {
                Log.d(TAG, "onConnectOtherRoom");
            }

            /**
             * 有用户加入当前房间
             * <p>
             * 出于性能方面的考虑，在两种不同的应用场景下，该通知的行为会有差别：
             * <p>
             * 通话场景（TRTCCloudDef.TRTC_APP_SCENE_VIDEOCALL 和 TRTCCloudDef.TRTC_APP_SCENE_AUDIOCALL）：该场景下用户没有角色的区别，任何用户进入房间都会触发该通知。
             * 直播场景（TRTCCloudDef.TRTC_APP_SCENE_LIVE 和 TRTCCloudDef.TRTC_APP_SCENE_VOICE_CHATROOM）：该场景不限制观众的数量，如果任何用户进出都抛出回调会引起很大的性能损耗，所以该场景下只有主播进入房间时才会触发该通知，观众进入房间不会触发该通知
             * 注意 onRemoteUserEnterRoom 和 onRemoteUserLeaveRoom 只适用于维护当前房间里的“成员列表”，如果需要显示远程画面，建议使用监听 onUserVideoAvailable() 事件回调。
             *
             * @param userId 用户标识
             */
            @Override
            public void onRemoteUserEnterRoom(String userId) {
                Log.d(TAG, "onRemoteUserEnterRoom");

            }

            /**
             * 用户是否开启摄像头视频
             * 当您收到 onUserVideoAvailable(userId, true) 通知时，表示该路画面已经有可用的视频数据帧到达。 此时，您需要调用 startRemoteView(userid) 接口加载该用户的远程画面。 然后，您会收到名为 onFirstVideoFrame(userid) 的首帧画面渲染回调。
             * <p>
             * 当您收到 onUserVideoAvailable(userId, false) 通知时，表示该路远程画面已经被关闭，可能由于该用户调用了 muteLocalVideo() 或 stopLocalPreview()。
             *
             * @param userId    用户标识
             * @param available 画面是否开启
             */

            @Override
            public void onUserVideoAvailable(String userId, boolean available) {
                Log.d(TAG, "onUserVideoAvailable"+available+":"+userId+":"+mRoomId);
                if (mTRTCCloud != null && mTrtcVideoView != null) {
                    if (available) {
                        if (mRoomId.equals(userId)) {
                            //主播开始直播
                            Log.d(TAG, "主播开始直播");
                            mTRTCCloud.startRemoteView(userId, mTrtcVideoView);
                        }
                    } else {
                        if (mRoomId.equals(userId)) {
                            Toast.makeText(AudienceLiveActivity.this, "主播已经离开", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "主播离开直播间");
                            finish();
                        }
                    }
                }

            }

            /**
             * 用户是否开启屏幕分享
             *
             * @param userId    用户标识
             * @param available 屏幕分享是否开启
             */

            @Override
            public void onUserSubStreamAvailable(String userId, boolean available) {
                Log.d(TAG, "onUserSubStreamAvailable");
            }

            /**
             * 用户是否开启音频上行
             *
             * @param userId    用户标识
             * @param available 声音是否开启
             */
            @Override
            public void onUserAudioAvailable(String userId, boolean available) {
                Log.d(TAG, "onUserAudioAvailable");

            }

            /**
             * 开始渲染本地或远程用户的首帧画面
             * <p>
             * 如果 userId 为 null，代表开始渲染本地采集的摄像头画面，需要您先调用 startLocalPreview 触发。 如果 userId 不为 null，代表开始渲染远程用户的首帧画面，需要您先调用 startRemoteView 触发。
             * 注意:
             * 只有当您调用 startLocalPreview()、startRemoteView() 或 startRemoteSubStreamView() 之后，才会触发该回调。
             *
             * @param userId     本地或远程用户 ID，如果 userId == null 代表本地，userId != null 代表远程。
             * @param streamType 视频流类型：摄像头或屏幕分享。
             * @param width      画面宽度
             * @param height     画面高度
             */

            @Override
            public void onFirstVideoFrame(String userId, int streamType, int width, int height) {
                Log.d(TAG, "onFirstVideoFrame" + userId);


            }

            /**
             * 开始播放远程用户的首帧音频
             *
             * @param userId 远程用户id
             */
            @Override
            public void onFirstAudioFrame(String userId) {
                Log.d(TAG, "onFirstAudioFrame" + userId);
            }

            /**
             * 首帧本地视频数据已经被送出
             * SDK 会在 enterRoom() 并 startLocalPreview() 成功后开始摄像头采集，并将采集到的画面进行编码。 当 SDK 成功向云端送出第一帧视频数据后，会抛出这个回调事件。
             *
             * @param streamType 视频流类型，大画面、小画面或辅流画面（屏幕分享）
             */

            @Override
            public void onSendFirstLocalVideoFrame(int streamType) {
                Log.d(TAG, "onSendFirstLocalVideoFrame");
            }

            /**
             * 首帧本地音频数据已经被送出
             */
            @Override
            public void onSendFirstLocalAudioFrame() {
                Log.d(TAG, "onSendFirstLocalAudioFrame");
            }

            /**
             * 废弃接口：有主播加入当前房间
             * <p>
             * 该回调接口可以被看作是 onRemoteUserEnterRoom 的废弃版本，不推荐使用。请使用 onUserVideoAvailable 或 onRemoteUserEnterRoom 进行替代。
             * <p>
             * 注意
             * 该接口已被废弃，不推荐使用
             *
             * @param userId 用户标识
             */
            @Override
            public void onUserEnter(String userId) {
                Log.d(TAG, "onUserEnter");
            }

            /**
             * 废弃接口： 有主播离开当前房间
             *
             * @param userId 用户标识
             * @param reason 离开原因。
             */
            @Override
            public void onUserExit(String userId, int reason) {
                Log.d(TAG, "onUserExit");
            }

            /**
             * 网络质量：该回调每2秒触发一次，统计当前网络的上行和下行质量
             *
             * @param localQuality  上行网络质量
             * @param remoteQuality 下行网络质量
             */
            @Override
            public void onNetworkQuality(TRTCCloudDef.TRTCQuality localQuality, ArrayList<TRTCCloudDef.TRTCQuality> remoteQuality) {
                Log.d(TAG, "onNetworkQuality " + localQuality.quality);
            }

            /**
             * 技术指标统计回调
             * <p>
             * 如果您是熟悉音视频领域相关术语，可以通过这个回调获取 SDK 的所有技术指标。 如果您是首次开发音视频相关项目，可以只关注 onNetworkQuality 回调。
             *
             * @param statics
             */
            @Override
            public void onStatistics(TRTCStatistics statics) {
                Log.d(TAG, "onStatistics" + statics.appCpu);

            }

            /**
             * SDK 跟服务器的连接断开
             */
            @Override
            public void onConnectionLost() {
                Log.d(TAG, "onConnectionLost");
            }

            /**
             * SDK 尝试重新连接到服务器
             */

            @Override
            public void onTryToReconnect() {
                Log.d(TAG, "onTryToReconnect");
            }

            /**
             * SDK 跟服务器的连接恢复
             */

            @Override
            public void onConnectionRecovery() {
                Log.d(TAG, "onConnectionRecovery");
            }

            /**
             * 服务器测速的回调，SDK 对多个服务器 IP 做测速，每个 IP 的测速结果通过这个回调通知
             *
             * @param currentResult
             * @param finishedCount
             * @param totalCount
             */

            @Override
            public void onSpeedTest(TRTCCloudDef.TRTCSpeedTestResult currentResult, int finishedCount, int totalCount) {
                Log.d(TAG, "onSpeedTest");
            }

            /**
             * 摄像头准备就绪
             */
            @Override
            public void onCameraDidReady() {
                Log.d(TAG, "onCameraDidReady");
            }

            /**
             * 麦克风准备就绪
             */
            @Override
            public void onMicDidReady() {
                Log.d(TAG, "onMicDidReady");
            }

            /**
             * 音频路由发生变化，音频路由即声音由哪里输出（扬声器、听筒）
             */
            @Override
            public void onAudioRouteChanged(int newRoute, int oldRoute) {
                Log.d(TAG, "onAudioRouteChanged" + newRoute + ":" + oldRoute);
            }

            /**
             * 用于提示音量大小的回调,包括每个 userId 的音量和远端总音量
             * <p>
             * 您可以通过调用 TRTCCloud 中的 enableAudioVolumeEvaluation 接口来开关这个回调或者设置它的触发间隔。
             * 需要注意的是，调用 enableAudioVolumeEvaluation 开启音量回调后，
             * 无论频道内是否有人说话，都会按设置的时间间隔调用这个回调; 如果没有人说话，则 userVolumes 为空，totalVolume 为0。
             * userId 为 null 时表示自己的音量，userVolumes 内仅包含正在说话（音量不为0）的用户音量信息。
             *
             * @param userVolumes 所有正在说话的房间成员的音量，取值范围0 - 100。
             * @param totalVolume 所有远端成员的总音量, 取值范围0 - 100。
             */

            @Override
            public void onUserVoiceVolume(ArrayList<TRTCCloudDef.TRTCVolumeInfo> userVolumes, int totalVolume) {
                Log.e(TAG, "onUserVoiceVolume" + userVolumes);

            }

            /**
             * 收到自定义消息回调
             *
             * @param userId  用户标识
             * @param cmdID   命令 ID
             * @param seq     消息序号
             * @param message 消息数据
             */

            @Override
            public void onRecvCustomCmdMsg(String userId, int cmdID, int seq, byte[] message) {
                Log.d(TAG, "onRecvCustomCmdMsg");

            }

            /**
             * 自定义消息丢失回调
             * <p>
             * 实时音视频使用 UDP 通道，即使设置了可靠传输（reliable）也无法确保100%不丢失，只是丢消息概率极低，能满足常规可靠性要求。 在发送端设置了可靠传输（reliable）后，SDK 都会通过此回调通知过去时间段内（通常为5s）传输途中丢失的自定义消息数量统计信息
             *
             * @param userId  用户标识
             * @param cmdID   命令 ID
             * @param errCode 错误码，当前版本为-1
             * @param missed  丢失的消息数量
             */

            @Override
            public void onMissCustomCmdMsg(String userId,
                                           int cmdID,
                                           int errCode,
                                           int missed) {
                Log.d(TAG, "onMissCustomCmdMsg");
            }

            /**
             * 收到 SEI 消息的回调
             *
             * @param userId 用户标识
             * @param data   数据
             */

            @Override
            public void onRecvSEIMsg(String userId, byte[] data) {
                Log.d(TAG, "onRecvSEIMsg");

            }

            /**
             * 开始向腾讯云的直播 CDN 推流的回调，对应于 TRTCCloud 中的 startPublishing() 接口
             *
             * @param err    0表示成功，其余值表示失败
             * @param errMsg 错误原因
             */

            @Override
            public void onStartPublishing(int err, String errMsg) {
                Log.d(TAG, "onStartPublishing");
            }

            /**
             * 停止向腾讯云的直播 CDN 推流的回调，对应于 TRTCCloud 中的 stopPublishing() 接口
             *
             * @param err    0表示成功，其余值表示失败
             * @param errMsg 错误原因
             */
            @Override
            public void onStopPublishing(int err, String errMsg) {
                Log.d(TAG, "onStopPublishing");
            }

            /**
             * 启动旁路推流到 CDN 完成的回调
             * <p>
             * 对应于 TRTCCloud 中的 startPublishCDNStream() 接口
             * <p>
             * 注意
             * Start 回调如果成功，只能说明转推请求已经成功告知给腾讯云，如果目标 CDN 有异常，还是有可能会转推失败。
             *
             * @param err    0表示成功，其余值表示失败
             * @param errMsg 错误原因
             */
            @Override
            public void onStartPublishCDNStream(int err, String errMsg) {
                Log.d(TAG, "onStartPublishCDNStream");
            }

            /**
             * 停止旁路推流到 CDN 完成的回调
             *
             * @param err    0表示成功，其余值表示失败
             * @param errMsg 错误原因
             */

            @Override
            public void onStopPublishCDNStream(int err, String errMsg) {
                Log.d(TAG, "onStopPublishCDNStream");
            }

            /**
             * 设置云端的混流转码参数的回调，对应于 TRTCCloud 中的 setMixTranscodingConfig() 接口
             *
             * @param err    0表示成功，其余值表示失败
             * @param errMsg 错误原因
             */
            @Override
            public void onSetMixTranscodingConfig(int err, String errMsg) {
                Log.d(TAG, "onSetMixTranscodingConfig");
            }

            /**
             * 播放音效结束回调
             *
             * @param effectId
             * @param code     0表示播放正常结束；其他表示异常结束，暂无异常值
             */
            @Override
            public void onAudioEffectFinished(int effectId, int code) {
                Log.d(TAG, "onAudioEffectFinished");
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
    }
}
