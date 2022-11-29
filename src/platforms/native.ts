import {NativeModules} from 'react-native';

import {
  CameraOptions,
  ImageLibraryOptions,
  Callback,
  ImagePickerResponse,
} from '../types';

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
  includeExtra: false,
  presentationStyle: 'pageSheet',
};

export function camera(
  options: CameraOptions,
  callback?: Callback,
): Promise<ImagePickerResponse> {
  return new Promise((resolve) => {
    filterUndefinedProperties(options);
    NativeModules.ImagePickerManager.launchCamera(
      {...DEFAULT_OPTIONS, ...options},
      (result: ImagePickerResponse) => {
        if (callback) callback(result);
        resolve(result);
      },
    );
  });
}

export function imageLibrary(
  options: ImageLibraryOptions,
  callback?: Callback,
): Promise<ImagePickerResponse> {
  return new Promise((resolve) => {
    filterUndefinedProperties(options);
    NativeModules.ImagePickerManager.launchImageLibrary(
      {...DEFAULT_OPTIONS, ...options},
      (result: ImagePickerResponse) => {
        if (callback) callback(result);
        resolve(result);
      },
    );
  });
}

function filterUndefinedProperties(obj: any) {
  Object.keys(obj).forEach(key => {
    if (obj[key] === undefined) {
      delete obj[key];
    }
  });
}