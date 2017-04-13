package com.imagepicker.testing.mock;

import android.annotation.TargetApi;
import android.media.ExifInterface;
import android.os.Build;

import org.robolectric.annotation.Implements;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

/**
 * Created by rusfearuth on 13.04.17.
 */

@Implements(ExifInterface.class)
public class MockExifInterface
        extends ExifInterface
{
    private HashMap<String, String> metadata = new HashMap<>();

    public MockExifInterface(String filename) throws
            IOException
    {
        super(filename);
    }

    @TargetApi(Build.VERSION_CODES.N)
    public MockExifInterface(FileDescriptor fileDescriptor) throws
            IOException
    {
        super(fileDescriptor);
    }

    @TargetApi(Build.VERSION_CODES.N)
    public MockExifInterface(InputStream inputStream) throws
            IOException
    {
        super(inputStream);
    }

    @Override
    public void setAttribute(String attr, String value)
    {
        metadata.put(attr, value);
    }

    @Override
    public String getAttribute(String attr)
    {
        return metadata.get(attr);
    }

    @Override
    public int getAttributeInt(String attr, int defaultValue)
    {
        if (!metadata.containsKey(attr))
        {
            return defaultValue;
        }
        return Integer.valueOf(metadata.get(attr));
    }

    @Override
    public boolean getLatLong(float[] latLong)
    {
        if (!metadata.containsKey(ExifInterface.TAG_GPS_LATITUDE) ||
                !metadata.containsKey(ExifInterface.TAG_GPS_LONGITUDE))
        {
            return false;
        }
        latLong[0] = Float.valueOf(metadata.get(ExifInterface.TAG_GPS_LATITUDE));
        latLong[1] = Float.valueOf(metadata.get(ExifInterface.TAG_GPS_LONGITUDE));
        return true;
    }
}
