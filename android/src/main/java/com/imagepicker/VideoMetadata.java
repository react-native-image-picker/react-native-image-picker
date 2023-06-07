package com.imagepicker;

import static java.lang.Integer.parseInt;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;

public class VideoMetadata extends Metadata {
  private int duration;
  private int bitrate;

  public VideoMetadata(Uri uri, Context context) {
    MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
    metadataRetriever.setDataSource(context, uri);

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

    String width = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
    String height = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);

    if(height != null && width != null) {
      String rotation = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
      int rotationI = rotation == null ? 0 : Integer.parseInt(rotation);

      if(rotationI == 90 || rotationI == 270) {
        this.width = Integer.parseInt(height);
        this.height = Integer.parseInt(width);
      } else {
        this.width = Integer.parseInt(width);
        this.height = Integer.parseInt(height);
      }
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
}
