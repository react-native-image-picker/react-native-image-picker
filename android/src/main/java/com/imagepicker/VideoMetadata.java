package com.imagepicker;

import static java.lang.Integer.parseInt;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

public class VideoMetadata {
  int duration;
  int bitrate;

  public VideoMetadata(Uri uri, Context context) {
    MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
    metadataRetriever.setDataSource(context, uri);
    String duration = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
    String bitrate = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
    // Extract anymore metadata here...

    this.duration = Math.round(Float.parseFloat(duration)) / 1000;
    this.bitrate = parseInt(bitrate);
    metadataRetriever.release();
  }

  public int getBitrate() {
    return bitrate;
  }

  public int getDuration() {
    return duration;
  }
}
