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
    okTitle: 'I\'m sure',
  }
};

function _call(func, options, callback) {
  if (typeof options === 'function') {
    callback = options;
    options = {};
  }
  return ImagePickerManager[func]({ ...DEFAULT_OPTIONS, ...options }, callback);
}

module.exports = {
  showImagePicker: function showImagePicker(options, callback) {
    _call('showImagePicker', options, callback);
  },

  launchImageLibrary: function launchImageLibrary(options, callback) {
    _call('launchImageLibrary', options, callback);
  },

  launchCamera: function launchCamera(options, callback) {
    _call('launchCamera', options, callback);
  }
};
