//
//  RPPG.cpp
//  Heartbeat
//
//  Created by Philipp Rouast on 21/05/2016.
//  Copyright © 2016 Philipp Roüast. All rights reserved.
//

#include "RPPG.hpp"

#include <android/log.h>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/core/core.hpp>
#include <opencv2/video/video.hpp>
#include <stdtostring.h>

#include "opencv.hpp"

using namespace cv;
using namespace std;

#define LOW_BPM 42
#define HIGH_BPM 240
#define REL_MIN_FACE_SIZE 0.2
#define SEC_PER_MIN 60
#define MAX_CORNERS 10
#define MIN_CORNERS 5
#define QUALITY_LEVEL 0.01
#define MIN_DISTANCE 25

#define LOG_TAG "Heartbeat::RPPG"
#define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__))

bool RPPG::load(jobject listener, JNIEnv *jenv,
                int algorithm,
                const int width, const int height, const int offset,const int minLight, const int maxLight,const double timeBase, const int downsample,
                const double samplingFrequency, const double rescanFrequency,
                const int minSignalSize, const int maxSignalSize,
                const string &logPath, const string &classifierPath,
                const bool log, const bool gui) {

    this->algorithm = (RPPGAlgorithm)algorithm;
    this->guiMode = gui;
    this->lastSamplingTime = 0;
    this->logMode = log;
    this->minFaceSize = Size(min(width, height) * REL_MIN_FACE_SIZE, min(width, height) * REL_MIN_FACE_SIZE);
    this->maxSignalSize = maxSignalSize;
    this->minSignalSize = minSignalSize;
    this->Height = height;
    this->Width = width;
    this->Offset = offset;
    this->min_Light = minLight;
    this->max_Light = maxLight;
    this->rescanFlag = false;
    this->rescanFrequency = rescanFrequency;
    this->samplingFrequency = samplingFrequency;
    this->timeBase = timeBase;

    LOGD("Using algorithm %f", algorithm);

    // Save reference to Java VM
    jenv->GetJavaVM(&jvm);

    // Save global reference to listener object
    this->listener = jenv->NewGlobalRef(listener);

    // Load classifiers
    classifier.load(classifierPath);
    
    // Setting up logfilepath
    std::ostringstream path_1;
    path_1 << logPath << "_a=" << algorithm << "_min=" << minSignalSize << "_max=" << maxSignalSize << "_ds=" << downsample;
    this->logfilepath = path_1.str();
    
    // Logging bpm according to sampling frequency
    std::ostringstream path_2;
    path_2 << logfilepath << "_bpm.csv";
    logfile.open(path_2.str().c_str());
    logfile << "time;face_valid;mean;min;max\n";
    logfile.flush();
    
    // Logging bpm detailed
    std::ostringstream path_3;
    path_3 << logfilepath << "_bpmAll.csv";
    logfileDetailed.open(path_3.str().c_str());
    logfileDetailed << "time;face_valid;bpm\n";
    logfileDetailed.flush();

    return true;
}

void RPPG::exit(JNIEnv *jenv) {
    jenv->DeleteGlobalRef(listener);
    listener = NULL;
    logfile.close();
    logfileDetailed.close();
}

