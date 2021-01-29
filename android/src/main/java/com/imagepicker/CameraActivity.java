package com.imagepicker;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class CameraActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 11;

    private static final String TAG = "CameraActivity";

    CameraFragment mCameraFragment;
    boolean askingCameraPermission = false;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Taken from http://developer.android.com/training/system-ui/navigation.html to hide buttons
//        View decorView = getWindow().getDecorView();
//        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
//        decorView.setSystemUiVisibility(uiOptions);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        mCameraFragment = new CameraFragment();
        Bundle args = new Bundle();
        args.putString(StaticStringKeys.FILE_LOCATION, getIntent().getStringExtra(StaticStringKeys.FILE_LOCATION));
        mCameraFragment.setArguments(args);
        //fragmentTransaction.add(R.id.fragment_container, mCameraFragment, "MyActivity");
        fragmentTransaction.commit();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_REQUEST_CAMERA);
            askingCameraPermission = true;
        } else {
            mCameraFragment.setmCameraPermissionGranted(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    askingCameraPermission = false;
                    mCameraFragment.setmCameraPermissionGranted(true);
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    finish();
                    Toast.makeText(this, "Please provide camera permission.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
