package com.imagepicker.testing.media;

import com.facebook.react.bridge.JavaOnlyMap;
import com.facebook.react.bridge.WritableMap;
import com.imagepicker.media.ImageConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

/**
 * Created by rusfearuth on 11.04.17.
 */

@RunWith(RobolectricTestRunner.class)
public class ImageConfigTest
{
    @Test
    public void testOnImmutable()
    {
        ImageConfig original = new ImageConfig(new File("original.txt"), new File("resized.txt"), 0, 0, 0, 0, false);
        ImageConfig updated = original.withOriginalFile(null);

        assertNotNull("Original has got original file", original.original);
        assertNull("Updated hasn't got original file", updated.original);

        updated = original.withResizedFile(null);

        assertNotNull("Original has got resized file", original.resized);
        assertNull("Updated hasn't got resized file", updated.resized);

        updated = original.withMaxWidth(1);

        assertEquals("Original max width", 0, original.maxWidth);
        assertEquals("Updated max width", 1, updated.maxWidth);

        updated = original.withMaxHeight(2);

        assertEquals("Original max height", 0, original.maxHeight);
        assertEquals("Updated max height", 2, updated.maxHeight);

        updated = original.withQuality(29);

        assertEquals("Original quality", 0, original.quality);
        assertEquals("Updated quality", 29, updated.quality);

        updated = original.withRotation(135);

        assertEquals("Original rotation", 0, original.rotation);
        assertEquals("Updated rotation", 135, updated.rotation);

        updated = original.withSaveToCameraRoll(true);

        assertEquals("Original saveToCameraRoll", false, original.saveToCameraRoll);
        assertEquals("Updated saveToCameraRoll", true, updated.saveToCameraRoll);
    }

    @Test
    public void testParsingOptions()
    {
        WritableMap options = defaultOptions();
        ImageConfig config = new ImageConfig(null, null, 0, 0, 0, 0, false);
        config = config.updateFromOptions(options);
        assertEquals("maxWidth", 1000, config.maxWidth);
        assertEquals("maxHeight", 600, config.maxHeight);
        assertEquals("quality", 50, config.quality);
        assertEquals("rotation", 135, config.rotation);
        assertTrue("storageOptions.cameraRoll", config.saveToCameraRoll);
    }

    @Test
    public void testUseOriginal()
    {
        ImageConfig config = new ImageConfig(null, null, 800, 600, 100, 90, false);

        assertEquals("Image wont be resized", true, config.useOriginal(100, 100, 90));
        assertEquals("Image will be resized because of rotation", false, config.useOriginal(100, 100, 80));
        assertEquals("Image will be resized because of initial width", false, config.useOriginal(1000, 100, 80));
        assertEquals("Image will be resized because of initial height", false, config.useOriginal(100, 1000, 80));

        ImageConfig qualityIsLow = config.withQuality(90);
        assertEquals("Image will be resized because of quality is low", false, qualityIsLow.useOriginal(100, 100, 90));
    }

    @Test
    public void testGetActualFile()
    {
        ImageConfig originalConfig = new ImageConfig(new File("original.txt"), null, 0, 0, 0, 0, false);
        ImageConfig resizedConfig = originalConfig.withResizedFile(new File("resized.txt"));

        assertEquals("For config which has got only original file", "original.txt", originalConfig.getActualFile().getName());
        assertEquals("For config which has got resized file too", "resized.txt", resizedConfig.getActualFile().getName());
    }

    private JavaOnlyMap defaultOptions()
    {
        JavaOnlyMap options = new JavaOnlyMap();
        options.putInt("maxWidth", 1000);
        options.putInt("maxHeight", 600);
        options.putDouble("quality", 0.5);
        options.putInt("rotation", 135);

        JavaOnlyMap storage = new JavaOnlyMap();
        storage.putBoolean("cameraRoll", true);

        options.putMap("storageOptions", storage);

        return options;
    }
}
