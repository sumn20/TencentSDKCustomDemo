package com.project.tencentsdkcustomdemo.media.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;

/**
 * 基于camera2的自定义采集实现类
 * 参考代码 https://github.com/googlearchive/android-Camera2Basic
 */
public class Camera2Helper implements CameraInterface {
    private static final String TAG = "Camera2Helper";

    private String mCameraId;
    private String specificCameraId;
    private SurfaceTexture mSurfaceTexture;
    private Context context;
    /**
     * {@link CameraCaptureSession } 用于相机预览
     */
    private CameraCaptureSession mCaptureSession;
    /**
     * 用于获取相机设备对象 {@link CameraDevice}.
     */
    private CameraDevice mCameraDevice;
    /**
     * 创造一个Handler子线程配合handler在后台持续获取相机数据
     */
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    /**
     * {@link CaptureRequest.Builder} 用于相机预览
     */
    private CaptureRequest.Builder mPreviewRequestBuilder;
    /**
     * A {@link Semaphore} 以防止应用程序在关闭相机前退出。
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    private Size mPreviewSize;

    Camera2Helper(CameraBuilder builder) {
        mSurfaceTexture = builder.getPreviewDisplayView();
        specificCameraId = builder.getSpecificCameraId();
        mPreviewSize=builder.getPreviewSize();
        context = builder.getContext();
    }
    @Override
    public void switchCamera() {
        if (CameraBuilder.CAMERA_ID_BACK.equals(mCameraId)) {
            specificCameraId =CameraBuilder. CAMERA_ID_FRONT;
        } else if (CameraBuilder.CAMERA_ID_FRONT.equals(mCameraId)) {
            specificCameraId = CameraBuilder.CAMERA_ID_BACK;
        }
        stop();
        start();
    }


    @Override
    public synchronized void start() {
        if (mCameraDevice != null) {
            return;
        }
        Log.e(TAG,"Camera2Helper");
        startBackgroundThread();
        openCamera();
    }

    private synchronized void stop() {
        if (mCameraDevice == null) {
            return;
        }
        closeCamera();
        stopBackgroundThread();
    }
    @Override
    public synchronized void release() {
        stop();
        mSurfaceTexture = null;
        context = null;
    }

    private void setUpCameraOutputs(CameraManager cameraManager) {
        try {
            if (configCameraParams(cameraManager, specificCameraId)) {
                return;
            }
            for (String cameraId : cameraManager.getCameraIdList()) {
                if (configCameraParams(cameraManager, cameraId)) {
                    return;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            //使用Camera2 API但运行此代码的设备不支持时，将引发NPE。
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    private boolean configCameraParams(CameraManager manager, String cameraId) throws CameraAccessException {
        CameraCharacteristics characteristics
                = manager.getCameraCharacteristics(cameraId);

        StreamConfigurationMap map = characteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map == null) {
            return false;
        }
        mCameraId = cameraId;
        return true;
    }

    @SuppressLint("MissingPermission")
    private void openCamera() {
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        setUpCameraOutputs(cameraManager);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            cameraManager.openCamera(mCameraId, mDeviceStateCallback, mBackgroundHandler);
        } catch (CameraAccessException | InterruptedException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    /**
     * 关闭并且释放相机{@link CameraDevice}.
     */
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }

        } catch (InterruptedException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * 启动后台线程及其 {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * 停止后台线程及其 {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建一个{@link CameraCaptureSession} 用于主播得TextureView界面预览
     */
    private void createCameraPreviewSession() {
        try {
            assert mSurfaceTexture != null;
            // We configure the size of default buffer to be the size of camera preview we want

            mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(mSurfaceTexture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            mPreviewRequestBuilder.addTarget(surface);
            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Collections.singletonList(surface),
                    mCaptureStateCallback, mBackgroundHandler
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    private CameraCaptureSession.StateCallback mCaptureStateCallback = new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
            Log.i(TAG, "onConfigured: ");
            // The camera is already closed
            if (null == mCameraDevice) {
                return;
            }

            // When the session is ready, we start displaying the preview.
            mCaptureSession = cameraCaptureSession;
            try {
                mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),
                        new CameraCaptureSession.CaptureCallback() {
                        }, mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(
                @NonNull CameraCaptureSession cameraCaptureSession) {
            Log.i(TAG, "onConfigureFailed: ");

        }
    };
    private CameraDevice.StateCallback mDeviceStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            Log.i(TAG, "onOpened: ");
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();

        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            Log.i(TAG, "onDisconnected: ");
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;

        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            Log.i(TAG, "onError: ");
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;


        }

    };


}
