package com.imagepicker;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import androidx.exifinterface.media.ExifInterface;
import java.io.InputStream;

public class ImageMetadata extends Metadata {
  public ImageMetadata(Uri uri, Context context) {
    try {
      InputStream inputStream = context.getContentResolver().openInputStream(uri);
      ExifInterface exif = new ExifInterface(inputStream);
      String datetimeTag = exif.getAttribute(ExifInterface.TAG_DATETIME);

      // Extract anymore metadata here...
      if(datetimeTag != null) this.datetime = getDateTimeInUTC(datetimeTag, "yyyy:MM:dd HH:mm:ss");
    } catch (Exception e) {
      // This error does not bubble up to RN as we don't want failed datetime retrieval to prevent selection
      Log.e("RNIP", "Could not load image metadata: " + e.getMessage());
    }
  }

  @Override
  public String getDateTime() { return datetime; }
  
  // At the moment we are not using the ImageMetadata class to get width/height
  // TODO: to use this class for extracting image width and height in the future
  @Override
  public int getWidth() { return 0; }
  @Override
  public int getHeight() { return 0; }
}
