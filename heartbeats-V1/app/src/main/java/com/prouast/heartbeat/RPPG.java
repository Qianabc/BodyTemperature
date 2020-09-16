package com.prouast.heartbeat;

/**
 * Created by prouast on 22/05/16.
 */
public class RPPG {

    public enum RPPGAlgorithm {
        g, pca, xminay
    }

    /**
     * Listener must implement this interface.
     */
    public interface RPPGListener {
        void onRPPGResult(RPPGResult result);
    }

    public RPPG() {
        self = _initialise();
    }

    public void load(RPPGListener listener,
                     RPPGAlgorithm algorithm,
                     int width, int height, int offset, int minLight, int maxLight, double timeBase, int downsample,
                     double samplingFrequency, double rescanFrequency,
                     int minSignalSize, int maxSignalSize,
                     String logPath, String classifierPath,
                     boolean log, boolean gui) {
        _load(self, listener, algorithm.ordinal(), width, height, offset, minLight, maxLight, timeBase, downsample, samplingFrequency, rescanFrequency, minSignalSize, maxSignalSize, logPath, classifierPath, log, gui);
    }

    public void exit() {
        _exit(self);
    }

    public int processFrame(long frameRGB, long frameGray, long now ) {
        return _processFrame(self, frameRGB, frameGray, now);
    }


    private long self = 0;
    private static native long _initialise();
    private static native void _load(long self, RPPGListener listener, int algorithm, int width, int height, int offset, int minLight, int maxLight, double timeBase, int downsample, double samplingFrequency, double rescanFrequency, int minSignalSize, int maxSignalSize, String logPath, String classifierPath, boolean log, boolean gui);
    private native int _processFrame(long self, long frameRGB, long frameGray, long time);
    private static native void _exit(long self);
}
