'use strict'

const { NativeModules } = require('react-native');
const { ImagePickerManager } = NativeModules;

const DEFAULT_OPTIONS = {
  title: 'Select a Photo',
  cancelButtonTitle: 'Cancel',
  takePhotoButtonTitle: 'Take Photo…',
  chooseFromLibraryButtonTitle: 'Choose from Library…',
  quality: 1.0,
  allowsEditing: false,
  permissionDenied: {
    title: 'Permission denied',
    text: 'To be able to take pictures with your camera and choose images from your library.',
    reTryTitle: 're-try',
    settingTitle: 'Open Settins',
    okTitle: 'I\'m sure',
  }
};

module.exports = {
  ...ImagePickerManager,
  showImagePicker: function showImagePicker(options, callback) {
    if(this.hasShow){
      return;
    }
    this.hasShow = true;
    if (typeof options === 'function') {
      callback = options;
      options = {};
    }
    return ImagePickerManager.showImagePicker({...DEFAULT_OPTIONS, ...options}, (response)=>{
      this.hasShow = false;
      callback&&callback(response);
    })
  }
}
