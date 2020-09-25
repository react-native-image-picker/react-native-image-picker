package com.imagepicker.utils;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class RealPathUtil {

	public static @Nullable Uri compatUriFromFile(@NonNull final Context context,
												  @NonNull final File file) {
		Uri result = null;
		if (Build.VERSION.SDK_INT < 21) {
			result = Uri.fromFile(file);
		}
		else {
			final String packageName = context.getApplicationContext().getPackageName();
			final String authority =  new StringBuilder(packageName).append(".provider").toString();
			try {
				result = FileProvider.getUriForFile(context, authority, file);
			}
			catch(IllegalArgumentException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	@SuppressLint("NewApi")
	public static @Nullable String getRealPathFromURI(@NonNull final Context context,
													  @NonNull final Uri uri) {

		final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

		final String Tag = "PadletRealPathUtil";

		// DocumentProvider
		if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
			// ExternalStorageProvider
			if (isExternalStorageDocument(uri)) {
				final String docId = DocumentsContract.getDocumentId(uri);
				final String[] split = docId.split(":");
				final String type = split[0];

				if ("primary".equalsIgnoreCase(type)) {
					return Environment.getExternalStorageDirectory() + "/" + split[1];
				}

				// TODO handle non-primary volumes
			}
			// DownloadsProvider
			else if (isDownloadsDocument(uri)) {

				final String id = DocumentsContract.getDocumentId(uri);

				if (id != null) {
					// Handle raw file urls differently, we can just return the current string minus
					// the raw: prefix. https://github.com/Yalantis/uCrop/issues/318
					if (id.startsWith("raw:")) {
						return id.substring(4);
					}

					try {
						final Uri contentUri = ContentUris.withAppendedId(
								Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));
						return getDataColumn(context, contentUri, null, null);
					} catch (NumberFormatException e) {
						Log.w(Tag, "Non-numeric document id: " + id);
						// This will fallback to creating a temporary file.
					} catch(Exception e) {
						e.printStackTrace();
					}
				}

				// File couldn't be loaded so we have to create a temporary file and then stream
				// the contents into that file, some of this code has been implemented from
				// https://github.com/coltoscosmin/FileUtils/blob/master/FileUtils.java 
				
				String fileName = getFileName(context, uri);
				File cacheDir = new File(context.getCacheDir(), "documents");
				if (!cacheDir.exists()) cacheDir.mkdirs();
				File file = new File(cacheDir, fileName);
				try {
					file.createNewFile();
				}
				catch(Exception e) {
					e.printStackTrace();
				}
				String destinationPath = file.getAbsolutePath();
				Log.d(Tag, "Destination path: " + destinationPath);
				if (file != null) {
					destinationPath = file.getAbsolutePath();
					saveFileFromUri(context, uri, destinationPath);
				}

				return destinationPath;
			}
			// MediaProvider
			else if (isMediaDocument(uri)) {
				final String docId = DocumentsContract.getDocumentId(uri);
				final String[] split = docId.split(":");
				final String type = split[0];

				Uri contentUri = null;
				if ("image".equals(type)) {
					contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
				} else if ("video".equals(type)) {
					contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
				} else if ("audio".equals(type)) {
					contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
				}

				final String selection = "_id=?";
				final String[] selectionArgs = new String[] {
						split[1]
				};

				return getDataColumn(context, contentUri, selection, selectionArgs);
			}
		}
		// MediaStore (and general)
		else if ("content".equalsIgnoreCase(uri.getScheme())) {

			// Return the remote address
			if (isGooglePhotosUri(uri))
				return uri.getLastPathSegment();

			if (isFileProviderUri(context, uri))
				return getFileProviderPath(context, uri);

			return getDataColumn(context, uri, null, null);
		}
		// File
		else if ("file".equalsIgnoreCase(uri.getScheme())) {
			return uri.getPath();
		}

		return null;
	}

	/**
	 * Get the value of the data column for this Uri. This is useful for
	 * MediaStore Uris, and other file-based ContentProviders.
	 *
	 * @param context The context.
	 * @param uri The Uri to query.
	 * @param selection (Optional) Filter used in the query.
	 * @param selectionArgs (Optional) Selection arguments used in the query.
	 * @return The value of the _data column, which is typically a file path.
	 */
	public static String getDataColumn(Context context, Uri uri, String selection,
	                                   String[] selectionArgs) {

		Cursor cursor = null;
		final String column = "_data";
		final String[] projection = {
				column
		};

		try {
			cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
					null);
			if (cursor != null && cursor.moveToFirst()) {
				final int index = cursor.getColumnIndex(column);
				if (index >= 0) {
					return cursor.getString(index);
				}
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return null;
	}


	/**
	 * @param uri The Uri to check.
	 * @return Whether the Uri authority is ExternalStorageProvider.
	 */
	public static boolean isExternalStorageDocument(Uri uri) {
		return "com.android.externalstorage.documents".equals(uri.getAuthority());
	}

	/**
	 * @param uri The Uri to check.
	 * @return Whether the Uri authority is DownloadsProvider.
	 */
	public static boolean isDownloadsDocument(Uri uri) {
		return "com.android.providers.downloads.documents".equals(uri.getAuthority());
	}

	/**
	 * @param uri The Uri to check.
	 * @return Whether the Uri authority is MediaProvider.
	 */
	public static boolean isMediaDocument(Uri uri) {
		return "com.android.providers.media.documents".equals(uri.getAuthority());
	}

	/**
	 * @param uri The Uri to check.
	 * @return Whether the Uri authority is Google Photos.
	 */
	public static boolean isGooglePhotosUri(@NonNull final Uri uri) {
		return "com.google.android.apps.photos.content".equals(uri.getAuthority());
	}

	/**
	 * @param context The Application context
	 * @param uri The Uri is checked by functions
	 * @return Whether the Uri authority is FileProvider
	 */
	public static boolean isFileProviderUri(@NonNull final Context context,
	                                        @NonNull final Uri uri) {
		final String packageName = context.getPackageName();
		final String authority = new StringBuilder(packageName).append(".provider").toString();
		return authority.equals(uri.getAuthority());
	}

	/**
	 * @param context The Application context
	 * @param uri The Uri is checked by functions
	 * @return File path or null if file is missing
	 */
	public static @Nullable String getFileProviderPath(@NonNull final Context context,
	                                                   @NonNull final Uri uri)
	{
		final File appDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
		final File file = new File(appDir, uri.getLastPathSegment());
		return file.exists() ? file.toString(): null;
	}

	private static void saveFileFromUri(Context context, Uri uri, String destinationPath) {
		InputStream is = null;
		BufferedOutputStream bos = null;
		try {
			is = context.getContentResolver().openInputStream(uri);
			bos = new BufferedOutputStream(new FileOutputStream(destinationPath, false));
			byte[] buf = new byte[1024];
			is.read(buf);
			do {
				bos.write(buf);
			} while (is.read(buf) != -1);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (is != null) is.close();
				if (bos != null) bos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static String getFileName(@NonNull Context context, Uri uri) {
		String mimeType = context.getContentResolver().getType(uri);
		String filename = null;

		if (mimeType == null && context != null) {
			String path = getPath(context, uri);
			if (path == null) {
				filename = getName(uri.toString());
			} else {
				File file = new File(path);
				filename = file.getName();
			}
		} else {
			Cursor returnCursor = context.getContentResolver().query(uri, null,
					null, null, null);
			if (returnCursor != null) {
				int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
				returnCursor.moveToFirst();
				filename = returnCursor.getString(nameIndex);
				returnCursor.close();
			}
		}

		return filename;
	}

	public static String getName(String filename) {
		if (filename == null) {
			return null;
		}
		int index = filename.lastIndexOf('/');
		return filename.substring(index + 1);
	}

	public static String getPath(final Context context, final Uri uri) {
		String absolutePath = getRealPathFromURI(context, uri);
		// String absolutePath = getLocalPath(context, uri);
		return absolutePath != null ? absolutePath : uri.toString();
	}
}
