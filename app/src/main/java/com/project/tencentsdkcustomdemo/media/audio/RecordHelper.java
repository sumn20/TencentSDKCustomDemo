package com.project.tencentsdkcustomdemo.media.audio;

import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;


/**
 * 录音实现类 通过AudioRecord来实现录音获取PCM数据
 */
public class RecordHelper {
    private static final String TAG = RecordHelper.class.getSimpleName();
    private volatile static RecordHelper instance;
    private volatile RecordState state = RecordState.IDLE;
    //录音状态变化监听
    private RecordStateListener recordStateListener;
    //录音数据监听
    private RecordDataListener recordDataListener;
    //录音配置
    private RecordConfig currentConfig;
    //录音线程
    private AudioRecordThread audioRecordThread;
    private Handler mainHandler = new Handler(Looper.getMainLooper());


    private RecordHelper() {
    }

    public static RecordHelper getInstance() {
        if (instance == null) {
            synchronized (RecordHelper.class) {
                if (instance == null) {
                    instance = new RecordHelper();
                }
            }
        }
        return instance;
    }

    RecordState getState() {
        return state;
    }

    public RecordConfig getCurrentConfig() {

        return currentConfig;
    }

    /**
     * 设置录音状态监听实现类
     *
     * @param recordStateListener 具体实现类
     */
    public void setRecordStateListener(RecordStateListener recordStateListener) {
        this.recordStateListener = recordStateListener;
    }

    /**
     * 设置录音数据监听实现类
     *
     * @param recordDataListener 具体实现类
     */
    public void setRecordDataListener(RecordDataListener recordDataListener) {
        this.recordDataListener = recordDataListener;
    }

    /**
     * 开始录音
     *
     * @param config
     */
    public void start(RecordConfig config) {
        if (config != null) {
            this.currentConfig = config;
        } else {
            currentConfig = new RecordConfig();
        }
        if (state != RecordState.IDLE && state != RecordState.STOP) {
            Log.e(TAG, "状态异常当前状态:" + state.name());
            return;
        }


        Log.d(TAG, "----------------开始录制 ------------------------" + currentConfig.getFormat().name());
        Log.d(TAG, "参数：" + currentConfig.toString());

        audioRecordThread = new AudioRecordThread();
        audioRecordThread.start();
    }

    /**
     * 停止录音
     */
    public void stop() {
        if (state == RecordState.IDLE) {
            Log.e(TAG, "状态异常当前状态：" + state.name());
            return;
        }
        if (state == RecordState.PAUSE) {
            state = RecordState.IDLE;
            notifyState();
        } else {
            state = RecordState.STOP;
            notifyState();
        }
    }

    /**
     * 暂停录音
     */
    void pause() {
        if (state != RecordState.RECORDING) {
            Log.e(TAG, "状态异常当前状态： " + state.name());
            return;
        }
        state = RecordState.PAUSE;
        notifyState();
    }

    /**
     * 开始录音
     */
    void resume() {
        if (state != RecordState.PAUSE) {
            Log.e(TAG, "状态异常当前状态： " + state.name());
            return;
        }
        audioRecordThread = new AudioRecordThread();
        audioRecordThread.start();
    }

    /**
     * 录音状态刷新
     */
    private void notifyState() {
        if (recordStateListener == null) {
            return;
        }
        mainHandler.post(() -> recordStateListener.onStateChange(state));
    }

    /**
     * 录音结束
     */
    private void notifyFinish() {
        Log.d(TAG, "录音结束 ");

        mainHandler.post(() -> {
            if (recordStateListener != null) {
                recordStateListener.onStateChange(RecordState.FINISH);
            }

        });
    }

    /**
     * 录音错误回调
     *
     * @param error 错误信息
     */
    private void notifyError(final String error) {
        if (recordStateListener == null) {
            return;
        }
        mainHandler.post(() -> recordStateListener.onError(error));
    }

    /**
     * 录音数据通知
     *
     * @param data pcm数据
     */
    private void notifyData(final byte[] data) {
        if (recordDataListener == null) {
            return;
        }
        mainHandler.post(() -> {
            if (recordDataListener != null) {
                recordDataListener.onData(data);
            }
        });
    }

    /**
     * 录音执行线程
     */
    private class AudioRecordThread extends Thread {
        private AudioRecord audioRecord;
        private int bufferSize;

        //参数初始化
        AudioRecordThread() {
            bufferSize = AudioRecord.getMinBufferSize(currentConfig.getSampleRate(),
                    currentConfig.getChannelConfig(), currentConfig.getEncodingConfig()) ;
            Log.d(TAG, "record buffer size = " + bufferSize);
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, currentConfig.getSampleRate(),
                    currentConfig.getChannelConfig(), currentConfig.getEncodingConfig(), bufferSize);

        }

        @Override
        public void run() {
            super.run();
            startPcmRecorder();
        }

        private void startPcmRecorder() {
            state = RecordState.RECORDING;
            notifyState();
            Log.d(TAG, "开始录制 Pcm");
            try {
                audioRecord.startRecording();
                byte[] byteBuffer = new byte[bufferSize];

                while (state == RecordState.RECORDING) {
                    audioRecord.read(byteBuffer, 0, byteBuffer.length);
                    notifyData(byteBuffer);
                }
                audioRecord.stop();
                if (state == RecordState.STOP) {
                    Log.i(TAG, "停止录音！");
                } else {
                    Log.i(TAG, "暂停！");
                }
            } catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
                notifyError("录音失败");
            }
            if (state != RecordState.PAUSE) {
                state = RecordState.IDLE;
                notifyState();
                Log.d(TAG, "录音结束");
            }
        }


    }


    /**
     * 表示当前状态
     */
    public enum RecordState {
        /**
         * 空闲状态
         */
        IDLE,
        /**
         * 录音中
         */
        RECORDING,
        /**
         * 暂停中
         */
        PAUSE,
        /**
         * 正在停止
         */
        STOP,
        /**
         * 录音流程结束（转换结束）
         */
        FINISH
    }

}
