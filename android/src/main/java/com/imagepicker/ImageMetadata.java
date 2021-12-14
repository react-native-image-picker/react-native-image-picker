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
}
