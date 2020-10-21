package com.project.tencentsdkcustomdemo.media.audio;

import android.media.AudioFormat;

import java.io.Serializable;
import java.util.Locale;


public class RecordConfig implements Serializable {
    /**
     * 录音格式
     */
    private RecordFormat format = RecordFormat.PCM;
    /**
     * 通道数:默认单通道
     */
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;

    /**
     * 位宽
     */
    private int encodingConfig = AudioFormat.ENCODING_PCM_16BIT;

    /**
     * 采样率
     */
    private int sampleRate = 16000;


    public RecordConfig() {
    }

    public RecordConfig(RecordFormat format) {
        this.format = format;
    }

    /**
     * @param format         录音文件的格式
     * @param channelConfig  声道配置
     *                       单声道：See {@link AudioFormat#CHANNEL_IN_MONO}
     *                       双声道：See {@link AudioFormat#CHANNEL_IN_STEREO}
     * @param encodingConfig 位宽配置
     *                       8Bit： See {@link AudioFormat#ENCODING_PCM_8BIT}
     *                       16Bit: See {@link AudioFormat#ENCODING_PCM_16BIT},
     * @param sampleRate     采样率 hz: 8000/16000/44100
     */
    public RecordConfig(RecordFormat format, int channelConfig, int encodingConfig, int sampleRate) {
        this.format = format;
        this.channelConfig = channelConfig;
        this.encodingConfig = encodingConfig;
        this.sampleRate = sampleRate;
    }



    /**
     * 获取当前录音的采样位宽 单位bit
     *
     * @return 采样位宽 0: error
     */
    public int getEncoding() {

        if (encodingConfig == AudioFormat.ENCODING_PCM_8BIT) {
            return 8;
        } else if (encodingConfig == AudioFormat.ENCODING_PCM_16BIT) {
            return 16;
        } else {
            return 0;
        }
    }

    /**
     * 获取当前录音的采样位宽 单位bit
     *
     * @return 采样位宽 0: error
     */
    public int getRealEncoding() {
        if (encodingConfig == AudioFormat.ENCODING_PCM_8BIT) {
            return 8;
        } else if (encodingConfig == AudioFormat.ENCODING_PCM_16BIT) {
            return 16;
        } else {
            return 0;
        }
    }

    /**
     * 当前的声道数
     *
     * @return 声道数： 0：error
     */
    public int getChannelCount() {
        if (channelConfig == AudioFormat.CHANNEL_IN_MONO) {
            return 1;
        } else if (channelConfig == AudioFormat.CHANNEL_IN_STEREO) {
            return 2;
        } else {
            return 0;
        }
    }

    //get&set

    public RecordFormat getFormat() {
        return format;
    }

    public RecordConfig setFormat(RecordFormat format) {
        this.format = format;
        return this;
    }

    public int getChannelConfig() {
        return channelConfig;
    }

    public RecordConfig setChannelConfig(int channelConfig) {
        this.channelConfig = channelConfig;
        return this;
    }

    public int getEncodingConfig() {

        return encodingConfig;
    }

    public RecordConfig setEncodingConfig(int encodingConfig) {
        this.encodingConfig = encodingConfig;
        return this;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public RecordConfig setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
        return this;
    }


    @Override
    public String toString() {
        return String.format(Locale.getDefault(), "录制格式： %s,采样率：%sHz,位宽：%s bit,声道数：%s", format, sampleRate, getEncoding(), getChannelCount());
    }

    public enum RecordFormat {

        /**
         * pcm格式
         */
        PCM(".pcm");

        private String extension;

        public String getExtension() {
            return extension;
        }

        RecordFormat(String extension) {
            this.extension = extension;
        }
    }
}
