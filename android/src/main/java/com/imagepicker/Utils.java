package com.imagepicker;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Base64;

import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import static com.imagepicker.ImagePickerModule.*;

public class Utils {
    public static String fileNamePrefix = "rn_image_picker_lib_temp_";

    public static String errCameraUnavailable = "camera_unavailable";
    public static String errPermission = "permission";
    public static String errOthers = "others";


    public static Uri createUri(Context reactContext, String fileType) {
        try {
            String filename = fileNamePrefix  + UUID.randomUUID() + "." + fileType;

            File fileDir = reactContext.getExternalFilesDir(null);
            deleteTempFiles(fileDir);

            File file = new File(fileDir, filename);
            file.createNewFile();
            String authority = reactContext.getApplicationContext().getPackageName() + ".imagepickerprovider";
            return FileProvider.getUriForFile(reactContext, authority, file);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void deleteTempFiles(File fileDir) {
        for (File f : fileDir.listFiles()) {
            if (f.getName().startsWith(fileNamePrefix)) {
                f.delete();
            }
        }
    }

    public static void saveToPublicDirectory(Uri uri, Context context, String mediaType) {
        ContentResolver resolver = context.getContentResolver();
        Uri mediaStoreUri;
        ContentValues fileDetails = new ContentValues();

        if (mediaType.equals("video")) {
            fileDetails.put(MediaStore.Video.Media.DISPLAY_NAME, UUID.randomUUID().toString());
            mediaStoreUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, fileDetails);
        } else {
            fileDetails.put(MediaStore.Images.Media.DISPLAY_NAME, UUID.randomUUID().toString());
            mediaStoreUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, fileDetails);
        }

        copyUri(uri, mediaStoreUri, resolver);
    }

    public static void copyUri(Uri fromUri, Uri toUri, ContentResolver resolver) {
        try {
            OutputStream os = resolver.openOutputStream(toUri);
            InputStream is = resolver.openInputStream(fromUri);

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isCameraAvailable(Context reactContext) {
        return reactContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)
                || reactContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
    }

    public static int[] getImageDimensions(Uri uri, Context reactContext) {
        InputStream inputStream;
        try {
            inputStream = reactContext.getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return new int[]{0, 0};
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(inputStream,null, options);
        return new int[]{options.outWidth, options.outHeight};
    }

    static boolean hasPermission(final Activity activity) {
        final int writePermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return writePermission == PackageManager.PERMISSION_GRANTED ? true : false;
    }

    static String getBase64String(Uri uri, Context reactContext) {
        InputStream inputStream;
        try {
            inputStream = reactContext.getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        byte[] bytes;
        byte[] buffer = new byte[8192];
        int bytesRead;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        bytes = output.toByteArray();
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    public static Uri resizeImage(Uri uri, Context context, Options options) {
        try {
            int[] origDimens = getImageDimensions(uri, context);

            if (!shouldResizeImage(origDimens[0], origDimens[1], options)) {
                return uri;
            }

            int[] newDimens = getImageDimensBasedOnConstraints(origDimens[0], origDimens[1], options);

            InputStream imageStream = context.getContentResolver().openInputStream(uri);
            String mimeType =  context.getContentResolver().getType(uri);
            Bitmap b = BitmapFactory.decodeStream(imageStream);
            b = Bitmap.createScaledBitmap(b, newDimens[0], newDimens[1], true);

            Uri newUri = createUri(context, getFileTypeFromMime(mimeType));
            OutputStream os = context.getContentResolver().openOutputStream(newUri);
            b.compress(getBitmapCompressFormat(mimeType), options.quality, os);
            return newUri;

        } catch (Exception e) {
            e.printStackTrace();
            return  null;
        }
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
        try {
            ParcelFileDescriptor f = context.getContentResolver().openFileDescriptor(uri, "r");
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
            case "image/jpeg": return Bitmap.CompressFormat.JPEG;
            case "image/png": return Bitmap.CompressFormat.PNG;
        }
        return Bitmap.CompressFormat.JPEG;
    }

    static String getFileTypeFromMime(String mimeType) {
        if (mimeType == null) {
            return "jpg";
        }
        switch (mimeType) {
            case "image/jpeg": return "jpg";
            case "image/png": return "png";
        }
        return "jpg";
    }

    static void deleteFile(Uri uri, Context context) {
        context.getContentResolver().delete(uri, null, null);
    }

    // Since library users can have many modules in their project, we should respond to onActivityResult only for our request.
    static boolean isValidRequestCode(int requestCode) {
        switch (requestCode) {
            case REQUEST_LAUNCH_IMAGE_CAPTURE:
            case REQUEST_LAUNCH_IMAGE_LIBRARY:
            case REQUEST_LAUNCH_VIDEO_CAPTURE:
            case REQUEST_LAUNCH_VIDEO_LIBRARY: return true;
            default: return false;
        }
    }

    static ReadableMap getResponseMap(Uri uri, Options options, Context context) {
        ContentResolver resolver = context.getContentResolver();

        Cursor returnCursor = resolver.query(uri, null, null, null, null);
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        returnCursor.moveToFirst();

        String fileName = returnCursor.getString(nameIndex);
        int[] dimensions = getImageDimensions(uri, context);

        WritableMap map = Arguments.createMap();
        map.putString("uri", uri.toString());
        map.putDouble("fileSize", getFileSize(uri, context));
        map.putString("fileName", fileName);
        map.putString("type", resolver.getType(uri));
        map.putInt("width", dimensions[0]);
        map.putInt("height", dimensions[1]);

        if (options.includeBase64) {
            map.putString("base64", getBase64String(uri, context));
        }
        returnCursor.close();
        return map;
    }

    static ReadableMap getVideoResponseMap(Uri uri, Options options, Context context) {
        ContentResolver resolver = context.getContentResolver();

        Cursor returnCursor = resolver.query(uri, null, null, null, null);
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        returnCursor.moveToFirst();

        String fileName = returnCursor.getString(nameIndex);

        WritableMap map = Arguments.createMap();
        map.putString("uri", uri.toString());
        map.putDouble("fileSize", getFileSize(uri, context));
        map.putString("fileName", fileName);
        return map;
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
