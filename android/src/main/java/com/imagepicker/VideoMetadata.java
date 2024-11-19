package com.imagepicker;

import static java.lang.Integer.parseInt;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;

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
                // METADATA_KEY_DATE gives us the following format: "20211214T102646.000Z"
                // This date is always returned in UTC, so we strip the ending that `SimpleDateFormat` can't parse, and append `+GMT`
                String datetimeToFormat = datetime.substring(0, datetime.indexOf(".")) + "+GMT";
                this.datetime = getDateTimeInUTC(datetimeToFormat, "yyyyMMdd'T'HHmmss+zzz");
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
