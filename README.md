# Android TRTC如何去实现自定义采集/渲染
## 内容介绍
#### 为何使用自定义采集?
为sdk的视频采集是使用camera1去做采集的,如果您的设备不支持camera1只支持camera2，则需要自己采集和处理摄像头拍摄画面，您可以通过 TRTCCloud 的 enableCustomVideoCapture 接口关闭 TRTC SDK 自己的摄像头采集和图像处理逻辑。然后您可以使用 sendCustomVideoData 接口向 TRTC SDK 填充您自己的视频数据。
#### 为何使用自定义渲染?
如果您是用在游戏开发中，或者需要在自己的界面引擎中嵌入 TRTC SDK，那么就要自己渲染视频画面。

#### 内容实现
项目实现了camera1和camera2的自定义采集实现,使用SurfaceTexture实现自定义渲染


#### 介绍

 media包---自定义采集关键代码

 - [CameraHelper.java](https://github.com/sumn20/TencentSDKCustomDemo/blob/master/app/src/main/java/com/project/tencentsdkcustomdemo/media/camera/CameraHelper.java)  
    ---基于camera1的自定义摄像头采集实现

 - [Camera2Helper.java](https://github.com/sumn20/TencentSDKCustomDemo/blob/master/app/src/main/java/com/project/tencentsdkcustomdemo/media/camera/Camera2Helper.java)  
    ---基于camera2的自定义摄像头采集实现

 render包---自定义渲染关键代码  

 - [TestRenderVideoFrame.java](https://github.com/sumn20/TencentSDKCustomDemo/blob/master/app/src/main/java/com/project/tencentsdkcustomdemo/render/TestRenderVideoFrame.java)


#### 实现参考

[camera2项目](https://github.com/googlearchive/android-Camera2Basic)

[TestRenderVideoFrame](https://github.com/tencentyun/TRTCSDK/blob/master/Android/TRTCSimpleDemo/customcapture/src/main/java/com/tencent/custom/customcapture/TestRenderVideoFrame.java)

#### 其他

基于camera2的buffer方案,但是性能较差，不适合分辨率较高的场景,故未添加到项目中,如果想要可以下载以下参考代码
[参考下载地址](http://image-duxin.test.upcdn.net/CustomVideoDemo(%E4%BB%85%E4%BE%9B%E5%8F%82%E8%80%83).zip)
