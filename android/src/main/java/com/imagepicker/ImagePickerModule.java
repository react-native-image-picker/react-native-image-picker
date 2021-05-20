package com.imagepicker;

import android.app.Activity;
import android.content.ActivityNotFoundException;
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

import java.io.File;

import static com.imagepicker.Utils.*;

@ReactModule(name = ImagePickerModule.NAME)
public class ImagePickerModule extends ReactContextBaseJavaModule implements ActivityEventListener {
    static final String NAME = "ImagePickerManager";

    // Public to let consuming apps hook into the image picker response
    public static final int REQUEST_LAUNCH_IMAGE_CAPTURE = 13001;
    public static final int REQUEST_LAUNCH_VIDEO_CAPTURE = 13002;
    public static final int REQUEST_LAUNCH_LIBRARY = 13003;

    private Uri fileUri;

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
        File file;
        Intent cameraIntent;

        if (this.options.mediaType.equals(mediaTypeVideo)) {
            requestCode = REQUEST_LAUNCH_VIDEO_CAPTURE;
            cameraIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            cameraIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, this.options.videoQuality);
            if (this.options.durationLimit > 0) {
                cameraIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, this.options.durationLimit);
            }
            file = createFile(reactContext, "mp4");
            cameraCaptureURI = createUri(file, reactContext);
        } else {
            requestCode = REQUEST_LAUNCH_IMAGE_CAPTURE;
            cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            file = createFile(reactContext, "jpg");
            cameraCaptureURI = createUri(file, reactContext);
        }

        if (this.options.useFrontCamera) {
            setFrontCamera(cameraIntent);
        }

        fileUri = Uri.fromFile(file);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraCaptureURI);
        cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        try {
            currentActivity.startActivityForResult(cameraIntent, requestCode);
        } catch (ActivityNotFoundException e) {
            callback.invoke(getErrorMap(errOthers, e.getMessage()));
            this.callback = null;
        }
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
        requestCode = REQUEST_LAUNCH_LIBRARY;
        if (this.options.mediaType.equals(mediaTypeVideo)) {
            libraryIntent = new Intent(Intent.ACTION_PICK);
            libraryIntent.setType("video/*");
        } else if (this.options.mediaType.equals(mediaTypePhoto)) {
            libraryIntent = new Intent(Intent.ACTION_PICK);
            libraryIntent.setType("image/*");
        } else {
            libraryIntent = new Intent(Intent.ACTION_GET_CONTENT);
            libraryIntent.setType("*/*");
            libraryIntent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
            libraryIntent.addCategory(Intent.CATEGORY_OPENABLE);
        }
        try {
            currentActivity.startActivityForResult(Intent.createChooser(libraryIntent, null), requestCode);
        } catch (ActivityNotFoundException e) {
            callback.invoke(getErrorMap(errOthers, e.getMessage()));
            this.callback = null;
        }
    }

    void onImageObtained(Uri uri) {
        Uri newUri = resizeImage(uri, reactContext, options);
        callback.invoke(getResponseMap(newUri, options, reactContext));
    }

    void onVideoObtained(Uri uri) {
        callback.invoke(getVideoResponseMap(uri, reactContext));
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {

        // onActivityResult is called even when ActivityNotFoundException occurs
        if (!isValidRequestCode(requestCode) || (this.callback == null)) {
            return;
        }

        if (resultCode != Activity.RESULT_OK) {
            if (requestCode == REQUEST_LAUNCH_IMAGE_CAPTURE) {
                deleteFile(fileUri);
            }
            callback.invoke(getCancelMap());
            return;
        }

        switch (requestCode) {
            case REQUEST_LAUNCH_IMAGE_CAPTURE:
                if (options.saveToPhotos) {
                    saveToPublicDirectory(cameraCaptureURI, reactContext, "photo");
                }
                onImageObtained(fileUri);
                break;

            case REQUEST_LAUNCH_LIBRARY:
                Uri uri = data.getData();
                if (isImageType(uri, reactContext)) {
                    onImageObtained(getAppSpecificStorageUri(uri, reactContext));
                } else if (isVideoType(uri, reactContext)) {
                    onVideoObtained(uri);
                } else {
                    // This could happen in rarest case when mediaType is mixed and the user selects some other file type like contacts etc, ideally these file options should not be shown by android
                    callback.invoke(getErrorMap(errOthers, "Unsupported file type"));
                }
                break;

            case REQUEST_LAUNCH_VIDEO_CAPTURE:
                if (options.saveToPhotos) {
                    saveToPublicDirectory(cameraCaptureURI, reactContext, "video");
                }
                onVideoObtained(fileUri);
                break;
        }
    }

    @Override
    public void onNewIntent(Intent intent) { }
}
