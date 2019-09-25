# Install

```
yarn add react-native-image-picker

# RN >= 0.60
cd ios && pod install

# RN < 0.60
react-native link react-native-image-picker
```

⚠️ If you need to support React Native < 0.40, you must install this package: `react-native-image-picker@0.24`.

## Post-install Steps

### iOS

For iOS 10+:

Add the `NSPhotoLibraryUsageDescription`, `NSCameraUsageDescription`, `NSPhotoLibraryAddUsageDescription` and `NSMicrophoneUsageDescription` (if allowing video) keys to your `Info.plist` with strings describing why your app needs these permissions.

**Note: You will get a SIGABRT crash if you don't complete this step**

```xml
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
    <string>$(PRODUCT_NAME) would like to use your microphone (for videos)</string>
  </dict>
</plist>
```

⚠️ If you are planning on submitting your application to app store:

To be compliant with Guideline 5.1.1 - Legal - Privacy - Data Collection and Storage, the permission request alert should specify how your app will use this feature to help users understand why your app is requesting access to their personal data.

```
$(PRODUCT_NAME) would like access to your photo gallery to change your profile picture
```


### Android

Add the required permissions in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```

### Android (Optional)

If you've defined _[project-wide properties](https://developer.android.com/studio/build/gradle-tips.html)_ (**recommended**) in your root `build.gradle`, this library will detect the presence of the following properties:

```groovy
buildscript {...}
allprojects {...}

/**
  + Project-wide Gradle configuration properties
  */
ext {
    compileSdkVersion   = 27
    targetSdkVersion    = 27
    buildToolsVersion   = "27.0.3"
}
```

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

## Manual Installation

### iOS

1. In the XCode's "Project navigator", right click on your project's Libraries folder ➜ `Add Files to <...>`.
1. Go to `node_modules` ➜ `react-native-image-picker` ➜ `ios` ➜ select `RNImagePicker.xcodeproj`.
1. Add `libRNImagePicker.a` to `Build Phases -> Link Binary With Libraries`.
1. Refer to [Post-install Steps](Install.md#post-install-steps).
1. Compile and have fun.

### Android

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

4. Add the implementation line to the dependencies in `android/app/build.gradle`:

   ```gradle
   dependencies {
       implementation project(':react-native-image-picker')
   }
   ```

5. Add the import and link the package in `MainApplication.java`:

   ```java
   import com.imagepicker.ImagePickerPackage; // <-- add this import

   public class MainApplication extends Application implements ReactApplication {
       @Override
       protected List<ReactPackage> getPackages() {
           return Arrays.<ReactPackage>asList(
               new MainReactPackage(),
               new ImagePickerPackage(), // <-- add this line
               // OR if you want to customize dialog style
               new ImagePickerPackage(R.style.my_dialog_style)
           );
       }
   }
   ```

6. If `MainActivity` is not instance of `ReactActivity`, you will need to implement `OnImagePickerPermissionsCallback` to `MainActivity`:

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

7. Refer to [Post-install Steps](Install.md#post-install-steps).
