package com.imagepicker;

import android.media.ExifInterface;
import android.os.Build;

import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.media.ExifInterface.*;

class ImageMetadata {

    static WritableMap extract(String path) throws IOException {
        WritableMap exifData = new WritableNativeMap();

        List<String> attributes = getBasicAttributes();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            attributes.addAll(getLevel23Attributes());
        }

        ExifInterface exif = new ExifInterface(path);

        for (String attribute : attributes) {
            String value = exif.getAttribute(attribute);
            exifData.putString(attribute, value);
        }

        return exifData;
    }

    private static List<String> getBasicAttributes() {
        return new ArrayList<>(Arrays.asList(
                TAG_APERTURE,
                TAG_DATETIME,
                TAG_EXPOSURE_TIME,
                TAG_FLASH,
                TAG_FOCAL_LENGTH,
                TAG_GPS_ALTITUDE,
                TAG_GPS_ALTITUDE_REF,
                TAG_GPS_DATESTAMP,
                TAG_GPS_LATITUDE,
                TAG_GPS_LATITUDE_REF,
                TAG_GPS_LONGITUDE,
                TAG_GPS_LONGITUDE_REF,
                TAG_GPS_PROCESSING_METHOD,
                TAG_GPS_TIMESTAMP,
                TAG_IMAGE_LENGTH,
                TAG_IMAGE_WIDTH,
                TAG_ISO,
                TAG_MAKE,
                TAG_MODEL,
                TAG_ORIENTATION,
                TAG_WHITE_BALANCE
        ));
    }

    private static List<String> getLevel23Attributes() {
        return new ArrayList<>(Arrays.asList(
                TAG_DATETIME_DIGITIZED,
                TAG_SUBSEC_TIME,
                TAG_SUBSEC_TIME_DIG,
                TAG_SUBSEC_TIME_ORIG
        ))
    }
  

  
}
