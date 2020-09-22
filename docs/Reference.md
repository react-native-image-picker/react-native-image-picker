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
| noData             | OK    | OK      | If true, disables the base64 `data` field from being generated (greatly improves performance on large image)     |


## The Response Object

| key              | iOS  | Android | Description                                                                                                          |
| ---------------- | -----| --------| -------------------------------------------------------------------------------------------------------------------- |
| didCancel        | OK   | OK      | Informs you if the user cancelled the process                                                                        |
| error            | OK   | OK      | Contains an error message, if there is one                                                                           |
| data             | OK   | OK      | The base64 encoded image data (photos only)                                                                          |
| uri              | OK   | OK      | The uri to the local file asset on the device                                                                        |
| width            | OK   | OK      | Image dimensions (photos only)                                                                                       |
| height           | OK   | OK      | Image dimensions (photos only)                                                                                       |
| fileSize         | OK   | OK      | The file size (photos only)                                                                                          |
| type             | OK   | OK      | The file type (photos only)                                                                                          |
| fileName         | OK   | OK      | The file name                                                                                                        |