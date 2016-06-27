'use strict'

const { NativeModules } = require('react-native');
const { ImagePickerManager } = NativeModules
const SHOW_PICKER_DEFAULT_OPTS = {
  title: 'Select a Photo',
  cancelButtonTitle: 'Cancel',
  takePhotoButtonTitle: 'Take Photo…',
  chooseFromLibraryButtonTitle: 'Choose from Library…',
  quality : 0.2, // 1.0 best to 0.0 worst
  allowsEditing : false
}

module.exports = {
  ...ImagePickerManager,
  showImagePicker: function showImagePicker (opts, callback) {
    if (typeof opts === 'function') {
      callback = opts
      opts = null
    }

    opts = opts || {}
    if ('cancelButtonTitle' in opts && !opts.cancelButtonTitle) {
      throw new Error('empty cancel button not allowed')
    }

    return ImagePickerManager.showImagePicker({ ...SHOW_PICKER_DEFAULT_OPTS, ...opts }, callback)
  }
}