int RPPG::processFrame(Mat &frameRGB, Mat &frameGray, int64_t time) {

    // Set time
    this->time = time;

     Mat temprgb, tempgray;
     double output_bpms = 0;


    cv::cvtColor(frameRGB, tempgray, CV_RGB2GRAY);
    if (!faceValid) {
        
        LOGD("Not valid, finding a new face");
        
        lastScanTime = time;
        detectFace(frameRGB, frameGray);
        
    } else if ((time - lastScanTime) * timeBase >= 1/rescanFrequency) {
        
        LOGD("Valid, but rescanning face");
        
        lastScanTime = time;
        detectFace(frameRGB, frameGray);
        rescanFlag = true;

    } else {
        
        LOGD("Tracking face");
        
        trackFace(frameGray);
    }
    
    if (faceValid) {

        // Update fps
        fps = getFps(t, timeBase);
        //local_FPS = fps;

        // Remove old values from buffer
        while (s.rows > fps * maxSignalSize) {
            push(s);
            push(t);
            push(re);
        }

        assert(s.rows == t.rows && s.rows == re.rows);

        // New values
        Scalar means = mean(frameRGB, mask);

        // Add new values to raw signal buffer
        double values[] = {means(0), means(1), means(2)};
        s.push_back(Mat(1, 3, CV_64F, values));
        t.push_back<long>(time);

        // Save rescan flag
        re.push_back<bool>(rescanFlag);

        // Update fps
        fps = getFps(t, timeBase);

        // Update band spectrum limits
        low = (int)(s.rows * LOW_BPM / SEC_PER_MIN / fps);
        high = (int)(s.rows * HIGH_BPM / SEC_PER_MIN / fps) + 1;
        
        // If valid signal is large enough: estimate
        if (s.rows >= fps * minSignalSize) {

            // Filtering
            switch (algorithm) {
                case g:
                    extractSignal_g();
                    break;
                case pca:
                    extractSignal_pca();
                    break;
                case xminay:
                    extractSignal_xminay();
                    break;
            }

            // PSD estimation
            estimateHeartrate();
            //local_BPMS = meanBpm;

            // Log
            log();
        }

        int light;
        light = roilight(tempgray);
        if (abs((box.tl().x + box.width / 2) - Width / 2) > Offset || abs((box.tl().y + box.height / 2) - Height / 2) > Offset) {
                    //putText(frameRGB, "Please keep your face in contour!", Point(Width/2-300, Height/2 - 10), FONT_HERSHEY_PLAIN, 3, RED, 2);
                     output_bpms = 2.0;
        }else if(light<min_Light || light > max_Light){
            //putText(frameRGB, "Please adjust the light on your face", Point(Width/2-300, Height/2 - 10), FONT_HERSHEY_PLAIN, 3, RED, 2);
            output_bpms = 3.0;
        }else if (guiMode) {
            draw(frameRGB);
            output_bpms = meanBpm;
        }else{
            output_bpms = 1.0;
        }

    }else {
        output_bpms = 1.0;
    }

    if (!guiMode) {
        // Indicator
        frameRGB.setTo(BLACK);
        // circle(frameRGB, Point(1250, 100), 25, faceValid ? GREEN : RED, -1, 8, 0);
    }

    rescanFlag = false;
    
    frameGray.copyTo(lastFrameGray);
    if (output_bpms >= 10 && output_bpms <= 200 && fps > 5.0) {
            bpms_arr.push_back(output_bpms);
    }
    else {
        bpms_arr.clear();
    }

    return int(output_bpms*10)*1000+int(fps*10);
}

double RPPG::outputreference() {
    double bpms_slice[4];

    vector<double> bpms_temp;
    double  bpms_total[100][2];
    for (int i = 1; i < bpms_arr.size(); i++) {
        if (bpms_arr[i] != bpms_arr[i - 1]) {
            bpms_temp.push_back(bpms_arr[i]);
        }
    }

    for (int j = 0; j < bpms_temp.size() - 4; j++) {
        double total = 0;
        double mean = 0;
        double variance = 0;
        for (int k = 0; k < 4; k++) {
            bpms_slice[k] = bpms_temp[j + k];
            total += bpms_temp[j + k];
        }

        mean = total / 4;

        for (int i = 0; i < 4; i++) {
            variance += pow(bpms_temp[i] - mean, 2);
        }
        variance = variance / 4;

        bpms_total[j][0] = mean;
        bpms_total[j][1] = variance;
    }

    double min_variance = bpms_total[0][1];
    double result_bpms = bpms_total[0][0];
    for (int i = 1; i < bpms_temp.size() - 4; i++) {
        if (bpms_total[i][1] < min_variance) {
            min_variance = bpms_total[i][1];
            result_bpms = bpms_total[i][0];
        }
    }
    return result_bpms;
}

