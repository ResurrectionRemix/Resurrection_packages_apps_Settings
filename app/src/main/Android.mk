LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_PACKAGE_NAME := DU-Tweaks
LOCAL_SDK_VERSION := current
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

LOCAL_SRC_FILES := $(call all-java-files-under, java)
LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res \
    $(support_library_root_dir)/v7/appcompat/res \
    $(support_library_root_dir)/design/res

LOCAL_AAPT_FLAGS := \
    --auto-add-overlay \
    --extra-packages android.support.v7.appcompat \
    --extra-packages android.support.design

LOCAL_STATIC_JAVA_LIBRARIES := \
    android-common \
    android-support-v13 \
    android-support-v4 \
    android-support-v7-appcompat \
    android-support-design

include $(BUILD_PACKAGE)
