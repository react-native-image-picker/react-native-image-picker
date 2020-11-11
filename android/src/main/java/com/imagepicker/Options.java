package com.imagepicker;

import com.facebook.react.bridge.ReadableMap;

public class Options {
    Boolean pickVideo = false;
    String type;
    Boolean includeBase64;
    String videoQuality;
    int quality;
    int maxWidth;
    int maxHeight;
    Boolean saveToPhotos;


    Options(ReadableMap options) {
        if (options.getString("mediaType").equals("video")) {
            pickVideo = true;
        }
        type = options.getString("mediaType");
        includeBase64 = options.getBoolean("includeBase64");
        videoQuality = options.getString("videoQuality");
        quality = (int) (options.getDouble("quality") * 100);
        maxHeight = options.getInt("maxHeight");
        maxWidth = options.getInt("maxWidth");
        saveToPhotos = options.getBoolean("saveToPhotos");
    }
}