//double RPPG::temperature(int age, double std_time_diff, double sleep_time_diff,int modechoose,
//    int hr_standard,int gender_flag ,double measure_interval, double store_bpms, double store_te, double now_bpms) {
double RPPG::temperature(int age,double measure_time_diff, double sleep_time_diff, int modechoose, int hr_standard,
                                                       int gender_flag, double measure_interval, double store_bpms, double store_te, double now_bpms){
    int age_flag;
    if (age <= 29) {
        age_flag = 1;
    }
    else if (age <= 39) {
        age_flag = 2;
    }
    else if (age <= 59) {
        age_flag = 3;
    }
    else {
        age_flag = 4;
    }

    double hr_predict = hr_standard + hr_delta(sleep_time_diff, age_flag) - hr_delta(measure_time_diff, age_flag);
    int fever_flag;
    if (now_bpms - hr_predict > 11) {    // 如果心率实际变化比预测变化大
        fever_flag = 0; //可能发烧了
    }
    else {
        fever_flag = 1; //判断未发烧
    }

    double bt_st = 36.47 + (0.2197 * sin(0.03134 * age + 2.507) + 0.02648 * sin(0.1318 * age - 1.46))
        - 0.1 * cos(sleep_time_diff * 0.2661) - 0.39 * sin(sleep_time_diff * 0.2661)
        + 0.04 * cos(sleep_time_diff * 0.5322) - 0.05 * sin(sleep_time_diff * 0.5322) + (0.05 * gender_flag);
    double bt_hr = bt_st + (1 - 0.4409 * pow(age, 0.1507) * fever_flag) * (now_bpms - hr_predict) / (8.694 * exp(-0.03897 * age) + 5.432 * exp(-0.0017 * age) + 0.25 * gender_flag);

    double delta_bt = (8.694 * exp(-0.03897 * age) + 5.432 * exp(-0.0017 * age) + 0.25 * gender_flag);

    double diff_bpms = abs(store_bpms - now_bpms);

    if (measure_interval <= 20) {
        if (delta_bt >= 2.0 || diff_bpms == 0) {
            if (measure_interval <= 4) {
                delta_bt = 0;
            }
            else if (measure_interval <= 10) {
                delta_bt = 0.15;
            }
            else {
                delta_bt = 0.1;
            }
        }
    }

    double result_te = store_te + delta_bt;
    if (bt_hr < 36) {
        bt_hr = 36.0 + rand() / double(RAND_MAX);
    }



    if (modechoose == 1) {
        return result_te;
    }
    else {
        return bt_hr;
    }

}

double RPPG::hr_delta(double time, int flag) {
    double delta = 0;
    switch (flag) {
    case 1:
        delta = 9.272 * sin(0.2556 * time - 2.454) + 16.95 * sin(0.00739 * time + 0.09574)
            + 3.864 * sin(0.5958 * time + 1.279);
        break;
    case 2:
        delta = 106 * sin(0.0049 * time + 0.0194) + 11.57 * sin(0.3075 * time + 3.388)
            + 2.466 * sin(0.7002 * time + 0.3811);
        break;
    case 3:
        delta = 15.35 * sin(0.1308 * time - 0.6176) + 297.3 * sin(0.5407 * time + 0.07197)
            + 297.2 * sin(0.5425 * time + 3.183);
        break;
    default:
        delta = 5.707 * sin(0.2844 * time - 2.59) + 5.67 * sin(0.01665 * time + 0.404)
            + 1.527 * sin(0.5567 * time + 2.67);

    }

    return delta;
}

void RPPG::detectFace(Mat &frameRGB, Mat &frameGray) {
    
    LOGD("Scanning for faces…");
    
    // Detect faces with Haar classifier
    vector<Rect> boxes;
    classifier.detectMultiScale(frameGray, boxes, 1.1, 2, CV_HAAR_SCALE_IMAGE, minFaceSize);
    
    if (boxes.size() > 0) {
        
        LOGD("Found a face");
        
        setNearestBox(boxes);
        detectCorners(frameGray);
        updateROI();
        updateMask(frameGray);
        faceValid = true;

    } else {
        
        LOGD("Found no face");
        invalidateFace();
    }
}

