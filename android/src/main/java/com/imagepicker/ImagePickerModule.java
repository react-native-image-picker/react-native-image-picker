package com.imagepicker;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import android.provider.OpenableColumns;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.module.annotations.ReactModule;

import static com.imagepicker.Utils.*;

@ReactModule(name = ImagePickerModule.NAME)
public class ImagePickerModule extends ReactContextBaseJavaModule implements ActivityEventListener {
    static final String NAME = "ImagePickerManager";

    static final int REQUEST_LAUNCH_IMAGE_CAPTURE = 13001;
    static final int REQUEST_LAUNCH_IMAGE_LIBRARY = 13002;
    static final int REQUEST_LAUNCH_VIDEO_LIBRARY = 13003;
    static final int REQUEST_LAUNCH_VIDEO_CAPTURE = 13004;

    final ReactApplicationContext reactContext;

    Callback callback;

    Options options;
    Uri cameraCaptureURI;

    public ImagePickerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.reactContext.addActivityEventListener(this);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @ReactMethod
    public void launchCamera(final ReadableMap options, final Callback callback) {
        if (!isCameraAvailable(reactContext)) {
            callback.invoke(getErrorMap("Camera not available"));
            return;
        }

        final Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            callback.invoke(getErrorMap("can't find current Activity"));
            return;
        }

        this.callback = callback;
        this.options = new Options(options);

        if (!hasPermission(currentActivity)) {
            callback.invoke(getErrorMap("Permissions weren't granted"));
            return;
        }

        int requestCode;
        Intent cameraIntent;

        if (this.options.pickVideo) {
            requestCode = REQUEST_LAUNCH_VIDEO_CAPTURE;
            cameraIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            cameraIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, this.options.videoQuality);
        } else {
            requestCode = REQUEST_LAUNCH_IMAGE_CAPTURE;
            cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraCaptureURI = createUri(reactContext, null, false);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraCaptureURI);
        }

        if (cameraIntent.resolveActivity(reactContext.getPackageManager()) == null) {
            callback.invoke(getErrorMap("Cannot launch camera"));
            return;
        }

        currentActivity.startActivityForResult(cameraIntent, requestCode);
    }

    @ReactMethod
    public void launchImageLibrary(final ReadableMap options, final Callback callback) {
        final Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            callback.invoke(getErrorMap("can't find current Activity"));
            return;
        }

        this.callback = callback;
        this.options = new Options(options);

        if (!hasPermission(currentActivity)) {
            callback.invoke(getErrorMap("Permissions weren't granted"));
            return;
        }

        int requestCode;
        Intent libraryIntent;
        if (this.options.pickVideo) {
            requestCode = REQUEST_LAUNCH_VIDEO_LIBRARY;
            libraryIntent = new Intent(Intent.ACTION_PICK);
            libraryIntent.setType("video/*");
        } else {
            requestCode = REQUEST_LAUNCH_IMAGE_LIBRARY;
            libraryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        }

        if (libraryIntent.resolveActivity(reactContext.getPackageManager()) == null) {
            callback.invoke(getErrorMap("Cannot launch photo library"));
            return;
        }

        currentActivity.startActivityForResult(Intent.createChooser(libraryIntent, this.options.chooseWhichLibraryTitle), requestCode);
    }

    void onImageObtained(Uri uri, boolean shouldDeleteOriginalImage) {
        if (uri == null) {
            callback.invoke(getErrorMap("Image uri error"));
        }

        Uri newUri = resizeImage(uri, reactContext, options);

        if (newUri != uri && shouldDeleteOriginalImage) {
            deleteFile(uri, reactContext);
        }

        callback.invoke(getResponseMap(newUri, options, reactContext));
    }

    void onVideoObtained(Uri uri) {
        callback.invoke(getVideoResponseMap(uri, options, reactContext));
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {

        if (resultCode != Activity.RESULT_OK) {
            if (requestCode == REQUEST_LAUNCH_IMAGE_CAPTURE) {
                deleteFile(cameraCaptureURI, reactContext);
            }
            callback.invoke(getCancelMap());
            return;
        }

        switch (requestCode) {
            case REQUEST_LAUNCH_IMAGE_CAPTURE:
                onImageObtained(cameraCaptureURI, true);
                break;

            case REQUEST_LAUNCH_IMAGE_LIBRARY:
                onImageObtained(data.getData(), false);
                break;

            case REQUEST_LAUNCH_VIDEO_LIBRARY:
                onVideoObtained(data.getData());
                break;

            case REQUEST_LAUNCH_VIDEO_CAPTURE:
                onVideoObtained(data.getData());
                break;
        }
    }

    @Override
    public void onNewIntent(Intent intent) { }
}
