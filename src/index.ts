import {NativeModules} from 'react-native';
import {
  CameraOptions,
  ImagePickerResponse,
  ImageLibraryOptions,
  Callback,
} from './types';
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

export function launchCameraAsync(
  options: CameraOptions,
): Promise<ImagePickerResponse> {
  return new Promise((resolve) => {
    NativeModules.ImagePickerManager.launchCamera(
      {...DEFAULT_OPTIONS, ...options},
      resolve,
    );
  });
}

export function launchImageLibraryAsync(
  options: ImageLibraryOptions,
): Promise<Response> {
  return new Promise((resolve) => {
    NativeModules.ImagePickerManager.launchImageLibrary(
      {...DEFAULT_OPTIONS, ...options},
      resolve,
    );
  });
}

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
