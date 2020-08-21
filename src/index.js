import { NativeModules } from 'react-native';

const DEFAULT_OPTIONS = {
  mediaType: 'photo',
  videoQuality: 'high',
  quality: 1,
  chooseWhichLibraryTitle: '',
  maxWidth: 0,
  maxHeight: 0,
  noData: true
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
