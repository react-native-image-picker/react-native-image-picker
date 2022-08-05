import {
  CameraOptions,
  ImageLibraryOptions,
  Callback,
  ImagePickerResponse,
  ErrorCode,
  Asset,
  MediaType,
} from '../types';

const DEFAULT_OPTIONS: Pick<
  ImageLibraryOptions & CameraOptions,
  'mediaType' | 'includeBase64' | 'selectionLimit'
> = {
  mediaType: 'photo',
  includeBase64: false,
  selectionLimit: 1,
};

export function camera(
  options: ImageLibraryOptions = DEFAULT_OPTIONS,
  callback?: Callback,
): Promise<ImagePickerResponse> {
  return new Promise((resolve) => {
    const result = {
      errorCode: 'camera_unavailable' as ErrorCode,
      errorMessage: 'launchCamera is not supported for web yet',
    };

    if (callback) callback(result);

    resolve(result);
  });
}

export function imageLibrary(
  options: ImageLibraryOptions = DEFAULT_OPTIONS,
  callback?: Callback,
): Promise<ImagePickerResponse> {
  // Only supporting 'photo' mediaType for now.
  if (options.mediaType !== 'photo') {
    const result = {
      errorCode: 'others' as ErrorCode,
      errorMessage: 'For now, only photo mediaType is supported for web',
    };

    if (callback) callback(result);

    return Promise.resolve(result);
  }

  const input = document.createElement('input');
  input.style.display = 'none';
  input.setAttribute('type', 'file');
  input.setAttribute('accept', getWebMediaType(options.mediaType));

  if (options.selectionLimit! > 1) {
    input.setAttribute('multiple', 'multiple');
  }

  document.body.appendChild(input);

  return new Promise((resolve) => {
    input.addEventListener('change', async () => {
      if (input.files) {
        if (options.selectionLimit! <= 1) {
          const img = await readFile(input.files[0], {
            includeBase64: options.includeBase64,
          });

          const result = {assets: [img]};

          if (callback) callback(result);

          resolve(result);
        } else {
          const imgs = await Promise.all(
            Array.from(input.files).map((file) =>
              readFile(file, {includeBase64: options.includeBase64}),
            ),
          );

          const result = {
            didCancel: false,
            assets: imgs,
          };

          if (callback) callback(result);

          resolve(result);
        }
      }
      document.body.removeChild(input);
    });

    const event = new MouseEvent('click');
    input.dispatchEvent(event);
  });
}

function readFile(
  targetFile: Blob,
  options: Partial<ImageLibraryOptions>,
): Promise<Asset> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onerror = () => {
      reject(
        new Error(
          `Failed to read the selected media because the operation failed.`,
        ),
      );
    };
    reader.onload = ({target}) => {
      const uri = target?.result;

      const returnRaw = () =>
        resolve({
          uri: uri as string,
          width: 0,
          height: 0,
        });

      if (typeof uri === 'string') {
        const image = new Image();
        image.src = uri;
        image.onload = () =>
          resolve({
            uri,
            width: image.naturalWidth ?? image.width,
            height: image.naturalHeight ?? image.height,
            // The blob's result cannot be directly decoded as Base64 without
            // first removing the Data-URL declaration preceding the
            // Base64-encoded data. To retrieve only the Base64 encoded string,
            // first remove data:*/*;base64, from the result.
            // https://developer.mozilla.org/en-US/docs/Web/API/FileReader/readAsDataURL
            ...(options.includeBase64 && {
              base64: uri.substr(uri.indexOf(',') + 1),
            }),
          });
        image.onerror = () => returnRaw();
      } else {
        returnRaw();
      }
    };

    reader.readAsDataURL(targetFile);
  });
}

function getWebMediaType(mediaType: MediaType) {
  const webMediaTypes = {
    photo: 'image/*',
    video: 'video/*',
    mixed: 'image/*,video/*',
  };

  return webMediaTypes[mediaType] ?? webMediaTypes.photo;
}
