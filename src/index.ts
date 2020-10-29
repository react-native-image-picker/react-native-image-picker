import {NativeModules} from 'react-native';
import {CameraOptions, ImageLibraryOptions, Callback} from './types';
export * from './types';

const DEFAULT_OPTIONS: CameraOptions = {
  mediaType: 'photo',
  videoQuality: 'high',
  quality: 1,
  maxWidth: 0,
  maxHeight: 0,
  includeBase64: false,
  saveToPhotos: false,
};

export function launchCamera(options: CameraOptions, callback: Callback) {
  NativeModules.ImagePickerManager.launchCamera(
    {...DEFAULT_OPTIONS, ...options},
    callback,
  );
}

export function launchImageLibrary(
  options: ImageLibraryOptions,
  callback: Callback,
) {
  NativeModules.ImagePickerManager.launchCamera(
    {...DEFAULT_OPTIONS, ...options},
    callback,
  );
}
