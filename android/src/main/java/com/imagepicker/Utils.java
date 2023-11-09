package com.imagepicker;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.camera2.CameraCharacteristics;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.webkit.MimeTypeMap;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.imagepicker.ImagePickerModuleImpl.*;

public class Utils {
    public static String fileNamePrefix = "rn_image_picker_lib_temp_";

    public static String errCameraUnavailable = "camera_unavailable";
    public static String errPermission = "permission";
    public static String errOthers = "others";

    public static String mediaTypePhoto = "photo";
    public static String mediaTypeVideo = "video";

    public static String cameraPermissionDescription = "This library does not require Manifest.permission.CAMERA, if you add this permission in manifest then you have to obtain the same.";

    public static File createFile(Context reactContext, String fileType) {
        try {
            String filename = fileNamePrefix + UUID.randomUUID() + "." + fileType;

            // getCacheDir will auto-clean according to android docs
            File fileDir = reactContext.getCacheDir();

            File file = new File(fileDir, filename);
            file.createNewFile();
            return file;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Uri createUri(File file, Context reactContext) {
        String authority = reactContext.getApplicationContext().getPackageName() + ".imagepickerprovider";
        return FileProvider.getUriForFile(reactContext, authority, file);
    }

    public static void saveToPublicDirectory(Uri uri, Context context, String mediaType) {
        ContentResolver resolver = context.getContentResolver();
        Uri mediaStoreUri;
        ContentValues fileDetails = new ContentValues();

        if (mediaType.equals("video")) {
            fileDetails.put(MediaStore.Video.Media.DISPLAY_NAME, UUID.randomUUID().toString());
            fileDetails.put(MediaStore.Video.Media.MIME_TYPE, resolver.getType(uri));
            mediaStoreUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, fileDetails);
        } else {
            fileDetails.put(MediaStore.Images.Media.DISPLAY_NAME, UUID.randomUUID().toString());
            fileDetails.put(MediaStore.Images.Media.MIME_TYPE, resolver.getType(uri));
            mediaStoreUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, fileDetails);
        }

        copyUri(uri, mediaStoreUri, resolver);
    }

