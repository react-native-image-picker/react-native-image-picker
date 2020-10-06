# React Native Image Picker

A React Native module that allows you to select a photo/video from the device library or camera.

# Install

```
yarn add react-native-image-picker

# RN >= 0.60
cd ios && pod install

# RN < 0.60
react-native link react-native-image-picker
```

## Post-install Steps

### iOS

Add the appropriate keys to your Info.plist,

If you are allowing user to select image/video from photos, add `NSPhotoLibraryUsageDescription`.

If you are allowing user to capture image add `NSCameraUsageDescription` key also.

If you are allowing user to capture video add `NSCameraUsageDescription` add `NSMicrophoneUsageDescription` key also.

### Android

Add permissions in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```


# API Reference

## Methods

### `launchCamera()`

Launch camera to take photo or video.

```js
static launchCamera(options?, callback)
```

See [Options](#options) for further information on `options`.

The `callback` will be called with a response object, refer to [The Response Object](#the-response-object).

### `launchImageLibrary()`

Launch gallery to pick image or video.

```js
static launchImageLibrary(options?, callback)
```

See [Options](#options) for further information on `options`.

The `callback` will be called with a response object, refer to [The Response Object](#the-response-object).

### Permission Handling on Android

On android, this library does not ask for users permission. You have to make sure WRITE_EXTERNAL_STORAGE permission is obtained before calling the above methods.

## Options

| Option             | iOS   | Android | Description                                                                                                      |
| ------------------ | ------| ------- | ---------------------------------------------------------------------------------------------------------------- |
| mediaType          | OK    | OK      | 'photo' or 'video'                                                                                               |
| maxWidth           | OK    | OK      | To resize the image                                                                                              |
| maxHeight          | OK    | OK      | To resize the image                                                                                              |
| videoQuality       | OK    | OK      | 'low', 'medium', or 'high' on iOS, 'low' or 'high' on Android                                                    |
| quality            | OK    | OK      | 0 to 1, photos                                                                                                   |
| includeBase64      | OK    | OK      | If true, creates base64 string of the image (Avoid using on large image files due to performance)                |
| saveToPhotos       | OK    | OK      | (Boolean) Only for launchCamera, saves the image/video file captured to public photo                             |


## The Response Object

| key              | iOS  | Android | Description                                                                                                          |
| ---------------- | -----| --------| -------------------------------------------------------------------------------------------------------------------- |
| didCancel        | OK   | OK      | `true` if the user cancelled the process                                                                             |
| errorCode        | OK   | OK      | Check [ErrorCode](#ErrorCode) for all error codes                                                                    |
| errorMessage     | OK   | OK      | Description of the error, use it for debug purpose only                                                              |
| base64           | OK   | OK      | The base64 string of the image (photos only)                                                                         |
| uri              | OK   | OK      | The uri to the local file on the device (uri might change for same file for different session so don't save it)      |
| width            | OK   | OK      | Image dimensions (photos only)                                                                                       |
| height           | OK   | OK      | Image dimensions (photos only)                                                                                       |
| fileSize         | OK   | OK      | The file size (photos only)                                                                                          |
| type             | OK   | OK      | The file type (photos only)                                                                                          |
| fileName         | OK   | OK      | The file name                                                                                                        |

## Note on file storage
Image/video captured via camera will be stored in temporary folder so will be deleted any time, so don't expect it to persist. Use `saveToPhotos: true` (default is false) to save the file in the public photos.


## ErrorCode

| Code               | Description                                                                                                      |
| ------------------ | ---------------------------------------------------------------------------------------------------------------- |
| camera_unavailable | camera not available on device                                                                                   |
| permission         | Permission not satisfied                                                                                         |
| others             | other errors (check errorMessage for description)                                                                |

## License

[MIT](LICENSE.md)
