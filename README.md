# react-native-image-picker 🎆

A React Native module that allows you to select a photo/video from the device library or camera.

[![npm downloads](https://img.shields.io/npm/dw/react-native-image-picker)](https://img.shields.io/npm/dw/react-native-image-picker)
[![npm package](https://img.shields.io/npm/v/react-native-image-picker?color=red)](https://img.shields.io/npm/v/react-native-image-picker?color=red)
[![License](https://img.shields.io/github/license/react-native-image-picker/react-native-image-picker?color=blue)](https://github.com/react-native-image-picker/react-native-image-picker/blob/main/LICENSE.md)

## Installation

```bash
yarn add react-native-image-picker
```

### New Architecture

To take advantage of the new architecture run-

#### iOS

```bash
RCT_NEW_ARCH_ENABLED=1 npx pod-install ios
```

#### Android

Set `newArchEnabled` to `true` inside `android/gradle.properties`

### Pre-Fabric (AKA not using the new architecture)

```bash
npx pod-install ios
```

## Post-install Steps

### iOS

Add the appropriate keys to your `Info.plist` depending on your requirement:

| Requirement                    | Key                                                 |
| ------------------------------ | --------------------------------------------------- |
| Select image/video from photos | NSPhotoLibraryUsageDescription                      |
| Capture Image                  | NSCameraUsageDescription                            |
| Capture Video                  | NSCameraUsageDescription & NSMicrophoneUsageDescription |

### Android

No permissions required (`saveToPhotos` requires permission [check](#note-on-file-storage)).

Note: This library does not require `Manifest.permission.CAMERA`, if your app declares as using this permission in manifest then you have to obtain the permission before using `launchCamera`.

## API Reference

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

| Option                  | iOS | Android | Web | Description                                                                                                                                                                     |
| ----------------------- | --- | ------- | --- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| mediaType               | OK  | OK      | OK  | `photo` or `video` or `mixed`(`launchCamera` on Android does not support 'mixed'). Web only supports 'photo' for now.                                                           |
| maxWidth                | OK  | OK      | NO  | To resize the image.                                                                                                                                                            |
| maxHeight               | OK  | OK      | NO  | To resize the image.                                                                                                                                                            |
| videoQuality            | OK  | OK      | NO  | `low`, `medium`, or `high` on iOS, `low` or `high` on Android.                                                                                                                  |
| durationLimit           | OK  | OK      | NO  | Video max duration (in seconds).                                                                                                                                                |
| quality                 | OK  | OK      | NO  | 0 to 1, photos.                                                                                                                                                                 |
| cameraType              | OK  | OK      | NO  | 'back' or 'front' (May not be supported in few android devices).                                                                                                                |
| includeBase64           | OK  | OK      | OK  | If `true`, creates base64 string of the image (Avoid using on large image files due to performance).                                                                            |
| includeExtra            | OK  | OK      | NO  | If `true`, will include extra data which requires library permissions to be requested (i.e. exif data).                                                                         |
| saveToPhotos            | OK  | OK      | NO  | (Boolean) Only for `launchCamera`, saves the image/video file captured to public photo.                                                                                         |
| selectionLimit          | OK  | OK      | OK  | Supports providing any integer value. Use `0` to allow any number of files on iOS version >= 14 & Android version >= 13. Default is `1`.                                        |
| presentationStyle       | OK  | NO      | NO  | Controls how the picker is presented. `currentContext`, `pageSheet`, `fullScreen`, `formSheet`, `popover`, `overFullScreen`, `overCurrentContext`. Default is `currentContext`. |
| formatAsMp4             | OK  | NO      | NO  | Converts the selected video to MP4 (iOS Only).                                                                                                                                  |
| assetRepresentationMode | OK  | NO      | NO  | A mode that determines which representation to use if an asset contains more than one. Possible values: 'auto', 'current', 'compatible'. Default is 'auto'.                     |


## The Response Object

| key          | iOS | Android | Web | Description                                                         |
| ------------ | --- | ------- | --- | ------------------------------------------------------------------- |
| didCancel    | OK  | OK      | OK  | `true` if the user cancelled the process                            |
| errorCode    | OK  | OK      | OK  | Check [ErrorCode](#ErrorCode) for all error codes                   |
| errorMessage | OK  | OK      | OK  | Description of the error, use it for debug purpose only             |
| assets       | OK  | OK      | OK  | Array of the selected media, [refer to Asset Object](#Asset-Object) |

## Asset Object

| key       | iOS | Android | Web | Photo/Video | Requires Permissions | Description                                                                                                                                                                                                                                                                    |
| --------- | --- | ------- | --- | ----------- | -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| base64    | OK  | OK      | OK  | PHOTO ONLY  | NO                   | The base64 string of the image (photos only)                                                                                                                                                                                                                                   |
| uri       | OK  | OK      | OK  | BOTH        | NO                   | The file uri in app specific cache storage. Except when picking **video from Android gallery** where you will get read only content uri, to get file uri in this case copy the file to app specific storage using any react-native library. For web it uses the base64 as uri. |
| originalPath       | NO  | OK      | NO  | BOTH        | NO                   | The original file path. |
| width     | OK  | OK      | OK  | BOTH        | NO                   | Asset dimensions                                                                                                                                                                                                                                                               |
| height    | OK  | OK      | OK  | BOTH        | NO                   | Asset dimensions                                                                                                                                                                                                                                                               |
| fileSize  | OK  | OK      | NO  | BOTH        | NO                   | The file size                                                                                                                                                                                                                                                                  |
| type      | OK  | OK      | NO  | BOTH        | NO                   | The file type                                                                                                                                                                                                                                                                  |
| fileName  | OK  | OK      | NO  | BOTH        | NO                   | The file name                                                                                                                                                                                                                                                                  |
| duration  | OK  | OK      | NO  | VIDEO ONLY  | NO                   | The selected video duration in seconds                                                                                                                                                                                                                                         |
| bitrate   | --- | OK      | NO  | VIDEO ONLY  | NO                   | The average bitrate (in bits/sec) of the selected video, if available. (Android only)                                                                                                                                                                                          |
| timestamp | OK  | OK      | NO  | BOTH        | YES                  | Timestamp of the asset. Only included if 'includeExtra' is true                                                                                                                                                                                                                |
| id        | OK  | OK      | NO  | BOTH        | YES                  | local identifier of the photo or video. On Android, this is the same as fileName                                                                                                                                                                                               |

## Note on file storage

Image/video captured via camera will be stored in temporary folder allowing it to be deleted any time, so don't expect it to persist. Use `saveToPhotos: true` (default is `false`) to save the file in the public photos. `saveToPhotos` requires `WRITE_EXTERNAL_STORAGE` permission on Android 28 and below (The permission has to obtained by the App manually as the library does not handle that).

For web, this doesn't work.

## ErrorCode

| Code               | Description                                       |
| ------------------ | ------------------------------------------------- |
| camera_unavailable | Camera not available on device                    |
| permission         | Permission not satisfied                          |
| others             | Other errors (check errorMessage for description) |

## License

[MIT](LICENSE.md)
