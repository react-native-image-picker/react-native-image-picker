package com.imagepicker;

import com.facebook.react.bridge.ReadableMap;

public class Options {
    Boolean pickVideo = false;
    Boolean noData;
    String videoQuality;
    String chooseWhichLibraryTitle;
    int quality;
    int maxWidth;
    int maxHeight;


    Options(ReadableMap options) {
        if (options.getString("mediaType").equals("video")) {
            pickVideo = true;
        }
        noData = options.getBoolean("noData");
        videoQuality = options.getString("videoQuality");
        chooseWhichLibraryTitle = options.getString("chooseWhichLibraryTitle");
        quality = (int) (options.getDouble("quality") * 100);
        maxHeight = options.getInt("maxHeight");
        maxWidth = options.getInt("maxWidth");
    }
}
