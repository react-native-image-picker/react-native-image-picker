package com.imagepicker.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;

import java.util.LinkedList;
import java.util.List;


/**
 * Created by rusfearuth on 20.02.17.
 */

public class ButtonsHelper
{
    public static class Item
    {
        public final String title;
        public final String action;

        public Item(@NonNull final String title,
                    @NonNull final String action)
        {
            this.title = title;
            this.action = action;
        }
    }

    public final @Nullable Item btnCamera;
    public final @Nullable Item btnLibrary;
    public final @Nullable Item btnCancel;
    public final List<Item> customButtons;

    public ButtonsHelper(@Nullable final Item btnCamera,
                         @Nullable final Item btnLibrary,
                         @Nullable final Item btnCancel,
                         @NonNull final LinkedList<Item> customButtons)
    {
        this.btnCamera = btnCamera;
        this.btnLibrary = btnLibrary;
        this.btnCancel = btnCancel;
        this.customButtons = customButtons;
    }

    public List<String> getTitles()
    {
        List<String> result = new LinkedList<>();

        if (btnCamera != null)
        {
            result.add(btnCamera.title);
        }

        if (btnLibrary != null)
        {
            result.add(btnLibrary.title);
        }

        for (int i = 0; i < customButtons.size(); i++)
        {
            result.add(customButtons.get(i).title);
        }

        return result;
    }

    public List<String> getActions()
    {
        List<String> result = new LinkedList<>();

        if (btnCamera != null)
        {
            result.add(btnCamera.action);
        }

        if (btnLibrary != null)
        {
            result.add(btnLibrary.action);
        }

        for (int i = 0; i < customButtons.size(); i++)
        {
            result.add(customButtons.get(i).action);
        }

        return result;
    }

    public static ButtonsHelper newInstance(@NonNull final ReadableMap options)
    {
        Item btnCamera = getItemFromOption(options, "takePhotoButtonTitle", "photo");
        Item btnLibrary = getItemFromOption(options, "chooseFromLibraryButtonTitle", "library");
        Item btnCancel = getItemFromOption(options, "cancelButtonTitle", "cancel");
        LinkedList<Item> customButtons = getCustomButtons(options);

        return new ButtonsHelper(btnCamera, btnLibrary, btnCancel, customButtons);
    }

    private static @Nullable Item getItemFromOption(@NonNull final ReadableMap options,
                                                    @NonNull final String key,
                                                    @NonNull final String action)
    {
        if (!ReadableMapUtils.hasAndNotEmptyString(options, key))
        {
            return null;
        }

        final String title = options.getString(key);

        return new Item(title, action);
    }

    private static @NonNull LinkedList<Item> getCustomButtons(@NonNull final ReadableMap options)
    {
        LinkedList<Item> result = new LinkedList<>();
        if (!options.hasKey("customButtons"))
        {
            return result;
        }

        final ReadableArray customButtons = options.getArray("customButtons");
        for (int i = 0; i < customButtons.size(); i++)
        {
            final ReadableMap button = customButtons.getMap(i);
            final String title = button.getString("title");
            final String action = button.getString("name");
            result.add(new Item(title, action));
        }

        return result;
    }
}
