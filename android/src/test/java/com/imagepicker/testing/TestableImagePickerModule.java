package com.imagepicker.testing;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.imagepicker.ImagePickerModule;
import com.imagepicker.ResponseHelper;

import java.lang.reflect.Field;

/**
 * Created by rusfearuth on 10.04.17.
 */

public class TestableImagePickerModule extends ImagePickerModule
{
    public TestableImagePickerModule(ReactApplicationContext reactContext,
                                     @StyleRes int dialogThemeId)
    {
        super(reactContext, dialogThemeId);
    }

    public void setCallback(@NonNull final Callback callback)
    {
        this.callback = callback;
    }

    public void setCameraCaptureUri(@Nullable final Uri uri)
    {
        this.cameraCaptureURI = uri;
    }
}
