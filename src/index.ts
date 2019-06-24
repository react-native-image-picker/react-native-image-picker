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

const DEFAULT_OPTIONS = {
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

export default {
  ...NativeInterface,
  showImagePicker: (
    options: ImagePickerOptions,
    callback: (response: ImagePickerResponse) => void,
  ) => {
    if (typeof options === 'function') {
      callback = options;
      options = {};
    }

    // @ts-ignore - if this is undefined, then nativeInterface will throw an exception
    return NativeInterface.showImagePicker(
      {...DEFAULT_OPTIONS, ...options},
      callback,
    );
  },
};

export * from './internal/types';
