THIS_DIR := $(call my-dir)

include $(REACT_ANDROID_DIR)/Android-prebuilt.mk

include $(REACT_ANDROID_DIR)/../../react-native-image-picker/android/build/generated/source/codegen/jni/Android.mk 

include $(CLEAR_VARS)

LOCAL_PATH := $(THIS_DIR)

# You can customize the name of your application .so file here.
LOCAL_MODULE := example_appmodules

LOCAL_C_INCLUDES := $(LOCAL_PATH)
LOCAL_SRC_FILES := $(wildcard $(LOCAL_PATH)/*.cpp)
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)

LOCAL_SHARED_LIBRARIES := \
  libfabricjni \
  libfbjni \
  libfolly_runtime \
  libglog \
  libjsi \
  libreact_codegen_rncore \
  libreact_debug \
  libreact_nativemodule_core \
  libreact_render_componentregistry \
  libreact_render_core \
  libreact_render_debug \
  libreact_render_graphics \
  librrc_view \
  libruntimeexecutor \
  libturbomodulejsijni \
  libyoga

# ImagePicker
LOCAL_SHARED_LIBRARIES += libreact_codegen_imagepicker

LOCAL_CFLAGS := -DLOG_TAG=\"ReactNative\" -fexceptions -frtti -std=c++17 -Wall

include $(BUILD_SHARED_LIBRARY)
