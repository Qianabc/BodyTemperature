LOCAL_PATH := $(call my-dir)

# Path to ffmpeg
# Has to be built and Android.mk written (libavformat, libavcodec, libavutil, libswscale needed)
# See https://enoent.fr/blog/2014/06/20/compile-ffmpeg-for-android/
FFMPEG_PATH := D:/softwares/android-ndk-r10e/sources/ffmpeg-2.2.3ssss/android/arm
FFMPEG_LIB_PATH := D:/softwares/android-ndk-r10e/sources/ffmpeg-2.2.3ssss/android/arm/lib
$(warning  $(LOCAL_PATH))
# Path to OpenCV
# http://sourceforge.net/projects/opencvlibrary/files/opencv-android/3.1.0/OpenCV-3.1.0-android-sdk.zip/download
OPENCV_PATH := D:/softwares/android-ndk-r10e/sources/OpenCV-android-sdk
include $(CLEAR_VARS)
LOCAL_MODULE:= libavcodec
LOCAL_SRC_FILES:= $(FFMPEG_LIB_PATH)/libavcodec-55.so
LOCAL_EXPORT_C_INCLUDES := $(INCLUDE_PATH)
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE:= libavformat
LOCAL_SRC_FILES:= $(FFMPEG_LIB_PATH)/libavformat-55.so
LOCAL_EXPORT_C_INCLUDES := $(INCLUDE_PATH)
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE:= libswscale
LOCAL_SRC_FILES:= $(FFMPEG_LIB_PATH)/libswscale-2.so
LOCAL_EXPORT_C_INCLUDES := $(INCLUDE_PATH)
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE:= libavutil
LOCAL_SRC_FILES:= $(FFMPEG_LIB_PATH)/libavutil-52.so
LOCAL_EXPORT_C_INCLUDES := $(INCLUDE_PATH)
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE:= libavfilter
LOCAL_SRC_FILES:= $(FFMPEG_LIB_PATH)/libavfilter-4.so
LOCAL_EXPORT_C_INCLUDES := $(INCLUDE_PATH)
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE:= libswresample
LOCAL_SRC_FILES:= $(FFMPEG_LIB_PATH)/libswresample-0.so
LOCAL_EXPORT_C_INCLUDES := $(INCLUDE_PATH)
include $(PREBUILT_SHARED_LIBRAR)

include $(CLEAR_VARS)
LOCAL_MODULE:= libpostproc
LOCAL_SRC_FILES:= $(FFMPEG_LIB_PATH)/libpostproc-52.so
LOCAL_EXPORT_C_INCLUDES := $(INCLUDE_PATH)
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE:= libavdevice
LOCAL_SRC_FILES:= $(FFMPEG_LIB_PATH)/libavdevice-55.so
LOCAL_EXPORT_C_INCLUDES := $(INCLUDE_PATH)
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := FFmpegEncoder
LOCAL_LDLIBS := -llog -ljnigraphics -lz -landroid
LOCAL_C_INCLUDES += $(FFMPEG_PATH)/include
LOCAL_SRC_FILES := FFmpegEncoder.cpp com_prouast_heartbeat_FFmpegEncoder.cpp
LOCAL_SHARED_LIBRARIES := libavformat libavcodec libavutil libswscale
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
OPENCV_INSTALL_MODULES:=on
include $(OPENCV_PATH)/sdk/native/jni/OpenCV.mk
LOCAL_MODULE := RPPG
LOCAL_SRC_FILES := RPPG.cpp opencv.cpp com_prouast_heartbeat_RPPG.cpp
LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_LDLIBS := -llog -ldl
include $(BUILD_SHARED_LIBRARY)

#include $(FFMPEG_PATH)/Android.mk