package com.imagepicker;

import com.facebook.react.bridge.ReadableMap;

import android.text.TextUtils;

public class Options {
    int selectionLimit;
    Boolean includeBase64;
    Boolean includeExtra;
    int videoQuality = 1;
    int quality;
    int conversionQuality = 92;
    Boolean convertToJpeg = true;
    int maxWidth;
    int maxHeight;
    Boolean saveToPhotos;
    int durationLimit;
    Boolean useFrontCamera = false;
    String mediaType;
    String[] restrictMimeTypes;

    Options(ReadableMap options) {
        mediaType = options.getString("mediaType");
        restrictMimeTypes = options.getArray("restrictMimeTypes").toArrayList().stream()
                                .map(Object::toString)
                                .toArray(size -> new String[size]);
        selectionLimit = options.getInt("selectionLimit");
        includeBase64 = options.getBoolean("includeBase64");
        includeExtra = options.getBoolean("includeExtra");

        String videoQualityString = options.getString("videoQuality");
        if (!TextUtils.isEmpty(videoQualityString) && !videoQualityString.toLowerCase().equals("high")) {
            videoQuality = 0;
        }

        if (options.hasKey("conversionQuality")) {
            conversionQuality = (int) (options.getDouble("conversionQuality") * 100);
        }

        String assetRepresentationMode = options.getString("assetRepresentationMode");
        if (!TextUtils.isEmpty(assetRepresentationMode) && assetRepresentationMode.toLowerCase().equals("current")) {
            convertToJpeg = false;
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
