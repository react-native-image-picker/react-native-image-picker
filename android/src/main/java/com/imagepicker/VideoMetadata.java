package com.imagepicker;

import static java.lang.Integer.parseInt;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

public class VideoMetadata {
  int duration;
  int bitrate;

  public VideoMetadata(Uri uri, Context context) {
    MediaMetadataRetriever m = new MediaMetadataRetriever();
    m.setDataSource(context, uri);
    this.duration = Math.round(Float.parseFloat(m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))) / 1000;
    this.bitrate = parseInt(m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE));
    m.release();
  }

  public int getBitrate() {
    return bitrate;
  }

  public int getDuration() {
    return duration;
  }
}
