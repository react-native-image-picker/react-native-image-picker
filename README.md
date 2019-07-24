# React Native Image Picker

[![npm version](https://badge.fury.io/js/react-native-image-picker.svg)](https://badge.fury.io/js/react-native-image-picker)
[![npm](https://img.shields.io/npm/dt/react-native-image-picker.svg)](https://npmcharts.com/compare/react-native-image-picker?minimal=true)
![MIT](https://img.shields.io/dub/l/vibe-d.svg)
![Platform - Android and iOS](https://img.shields.io/badge/platform-Android%20%7C%20iOS-yellow.svg)
[![Gitter chat](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/react-native-image-picker/Lobby)

A React Native module that allows you to use native UI to select a photo/video from the device library or directly from the camera, like so:

| iOS                                                                                                                   | Android                                                                                                                       |
| --------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------- |
| <img title="iOS" src="https://github.com/react-community/react-native-image-picker/blob/master/images/ios-image.png"> | <img title="Android" src="https://github.com/react-community/react-native-image-picker/blob/master/images/android-image.png"> |

#### _Before you open an issue_

This library started as a basic bridge of the native iOS image picker, and I want to keep it that way. As such, functionality beyond what the native `UIImagePickerController` supports will not be supported here. **Multiple image selection, more control over the crop tool, and landscape support** are things missing from the native iOS functionality - **not issues with my library**. If you need these things, [react-native-image-crop-picker](https://github.com/ivpusic/react-native-image-crop-picker) might be a better choice for you.

## React Native Compatibility
To use this library you need to ensure you match up with the correct version of React Native you are using.

p.s. React Native introduced AndroidX support in 0.60, which is a **breaking change** for most libraries (incl. this one) using native Android functionality.

| `@react-native-community/imagepicker` version | Required React Native Version                                                     |
| ----------------------------------------- | --------------------------------------------------------------------------------- |
| `1.x.x`                                   | `>= 0.60` or `>= 0.59` if using [Jetifier](https://github.com/mikehardy/jetifier) |
| `0.x.x`                                   | `<= 0.59`                                                                         |


## Getting Started

```
yarn add react-native-image-picker
react-native link react-native-image-picker
```

You will also need to add `UsageDescription` on iOS and some permissions on Android, refer to the [Install doc](docs/Install.md).

## Usage

```javascript
import ImagePicker from 'react-native-image-picker';

// More info on all the options is below in the API Reference... just some common use cases shown here
const options = {
  title: 'Select Avatar',
  customButtons: [{ name: 'fb', title: 'Choose Photo from Facebook' }],
  storageOptions: {
    skipBackup: true,
    path: 'images',
  },
};

/**
 * The first arg is the options object for customization (it can also be null or omitted for default options),
 * The second arg is the callback which sends object: response (more info in the API Reference)
 */
ImagePicker.showImagePicker(options, (response) => {
  console.log('Response = ', response);

  if (response.didCancel) {
    console.log('User cancelled image picker');
  } else if (response.error) {
    console.log('ImagePicker Error: ', response.error);
  } else if (response.customButton) {
    console.log('User tapped custom button: ', response.customButton);
  } else {
    const source = { uri: response.uri };

    // You can also display the image using data:
    // const source = { uri: 'data:image/jpeg;base64,' + response.data };

    this.setState({
      avatarSource: source,
    });
  }
});
```

Then later, if you want to display this image in your render() method:

```javascript
<Image source={this.state.avatarSource} style={styles.uploadAvatar} />
```

### Directly Launching the Camera or Image Library

To Launch the Camera or Image Library directly (skipping the alert dialog) you can
do the following:

```javascript
// Launch Camera:
ImagePicker.launchCamera(options, (response) => {
  // Same code as in above section!
});

// Open Image Library:
ImagePicker.launchImageLibrary(options, (response) => {
  // Same code as in above section!
});
```

#### Notes

On iOS, don't assume that the absolute uri returned will persist. See [#107](/../../issues/107)

For more, read the [API Reference](docs/Reference.md).

## License

[MIT](LICENSE.md)
