package com.imagepicker.media;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.facebook.react.bridge.ReadableMap;

import java.io.File;

/**
 * Created by rusfearuth on 16.03.17.
 */

public class VideoConfig
{
    public @Nullable final File original;

    public VideoConfig(@Nullable final File original)
    {
        this.original = original;
    }

    public @NonNull VideoConfig withOriginalFile(@Nullable final File original)
    {
        return new VideoConfig(original);
    }

    public File getActualFile()
    {
        return original;
    }
}
