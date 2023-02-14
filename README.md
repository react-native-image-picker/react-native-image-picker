# React Native Image Picker

A React Native module that allows you to select a photo/video from the device library or camera.

<p align="center">
  <img src="https://img.shields.io/npm/dw/react-native-image-picker" />
  <img src="https://img.shields.io/npm/v/react-native-image-picker" />
</p>

### Make sure you're reading the doc applicable to your version, for example if you're using version 3.8.0 go to tag 3.8.0 and read those docs. This doc is always that of main branch.

### Also read version release notes for any breaking changes especially if you're updating the major version.

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

No permissions required (`saveToPhotos` requires permission [check](#note-on-file-storage)).

Note: This library does not require Manifest.permission.CAMERA, if your app declares as using this permission in manifest then you have to obtain the permission before using `launchCamera`.

# API Reference

## Methods

```js
import {launchCamera, launchImageLibrary} from 'react-native-image-picker';
```

### `launchCamera()`

Launch camera to take photo or video.

```js
launchCamera(options?, callback);

// You can also use as a promise without 'callback':
const result = await launchCamera(options?);
```

See [Options](#options) for further information on `options`.

The `callback` will be called with a response object, refer to [The Response Object](#the-response-object).

### `launchImageLibrary`

Launch gallery to pick image or video.

```js
launchImageLibrary(options?, callback)

// You can also use as a promise without 'callback':
const result = await launchImageLibrary(options?);
```

See [Options](#options) for further information on `options`.

The `callback` will be called with a response object, refer to [The Response Object](#the-response-object).

## Options

| Option         | iOS | Android | Web | Description                                                                                                                         |
| -------------- | --- | ------- | --- |------------------------------------------------------------------------------------------------------------------------------------ |
| mediaType      | OK  | OK      | OK  | 'photo' or 'video' or 'mixed'(mixed supported only for launchImageLibrary, to pick an photo or video). Web only suppots 'photo' for now.                             |
| maxWidth       | OK  | OK      | NO | To resize the image                                                                                                                       |
| maxHeight      | OK  | OK      | NO | To resize the image                                                                                                                       |
| videoQuality   | OK  | OK      | NO | 'low', 'medium', or 'high' on iOS, 'low' or 'high' on Android                                                                             |
| durationLimit  | OK  | OK      | NO | Video max duration in seconds                                                                                                             |
| quality        | OK  | OK      | NO | 0 to 1, photos                                                                                                                            |
| cameraType     | OK  | OK      | NO | 'back' or 'front'. May not be supported in few android devices                                                                            |
| includeBase64  | OK  | OK      | OK | If true, creates base64 string of the image (Avoid using on large image files due to performance)                                         |                                                   |
| includeExtra   | OK  | OK      | NO | If true, will include extra data which requires library permissions to be requested (i.e. exif data)                                      |
| saveToPhotos   | OK  | OK      | NO |(Boolean) Only for launchCamera, saves the image/video file captured to public photo                                                      |
| selectionLimit | OK  | OK      | OK |Default is `1`, use `0` to allow any number of files. Only iOS version >= 14 & Android version >= 13 support `0` and also it supports providing any integer value |
| presentationStyle | OK  | NO      | NO |Controls how the picker is presented. 'pageSheet', 'fullScreen', 'pageSheet', 'formSheet', 'popover', 'overFullScreen', 'overCurrentContext'. Default is 'currentContext' |

## The Response Object

| key          | iOS | Android | Web | Description                                                         |
| ------------ | --- | ------- | --- | ------------------------------------------------------------------- |
| didCancel    | OK  | OK      | OK  | `true` if the user cancelled the process                            |
| errorCode    | OK  | OK      | OK  | Check [ErrorCode](#ErrorCode) for all error codes                   |
| errorMessage | OK  | OK      | OK  | Description of the error, use it for debug purpose only             |
| assets       | OK  | OK      | OK  | Array of the selected media, [refer to Asset Object](#Asset-Object) |

## Asset Object

| key       | iOS | Android | Web | Photo/Video | Requires Permissions | Description               |
| --------- | --- | ------- | --- | ----------- | -------------------- | ------------------------- |
| base64    | OK  | OK      | OK  | PHOTO ONLY  | NO                   | The base64 string of the image (photos only) |
| uri       | OK  | OK      | OK  | BOTH        | NO                   | The file uri in app specific cache storage. Except when picking **video from Android gallery** where you will get read only content uri, to get file uri in this case copy the file to app specific storage using any react-native library. For web it uses the base64 as uri. |
| width     | OK  | OK      | OK  | BOTH        | NO                   | Asset dimensions                |
| height    | OK  | OK      | OK  | BOTH        | NO                   | Asset dimensions                |
| fileSize  | OK  | OK      | NO  | BOTH        | NO                   | The file size                                 |
| type      | OK  | OK      | NO  | BOTH        | NO                   | The file type                                 |
| fileName  | OK  | OK      | NO  | BOTH        | NO                   | The file name                                 |
| duration  | OK  | OK      | NO  | VIDEO ONLY  | NO                   | The selected video duration in seconds        |
| bitrate   | --- | OK      | NO  | VIDEO ONLY  | NO                   | The average bitrate (in bits/sec) of the selected video, if available. (Android only) |
| timestamp | OK  | OK      | NO  | BOTH        | YES                  | Timestamp of the asset. Only included if 'includeExtra' is true |
| id        | OK  | OK      | NO  | BOTH        | YES                  | local identifier of the photo or video. On Android, this is the same as fileName |

## Note on file storage

Image/video captured via camera will be stored in temporary folder so will be deleted any time, so don't expect it to persist. Use `saveToPhotos: true` (default is false) to save the file in the public photos. `saveToPhotos` requires WRITE_EXTERNAL_STORAGE permission on Android 28 and below (You have to obtain the permission, the library does not).

For web this doesn't work.

## ErrorCode

| Code               | Description                                       |
| ------------------ | ------------------------------------------------- |
| camera_unavailable | camera not available on device                    |
| permission         | Permission not satisfied                          |
| others             | other errors (check errorMessage for description) |

## License

[MIT](LICENSE.md)
