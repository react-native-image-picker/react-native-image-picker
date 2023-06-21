import {Platform} from 'react-native';

import {
  camera as nativeCamera,
  imageLibrary as nativeImageLibrary,
  updatePickerAccess as nativeUpdatePickerAccess,
} from './platforms/native';
import {
  camera as webCamera,
  imageLibrary as webImageLibrary,
} from './platforms/web';
import {Callback, CameraOptions, ImageLibraryOptions} from './types';

export * from './types';

export function launchCamera(options: CameraOptions, callback?: Callback) {
  return Platform.OS === 'web'
    ? webCamera(options, callback)
    : nativeCamera(options, callback);
}

export function launchImageLibrary(
  options: ImageLibraryOptions,
  callback?: Callback,
) {
  return Platform.OS === 'web'
    ? webImageLibrary(options, callback)
    : nativeImageLibrary(options, callback);
}

export function updatePickerAccess(callback?: () => void) {
  return Platform.OS === 'ios'
    ? nativeUpdatePickerAccess(callback)
    : callback?.();
}
