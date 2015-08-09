LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_JAVA_LIBRARIES := bouncycastle conscrypt telephony-common ims-common
LOCAL_STATIC_JAVA_LIBRARIES := \
	android-support-v4 \
	android-support-v7-cardview \
	android-support-v13 \
	jsr305 \
	org.cyanogenmod.platform.sdk

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
        $(call all-java-files-under, src) \
        src/com/android/settings/EventLogTags.logtags

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res \
    frameworks/support/v7/cardview/res

LOCAL_SRC_FILES += \
        src/com/android/location/XT/IXTSrv.aidl \
        src/com/android/location/XT/IXTSrvCb.aidl

LOCAL_SRC_FILES += \
        src/com/android/display/IPPService.aidl

LOCAL_PACKAGE_NAME := Settings
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

LOCAL_AAPT_FLAGS := \
    --auto-add-overlay \
    --extra-packages android.support.v7.cardview

LOCAL_AAPT_INCLUDE_ALL_RESOURCES := true

include frameworks/opt/setupwizard/navigationbar/common.mk
include $(BUILD_PACKAGE)

# Use the following include to make our test apk.
ifeq (,$(ONE_SHOT_MAKEFILE))
include $(call all-makefiles-under,$(LOCAL_PATH))
endif
