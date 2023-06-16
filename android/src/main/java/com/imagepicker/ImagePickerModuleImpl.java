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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.imagepicker.Utils.*;
import com.imagepicker.Options;

import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia;
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia;

public class ImagePickerModuleImpl implements ActivityEventListener {
    static final String NAME = "ImagePicker";

    // Public to let consuming apps hook into the image picker response
    public static final int REQUEST_LAUNCH_IMAGE_CAPTURE = 13001;
    public static final int REQUEST_LAUNCH_VIDEO_CAPTURE = 13002;
    public static final int REQUEST_LAUNCH_LIBRARY = 13003;

    private Uri fileUri;

    private ReactApplicationContext reactContext;

    Callback callback;

    Options options;
    Uri cameraCaptureURI;

    public ImagePickerModuleImpl(ReactApplicationContext reactContext) {
        this.reactContext = reactContext;
        this.reactContext.addActivityEventListener(this);
    }

    public void launchCamera(final ReadableMap options, final Callback callback) {
        if (!isCameraAvailable(reactContext)) {
            callback.invoke(getErrorMap(errCameraUnavailable, null));
            return;
        }

        final Activity currentActivity = this.reactContext.getCurrentActivity();
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

    public void launchImageLibrary(final ReadableMap options, final Callback callback) {
        final Activity currentActivity = this.reactContext.getCurrentActivity();
        if (currentActivity == null) {
            callback.invoke(getErrorMap(errOthers, "Activity error"));
            return;
        }

        this.callback = callback;
        this.options = new Options(options);

        PickVisualMedia.VisualMediaType mediaType;
        PickVisualMediaRequest mediaRequest;

        int requestCode;
        Intent libraryIntent;
        requestCode = REQUEST_LAUNCH_LIBRARY;

        int selectionLimit = this.options.selectionLimit;
        boolean isSingleSelect = selectionLimit == 1;
        boolean isPhoto = this.options.mediaType.equals(mediaTypePhoto);
        boolean isVideo = this.options.mediaType.equals(mediaTypeVideo);

        // Note: Casting works, even though Android Studio complains about it
        if (isPhoto) {
            mediaType = (PickVisualMedia.VisualMediaType) PickVisualMedia.ImageOnly.INSTANCE;
        } else if (isVideo) {
            mediaType = (PickVisualMedia.VisualMediaType) PickVisualMedia.VideoOnly.INSTANCE;
        } else {
            mediaType = (PickVisualMedia.VisualMediaType) PickVisualMedia.ImageAndVideo.INSTANCE;
        }

        mediaRequest = new PickVisualMediaRequest.Builder()
                .setMediaType(mediaType)
                .build();

        // https://developer.android.com/training/data-storage/shared/photopicker
        if (isSingleSelect) {
            libraryIntent = new PickVisualMedia().createIntent(this.reactContext.getApplicationContext(), mediaRequest);
        } else {
            PickMultipleVisualMedia pickMultipleVisualMedia = selectionLimit > 1
                    ? new PickMultipleVisualMedia(selectionLimit)
                    : new PickMultipleVisualMedia();
            libraryIntent = pickMultipleVisualMedia.createIntent(this.reactContext.getApplicationContext(), mediaRequest);
        }

        if(this.options.restrictMimeTypes.length > 0) {
            libraryIntent.putExtra(Intent.EXTRA_MIME_TYPES, this.options.restrictMimeTypes);
        }

        try {
            currentActivity.startActivityForResult(libraryIntent, requestCode);
        } catch (ActivityNotFoundException e) {
            callback.invoke(getErrorMap(errOthers, e.getMessage()));
            this.callback = null;
        }
    }

    void onAssetsObtained(List<Uri> fileUris) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.submit(() -> {
            try {
                callback.invoke(getResponseMap(fileUris, options, reactContext));
            } catch (RuntimeException exception) {
                callback.invoke(getErrorMap(errOthers, exception.getMessage()));
            } finally {
                callback = null;
            }
        });
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
            try {
                callback.invoke(getCancelMap());
                return;
            } catch (RuntimeException exception) {
                callback.invoke(getErrorMap(errOthers, exception.getMessage()));
            } finally {
                callback = null;
            }
        }

        switch (requestCode) {
            case REQUEST_LAUNCH_IMAGE_CAPTURE:
                if (options.saveToPhotos) {
                    saveToPublicDirectory(cameraCaptureURI, reactContext, "photo");
                }

                onAssetsObtained(Collections.singletonList(fileUri));
                break;

            case REQUEST_LAUNCH_LIBRARY:
                onAssetsObtained(collectUrisFromData(data));
                break;

            case REQUEST_LAUNCH_VIDEO_CAPTURE:
                if (options.saveToPhotos) {
                    saveToPublicDirectory(cameraCaptureURI, reactContext, "video");
                }

                onAssetsObtained(Collections.singletonList(fileUri));
                break;
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
    }
}
