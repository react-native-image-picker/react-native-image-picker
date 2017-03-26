package com.imagepicker.utils;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.facebook.react.bridge.ReadableMap;

/**
 * Created by rusfearuth on 22.02.17.
 */

public class ReadableMapUtils
{
    public static @NonNull boolean hasAndNotEmpty(@NonNull final ReadableMap target,
                                                  @NonNull final String key)
    {
        if (!target.hasKey(key))
        {
            return false;
        }

        final String value = target.getString(key);

        return !TextUtils.isEmpty(value);
    }
}
