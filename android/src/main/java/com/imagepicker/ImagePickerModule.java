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
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.WritableMap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
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

    // Options
    private Boolean mNoData;
    private Boolean mTmpImage;
    private Boolean mAllowEditing;
    private Boolean mPickVideo;
    private Boolean mSavePrivate;
    private String mPath;
    private int mMaxWidth;
    private int mMaxHeight;
    private int mAspectX;
    private int mAspectY;
    private int mQuality;
    private int mAngle;
    private Boolean mForceAngle;
    private int mVideoQuality;
    private int mVideoDurationLimit;

    private WritableMap mResponse;

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
        mResponse = Arguments.createMap();

        if (currentActivity == null) {
            mResponse.putString("error", "can't find current Activity");
            callback.invoke(mResponse);
            return;
        }

        final List<String> titles = new ArrayList<>();
        final List<String> actions = new ArrayList<>();

        String cancelButtonTitle = getReactApplicationContext().getString(android.R.string.cancel);

        if (options.hasKey("takePhotoButtonTitle")
                && options.getString("takePhotoButtonTitle") != null
                && !options.getString("takePhotoButtonTitle").isEmpty()) {
            titles.add(options.getString("takePhotoButtonTitle"));
            actions.add("photo");
        }
        if (options.hasKey("chooseFromLibraryButtonTitle")
                && options.getString("chooseFromLibraryButtonTitle") != null
                && !options.getString("chooseFromLibraryButtonTitle").isEmpty()) {
            titles.add(options.getString("chooseFromLibraryButtonTitle"));
            actions.add("library");
        }
        if (options.hasKey("cancelButtonTitle")
                && !options.getString("cancelButtonTitle").isEmpty()) {
            cancelButtonTitle = options.getString("cancelButtonTitle");
        }

        if (options.hasKey("customButtons")) {
            ReadableMap buttons = options.getMap("customButtons");
            ReadableMapKeySetIterator it = buttons.keySetIterator();
            // Keep the current size as the iterator returns the keys in the reverse order they are defined
            int currentIndex = titles.size();
            while (it.hasNextKey()) {
                String key = it.nextKey();

                titles.add(currentIndex, key);
                actions.add(currentIndex, buttons.getString(key));
            }
        }

        titles.add(cancelButtonTitle);
        actions.add("cancel");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(currentActivity, android.R.layout.select_dialog_item, titles);
        AlertDialog.Builder builder = new AlertDialog.Builder(currentActivity);
        if (options.hasKey("title")
                && options.getString("title") != null
                && !options.getString("title").isEmpty()) {
            builder.setTitle(options.getString("title"));
        }

        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int index) {
                String action = actions.get(index);

                switch (action) {
                    case "photo":
                        launchCamera(options, callback);
                        break;
                    case "library":
                        launchImageLibrary(options, callback);
                        break;
                    case "cancel":
                        mResponse.putBoolean("didCancel", true);
                        callback.invoke(mResponse);
                        break;
                    default: // custom button
                        mResponse.putString("customButton", action);
                        callback.invoke(mResponse);
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
                mResponse.putBoolean("didCancel", true);
                callback.invoke(mResponse);
            }
        });
        dialog.show();
    }

    // NOTE: Currently not reentrant / doesn't support concurrent requests
    @ReactMethod
    public void launchCamera(final ReadableMap options, final Callback callback) {
        mResponse = Arguments.createMap();
        Activity currentActivity = getCurrentActivity();

        if (currentActivity == null) {
            mResponse.putString("error", "can't find current Activity");
            callback.invoke(mResponse);
            return;
        }

        parseOptions(options);

        Intent cameraIntent;
        int requestCode;

        if (mPickVideo) {
            requestCode = REQUEST_LAUNCH_VIDEO_CAPTURE;
            cameraIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            cameraIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, mVideoQuality);
            if (mVideoDurationLimit > 0) {
                cameraIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, mVideoDurationLimit);
            }
        } else {
            requestCode = REQUEST_LAUNCH_IMAGE_CAPTURE;
            cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            // we create a tmp file to save the result
            File imageFile = createOutputFile();
            if (imageFile == null) {
                mResponse.putString("error", "Cannot create file");
                callback.invoke(mResponse);
                return;
            }

            mImageURI = Uri.fromFile(imageFile);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile));

            if (mAllowEditing) {
                cameraIntent.putExtra("crop", "true");
                cameraIntent.putExtra("aspectX", mAspectX);
                cameraIntent.putExtra("aspectY", mAspectY);
            }
        }

        if (cameraIntent.resolveActivity(mReactContext.getPackageManager()) == null) {
            mResponse.putString("error", "Cannot launch camera");
            callback.invoke(mResponse);
            return;
        }

        try {
            mCallback = callback;
            currentActivity.startActivityForResult(cameraIntent, requestCode);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    // NOTE: Currently not reentrant / doesn't support concurrent requests
    @ReactMethod
    public void launchImageLibrary(final ReadableMap options, final Callback callback) {
        mResponse = Arguments.createMap();
        Activity currentActivity = getCurrentActivity();

        if (currentActivity == null) {
            mResponse.putString("error", "can't find current Activity");
            callback.invoke(mResponse);
            return;
        }

        parseOptions(options);

        Intent libraryIntent;
        int requestCode;

        if (mPickVideo) {
            requestCode = REQUEST_LAUNCH_VIDEO_LIBRARY;
            libraryIntent = new Intent(Intent.ACTION_PICK);
            libraryIntent.setType("video/*");
        } else {
            requestCode = REQUEST_LAUNCH_IMAGE_LIBRARY;
            libraryIntent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

            mImageURI = null;
            if (mAllowEditing) {
                File imageFile = createOutputFile();
                if (imageFile == null) {
                    mResponse.putString("error", "Cannot create file");
                    callback.invoke(mResponse);
                    return;
                }

                mImageURI = Uri.fromFile(imageFile);
                libraryIntent.putExtra(MediaStore.EXTRA_OUTPUT, mImageURI);
                libraryIntent.putExtra("crop", "true");
                libraryIntent.putExtra("aspectX", mAspectX);
                libraryIntent.putExtra("aspectY", mAspectY);
            }
        }

        if (libraryIntent.resolveActivity(mReactContext.getPackageManager()) == null) {
            mResponse.putString("error", "Cannot launch photo library");
            callback.invoke(mResponse);
            return;
        }

        try {
            mCallback = callback;
            currentActivity.startActivityForResult(libraryIntent, requestCode);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!isValidResult(requestCode)) {
            return;
        }

        // user cancel
        if (resultCode != Activity.RESULT_OK) {
            if (mImageURI != null) {
                deleteFile(new File(getPathFromURI(mImageURI)), requestCode);
            }
            mResponse.putBoolean("didCancel", true);
            mCallback.invoke(mResponse);
            return;
        }

        Uri uri = getUriFromResult(requestCode, data);
        if (uri == null) {
            return;
        }

        String path = getPathFromURI(uri);
        boolean isUrl = false;

        if (path != null) {
            try {
                new URL(path);
                isUrl = true;
            } catch (MalformedURLException e) {
                // not a url
            }
        }

        // image isn't in memory cache
        if (path == null || isUrl) {
            try {
                File file = createFileFromURI(uri);
                path = file.getAbsolutePath();
                uri = Uri.fromFile(file);
            } catch (Exception e) {
                // image not in cache
                mResponse.putString("error", "Could not read photo");
                mResponse.putString("uri", uri.toString());
                mCallback.invoke(mResponse);
                return;
            }
        }

        int currentAngle = 0;
        try {
            ExifInterface exif = new ExifInterface(path);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            boolean isVertical = true;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    isVertical = false;
                    currentAngle = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    isVertical = false;
                    currentAngle = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    currentAngle = 180;
                    break;
            }
            mResponse.putBoolean("isVertical", isVertical);
        } catch (IOException e) {
            e.printStackTrace();
            deleteFile(new File(getPathFromURI(uri)), requestCode);
            mResponse.putString("error", e.getMessage());
            mCallback.invoke(mResponse);
            return;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        int initialWidth = options.outWidth;
        int initialHeight = options.outHeight;

        // don't create a new file if contraint are respected
        if (((initialWidth < mMaxWidth && mMaxWidth > 0) || mMaxWidth == 0)
                && ((initialHeight < mMaxHeight && mMaxHeight > 0) || mMaxHeight == 0)
                && mQuality == 100 && (!mForceAngle || currentAngle == mAngle)) {
            mResponse.putInt("width", initialWidth);
            mResponse.putInt("height", initialHeight);
        } else {
            File resized = getResizedImage(getPathFromURI(uri), initialWidth, initialHeight);
            deleteFile(new File(getPathFromURI(uri)), requestCode);
            path = resized.getAbsolutePath();
            uri = Uri.fromFile(resized);
            BitmapFactory.decodeFile(path, options);
            mResponse.putInt("width", options.outWidth);
            mResponse.putInt("height", options.outHeight);
        }

        mResponse.putString("uri", uri.toString());
        mResponse.putString("path", path);

        if (!mNoData) {
            mResponse.putString("data", getBase64StringFromFile(path));
        }

        mCallback.invoke(mResponse);
    }

    /**
     * Checks if returned result from activity is expected
     * @param requestCode Request code for activity
     * @return True if valid, otherwise false
     */
    private boolean isValidResult(int requestCode) {
        return mCallback != null && (requestCode == REQUEST_LAUNCH_IMAGE_CAPTURE || requestCode == REQUEST_LAUNCH_IMAGE_LIBRARY
                || requestCode == REQUEST_LAUNCH_VIDEO_LIBRARY || requestCode == REQUEST_LAUNCH_VIDEO_CAPTURE);
    }

    /**
     * Returns uri from activity result
     * @param requestCode Requested activity code
     * @param data Data returned by activity
     * @return Uri, or null if result is handled
     */
    private Uri getUriFromResult(int requestCode, Intent data) {
        switch (requestCode) {
            case REQUEST_LAUNCH_IMAGE_LIBRARY:
                if (mImageURI == null) {
                    Uri uri = data.getData();

                    if (!mSavePrivate) {
                        return uri;
                    }

                    mImageURI = uri;
                }
            case REQUEST_LAUNCH_IMAGE_CAPTURE:
                File tmpFile = new File(getPathFromURI(mImageURI));
                File outputFile = createNewFile();

                if (outputFile != null) {
                    copyFile(tmpFile, outputFile);
                    deleteFile(tmpFile, requestCode);
                    return Uri.fromFile(outputFile);
                }

                deleteFile(tmpFile, requestCode);
                mResponse.putString("error", "Could create requested photo file");
                mCallback.invoke(mResponse);
                return null;
            case REQUEST_LAUNCH_VIDEO_LIBRARY:
                mResponse.putString("uri", data.getData().toString());
                mCallback.invoke(mResponse);
                return null;
            case REQUEST_LAUNCH_VIDEO_CAPTURE:
                mResponse.putString("uri", data.getData().toString());
                mCallback.invoke(mResponse);
                return null;
            default:
                return null;
        }
    }

    /**
     * Resolves Uri to path
     * @param uri Uri to resolve
     * @return Path for given uri
     */
    private String getPathFromURI(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = mReactContext.getContentResolver().query(uri, projection, null, null, null);

        if (cursor == null) { // Source is Dropbox or other similar local file path
            return uri.getPath();
        }

        cursor.moveToFirst();
        int idx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        String path = cursor.getString(idx);
        cursor.close();

        return path;
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
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            output.close();
            input.close();
        }

        return file;
    }

    /**
     * Encodes files content to base64 string
     * @param absoluteFilePath File to encode
     * @return Base64 encoded content
     */
    private String getBase64StringFromFile(String absoluteFilePath) {
        try {
            InputStream inputStream = new FileInputStream(absoluteFilePath);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            byte[] bytes = outputStream.toByteArray();

            inputStream.close();
            outputStream.close();

            return Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Create a resized image to fill the maxWidth/maxHeight values,the quality
     * value and the angle value
     *
     * @param realPath Path of file to resize
     * @param initialWidth
     * @param initialHeight
     * @return resized file
     */
    private File getResizedImage(final String realPath, final int initialWidth, final int initialHeight) {
        if (mMaxWidth == 0) {
            mMaxWidth = initialWidth;
        }

        if (mMaxHeight == 0) {
            mMaxHeight = initialHeight;
        }

        double widthRatio = (double) mMaxWidth / initialWidth;
        double heightRatio = (double) mMaxHeight / initialHeight;

        double ratio = (widthRatio < heightRatio)
                ? widthRatio
                : heightRatio;

        Matrix matrix = new Matrix();
        matrix.postRotate(mAngle);
        matrix.postScale((float) ratio, (float) ratio);

        Bitmap photo = BitmapFactory.decodeFile(realPath);
        Bitmap scaledPhoto = Bitmap.createBitmap(photo, 0, 0, photo.getWidth(), photo.getHeight(), matrix, true);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        scaledPhoto.compress(Bitmap.CompressFormat.JPEG, mQuality, bytes);

        File f = createNewFile();

        try {
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        }

        scaledPhoto.recycle();
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
        if (mTmpImage) {
            directory = mReactContext.getCacheDir();
        } else if (mSavePrivate) {
            directory = mReactContext.getFilesDir();
        } else {
            directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        }

        if (!TextUtils.isEmpty(mPath)) {
            directory = new File(directory, mPath);
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
     * Delete file if allowed
     * @param file File to delete
     * @param pickerType Image capture source
     */
    private void deleteFile(File file, int pickerType) {
        if (pickerType == REQUEST_LAUNCH_IMAGE_CAPTURE || mAllowEditing) {
            file.delete();
        }
    }

    /**
     * Copies file from src to dest
     * @param src Source file to copy
     * @param dst Destination file
     * @return True if file is copied, false otherwise
     */
    public boolean copyFile(File src, File dst) {
        try {
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dst);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) != -1) {
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
        mNoData = false;
        if (options.hasKey("noData")) {
            mNoData = options.getBoolean("noData");
        }

        mMaxWidth = 0;
        if (options.hasKey("maxWidth")) {
            mMaxWidth = options.getInt("maxWidth");
        }

        mMaxHeight = 0;
        if (options.hasKey("maxHeight")) {
            mMaxHeight = options.getInt("maxHeight");
        }

        mAspectX = 0;
        if (options.hasKey("aspectX")) {
            mAspectX = options.getInt("aspectX");
        }

        mAspectY = 0;
        if (options.hasKey("aspectY")) {
            mAspectY = options.getInt("aspectY");
        }

        mQuality = 100;
        if (options.hasKey("quality")) {
            mQuality = (int) (options.getDouble("quality") * 100);
        }

        mTmpImage = true;
        mSavePrivate = false;
        mPath = null;
        if (options.hasKey("storageOptions")) {
            mTmpImage = false;

            ReadableMap storageOptions = options.getMap("storageOptions");
            if (storageOptions.hasKey("savePrivate")) {
                mSavePrivate = storageOptions.getBoolean("savePrivate");
            }

            if (storageOptions.hasKey("path")) {
                mPath = storageOptions.getString("path");
            }
        }

        mAllowEditing = options.hasKey("allowsEditing") && options.getBoolean("allowsEditing");

        mForceAngle = options.hasKey("angle");
        mAngle = 0;
        if (mForceAngle) {
            mAngle = options.getInt("angle");
        }

        mPickVideo = options.hasKey("mediaType") && options.getString("mediaType").equals("video");

        mVideoQuality = 1;
        if (options.hasKey("videoQuality") && options.getString("videoQuality").equals("low")) {
            mVideoQuality = 0;
        }

        mVideoDurationLimit = 0;
        if (options.hasKey("durationLimit")) {
            mVideoDurationLimit = options.getInt("durationLimit");
        }
    }
}
