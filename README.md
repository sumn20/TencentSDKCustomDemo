# Android 基于[腾讯实时音视频](https://cloud.tencent.com/document/product/647)的自定义采集/渲染Demo
## 注意！！！
此demo只是参考，非官方demo，并未规范测试过，不建议在项目中直接使用

## 内容介绍
#### 为何使用自定义采集?
为sdk的视频采集是使用camera1去做采集的,如果您的设备不支持camera1只支持camera2，则需要自己采集和处理摄像头拍摄画面，您可以通过 TRTCCloud 的 enableCustomVideoCapture 接口关闭 TRTC SDK 自己的摄像头采集和图像处理逻辑。然后您可以使用 sendCustomVideoData 接口向 TRTC SDK 填充您自己的视频数据。

#### 为何使用自定义渲染?
如果您是用在游戏开发中，或者需要在自己的界面引擎中嵌入 TRTC SDK，那么就要自己渲染视频画面。

#### 内容实现
项目实现了camera1和camera2的自定义采集实现


#### 介绍使用

```java

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
```


 

#### 实现参考

[camera2项目](https://github.com/googlearchive/android-Camera2Basic)

[TestRenderVideoFrame](https://github.com/tencentyun/TRTCSDK/blob/master/Android/TRTCSimpleDemo/customcapture/src/main/java/com/tencent/custom/customcapture/TestRenderVideoFrame.java)

#### 其他

基于camera2的buffer方案,但是性能较差，不适合分辨率较高的场景,故未添加到项目中,如果想要可以下载以下参考代码
[参考下载地址](http://image-duxin.test.upcdn.net/CustomVideoDemo(%E4%BB%85%E4%BE%9B%E5%8F%82%E8%80%83).zip)
