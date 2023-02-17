package com.imagepicker;

import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReadableMap;

import java.util.Map;
import java.util.HashMap;

public class ImagePickerModule extends ReactContextBaseJavaModule {

    final ImagePickerModuleImpl imagePickerModuleImpl;

    ImagePickerModule(ReactApplicationContext context) {
        super(context);
        imagePickerModuleImpl = new ImagePickerModuleImpl(context);
    }

    @Override
    public String getName() {
        return ImagePickerModuleImpl.NAME;
    }

    @ReactMethod
    public void launchCamera(final ReadableMap options, final Callback callback) {
        imagePickerModuleImpl.launchCamera(options, callback);
    }

    @ReactMethod
    public void launchImageLibrary(final ReadableMap options, final Callback callback) {
        imagePickerModuleImpl.launchImageLibrary(options, callback);
    }
}
