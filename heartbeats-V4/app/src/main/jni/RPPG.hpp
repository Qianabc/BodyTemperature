//
//  RPPG.hpp
//  Heartbeat
//
//  Created by Philipp Rouast on 21/05/2016.
//  Copyright © 2016 Philipp Roüast. All rights reserved.
//

#ifndef RPPG_hpp
#define RPPG_hpp

#include <fstream>
#include <string>
#include <opencv2/objdetect/objdetect.hpp>
#include <stdio.h>
#include <jni.h>

using namespace cv;
using namespace std;

enum RPPGAlgorithm { g, pca, xminay };

class RPPG {
    
public:
    
    // Constructor
    RPPG() {;}
    
    // Load Settings
    bool load(jobject listener, JNIEnv *jenv,                                   // Listener and environment for Java callback
              int algorithm,
              const int width, const int height, const int offset, const int minLight, const int maxLight, const double timeBase, const int downsample,
              const double samplingFrequency, const double rescanFrequency,
              const int minSignalSize, const int maxSignalSize,
              const string &logPath, const string &classifierPath,
              const bool log, const bool gui);
    
    int processFrame(Mat &frameRGB, Mat &frameGray, int64_t time);
    double outputreference();
    //double temperature(int age, double measure_time_diff, double sleep_time_diff, int modechoose, int hr_standard,
    //       int gender_flag, double measure_interval, double store_bpms, double store_te, double now_bpms);
    double temperature(int age,double std_time_diff, double measure_time_diff, int modechoose, int hr_standard,
                                                     int gender_flag, double measure_interval, double store_bpms, double store_te, double now_bpms);
    double hr_delta(double time, int flag);
    
    void exit(JNIEnv *jenv);
    
    typedef vector<Point2f> Contour2f;
    
private:
    
    void detectFace(Mat &frameRGB, Mat &frameGray);
    void setNearestBox(vector<Rect> boxes);
    void detectCorners(Mat &frameGray);
    void trackFace(Mat &frameGray);
    void updateMask(Mat &frameGray);
    void updateROI();
    void extractSignal_g();
    void extractSignal_pca();
    void extractSignal_xminay();
    void estimateHeartrate();
    void draw(Mat &frameRGB);
    void invalidateFace();
    void log();

    int roilight(Mat &frameGray);

    void callback(int64_t now, double meanBpm, double minBpm, double maxBpm);   // Callback to Java

    // The JavaVM
    JavaVM *jvm;

    // The listener
    jobject listener;

    // The algorithm
    RPPGAlgorithm algorithm;

    // The classifiers
    CascadeClassifier classifier;

    // Settings
    Size minFaceSize;
    int maxSignalSize;
    int minSignalSize;
    int Height;
    int Width;
    int Offset;
    int min_Light;
    int max_Light;
    double rescanFrequency;
    double samplingFrequency;
    double timeBase;
    bool logMode;
    bool guiMode;

    // State variables
    int64_t time;
    double fps;
    int high;
    int64_t lastSamplingTime;
    int64_t lastScanTime;
    int low;
    int64_t now;
    bool faceValid;
    bool rescanFlag;
    
    // Tracking
    Mat lastFrameGray;
    Contour2f corners;

    // Mask
    Rect box;
    Mat1b mask;
    Rect roi;

    // Raw signal
    Mat1d s;
    Mat1d t;
    Mat1b re;

    // Estimation
    Mat1d s_f;
    Mat1d bpms;
    //Mat1d bpms_ws;
    Mat1d powerSpectrum;
    double bpm = 0.0;
    //double bpm_ws = 0.0;
    double meanBpm;
    double minBpm;
    double maxBpm;
    double output_bpms;
    vector<double> bpms_arr;
    //double meanBpm_ws;
    //double minBpm_ws;
    //double maxBpm_ws;
    
    // Logfiles
    ofstream logfile;
    ofstream logfileDetailed;
    string logfilepath;
};

#endif /* RPPG_hpp */
