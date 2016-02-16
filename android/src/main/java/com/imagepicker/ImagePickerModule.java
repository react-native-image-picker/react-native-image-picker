package com.imagepicker;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.database.Cursor;
import android.util.Base64;
import android.app.AlertDialog;
import android.widget.ArrayAdapter;
import android.content.DialogInterface;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.content.ComponentName;
import android.graphics.Matrix;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.util.List;
import java.util.ArrayList;
import java.io.FileOutputStream;
import java.util.UUID;
import java.net.URL;
import java.net.MalformedURLException;

public class ImagePickerModule extends ReactContextBaseJavaModule {
  static final int REQUEST_LAUNCH_CAMERA = 1;
  static final int REQUEST_LAUNCH_IMAGE_LIBRARY = 2;
  static final int REQUEST_IMAGE_CROPPING = 3;

  private final ReactApplicationContext mReactContext;
  private final Activity mMainActivity;

  private Uri mCameraCaptureURI;
  private Uri mCropImagedUri;
  private Callback mCallback;
  private Boolean noData = false;
  private Boolean tmpImage;
  private Boolean allowEditing = false;
  private int maxWidth = 0;
  private int maxHeight = 0;
  private int aspectX = 0;
  private int aspectY = 0;
  private int quality = 100;
  private int angle = 0;
  private Boolean forceAngle = false;
  WritableMap response;

  public ImagePickerModule(ReactApplicationContext reactContext, Activity mainActivity) {
    super(reactContext);

    mReactContext = reactContext;
    mMainActivity = mainActivity;
  }

  @Override
  public String getName() {
    return "UIImagePickerManager"; // To coincide with the iOS native module name
  }

