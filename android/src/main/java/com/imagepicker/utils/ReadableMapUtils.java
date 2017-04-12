package com.imagepicker.utils;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.facebook.react.bridge.ReadableMap;

/**
 * Created by rusfearuth on 22.02.17.
 */

public class ReadableMapUtils
{
    public static @NonNull boolean hasAndNotEmpty(@NonNull Class clazz,
                                                  @NonNull final ReadableMap target,
                                                  @NonNull final String key)
    {
        if (!target.hasKey(key))
        {
            return false;
        }

        if (target.isNull(key))
        {
            return false;
        }

        if (String.class.equals(clazz))
        {
            final String value = target.getString(key);
            return !TextUtils.isEmpty(value);
        }

        return true;
    }


    public static @NonNull boolean hasAndNotNullReadableMap(@NonNull final ReadableMap target,
                                                            @NonNull final String key)
    {
        return hasAndNotEmpty(ReadableMap.class, target, key);
    }



    public static @NonNull boolean hasAndNotEmptyString(@NonNull final ReadableMap target,
                                                        @NonNull final String key)
    {
        return hasAndNotEmpty(String.class, target, key);
    }
}
