package com.imagepicker;

import com.facebook.react.bridge.ReadableMap;

import android.os.Build;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

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
    String androidCameraPackage;
    List<String> preselectedAssetIds;

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

        if (options.hasKey("preselectedAssetIds")) {
            preselectedAssetIds = new ArrayList<>();
            for (Object id : options.getArray("preselectedAssetIds").toArrayList()) {
                preselectedAssetIds.add(id.toString());
            }
        }

        if( options.hasKey("androidCameraPackage")) {
            androidCameraPackage = options.getString("androidCameraPackage");
        }

        quality = (int) (options.getDouble("quality") * 100);
        maxHeight = options.getInt("maxHeight");
        maxWidth = options.getInt("maxWidth");
        saveToPhotos = options.getBoolean("saveToPhotos");
        durationLimit = options.getInt("durationLimit");
    }

    // The system photo picker supports pre-selection from Android 16 (API 36), for multiple selection only.
    boolean appliesPreselection() {
        return preselectedAssetIds != null && selectionLimit != 1 && Build.VERSION.SDK_INT >= 36;
    }
}
