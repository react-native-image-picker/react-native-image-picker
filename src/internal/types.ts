/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @format
 */

export interface ImagePickerResponse {
  customButton: string;
  didCancel: boolean;
  error: string;
  data: string;
  uri: string;
  origURL?: string;
  isVertical: boolean;
  width: number;
  height: number;
  fileSize: number;
  type?: string;
  fileName?: string;
  path?: string;
  latitude?: number;
  longitude?: number;
  timestamp?: string;
}

export interface ImagePickerCustomButtonOptions {
  name?: string;
  title?: string;
}

export interface ImagePickerOptions {
  title?: string;
  cancelButtonTitle?: string;
  takePhotoButtonTitle?: string;
  chooseFromLibraryButtonTitle?: string;
  chooseWhichLibraryTitle?: string;
  customButtons?: ImagePickerCustomButtonOptions[];
  cameraType?: 'front' | 'back';
  mediaType?: 'photo' | 'video' | 'mixed';
  maxWidth?: number;
  maxHeight?: number;
  quality?: number;
  videoQuality?: 'low' | 'medium' | 'high';
  durationLimit?: number;
  rotation?: number;
  allowsEditing?: boolean;
  noData?: boolean;
  storageOptions?: ImagePickerStorageOptions;
  permissionDenied?: ImagePickerPermissionDeniedOptions;
  tintColor?: number | string;
}

export interface ImagePickerStorageOptions {
  skipBackup?: boolean;
  path?: string;
  cameraRoll?: boolean;
  waitUntilSaved?: boolean;
}

export interface ImagePickerPermissionDeniedOptions {
  title: string;
  text: string;
  reTryTitle: string;
  okTitle: string;
}
