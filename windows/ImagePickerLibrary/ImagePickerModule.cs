using Newtonsoft.Json.Linq;
using ReactNative.Bridge;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Windows.ApplicationModel.Core;
using Windows.Media.Capture;
using Windows.Storage;
using Windows.Storage.FileProperties;
using Windows.Storage.Pickers;
using Windows.Storage.Streams;
using Windows.UI.Core;
using Windows.UI.Popups;
using Windows.UI.Xaml.Controls;

namespace ImagePicker
{
    public class ImagePickerModule : ReactContextNativeModuleBase
    {
        private const string MODULE_NAME = "ImagePickerManager";
        public ImagePickerModule(ReactContext reactContext) : base(reactContext)
        {
        }
        public override string Name
        {
            get
            {
                return MODULE_NAME;
            }
        }

        [ReactMethod]
        public async void showImagePicker(JObject options, ICallback callback)
        {
            List<String> titles = new List<string>();
            List<String> actions = new List<string>();
            bool cameraAvailable = await isCameraAvailable();

            if (options["takePhotoButtonTitle"] != null && options["takePhotoButtonTitle"].Value<string>().Length != 0
                && cameraAvailable)
            {
                titles.Add(options["takePhotoButtonTitle"].Value<string>());
                actions.Add("photo");
            }

            if (options["chooseFromLibraryButtonTitle"] != null && options["chooseFromLibraryButtonTitle"].Value<string>().Length != 0)
            {
                titles.Add(options["chooseFromLibraryButtonTitle"].Value<string>());
                actions.Add("library");
            }

            if (options["customButtons"] != null)
            {
                List<JToken> customButtons = options["customButtons"].ToList<JToken>();

                foreach (var button in customButtons)
                {
                    int currentIndex = titles.Count;
                    titles.Add(button["title"].ToString());
                    actions.Add(button["name"].ToString());
                }
            }

            if (options["cancelButtonTitle"] != null && options["cancelButtonTitle"].Value<string>().Length != 0)
            {
                titles.Add(options["cancelButtonTitle"].Value<string>());
                actions.Add("cancel");
            }

            RunOnDispatcher(async () => {
                var dialog = new OptionsModal();
                dialog.setTitle(options["title"].Value<string>() ?? "Title");
                for (var i = 0; i < titles.Count; ++i)
                {
                    dialog.addButton(new Button
                    {
                        Content = titles[i],
                        Tag = actions[i],
                    });
                }
                await dialog.ShowAsync();
                string result = dialog.Result;

                JObject response = new JObject();
                switch (result)
                {
                    case "photo":
                        launchCamera(options, callback);
                        break;
                    case "library":
                        launchImageLibrary(options, callback);
                        break;
                    case "cancel":
                        response["didCancel"] = true;
                        callback.Invoke(response);
                        break;
                    default:
                        response["customButton"] = result;
                        callback.Invoke(response);
                        break;
                }
            });
        }

        [ReactMethod]
        public async void launchCamera(JObject options, ICallback callback)
        {
            JObject response = new JObject();
            if (!await isCameraAvailable())
            {
                response["error"] = "Camera not available";
                callback.Invoke(response);
                return;
            }

            Dictionary<string, object> parsedOptions = parseOptions(options);

            RunOnDispatcher(async () =>
            {
                CameraCaptureUI _captureUI = new CameraCaptureUI();
                StorageFile data;
                if ((bool)parsedOptions["pickVideo"])
                {
                    _captureUI.VideoSettings.Format = CameraCaptureUIVideoFormat.Mp4;
                    if ((int)parsedOptions["videoDurationLimit"] > 0)
                    {
                        _captureUI.VideoSettings.MaxDurationInSeconds = (int)parsedOptions["videoDurationLimit"];
                    }
                    data = await _captureUI.CaptureFileAsync(CameraCaptureUIMode.Video); // TODO: Enable mixed mode
                }
                else
                {
                    _captureUI.PhotoSettings.Format = CameraCaptureUIPhotoFormat.Jpeg;
                    data = await _captureUI.CaptureFileAsync(CameraCaptureUIMode.Photo);
                }

                string base64Data = "";
                if (data != null)
                {
                    IRandomAccessStream photoStream = await data.OpenAsync(FileAccessMode.Read);
                    DataReader reader = new DataReader(photoStream.GetInputStreamAt(0));
                    await reader.LoadAsync((uint)photoStream.Size);
                    byte[] byteArray = new byte[photoStream.Size];
                    reader.ReadBytes(byteArray);
                    base64Data = Convert.ToBase64String(byteArray);
                    if (!(bool)parsedOptions["noData"])
                    {
                        response["data"] = base64Data;
                    }
                    response["uri"] = data.Path;
                    if (!(bool)parsedOptions["tmpImage"])
                    {
                        try
                        {
                            StorageFolder picturesLibrary = KnownFolders.PicturesLibrary;
                            StorageFile imageFile = await picturesLibrary.CreateFileAsync(data.Name, CreationCollisionOption.GenerateUniqueName);
                        }
                        catch (Exception e)
                        {
                            response["error"] = "Error when saving file " + e.ToString();
                        }
                    }
                    callback.Invoke(response);
                }
                else
                {
                    response["didCancel"] = true;
                }
            });
        }

