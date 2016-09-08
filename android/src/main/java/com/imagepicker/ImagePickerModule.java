package com.imagepicker;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.util.Base64;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.webkit.MimeTypeMap;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.WritableMap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

public class ImagePickerModule extends ReactContextBaseJavaModule implements ActivityEventListener {

  static final int REQUEST_LAUNCH_IMAGE_CAPTURE = 1;
  static final int REQUEST_LAUNCH_IMAGE_LIBRARY = 2;
  static final int REQUEST_LAUNCH_VIDEO_LIBRARY = 3;
  static final int REQUEST_LAUNCH_VIDEO_CAPTURE = 4;

  private final ReactApplicationContext mReactContext;

  private Uri mCameraCaptureURI;
  private Callback mCallback;
  private Boolean noData = false;
  private Boolean tmpImage;
  private Boolean pickVideo = false;
  private int maxWidth = 0;
  private int maxHeight = 0;
  private int quality = 100;
  private int rotation = 0;
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

    if (currentActivity == null) {
      response = Arguments.createMap();
      response.putString("error", "can't find current Activity");
      callback.invoke(response);
      return;
    }

    final List<String> titles = new ArrayList<String>();
    final List<String> actions = new ArrayList<String>();

    if (options.hasKey("takePhotoButtonTitle")
            && options.getString("takePhotoButtonTitle") != null
            && !options.getString("takePhotoButtonTitle").isEmpty()
            && isCameraAvailable()) {
      titles.add(options.getString("takePhotoButtonTitle"));
      actions.add("photo");
    }
    if (options.hasKey("chooseFromLibraryButtonTitle")
            && options.getString("chooseFromLibraryButtonTitle") != null
            && !options.getString("chooseFromLibraryButtonTitle").isEmpty()) {
      titles.add(options.getString("chooseFromLibraryButtonTitle"));
      actions.add("library");
    }
    if (options.hasKey("customButtons")) {
      ReadableArray customButtons = options.getArray("customButtons");
      for (int i = 0; i < customButtons.size(); i++) {
        ReadableMap button = customButtons.getMap(i);
        int currentIndex = titles.size();
        titles.add(currentIndex, button.getString("title"));
        actions.add(currentIndex, button.getString("name"));
      }
    }
    String cancelButtonTitle = options.getString("cancelButtonTitle");
    titles.add(cancelButtonTitle);
    actions.add("cancel");

    ArrayAdapter<String> adapter = new ArrayAdapter<String>(currentActivity,
            android.R.layout.select_dialog_item, titles);
    AlertDialog.Builder builder = new AlertDialog.Builder(currentActivity);
    if (options.hasKey("title") && options.getString("title") != null && !options.getString("title").isEmpty()) {
      builder.setTitle(options.getString("title"));
    }

    builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int index) {
        String action = actions.get(index);
        response = Arguments.createMap();

        switch (action) {
          case "photo":
            launchCamera(options, callback);
            break;
          case "library":
            launchImageLibrary(options, callback);
            break;
          case "cancel":
            response.putBoolean("didCancel", true);
            callback.invoke(response);
            break;
          default: // custom button
            response.putString("customButton", action);
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
        response = Arguments.createMap();
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

    if (!isCameraAvailable()) {
        response.putString("error", "Camera not available");
        callback.invoke(response);
        return;
    }

    Activity currentActivity = getCurrentActivity();
    if (currentActivity == null) {
      response.putString("error", "can't find current Activity");
      callback.invoke(response);
      return;
    }

    if (!permissionsCheck(currentActivity)) {
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
      File imageFile = createNewFile();
      mCameraCaptureURI = Uri.fromFile(imageFile);
      cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCameraCaptureURI);
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
      response = Arguments.createMap();
      response.putString("error", "Cannot launch camera");
      callback.invoke(response);
    }
  }

  // NOTE: Currently not reentrant / doesn't support concurrent requests
  @ReactMethod
  public void launchImageLibrary(final ReadableMap options, final Callback callback) {
    int requestCode;
    Intent libraryIntent;
    Activity currentActivity = getCurrentActivity();

    if (currentActivity == null) {
      response = Arguments.createMap();
      response.putString("error", "can't find current Activity");
      callback.invoke(response);
      return;
    }

    if (!permissionsCheck(currentActivity)) {
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
      MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
    }

    if (libraryIntent.resolveActivity(mReactContext.getPackageManager()) == null) {
      response = Arguments.createMap();
      response.putString("error", "Cannot launch photo library");
      callback.invoke(response);
      return;
    }

    mCallback = callback;

    try {
      currentActivity.startActivityForResult(libraryIntent, requestCode);
    } catch (ActivityNotFoundException e) {
      e.printStackTrace();
      response = Arguments.createMap();
      response.putString("error", "Cannot launch photo library");
      callback.invoke(response);
    }
  }

  @Override
  public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
    //robustness code
    if (mCallback == null || (mCameraCaptureURI == null && requestCode == REQUEST_LAUNCH_IMAGE_CAPTURE)
            || (requestCode != REQUEST_LAUNCH_IMAGE_CAPTURE && requestCode != REQUEST_LAUNCH_IMAGE_LIBRARY
            && requestCode != REQUEST_LAUNCH_VIDEO_LIBRARY && requestCode != REQUEST_LAUNCH_VIDEO_CAPTURE)) {
      return;
    }

    response = Arguments.createMap();

    // user cancel
    if (resultCode != Activity.RESULT_OK) {
      response.putBoolean("didCancel", true);
      mCallback.invoke(response);
      return;
    }

    Uri uri;
    switch (requestCode) {
      case REQUEST_LAUNCH_IMAGE_CAPTURE:
        uri = mCameraCaptureURI;
        this.fileScan(uri.getPath());
        break;
      case REQUEST_LAUNCH_IMAGE_LIBRARY:
        uri = data.getData();
        break;
      case REQUEST_LAUNCH_VIDEO_LIBRARY:
        response.putString("uri", data.getData().toString());
        response.putString("path", getRealPathFromURI(data.getData()));
        mCallback.invoke(response);
        return;
      case REQUEST_LAUNCH_VIDEO_CAPTURE:
        response.putString("uri", data.getData().toString());
        response.putString("path", getRealPathFromURI(data.getData()));
        this.fileScan(response.getString("path"));
        mCallback.invoke(response);
        return;
      default:
        uri = null;
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

    int currentRotation = 0;
    try {
      ExifInterface exif = new ExifInterface(realPath);

      // extract lat, long, and timestamp and add to the response
      float[] latlng = new float[2];
      exif.getLatLong(latlng);
      float latitude = latlng[0];
      float longitude = latlng[1];
      if(latitude != 0f || longitude != 0f) {
        response.putDouble("latitude", latitude);
        response.putDouble("longitude", longitude);
      }

      String timestamp = exif.getAttribute(ExifInterface.TAG_DATETIME);
      SimpleDateFormat exifDatetimeFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");

      DateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
      isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

      try {
        String isoFormatString = isoFormat.format(exifDatetimeFormat.parse(timestamp)) + "Z";
        response.putString("timestamp", isoFormatString);
      } catch (Exception e) {}

      int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
      boolean isVertical = true;
      switch (orientation) {
        case ExifInterface.ORIENTATION_ROTATE_270:
          isVertical = false;
          currentRotation = 270;
          break;
        case ExifInterface.ORIENTATION_ROTATE_90:
          isVertical = false;
          currentRotation = 90;
          break;
        case ExifInterface.ORIENTATION_ROTATE_180:
          currentRotation = 180;
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
    int initialWidth = options.outWidth;
    int initialHeight = options.outHeight;

    // don't create a new file if contraint are respected
    if (((initialWidth < maxWidth && maxWidth > 0) || maxWidth == 0) && ((initialHeight < maxHeight && maxHeight > 0) || maxHeight == 0) && quality == 100 && (rotation == 0 || currentRotation == rotation)) {
      response.putInt("width", initialWidth);
      response.putInt("height", initialHeight);
    } else {
      File resized = getResizedImage(realPath, initialWidth, initialHeight);
      if (resized == null) {
        response.putString("error", "Can't resize the image");
      } else {
         realPath = resized.getAbsolutePath();
         uri = Uri.fromFile(resized);
         response.putInt("width", options.outWidth);
         response.putInt("height", options.outHeight);
      }
    }

    response.putString("uri", uri.toString());
    response.putString("path", realPath);

    if (!noData) {
      response.putString("data", getBase64StringFromFile(realPath));
    }

    putExtraFileInfo(realPath, response);

    mCallback.invoke(response);
  }

  /**
   * Returns number of milliseconds since Jan. 1, 1970, midnight local time.
   * Returns -1 if the date time information if not available.
   * copied from ExifInterface.java
   * @hide
   */
  private static long parseTimestamp(String dateTimeString, String subSecs) {
    if (dateTimeString == null) return -1;

    SimpleDateFormat sFormatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault());
    sFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    ParsePosition pos = new ParsePosition(0);
    try {
      // The exif field is in local time. Parsing it as if it is UTC will yield time
      // since 1/1/1970 local time
      Date datetime = sFormatter.parse(dateTimeString, pos);
      if (datetime == null) return -1;
      long msecs = datetime.getTime();

      if (subSecs != null) {
        try {
          long sub = Long.valueOf(subSecs);
          while (sub > 1000) {
            sub /= 10;
          }
          msecs += sub;
        } catch (NumberFormatException e) {
          //expected
        }
      }
      return msecs;
    } catch (IllegalArgumentException ex) {
      return -1;
    }
  }

  private boolean permissionsCheck(Activity activity) {
    int writePermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    int cameraPermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);
    if (writePermission != PackageManager.PERMISSION_GRANTED || cameraPermission != PackageManager.PERMISSION_GRANTED) {
      String[] PERMISSIONS = {
              Manifest.permission.WRITE_EXTERNAL_STORAGE,
              Manifest.permission.CAMERA
      };
      ActivityCompat.requestPermissions(activity, PERMISSIONS, 1);
      return false;
    }
    return true;
  }


  private boolean isCameraAvailable() {
    return mReactContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)
      || mReactContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
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
    File file = new File(mReactContext.getExternalCacheDir(), "photo-" + uri.getLastPathSegment());
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
      inputStream = new FileInputStream(new File(absoluteFilePath));
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
   * Create a resized image to fulfill the maxWidth/maxHeight, quality and rotation values
   *
   * @param realPath
   * @param initialWidth
   * @param initialHeight
   * @return resized file
   */
  private File getResizedImage(final String realPath, final int initialWidth, final int initialHeight) {
    Options options = new BitmapFactory.Options();
    options.inScaled = false;
    Bitmap photo = BitmapFactory.decodeFile(realPath, options);

    if (photo == null) {
        return null;
    }

    Bitmap scaledphoto = null;
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
    matrix.postRotate(rotation);
    matrix.postScale((float) ratio, (float) ratio);

    ExifInterface exif;
    try {
      exif = new ExifInterface(realPath);

      int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);

      if (orientation == 6) {
        matrix.postRotate(90);
      } else if (orientation == 3) {
        matrix.postRotate(180);
      } else if (orientation == 8) {
        matrix.postRotate(270);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

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
    File path;
    if (tmpImage) {
      path = mReactContext.getExternalCacheDir();
    } else {
      path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
    }

    File f = new File(path, filename);
    try {
      path.mkdirs();
      f.createNewFile();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return f;
  }

  private void putExtraFileInfo(final String path, WritableMap response) {
    // size && filename
    try {
      File f = new File(path);
      response.putDouble("fileSize", f.length());
      response.putString("fileName", f.getName());
    } catch (Exception e) {
      e.printStackTrace();
    }

    // type
    String extension = MimeTypeMap.getFileExtensionFromUrl(path);
    if (extension != null) {
      response.putString("type", MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension));
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
    quality = 100;
    if (options.hasKey("quality")) {
      quality = (int) (options.getDouble("quality") * 100);
    }
    tmpImage = true;
    if (options.hasKey("storageOptions")) {
      tmpImage = false;
    }
    rotation = 0;
    if (options.hasKey("rotation")) {
      rotation = options.getInt("rotation");
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

  public void fileScan(String path){
    MediaScannerConnection.scanFile(mReactContext,
            new String[] { path }, null,
            new MediaScannerConnection.OnScanCompletedListener() {

              public void onScanCompleted(String path, Uri uri) {
                Log.i("TAG", "Finished scanning " + path);
              }
            });
  }

  // Required for RN 0.30+ modules than implement ActivityEventListener
  public void onNewIntent(Intent intent) { }
}
