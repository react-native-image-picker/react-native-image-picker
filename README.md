# react-native-image-picker
A React Native module that allows you to use the native UIImagePickerController UI to either select a photo from the device library or directly from the camera. 

# Usage
1. Add `UIImagePickerManager.h` and `UIImagePickerManager.m` files to your XCode project
2. In your React Native javascript code, bring in the native module:

  ```javascript
var UIImagePickerManager = require('NativeModules').UIImagePickerManager;
  ```
3. Use it like so:

  When you want to display the picker:
  ```javascript  
  // The first arg will be the title of your UIAlertSheet, the second is your callback
  // which sends bool: isData, string: response 
  UIImagePickerManager.showImagePicker('Select Avatar', (isData, response) => {
    let source;
    if (isData) { // New photo taken -  response is the 64 bit encoded image data string
      source = {uri: 'data:image/jpeg;base64,' + response, isStatic: true};
    } else { // Selected from library - response is the URI to the local file asset
      source = {uri: response};
    }

    this.setState({
      avatarSource: source
    });
  });
  ```
  Then later, if you want to display this image in your render() method:
  ```javascript
  <Image source={this.state.avatarSource} style={styles.uploadAvatar} />
  ```

