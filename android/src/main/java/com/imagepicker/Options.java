package com.imagepicker;

import com.facebook.react.bridge.ReadableMap;
import android.text.TextUtils;

public class Options {
    int selectionLimit;
    Boolean includeBase64;
    Boolean includeExtra;
    int videoQuality = 1;
    int quality;
    int maxWidth;
    int maxHeight;
    Boolean saveToPhotos;
    int durationLimit;
    Boolean useFrontCamera = false;
    String mediaType;


    Options(ReadableMap options) {
        mediaType = options.getString("mediaType");
        selectionLimit = options.getInt("selectionLimit");
        includeBase64 = options.getBoolean("includeBase64");
        includeExtra = options.getBoolean("includeExtra");

        String videoQualityString = options.getString("videoQuality");
        if(!TextUtils.isEmpty(videoQualityString) && !videoQualityString.toLowerCase().equals("high")) {
            videoQuality = 0;
        }

        if (options.getString("cameraType").equals("front")) {
            useFrontCamera = true;
        }

        quality = (int) (options.getDouble("quality") * 100);
        maxHeight = options.getInt("maxHeight");
        maxWidth = options.getInt("maxWidth");
        saveToPhotos = options.getBoolean("saveToPhotos");
        durationLimit = options.getInt("durationLimit");
    }
}