        private async Task<string> convertToBase64(StorageFile image)
        {
            IRandomAccessStream photoStream = await image.OpenAsync(FileAccessMode.Read);
            DataReader reader = new DataReader(photoStream.GetInputStreamAt(0));
            await reader.LoadAsync((uint)photoStream.Size);
            byte[] byteArray = new byte[photoStream.Size];
            reader.ReadBytes(byteArray);
            return Convert.ToBase64String(byteArray);
        }

        private async void processResult(StorageFile file, JObject response, ICallback callback)
        {
            ImageProperties properties = await file.Properties.GetImagePropertiesAsync();

            response["longitude"] = properties.Longitude;
            response["latitude"] = properties.Latitude;
            string timestamp = properties.DateTaken.DateTime.ToString("s");
            response["timestamp"] = timestamp;

            var orientation = properties.Orientation;
            bool isVertical = true;
            int currentRotation = 0;
            switch (orientation)
            {
                case PhotoOrientation.Rotate270:
                    isVertical = false;
                    currentRotation = 270;
                    break;
                case PhotoOrientation.Rotate90:
                    isVertical = false;
                    currentRotation = 90;
                    break;
                case PhotoOrientation.Rotate180:
                    isVertical = true;
                    currentRotation = 180;
                    break;
                case PhotoOrientation.Normal:
                    isVertical = true;
                    currentRotation = 0;
                    break;
            }

            response["originalRotation"] = currentRotation;
            response["isVertical"] = isVertical;
            response["initialWidth"] = properties.Width;
            response["initialHeight"] = properties.Height;

            string base64Image = await convertToBase64(file);
            response["data"] = base64Image; // TODO: Return URI
            response["uri"] = file.Path;
            callback.Invoke(response);
        }

        [ReactMethod]
        private void launchImageLibrary(JObject options, ICallback callback)
        {
            JObject response = new JObject();
            // https://blog.kulman.sk/choosing-an-image-from-gallery-or-camera-in-uwp/
            var openPicker = new FileOpenPicker
            {
                ViewMode = PickerViewMode.Thumbnail,
                SuggestedStartLocation = PickerLocationId.PicturesLibrary
            };
            openPicker.FileTypeFilter.Add(".jpg");
            RunOnDispatcher(async () =>
            {
                StorageFile file = await openPicker.PickSingleFileAsync();
                if (file != null)
                {
                    processResult(file, response, callback);
                }
                else
                {
                    response["didCancel"] = true;
                    callback.Invoke(response);
                }
            });
        }

        private Dictionary<string, object> parseOptions(JObject options)
        {
            var parsedOptions = new Dictionary<string, object>();
            parsedOptions["noData"] = false;
            if (options["noData"] != null)
            {
                parsedOptions["noData"] = options["noData"].Value<bool>();
            }
            parsedOptions["maxWidth"] = 0;
            if (options["maxWidth"] != null)
            {
                parsedOptions["maxWidth"] = options["maxWidth"].Value<int>();
            }

            parsedOptions["maxHeight"] = 0;
            if (options["maxHeight"] != null)
            {
                parsedOptions["maxHeight"] = options["maxHeight"].Value<int>();
            }

            parsedOptions["quality"] = 100;
            if (options["quality"] != null)
            {
                parsedOptions["quality"] = (int)(options["quality"].Value<double>() * 100);
            }
            parsedOptions["tmpImage"] = true;
            if (options["storageOptions"] != null)
            {
                parsedOptions["tmpImage"] = false;
            }

            parsedOptions["rotation"] = 0;
            if (options["rotation"] != null)
            {
                parsedOptions["rotation"] = options["rotation"].Value<int>();
            }

            parsedOptions["pickVideo"] = false;
            if (options["mediaType"] != null && options["mediaType"].Value<string>().Equals("video"))
            {
                parsedOptions["pickVideo"] = true;
            }

            parsedOptions["videoQuality"] = 1;
            if (options["videoQuality"] != null && options["videoQuality"].Value<string>().Equals("low"))
            {
                parsedOptions["videoQuality"] = 0;
            }

            parsedOptions["videoDurationLimit"] = 0;
            if (options["durationLimit"] != null)
            {
                parsedOptions["videoDurationLimit"] = options["durationLimit"].Value<int>();
            }
            return parsedOptions;
        }
        // TODO: parseTimestamp
        // TODO: permissionsCheck
        // TODO: getRealPathFromURI
        // TODO: createFileFromURI
        // TODO: getBase64StringFromFile (maybe not necessary)
        // TODO: getResizedImage
        // TODO: createNewFile
        // TODO: putExtraFileInfo
        // TODO: fileScan (maybe not necessary)

        private async Task<bool> isCameraAvailable()
        {
            // http://stackoverflow.com/a/30810640/2628463
            var devices = await Windows.Devices.Enumeration.DeviceInformation.FindAllAsync(Windows.Devices.Enumeration.DeviceClass.VideoCapture);
            return devices.Count > 0;
        }

        private static async void RunOnDispatcher(DispatchedHandler action)
        {
            await CoreApplication.MainView.CoreWindow.Dispatcher.RunAsync(CoreDispatcherPriority.Normal, action).AsTask().ConfigureAwait(false);
        }
    }
}
