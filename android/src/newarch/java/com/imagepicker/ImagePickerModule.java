package com.imagepicker;

import androidx.annotation.NonNull;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReadableMap;

public class ImagePickerModule extends NativeImagePickerSpec {

    final ImagePickerModuleImpl imagePickerModuleImpl;

    ImagePickerModule(ReactApplicationContext context) {
        super(context);
        imagePickerModuleImpl = new ImagePickerModuleImpl(context);
    }

    @Override
    @NonNull
    public String getName() {
        return ImagePickerModuleImpl.NAME;
    }

    @Override
    public void launchCamera(final ReadableMap options, final Callback callback) {
        imagePickerModuleImpl.launchCamera(options, callback);
    }

    @Override
    public void launchImageLibrary(final ReadableMap options, final Callback callback) {
        imagePickerModuleImpl.launchImageLibrary(options, callback);
    }
}
