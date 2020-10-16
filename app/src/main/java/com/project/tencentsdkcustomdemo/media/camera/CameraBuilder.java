package com.project.tencentsdkcustomdemo.media.camera;

import android.content.Context;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.util.Size;

/**
 * Camera1和Camera2实现切换
 */
public class CameraBuilder {
    public enum CameraType {
        Camera1,
        Camera2;

    }

    private static final String TAG = "CameraBuilder";
    public static final String CAMERA_ID_FRONT = "1";
    public static final String CAMERA_ID_BACK = "0";

    /**
     * 预览显示的view，目前仅支持textureView
     */
    private SurfaceTexture previewDisplayView;


    /**
     * 指定的相机ID
     */
    private String specificCameraId;


    /**
     * 上下文，用于获取CameraManager
     */
    private Context context;
    /**
     * 相机类型 默认Camera2
     */
    private CameraType cameraType = CameraType.Camera2;
    /**
     * 摄像头采集数据的宽高
     */
    private Size previewSize;

    private CameraInterface cameraInterface;

    public CameraBuilder() {
    }


    public CameraBuilder previewOn(SurfaceTexture val) {
        previewDisplayView = val;
        return this;
    }



    public CameraBuilder specificCameraId(String val) {
        specificCameraId = val;
        return this;
    }

    public CameraBuilder previewSize(Size val) {
        previewSize = val;
        return this;
    }

    public CameraBuilder context(Context val) {
        context = val;
        return this;
    }

    public CameraBuilder setCameraType(CameraType cameraType) {
        this.cameraType = cameraType;
        return this;
    }

    public SurfaceTexture getPreviewDisplayView() {
        return previewDisplayView;
    }


    public String getSpecificCameraId() {
        return specificCameraId;
    }


    public Context getContext() {
        return context;
    }

    public Size getPreviewSize() {
        return previewSize;
    }

    public CameraInterface build() {
        if (previewSize==null){
            throw new NullPointerException("previewSize 不能为空");
        }
        if (previewDisplayView == null) {
            throw new NullPointerException("you must preview on a textureView or a surfaceView");
        }
        if (cameraInterface != null) {
            cameraInterface.release();
        }
        if (cameraType == CameraType.Camera2) {
            return cameraInterface = new Camera2Helper(this);
        } else {
            return cameraInterface = new CameraHelper(this);
        }


    }
}

