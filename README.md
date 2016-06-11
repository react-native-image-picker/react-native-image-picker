#### _Before you open an issue_
This library started as a basic bridge of the native iOS image picker, and I want to keep it that way. As such, functionality beyond what the native `UIImagePickerController` supports will not be supported here. **Multiple image selection, more control over the crop tool, and landscape support** are things missing from the native iOS functionality - **not issues with my library**. If you need these things, [react-native-image-crop-picker](https://github.com/ivpusic/react-native-image-crop-picker) might be a better choice for you.    
As for Android, I want to keep it in parity with iOS. So while you may have better luck with cropping/landscape, we will not support multiple image selection there either.

# react-native-image-picker
A React Native module that allows you to use native UI to select a photo/video from the device library or directly from the camera, like so:

iOS | Android
------- | ----
<img title="iOS" src="https://github.com/marcshilling/react-native-image-picker/blob/master/images/ios-image.png"> | <img title="Android" src="https://github.com/marcshilling/react-native-image-picker/blob/master/images/android-image.png">

## Table of contents
- [Install](#install)
  - [iOS](#ios)
  - [Android](#android)
- [Usage](#usage)
- [Direct launch](#directly-launching-the-camera-or-image-library)
- [Options](#options)
- [Response object](#the-response-object)

## Install

`npm install react-native-image-picker@latest --save`

Use [rnpm](https://github.com/rnpm/rnpm) to automatically complete the installation, or link manually like so:

### iOS

1. In the XCode's "Project navigator", right click on your project's Libraries folder ➜ `Add Files to <...>`
2. Go to `node_modules` ➜ `react-native-image-picker` ➜ `ios` ➜ select `RNImagePicker.xcodeproj`
3. Add `RNImagePicker.a` to `Build Phases -> Link Binary With Libraries`
4. Compile and have fun

### Android
```gradle
// file: android/settings.gradle
...

include ':react-native-image-picker'
project(':react-native-image-picker').projectDir = new File(settingsDir, '../node_modules/react-native-image-picker/android')
```
```gradle
// file: android/app/build.gradle
...

dependencies {
    ...
    compile project(':react-native-image-picker')
}
```
```xml
<!-- file: android/app/src/main/AndroidManifest.xml -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.myApp">

    <uses-permission android:name="android.permission.INTERNET" />

    <!-- add following permissions -->
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
    <uses-feature android:name="android.hardware.camera" android:required="false"/>
    <uses-feature android:name="android.hardware.camera.autofocus" android:required="false"/>
    <!-- -->
    ...
```
```java
// file: android/app/src/main/java/com/<...>/MainActivity.java
...

import com.imagepicker.ImagePickerPackage; // import package

public class MainActivity extends ReactActivity {

   /**
   * A list of packages used by the app. If the app uses additional views
   * or modules besides the default ones, add more packages here.
   */
    @Override
    protected List<ReactPackage> getPackages() {
        return Arrays.<ReactPackage>asList(
            new MainReactPackage(),
            new ImagePickerPackage() // Add package
        );
    }
...
}

```
## Usage

```javascript
var ImagePicker = require('react-native-image-picker');

var options = {
  title: 'Select Avatar', // specify null or empty string to remove the title
  cancelButtonTitle: 'Cancel',
  takePhotoButtonTitle: 'Take Photo...', // specify null or empty string to remove this button
  chooseFromLibraryButtonTitle: 'Choose from Library...', // specify null or empty string to remove this button
  customButtons: {
    'Choose Photo from Facebook': 'fb', // [Button Text] : [String returned upon selection]
  },
  cameraType: 'back', // 'front' or 'back'
  mediaType: 'photo', // 'photo' or 'video'
  videoQuality: 'high', // 'low', 'medium', or 'high'
  durationLimit: 10, // video recording max time in seconds
  maxWidth: 100, // photos only
  maxHeight: 100, // photos only
  aspectX: 2, // android only - aspectX:aspectY, the cropping image's ratio of width to height
  aspectY: 1, // android only - aspectX:aspectY, the cropping image's ratio of width to height
  quality: 0.2, // 0 to 1, photos only
  angle: 0, // android only, photos only
  allowsEditing: false, // Built in functionality to resize/reposition the image after selection
  noData: false, // photos only - disables the base64 `data` field from being generated (greatly improves performance on large photos)
  storageOptions: { // if this key is provided, the image will get saved in the documents directory on ios, and the pictures directory on android (rather than a temporary directory)
    skipBackup: true, // ios only - image will NOT be backed up to icloud
    path: 'images' // ios only - will save image at /Documents/images rather than the root
  }
};

/**
 * The first arg will be the options object for customization, the second is
 * your callback which sends object: response.
 *
 * See the README for info about the response
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
    // You can display the image using either data:
    const source = {uri: 'data:image/jpeg;base64,' + response.data, isStatic: true};

    // uri (on iOS)
    const source = {uri: response.uri.replace('file://', ''), isStatic: true};
    // uri (on android)
    const source = {uri: response.uri, isStatic: true};

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

option | iOS  | Android
------ | ---- | -------
title | OK | OK
cancelButtonTitle | OK | OK
takePhotoButtonTitle | OK | OK
chooseFromLibraryButtonTitle | OK | OK
customButtons | OK | OK
cameraType | OK | -
mediaType | 'video' or 'photo' | 'video' or 'photo'
videoQuality | 'low', 'medium', or 'high' | 'low' or 'high'
durationLimit | OK | OK
angle | - | OK
aspectX | - | OK
aspectY | - | OK
maxWidth | OK | OK
maxHeight | OK | OK
quality | OK | OK
allowsEditing | OK | OK
noData | OK | OK
storageOptions | OK | OK

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
fileName | - | OK | The file name (photos only)
path | - | OK | The file path
latitude | - | OK | Latitude metadata, if available
longitude | - | OK | Longitude metadata, if available
timestamp | - | OK | Timestamp metadata, if available, in ISO8601 UTC format
