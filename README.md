
# React Native Image Picker [![npm version](https://badge.fury.io/js/react-native-image-picker.svg)](https://badge.fury.io/js/react-native-image-picker) [![npm](https://img.shields.io/npm/dt/react-native-image-picker.svg)](https://npmcharts.com/compare/react-native-image-picker?minimal=true) ![MIT](https://img.shields.io/dub/l/vibe-d.svg) ![Platform - Android and iOS](https://img.shields.io/badge/platform-Android%20%7C%20iOS-yellow.svg) [![Gitter chat](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/react-native-image-picker/Lobby)

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

### NOTE: THIS PACKAGE IS NOW BUILT FOR REACT NATIVE 0.40 OR GREATER! IF YOU NEED TO SUPPORT REACT NATIVE < 0.40, YOU SHOULD INSTALL THIS PACKAGE `@0.24`

`npm install react-native-image-picker@latest --save`

### Automatic Installation

`react-native link`

IMPORTANT NOTE: You'll still need to perform step 4 for iOS and steps 2 and 5 for Android of the manual instructions below.

### Manual Installation

#### iOS

1. In the XCode's "Project navigator", right click on your project's Libraries folder ➜ `Add Files to <...>`
2. Go to `node_modules` ➜ `react-native-image-picker` ➜ `ios` ➜ select `RNImagePicker.xcodeproj`
3. Add `RNImagePicker.a` to `Build Phases -> Link Binary With Libraries`
4. For iOS 10+, Add the `NSPhotoLibraryUsageDescription`, `NSCameraUsageDescription`, `NSPhotoLibraryAddUsageDescription` and `NSMicrophoneUsageDescription` (if allowing video) keys to your `Info.plist` with strings describing why your app needs these permissions. **Note: You will get a SIGABRT crash if you don't complete this step**

```
<plist version="1.0">
  <dict>
    ...
    <key>NSPhotoLibraryUsageDescription</key>
    <string>$(PRODUCT_NAME) would like access to your photo gallery</string>
    <key>NSCameraUsageDescription</key>
    <string>$(PRODUCT_NAME) would like to use your camera</string>
    <key>NSPhotoLibraryAddUsageDescription</key>
    <string>$(PRODUCT_NAME) would like to save photos to your photo gallery</string>
    <key>NSMicrophoneUsageDescription</key>
    <string>$(PRODUCT_NAME) would like to your microphone (for videos)</string>
  </dict>
</plist>
```
5. Compile and have fun

#### Android
1. Add the following lines to `android/settings.gradle`:
    ```gradle
    include ':react-native-image-picker'
    project(':react-native-image-picker').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-image-picker/android')
    ```

2. Update the android build tools version to `2.2.+` in `android/build.gradle`:
    ```gradle
    buildscript {
        ...
        dependencies {
            classpath 'com.android.tools.build:gradle:2.2.+' // <- USE 2.2.+ version
        }
        ...
    }
    ...
    ```

3. Update the gradle version to `2.14.1` in `android/gradle/wrapper/gradle-wrapper.properties`:
    ```
    ...
    distributionUrl=https\://services.gradle.org/distributions/gradle-2.14.1-all.zip
    ```

4. Add the compile line to the dependencies in `android/app/build.gradle`:
    ```gradle
    dependencies {
        compile project(':react-native-image-picker')
    }
    ```

5. Add the required permissions in `AndroidManifest.xml`:
    ```xml
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
    ```

6. Add the import and link the package in `MainApplication.java`:
    ```java
    import com.imagepicker.ImagePickerPackage; // <-- add this import

    public class MainApplication extends Application implements ReactApplication {
        @Override
        protected List<ReactPackage> getPackages() {
            return Arrays.<ReactPackage>asList(
                new MainReactPackage(),
                new ImagePickerPackage() // <-- add this line
                // OR if you want to customize dialog style
                new ImagePickerPackage(R.style.my_dialog_style)
            );
        }
    }
    ```

##### Android (Optional)

Customization settings of dialog `android/app/res/values/themes.xml` (`android/app/res/values/style.xml` is a valid path as well):
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="DefaultExplainingPermissionsTheme" parent="Theme.AppCompat.Light.Dialog.Alert">
        <!-- Used for the buttons -->
        <item name="colorAccent">@color/your_color</item>

        <!-- Used for the title and text -->
        <item name="android:textColorPrimary">@color/your_color</item>

        <!-- Used for the background -->
        <item name="android:background">@color/your_color</item>
    </style>
</resources>
```

If `MainActivity` is not instance of `ReactActivity`, you will need to implement `OnImagePickerPermissionsCallback` to `MainActivity`:
```java
import com.imagepicker.permissions.OnImagePickerPermissionsCallback; // <- add this import
import com.facebook.react.modules.core.PermissionListener; // <- add this import

public class MainActivity extends YourActivity implements OnImagePickerPermissionsCallback {
  private PermissionListener listener; // <- add this attribute

  // Your methods here

  // Copy from here

  @Override
  public void setPermissionListener(PermissionListener listener)
  {
    this.listener = listener;
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
  {
    if (listener != null)
    {
      listener.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }

  // To here
}
```
This code allows to pass result of request permissions to native part.

## Usage

```javascript
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
    let source = { uri: response.uri };

    // You can also display the image using data:
    // let source = { uri: 'data:image/jpeg;base64,' + response.data };

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
storageOptions | OK | OK | If this key is provided, the image will be saved in your app's `Documents` directory on iOS, or your app's `Pictures` directory on Android (rather than a temporary directory)
storageOptions.skipBackup | OK | - | If true, the photo will NOT be backed up to iCloud
storageOptions.path | OK | - | If set, will save the image at `Documents/[path]/` rather than the root `Documents`
storageOptions.cameraRoll | OK | OK | If true, the cropped photo will be saved to the iOS Camera Roll or Android DCIM folder.
storageOptions.waitUntilSaved | OK | - | If true, will delay the response callback until after the photo/video was saved to the Camera Roll. If the photo or video was just taken, then the file name and timestamp fields are only provided in the response object when this AND `cameraRoll` are both true.
permissionDenied.title | - | OK | Title of explaining permissions dialog. By default `Permission denied`.
permissionDenied.text | - | OK | Message of explaining permissions dialog. By default `To be able to take pictures with your camera and choose images from your library.`.
permissionDenied.reTryTitle | - | OK | Title of re-try button. By default `re-try`
permissionDenied.okTitle | - | OK | Title of ok button. By default `I'm sure`

### The Response Object

key | iOS | Android | Description
------ | ---- | ------- | ----------------------
didCancel | OK | OK | Informs you if the user cancelled the process
error | OK | OK | Contains an error message, if there is one
customButton | OK | OK | If the user tapped one of your custom buttons, contains the name of it
data | OK | OK | The base64 encoded image data (photos only)
uri | OK | OK | The uri to the local file asset on the device (photo or video)
origURL | OK | - | The URL of the original asset in photo library, if it exists
isVertical | OK | OK | Will be true if the image is vertically oriented
width | OK | OK | Image dimensions (photos only)
height | OK | OK | Image dimensions (photos only)
fileSize | OK | OK | The file size (photos only)
type | - | OK | The file type (photos only)
fileName | OK (photos and videos) | OK (photos) | The file name
path | - | OK | The file path
latitude | OK | OK | Latitude metadata, if available
longitude | OK | OK | Longitude metadata, if available
timestamp | OK | OK | Timestamp metadata, if available, in ISO8601 UTC format
originalRotation | - | OK | Rotation degrees (photos only) *See [#109](/../../issues/199)*
