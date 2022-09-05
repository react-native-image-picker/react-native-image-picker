package com.imagepicker;

import static java.lang.Integer.parseInt;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;

public class VideoMetadata extends Metadata {
  private int duration;
  private int bitrate;

  public VideoMetadata(Uri uri, Context context) {
    MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
    metadataRetriever.setDataSource(context, uri);
    Bitmap bitmap = getBitmap(uri, context, metadataRetriever);

    String duration = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
    String bitrate = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
    String datetime = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE);

    // Extract anymore metadata here...
    if(duration != null) this.duration = Math.round(Float.parseFloat(duration)) / 1000;
    if(bitrate != null) this.bitrate = parseInt(bitrate);

    if(datetime != null) {
      // METADATA_KEY_DATE gives us the following format: "20211214T102646.000Z"
      // This format is very hard to parse, so we convert it to "20211214 102646" ("yyyyMMdd HHmmss")
      String datetimeToFormat = datetime.substring(0, datetime.indexOf(".")).replace("T", " ");
      this.datetime = getDateTimeInUTC(datetimeToFormat, "yyyyMMdd HHmmss");
    }

    if(bitmap != null) {
      this.width = bitmap.getWidth();
      this.height = bitmap.getHeight();
    }

    try {
      metadataRetriever.release();
    } catch (IOException e) {
      Log.e("VideoMetadata", "IO error releasing metadataRetriever", e);
    }
  }

  public int getBitrate() {
    return bitrate;
  }
  public int getDuration() {
    return duration;
  }
  @Override
  public String getDateTime() { return datetime; }
  @Override
  public int getWidth() { return width; }
  @Override
  public int getHeight() { return height; }

  private @Nullable
  Bitmap getBitmap(Uri uri, Context context, MediaMetadataRetriever retriever) {
    try {
      FileDescriptor fileDescriptor = context.getContentResolver().openFileDescriptor(uri, "r").getFileDescriptor();
      FileInputStream inputStream = new FileInputStream(fileDescriptor);
      retriever.setDataSource(inputStream.getFD());
      return retriever.getFrameAtTime();
    } catch (IOException | RuntimeException e) {
      // These errors do not bubble up to RN as we don't want failed width/height retrieval to prevent selection.
      Log.e("RNIP", "Could not retrieve width and height from video: " + e.getMessage());
    }

    return null;
  }
}
