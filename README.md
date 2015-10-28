# react-native-image-picker
A React Native module that allows you to use the native UIImagePickerController UI to either select a photo from the device library or directly from the camera, like so:
![Screenshot of the UIActionSheet](https://github.com/marcshilling/react-native-image-picker/blob/master/AlertSheetImage.jpg)

**Requires iOS 8 or higher**

## Install
1. `npm install react-native-image-picker@latest --save`
2. In the XCode's "Project navigator", right click on your project's Libraries folder ➜ `Add Files to <...>`
3. Go to `node_modules` ➜ `react-native-image-picker` ➜ select the `UIImagePickerManager` folder **Make sure you have 'Create Groups' selected**
4. Make sure `UIImagePickerManager.m` is listed under 'Compile Sources' in your project's 'Build Phases' tab
5. Compile and have fun!

## Usage
1. In your React Native javascript code, bring in the native module:

  ```javascript
var UIImagePickerManager = require('NativeModules').UIImagePickerManager;
  ```
2. Use it like so:

  When you want to display the picker:
  ```javascript

  // Specify any or all of these keys
  var options = {
    title: 'Select Avatar', // specify null or empty string to remove the title
    cancelButtonTitle: 'Cancel',
    takePhotoButtonTitle: 'Take Photo...', // specify null or empty string to remove this button
    chooseFromLibraryButtonTitle: 'Choose from Library...', // specify null or empty string to remove this button
    customButtons: {
      'Choose Photo from Facebook': 'fb', // [Button Text] : [String returned upon selection]
    },
    maxWidth: 100,
    maxHeight: 100,
    quality: 0.2,
    allowsEditing: false, // Built in iOS functionality to resize/reposition the image
    noData: false, // Disables the base64 `data` field from being generated (greatly improves performance on large photos)
    storageOptions: { // if this key is provided, the image will get saved in the documents directory (rather than a temporary directory)
      skipBackup: true, // image will NOT be backed up to icloud
      path: 'images' // will save image at /Documents/images rather than the root
    }
  };

  // The first arg will be the options object for customization, the second is
  // your callback which sends bool: didCancel, object: response.
  //
  // response.data is the base64 encoded image data
  // response.uri is the uri to the local file asset on the device
  // response.isVertical will be true if the image is vertically oriented
  // response.width & response.height give you the image dimensions
  UIImagePickerManager.showImagePicker(options, (didCancel, response) => {
    console.log('Response = ', response);

    if (didCancel) {
      console.log('User cancelled image picker');
    }
    else {
      if (response.customButton) {
        console.log('User tapped custom button: ', response.customButton);
      }
      else {
        // You can display the image using either:
        const source = {uri: 'data:image/jpeg;base64,' + response.data, isStatic: true};
        const source = {uri: response.uri.replace('file://', ''), isStatic: true};

        this.setState({
          avatarSource: source
        });
      }
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
  UIImagePickerManager.launchCamera(options, (didCancel, response)  => {
    // Same code as in above section!
  });

  // Open Image Library:
  UIImagePickerManager.launchImageLibrary(options, (didCancel, response)  => {
    // Same code as in above section!
  });
  ```
