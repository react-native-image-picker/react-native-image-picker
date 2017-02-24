package com.imagepicker.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.ArrayAdapter;

import com.facebook.react.bridge.ReadableMap;
import com.imagepicker.Buttons;

import java.util.List;

/**
 * @author Alexander Ustinov
 */
public class UI
{
    public static void showDialog(@Nullable final Context context,
                                  @NonNull final ReadableMap options,
                                  @Nullable final OnAction callback)
    {
        final Buttons buttons = Buttons.newInstance(options);
        final List<String> titles = buttons.getTitles();
        final List<String> actions = buttons.getActions();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                context,
                android.R.layout.select_dialog_item,
                titles
        );
        AlertDialog.Builder builder = new AlertDialog.Builder(context, android.R.style.Theme_Holo_Light_Dialog);
        if (ReadableMapUtils.hasAndNotEmpty(options, "title"))
        {
            builder.setTitle(options.getString("title"));
        }

        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int index) {
                final String action = actions.get(index);

                switch (action) {
                    case "photo":
                        callback.onTakePhoto();
                        break;

                    case "library":
                        callback.onUseLibrary();
                        break;

                    case "cancel":
                        callback.onCancel();
                        break;

                    default:
                        callback.onCustomButton(action);
                }
            }
        });

        final AlertDialog dialog = builder.create();

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener()
        {
            @Override
            public void onCancel(@NonNull final DialogInterface dialog)
            {
                callback.onDialogWasCanceled("didCancel");
                dialog.dismiss();
            }
        });

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.show();
    }

    public interface OnAction
    {
        void onTakePhoto();
        void onUseLibrary();
        void onCancel();
        void onCustomButton(String action);
        void onDialogWasCanceled(String action);
    }
}
