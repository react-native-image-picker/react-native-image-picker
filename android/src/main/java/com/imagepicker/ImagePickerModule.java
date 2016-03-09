package com.imagepicker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.widget.ArrayAdapter;
import com.facebook.react.bridge.*;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ImagePickerModule extends ReactContextBaseJavaModule implements ActivityEventListener {

    static final int REQUEST_LAUNCH_IMAGE_CAPTURE = 1;
    static final int REQUEST_LAUNCH_IMAGE_LIBRARY = 2;
    static final int REQUEST_LAUNCH_VIDEO_LIBRARY = 3;
    static final int REQUEST_LAUNCH_VIDEO_CAPTURE = 4;

    private final ReactApplicationContext mReactContext;

    private Uri mImageURI;
    private Callback mCallback;
    private Boolean noData = false;
    private Boolean tmpImage;
    private Boolean allowEditing = false;
    private Boolean pickVideo = false;
    private Boolean savePrivate;
    private String path;
    private int maxWidth = 0;
    private int maxHeight = 0;
    private int aspectX = 0;
    private int aspectY = 0;
    private int quality = 100;
    private int angle = 0;
    private Boolean forceAngle;
    private int videoQuality = 1;
    private int videoDurationLimit = 0;
    WritableMap response;

    public ImagePickerModule(ReactApplicationContext reactContext) {
        super(reactContext);

        reactContext.addActivityEventListener(this);

        mReactContext = reactContext;
    }

    @Override
    public String getName() {
        return "ImagePickerManager";
    }

    @ReactMethod
    public void showImagePicker(final ReadableMap options, final Callback callback) {
        Activity currentActivity = getCurrentActivity();
        response = Arguments.createMap();

        if (currentActivity == null) {
            response.putString("error", "can't find current Activity");
            callback.invoke(response);
            return;
        }

        List<String> mTitles = new ArrayList<String>();
        List<String> mActions = new ArrayList<String>();

        String cancelButtonTitle = "Cancel";

        if (options.hasKey("takePhotoButtonTitle")
                && options.getString("takePhotoButtonTitle") != null
                && !options.getString("takePhotoButtonTitle").isEmpty()) {
            mTitles.add(options.getString("takePhotoButtonTitle"));
            mActions.add("photo");
        }
        if (options.hasKey("chooseFromLibraryButtonTitle")
                && options.getString("chooseFromLibraryButtonTitle") != null
                && !options.getString("chooseFromLibraryButtonTitle").isEmpty()) {
            mTitles.add(options.getString("chooseFromLibraryButtonTitle"));
            mActions.add("library");
        }
        if (options.hasKey("cancelButtonTitle")
                && !options.getString("cancelButtonTitle").isEmpty()) {
            cancelButtonTitle = options.getString("cancelButtonTitle");
        }
        mTitles.add(cancelButtonTitle);
        mActions.add("cancel");

        String[] option = new String[mTitles.size()];
        option = mTitles.toArray(option);

        String[] action = new String[mActions.size()];
        action = mActions.toArray(action);
        final String[] act = action;

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(currentActivity,
                android.R.layout.select_dialog_item, option);
        AlertDialog.Builder builder = new AlertDialog.Builder(currentActivity);
        if (options.hasKey("title") && options.getString("title") != null && !options.getString("title").isEmpty()) {
            builder.setTitle(options.getString("title"));
        }

        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int index) {
                if (act[index].equals("photo")) {
                    launchCamera(options, callback);
                } else if (act[index].equals("library")) {
                    launchImageLibrary(options, callback);
                } else {
                    response.putBoolean("didCancel", true);
                    callback.invoke(response);
                }
            }
        });

        final AlertDialog dialog = builder.create();
        /**
         * override onCancel method to callback cancel in case of a touch outside of
         * the dialog or the BACK key pressed
         */
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
                response.putBoolean("didCancel", true);
                callback.invoke(response);
            }
        });
        dialog.show();
    }

    // NOTE: Currently not reentrant / doesn't support concurrent requests
    @ReactMethod
    public void launchCamera(final ReadableMap options, final Callback callback) {
        int requestCode;
        Intent cameraIntent;
        response = Arguments.createMap();
        Activity currentActivity = getCurrentActivity();

        if (currentActivity == null) {
            response.putString("error", "can't find current Activity");
            callback.invoke(response);
            return;
        }

        parseOptions(options);

        if (pickVideo) {
            requestCode = REQUEST_LAUNCH_VIDEO_CAPTURE;
            cameraIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            cameraIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, videoQuality);
            if (videoDurationLimit > 0) {
                cameraIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, videoDurationLimit);
            }
        } else {
            requestCode = REQUEST_LAUNCH_IMAGE_CAPTURE;
            cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            // we create a tmp file to save the result
            File imageFile = createOutputFile();
            if (imageFile == null) {
                response.putString("error", "Cannot create file");
                callback.invoke(response);
                return;
            }

            mImageURI = Uri.fromFile(imageFile);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile));

            if (allowEditing) {
                cameraIntent.putExtra("crop", "true");
                cameraIntent.putExtra("aspectX", aspectX);
                cameraIntent.putExtra("aspectY", aspectY);
            }
        }

        if (cameraIntent.resolveActivity(mReactContext.getPackageManager()) == null) {
            response.putString("error", "Cannot launch camera");
            callback.invoke(response);
            return;
        }

        mCallback = callback;

        try {
            currentActivity.startActivityForResult(cameraIntent, requestCode);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    // NOTE: Currently not reentrant / doesn't support concurrent requests
    @ReactMethod
    public void launchImageLibrary(final ReadableMap options, final Callback callback) {
        int requestCode;
        Intent libraryIntent;
        response = Arguments.createMap();
        Activity currentActivity = getCurrentActivity();

        if (currentActivity == null) {
            response.putString("error", "can't find current Activity");
            callback.invoke(response);
            return;
        }

        parseOptions(options);

        if (pickVideo) {
            requestCode = REQUEST_LAUNCH_VIDEO_LIBRARY;
            libraryIntent = new Intent(Intent.ACTION_PICK);
            libraryIntent.setType("video/*");
        } else {
            requestCode = REQUEST_LAUNCH_IMAGE_LIBRARY;
            libraryIntent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

            mImageURI = null;
            if (allowEditing) {
                File imageFile = createOutputFile();
                if (imageFile == null) {
                    response.putString("error", "Cannot create file");
                    callback.invoke(response);
                    return;
                }

                mImageURI = Uri.fromFile(imageFile);
                libraryIntent.putExtra(MediaStore.EXTRA_OUTPUT, mImageURI);
                libraryIntent.putExtra("crop", "true");
                libraryIntent.putExtra("aspectX", aspectX);
                libraryIntent.putExtra("aspectY", aspectY);
            }
        }

        if (libraryIntent.resolveActivity(mReactContext.getPackageManager()) == null) {
            response.putString("error", "Cannot launch photo library");
            callback.invoke(response);
            return;
        }

        mCallback = callback;

        try {
            currentActivity.startActivityForResult(libraryIntent, requestCode);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        //robustness code

        if (mCallback == null || (requestCode != REQUEST_LAUNCH_IMAGE_CAPTURE && requestCode != REQUEST_LAUNCH_IMAGE_LIBRARY
                && requestCode != REQUEST_LAUNCH_VIDEO_LIBRARY && requestCode != REQUEST_LAUNCH_VIDEO_CAPTURE)) {
            return;
        }

        // user cancel
        if (resultCode != Activity.RESULT_OK) {
            if (mImageURI != null) {
                deleteFile(new File(getRealPathFromURI(mImageURI)), requestCode);
            }
            response.putBoolean("didCancel", true);
            mCallback.invoke(response);
            return;
        }

        Uri uri;
        switch (requestCode) {
            case REQUEST_LAUNCH_IMAGE_LIBRARY:
                if (mImageURI == null) {
                    uri = data.getData();

                    if (!savePrivate) {
                        break;
                    }

                    mImageURI = uri;
                }
            case REQUEST_LAUNCH_IMAGE_CAPTURE:
                File outputFile = createNewFile();
                File tmpFile = new File(getRealPathFromURI(mImageURI));
                copyFile(tmpFile, outputFile);
                deleteFile(tmpFile, requestCode);
                uri = Uri.fromFile(outputFile);
                break;
            case REQUEST_LAUNCH_VIDEO_LIBRARY:
                response.putString("uri", data.getData().toString());
                mCallback.invoke(response);
                return;
            case REQUEST_LAUNCH_VIDEO_CAPTURE:
                response.putString("uri", data.getData().toString());
                mCallback.invoke(response);
                return;
            default:
                uri = null;
        }

        String realPath = getRealPathFromURI(uri);
        boolean isUrl = false;

        if (realPath != null) {
            try {
                new URL(realPath);
                isUrl = true;
            } catch (MalformedURLException e) {
                // not a url
            }
        }

        // image isn't in memory cache
        if (realPath == null || isUrl) {
            try {
                File file = createFileFromURI(uri);
                realPath = file.getAbsolutePath();
                uri = Uri.fromFile(file);
            } catch (Exception e) {
                // image not in cache
                response.putString("error", "Could not read photo");
                response.putString("uri", uri.toString());
                mCallback.invoke(response);
                return;
            }
        }

        int CurrentAngle = 0;
        try {
            ExifInterface exif = new ExifInterface(realPath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            boolean isVertical = true;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    isVertical = false;
                    CurrentAngle = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    isVertical = false;
                    CurrentAngle = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    CurrentAngle = 180;
                    break;
            }
            response.putBoolean("isVertical", isVertical);
        } catch (IOException e) {
            e.printStackTrace();
            deleteFile(new File(getRealPathFromURI(uri)), requestCode);
            response.putString("error", e.getMessage());
            mCallback.invoke(response);
            return;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(realPath, options);
        int initialWidth = options.outWidth;
        int initialHeight = options.outHeight;

        // don't create a new file if contraint are respected
        if (((initialWidth < maxWidth && maxWidth > 0) || maxWidth == 0)
                && ((initialHeight < maxHeight && maxHeight > 0) || maxHeight == 0)
                && quality == 100 && (!forceAngle || CurrentAngle == angle)) {
            response.putInt("width", initialWidth);
            response.putInt("height", initialHeight);
        } else {
            File resized = getResizedImage(getRealPathFromURI(uri), initialWidth, initialHeight);
            deleteFile(new File(getRealPathFromURI(uri)), requestCode);
            realPath = resized.getAbsolutePath();
            uri = Uri.fromFile(resized);
            BitmapFactory.decodeFile(realPath, options);
            response.putInt("width", options.outWidth);
            response.putInt("height", options.outHeight);
        }

        response.putString("uri", uri.toString());
        response.putString("path", realPath);

        if (!noData) {
            response.putString("data", getBase64StringFromFile(realPath));
        }

        mCallback.invoke(response);
    }

    private String getRealPathFromURI(Uri uri) {
        String result;
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = mReactContext.getContentResolver().query(uri, projection, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = uri.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    /**
     * Create a file from uri to allow image picking of image in disk cache
     * (Exemple: facebook image, google image etc..)
     *
     * @doc =>
     * https://github.com/nostra13/Android-Universal-Image-Loader#load--display-task-flow
     *
     * @param uri
     * @return File
     * @throws Exception
     */
    private File createFileFromURI(Uri uri) throws Exception {
        File file = new File(mReactContext.getCacheDir(), uri.getLastPathSegment());
        InputStream input = mReactContext.getContentResolver().openInputStream(uri);
        OutputStream output = new FileOutputStream(file);

        try {
            byte[] buffer = new byte[4 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            output.flush();
        } finally {
            output.close();
            input.close();
        }

        return file;
    }

    private String getBase64StringFromFile(String absoluteFilePath) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(absoluteFilePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
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

    /**
     * Create a resized image to fill the maxWidth/maxHeight values,the quality
     * value and the angle value
     *
     * @param realPath
     * @param initialWidth
     * @param initialHeight
     * @return resized file
     */
    private File getResizedImage(final String realPath, final int initialWidth, final int initialHeight) {
        Bitmap photo = BitmapFactory.decodeFile(realPath);

        Bitmap scaledphoto;
        if (maxWidth == 0) {
            maxWidth = initialWidth;
        }
        if (maxHeight == 0) {
            maxHeight = initialHeight;
        }
        double widthRatio = (double) maxWidth / initialWidth;
        double heightRatio = (double) maxHeight / initialHeight;

        double ratio = (widthRatio < heightRatio)
                ? widthRatio
                : heightRatio;

        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        matrix.postScale((float) ratio, (float) ratio);

        scaledphoto = Bitmap.createBitmap(photo, 0, 0, photo.getWidth(), photo.getHeight(), matrix, true);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        scaledphoto.compress(Bitmap.CompressFormat.JPEG, quality, bytes);

        File f = createNewFile();
        FileOutputStream fo;
        try {
            fo = new FileOutputStream(f);
            try {
                fo.write(bytes.toByteArray());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        scaledphoto.recycle();
        photo.recycle();

        return f;
    }

    /**
     * Creates output file for camera or edited image
     * @return Empty file
     */
    private File createOutputFile() {
        String filename = String.format("%s.jpg", UUID.randomUUID().toString());
        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        File file = new File(directory, filename);

        try {
            if (!file.createNewFile()) {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return file;
    }

    /**
     * Creates new file requested by action options
     * @return Empty file
     */
    private File createNewFile() {
        String filename = String.format("%s.jpg", UUID.randomUUID().toString());
        File directory;

        if (tmpImage) {
            directory = mReactContext.getCacheDir();
        } else if (savePrivate) {
            directory = mReactContext.getFilesDir();
        } else {
            directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        }

        if (!TextUtils.isEmpty(path)) {
            directory = new File(directory, path);
            if (!directory.exists() && !directory.mkdirs()) {
                return null;
            }
        }

        File file = new File(directory, filename);

        try {
            if (!file.createNewFile()) {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return file;
    }

    /**
     * Deletes file if allowed
     * @param file
     * @param pickerType
     */
    private void deleteFile(File file, int pickerType) {
        if (pickerType == REQUEST_LAUNCH_IMAGE_CAPTURE || allowEditing) {
            file.delete();
        }
    }

    /**
     * Copies file from src to dest
     * @param src
     * @param dst
     */
    public boolean copyFile(File src, File dst) {
        try {
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dst);

            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            return true;
        }catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void parseOptions(final ReadableMap options) {
        noData = false;
        if (options.hasKey("noData")) {
            noData = options.getBoolean("noData");
        }
        maxWidth = 0;
        if (options.hasKey("maxWidth")) {
            maxWidth = options.getInt("maxWidth");
        }
        maxHeight = 0;
        if (options.hasKey("maxHeight")) {
            maxHeight = options.getInt("maxHeight");
        }
        aspectX = 0;
        if (options.hasKey("aspectX")) {
            aspectX = options.getInt("aspectX");
        }
        aspectY = 0;
        if (options.hasKey("aspectY")) {
            aspectY = options.getInt("aspectY");
        }
        quality = 100;
        if (options.hasKey("quality")) {
            quality = (int) (options.getDouble("quality") * 100);
        }
        tmpImage = true;
        savePrivate = false;
        path = null;
        if (options.hasKey("storageOptions")) {
            tmpImage = false;
            ReadableMap storageOptions = options.getMap("storageOptions");
            if (storageOptions.hasKey("savePrivate")) {
                savePrivate = storageOptions.getBoolean("savePrivate");
            }
            if (storageOptions.hasKey("path")) {
                path = storageOptions.getString("path");
            }
        }
        allowEditing = false;
        if (options.hasKey("allowsEditing")) {
            allowEditing = options.getBoolean("allowsEditing");
        }
        forceAngle = false;
        angle = 0;
        if (options.hasKey("angle")) {
            forceAngle = true;
            angle = options.getInt("angle");
        }
        pickVideo = false;
        if (options.hasKey("mediaType") && options.getString("mediaType").equals("video")) {
            pickVideo = true;
        }
        videoQuality = 1;
        if (options.hasKey("videoQuality") && options.getString("videoQuality").equals("low")) {
            videoQuality = 0;
        }
        videoDurationLimit = 0;
        if (options.hasKey("durationLimit")) {
            videoDurationLimit = options.getInt("durationLimit");
        }
    }
}
