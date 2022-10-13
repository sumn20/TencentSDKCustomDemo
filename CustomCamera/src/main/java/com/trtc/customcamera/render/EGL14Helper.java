package com.trtc.customcamera.render;

import android.annotation.TargetApi;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Build;
import android.util.Log;
import android.view.Surface;





@TargetApi(18)
public class EGL14Helper implements EGLHelper<EGLContext> {

    private static final String TAG = "EGL14Helper";

    private static final int EGL_RECORDABLE_ANDROID = 0x3142;

    public static EGL14Helper createEGLSurface(EGLConfig config, EGLContext context,
                                               Surface surface, int width, int height)  {
        EGL14Helper egl = new EGL14Helper(width, height);
        try {
            egl.initialize(config, context, surface);
        } catch (Exception e) {
            Log.e(TAG,e.getLocalizedMessage());
            egl.destroy();
        }
        return egl;
    }

    private final int mWidth;
    private final int mHeight;
    private EGLConfig mEGLConfig = null;
    private EGLDisplay mEGLDisplay = EGL14.EGL_NO_DISPLAY;
    private EGLContext mEGLContext = EGL14.EGL_NO_CONTEXT;
    private EGLSurface mEGLSurface = EGL14.EGL_NO_SURFACE;

    private EGL14Helper(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    public void updateSurface(Object nativeWindow) throws Exception {
        destroySurface();
        int[] surfaceAttribs = {EGL14.EGL_NONE};
        mEGLSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, mEGLConfig, nativeWindow, surfaceAttribs, 0);
        throwEGLExceptionIfFailed();
    }

