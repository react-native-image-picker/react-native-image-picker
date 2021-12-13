package com.imagepicker;

import static java.lang.Integer.parseInt;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class VideoMetadata {
  int duration;
  int bitrate;
  private Bitmap bitmap = null;

  public VideoMetadata(Uri uri, Context context) {
    MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
    metadataRetriever.setDataSource(context, uri);
    Bitmap bitmap = null;


    String duration = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
    String bitrate = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
    // Extract anymore metadata here...

    this.duration = Math.round(Float.parseFloat(duration)) / 1000;
    this.bitrate = parseInt(bitrate);
    this.bitmap = getBitmap(uri, metadataRetriever);
    metadataRetriever.release();
  }

  public int getBitrate() {
    return bitrate;
  }

  public int getDuration() {
    return duration;
  }

  public int getWidth() {
    if(this.bitmap != null) {
      return this.bitmap.getWidth();
    }

    return 0;
  }

  public int getHeight() {
    if(this.bitmap != null) {
      return this.bitmap.getHeight();
    }

    return 0;
  }

  private @Nullable
  Bitmap getBitmap(Uri uri, MediaMetadataRetriever retriever) {
    FileInputStream inputStream = null;
    File file = new File(uri.getPath());

    // These errors do not bubble up to RN as we don't want failed width/height
    // retrieval to prevent selection.
    try {
      inputStream = new FileInputStream(file.getAbsolutePath());
      retriever.setDataSource(inputStream.getFD());
      return retriever.getFrameAtTime();
    } catch (FileNotFoundException e) {
      Log.e("RNIP", "Could not retrieve width and height from video: " + e.getMessage());
    } catch (IOException e) {
      Log.e("RNIP", "Could not retrieve width and height from video: " + e.getMessage());
    } catch (RuntimeException e) {
      Log.e("RNIP", "Could not retrieve width and height from video: " + e.getMessage());
    }

    return null;
  }
}
