package com.imagepicker.testing.utils;

import android.app.Application;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.os.Environment;
import android.support.annotation.NonNull;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.JavaOnlyArray;
import com.facebook.react.bridge.JavaOnlyMap;
import com.imagepicker.ImagePickerModule;
import com.imagepicker.ResponseHelper;
import com.imagepicker.media.ImageConfig;
import com.imagepicker.testing.mock.MockExifInterface;
import com.imagepicker.utils.MediaUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowBitmap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

/**
 * Created by rusfearuth on 12.04.17.
 */

@RunWith(RobolectricTestRunner.class)
@SuppressStaticInitializationFor("com.facebook.react.common.build.ReactBuildConfig")
@PrepareForTest({ Arguments.class, MediaUtils.class })
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*"})
@Config(manifest = Config.NONE)
public class MediaUtilsTest
{
    @Rule
    public PowerMockRule rule = new PowerMockRule();
    private ExifInterface exifMock;

    @Before
    public void setUp() throws Exception
    {
        PowerMockito.mockStatic(Arguments.class);
        when(Arguments.createArray()).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return new JavaOnlyArray();
            }
        });
        when(Arguments.createMap()).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return new JavaOnlyMap();
            }
        });
        exifMock = new MockExifInterface("");
        whenNew(ExifInterface.class).withAnyArguments().thenReturn(exifMock);
    }

    @Test
    public void testCreatingFile() throws IOException
    {
        Application application = RuntimeEnvironment.application;
        File newFile = MediaUtils.createNewFile(application);
        assertNotNull("File was created", newFile);
        newFile.createNewFile();
        assertTrue("File exists", newFile.exists());
    }

    @Test
    public void testGetResizedFile()
    {
        Application application = RuntimeEnvironment.application;
        Bitmap original = ShadowBitmap.createBitmap(100, 100, Bitmap.Config.ALPHA_8);
        File file = MediaUtils.createNewFile(application);

        assertTrue("Image was created", saveToFile(file, original));

        ImageConfig config = new ImageConfig(file, null, 50, 50, 100, 0, false);
        ImageConfig resizedConfig = MediaUtils.getResizedImage(application, config, 10, 10);

        assertNotNull("Image was resized", resizedConfig.resized);
        assertNotSame(
                "Original and resized files aren't the same",
                config.original.getAbsolutePath(),
                resizedConfig.resized.getAbsolutePath()
        );
        assertNotSame(
                "Original and resized files have different size",
                config.original.length(),
                resizedConfig.resized.length()
        );
    }

    @Test
    public void testRemoveUselessFiles()
    {
        Application application = RuntimeEnvironment.application;
        Bitmap original = ShadowBitmap.createBitmap(100, 100, Bitmap.Config.ALPHA_8);
        File originalFile = MediaUtils.createNewFile(application);

        assertTrue("Original file was created", saveToFile(originalFile, original));

        Bitmap resized = ShadowBitmap.createBitmap(10, 10, Bitmap.Config.ALPHA_8);
        File resizedFile = MediaUtils.createNewFile(application);

        assertTrue("Resized file was created", saveToFile(resizedFile, resized));

        assertTrue("Original file exists", originalFile.exists());
        assertTrue("Resized file exists", resizedFile.exists());

        ImageConfig config = new ImageConfig(originalFile, resizedFile, 100, 100, 100, 0, false);

        MediaUtils.removeUselessFiles(ImagePickerModule.REQUEST_LAUNCH_IMAGE_LIBRARY, config);

        assertTrue("Original file exists, because requestCode is invalid", originalFile.exists());
        assertTrue("Resized file exists, because requestCode is invalid", resizedFile.exists());

        MediaUtils.removeUselessFiles(ImagePickerModule.REQUEST_LAUNCH_IMAGE_CAPTURE, config);

        assertFalse("Original file was removed", config.original.exists());
        assertFalse("Resized file was removed", config.resized.exists());
    }

    @Test
    public void testReadExifInterface() throws Exception
    {
        final SimpleDateFormat exifDatetimeFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
        final DateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        final long currentTimestamp = System.currentTimeMillis();
        final String fileDateTime = exifDatetimeFormat.format(new Date(currentTimestamp));
        final String dateTimeForCheckout = isoFormat.format(new Date(currentTimestamp));

        final Application application = RuntimeEnvironment.application;
        final Bitmap original = ShadowBitmap.createBitmap(100, 100, Bitmap.Config.ALPHA_8);
        final File originalFile = MediaUtils.createNewFile(application);
        final ImageConfig imageConfig = new ImageConfig(originalFile, null, 100, 100, 100, 0, false);

        assertTrue("Original file was created", saveToFile(originalFile, original));

        ExifInterface exif = new ExifInterface(originalFile.getAbsolutePath());
        exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_ROTATE_270));
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, String.valueOf(1.0f));
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, String.valueOf(-1.0f));
        exif.setAttribute(ExifInterface.TAG_DATETIME, fileDateTime);
        exif.saveAttributes();

        ResponseHelper helper = new ResponseHelper();
        helper.cleanResponse();
        MediaUtils.ReadExifResult result = MediaUtils.readExifInterface(helper, imageConfig);

        assertNull("Exif interface was read", result.error);

        assertNotNull("Orientation was read", helper.getResponse().getInt("originalRotation"));
        assertEquals("Orientation value is", helper.getResponse().getInt("originalRotation"), 270);

        assertNotNull("Latitude was read", helper.getResponse().getDouble("latitude"));
        assertEquals("Latitude value is", Math.floor(helper.getResponse().getDouble("latitude")), Math.floor(1.0f));

        assertNotNull("Longitude was read", helper.getResponse().getDouble("longitude"));
        assertEquals("Longitude value is", Math.floor(helper.getResponse().getDouble("longitude")), Math.floor(-1.0f));

        assertNotNull("DateTime was read", helper.getResponse().getString("timestamp"));
        assertEquals("DateTIme value is", helper.getResponse().getString("timestamp"), dateTimeForCheckout);

        assertNotNull("Vertical flag was generated", helper.getResponse().getBoolean("isVertical"));
        assertFalse("Is image vertical", helper.getResponse().getBoolean("isVertical"));
    }

    @Test
    public void testRolloutPhotoFromCamera()
    {
        Application application = RuntimeEnvironment.application;
        Bitmap original = ShadowBitmap.createBitmap(100, 100, Bitmap.Config.ALPHA_8);
        File originalFile = MediaUtils.createNewFile(application);
        assertTrue("Original file was created", saveToFile(originalFile, original));

        ImageConfig imageConfig = new ImageConfig(originalFile, null, 100, 100, 100, 0, true);
        MediaUtils.RolloutPhotoResult result = MediaUtils.rolloutPhotoFromCamera(imageConfig);

        assertNull("Rollout of original file has done", result.error);
        assertNotSame("Original files are different", imageConfig.original.getAbsolutePath(), result.imageConfig.original.getAbsolutePath());
        assertEquals("Original file names are the same", imageConfig.original.getName(), result.imageConfig.original.getName());
        assertTrue("Original file was moved", result.imageConfig.original.getAbsolutePath().toLowerCase().contains("/dcim/"));
        assertFalse("Original file was moved", imageConfig.original.exists());

        Bitmap resized = ShadowBitmap.createBitmap(10, 10, Bitmap.Config.ALPHA_8);
        File resizedFile = MediaUtils.createNewFile(application);
        assertTrue("Resized file was created", saveToFile(resizedFile, resized));

        imageConfig = imageConfig.withResizedFile(resizedFile);
        result = MediaUtils.rolloutPhotoFromCamera(imageConfig);
        assertNull("Rollout of resized file has done", result.error);

        assertNotSame("Different resized files", imageConfig.resized.getAbsolutePath(), result.imageConfig.resized.getAbsolutePath());
        assertEquals("Resized file names are the same", imageConfig.resized.getName(), result.imageConfig.resized.getName());
        assertTrue("Resized file was moved", result.imageConfig.resized.getAbsolutePath().toLowerCase().contains("/dcim/"));
        assertFalse("Resized file was moved", imageConfig.original.exists());
    }

    @Test
    public void testMoveFile() throws IOException
    {
        Application application = RuntimeEnvironment.application;
        File oldFile = new File(application.getCacheDir(), "original.txt");
        oldFile.createNewFile();
        File targetDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        File newFile = new File(targetDir, oldFile.getName());
        MediaUtils.moveFile(oldFile, newFile);

        assertFalse("Old file has left", oldFile.exists());
        assertTrue("New file was copied", newFile.exists());
    }

    @Test
    public void testRemoveOriginIfNeeded() throws IOException
    {
        Application application = RuntimeEnvironment.application;
        Bitmap original = ShadowBitmap.createBitmap(100, 100, Bitmap.Config.ALPHA_8);
        File originalFile = MediaUtils.createNewFile(application);

        assertTrue("Original file was created", saveToFile(originalFile, original));
        assertTrue("Original file exists", originalFile.exists());

        ImageConfig imageConfig = new ImageConfig(originalFile, null, 100, 100, 100, 0, false);

        MediaUtils.removeOriginIfNeeded(imageConfig, ImagePickerModule.REQUEST_LAUNCH_IMAGE_LIBRARY);
        assertTrue("Original file wasn't removed on REQUEST_LAUNCH_IMAGE_LIBRARY", originalFile.exists());

        MediaUtils.removeOriginIfNeeded(imageConfig, ImagePickerModule.REQUEST_LAUNCH_IMAGE_CAPTURE);
        assertFalse("Original file was removed on REQUEST_LAUNCH_IMAGE_CAPTURE", originalFile.exists());
    }

    private boolean saveToFile(@NonNull final File imageFile,
                               @NonNull final Bitmap bitmap)
    {
        boolean result = false;

        FileOutputStream fos = null;
        try
        {
            fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            result = true;
        }
        catch (Exception e)
        {

        }
        finally
        {
            if (fos != null)
            {
                try
                {
                    fos.close();
                }
                catch (IOException e)
                {
                    result = false;
                    e.printStackTrace();
                }
            }
        }

        return result;
    }
}
