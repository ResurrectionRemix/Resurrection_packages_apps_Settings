LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

#Include res dir from libraries
appcompat_dir := ../../../$(SUPPORT_LIBRARY_ROOT)/v7/appcompat/res
cardview_dir := ../../../$(SUPPORT_LIBRARY_ROOT)/v7/cardview/res
recyclerview_dir := ../../../$(SUPPORT_LIBRARY_ROOT)/v7/recyclerview/res
design_dir := ../../../$(SUPPORT_LIBRARY_ROOT)/design/res

res_dirs := res $(appcompat_dir) $(cardview_dir) $(recyclerview_dir) $(design_dir)

LOCAL_JAVA_LIBRARIES := bouncycastle conscrypt telephony-common ims-common
LOCAL_STATIC_JAVA_LIBRARIES := \
	android-support-v4 \
    	android-support-v7-appcompat \
    	android-support-v7-recyclerview \
    	android-support-v7-cardview \
	android-support-v13 \
	android-support-design \
	jsr305 \
	org.cyanogenmod.platform.internal

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
        $(call all-java-files-under, src) \
        src/com/android/settings/EventLogTags.logtags

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

LOCAL_SRC_FILES += \
        src/com/android/display/IPPService.aidl

LOCAL_AAPT_FLAGS += --extra-packages android.support.v7.appcompat:android.support.v7.cardview:android.support.v7.recyclerview:android.support.design

LOCAL_PACKAGE_NAME := Settings
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

ifneq ($(INCREMENTAL_BUILDS),)
    LOCAL_PROGUARD_ENABLED := disabled
    LOCAL_JACK_ENABLED := incremental
endif

include frameworks/opt/setupwizard/navigationbar/common.mk
include frameworks/opt/setupwizard/library/common.mk
include frameworks/base/packages/SettingsLib/common.mk

LOCAL_JAVA_LIBRARIES += org.cyanogenmod.hardware
include $(BUILD_PACKAGE)

# Use the following include to make our test apk.
ifeq (,$(ONE_SHOT_MAKEFILE))
include $(call all-makefiles-under,$(LOCAL_PATH))
endif
