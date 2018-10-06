	LOCAL_PATH := $(call my-dir)

	include $(CLEAR_VARS)

	LOCAL_SRC_FILES := com_example_alvin_camerasource_NativeClass.cpp

	LOCAL_LDLIBS += -llog
	LOCAL_MODULE := MyLib


	include $(BUILD_SHARED_LIBRARY)