package com.imagepicker;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
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
            callback.invoke(getErrorMap(errCameraUnavailable, null));
            return;
        }

        final Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            callback.invoke(getErrorMap(errOthers, "Activity error"));
            return;
        }

        if (!isCameraPermissionFulfilled(reactContext, currentActivity)) {
            callback.invoke(getErrorMap(errOthers, cameraPermissionDescription));
            return;
        }

        this.callback = callback;
        this.options = new Options(options);

        if (this.options.saveToPhotos && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && !hasPermission(currentActivity)) {
            callback.invoke(getErrorMap(errPermission, null));
            return;
        }

        int requestCode;
        Intent cameraIntent;

        if (this.options.pickVideo) {
            requestCode = REQUEST_LAUNCH_VIDEO_CAPTURE;
            cameraIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            cameraIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, this.options.videoQuality);
            cameraCaptureURI = createUri(createFile(reactContext, "mp4"), reactContext);
        } else {
            requestCode = REQUEST_LAUNCH_IMAGE_CAPTURE;
            cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraCaptureURI = createUri(createFile(reactContext, "jpg"), reactContext);
        }
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraCaptureURI);

        if (cameraIntent.resolveActivity(reactContext.getPackageManager()) == null) {
            callback.invoke(getErrorMap(errOthers, "Activity error"));
            return;
        }

        currentActivity.startActivityForResult(cameraIntent, requestCode);
    }

    @ReactMethod
    public void launchImageLibrary(final ReadableMap options, final Callback callback) {
        final Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            callback.invoke(getErrorMap(errOthers, "Activity error"));
            return;
        }

        this.callback = callback;
        this.options = new Options(options);

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
            callback.invoke(getErrorMap(errOthers, "Activity error"));
            return;
        }

        currentActivity.startActivityForResult(Intent.createChooser(libraryIntent, null), requestCode);
    }

    void onImageObtained(Uri uri) {
        if (uri == null) {
            callback.invoke(getErrorMap(errOthers, "Uri error"));
            return;
        }
        Uri newUri = resizeImage(uri, reactContext, options);
        callback.invoke(getResponseMap(newUri, options, reactContext));
    }

    void onVideoObtained(Uri uri) {
        callback.invoke(getVideoResponseMap(uri, options, reactContext));
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {

        if (!isValidRequestCode(requestCode)) {
            return;
        }

        if (resultCode != Activity.RESULT_OK) {
            if (requestCode == REQUEST_LAUNCH_IMAGE_CAPTURE) {
                deleteFile(cameraCaptureURI, reactContext);
            }
            callback.invoke(getCancelMap());
            return;
        }

        switch (requestCode) {
            case REQUEST_LAUNCH_IMAGE_CAPTURE:
                if (options.saveToPhotos) {
                    saveToPublicDirectory(cameraCaptureURI, reactContext, "photo");
                }
                onImageObtained(cameraCaptureURI);
                break;

            case REQUEST_LAUNCH_IMAGE_LIBRARY:
                onImageObtained(data.getData());
                break;

            case REQUEST_LAUNCH_VIDEO_LIBRARY:
                onVideoObtained(data.getData());
                break;

            case REQUEST_LAUNCH_VIDEO_CAPTURE:
                if (options.saveToPhotos) {
                    saveToPublicDirectory(cameraCaptureURI, reactContext, "video");
                }
                onVideoObtained(cameraCaptureURI);
                break;
        }
    }

    @Override
    public void onNewIntent(Intent intent) { }
}
