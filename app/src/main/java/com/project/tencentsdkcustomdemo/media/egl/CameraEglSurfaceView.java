package com.project.tencentsdkcustomdemo.media.egl;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Size;
import android.view.Surface;
import android.view.WindowManager;

import com.project.tencentsdkcustomdemo.media.camera.CameraBuilder;
import com.project.tencentsdkcustomdemo.media.camera.CameraInterface;


public class CameraEglSurfaceView extends EglSurfaceView implements CameraFboRender.OnSurfaceListener {

    private CameraFboRender render;
    private Context mContext;
    private CameraInterface cameraInterface;
    private CameraBuilder cameraBuilder;


    public CameraEglSurfaceView(Context context) {
        this(context, null);
    }

    public CameraEglSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraEglSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setRenderMode(RENDERMODE_CONTINUOUSLY);
        mContext = context;
        render = new CameraFboRender(context);
        render.setOnSurfaceListener(this);
        cameraBuilder = new CameraBuilder()
                .setCameraType(CameraBuilder.CameraType.Camera1)
                .specificCameraId(CameraBuilder.CAMERA_ID_BACK)
                .context(mContext)
                .previewSize(new Size(1080, 1920));
        setRender(render);
        previewAngle(context);

    }

    public void setVideoFrameReadListener(CameraFboRender.VideoFrameReadListener videoFrameReadListener) {
        if (render != null) {
            render.setVideoFrameReadListener(videoFrameReadListener);
        }
    }

    @Override
    public void onSurfaceCreate(SurfaceTexture surfaceTexture) {
        cameraInterface = cameraBuilder
                .previewOn(surfaceTexture)
                .build();
        cameraInterface.start();
    }

    public void changeCameraInterface(CameraBuilder.CameraType cameraType) {
        cameraBuilder.setCameraType(cameraType);

    }

    public void switchCamera() {
        if (cameraInterface != null) {
            cameraInterface.switchCamera();
            if (cameraBuilder.getSpecificCameraId().equals(CameraBuilder.CAMERA_ID_BACK)) {
                cameraBuilder.specificCameraId(CameraBuilder.CAMERA_ID_FRONT);
            } else {
                cameraBuilder.specificCameraId(CameraBuilder.CAMERA_ID_BACK);
            }
            previewAngle(mContext);
        }
    }

    public String getSpecificCameraId() {
        if (cameraBuilder != null) {
            return cameraBuilder.getSpecificCameraId();
        }else {
            return CameraBuilder.CAMERA_ID_BACK;
        }
    }

    public void onDestroy() {
        if (cameraInterface != null) {
            cameraInterface.release();
        }
    }


    public void previewAngle(Context context) {
        int angle = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        render.resetMatirx();
        switch (angle) {
            case Surface.ROTATION_0:
                if (cameraBuilder.getSpecificCameraId().equals(CameraBuilder.CAMERA_ID_BACK)) {
                    render.setAngle(90, 0, 0, 1);
                    render.setAngle(180, 1, 0, 0);
                } else {
                    render.setAngle(90f, 0f, 0f, 1f);
                }
                break;
            case Surface.ROTATION_90:

                if (cameraBuilder.getSpecificCameraId().equals(CameraBuilder.CAMERA_ID_BACK)) {
                    render.setAngle(180, 0, 0, 1);
                    render.setAngle(180, 0, 1, 0);
                } else {
                    render.setAngle(90f, 0f, 0f, 1f);
                }
                break;
            case Surface.ROTATION_180:
                if (cameraBuilder.getSpecificCameraId().equals(CameraBuilder.CAMERA_ID_BACK)) {
                    render.setAngle(90f, 0.0f, 0f, 1f);
                    render.setAngle(180f, 0.0f, 1f, 0f);
                } else {
                    render.setAngle(-90, 0f, 0f, 1f);
                }
                break;
            case Surface.ROTATION_270:
                if (cameraBuilder.getSpecificCameraId().equals(CameraBuilder.CAMERA_ID_BACK)) {
                    render.setAngle(180f, 0.0f, 1f, 0f);
                } else {
                    render.setAngle(0f, 0f, 0f, 1f);
                }
                break;
        }
    }
}
