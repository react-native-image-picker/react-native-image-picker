package com.imagepicker;

import com.facebook.react.bridge.ReadableMap;
import android.text.TextUtils;

public class Options {
    Boolean pickVideo = false;
    Boolean includeBase64;
    int videoQuality = 1;
    int quality;
    int maxWidth;
    int maxHeight;
    Boolean saveToPhotos;
    int durationLimit;
    Boolean useFrontCamera = false;


    Options(ReadableMap options) {
        if (options.getString("mediaType").equals("video")) {
            pickVideo = true;
        }
        includeBase64 = options.getBoolean("includeBase64");
        
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
