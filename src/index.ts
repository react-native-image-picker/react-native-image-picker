import {Platform} from 'react-native';

import {CameraOptions, ImageLibraryOptions, Callback} from './types';
import {
  imageLibrary as nativeImageLibrary,
  camera as nativeCamera,
} from './platforms/native';
import {
  imageLibrary as webImageLibrary,
  camera as webCamera,
} from './platforms/web';

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
