package com.imagepicker;

import android.util.Log;

import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

abstract class Metadata {
  protected String datetime;
  protected int height;
  protected int width;

  abstract public String getDateTime();
  abstract public int getWidth();
  abstract public int getHeight();

  /**
   * Converts a timestamp to a UTC timestamp
   *
   * @param value - timestamp
   * @param format - input format
   * @return formatted timestamp
   */
  protected @Nullable
  String getDateTimeInUTC(String value, String format) {
    try {
      Date datetime = new SimpleDateFormat(format, Locale.US).parse(value);
      SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);

      if (datetime != null) {
        return formatter.format(datetime);
      }

      return null;
    } catch (Exception e) {
      // This error does not bubble up to RN as we don't want failed datetime parsing to prevent selection
      Log.e("RNIP", "Could not parse image datetime to UTC: " + e.getMessage());
      return null;
    }
  }
}