int RPPG::roilight(Mat &frameGray) {
    int i, j;
    int resultlight = 0;
    Scalar frame_sca;
    for (i = 1; i < roi.width; ++i) {
        for (j = 1; j < roi.height; ++j) {
            //result += frameGray.ptr<uchar>(1)[1];
            resultlight += frameGray.ptr<uchar>(i+roi.tl().x)[j+roi.tl().y];
        }
    }
    resultlight = (resultlight / (roi.width * roi.height));

    return resultlight;
}

void RPPG::setNearestBox(vector<Rect> boxes) {
    int index = 0;
    Point p = box.tl() - boxes.at(0).tl();
    int min = p.x * p.x + p.y * p.y;
    for (int i = 1; i < boxes.size(); i++) {
        p = box.tl() - boxes.at(i).tl();
        int d = p.x * p.x + p.y * p.y;
        if (d < min) {
            min = d;
            index = i;
        }
    }
    box = boxes.at(index);
}

void RPPG::detectCorners(Mat &frameGray) {
    
    // Define tracking region
    Mat trackingRegion = Mat::zeros(frameGray.rows, frameGray.cols, CV_8UC1);
    Point points[1][4];
    points[0][0] = Point(box.tl().x + 0.22 * box.width,
                         box.tl().y + 0.21 * box.height);
    points[0][1] = Point(box.tl().x + 0.78 * box.width,
                         box.tl().y + 0.21 * box.height);
    points[0][2] = Point(box.tl().x + 0.70 * box.width,
                         box.tl().y + 0.50 * box.height);
    points[0][3] = Point(box.tl().x + 0.30 * box.width,
                         box.tl().y + 0.50 * box.height);
    const Point *pts[1] = {points[0]};
    int npts[] = {4};
    fillPoly(trackingRegion, pts, npts, 1, WHITE);
    
    // Apply corner detection
    goodFeaturesToTrack(frameGray,
                        corners,
                        MAX_CORNERS,
                        QUALITY_LEVEL,
                        MIN_DISTANCE,
                        trackingRegion,
                        3,
                        false,
                        0.04);
}

void RPPG::trackFace(Mat &frameGray) {
    
    // Make sure enough corners are available
    if (corners.size() < MIN_CORNERS) {
        detectCorners(frameGray);
    }

    Contour2f corners_1;
    Contour2f corners_0;
    vector<uchar> cornersFound_1;
    vector<uchar> cornersFound_0;
    Mat err;

    // Track face features with Kanade-Lucas-Tomasi (KLT) algorithm
    calcOpticalFlowPyrLK(lastFrameGray, frameGray, corners, corners_1, cornersFound_1, err);

    // Backtrack once to make it more robust
    calcOpticalFlowPyrLK(frameGray, lastFrameGray, corners_1, corners_0, cornersFound_0, err);

    // Exclude no-good corners
    Contour2f corners_1v;
    Contour2f corners_0v;
    for (size_t j = 0; j < corners.size(); j++) {
        if (cornersFound_1[j] && cornersFound_0[j]
            && cv::norm(corners[j]-corners_0[j]) < 2) {
            corners_0v.push_back(corners_0[j]);
            corners_1v.push_back(corners_1[j]);
        } else {
            LOGD("Mis!");
        }
    }

    if (corners_1v.size() >= MIN_CORNERS) {

        // Save updated features
        corners = corners_1v;

        // Estimate affine transform
        Mat transform = estimateRigidTransform(corners_0v, corners_1v, false);

        if (transform.total() > 0) {

            // Update box
            Contour2f boxCoords;
            boxCoords.push_back(box.tl());
            boxCoords.push_back(box.br());
            Contour2f transformedBoxCoords;
            cv::transform(boxCoords, transformedBoxCoords, transform);
            box = Rect(transformedBoxCoords[0], transformedBoxCoords[1]);

            // Update roi
            Contour2f roiCoords;
            roiCoords.push_back(roi.tl());
            roiCoords.push_back(roi.br());
            Contour2f transformedRoiCoords;
            cv::transform(roiCoords, transformedRoiCoords, transform);
            roi = Rect(transformedRoiCoords[0], transformedRoiCoords[1]);

            updateMask(frameGray);
        }

    } else {

        LOGD("Tracking failed! Not enough corners left.");
        invalidateFace();
    }
}

