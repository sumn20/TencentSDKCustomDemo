package com.trtc.customcamera.capture;

import android.content.Context;
import android.hardware.Camera;

public class CameraBuilder {
    public enum CameraType {
        Camera1,
        Camera2;
    }

    private int WIDTH = 1280;
    private int HEIGHT = 720;
    private int MAX_VIDEO_FPS = 30;
    private int MIN_VIDEO_FPS = 15;
    private Context mContext;
    private CameraCapture cameraCapture;
    private CameraType cameraType = CameraType.Camera2;
    private int cameraID = 0;

    public CameraBuilder(Context mContext) {
        this.mContext = mContext;
    }

    public CameraBuilder(int WIDTH, int HEIGHT, int MAX_VIDEO_FPS, int MIN_VIDEO_FPS, Context mContext) {
        this.WIDTH = WIDTH;
        this.HEIGHT = HEIGHT;
        this.MAX_VIDEO_FPS = MAX_VIDEO_FPS;
        this.MIN_VIDEO_FPS = MIN_VIDEO_FPS;
        this.mContext = mContext;
    }

    public int getCameraID() {
        return cameraID;
    }

    public CameraBuilder setCameraID(int cameraID) {
        this.cameraID = cameraID;
        return this;
    }

    public CameraBuilder setCameraType(CameraType cameraType) {
        this.cameraType = cameraType;
        return this;
    }

    public CameraBuilder setWidth(int WIDTH) {
        this.WIDTH = WIDTH;
        return this;
    }

    public CameraBuilder setHeight(int HEIGHT) {
        this.HEIGHT = HEIGHT;
        return this;
    }

    public CameraBuilder setMaxVideoFps(int MAX_VIDEO_FPS) {
        this.MAX_VIDEO_FPS = MAX_VIDEO_FPS;
        return this;
    }

    public CameraBuilder setMinVideoFps(int MIN_VIDEO_FPS) {
        this.MIN_VIDEO_FPS = MIN_VIDEO_FPS;
        return this;
    }

    public CameraBuilder setContext(Context mContext) {
        this.mContext = mContext;
        return this;
    }

    public int getWidth() {
        return WIDTH;
    }

    public int getHeight() {
        return HEIGHT;
    }

    public int getMaxVideoFps() {
        return MAX_VIDEO_FPS;
    }

    public int getMinVideoFps() {
        return MIN_VIDEO_FPS;
    }

    public Context getContext() {
        return mContext;
    }

    public CameraCapture getCameraCapture() {
        return cameraCapture;
    }

    public CameraType getCameraType() {
        return cameraType;
    }

    public CameraCapture build() {
        if (cameraCapture != null) {
            cameraCapture.destroy();
            cameraCapture = null;
        }
        return cameraCapture = new CameraCapture(this);

    }
}