  @ReactMethod
  public void showImagePicker(final ReadableMap options, final Callback callback) {
      response = Arguments.createMap();

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

      ArrayAdapter<String> adapter = new ArrayAdapter<String>(mMainActivity,
                           android.R.layout.select_dialog_item, option);
       AlertDialog.Builder builder = new AlertDialog.Builder(mMainActivity);
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
        * override onCancel method to callback cancel in case of a touch
        * outside of the dialog or the BACK key pressed
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
    response = Arguments.createMap();

    if (options.hasKey("noData")) {
        noData = options.getBoolean("noData");
    }
    if (options.hasKey("maxWidth")) {
        maxWidth = options.getInt("maxWidth");
    }
    if (options.hasKey("maxHeight")) {
        maxHeight = options.getInt("maxHeight");
    }
    if (options.hasKey("aspectX")) {
        aspectX = options.getInt("aspectX");
    }
    if (options.hasKey("aspectY")) {
        aspectY = options.getInt("aspectY");
    }
    if (options.hasKey("quality")) {
        quality = (int)(options.getDouble("quality") * 100);
    }
    tmpImage = true;
    if (options.hasKey("storageOptions")) {
        tmpImage = false;
    }
    if (options.hasKey("allowsEditing")) {
        allowEditing = options.getBoolean("allowsEditing");
    }
    forceAngle = false;
    if (options.hasKey("angle")) {
        forceAngle = true;
        angle = options.getInt("angle");
    }

    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    if (cameraIntent.resolveActivity(mMainActivity.getPackageManager()) == null) {
        response.putString("error", "Cannot launch camera");
        callback.invoke(response);
        return;
    }

    // we create a tmp file to save the result
    File path = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES);
    File imageFile;
    try {
        // Make sure the Pictures directory exists.
        path.mkdirs();
        imageFile = File.createTempFile("capture", ".jpg", path);
    } catch (IOException e) {
        e.printStackTrace();
        response.putString("error", e.getMessage());
        callback.invoke(response);
        return;
    }
    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile));
    mCameraCaptureURI = Uri.fromFile(imageFile);
    mCallback = callback;

    try {
        mMainActivity.startActivityForResult(cameraIntent, REQUEST_LAUNCH_CAMERA);
    }
    catch(ActivityNotFoundException e) {
        e.printStackTrace();
    }
  }

  // NOTE: Currently not reentrant / doesn't support concurrent requests
  @ReactMethod
  public void launchImageLibrary(final ReadableMap options, final Callback callback) {
    response = Arguments.createMap();

    if (options.hasKey("noData")) {
        noData = options.getBoolean("noData");
    }
    if (options.hasKey("maxWidth")) {
        maxWidth = options.getInt("maxWidth");
    }
    if (options.hasKey("maxHeight")) {
        maxHeight = options.getInt("maxHeight");
    }
    if (options.hasKey("aspectX")) {
        aspectX = options.getInt("aspectX");
    }
    if (options.hasKey("aspectY")) {
        aspectY = options.getInt("aspectY");
    }
    if (options.hasKey("quality")) {
        quality = (int)(options.getDouble("quality") * 100);
    }
    tmpImage = true;
    if (options.hasKey("storageOptions")) {
        tmpImage = false;
    }
    if (options.hasKey("allowsEditing")) {
        allowEditing = options.getBoolean("allowsEditing");
    }
    forceAngle = false;
    if (options.hasKey("angle")) {
        forceAngle = true;
        angle = options.getInt("angle");
    }

    Intent libraryIntent = new Intent(Intent.ACTION_PICK,
        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

    if (libraryIntent.resolveActivity(mMainActivity.getPackageManager()) == null) {
        response.putString("error", "Cannot launch photo library");
        callback.invoke(response);
        return;
    }

    mCallback = callback;

    try {
        mMainActivity.startActivityForResult(libraryIntent, REQUEST_LAUNCH_IMAGE_LIBRARY);
    } catch(ActivityNotFoundException e) {
        e.printStackTrace();
    }
  }

  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    //robustness code
    if (mCallback == null || (mCameraCaptureURI == null && requestCode == REQUEST_LAUNCH_CAMERA)
            || (requestCode != REQUEST_LAUNCH_CAMERA && requestCode != REQUEST_LAUNCH_IMAGE_LIBRARY
            && requestCode != REQUEST_IMAGE_CROPPING)) {
      return;
    }

    // user cancel
    if (resultCode != Activity.RESULT_OK) {
      response.putBoolean("didCancel", true);
      mCallback.invoke(response);
      return;
    }

    Uri uri;
    switch (requestCode)
    {
        case REQUEST_LAUNCH_CAMERA:
            uri = mCameraCaptureURI;
            break;
        case REQUEST_IMAGE_CROPPING:
            uri = mCropImagedUri;
            break;
        default:
            uri = data.getData();
    }

    if (requestCode != REQUEST_IMAGE_CROPPING && allowEditing == true) {
        Intent cropIntent = new Intent("com.android.camera.action.CROP"); 
        cropIntent.setDataAndType(uri, "image/*");
        cropIntent.putExtra("crop", "true");

        if (aspectX > 0 && aspectY > 0) {
          // aspectX:aspectY, the ratio of width to height
          cropIntent.putExtra("aspectX", aspectX);
          cropIntent.putExtra("aspectY", aspectY);
          cropIntent.putExtra("scale", true);
        }

        // we create a file to save the result
        File imageFile = createNewFile();
        mCropImagedUri = Uri.fromFile(imageFile);
        cropIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCropImagedUri);

        try {
            mMainActivity.startActivityForResult(cropIntent, REQUEST_IMAGE_CROPPING);
        } catch(ActivityNotFoundException e) {
            e.printStackTrace();
        }
        return;
    }

    String realPath = getRealPathFromURI(uri);
    boolean isUrl = false;

    if (realPath != null) {
      try {
        URL url = new URL(realPath);
        isUrl = true;
      } catch (MalformedURLException e) {
        // not a url
      }
    }

    if (realPath ==  null || isUrl) {
      try {
        File file = createFileFromURI(uri);
        realPath = file.getAbsolutePath();
        uri = Uri.fromFile(file);
      }
      catch(Exception e) {
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
        boolean isVertical = true ;
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_270:
                isVertical = false ;
                CurrentAngle = 270;
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                isVertical = false ;
                CurrentAngle = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                CurrentAngle = 180;
                break;
        }
        response.putBoolean("isVertical", isVertical);
    } catch (IOException e) {
        e.printStackTrace();
        response.putString("error", e.getMessage());
        mCallback.invoke(response);
        return;
    }

    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    Bitmap photo = BitmapFactory.decodeFile(realPath, options);
    int initialWidth = options.outWidth;
    int initialHeight = options.outHeight;

    // don't create a new file if contraint are respected
    if (((initialWidth < maxWidth && maxWidth > 0) || maxWidth == 0)
            && ((initialHeight < maxHeight && maxHeight > 0) || maxHeight == 0)
            && quality == 100 && (!forceAngle || (forceAngle && CurrentAngle == angle))) {
        response.putInt("width", initialWidth);
        response.putInt("height", initialHeight);
    } else {
        File resized = getResizedImage(getRealPathFromURI(uri), initialWidth, initialHeight);
        realPath = resized.getAbsolutePath();
        uri = Uri.fromFile(resized);
        photo = BitmapFactory.decodeFile(realPath, options);
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
    String[] projection = { MediaStore.Images.Media.DATA };
    Cursor cursor = mMainActivity.getContentResolver().query(uri, projection, null, null, null);
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
   * @doc => https://github.com/nostra13/Android-Universal-Image-Loader#load--display-task-flow
   * 
   * @param uri
   * @return File
   * @throws Exception 
   */
  private File createFileFromURI(Uri uri) throws Exception {
    File file = new File(mReactContext.getCacheDir(), "photo-" + uri.getLastPathSegment());
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

  private String getBase64StringFromFile (String absoluteFilePath) {
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
   * Create a resized image to fill the maxWidth/maxHeight values,the
   * quality value and the angle value
   *
   * @param realPath
   * @param initialWidth
   * @param initialHeight
   * @return resized file
   */
  private File getResizedImage (final String realPath, final int initialWidth, final int initialHeight) {
    Bitmap photo = BitmapFactory.decodeFile(realPath);

    Bitmap scaledphoto = null;
    if (maxWidth == 0) {
        maxWidth = initialWidth;
    }
    if (maxHeight == 0) {
        maxHeight = initialHeight;
    }
    double widthRatio = (double)maxWidth / initialWidth;
    double heightRatio = (double)maxHeight / initialHeight;

    double ratio = (widthRatio < heightRatio)
            ? widthRatio
            : heightRatio;

    Matrix matrix = new Matrix();
    matrix.postRotate(angle);
    matrix.postScale((float)ratio, (float)ratio);
    
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

    // recycle to avoid java.lang.OutOfMemoryError
    if (photo != null) {
        scaledphoto.recycle();
        photo.recycle();
        scaledphoto = null;
        photo = null;
    }
    return f;
  }

  /**
   * Create a new file
   *
   * @return an empty file
   */
  private File createNewFile() {
    String filename = "image-" + UUID.randomUUID().toString() + ".jpg";
    if (tmpImage) {
      return new File(mReactContext.getCacheDir(), filename);
    } else {
      File path = Environment.getExternalStoragePublicDirectory(
          Environment.DIRECTORY_PICTURES);
      File f = new File(path, filename);

      try {
        path.mkdirs();
        f.createNewFile();
      }
      catch (IOException e) {
        e.printStackTrace();
      }
      return f;
    }
  }
}
