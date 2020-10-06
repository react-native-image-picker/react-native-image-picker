import { NativeModules } from 'react-native';

const DEFAULT_OPTIONS = {
  mediaType: 'photo',
  videoQuality: 'high',
  quality: 1,
  maxWidth: 0,
  maxHeight: 0,
  includeBase64: false,
  saveToPhotos: false
};

function launchCamera(options, callback) {
  NativeModules.ImagePickerManager.launchCamera({...DEFAULT_OPTIONS, ...options}, callback);
}

function launchImageLibrary(options, callback) {
  NativeModules.ImagePickerManager.launchImageLibrary({...DEFAULT_OPTIONS, ...options}, callback);
}

export default {
  launchCamera,
  launchImageLibrary
}
