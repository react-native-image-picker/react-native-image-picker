package com.imagepicker.media;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.facebook.react.bridge.ReadableMap;

import java.io.File;

/**
 * Created by rusfearuth on 15.03.17.
 */

public class ImageConfig
{
    public @Nullable final File original;
    public @Nullable final File resized;
    public final int maxWidth;
    public final int maxHeight;
    public final int quality;
    public final int rotation;
    public final boolean saveToCameraRoll;

    public ImageConfig(@Nullable final File original,
                       @Nullable final File resized,
                       final int maxWidth,
                       final int maxHeight,
                       final int quality,
                       final int rotation,
                       final boolean saveToCameraRoll)
    {
        this.original = original;
        this.resized = resized;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.quality = quality;
        this.rotation = rotation;
        this.saveToCameraRoll = saveToCameraRoll;
    }

    public @NonNull ImageConfig withMaxWidth(final int maxWidth)
    {
        return new ImageConfig(
                this.original, this.resized, maxWidth,
                this.maxHeight, this.quality, this.rotation,
                this.saveToCameraRoll
        );
    }

    public @NonNull ImageConfig withMaxHeight(final int maxHeight)
    {
        return new ImageConfig(
                this.original, this.resized, this.maxWidth,
                maxHeight, this.quality, this.rotation,
                this.saveToCameraRoll
        );

    }

    public @NonNull ImageConfig withQuality(final int quality)
    {
        return new ImageConfig(
                this.original, this.resized, this.maxWidth,
                this.maxHeight, quality, this.rotation,
                this.saveToCameraRoll
        );
    }

    public @NonNull ImageConfig withRotation(final int rotation)
    {
        return new ImageConfig(
                this.original, this.resized, this.maxWidth,
                this.maxHeight, this.quality, rotation,
                this.saveToCameraRoll
        );
    }

    public @NonNull ImageConfig withOriginalFile(@Nullable final File original)
    {
        return new ImageConfig(
                original, this.resized, this.maxWidth,
                this.maxHeight, this.quality, this.rotation,
                this.saveToCameraRoll
        );
    }

    public @NonNull ImageConfig withResizedFile(@Nullable final File resized)
    {
        return new ImageConfig(
                this.original, resized, this.maxWidth,
                this.maxHeight, this.quality, this.rotation,
                this.saveToCameraRoll
        );
    }

    public @NonNull ImageConfig withSaveToCameraRoll(@Nullable final boolean saveToCameraRoll)
    {
        return new ImageConfig(
                this.original, this.resized, this.maxWidth,
                this.maxHeight, this.quality, this.rotation,
                saveToCameraRoll
        );
    }

    public @NonNull ImageConfig updateFromOptions(@NonNull final ReadableMap options)
    {
        int maxWidth = 0;
        if (options.hasKey("maxWidth"))
        {
            maxWidth = options.getInt("maxWidth");
        }
        int maxHeight = 0;
        if (options.hasKey("maxHeight"))
        {
            maxHeight = options.getInt("maxHeight");
        }
        int quality = 100;
        if (options.hasKey("quality"))
        {
            quality = (int) (options.getDouble("quality") * 100);
        }
        int rotation = 0;
        if (options.hasKey("rotation"))
        {
            rotation = options.getInt("rotation");
        }
        boolean saveToCameraRoll = false;
        if (options.hasKey("storageOptions"))
        {
            final ReadableMap storageOptions = options.getMap("storageOptions");
            if (storageOptions.hasKey("cameraRoll"))
            {
                saveToCameraRoll = storageOptions.getBoolean("cameraRoll");
            }
        }
        return new ImageConfig(this.original, this.resized, maxWidth, maxHeight, quality, rotation, saveToCameraRoll);
    }

    public boolean useOriginal(int initialWidth,
                               int initialHeight,
                               int currentRotation)
    {
        return ((initialWidth < maxWidth && maxWidth > 0) || maxWidth == 0) &&
                ((initialHeight < maxHeight && maxHeight > 0) || maxHeight == 0) &&
                quality == 100 && (rotation == 0 || currentRotation == rotation);
    }

    public File getActualFile()
    {
        return resized != null ? resized: original;
    }
}