void RPPG::updateROI() {
    this->roi = Rect(Point(box.tl().x + 0.3 * box.width, box.tl().y + 0.1 * box.height),
                     Point(box.tl().x + 0.7 * box.width, box.tl().y + 0.25 * box.height));
}

void RPPG::updateMask(Mat &frameGray) {
    
    LOGD("Update mask");

    mask = cv::Mat::zeros(frameGray.rows, frameGray.cols, CV_8U);
    rectangle(mask, this->roi, WHITE, FILLED);
}

void RPPG::invalidateFace() {

    s = Mat1d();
    s_f = Mat1d();
    t = Mat1d();
    re = Mat1b();
    powerSpectrum = Mat1d();
    faceValid = false;
}

void RPPG::extractSignal_g() {

    // Denoise
    Mat s_den = Mat(s.rows, 1, CV_64F);
    denoise(s.col(1), re, s_den);

    // Normalise
    normalization(s_den, s_den);

    // Detrend
    Mat s_det = Mat(s_den.rows, s_den.cols, CV_64F);
    detrend(s_den, s_det, fps);

    // Moving average
    Mat s_mav = Mat(s_det.rows, s_det.cols, CV_64F);
    movingAverage(s_det, s_mav, 3, fmax(floor(fps/6), 2));

    s_mav.copyTo(s_f);

    // Logging
    if (logMode) {
        std::ofstream log;
        std::ostringstream filepath;
        filepath << logfilepath << "_signal_" << time << ".csv";
        log.open(filepath.str().c_str());
        log << "g;g_den;g_det;g_mav\n";
        for (int i = 0; i < s.rows; i++) {
            log << s.at<double>(i, 1) << ";";
            log << s_den.at<double>(i, 0) << ";";
            log << s_det.at<double>(i, 0) << ";";
            log << s_mav.at<double>(i, 0) << "\n";
        }
        log.close();
    }
}

void RPPG::extractSignal_pca() {

    // Denoise signals
    Mat s_den = Mat(s.rows, s.cols, CV_64F);
    denoise(s, re, s_den);

    // Normalize signals
    normalization(s_den, s_den);

    // Detrend
    Mat s_det = Mat(s.rows, s.cols, CV_64F);
    detrend(s_den, s_det, fps);

    // PCA to reduce dimensionality
    Mat s_pca = Mat(s.rows, 1, CV_32F);
    Mat pc = Mat(s.rows, s.cols, CV_32F);
    pcaComponent(s_det, s_pca, pc, low, high);

    // Moving average
    Mat s_mav = Mat(s.rows, 1, CV_32F);
    movingAverage(s_pca, s_mav, 3, fmax(floor(fps/6), 2));

    s_mav.copyTo(s_f);

    // Logging
    if (logMode) {
        std::ofstream log;
        std::ostringstream filepath;
        filepath << logfilepath << "_signal_" << time << ".csv";
        log.open(filepath.str().c_str());
        log << "re;r;g;b;r_den;g_den;b_den;r_det;g_det;b_det;pc1;pc2;pc3;s_pca;s_mav\n";
        for (int i = 0; i < s.rows; i++) {
            log << re.at<bool>(i, 0) << ";";
            log << s.at<double>(i, 0) << ";";
            log << s.at<double>(i, 1) << ";";
            log << s.at<double>(i, 2) << ";";
            log << s_den.at<double>(i, 0) << ";";
            log << s_den.at<double>(i, 1) << ";";
            log << s_den.at<double>(i, 2) << ";";
            log << s_det.at<double>(i, 0) << ";";
            log << s_det.at<double>(i, 1) << ";";
            log << s_det.at<double>(i, 2) << ";";
            log << pc.at<double>(i, 0) << ";";
            log << pc.at<double>(i, 1) << ";";
            log << pc.at<double>(i, 2) << ";";
            log << s_pca.at<double>(i, 0) << ";";
            log << s_mav.at<double>(i, 0) << "\n";
        }
        log.close();
    }
}

