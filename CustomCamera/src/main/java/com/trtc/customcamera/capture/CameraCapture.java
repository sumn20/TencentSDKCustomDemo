package com.trtc.customcamera.capture;

import static com.trtc.customcamera.render.opengl.OpenGlUtils.NO_TEXTURE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.Pair;
import android.util.Range;
import android.view.Surface;
import android.widget.ImageView;

import androidx.annotation.NonNull;


import com.trtc.customcamera.basic.FrameBuffer;
import com.trtc.customcamera.basic.TextureFrame;
import com.trtc.customcamera.basic.VideoFrameReadListener;
import com.trtc.customcamera.render.EglCore;
import com.trtc.customcamera.render.opengl.GPUImageFilter;
import com.trtc.customcamera.render.opengl.GPUImageFilterGroup;
import com.trtc.customcamera.render.opengl.OesInputFilter;
import com.trtc.customcamera.render.opengl.OpenGlUtils;
import com.trtc.customcamera.render.opengl.Rotation;


import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CameraCapture implements SurfaceTexture.OnFrameAvailableListener {
    public static final String TAG = CameraCapture.class.getSimpleName();
    private static final int WHAT_START = 0;
    private static final int WHAT_UPDATE = 1;
    private final CameraBuilder mCameraBuilder;
    private VideoFrameReadListener mVideoFrameReadListener;

    //openGl相关
    private SurfaceTexture mSurfaceTexture;
    private EglCore mEglCore;
    private FrameBuffer mFrameBuffer;
    private OesInputFilter mOesInputFilter;
    private GPUImageFilterGroup mGpuImageFilterGroup;

    private final FloatBuffer mGLCubeBuffer;
    private final FloatBuffer mGLTextureBuffer;
    private final float[] mTextureTransform = new float[16]; // OES纹理转换为2D纹理
    private int mSurfaceTextureId = NO_TEXTURE;
    private boolean mFrameUpdated;
    private HandlerThread mRenderHandlerThread;
    private volatile RenderHandler mRenderHandler;

    //camera相关
    //camera1
    private Camera mCamera;

    //camera2相关
    private CameraCaptureSession mCaptureSession;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private final Semaphore mCameraOpenCloseLock = new Semaphore(1);

    public CameraCapture(CameraBuilder mCameraBuilder) {
        this.mCameraBuilder = mCameraBuilder;
        mFrameUpdated = false;
        Pair<float[], float[]> cubeAndTextureBuffer = OpenGlUtils.calcCubeAndTextureBuffer(ImageView.ScaleType.CENTER, Rotation.NORMAL, false, mCameraBuilder.getWidth(), mCameraBuilder.getHeight(), mCameraBuilder.getWidth(), mCameraBuilder.getHeight());
        mGLCubeBuffer = ByteBuffer.allocateDirect(OpenGlUtils.CUBE.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mGLCubeBuffer.put(cubeAndTextureBuffer.first);
        mGLTextureBuffer = ByteBuffer.allocateDirect(OpenGlUtils.TEXTURE.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mGLTextureBuffer.put(cubeAndTextureBuffer.second);
    }

    public void startCameraCapture(final VideoFrameReadListener videoFrameReadListener) {
        mVideoFrameReadListener = videoFrameReadListener;
        mRenderHandlerThread = new HandlerThread("RenderHandlerThread");
        mRenderHandlerThread.start();
        mRenderHandler = new RenderHandler(mRenderHandlerThread.getLooper(), this);
        mRenderHandler.sendEmptyMessage(WHAT_START);
    }

    public void changeCameraID(int cameraID) {
        mCameraBuilder.setCameraID(cameraID);
        checkCameraID();
        stopCameraDevice();
        startCameraDevice();
    }

    private void checkCameraID() {
        int cameraID = mCameraBuilder.getCameraID();
        if (mCameraBuilder.getCameraType() == CameraBuilder.CameraType.Camera1) {
            if (cameraID >= Camera.getNumberOfCameras()) {
                cameraID = Camera.getNumberOfCameras() - 1;
            }
        } else {
            CameraManager cameraManager = (CameraManager) mCameraBuilder.getContext().getSystemService(Context.CAMERA_SERVICE);
            try {
                int lastCameraID = Integer.parseInt(cameraManager.getCameraIdList()[cameraManager.getCameraIdList().length - 1]);
                if (cameraID > lastCameraID) {
                    cameraID = lastCameraID;
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
        mCameraBuilder.setCameraID(cameraID);
    }

    private void startInternal() {
        mEglCore = new EglCore(mCameraBuilder.getWidth(), mCameraBuilder.getHeight());
        mEglCore.makeCurrent();
        mFrameBuffer = new FrameBuffer(mCameraBuilder.getWidth(), mCameraBuilder.getHeight());
        mFrameBuffer.initialize();
        mGpuImageFilterGroup = new GPUImageFilterGroup();
        mOesInputFilter = new OesInputFilter();
        mGpuImageFilterGroup.addFilter(mOesInputFilter);
        mGpuImageFilterGroup.addFilter(new GPUImageFilter(true));
        mGpuImageFilterGroup.init();
        mGpuImageFilterGroup.onOutputSizeChanged(mCameraBuilder.getWidth(), mCameraBuilder.getHeight());
        mSurfaceTextureId = OpenGlUtils.generateTextureOES();
        mSurfaceTexture = new SurfaceTexture(mSurfaceTextureId);
        mSurfaceTexture.setOnFrameAvailableListener(this);
        startCameraDevice();
    }

    /**
     * 初始化相机设备
     */
    private void startCameraDevice() {
        Log.i(TAG,"startCameraDevice cameraType:"+mCameraBuilder.getCameraType()+" cameraID:"+mCameraBuilder.getCameraID());
        if (mCameraBuilder.getCameraType() == CameraBuilder.CameraType.Camera1) {
            createCamera1();
        } else {
            createCamera2();
        }
    }

    /**
     * 创建camera1相机
     */
    private void createCamera1() {
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                checkCameraID();
                mCamera = Camera.open(mCameraBuilder.getCameraID());
                mCamera.setPreviewTexture(mSurfaceTexture);
                mCamera.setDisplayOrientation(90);
                Camera.Parameters parameters = mCamera.getParameters();
                parameters.setPreviewSize(mCameraBuilder.getWidth(), mCameraBuilder.getHeight());
                List<int[]> fpsList = parameters.getSupportedPreviewFpsRange();
                if (fpsList != null && fpsList.size() > 0) {
                    if (mCameraBuilder.getMinVideoFps() * 1000 < fpsList.get(0)[0]) {
                        mCameraBuilder.setMinVideoFps(fpsList.get(0)[0] / 1000);
                    }
                    if (mCameraBuilder.getMaxVideoFps() * 1000 > fpsList.get(fpsList.size() - 1)[1]) {
                        mCameraBuilder.setMaxVideoFps(fpsList.get(fpsList.size() - 1)[1] / 1000);
                    }
                }
                parameters.setPreviewFpsRange(mCameraBuilder.getMinVideoFps() * 1000, mCameraBuilder.getMaxVideoFps() * 1000);
                mCamera.setParameters(parameters);
                mCamera.startPreview();
                mCamera.autoFocus(null);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Camera1 Open Error,msg:" + Log.getStackTraceString(e));
                stopCameraDevice();
                Log.d(TAG, "Camera1 Open Error,try camera2");
                mCameraBuilder.setCameraType(CameraBuilder.CameraType.Camera2);
                startCameraDevice();
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void createCamera2() {
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                mBackgroundThread = new HandlerThread("CameraBackground");
                mBackgroundThread.start();
                mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
                CameraManager cameraManager = (CameraManager) mCameraBuilder.getContext().getSystemService(Context.CAMERA_SERVICE);
                checkCameraID();
                CameraCharacteristics characteristics
                        = cameraManager.getCameraCharacteristics(String.valueOf(mCameraBuilder.getCameraID()));
                Range<Integer>[] fpsRange = characteristics.get(
                        CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
                if (fpsRange != null && fpsRange.length > 0) {
                    if (mCameraBuilder.getMinVideoFps() < fpsRange[0].getLower()) {
                        mCameraBuilder.setMinVideoFps(fpsRange[0].getLower());
                    }
                    if (mCameraBuilder.getMaxVideoFps() > fpsRange[fpsRange.length - 1].getUpper()) {
                        mCameraBuilder.setMaxVideoFps(fpsRange[fpsRange.length - 1].getUpper());
                    }
                }
                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    throw new NullPointerException("map is Null");
                }
                if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                    throw new RuntimeException("Time out waiting to lock camera opening.");
                }
                cameraManager.openCamera(String.valueOf(mCameraBuilder.getCameraID()), new CameraDevice.StateCallback() {

                    @Override
                    public void onOpened(@NonNull CameraDevice cameraDevice) {
                        Log.i(TAG, "Camera2 onOpened: ");
                        // This method is called when the camera is opened.  We start camera preview here.
                        mCameraOpenCloseLock.release();
                        mCameraDevice = cameraDevice;
                        createCameraPreviewSession();

                    }

                    @Override
                    public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                        Log.i(TAG, "Camera2 onDisconnected: ");
                        mCameraOpenCloseLock.release();
                        cameraDevice.close();
                        mCameraDevice = null;

                    }

                    @Override
                    public void onError(@NonNull CameraDevice cameraDevice, int error) {
                        Log.e(TAG, "Camera2 onError: "+error);
                        mCameraOpenCloseLock.release();
                        cameraDevice.close();
                        mCameraDevice = null;
                        stopBackgroundThread();
                        stopCameraDevice();
                        Log.d(TAG, "Camera2 Open Error,try camera1");
                        mCameraBuilder.setCameraType(CameraBuilder.CameraType.Camera1);
                        startCameraDevice();

                    }

                }, mBackgroundHandler);
            } catch (Exception e) {
                stopBackgroundThread();
                Log.e(TAG, "Camera2 Open Error,msg:" + Log.getStackTraceString(e));
                stopCameraDevice();
                Log.d(TAG, "Camera2 Open Error,try camera1");
                mCameraBuilder.setCameraType(CameraBuilder.CameraType.Camera1);
                startCameraDevice();
            }
        });
    }

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

    private void createCameraPreviewSession() {
        try {
            assert mSurfaceTexture != null;

            mSurfaceTexture.setDefaultBufferSize(mCameraBuilder.getWidth(), mCameraBuilder.getHeight());
            Surface surface = new Surface(mSurfaceTexture);
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, new Range<>(mCameraBuilder.getMinVideoFps(), mCameraBuilder.getMaxVideoFps()));
            mPreviewRequestBuilder.addTarget(surface);
            mCameraDevice.createCaptureSession(Collections.singletonList(surface),
                    new CameraCaptureSession.StateCallback() {

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
                    }, mBackgroundHandler
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    /**
     * 关闭相机设备
     */
    private void stopCameraDevice() {
        if (mCameraBuilder.getCameraType() == CameraBuilder.CameraType.Camera1) {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.release();
            }
        } else {
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
            stopBackgroundThread();
        }
    }


    /**
     * 完全释放持有的资源
     */
    public void destroy() {
        stopCameraDevice();
        if (mRenderHandlerThread != null) {
            mRenderHandlerThread.quit();
        }
        if (mGpuImageFilterGroup != null) {
            mGpuImageFilterGroup.destroy();
            mGpuImageFilterGroup = null;
        }

        if (mFrameBuffer != null) {
            mFrameBuffer.uninitialize();
            mFrameBuffer = null;
        }

        if (mSurfaceTextureId != NO_TEXTURE) {
            OpenGlUtils.deleteTexture(mSurfaceTextureId);
            mSurfaceTextureId = NO_TEXTURE;
        }

        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }

        if (mEglCore != null) {
            mEglCore.unmakeCurrent();
            mEglCore.destroy();
            mEglCore = null;
        }
    }

    private void updateTexture() {
        synchronized (this) {
            if (mFrameUpdated) {
                mFrameUpdated = false;
            }
            try {
                if (mSurfaceTexture != null) {
                    mSurfaceTexture.updateTexImage();
                    mSurfaceTexture.getTransformMatrix(mTextureTransform);
                    mOesInputFilter.setTexutreTransform(mTextureTransform);
                    mGpuImageFilterGroup.draw(mSurfaceTextureId, mFrameBuffer.getFrameBufferId(), mGLCubeBuffer, mGLTextureBuffer);
                    GLES20.glFinish();
                    if (mVideoFrameReadListener != null) {
                        TextureFrame textureFrame = new TextureFrame();
                        textureFrame.eglContext = (EGLContext) mEglCore.getEglContext();
                        textureFrame.textureId = mFrameBuffer.getTextureId();
                        textureFrame.width = mCameraBuilder.getHeight();
                        textureFrame.height = mCameraBuilder.getWidth();
                        mVideoFrameReadListener.onFrameAvailable(textureFrame.eglContext, textureFrame.textureId, textureFrame.width, textureFrame.height);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "onFrameAvailable: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        mFrameUpdated = true;
        mRenderHandler.sendEmptyMessage(WHAT_UPDATE);
    }

    private static class RenderHandler extends Handler {

        private final WeakReference<CameraCapture> readerWeakReference;

        public RenderHandler(@NonNull Looper looper, CameraCapture cameraVideoFrameReader) {
            super(looper);
            readerWeakReference = new WeakReference<>(cameraVideoFrameReader);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            CameraCapture cameraVideoFrameReader = readerWeakReference.get();
            if (cameraVideoFrameReader != null) {
                if (WHAT_START == msg.what) {
                    cameraVideoFrameReader.startInternal();
                } else if (WHAT_UPDATE == msg.what) {
                    cameraVideoFrameReader.updateTexture();
                }
            }
        }
    }
}
