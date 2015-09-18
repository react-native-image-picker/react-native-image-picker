# react-native-image-picker
A React Native module that allows you to use the native UIImagePickerController UI to either select a photo from the device library or directly from the camera, like so:
![Screenshot of the UIActionSheet](https://github.com/marcshilling/react-native-image-picker/blob/master/AlertSheetImage.jpg)

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
    title: 'Select Avatar',
    cancelButtonTitle: 'Cancel',
    takePhotoButtonTitle: 'Take Photo...',
    takePhotoButtonHidden: false,
    chooseFromLibraryButtonTitle: 'Choose from Library...',
    chooseFromLibraryButtonHidden: false,
    customButtons: {
      'Choose Photo from Facebook': 'fb', // [Button Text] : [String returned upon selection]
    },
    maxWidth: 100,
    maxHeight: 100,
    returnBase64Image: false,
    returnIsVertical: false,
    quality: 0.2,
    allowsEditing: false // Built in iOS functionality to resize/reposition the image
  };

  // The first arg will be the options object for customization, the second is
  // your callback which sends string: type, string: response
  UIImagePickerManager.showImagePicker(options, (type, response) => {
    if (type !== 'cancel') {
      var source;
      if (type === 'data') { // New photo taken OR passed returnBase64Image true -  response is the 64 bit encoded image data string
        source = {uri: 'data:image/jpeg;base64,' + response, isStatic: true};
      } else { // Selected from library - response is the URI to the local file asset
        source = {uri: response};
      }

      this.setState({avatarSource:source});
    } else {
      console.log('Cancel');
    }
  });
  ```
  Then later, if you want to display this image in your render() method:
  ```javascript
  <Image source={this.state.avatarSource} style={styles.uploadAvatar} />
  ```
