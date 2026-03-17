package com.imagepicker;

import static java.lang.Integer.parseInt;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.IOException;

// MetadataRetriever only implements AutoCloseable starting with Android API 29
// So let's use our own wrapper for it
// See https://stackoverflow.com/a/74808462/1377358
class CustomMediaMetadataRetriever extends MediaMetadataRetriever implements AutoCloseable {
    public CustomMediaMetadataRetriever() {
        super();
    }

    @Override
    public void close() throws IOException {
        release();
    }
}

public class VideoMetadata extends Metadata {
    private int duration;
    private int bitrate;

    public VideoMetadata(Uri uri, Context context) {
        try (CustomMediaMetadataRetriever metadataRetriever = new CustomMediaMetadataRetriever()) {
            metadataRetriever.setDataSource(context, uri);

            String duration = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            String bitrate = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
            String datetime = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE);

            // Extract anymore metadata here...
            if (duration != null) this.duration = Math.round(Float.parseFloat(duration)) / 1000;
            if (bitrate != null) this.bitrate = parseInt(bitrate);

            if (datetime != null) {
                int dotIndex = datetime.indexOf(".");
                String datetimeToFormat = dotIndex > 0
                        ? datetime.substring(0, dotIndex) + "+GMT"
                        : datetime + "+GMT";
                this.datetime = getDateTimeInUTC(datetimeToFormat, "yyyyMMdd'T'HHmmss+zzz");
            }
            if (this.datetime == null) {
                this.datetime = getDateTimeFromMediaStore(uri, context);
            }
            if (this.datetime == null) {
                this.datetime = getDateTimeFromFile(uri);
            }

            String width = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String height = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);

            if (height != null && width != null) {
                String rotation = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
                int rotationI = rotation == null ? 0 : Integer.parseInt(rotation);

                if (rotationI == 90 || rotationI == 270) {
                    this.width = Integer.parseInt(height);
                    this.height = Integer.parseInt(width);
                } else {
                    this.width = Integer.parseInt(width);
                    this.height = Integer.parseInt(height);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getDateTimeFromMediaStore(Uri uri, Context context) {
        String[] projection = new String[]{
                MediaStore.Video.Media.DATE_TAKEN,
                MediaStore.MediaColumns.DATE_ADDED
        };
        try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int takenIndex = cursor.getColumnIndex(MediaStore.Video.Media.DATE_TAKEN);
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
            Log.e("RNIP", "Could not get video date from MediaStore: " + e.getMessage());
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
            Log.e("RNIP", "Could not get video date from file: " + e.getMessage());
        }
        return null;
    }

    public int getBitrate() {
        return bitrate;
    }

    public int getDuration() {
        return duration;
    }

    @Override
    public String getDateTime() {
        return datetime;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }
}