void RPPG::extractSignal_xminay() {

    // Denoise signals
    Mat s_den = Mat(s.rows, s.cols, CV_64F);
    denoise(s, re, s_den);

    // Normalize raw signals
    Mat s_n = Mat(s_den.rows, s_den.cols, CV_64F);
    normalization(s_den, s_n);

    // Calculate X_s signal
    Mat x_s = Mat(s.rows, s.cols, CV_64F);
    addWeighted(s_n.col(0), 3, s_n.col(1), -2, 0, x_s);

    // Calculate Y_s signal
    Mat y_s = Mat(s.rows, s.cols, CV_64F);
    addWeighted(s_n.col(0), 1.5, s_n.col(1), 1, 0, y_s);
    addWeighted(y_s, 1, s_n.col(2), -1.5, 0, y_s);

    // Bandpass
    Mat x_f = Mat(s.rows, s.cols, CV_32F);
    bandpass(x_s, x_f, low, high);
    x_f.convertTo(x_f, CV_64F);
    Mat y_f = Mat(s.rows, s.cols, CV_32F);
    bandpass(y_s, y_f, low, high);
    y_f.convertTo(y_f, CV_64F);

    // Calculate alpha
    Scalar mean_x_f;
    Scalar stddev_x_f;
    meanStdDev(x_f, mean_x_f, stddev_x_f);
    Scalar mean_y_f;
    Scalar stddev_y_f;
    meanStdDev(y_f, mean_y_f, stddev_y_f);
    double alpha = stddev_x_f.val[0]/stddev_y_f.val[0];

    // Calculate signal
    Mat xminay = Mat(s.rows, 1, CV_64F);
    addWeighted(x_f, 1, y_f, -alpha, 0, xminay);

    // Moving average
    movingAverage(xminay, s_f, 3, fmax(floor(fps/6), 2));

    // Logging
    if (logMode) {
        std::ofstream log;
        std::ostringstream filepath;
        filepath << logfilepath << "_signal_" << time << ".csv";
        log.open(filepath.str().c_str());
        log << "r;g;b;r_den;g_den;b_den;x_s;y_s;x_f;y_f;s;s_f\n";
        for (int i = 0; i < s.rows; i++) {
            log << s.at<double>(i, 0) << ";";
            log << s.at<double>(i, 1) << ";";
            log << s.at<double>(i, 2) << ";";
            log << s_den.at<double>(i, 0) << ";";
            log << s_den.at<double>(i, 1) << ";";
            log << s_den.at<double>(i, 2) << ";";
            log << x_s.at<double>(i, 0) << ";";
            log << y_s.at<double>(i, 0) << ";";
            log << x_f.at<double>(i, 0) << ";";
            log << y_f.at<double>(i, 0) << ";";
            log << xminay.at<double>(i, 0) << ";";
            log << s_f.at<double>(i, 0) << "\n";
        }
        log.close();
    }
}

void RPPG::estimateHeartrate() {

    int temp_HR = 0;
    for (int i = 0; i < s_f.rows-1;i++) {
        if (s_f[i][0] * s_f[i + 1][0] < 0) {
            temp_HR++;
        }
    }
    const int total = s_f.rows;
    bpm = temp_HR * fps / (2 * total) * SEC_PER_MIN;
    bpms.push_back(bpm);

    if ((time - lastSamplingTime) * timeBase >= 1 / samplingFrequency) {
        lastSamplingTime = time;

        cv::sort(bpms, bpms, SORT_EVERY_COLUMN);

        // average calculated BPMs since last sampling time
        meanBpm = mean(bpms)(0);
        minBpm = bpms.at<double>(0, 0);
        maxBpm = bpms.at<double>(bpms.rows - 1, 0);

        bpms.pop_back(bpms.rows);
    }
}

