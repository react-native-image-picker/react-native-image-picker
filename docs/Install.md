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

For iOS 10+:

Add the `NSPhotoLibraryUsageDescription`, `NSCameraUsageDescription`, `NSPhotoLibraryAddUsageDescription` and `NSMicrophoneUsageDescription` (if allowing video) keys to your `Info.plist` with strings describing why your app needs these permissions.

### Android

Add the required permissions in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```
