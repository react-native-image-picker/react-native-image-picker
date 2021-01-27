package com.imagepicker

import android.app.Activity
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import com.otaliastudios.cameraview.*
import java.io.File

class CameraActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_camera)
    val camera: CameraView = findViewById(R.id.camera)
    val cap: String = intent.getStringExtra("location")
    camera.setLifecycleOwner(this)
    camera.addCameraListener(object : CameraListener() {
      override fun onPictureTaken(result: PictureResult) {
        val file = File(cap)
        result.toFile(file
        ) {
          setResult(Activity.RESULT_OK)
          finish()
        };
      }

      override fun onVideoTaken(result: VideoResult) {

      }
    })
    findViewById<ImageButton>(R.id.capturePicture).setOnClickListener {
      camera.takePicture()
    }
  }

}
