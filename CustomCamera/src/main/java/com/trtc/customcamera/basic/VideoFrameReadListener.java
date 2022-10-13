package com.trtc.customcamera.basic;

import android.opengl.EGLContext;

public interface VideoFrameReadListener {
    void onFrameAvailable(EGLContext eglContext, int textureId, int width, int height);
}
