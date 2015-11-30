package com.imagepicker;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.database.Cursor;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

import java.io.File;

public class ImagePickerModule extends ReactContextBaseJavaModule {
  static final int REQUEST_LAUNCH_CAMERA = 1;
  static final int REQUEST_LAUNCH_IMAGE_LIBRARY = 2;

  private final ReactApplicationContext mReactContext;
  private final MainActivity mMainActivity;

  private Uri mCameraCaptureURI;
  private Callback mCallback;

  public ImagePickerModule(ReactApplicationContext reactContext, MainActivity mainActivity) {
    super(reactContext);

    mReactContext = reactContext;
    mMainActivity = mainActivity;
  }

  @Override
  public String getName() {
    return "UIImagePickerManager"; // To coincide with the iOS native module name
  }

  // NOTE: Currently not reentrant / doesn't support concurrent requests
  @ReactMethod
  public void launchCamera(ReadableMap options, Callback callback) {
    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    if (cameraIntent.resolveActivity(mMainActivity.getPackageManager()) == null) {
        callback.invoke(true, "error resolving activity");
        return;
    }

    // we create a tmp file to save the result
    File imageFile;
    imageFile = File.createTempFile("exponent_capture_", ".jpg",
      Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES));
    if (imageFile == null) {
        callback.invoke(true, "error file not created");
        return;
    }
    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile));
    mCameraCaptureURI = Uri.fromFile(imageFile);
    mCallback = callback;
    mMainActivity.startActivityForResult(cameraIntent, REQUEST_LAUNCH_CAMERA);
  }

  // NOTE: Currently not reentrant / doesn't support concurrent requests
  @ReactMethod
  public void launchImageLibrary(ReadableMap options, Callback callback) {
    Intent libraryIntent = new Intent();
    libraryIntent.setType("image/");
    libraryIntent.setAction(Intent.ACTION_GET_CONTENT);
    mCallback = callback;
    mMainActivity.startActivityForResult(libraryIntent, REQUEST_LAUNCH_IMAGE_LIBRARY);
  }

  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    //robustness code
    if (mCallback == null || (mCameraCaptureURI == null && requestCode == REQUEST_LAUNCH_CAMERA)
            || (requestCode != REQUEST_LAUNCH_CAMERA && requestCode != REQUEST_LAUNCH_IMAGE_LIBRARY)) {
      return;
    }

    // user cancel
    if (resultCode != Activity.RESULT_OK) {
      mCallback.invoke(true, Arguments.createMap());
      return;
    }

    WritableMap response = Arguments.createMap();
    Uri uri = (requestCode == REQUEST_LAUNCH_CAMERA)
    ? mCameraCaptureURI
    : data.getData();

    // let's set data
    response.putString("path", uri.toString());
    response.putString("uri", getRealPathFromURI(uri));

    mCallback.invoke(false, response);
  }

  public String getRealPathFromURI(Uri uri) {
    String result;
    Cursor cursor = mMainActivity.getContentResolver().query(uri, null, null, null, null);
    if (cursor == null) { // Source is Dropbox or other similar local file path
        result = uri.getPath();
    } else {
        cursor.moveToFirst();
        int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
        result = cursor.getString(idx);
        cursor.close();
    }
    return result;
  }
}