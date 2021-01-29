package com.imagepicker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * We need the accelerometer sensor to understand when the orientation changes
 * see: http://stackoverflow.com/questions/33052905/setting-androidscreenorientation-sensorlandscape-prevents-actiity-restart/33053521#33053521
 */
public class CameraFragment extends Fragment implements SensorEventListener {
  private static final String TAG = "CameraFragment";
  private static final int MY_PERMISSIONS_REQUEST_CAMERA = 11;
  private final int VIDEO_EDIT_INTENT = 1004;

  FrameLayout mPreviewContainer;
  Button mTakePictureView;
  View mShutterAnimationView;

  private int mCameraId;
  private Camera mCamera;
  private CameraPreview mPreview;

  private boolean mCameraPermissionGranted = false;
  Camera.Size pictureSize;

  /**
   * After a picture is taken from the camera, the preview can start after the 2 events have occoured:
   * 1. The picture file saved
   * 2. The btn_shutter animation completed.
   * This field keeps track of the event that has occoured. see {@link #startPreviewIfPossible()}
   */
  private boolean mCanStartPreview;
  private String fileStoragePath = null;
  private SensorManager mSensorManager;
  private Sensor mAccelerometer;

  private int mCurrentDisplayRotation = -1;

  public int getLayout() {
    return R.layout.fragment_camera;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(getLayout(), container, false);
    mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
    mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    mPreviewContainer = view.findViewById(R.id.previewContainer);
    mTakePictureView = view.findViewById(R.id.takePictureView);
    mShutterAnimationView = view.findViewById(R.id.shutterAnimationView);
    if (getArguments() != null) {
      fileStoragePath = getArguments().getString(StaticStringKeys.FILE_LOCATION, "");
    }
    mTakePictureView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        takePictureClicked(mTakePictureView);
      }
    });
    return view;
  }

  @Override
  public void onResume() {
    super.onResume();
    if (mCameraPermissionGranted) {
      tryToGetCamera();
    }
  }

  public void doneClicked() {
    Intent intent = new Intent();
    if (isConnectedToActivity()) {
      getActivity().setResult(Activity.RESULT_OK, intent);
      getActivity().finish();
    }
  }

  public void finishCancel() {
    if (getActivity() != null && !getActivity().isFinishing()) {
      getActivity().setResult(Activity.RESULT_CANCELED);
      getActivity().finish();
    }
  }

  void takePictureClicked(Button btn) {
    mCanStartPreview = false;
    mTakePictureView.setClickable(false);

    Animation shutterAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.fade_in_out);
    shutterAnimation.setAnimationListener(new Animation.AnimationListener() {
      @Override
      public void onAnimationStart(Animation animation) {
      }

      @Override
      public void onAnimationEnd(Animation animation) {
        startPreviewIfPossible();
      }

      @Override
      public void onAnimationRepeat(Animation animation) {
      }
    });
    mShutterAnimationView.setVisibility(View.VISIBLE);
    mShutterAnimationView.startAnimation(shutterAnimation);

    // null in case of emulator
    if (mCamera != null) {
      // get an image from the camera
      mCamera.takePicture(null, null, new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
          // Create a media file name
          File pictureFile = new File(fileStoragePath);
          try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(data);
            fos.close();
            doneClicked();
          } catch (FileNotFoundException e) {
            e.printStackTrace();
          } catch (IOException e) {
            e.printStackTrace();
          }
          CameraFragment.this.startPreviewIfPossible();
        }
      });
    } else {
    }
  }

  /**
   * Preview will start when this method is called twice (before {@link #takePictureClicked(Button btn)} is called).
   * See {@link #mCanStartPreview}
   */
  private void startPreviewIfPossible() {
    if (mCanStartPreview) {
      mCamera.startPreview();
      if (mTakePictureView != null) {
        mTakePictureView.setClickable(true);
      }
    } else {
      mCanStartPreview = true;
    }
  }

  public void setmCameraPermissionGranted(boolean granted) {
    mCameraPermissionGranted = granted;
    if (mCameraPermissionGranted && isResumed()) {
      tryToGetCamera();
    }
  }

  private void tryToGetCamera() {
    mCamera = null;
    try {
      mCameraId = getFirstRearCameraId();
      mCamera = Camera.open(mCameraId); // attempt to get a Camera instance

      // now we camera parameters one by one (to get proper error traces)
      try {
        // try to set auto focus
        Camera.Parameters parameters = mCamera.getParameters();
//        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
//        parameters.set("cam_mode", 1);
//        mCamera.setParameters(parameters);

        // try to set picture size
        parameters = mCamera.getParameters();
        pictureSize = getBestPictureSize(parameters.getSupportedPictureSizes());
        parameters.setPictureSize(pictureSize.width, pictureSize.height);
        mCamera.setParameters(parameters);

        // try to set preview size
        parameters = mCamera.getParameters();
        Camera.Size previewSize = getBestPreviewSize(pictureSize, parameters.getSupportedPreviewSizes());
        parameters.setPreviewSize(previewSize.width, previewSize.height);
        mCamera.setParameters(parameters);
      } catch (Exception e) {
        // camera will not work as expected
        if (isConnectedToActivity()) {
          Toast.makeText(getActivity().getApplicationContext(), "Pictures from this camera would be of bad quality. Please use your gallery to take pictures/video.", Toast.LENGTH_LONG).show();
          getActivity().finish();
        }
      }
      mCamera.setErrorCallback(new Camera.ErrorCallback() {
        @Override
        public void onError(int error, Camera camera1) {
          mPreview = new CameraPreview(CameraFragment.this.getContext(), CameraFragment.this.mCamera);
        }
      });
      mPreviewContainer.addView(mPreview);
      checkAndSetDisplayOrientation(getActivity().getWindowManager().getDefaultDisplay().getRotation());
    } catch (Exception e) {
      e.printStackTrace();
      showAlert();
    }
  }

  protected boolean isConnectedToActivity() {
    return getActivity() != null && !getActivity().isFinishing();
  }

  public void showAlert() {
    new AlertDialog.Builder(getActivity())
      .setCancelable(false)
      .setTitle("Could not access camera")
      .setMessage("Make sure it is not being used by any other app by killing other apps." +
        "\n\nIf the problem persists, take pictures/video from your camera app and add them" +
        "to your video using the gallery button")
      .setPositiveButton("Try again", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          dialog.dismiss();
          CameraFragment.this.tryToGetCamera();
        }
      }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        dialog.dismiss();
        CameraFragment.this.finishCancel();
      }
    })
      .show();
  }

  @Override
  public void onPause() {
    super.onPause();
    // release all resources (as we aren't finishing this activity when we open other activities)
    mSensorManager.unregisterListener(this);
    if (mPreview != null) {
      mPreview.releaseCamera();
    }
    mPreview = null;
    if (mCamera != null) {
      mCamera.stopPreview();
      mCamera.release();
      mCamera = null;
    }

    mPreviewContainer.removeAllViews();
  }

  public void onAccuracyChanged(Sensor sensor, int accuracy) {
  }

  public void onSensorChanged(SensorEvent event) {
    if (getActivity() == null) {
      return;
    }
    int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
    Log.d(TAG, "Sensor changed: " + rotation);
    if (mCurrentDisplayRotation != rotation) {
      mCurrentDisplayRotation = rotation;
      checkAndSetDisplayOrientation(rotation);
    }
  }

  private static int getFirstRearCameraId() {
    // Search for the front facing camera
    for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
      Camera.CameraInfo info = new Camera.CameraInfo();
      Camera.getCameraInfo(i, info);
      if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
        return i;
      }
    }
    return -1;
  }


  private static int getFirstFrontCameraId() {
    // Search for the front facing camera
    for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
      Camera.CameraInfo info = new Camera.CameraInfo();
      Camera.getCameraInfo(i, info);
      if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
        return i;
      }
    }
    return -1;
  }

  private int getOrientation(int rotation) {
    if (mCamera == null)
      return -1;
    Camera.CameraInfo info = new Camera.CameraInfo();
    Camera.getCameraInfo(mCameraId, info);
    int degrees = 0;
    switch (rotation) {
      case Surface.ROTATION_90:
        degrees = 90;
        break;
      case Surface.ROTATION_270:
        degrees = 270;
        break;
    }

    int result;
    if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
      result = (info.orientation + degrees) % 360;
      result = (360 - result) % 360;  // compensate the mirror
    } else {  // back-facing
      result = (info.orientation - degrees + 360) % 360;
    }
    return result;
  }

  private void checkAndSetDisplayOrientation(int rotation) {
    int result = getOrientation(rotation);
    if (result == -1) {
      return;
    }
    mCamera.setDisplayOrientation(result);
    // pirated from http://stackoverflow.com/a/18874394/1396264
    // also tested on Samsung Galaxy s3- works fine
    // STEP #2: Set the 'rotation' parameter
    Camera.Parameters params = mCamera.getParameters();
    params.setRotation(result);
    mCamera.setParameters(params);
  }

  private Camera.Size getBestPreviewSize(Camera.Size pictureSize, List<Camera.Size> sizes) throws RuntimeException {
    List<Camera.Size> goodSizes = new ArrayList<>(); // sizes with an acceptable aspect ratio
    for (Camera.Size size : sizes) {
      if (size.width == AndroidUtils.PHOTO_WIDTH_PIXELS &&
        size.height == AndroidUtils.PHOTO_HEIGHT_PIXELS) { // perfect
        return size;
      } else if (doSizesMatch(pictureSize, size)) {
        goodSizes.add(size);
      }
    }
    if (goodSizes.size() > 0) {
      // just pick the largest size of good sizes:
      return getMax(goodSizes);
    } else {
      throw new RuntimeException(String.format("no matching preview for picture size %d x %d",
        pictureSize.width, pictureSize.height));
    }
  }

  /**
   * @return true if the ratio of both sizes is (almost) equal
   */
  private boolean doSizesMatch(Camera.Size pictureSize, Camera.Size size) {
    return Math.abs((float) size.width / size.height -
      (float) pictureSize.width / pictureSize.height) < 0.00001;
  }

  private Camera.Size getBestPictureSize(List<Camera.Size> sizes) {
    List<Camera.Size> goodSizes = new ArrayList<>(); // sizes with an acceptable aspect ratio
    for (Camera.Size size : sizes) {
      if (size.width == AndroidUtils.PHOTO_WIDTH_PIXELS &&
        size.height == AndroidUtils.PHOTO_HEIGHT_PIXELS) { // perfect
        return size;
      } else if (isSizeGood(size)) {
        goodSizes.add(size);
      }
    }
    if (goodSizes.size() > 0) {
      // just pick the largest picture size of good sizes:
      return getMax(goodSizes);
    } else { // worst case
      // there will always be one preview size
      Camera.Size size = getMax(sizes);
      return size;
    }
  }

  private static Camera.Size getMax(List<Camera.Size> goodSizes) {
    return Collections.max(goodSizes, new Comparator<Camera.Size>() {
      @Override
      public int compare(Camera.Size lhs, Camera.Size rhs) {
        return lhs.width - rhs.width;
      }
    });
  }

  private static boolean isSizeGood(Camera.Size size) {
    float ratio = (float) size.width / size.height;
    return Math.abs(ratio - AndroidUtils.DEFAULT_ASPECT_RATIO) < 0.00001;
  }
}
