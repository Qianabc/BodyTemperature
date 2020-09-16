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
                     int width, int height, int offset, int minLight, int maxLight, int minVariance, double timeBase, int downsample,
                     double samplingFrequency, double rescanFrequency,
                     int minSignalSize, int maxSignalSize,
                     String logPath, String classifierPath,
                     boolean log, boolean gui) {
        _load(self, listener, algorithm.ordinal(), width, height, offset, minLight, maxLight, minVariance, timeBase, downsample, samplingFrequency, rescanFrequency, minSignalSize, maxSignalSize, logPath, classifierPath, log, gui);
    }

    public void exit() {
        _exit(self);
    }

    public int processFrame(long frameRGB, long frameGray, long now ) {
        return _processFrame(self, frameRGB, frameGray, now);
    }

    public double outputreference() {
        return _outputreference(self);
    }

//    public double temperature(int age, double measure_time_diff, double sleep_time_diff, int modechoose, int hr_standard,
//                                   int gender_flag, double measure_interval, double store_bpms, double store_te, double now_bpms){
//        return _temperature(self, age, measure_time_diff, sleep_time_diff, modechoose, hr_standard,
//                gender_flag, measure_interval, store_bpms, store_te, now_bpms);
//    }

    public double temperature(int age,double std_time_diff, double measure_time_diff, int modechoose, int hr_standard,
                              int gender_flag, double measure_interval, double store_bpms, double store_te, double now_bpms){
        return _temperature(self, age, std_time_diff, measure_time_diff, modechoose, hr_standard, gender_flag, measure_interval, store_bpms, store_te, now_bpms);
    }

    private long self = 0;
    private static native long _initialise();
    private native double _outputreference(long self);
    private native double _temperature(long self, int age,double std_time_diff, double measure_time_diff, int modechoose, int hr_standard,
                                       int gender_flag, double measure_interval, double store_bpms, double store_te, double now_bpms);
    private static native void _load(long self, RPPGListener listener, int algorithm, int width, int height, int offset, int minLight, int maxLight, int minvariance, double timeBase, int downsample, double samplingFrequency, double rescanFrequency, int minSignalSize, int maxSignalSize, String logPath, String classifierPath, boolean log, boolean gui);
    private native int _processFrame(long self, long frameRGB, long frameGray, long time);
    private static native void _exit(long self);
}