    @Override
    public void makeCurrent() {
        if (!EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)) {
            throwEGLExceptionIfFailed();
        }
    }


    public void destroySurface() {
        if (mEGLSurface != EGL14.EGL_NO_SURFACE) {
            unmakeCurrent();
            EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface);
            mEGLSurface = EGL14.EGL_NO_SURFACE;
        }
    }

    @Override
    public void destroy() {
        if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
            // Android is unusual in that it uses a reference-counted EGLDisplay. So for
            // every eglInitialize() we need an eglTerminate().
            EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
            if (mEGLSurface != EGL14.EGL_NO_SURFACE) {
                EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface);
                mEGLSurface = EGL14.EGL_NO_SURFACE;
            }
            if (mEGLContext != EGL14.EGL_NO_CONTEXT) {
                EGL14.eglDestroyContext(mEGLDisplay, mEGLContext);
                mEGLContext = EGL14.EGL_NO_CONTEXT;
            }
            EGL14.eglReleaseThread();
            EGL14.eglTerminate(mEGLDisplay);
        }
        mEGLDisplay = EGL14.EGL_NO_DISPLAY;
    }

    @Override
    public boolean swapBuffers() {
        // eglSwapBuffers performs an implicit flush operation on the context
        // (glFlush for an OpenGL ES or OpenGL context, vgFlush for an OpenVG context) bound to
        // surface before swapping. Subsequent client API commands may be issued on that context
        // immediately after calling eglSwapBuffers, but are not executed until the buffer exchange
        // is completed.
        GLES20.glFinish();
        if (!EGL14.eglSwapBuffers(mEGLDisplay, mEGLSurface)) {
            throwEGLExceptionIfFailed();
            return false;
        } else {
            return true;
        }
    }

    private void initialize(EGLConfig config, EGLContext context, Surface surface) throws Exception {
        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            Log.e(TAG, "unable to get EGL14 display");
            throw new Exception(String.valueOf(EGL14.EGL_FALSE));
        }

        int[] version = new int[2];
        if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            mEGLDisplay = null;
            Log.e(TAG, "unable to initialize EGL14");
            throw new Exception(String.valueOf(EGL14.EGL_FALSE));
        }

        if (config != null) {
            mEGLConfig = config;
        } else {
            EGLConfig[] configs = new EGLConfig[1];
            int[] numConfigs = new int[1];
            int[] attribList = surface == null ? ATTRIBUTE_LIST_FOR_OFFSCREEN_SURFACE : ATTRIBUTE_LIST_FOR_SURFACE;
            if (!EGL14.eglChooseConfig(mEGLDisplay, attribList, 0, configs, 0, configs.length, numConfigs, 0)) {
                throw new Exception(String.valueOf(EGL14.EGL_FALSE));
            }
            mEGLConfig = configs[0];
        }

        // 某些版本的系统，创建EGLContext的时候指定版本号为3，然后创建共享EGLContext指定为2时，会失败。
        // 因此这里默认创建的时候为2，如果失败则尝试3。
        // 重构中优先使用3，失败再尝试2。
        if (Build.VERSION.SDK_INT >= 18) {
            try {
                mEGLContext = createEGLContext(mEGLDisplay, mEGLConfig, 3, context);
            } catch (Exception e) {
                Log.i(TAG, "failed to create EGLContext of OpenGL ES 3.0, try 2.0");
                mEGLContext = createEGLContext(mEGLDisplay, mEGLConfig, 2, context);
            }
        } else {
            mEGLContext = createEGLContext(mEGLDisplay, mEGLConfig, 2, context);
        }
        Log.i(TAG, "create eglContext " + mEGLContext);
        if (surface == null) {
            int[] attribListPbuffer = {
                    EGL14.EGL_WIDTH, mWidth,
                    EGL14.EGL_HEIGHT, mHeight,
                    EGL14.EGL_NONE
            };
            mEGLSurface = EGL14.eglCreatePbufferSurface(mEGLDisplay, mEGLConfig, attribListPbuffer, 0);
        } else {
            int[] surfaceAttribs = {EGL14.EGL_NONE};
            try {
                mEGLSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, mEGLConfig, surface, surfaceAttribs, 0);
            } catch (Exception e) {
                int errorCode = EGL14.eglGetError();
                throw new Exception(String.valueOf(errorCode), e);
            }
        }

        throwEGLExceptionIfFailed();
        if (!EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)) {
            throwEGLExceptionIfFailed();
        }
    }

    private static EGLContext createEGLContext(EGLDisplay display,
                                               EGLConfig config,
                                               int clientVersion,
                                               EGLContext sharedContext) throws Exception {
        int[] attribList = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, clientVersion,
                EGL14.EGL_NONE
        };
        if (sharedContext == null) {
            sharedContext = EGL14.EGL_NO_CONTEXT;
        }
        EGLContext context = EGL14.eglCreateContext(display, config, sharedContext, attribList, 0);
        throwEGLExceptionIfFailed();
        return context;
    }

    @Override
    public void unmakeCurrent() {
        if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
        }
    }

    public void setPresentationTime(long nsecs) {
        EGLExt.eglPresentationTimeANDROID(mEGLDisplay, mEGLSurface, nsecs);
    }

    @Override
    public EGLContext getContext() {
        return mEGLContext;
    }

    private static void throwEGLExceptionIfFailed() {
        int ec = EGL14.eglGetError();
        if (ec != EGL14.EGL_SUCCESS) {
            Log.w(TAG, "opengl error: " + ec);
        }
    }

    private static final int[] ATTRIBUTE_LIST_FOR_SURFACE = {
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_DEPTH_SIZE, 0,
            EGL14.EGL_STENCIL_SIZE, 0,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL_RECORDABLE_ANDROID, 1,
            EGL14.EGL_NONE
    };

    private static final int[] ATTRIBUTE_LIST_FOR_OFFSCREEN_SURFACE = {
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,//前台显示Surface这里EGL10.EGL_WINDOW_BIT
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_DEPTH_SIZE, 0,
            EGL14.EGL_STENCIL_SIZE, 0,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL_RECORDABLE_ANDROID, 1,
            EGL14.EGL_NONE
    };
}
