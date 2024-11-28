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
  options: CameraOptions = DEFAULT_OPTIONS,
  callback?: Callback,
): Promise<ImagePickerResponse> {
  if (options.mediaType !== 'photo') {
    const result = {
      errorCode: 'others' as ErrorCode,
      errorMessage: 'For now, only photo mediaType is supported for web',
    }

    if (callback) callback(result);

    return Promise.resolve(result);
  }

  const container = document.createElement('div');
  const wrapper = document.createElement('div');
  const content = document.createElement('div');
  const buttons = document.createElement('div');
  const btnCapture = document.createElement('button');
  const btnBack = document.createElement('button');
  const btnSave = document.createElement('button');
  const btnCancel = document.createElement('button');
  const video = document.createElement('video');
  const canvas = document.createElement('canvas');

  // init video
  navigator.mediaDevices.getUserMedia({ audio: false, video: true })
    .then(stream => {
      video.srcObject = stream;
      video.play();
    }).catch(err => {
      console.log(err);
    })

  const isAlreadyUsingFontAwesome = !!document.getElementsByClassName('fa').length;

  if (!isAlreadyUsingFontAwesome) {
    const isAlreadyInjectedFontAwesome = !!document.getElementById('injected-font-awesome');
    if (!isAlreadyInjectedFontAwesome) { 
      // inject font-awesome
      const head = document.getElementsByTagName('HEAD')[0];
      const link = document.createElement('link');
      link.id = 'injected-font-awesome';
      link.rel = 'stylesheet';
      link.type = 'text/css';
      link.href = 'https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css';
      head.appendChild(link);
    }
  }

  container.style.cssText = `    
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: rgba(0,0,0,0.9);
    display: flex;
    align-items: center;
    justify-content: center;
  `;
  
  wrapper.style.cssText = `
    position: relative;
    min-height: min(480px, 100vh);
    min-width: min(640px, 100vw);
    border-radius: 8px 8px 0 0;
    background-color: #333333;
  `;

  video.style.cssText = 
  canvas.style.cssText = `
    position: absolute;
    top: 0;
    left: 0;
    border-radius: 8px 8px 0 0;
  `;

  content.style.cssText = `
    display: flex;
    flex-direction: column;
    margin: auto;
  `;

  buttons.style.cssText = `
    display: flex;
    align-items: center;
    justify-content: space-evenly;
    min-height: 60px;
    background-color: #333333;
    border-radius: 0 0 8px 8px;  
  `;
 
  btnCapture.innerHTML = '<i class="fa fa-2x fa-camera"></i>';
  // btnCapture.title = 'Capture';
  btnBack.innerHTML = '<i class="fa fa-2x fa-undo"></i>';
  // btnBack.title = 'Back';
  btnSave.innerHTML = '<i class="fa fa-2x fa-check"></i>';
  // btnSave.title = 'Apply';
  btnCancel.innerHTML = '<i class="fa fa-2x fa-close"></i>';
  // btnCancel.title = 'Cancel';

  btnCapture.style.cssText =
  btnBack.style.cssText =
  btnSave.style.cssText =
  btnCancel.style.cssText = `
    padding: 10px;
    color: #f2f2f2;
    border: 0;
    background: transparent;
  `;

  wrapper.append(video);
  wrapper.append(canvas);
  content.append(wrapper);
  content.append(buttons);
  container.append(content);

  document.body.appendChild(container);

  let hasPhoto = false;

  const handleButtons = () => {
    buttons.innerHTML = '';
    if (hasPhoto) {
      buttons.append(btnBack);
      buttons.append(btnSave);
    } else {
      buttons.append(btnCapture);
    }
    buttons.append(btnCancel);
  }

  handleButtons();

  return new Promise((resolve) => {
    btnCapture.addEventListener('click', async () => {
      canvas.width = video.videoWidth;
      canvas.height = video.videoHeight;
      canvas.getContext('2d')?.drawImage(video, 0, 0, canvas.width, canvas.height);
      hasPhoto = true;
      handleButtons();
    })

    btnBack.addEventListener('click', () => {
      canvas.getContext('2d')?.clearRect(0, 0, canvas.width, canvas.height);
      hasPhoto = false;
      handleButtons();
    })

    btnSave.addEventListener('click', async () => {
      const uri = canvas.toDataURL('image/png');
      const asset: Asset = { uri };
      const result = {assets: [asset]};

      if (callback) callback(result);
      resolve(result);

      document.body.removeChild(container);
    })
    
    btnCancel.addEventListener('click', async () => {
      const result = {
        assets: [],
        didCancel: true,
      }

      if (callback) callback(result);
      resolve(result);

      document.body.removeChild(container);
    })
  })
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
    const inputChangeHandler = async () => {
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
      cleanup();
    };

    const inputCancelHandler = async () => {
      resolve({didCancel: true});
      cleanup();
    };

    const cleanup = () => {
      input.removeEventListener('change', inputChangeHandler);
      input.removeEventListener('cancel', inputCancelHandler);
      document.body.removeChild(input);
    };

    input.addEventListener('change', inputChangeHandler);
    input.addEventListener('cancel', inputCancelHandler);

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
