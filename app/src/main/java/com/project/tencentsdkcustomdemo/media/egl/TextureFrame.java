package com.project.tencentsdkcustomdemo.media.egl;


import javax.microedition.khronos.egl.EGLContext;

public class TextureFrame {
    public EGLContext eglContext;

    public int textureId;

    public int width;

    public int height;

    public long timestampMs;

    @Override
    public String toString() {
        return "TextureFrame{" +
                "eglContext=" + eglContext +
                ", textureId=" + textureId +
                ", width=" + width +
                ", height=" + height +
                ", timestampMs=" + timestampMs +
                '}';
    }
}
