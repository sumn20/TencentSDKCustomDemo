package com.trtc.customcamera.basic;

public class Size {
    public int width;
    public int height;

    public Size() {
    }

    public Size(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public void swap() {
        int temp = width;
        width = height;
        height = temp;
    }
}
