import {NativeModules} from 'react-native';

import {
  CameraOptions,
  ImageLibraryOptions,
  Callback,
  ImagePickerResponse,
} from '../types';

const DEFAULT_OPTIONS: ImageLibraryOptions & CameraOptions = {
  mediaType: 'photo',
  restrictMimeTypes: [],
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
  assetRepresentationMode: 'auto',
};

// @ts-ignore We want to check whether __turboModuleProxy exitst, it may not
const isTurboModuleEnabled = global.__turboModuleProxy != null;

const nativeImagePicker = isTurboModuleEnabled
  ? require('./NativeImagePicker').default
  : NativeModules.ImagePicker;

export function camera(
  options: CameraOptions,
  callback?: Callback,
): Promise<ImagePickerResponse> {
  return new Promise((resolve) => {
    nativeImagePicker.launchCamera(
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
    nativeImagePicker.launchImageLibrary(
      {...DEFAULT_OPTIONS, ...options},
      (result: ImagePickerResponse) => {
        if (callback) callback(result);
        resolve(result);
      },
    );
  });
}
