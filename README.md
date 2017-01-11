
# React Native Image Picker [![npm version](https://badge.fury.io/js/react-native-image-picker.svg)](https://badge.fury.io/js/react-native-image-picker) [![npm](https://img.shields.io/npm/dt/react-native-image-picker.svg)](https://www.npmjs.org/package/react-native-image-picker) ![MIT](https://img.shields.io/dub/l/vibe-d.svg) ![Platform - Android and iOS](https://img.shields.io/badge/platform-Android%20%7C%20iOS-yellow.svg)

A React Native module that allows you to use native UI to select a photo/video from the device library or directly from the camera, like so:

iOS | Android
------- | ----
<img title="iOS" src="https://github.com/marcshilling/react-native-image-picker/blob/master/images/ios-image.png"> | <img title="Android" src="https://github.com/marcshilling/react-native-image-picker/blob/master/images/android-image.png">

#### _Before you open an issue_
This library started as a basic bridge of the native iOS image picker, and I want to keep it that way. As such, functionality beyond what the native `UIImagePickerController` supports will not be supported here. **Multiple image selection, more control over the crop tool, and landscape support** are things missing from the native iOS functionality - **not issues with my library**. If you need these things, [react-native-image-crop-picker](https://github.com/ivpusic/react-native-image-crop-picker) might be a better choice for you.   

## Table of contents
- [Install](#install)
- [Usage](#usage)
- [Direct launch](#directly-launching-the-camera-or-image-library)
- [Options](#options)
- [Response object](#the-response-object)

## Install

#### NOTE: AS OF v0.25.0, THIS PACKAGE REQUIRES YOU TO BE USING REACT NATIVE 0.40 OR GREATER!

`npm install react-native-image-picker@latest --save`

### Automatic Installation

`$rnpm link`

IMPORTANT NOTE: You'll still need to perform step 4 for iOS and step 3 for Android of the manual instructions below.

### Manual Installation

#### iOS

1. In the XCode's "Project navigator", right click on your project's Libraries folder ➜ `Add Files to <...>`
2. Go to `node_modules` ➜ `react-native-image-picker` ➜ `ios` ➜ select `RNImagePicker.xcodeproj`
3. Add `RNImagePicker.a` to `Build Phases -> Link Binary With Libraries`
4. For iOS 10+, Add the `NSPhotoLibraryUsageDescription`, `NSCameraUsageDescription`, and `NSMicrophoneUsageDescription` (if allowing video) keys to your `Info.plist` with strings describing why your app needs these permissions
5. Compile and have fun

#### Android
1. Add the following lines to `android/settings.gradle`:

    ```gradle
    include ':react-native-image-picker'
    project(':react-native-image-picker').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-image-picker/android')
    ```
2. Add the compile line to the dependencies in `android/app/build.gradle`:

    ```gradle
    dependencies {
        compile project(':react-native-image-picker')
    }
    ```
3. Add the required permissions in `AndroidManifest.xml`:

    ```xml
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
    ```
4. Add the import and link the package in `MainApplication.java`:

    ```java
    import com.imagepicker.ImagePickerPackage; // <-- add this import

    public class MainApplication extends Application implements ReactApplication {
        @Override
        protected List<ReactPackage> getPackages() {
            return Arrays.<ReactPackage>asList(
                new MainReactPackage(),
                new ImagePickerPackage() // <-- add this line
            );
        }
    }
```

## Usage

```javascript
var Platform = require('react-native').Platform;
var ImagePicker = require('react-native-image-picker');

// More info on all the options is below in the README...just some common use cases shown here
var options = {
  title: 'Select Avatar',
  customButtons: [
    {name: 'fb', title: 'Choose Photo from Facebook'},
  ],
  storageOptions: {
    skipBackup: true,
    path: 'images'
  }
};

/**
 * The first arg is the options object for customization (it can also be null or omitted for default options),
 * The second arg is the callback which sends object: response (more info below in README)
 */
ImagePicker.showImagePicker(options, (response) => {
  console.log('Response = ', response);

  if (response.didCancel) {
    console.log('User cancelled image picker');
  }
  else if (response.error) {
    console.log('ImagePicker Error: ', response.error);
  }
  else if (response.customButton) {
    console.log('User tapped custom button: ', response.customButton);
  }
  else {
    let source;

    // You can display the image using either data...
    source = {uri: 'data:image/jpeg;base64,' + response.data, isStatic: true};

    // Or a reference to the platform specific asset location
    if (Platform.OS === 'android') {
      source = { uri: response.uri };
    } else {
      source = { uri: response.uri.replace('file://', '') };
    }

    this.setState({
      avatarSource: source
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
ImagePicker.launchCamera(options, (response)  => {
  // Same code as in above section!
});

// Open Image Library:
ImagePicker.launchImageLibrary(options, (response)  => {
  // Same code as in above section!
});
```


#### Note
On iOS, don't assume that the absolute uri returned will persist. See [#107](/../../issues/107)

### Options

option | iOS  | Android | Info
------ | ---- | ------- | ----
title | OK | OK | Specify `null` or empty string to remove the title
cancelButtonTitle | OK | OK | Specify `null` or empty string to remove this button (Android only)
takePhotoButtonTitle | OK | OK | Specify `null` or empty string to remove this button
chooseFromLibraryButtonTitle | OK | OK | Specify `null` or empty string to remove this button
customButtons | OK | OK | An array containing objects with the name and title of buttons
cameraType | OK | - | 'front' or 'back'
mediaType | OK | OK | 'photo', 'video', or 'mixed' on iOS, 'photo' or 'video' on Android
maxWidth | OK | OK | Photos only
maxHeight | OK | OK | Photos only
quality | OK | OK | 0 to 1, photos only
videoQuality | OK |  OK | 'low', 'medium', or 'high' on iOS, 'low' or 'high' on Android
durationLimit | OK | OK | Max video recording time, in seconds
rotation | - | OK | Photos only, 0 to 360 degrees of rotation
allowsEditing | OK | - | bool - enables built in iOS functionality to resize the image after selection
noData | OK | OK | If true, disables the base64 `data` field from being generated (greatly improves performance on large photos)
storageOptions | OK | OK | If this key is provided, the image will get saved in the Documents directory on iOS, and the Pictures directory on Android (rather than a temporary directory)
storageOptions.skipBackup | OK | - | If true, the photo will NOT be backed up to iCloud
storageOptions.path | OK | - | If set, will save image at /Documents/[path] rather than the root
storageOptions.cameraRoll | OK | - | If true, the cropped photo will be saved to the iOS Camera Roll.
storageOptions.waitUntilSaved | OK | - | If true, will delay the response callback until after the photo/video was saved to the Camera Roll. If the photo or video was just taken, then the file name and timestamp fields are only provided in the response object when this is true.

### The Response Object

key | iOS | Android | Description
------ | ---- | ------- | ----------------------
didCancel | OK | OK | Informs you if the user cancelled the process
error | OK | OK | Contains an error message, if there is one
data | OK | OK | The base64 encoded image data (photos only)
uri | OK | OK | The uri to the local file asset on the device (photo or video)
origURL | OK | - | The URL of the original asset in photo library, if it exists
isVertical | OK | OK | Will be true if the image is vertically oriented
width | OK | OK | Image dimensions
height | OK | OK | Image dimensions
fileSize | OK | OK | The file size (photos only)
type | - | OK | The file type (photos only)
fileName | OK (photos and videos) | OK (photos) | The file name
path | - | OK | The file path
latitude | OK | OK | Latitude metadata, if available
longitude | OK | OK | Longitude metadata, if available
timestamp | OK | OK | Timestamp metadata, if available, in ISO8601 UTC format
originalRotation | - | OK | Rotation degrees (photos only) *See [#109](/../../issues/199)*
