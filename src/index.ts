/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @format
 */

import NativeInterface from './internal/nativeInterface';
import {ImagePickerOptions, ImagePickerResponse} from './internal/types';

const DEFAULT_OPTIONS: ImagePickerOptions = {
  title: 'Select a Photo',
  cancelButtonTitle: 'Cancel',
  takePhotoButtonTitle: 'Take Photo…',
  chooseFromLibraryButtonTitle: 'Choose from Library…',
  quality: 1.0,
  allowsEditing: false,
  permissionDenied: {
    title: 'Permission denied',
    text:
      'To be able to take pictures with your camera and choose images from your library.',
    reTryTitle: 're-try',
    okTitle: "I'm sure",
  },
};

type Callback = (response: ImagePickerResponse) => void;
type OptionsOrCallback = ImagePickerOptions | Callback;

class ImagePicker {
  showImagePicker(options: ImagePickerOptions, callback: Callback): void;
  showImagePicker(callback: Callback): void;

  showImagePicker(
    optionsOrCallback: OptionsOrCallback,
    callback?: Callback,
  ): void {
    if (typeof optionsOrCallback === 'function') {
      return NativeInterface.showImagePicker(
        DEFAULT_OPTIONS,
        optionsOrCallback,
      );
    }

    if (callback == null) {
      throw new Error('callback cannot be undefined');
    }

    return NativeInterface.showImagePicker(
      {...DEFAULT_OPTIONS, ...optionsOrCallback},
      callback,
    );
  }

  launchCamera(options: ImagePickerOptions, callback: Callback): void {
    return NativeInterface.launchCamera(
      {...DEFAULT_OPTIONS, ...options},
      callback,
    );
  }

  launchImageLibrary(options: ImagePickerOptions, callback: Callback): void {
    return NativeInterface.launchImageLibrary(
      {...DEFAULT_OPTIONS, ...options},
      callback,
    );
  }
}

export default new ImagePicker();

export * from './internal/types';
