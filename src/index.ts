import {NativeModules} from 'react-native';

import {CameraOptions, ImageLibraryOptions, Callback, ImagePickerResponse} from './types';
export * from './types';

const DEFAULT_OPTIONS: ImageLibraryOptions & CameraOptions = {
  mediaType: 'photo',
  videoQuality: 'high',
  quality: 1,
  maxWidth: 0,
  maxHeight: 0,
  includeBase64: false,
  cameraType: 'back',
  selectionLimit: 1,
  saveToPhotos: false,
  durationLimit: 0,
};

export function launchCameraAsPromise(options: CameraOptions) : Promise<ImagePickerResponse> {
  return new Promise(resolve => {
    launchCamera(options, result => resolve(result));
  });
}

export function launchCamera(options: CameraOptions, callback: Callback) {
  if (typeof callback !== 'function') {
    console.error("Send proper callback function, check API");
    return;
  }

  NativeModules.ImagePickerManager.launchCamera(
    {...DEFAULT_OPTIONS, ...options},
    callback,
  );
}

export function launchImageLibraryAsPromise(options: ImageLibraryOptions) : Promise<ImagePickerResponse> {
  return new Promise(resolve => {
    launchImageLibrary(options, result => resolve(result));
  });
}

export function launchImageLibrary(
  options: ImageLibraryOptions,
  callback: Callback,
) {
  if (typeof callback !== 'function') {
    console.error("Send proper callback function, check API");
    return;
  }
  NativeModules.ImagePickerManager.launchImageLibrary(
    {...DEFAULT_OPTIONS, ...options},
    callback,
  );
}
