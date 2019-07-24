package com.imagepicker.permissions;

import android.app.Activity;
import android.content.DialogInterface;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableNativeMap;
import com.imagepicker.ImagePickerModule;

import java.lang.ref.WeakReference;

/**
 * Created by rusfearuth on 03.03.17.
 */

public class PermissionUtils
{
    public static @Nullable AlertDialog explainingDialog(@NonNull final ImagePickerModule module,
                                                         @NonNull final ReadableMap options,
                                                         @NonNull final OnExplainingPermissionCallback callback)
    {
        if (module.getContext() == null)
        {
            return null;
        }
        if (!options.hasKey("permissionDenied"))
        {
            return null;
        }
        final ReadableMap permissionDenied = options.getMap("permissionDenied");
        if (((ReadableNativeMap) permissionDenied).toHashMap().size()  == 0)
        {
            return null;
        }

        final String title = permissionDenied.getString("title");
        final String text = permissionDenied.getString("text");
        final String btnReTryTitle = permissionDenied.getString("reTryTitle");
        final String btnOkTitle = permissionDenied.getString("okTitle");
        final WeakReference<ImagePickerModule> reference = new WeakReference<>(module);

        final Activity activity = module.getActivity();

        if (activity == null)
        {
            return null;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity, module.getDialogThemeId());
        builder
                .setTitle(title)
                .setMessage(text)
                .setCancelable(false)
                .setNegativeButton(btnOkTitle, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(final DialogInterface dialogInterface,
                                        int i)
                    {
                        callback.onCancel(reference, dialogInterface);
                    }
                })
                .setPositiveButton(btnReTryTitle, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface,
                                        int i)
                    {
                        callback.onReTry(reference, dialogInterface);
                    }
                });

        return builder.create();
    }

    public interface OnExplainingPermissionCallback {
        void onCancel(WeakReference<ImagePickerModule> moduleInstance, DialogInterface dialogInterface);
        void onReTry(WeakReference<ImagePickerModule> moduleInstance, DialogInterface dialogInterface);
    }
}