//void RPPG::estimateHeartrate() {
//
//    powerSpectrum = Mat(s_f.size(), CV_32F);
//    timeToFrequency(s_f, powerSpectrum, true);
//
//    // band mask
//    const int total = s_f.rows;
//    Mat bandMask = Mat::zeros(s_f.size(), CV_8U);
//    bandMask.rowRange(min(low, total), min(high, total) + 1).setTo(ONE);
//
//    if (!powerSpectrum.empty()) {
//
//        // grab index of max power spectrum
//        double min, max;
//        Point pmin, pmax;
//        minMaxLoc(powerSpectrum, &min, &max, &pmin, &pmax, bandMask);
//
//        // calculate BPM
//        bpm = pmax.y * fps / total * SEC_PER_MIN;
//        bpms.push_back(bpm);
//
//        // calculate BPM based on weighted squares power spectrum
//        //double weightedSquares = weightedSquaresMeanIndex(powerSpectrum, low, high);
//        //double bpm_ws = weightedSquares * fps / total * SEC_PER_MIN;
//        //bpms_ws.push_back(bpm_ws);
//
//        LOGD("FPS=%f Vals=%d Peak=%d BPM=%f", fps, powerSpectrum.rows, pmax.y, bpm);
//
//        // Logging
//        if (logMode) {
//            std::ofstream log;
//            std::ostringstream filepath;
//            filepath << logfilepath << "_estimation_" << time << ".csv";
//            log.open(filepath.str().c_str());
//            log << "i;powerSpectrum\n";
//            for (int i = 0; i < powerSpectrum.rows; i++) {
//                if (low <= i && i <= high) {
//                    log << i << ";";
//                    log << powerSpectrum.at<double>(i, 0) << "\n";
//                }
//            }
//            log.close();
//        }
//    }
//
//    if ((time - lastSamplingTime) * timeBase >= 1/samplingFrequency) {
//        lastSamplingTime = time;
//
//        cv::sort(bpms, bpms, SORT_EVERY_COLUMN);
//
//        // average calculated BPMs since last sampling time
//        meanBpm = mean(bpms)(0);
//        minBpm = bpms.at<double>(0, 0);
//        maxBpm = bpms.at<double>(bpms.rows-1, 0);
//
//        // cv::sort(bpms_ws, bpms_ws, SORT_EVERY_COLUMN);
//        // meanBpm_ws = mean(bpms_ws)(0);
//        // minBpm_ws = bpms_ws.at<double>(0, 0);
//        // maxBpm_ws = bpms_ws.at<double>(bpms_ws.rows-1, 0);
//
//        callback(time, meanBpm, minBpm, maxBpm);
//
//        bpms.pop_back(bpms.rows);
//        // bpms_ws.pop_back(bpms_ws.rows);
//    }
//}

void RPPG::log() {

    if (lastSamplingTime == time || lastSamplingTime == 0) {
        logfile << time << ";";
        logfile << faceValid << ";";
        logfile << meanBpm << ";";
        logfile << minBpm << ";";
        logfile << maxBpm << "\n";
        logfile.flush();
    }

    logfileDetailed << time << ";";
    logfileDetailed << faceValid << ";";
    logfileDetailed << bpm << "\n";
    logfileDetailed.flush();
}

void RPPG::callback(int64_t time, double meanBpm, double minBpm, double maxBpm) {

    JNIEnv *jenv;
    int stat = jvm->GetEnv((void **)&jenv, JNI_VERSION_1_6);

    if (stat == JNI_EDETACHED) {
        LOGD("GetEnv: not attached");
        if (jvm->AttachCurrentThread(&jenv, NULL) != 0) {
            LOGD("GetEnv: Failed to attach");
        } else {
            LOGD("GetEnv: Attached to %d", jenv);
        }
    } else if (stat == JNI_OK) {
        //
    } else if (stat == JNI_EVERSION) {
        LOGD("GetEnv: version not supported");
    }

    // Return object

    // Get Return object class reference
    jclass returnObjectClassRef = jenv->FindClass("com/prouast/heartbeat/RPPGResult");

    // Get Return object constructor method
    jmethodID constructorMethodID = jenv->GetMethodID(returnObjectClassRef, "<init>", "(JDDD)V");

    // Create Info class
    jobject returnObject = jenv->NewObject(returnObjectClassRef, constructorMethodID, (jlong)time, meanBpm, minBpm, maxBpm);

    // Listener

    // Get the Listener class reference
    jclass listenerClassRef = jenv->GetObjectClass(listener);

    // Use Listener class reference to load the eventOccurred method
    jmethodID listenerEventOccuredMethodID = jenv->GetMethodID(listenerClassRef, "onRPPGResult", "(Lcom/prouast/heartbeat/RPPGResult;)V");

    // Invoke listener eventOccurred
    jenv->CallVoidMethod(listener, listenerEventOccuredMethodID, returnObject);

    // Cleanup
    jenv->DeleteLocalRef(returnObject);
}

