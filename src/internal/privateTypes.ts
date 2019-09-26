/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @format
 */

import {ImagePickerResponse, ImagePickerOptions} from './types';

export interface ImagePickerNativeModule {
  showImagePicker(
    options: ImagePickerOptions,
    callback: (response: ImagePickerResponse) => void,
  ): void;
  launchCamera(
    options: ImagePickerOptions,
    callback: (response: ImagePickerResponse) => void,
  ): void;
  launchImageLibrary(
    options: ImagePickerOptions,
    callback: (response: ImagePickerResponse) => void,
  ): void;
}
