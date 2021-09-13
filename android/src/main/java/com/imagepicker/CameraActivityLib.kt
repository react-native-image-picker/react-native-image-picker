package com.imagepicker

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.CameraView
import com.otaliastudios.cameraview.PictureResult
import com.otaliastudios.cameraview.VideoResult
import java.io.File

class CameraActivityLib : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_camera)
    val camera: CameraView = findViewById(R.id.camera)
    val cap: String? = intent.getStringExtra(StaticStringKeys.FILE_LOCATION)
    camera.setLifecycleOwner(this)
    camera.addCameraListener(object : CameraListener() {
      override fun onPictureTaken(result: PictureResult) {
        cap?.let {
          val file = File(it)
          result.toFile(
            file
          ) {
            setResult(Activity.RESULT_OK)
            finish()
          }
        } ?: finish()
      }

      override fun onVideoTaken(result: VideoResult) {

      }
    })
    val shutterAnimation =
      AnimationUtils.loadAnimation(
        this,
        R.anim.fade_in_out
      )
    shutterAnimation.setAnimationListener(object : Animation.AnimationListener {
      override fun onAnimationStart(animation: Animation) {}
      override fun onAnimationEnd(animation: Animation) {
      }

      override fun onAnimationRepeat(animation: Animation) {}
    })
    val shutterAnimationView: View = findViewById(R.id.shutterAnimationView)
    findViewById<Button>(R.id.takePictureView).setOnClickListener {
      shutterAnimationView.visibility = View.VISIBLE
      shutterAnimationView.startAnimation(shutterAnimation)
      camera.takePicture()
    }
  }

}