void RPPG::draw(Mat &frameRGB) {

    // Draw roi
    rectangle(frameRGB, roi, GREEN);

    // Draw face shape
    //ellipse(frameRGB,
    //        Point(box.tl().x + box.width / 2.0, box.tl().y + box.height / 2.0),
    //        Size(box.width / 2.5, box.height / 2.0),
    //        0, 0, 360, GREEN);

    // Draw bounding box
    //rectangle(frameRGB, box, RED);

    // Draw signal
    if (!s_f.empty() && !powerSpectrum.empty()) {

        // Display of signals with fixed dimensions
        double displayHeight = box.height/2.0;
        double displayWidth = box.width*0.8;

        // Draw signal
        double vmin, vmax;
        Point pmin, pmax;
        minMaxLoc(s_f, &vmin, &vmax, &pmin, &pmax);
        double heightMult = displayHeight/(vmax - vmin);
        double widthMult = displayWidth/(s_f.rows - 1);
        double drawAreaTlX = box.tl().x + box.width + 20;
        double drawAreaTlY = box.tl().y;
        Point p1(drawAreaTlX, drawAreaTlY + (vmax - s_f.at<double>(0, 0))*heightMult);
        Point p2;
        for (int i = 1; i < s_f.rows; i++) {
            p2 = Point(drawAreaTlX + i * widthMult, drawAreaTlY + (vmax - s_f.at<double>(i, 0))*heightMult);
            line(frameRGB, p1, p2, RED, 2);
            p1 = p2;
        }

        // Draw powerSpectrum
        const int total = s_f.rows;
        Mat bandMask = Mat::zeros(s_f.size(), CV_8U);
        bandMask.rowRange(min(low, total), min(high, total) + 1).setTo(ONE);
        minMaxLoc(powerSpectrum, &vmin, &vmax, &pmin, &pmax, bandMask);
        heightMult = displayHeight/(vmax - vmin);
        widthMult = displayWidth/(high - low);
        drawAreaTlX = box.tl().x + box.width + 20;
        drawAreaTlY = box.tl().y + box.height/2.0;
        p1 = Point(drawAreaTlX, drawAreaTlY + (vmax - powerSpectrum.at<double>(low, 0))*heightMult);
        for (int i = low + 1; i <= high; i++) {
            p2 = Point(drawAreaTlX + (i - low) * widthMult, drawAreaTlY + (vmax - powerSpectrum.at<double>(i, 0)) * heightMult);
            line(frameRGB, p1, p2, RED, 2);
            p1 = p2;
        }
    }

    std::stringstream ss;

     //Draw BPM text
   // if (faceValid) {
     //   ss.precision(3);
       // ss << meanBpm << " bpm";
       // putText(frameRGB, ss.str(), Point(box.tl().x, box.tl().y - 10), FONT_HERSHEY_PLAIN, 2, RED, 2);
    //}

    // Draw FPS text
   // ss.str("");
   // ss << fps << " fps";
   // putText(frameRGB, ss.str(), Point(box.tl().x, box.br().y + 40), FONT_HERSHEY_PLAIN, 2, GREEN, 2);

    // Draw corners
    for (int i = 0; i < corners.size(); i++) {
        //circle(frameRGB, corners[i], r, WHITE, -1, 8, 0);
        line(frameRGB, Point(corners[i].x-5,corners[i].y), Point(corners[i].x+5,corners[i].y), GREEN, 1);
        line(frameRGB, Point(corners[i].x,corners[i].y-5), Point(corners[i].x,corners[i].y+5), GREEN, 1);
    }
}
