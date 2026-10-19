package com.imagepicker;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.exifinterface.media.ExifInterface;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageMetadata extends Metadata {
    public ImageMetadata(Uri uri, Context context) {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            ExifInterface exif = new ExifInterface(inputStream);
            String datetimeTag = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL);
            if (datetimeTag == null)
                datetimeTag = exif.getAttribute(ExifInterface.TAG_DATETIME);

            // Extract anymore metadata here...
            if (datetimeTag != null)
                this.datetime = getDateTimeInUTC(datetimeTag, "yyyy:MM:dd HH:mm:ss");
        } catch (Exception e) {
            // This error does not bubble up to RN as we don't want failed datetime retrieval to prevent selection
            Log.e("RNIP", "Could not load image metadata: " + e.getMessage());
        }

        if (this.datetime == null)
            this.datetime = getMediaStoreDateTime(uri, context);
    }

    private String getMediaStoreDateTime(Uri uri, Context context) {
        if (!"content".equals(uri.getScheme())) return null;
        try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor == null || !cursor.moveToFirst()) return null;
            long millis = getLong(cursor, MediaStore.Images.Media.DATE_TAKEN);
            if (millis <= 0) millis = getLong(cursor, MediaStore.Images.Media.DATE_MODIFIED) * 1000;
            if (millis <= 0) return null;
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).format(new Date(millis));
        } catch (Exception e) {
            Log.e("RNIP", "Could not load media store datetime: " + e.getMessage());
            return null;
        }
    }

    private static long getLong(Cursor cursor, String column) {
        int index = cursor.getColumnIndex(column);
        return index == -1 || cursor.isNull(index) ? 0 : cursor.getLong(index);
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
