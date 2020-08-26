package com.project.tencentsdkcustomdemo.media.camera;

public interface CameraInterface {
    /**
     * 启动相机
     */
    void start();

    /**
     * 释放相机
     */
    void release();

    /**
     * 选择摄像头
     */

    void switchCamera();
}
