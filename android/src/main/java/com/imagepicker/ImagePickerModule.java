package com.imagepicker;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.util.Base64;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;

import com.facebook.react.ReactActivity;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.imagepicker.permissions.PermissionUtils;
import com.imagepicker.permissions.OnImagePickerPermissionsCallback;
import com.imagepicker.utils.RealPathUtil;
import com.imagepicker.utils.UI;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import com.facebook.react.modules.core.PermissionListener;

public class ImagePickerModule extends ReactContextBaseJavaModule
        implements ActivityEventListener
{

  static final int REQUEST_LAUNCH_IMAGE_CAPTURE    = 13001;
  static final int REQUEST_LAUNCH_IMAGE_LIBRARY    = 13002;
  static final int REQUEST_LAUNCH_VIDEO_LIBRARY    = 13003;
  static final int REQUEST_LAUNCH_VIDEO_CAPTURE    = 13004;
  static final int REQUEST_PERMISSIONS_FOR_CAMERA  = 14001;
  static final int REQUEST_PERMISSIONS_FOR_LIBRARY = 14002;

  private final ReactApplicationContext reactContext;
  private final int dialogThemeId;

  private Callback callback;
  private ReadableMap options;
  private Uri cameraCaptureURI;
  private Boolean noData = false;
  private Boolean pickVideo = false;
  private int maxWidth = 0;
  private int maxHeight = 0;
  private int quality = 100;
  private int rotation = 0;
  private int videoQuality = 1;
  private int videoDurationLimit = 0;
  private Boolean saveToCameraRoll = false;
  private ResponseHelper responseHelper = new ResponseHelper();
  private PermissionListener listener = new PermissionListener()
  {
    public boolean onRequestPermissionsResult(final int requestCode,
                                              @NonNull final String[] permissions,
                                              @NonNull final int[] grantResults)
    {
      boolean permissionsGranted = true;
      for (int i = 0; i < permissions.length; i++)
      {
        final boolean granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
        permissionsGranted = permissionsGranted && granted;
      }

      if (callback == null || options == null)
      {
        return false;
      }

      if (!permissionsGranted)
      {
        responseHelper.invokeError(callback, "Permissions weren't granted");
        return false;
      }

      switch (requestCode)
      {
        case REQUEST_PERMISSIONS_FOR_CAMERA:
          launchCamera(options, callback);
          break;

        case REQUEST_PERMISSIONS_FOR_LIBRARY:
          launchImageLibrary(options, callback);
          break;

      }
      return true;
    }
  };

  public ImagePickerModule(ReactApplicationContext reactContext,
                           @StyleRes final int dialogThemeId)
  {
    super(reactContext);

    this.dialogThemeId = dialogThemeId;
    this.reactContext = reactContext;
    this.reactContext.addActivityEventListener(this);
  }

  @Override
  public String getName() {
    return "ImagePickerManager";
  }

  @ReactMethod
  public void showImagePicker(final ReadableMap options, final Callback callback) {
    Activity currentActivity = getCurrentActivity();

    if (currentActivity == null)
    {
      responseHelper.invokeError(callback, "can't find current Activity");
      return;
    }

    this.callback = callback;
    this.options = options;

    final AlertDialog dialog = UI.chooseDialog(this, options, new UI.OnAction()
    {
      @Override
      public void onTakePhoto()
      {
        launchCamera(options, callback);
      }

      @Override
      public void onUseLibrary()
      {
        launchImageLibrary(options, callback);
      }

      @Override
      public void onCancel()
      {
        doOnCancel();
      }

      @Override
      public void onCustomButton(@NonNull final String action)
      {
        responseHelper.invokeCustomButton(callback, action);
      }
    });
    dialog.show();
  }

  public void doOnCancel()
  {
    responseHelper.invokeCancel(callback);
  }

  // NOTE: Currently not reentrant / doesn't support concurrent requests
  @ReactMethod
  public void launchCamera(final ReadableMap options, final Callback callback) {
    if (!isCameraAvailable()) {
      responseHelper.invokeError(callback, "Camera not available");
      return;
    }

    final Activity currentActivity = getCurrentActivity();
    if (currentActivity == null) {
      responseHelper.invokeError(callback, "can't find current Activity");
      return;
    }

    this.options = options;

    if (!permissionsCheck(currentActivity, callback, REQUEST_PERMISSIONS_FOR_CAMERA))
    {
      return;
    }

    parseOptions(this.options);

    int requestCode;
    Intent cameraIntent;
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
      cameraCaptureURI = compatUriFromFile(reactContext, imageFile);
      if (cameraCaptureURI == null) {
        responseHelper.invokeError(callback, "Couldn't get file path for photo");
        return;
      }
      cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraCaptureURI);
    }

    if (cameraIntent.resolveActivity(reactContext.getPackageManager()) == null) {
      responseHelper.invokeError(callback, "Cannot launch camera");
      return;
    }

    this.callback = callback;

    try {
      currentActivity.startActivityForResult(cameraIntent, requestCode);
    } catch (ActivityNotFoundException e) {
      e.printStackTrace();
      responseHelper.invokeError(callback, "Cannot launch camera");
    }
  }

  // NOTE: Currently not reentrant / doesn't support concurrent requests
  @ReactMethod
  public void launchImageLibrary(final ReadableMap options, final Callback callback)
  {
    final Activity currentActivity = getCurrentActivity();
    if (currentActivity == null) {
      responseHelper.invokeError(callback, "can't find current Activity");
      return;
    }

    this.options = options;

    if (!permissionsCheck(currentActivity, callback, REQUEST_PERMISSIONS_FOR_LIBRARY))
    {
      return;
    }

    parseOptions(this.options);

    int requestCode;
    Intent libraryIntent;
    if (pickVideo) {
      requestCode = REQUEST_LAUNCH_VIDEO_LIBRARY;
      libraryIntent = new Intent(Intent.ACTION_PICK);
      libraryIntent.setType("video/*");
    } else {
      requestCode = REQUEST_LAUNCH_IMAGE_LIBRARY;
      libraryIntent = new Intent(Intent.ACTION_PICK,
      MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
    }

    if (libraryIntent.resolveActivity(reactContext.getPackageManager()) == null) {
      responseHelper.invokeError(callback, "Cannot launch photo library");
      return;
    }

    this.callback = callback;

    try {
      currentActivity.startActivityForResult(libraryIntent, requestCode);
    } catch (ActivityNotFoundException e) {
      e.printStackTrace();
      responseHelper.invokeError(callback, "Cannot launch photo library");
    }
  }

  @Override
  public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
    //robustness code
    if (callback == null || (cameraCaptureURI == null && requestCode == REQUEST_LAUNCH_IMAGE_CAPTURE)
            || (requestCode != REQUEST_LAUNCH_IMAGE_CAPTURE && requestCode != REQUEST_LAUNCH_IMAGE_LIBRARY
            && requestCode != REQUEST_LAUNCH_VIDEO_LIBRARY && requestCode != REQUEST_LAUNCH_VIDEO_CAPTURE)) {
      return;
    }

    responseHelper.cleanResponse();

    // user cancel
    if (resultCode != Activity.RESULT_OK) {
      responseHelper.invokeCancel(callback);
      callback = null;
      return;
    }

    Uri uri;
    switch (requestCode) {
      case REQUEST_LAUNCH_IMAGE_CAPTURE:
        uri = cameraCaptureURI;
        this.fileScan(uri.getPath());
        break;
      case REQUEST_LAUNCH_IMAGE_LIBRARY:
        uri = data.getData();
        break;
      case REQUEST_LAUNCH_VIDEO_LIBRARY:
        responseHelper.putString("uri", data.getData().toString());
        responseHelper.putString("path", getRealPathFromURI(data.getData()));
        responseHelper.invokeResponse(callback);
        callback = null;
        return;
      case REQUEST_LAUNCH_VIDEO_CAPTURE:
        final String path = getRealPathFromURI(data.getData());
        responseHelper.putString("uri", data.getData().toString());
        responseHelper.putString("path", path);
        this.fileScan(path);
        responseHelper.invokeResponse(callback);
        callback = null;
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
        responseHelper.putString("error", "Could not read photo");
        responseHelper.putString("uri", uri.toString());
        responseHelper.invokeResponse(callback);
        callback = null;
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
        responseHelper.putDouble("latitude", latitude);
        responseHelper.putDouble("longitude", longitude);
      }

      final String timestamp = exif.getAttribute(ExifInterface.TAG_DATETIME);
      final SimpleDateFormat exifDatetimeFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");

      final DateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
      isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

      try {
        final String isoFormatString = new StringBuilder(isoFormat.format(exifDatetimeFormat.parse(timestamp)))
                .append("Z").toString();
        responseHelper.putString("timestamp", isoFormatString);
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
      responseHelper.putInt("originalRotation", currentRotation);
      responseHelper.putBoolean("isVertical", isVertical);
    } catch (IOException e) {
      e.printStackTrace();
      responseHelper.invokeError(callback, e.getMessage());
      callback = null;
      return;
    }

    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(realPath, options);
    int initialWidth = options.outWidth;
    int initialHeight = options.outHeight;

    // don't create a new file if contraint are respected
    if (((initialWidth < maxWidth && maxWidth > 0) || maxWidth == 0) && ((initialHeight < maxHeight && maxHeight > 0) || maxHeight == 0) && quality == 100 && (rotation == 0 || currentRotation == rotation)) {
      responseHelper.putInt("width", initialWidth);
      responseHelper.putInt("height", initialHeight);
    } else {
      File resized = getResizedImage(realPath, initialWidth, initialHeight);
      if (resized == null) {
        responseHelper.putString("error", "Can't resize the image");
      } else {
         realPath = resized.getAbsolutePath();
         uri = Uri.fromFile(resized);
         BitmapFactory.decodeFile(realPath, options);
         responseHelper.putInt("width", options.outWidth);
         responseHelper.putInt("height", options.outHeight);
      }
    }

    if (saveToCameraRoll && requestCode == REQUEST_LAUNCH_IMAGE_CAPTURE) {
      final File oldFile = new File(uri.getPath());
      final File newDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
      final File newFile = new File(newDir.getPath(), uri.getLastPathSegment());

      try {
        moveFile(oldFile, newFile);
        uri = Uri.fromFile(newFile);
        realPath = newFile.getAbsolutePath();
      } catch (IOException e) {
        e.printStackTrace();
        responseHelper.putString("error", "Error moving image to camera roll: " + e.getMessage());
      }
    }

    responseHelper.putString("uri", uri.toString());
    responseHelper.putString("path", realPath);

    if (!noData) {
      responseHelper.putString("data", getBase64StringFromFile(realPath));
    }

    putExtraFileInfo(realPath, responseHelper);

    responseHelper.invokeResponse(callback);
    callback = null;
    this.options = null;
  }

  /**
   * Move a file from one location to another.
   *
   * This is done via copy + deletion, because Android will throw an error
   * if you try to move a file across mount points, e.g. to the SD card.
   */
  private void moveFile(final File oldFile, final File newFile) throws IOException {
    FileChannel oldChannel = null;
    FileChannel newChannel = null;

    try {
      oldChannel = new FileInputStream(oldFile).getChannel();
      newChannel = new FileOutputStream(newFile).getChannel();
      oldChannel.transferTo(0, oldChannel.size(), newChannel);

      oldFile.delete();
    } finally {
      try {
        if (oldChannel != null) oldChannel.close();
        if (newChannel != null) newChannel.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
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

  private boolean permissionsCheck(@NonNull final Activity activity,
                                   @NonNull final Callback callback,
                                   @NonNull final int requestCode)
  {
    final int writePermission = ActivityCompat
            .checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    final int cameraPermission = ActivityCompat
            .checkSelfPermission(activity, Manifest.permission.CAMERA);

    final boolean permissionsGrated = writePermission == PackageManager.PERMISSION_GRANTED &&
            cameraPermission == PackageManager.PERMISSION_GRANTED;

    if (!permissionsGrated)
    {
      final Boolean dontAskAgain = ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA);

      if (dontAskAgain)
      {
        final AlertDialog dialog = PermissionUtils
                .explainingDialog(this, options, new PermissionUtils.OnExplainingPermissionCallback()
                {
                  @Override
                  public void onCancel(WeakReference<ImagePickerModule> moduleInstance,
                                       DialogInterface dialogInterface)
                  {
                    final ImagePickerModule module = moduleInstance.get();
                    if (module == null)
                    {
                      return;
                    }
                    module.doOnCancel();
                  }

                  @Override
                  public void onReTry(WeakReference<ImagePickerModule> moduleInstance,
                                      DialogInterface dialogInterface)
                  {
                    final ImagePickerModule module = moduleInstance.get();
                    if (module == null)
                    {
                      return;
                    }
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", module.getContext().getPackageName(), null);
                    intent.setData(uri);
                    final Activity innerActivity = module.getActivity();
                    if (innerActivity == null)
                    {
                      return;
                    }
                    innerActivity.startActivityForResult(intent, 1);
                  }
                });
        dialog.show();
//        responseHelper.invokeError(callback, "Permissions weren't granted");
        return false;
      }
      else
      {
        String[] PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA};
        if (activity instanceof ReactActivity)
        {
          ((ReactActivity) activity).requestPermissions(PERMISSIONS, requestCode, listener);
        }
        else if (activity instanceof OnImagePickerPermissionsCallback)
        {
          ((OnImagePickerPermissionsCallback) activity).setPermissionListener(listener);
          ActivityCompat.requestPermissions(activity, PERMISSIONS, requestCode);
        }
        else
        {
          final String errorDescription = new StringBuilder(activity.getClass().getSimpleName())
                  .append(" must implement ")
                  .append(OnImagePickerPermissionsCallback.class.getSimpleName())
                  .toString();
          throw new UnsupportedOperationException(errorDescription);
        }
        return false;
      }
    }
    return true;
  }

  private boolean isCameraAvailable() {
    return reactContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)
      || reactContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
  }

  private @NonNull String getRealPathFromURI(@NonNull final Uri uri) {
    return RealPathUtil.getRealPathFromURI(reactContext, uri);
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
    File file = new File(reactContext.getExternalCacheDir(), "photo-" + uri.getLastPathSegment());
    InputStream input = reactContext.getContentResolver().openInputStream(uri);
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
    if (maxWidth == 0 || maxWidth > initialWidth) {
      maxWidth = initialWidth;
    }
    if (maxHeight == 0 || maxWidth > initialHeight) {
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
    String filename = new StringBuilder("image-")
            .append(UUID.randomUUID().toString())
            .append(".jpg")
            .toString();
    File path = reactContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);

    File f = new File(path, filename);
    try {
      path.mkdirs();
      f.createNewFile();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return f;
  }

  private void putExtraFileInfo(@NonNull final String path,
                                @NonNull final ResponseHelper responseHelper)
  {
    // size && filename
    try {
      File f = new File(path);
      responseHelper.putDouble("fileSize", f.length());
      responseHelper.putString("fileName", f.getName());
    } catch (Exception e) {
      e.printStackTrace();
    }

    // type
    String extension = MimeTypeMap.getFileExtensionFromUrl(path);
    if (extension != null) {
      responseHelper.putString("type", MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension));
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
    saveToCameraRoll = false;
    if (options.hasKey("storageOptions")) {
      final ReadableMap storageOptions = options.getMap("storageOptions");
      if (storageOptions.hasKey("cameraRoll")) {
        saveToCameraRoll = storageOptions.getBoolean("cameraRoll");
      }
    }
  }

  public void fileScan(String path) {
    MediaScannerConnection.scanFile(reactContext,
            new String[] { path }, null,
            new MediaScannerConnection.OnScanCompletedListener() {

              public void onScanCompleted(String path, Uri uri) {
                Log.i("TAG", "Finished scanning " + path);
              }
            });
  }

  @Override
  public void onNewIntent(Intent intent) { }

  public Context getContext()
  {
    return getReactApplicationContext();
  }

  public @StyleRes int getDialogThemeId()
  {
    return this.dialogThemeId;
  }

  public @NonNull Activity getActivity()
  {
    return getCurrentActivity();
  }

  private static @Nullable Uri compatUriFromFile(@NonNull final Context context,
                                                 @NonNull final File file)
  {
    Uri result = null;
    if (Build.VERSION.SDK_INT < 21)
    {
      result = Uri.fromFile(file);
    }
    else
    {
      final String packageName = context.getApplicationContext().getPackageName();
      final String authority =  new StringBuilder(packageName).append(".provider").toString();
      try
      {
        result = FileProvider.getUriForFile(context, authority, file);
      }
      catch(IllegalArgumentException e)
      {
        e.printStackTrace();
      }
    }
    return result;
  }
}