    public static void copyUri(Uri fromUri, Uri toUri, ContentResolver resolver) {
        try (OutputStream os = resolver.openOutputStream(toUri);
             InputStream is = resolver.openInputStream(fromUri)) {

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Make a copy of shared storage files inside app specific storage so that users can access it later.
    public static Uri getAppSpecificStorageUri(Uri sharedStorageUri, Context context) {
        if (sharedStorageUri == null) {
            return null;
        }
        ContentResolver contentResolver = context.getContentResolver();
        String fileType = getFileTypeFromMime(contentResolver.getType(sharedStorageUri));

        if (fileType == null) {
            Cursor cursor =
                    contentResolver.query(sharedStorageUri, null, null, null, null);
            if (cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                String fileName = cursor.getString(nameIndex);
                int lastDotIndex = fileName.lastIndexOf('.');

                if (lastDotIndex != -1) {
                    fileType = fileName.substring(lastDotIndex + 1);
                }
            }
        }

        Uri toUri = Uri.fromFile(createFile(context, fileType));
        copyUri(sharedStorageUri, toUri, contentResolver);
        return toUri;
    }

    public static boolean isCameraAvailable(Context reactContext) {
        return reactContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)
                || reactContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
    }

    // Opening front camera is not officially supported in android, the below hack is obtained from various online sources
    public static void setFrontCamera(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            intent.putExtra("android.intent.extras.CAMERA_FACING", CameraCharacteristics.LENS_FACING_FRONT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true);
            }
        } else {
            intent.putExtra("android.intent.extras.CAMERA_FACING", 1);
        }
    }

    public static int[] getImageDimensions(Uri uri, Context reactContext) {
        try (InputStream inputStream = reactContext.getContentResolver().openInputStream(uri)) {

            String orientation = getOrientation(uri,reactContext);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            if (needToSwapDimension(orientation)) {
                return new int[]{options.outHeight, options.outWidth};
            }else {
                return new int[]{options.outWidth, options.outHeight};
            }

        } catch (IOException e) {
            e.printStackTrace();
            return new int[]{0, 0};
        }
    }

    static boolean hasPermission(final Activity activity) {
        final int writePermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return writePermission == PackageManager.PERMISSION_GRANTED;
    }

    static String getBase64String(Uri uri, Context reactContext) {
        try (InputStream inputStream = reactContext.getContentResolver().openInputStream(uri);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] bytes;
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
            bytes = output.toByteArray();
            return Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static boolean needToSwapDimension(String orientation){
        return orientation.equals(String.valueOf(ExifInterface.ORIENTATION_ROTATE_90))
                || orientation.equals(String.valueOf(ExifInterface.ORIENTATION_ROTATE_270));
    }

    // Resize image
    // When decoding a jpg to bitmap all exif meta data will be lost, so make sure to copy orientation exif to new file else image might have wrong orientations
    public static Uri resizeImage(Uri uri, Context context, Options options) {
        try {
            int[] origDimens = getImageDimensions(uri, context);

            if (!shouldResizeImage(origDimens[0], origDimens[1], options)) {
                return uri;
            }

            int[] newDimens = getImageDimensBasedOnConstraints(origDimens[0], origDimens[1], options);

            try (InputStream imageStream = context.getContentResolver().openInputStream(uri)) {
                String mimeType = getMimeType(uri, context);
                Bitmap b = BitmapFactory.decodeStream(imageStream);
                String originalOrientation = getOrientation(uri, context);

                if (needToSwapDimension(originalOrientation)) {
                    b = Bitmap.createScaledBitmap(b, newDimens[1], newDimens[0], true);
                }else {
                    b = Bitmap.createScaledBitmap(b, newDimens[0], newDimens[1], true);
                }

                File file = createFile(context, getFileTypeFromMime(mimeType));

                try (OutputStream os = context.getContentResolver().openOutputStream(Uri.fromFile(file))) {
                    b.compress(getBitmapCompressFormat(mimeType), options.quality, os);
                }

                setOrientation(file, originalOrientation, context);

                deleteFile(uri);

                return Uri.fromFile(file);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return uri; // cannot resize the image, return the original uri
        }
    }

    static String getOrientation(Uri uri, Context context) throws IOException {
        ExifInterface exifInterface = new ExifInterface(context.getContentResolver().openInputStream(uri));
        return exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION);
    }

    // ExifInterface.saveAttributes is costly operation so don't set exif for unnecessary orientations
    static void setOrientation(File file, String orientation, Context context) throws IOException {
        if (orientation.equals(String.valueOf(ExifInterface.ORIENTATION_NORMAL)) || orientation.equals(String.valueOf(ExifInterface.ORIENTATION_UNDEFINED))) {
            return;
        }
        ExifInterface exifInterface = new ExifInterface(file);
        exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, orientation);
        exifInterface.saveAttributes();
    }

    static int[] getImageDimensBasedOnConstraints(int origWidth, int origHeight, Options options) {
        int width = origWidth;
        int height = origHeight;

        if (options.maxWidth == 0 || options.maxHeight == 0) {
            return new int[]{width, height};
        }

        if (options.maxWidth < width) {
            height = (int) (((float) options.maxWidth / width) * height);
            width = options.maxWidth;
        }

        if (options.maxHeight < height) {
            width = (int) (((float) options.maxHeight / height) * width);
            height = options.maxHeight;
        }

        return new int[]{width, height};
    }

    static double getFileSize(Uri uri, Context context) {
        try (ParcelFileDescriptor f = context.getContentResolver().openFileDescriptor(uri, "r")) {
            return f.getStatSize();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    static boolean shouldResizeImage(int origWidth, int origHeight, Options options) {
        if ((options.maxWidth == 0 || options.maxHeight == 0) && options.quality == 100) {
            return false;
        }

        if (options.maxWidth >= origWidth && options.maxHeight >= origHeight && options.quality == 100) {
            return false;
        }

        return true;
    }

    static Bitmap.CompressFormat getBitmapCompressFormat(String mimeType) {
        switch (mimeType) {
            case "image/jpeg":
                return Bitmap.CompressFormat.JPEG;
            case "image/png":
                return Bitmap.CompressFormat.PNG;
        }
        return Bitmap.CompressFormat.JPEG;
    }

    static String getFileTypeFromMime(String mimeType) {
        if (mimeType == null) {
            return "jpg";
        }
        switch (mimeType) {
            case "image/jpeg":
                return "jpg";
            case "image/png":
                return "png";
            case "image/gif":
                return "gif";
        }
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
    }

    static void deleteFile(Uri uri) {
        new File(uri.getPath()).delete();
    }


    // Since library users can have many modules in their project, we should respond to onActivityResult only for our request.
    static boolean isValidRequestCode(int requestCode) {
        switch (requestCode) {
            case REQUEST_LAUNCH_IMAGE_CAPTURE:
            case REQUEST_LAUNCH_VIDEO_CAPTURE:
            case REQUEST_LAUNCH_LIBRARY:
                return true;
            default:
                return false;
        }
    }

    // This library does not require Manifest.permission.CAMERA permission, but if user app declares as using this permission which is not granted, then attempting to use ACTION_IMAGE_CAPTURE|ACTION_VIDEO_CAPTURE will result in a SecurityException.
    // https://issuetracker.google.com/issues/37063818
    public static boolean isCameraPermissionFulfilled(Context context, Activity activity) {
        try {
            String[] declaredPermissions = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS)
                    .requestedPermissions;

            if (declaredPermissions == null) {
                return true;
            }

            if (Arrays.asList(declaredPermissions).contains(Manifest.permission.CAMERA)
                    && ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }

            return true;

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return true;
        }
    }

    static boolean isImageType(Uri uri, Context context) {
        return Utils.isContentType("image/", uri, context);
    }

    static boolean isVideoType(Uri uri, Context context) {
        return Utils.isContentType("video/", uri, context);
    }

    /**
     * Verifies the content typs of a file URI. A helper function
     * for isVideoType and isImageType
     *
     * @param contentMimeType - "video/" or "image/"
     * @param uri             - file uri
     * @param context         - react context
     * @return a boolean to determine if file is of specified content type i.e. image or video
     */
    static boolean isContentType(String contentMimeType, Uri uri, Context context) {
        final String mimeType = getMimeType(uri, context);

        if (mimeType != null) {
            return mimeType.contains(contentMimeType);
        }

        return false;
    }

    static String getMimeType(Uri uri, Context context) {
        if (uri.getScheme().equals("file")) {
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(uri.toString()));
        } else if (uri.getScheme().equals("content")) {
            ContentResolver contentResolver = context.getContentResolver();
            String contentResolverMimeType = contentResolver.getType(uri);

            if (contentResolverMimeType.isBlank()) {
                return getMimeTypeForContent(uri, context);
            } else {
                return contentResolverMimeType;
            }
        }

        return "Unknown";
    }

    static @Nullable String getMimeTypeForContent(Uri uri, Context context) {
        String fileName = getFileNameForContent(uri, context);
        String fileType = "Unknown";

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex != -1) {
            fileType = fileName.substring(lastDotIndex + 1);
        }
        return fileType;
    }

    static String getFileName(Uri uri, Context context) {
        if (uri.getScheme().equals("file")) {
            return uri.getLastPathSegment();
        } else if (uri.getScheme().equals("content")) {
            return getFileNameForContent(uri, context);
        }

        return "Unknown";
    }

    static String getOriginalFilePath(Uri uri, Context context) {
        String originPath;
        if (uri.getScheme().contains("content")) {
            originPath = getFilePathFromContent(uri, context);
            uri = getAppSpecificStorageUri(uri, context);
        } else {
            originPath = uri.toString();
        }

        return originPath;
    }

    private static String getFilePathFromContent(Uri uri, Context context) {
        String[] proj = {MediaStore.Images.Media.DATA};
        try (Cursor cursor = context.getContentResolver().query(uri, proj, null, null, null)) {
            int index = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
            if (index == -1) {
                return null;
            }
            cursor.moveToFirst();
            return cursor.getString(index);
        }
    }

    private static String getFileNameForContent(Uri uri, Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(uri, null, null, null, null);

        String fileName = uri.getLastPathSegment();
        try {
            if (cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                fileName = cursor.getString(nameIndex);
            }
        } finally {
            cursor.close();
        }
        return fileName;
    }

    static List<Uri> collectUrisFromData(Intent data) {
        // Default Gallery app on older Android versions doesn't support multiple image
        // picking and thus never uses clip data.
        if (data.getClipData() == null) {
            return Collections.singletonList(data.getData());
        }

        ClipData clipData = data.getClipData();
        List<Uri> fileUris = new ArrayList<>(clipData.getItemCount());

        for (int i = 0; i < clipData.getItemCount(); ++i) {
            fileUris.add(clipData.getItemAt(i).getUri());
        }

        return fileUris;
    }

    static ReadableMap getImageResponseMap(Uri uri, Uri appSpecificUri, Options options, Context context) {
        ImageMetadata imageMetadata = new ImageMetadata(appSpecificUri, context);
        int[] dimensions = getImageDimensions(appSpecificUri, context);

        String fileName = getFileName(uri, context);
        String originalPath = getOriginalFilePath(uri, context);

        WritableMap map = Arguments.createMap();
        map.putString("uri", appSpecificUri.toString());
        map.putDouble("fileSize", getFileSize(appSpecificUri, context));
        map.putString("fileName", fileName);
        map.putInt("width", dimensions[0]);
        map.putInt("height", dimensions[1]);
        map.putString("type", getMimeType(appSpecificUri, context));
        map.putString("originalPath", originalPath);

        if (options.includeBase64) {
            map.putString("base64", getBase64String(appSpecificUri, context));
        }

        if (options.includeExtra) {
            // Add more extra data here ...
            map.putString("timestamp", imageMetadata.getDateTime());
            map.putString("id", fileName);
        }

        return map;
    }

    static ReadableMap getVideoResponseMap(Uri uri, Uri appSpecificUri,Options options, Context context) {
        WritableMap map = Arguments.createMap();
        VideoMetadata videoMetadata = new VideoMetadata(appSpecificUri, context);

        String fileName = getFileName(uri, context);
        String originalPath = getOriginalFilePath(uri, context);

        map.putString("uri", appSpecificUri.toString());
        map.putDouble("fileSize", getFileSize(appSpecificUri, context));
        map.putInt("duration", videoMetadata.getDuration());
        map.putInt("bitrate", videoMetadata.getBitrate());
        map.putString("fileName", fileName);
        map.putString("type", getMimeType(appSpecificUri, context));
        map.putInt("width", videoMetadata.getWidth());
        map.putInt("height", videoMetadata.getHeight());
        map.putString("originalPath", originalPath);

        if (options.includeExtra) {
            // Add more extra data here ...
            map.putString("timestamp", videoMetadata.getDateTime());
            map.putString("id", fileName);
        }

        return map;
    }

    static ReadableMap getResponseMap(List<Uri> fileUris, Options options, Context context) throws RuntimeException {
        WritableArray assets = Arguments.createArray();

        for (int i = 0; i < fileUris.size(); ++i) {
            Uri uri = fileUris.get(i);

            Uri appSpecificUrl = uri;
            if (uri.getScheme().contains("content")) {
                appSpecificUrl = getAppSpecificStorageUri(uri, context);
            }

            // Call getAppSpecificStorageUri in the if block to avoid copying unsupported files
            if (isImageType(uri, context)) {
                appSpecificUrl = resizeImage(appSpecificUrl, context, options);
                assets.pushMap(getImageResponseMap(uri, appSpecificUrl, options, context));
            } else if (isVideoType(uri, context)) {
                if (uri.getScheme().contains("content")) {
                    appSpecificUrl = getAppSpecificStorageUri(uri, context);
                }
                assets.pushMap(getVideoResponseMap(uri, appSpecificUrl, options, context));
            } else {
                throw new RuntimeException("Unsupported file type");
            }
        }

        WritableMap response = Arguments.createMap();
        response.putArray("assets", assets);

        return response;
    }

    static ReadableMap getErrorMap(String errCode, String errMsg) {
        WritableMap map = Arguments.createMap();
        map.putString("errorCode", errCode);
        if (errMsg != null) {
            map.putString("errorMessage", errMsg);
        }
        return map;
    }

    static ReadableMap getCancelMap() {
        WritableMap map = Arguments.createMap();
        map.putBoolean("didCancel", true);
        return map;
    }
}
