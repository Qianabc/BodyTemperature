//
// Created by Philipp Rouast on 8/07/16.
//

#include "com_prouast_heartbeat_RPPG.h"
#include <android/log.h>
#include "RPPG.hpp"

#define LOG_TAG "Heartbeat::RPPG"
#define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__))

void GetJStringContent(JNIEnv *AEnv, jstring AStr, std::string &ARes) {
  if (!AStr) {
    ARes.clear();
    return;
  }
  const char *s = AEnv->GetStringUTFChars(AStr,NULL);
  ARes=s;
  AEnv->ReleaseStringUTFChars(AStr,s);
}

/*
 * Class:     com_prouast_heartbeat_RPPG
 * Method:    _initialise
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_prouast_heartbeat_RPPG__1initialise
(JNIEnv *jenv, jclass) {
    LOGD("Java_com_prouast_heartbeat_RPPG__1initialise enter");
    jlong result = 0;
    try {
        result = (jlong)new RPPG();
    } catch (...) {
        jclass je = jenv->FindClass("java/lang/Exception");
        jenv->ThrowNew(je, "Unknown exception in JNI code.");
    }
    LOGD("Java_com_prouast_heartbeat_RPPG__1initialise exit");
    return result;
}

/*
 * Class:     com_prouast_heartbeat_RPPG
 * Method:    _load
 * Signature: (JLcom/prouast/heartbeat/RPPG/RPPGListener;IIIDIDDIILjava/lang/String;Ljava/lang/String;ZZ)V
 */
JNIEXPORT void JNICALL Java_com_prouast_heartbeat_RPPG__1load
(JNIEnv *jenv, jclass, jlong self, jobject jlistener, jint jalgorithm, jint jwidth, jint jheight,
jint joffset,jint jminLight,jint jmaxLight,jdouble jtimeBase, jint jdownsample, jdouble jsamplingFrequency, jdouble jrescanFrequency,
jint jminSignalSize, jint jmaxSignalSize, jstring jlogPath, jstring jclassifierPath,
jboolean jlog, jboolean jgui) {
    LOGD("Java_com_prouast_heartbeat_RPPG__1load enter");
    bool log = jlog;
    bool gui = jgui;
    std::string logPath, classifierPath;
    try {
        GetJStringContent(jenv, jlogPath, logPath);
        GetJStringContent(jenv, jclassifierPath, classifierPath);
        ((RPPG *)self)->load(jlistener, jenv, jalgorithm, jwidth, jheight, joffset, jminLight, jmaxLight, jtimeBase, jdownsample,
                                   jsamplingFrequency, jrescanFrequency, jminSignalSize, jmaxSignalSize,
                                   logPath, classifierPath, log, gui);
    } catch (...) {
      jclass je = jenv->FindClass("java/lang/Exception");
      jenv->ThrowNew(je, "Unknown exception in JNI code.");
    }
    LOGD("Java_com_prouast_heartbeat_RPPG__1load exit");
}

/*
 * Class:     com_prouast_heartbeat_RPPG
 * Method:    _processFrame
 * Signature: (JJJJ)V
 */
JNIEXPORT jint JNICALL Java_com_prouast_heartbeat_RPPG__1processFrame
(JNIEnv *jenv, jclass, jlong self, jlong jframeRGB, jlong jframeGray, jlong jtime) {
    LOGD("Java_com_prouast_heartbeat_RPPG__1processFrame enter");
    jint result = 0;
    try {
        int64_t time = jtime;
        result = ((RPPG *)self)->processFrame(*((cv::Mat*)jframeRGB), *((cv::Mat*)jframeGray), time);
    } catch (...) {
        jclass je = jenv->FindClass("java/lang/Exception");
        jenv->ThrowNew(je, "Unknown exception in JNI code.");
    }
    LOGD("Java_com_prouast_heartbeat_RPPG__1processFrame exit");
    return result;
}

/*
  * Class:     com_prouast_heartbeat_RPPG
  * Method:    _output_reference
  * Signature: (J)V
  */
 JNIEXPORT jdouble JNICALL Java_com_prouast_heartbeat_RPPG__1outputreference
 (JNIEnv *jenv, jclass, jlong self) {
     LOGD("Java_com_prouast_heartbeat_RPPG__1output_reference enter");
     jdouble result_rpms = 0;
     try {
         result_rpms = ((RPPG *)self)->outputreference();
     } catch (...) {
         jclass je = jenv->FindClass("java/lang/Exception");
         jenv->ThrowNew(je, "Unknown exception in JNI code.");
     }
     LOGD("Java_com_prouast_heartbeat_RPPG__1output_reference exit");
     return result_rpms;
 }

/*
 * Class:     com_prouast_heartbeat_RPPG
 * Method:    _output_reference
 * Signature: (J)V
 */
//JNIEXPORT jdouble JNICALL Java_com_prouast_heartbeat_RPPG__1temperature
//(JNIEnv *jenv, jclass, jlong self, jint jage, jdouble jmeasure_time_diff, jdouble jsleep_time_diff,
//     jint jmodechoose, jint jhr_standard, jint jgender_flag, jdouble jmeasure_interval,
//     jdouble jstore_bpms, jdouble jstore_te, jdouble jnow_bpms) {
    // LOGD("Java_com_prouast_heartbeat_RPPG__1temperature enter");
    // jdouble result_te = 0;
    // try {
        // result_te = ((RPPG *)self)->temperature(jage, jmeasure_time_diff,jsleep_time_diff,
        // jmodechoose, jhr_standard, jgender_flag, jmeasure_interval, jstore_bpms, jstore_te, jnow_bpms);
    // } catch (...) {
        // jclass je = jenv->FindClass("java/lang/Exception");
        // jenv->ThrowNew(je, "Unknown exception in JNI code.");
        // return 2.0;
    // }
    // LOGD("Java_com_prouast_heartbeat_RPPG__1output_reference exit");
    // return result_te;
// }
 JNIEXPORT jdouble JNICALL Java_com_prouast_heartbeat_RPPG__1temperature
 (JNIEnv *jenv, jclass, jlong self, jint jage,jdouble jstd_time_diff, jdouble jmeasure_time_diff, jint jmodechoose, jint jhr_standard,jint jgender_flag, jdouble jmeasure_interval, jdouble jstore_bpms, jdouble jstore_te, jdouble jnow_bpms) {
     LOGD("Java_com_prouast_heartbeat_RPPG__1output_reference enter");
     jdouble result_te = 0;
     try {
         result_te = ((RPPG *)self)->temperature(jage, jstd_time_diff, jmeasure_time_diff, jmodechoose, jhr_standard, jgender_flag, jmeasure_interval, jstore_bpms, jstore_te, jnow_bpms);
     } catch (...) {
         jclass je = jenv->FindClass("java/lang/Exception");
         jenv->ThrowNew(je, "Unknown exception in JNI code.");
     }
     LOGD("Java_com_prouast_heartbeat_RPPG__1output_reference exit");
     return result_te;
 }

/*
 * Class:     com_prouast_heartbeat_RPPG
 * Method:    _exit
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_prouast_heartbeat_RPPG__1exit
(JNIEnv *jenv, jclass, jlong self) {
    LOGD("Java_com_prouast_heartbeat_RPPG__1exit enter");
    try {
        ((RPPG *)self)->exit(jenv);
    } catch (...) {
        jclass je = jenv->FindClass("java/lang/Exception");
        jenv->ThrowNew(je, "Unknown exception in JNI code.");
    }
    LOGD("Java_com_prouast_heartbeat_RPPG__1exit exit");
}