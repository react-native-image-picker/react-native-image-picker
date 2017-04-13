package com.imagepicker.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.react.bridge.ReadableMap;
import com.imagepicker.ImagePickerModule;
import com.imagepicker.ResponseHelper;
import com.imagepicker.media.ImageConfig;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.UUID;

import static com.imagepicker.ImagePickerModule.REQUEST_LAUNCH_IMAGE_CAPTURE;

/**
 * Created by rusfearuth on 15.03.17.
 */

public class MediaUtils
{
    public static @NonNull File createNewFile(@NonNull final Context reactContext)
    {
        final String filename = new StringBuilder("image-")
                .append(UUID.randomUUID().toString())
                .append(".jpg")
                .toString();
        final File path = reactContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File result = new File(path, filename);
        path.mkdirs();
        return result;
    }

    /**
     * Create a resized image to fulfill the maxWidth/maxHeight, quality and rotation values
     *
     * @param context
     * @param imageConfig
     * @param initialWidth
     * @param initialHeight
     * @return updated ImageConfig
     */
    public static @NonNull ImageConfig getResizedImage(@NonNull final Context context,
                                                       @NonNull final ImageConfig imageConfig,
                                                       final int initialWidth,
                                                       final int initialHeight)
    {
        BitmapFactory.Options imageOptions = new BitmapFactory.Options();
        imageOptions.inScaled = false;
        // FIXME: OOM here
        Bitmap photo = BitmapFactory.decodeFile(imageConfig.original.getAbsolutePath(), imageOptions);

        if (photo == null)
        {
            return null;
        }

        ImageConfig result = imageConfig;

        Bitmap scaledPhoto = null;
        if (imageConfig.maxWidth == 0 || imageConfig.maxWidth > initialWidth)
        {
            result = result.withMaxWidth(initialWidth);
        }
        if (imageConfig.maxHeight == 0 || imageConfig.maxWidth > initialHeight)
        {
            result = result.withMaxHeight(initialHeight);
        }

        double widthRatio = (double) result.maxWidth / initialWidth;
        double heightRatio = (double) result.maxHeight / initialHeight;

        double ratio = (widthRatio < heightRatio)
                ? widthRatio
                : heightRatio;

        Matrix matrix = new Matrix();
        matrix.postRotate(result.rotation);
        matrix.postScale((float) ratio, (float) ratio);

        ExifInterface exif;
        try
        {
            exif = new ExifInterface(result.original.getAbsolutePath());

            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);

            switch (orientation)
            {
                case 6:
                    matrix.postRotate(90);
                    break;
                case 3:
                    matrix.postRotate(180);
                    break;
                case 8:
                    matrix.postRotate(270);
                    break;
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        scaledPhoto = Bitmap.createBitmap(photo, 0, 0, photo.getWidth(), photo.getHeight(), matrix, true);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        scaledPhoto.compress(Bitmap.CompressFormat.JPEG, result.quality, bytes);

        final File resized = createNewFile(context);

        if (resized == null)
        {
            if (photo != null)
            {
                photo.recycle();
                photo = null;
            }
            if (scaledPhoto != null)
            {
                scaledPhoto.recycle();
                scaledPhoto = null;
            }
            return imageConfig;
        }

        result = result.withResizedFile(resized);

        FileOutputStream fos;
        try
        {
            fos = new FileOutputStream(result.resized);
            fos.write(bytes.toByteArray());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        if (photo != null)
        {
            photo.recycle();
            photo = null;
        }
        if (scaledPhoto != null)
        {
            scaledPhoto.recycle();
            scaledPhoto = null;
        }
        return result;
    }

    public static void removeUselessFiles(final int requestCode,
                                          @NonNull final ImageConfig imageConfig)
    {
        if (requestCode != ImagePickerModule.REQUEST_LAUNCH_IMAGE_CAPTURE)
        {
            return;
        }

        if (imageConfig.original != null && imageConfig.original.exists())
        {
            imageConfig.original.delete();
        }

        if (imageConfig.resized != null && imageConfig.resized.exists())
        {
            imageConfig.resized.delete();
        }
    }

    public static void fileScan(@Nullable final Context reactContext,
                                @NonNull final String path)
    {
        if (reactContext == null)
        {
            return;
        }
        MediaScannerConnection.scanFile(reactContext,
                new String[] { path }, null,
                new MediaScannerConnection.OnScanCompletedListener()
                {
                    public void onScanCompleted(String path, Uri uri)
                    {
                        Log.i("TAG", new StringBuilder("Finished scanning ").append(path).toString());
                    }
                });
    }

    public static ReadExifResult readExifInterface(@NonNull ResponseHelper responseHelper,
                                                   @NonNull final ImageConfig imageConfig)
    {
        ReadExifResult result;
        int currentRotation = 0;

        try
        {
            ExifInterface exif = new ExifInterface(imageConfig.original.getAbsolutePath());

            // extract lat, long, and timestamp and add to the response
            float[] latLng = new float[2];
            if(exif.getLatLong(latLng))
            {
                responseHelper.putDouble("latitude", latLng[0]);
                responseHelper.putDouble("longitude", latLng[1]);
            }

            final String timestamp = exif.getAttribute(ExifInterface.TAG_DATETIME);
            final SimpleDateFormat exifDatetimeFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");

            final DateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            try
            {
                final String isoFormatString = new StringBuilder(isoFormat.format(exifDatetimeFormat.parse(timestamp)))
                        .append("Z").toString();
                responseHelper.putString("timestamp", isoFormatString);
            }
            catch (Exception e) {}

            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            boolean isVertical = true;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    isVertical = false;
                    currentRotation = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    isVertical = false;
                    currentRotation = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    currentRotation = 180;
                    break;
            }
            responseHelper.putInt("originalRotation", currentRotation);
            responseHelper.putBoolean("isVertical", isVertical);
            result = new ReadExifResult(currentRotation, null);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            result = new ReadExifResult(currentRotation, e);
        }

        return result;
    }

    public static @Nullable RolloutPhotoResult rolloutPhotoFromCamera(@NonNull final ImageConfig imageConfig)
    {
        RolloutPhotoResult result = null;
        final File oldFile = imageConfig.resized == null ? imageConfig.original: imageConfig.resized;
        final File newDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        final File newFile = new File(newDir.getPath(), oldFile.getName());

        try
        {
            moveFile(oldFile, newFile);
            ImageConfig newImageConfig;
            if (imageConfig.resized != null)
            {
                newImageConfig = imageConfig.withResizedFile(newFile);
            }
            else
            {
                newImageConfig = imageConfig.withOriginalFile(newFile);
            }
            result = new RolloutPhotoResult(newImageConfig, null);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            result = new RolloutPhotoResult(imageConfig, e);
        }
        return result;
    }

    /**
     * Move a file from one location to another.
     *
     * This is done via copy + deletion, because Android will throw an error
     * if you try to move a file across mount points, e.g. to the SD card.
     */
    public static void moveFile(@NonNull final File oldFile,
                                 @NonNull final File newFile) throws IOException
    {
        FileInputStream in = new FileInputStream(oldFile);
        FileOutputStream out = new FileOutputStream(newFile);

        try
        {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            oldFile.delete();
        }
        finally
        {
            try
            {
                if (in != null)
                {
                    in.close();
                }
                if (out != null)
                {
                    out.close();
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public static void removeOriginIfNeeded(@NonNull final ImageConfig imageConfig,
                                            final int requestCode)
    {
        if (requestCode != ImagePickerModule.REQUEST_LAUNCH_IMAGE_CAPTURE)
        {
            return;
        }
        imageConfig.original.delete();
    }


    public static class RolloutPhotoResult
    {
        public final ImageConfig imageConfig;
        public final Throwable error;

        public RolloutPhotoResult(@NonNull final ImageConfig imageConfig,
                                  @Nullable final Throwable error)
        {
            this.imageConfig = imageConfig;
            this.error = error;
        }
    }


    public static class ReadExifResult
    {
        public final int currentRotation;
        public final Throwable error;

        public ReadExifResult(int currentRotation,
                              @Nullable final Throwable error)
        {
            this.currentRotation = currentRotation;
            this.error = error;
        }
    }
}
