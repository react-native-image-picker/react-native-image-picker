package com.imagepicker;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import androidx.exifinterface.media.ExifInterface;

import java.io.InputStream;
import java.io.File;

import android.provider.MediaStore;

public class ImageMetadata extends Metadata {
    public ImageMetadata(Uri uri, Context context) {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            ExifInterface exif = new ExifInterface(inputStream);
            String datetimeTag = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL);
            if (datetimeTag == null) {
                datetimeTag = exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED);
            }
            if (datetimeTag == null) {
                datetimeTag = exif.getAttribute(ExifInterface.TAG_DATETIME);
            }
            if (datetimeTag != null) {
                this.datetime = getDateTimeInUTC(datetimeTag, "yyyy:MM:dd HH:mm:ss");
            }
        } catch (Exception e) {
            Log.e("RNIP", "Could not load image metadata: " + e.getMessage());
        }
        if (this.datetime == null) {
            this.datetime = getDateTimeFromMediaStore(uri, context);
        }
        if (this.datetime == null) {
            this.datetime = getDateTimeFromFile(uri);
        }
    }

    private String getDateTimeFromMediaStore(Uri uri, Context context) {
        String[] projection = new String[]{
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.MediaColumns.DATE_ADDED
        };
        try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int takenIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN);
                if (takenIndex >= 0) {
                    long dateTaken = cursor.getLong(takenIndex);
                    if (dateTaken > 0) {
                        return getDateTimeInUTCFromMillis(dateTaken);
                    }
                }
                int addedIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED);
                if (addedIndex >= 0) {
                    long dateAdded = cursor.getLong(addedIndex);
                    if (dateAdded > 0) {
                        return getDateTimeInUTCFromMillis(dateAdded * 1000L);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("RNIP", "Could not get date from MediaStore: " + e.getMessage());
        }
        return null;
    }

    private String getDateTimeFromFile(Uri uri) {
        if (uri == null || !"file".equalsIgnoreCase(uri.getScheme())) {
            return null;
        }
        try {
            String path = uri.getPath();
            if (path != null) {
                File file = new File(path);
                if (file.exists()) {
                    long lastModified = file.lastModified();
                    if (lastModified > 0) {
                        return getDateTimeInUTCFromMillis(lastModified);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("RNIP", "Could not get date from file: " + e.getMessage());
        }
        return null;
    }

    @Override
    public String getDateTime() {
        return datetime;
    }

    // At the moment we are not using the ImageMetadata class to get width/height
    // TODO: to use this class for extracting image width and height in the future
    @Override
    public int getWidth() {
        return 0;
    }

    @Override
    public int getHeight() {
        return 0;
    }
}
