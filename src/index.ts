import {NativeModules} from 'react-native';
import mime from 'mime/lite';

import {Asset, CameraOptions, ImageLibraryOptions, Callback, ImagePickerResponse} from './types';
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
  includeExtra: false,
};

function withCookedFileName(asset: Asset): Asset {
  const unchanged = () => ({ ...asset, cookedFileName: asset.fileName });

  const { type, fileName } = asset;
  if (type == null || fileName == null) {
    // Not enough data
    // TODO: Can remove this condition if `Asset.type` and `Asset.fileName`
    //   become non-optional.
    return unchanged();
  }
  if (/.\..+$/.test(fileName)) {
    // Already has an extension
    return unchanged();
  }
  // An extension can't in general be extracted directly from a MIME-type
  // string; we need a data source. See
  //   https://github.com/react-native-image-picker/react-native-image-picker/pull/1881#issuecomment-992034446
  //   https://www.npmjs.com/package/mime#user-content-lite-version
  const extension = mime.getExtension(type);
  if (extension == null) {
    // Appropriate extension not found
    return unchanged();
  }
  return {
    ...asset,
    cookedFileName: `${fileName}.${extension}`,
  };
}

export function launchCamera(options: CameraOptions, callback?: Callback) : Promise<ImagePickerResponse> {
  return new Promise(resolve => {
    NativeModules.ImagePickerManager.launchCamera(
      {...DEFAULT_OPTIONS, ...options},
      (rawResult: ImagePickerResponse) => {
        const result = {
          ...rawResult,
          assets: rawResult.assets?.map(asset => withCookedFileName(asset)),
        };
        if(callback) callback(result);
        resolve(result);
      },
    );
  });  
}

export function launchImageLibrary(
  options: ImageLibraryOptions,
  callback?: Callback,
) : Promise<ImagePickerResponse> {
  return new Promise(resolve => {
    NativeModules.ImagePickerManager.launchImageLibrary(
      {...DEFAULT_OPTIONS, ...options},
      (rawResult: ImagePickerResponse) => {
        const result = {
          ...rawResult,
          assets: rawResult.assets?.map(asset => withCookedFileName(asset)),
        };
        if(callback) callback(result);
        resolve(result);
      },
    );
  })
  
}
