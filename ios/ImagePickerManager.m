#import "ImagePickerManager.h"
#import "ImagePickerUtils.h"
#import <React/RCTConvert.h>
#import <AVFoundation/AVFoundation.h>
#import <Photos/Photos.h>
#import <PhotosUI/PhotosUI.h>

@import MobileCoreServices;

@interface ImagePickerManager ()

@property (nonatomic, strong) RCTResponseSenderBlock callback;
@property (nonatomic, copy) NSDictionary *options;

@end

@interface ImagePickerManager(PHPickerViewControllerDelegate) <PHPickerViewControllerDelegate>
@end

@implementation ImagePickerManager

NSString *errCameraUnavailable = @"camera_unavailable";
NSString *errPermission = @"permission";
NSString *errOthers = @"others";
RNImagePickerTarget target;

RCT_EXPORT_MODULE();

RCT_EXPORT_METHOD(launchCamera:(NSDictionary *)options callback:(RCTResponseSenderBlock)callback)
{
    target = camera;
    dispatch_async(dispatch_get_main_queue(), ^{
        [self launchImagePicker:options callback:callback];
    });
}

RCT_EXPORT_METHOD(launchImageLibrary:(NSDictionary *)options callback:(RCTResponseSenderBlock)callback)
{
    target = library;
    dispatch_async(dispatch_get_main_queue(), ^{
        [self launchImagePicker:options callback:callback];
    });
}

- (void)launchImagePicker:(NSDictionary *)options callback:(RCTResponseSenderBlock)callback
{
    self.callback = callback;
    
    if (target == camera && [ImagePickerUtils isSimulator]) {
        self.callback(@[@{@"errorCode": errCameraUnavailable}]);
        return;
    }
    
    self.options = options;

    if (@available(iOS 14, *)) {
        if (target == library) {
            PHPickerConfiguration *configuration = [ImagePickerUtils makeConfigurationFromOptions:options];
            PHPickerViewController *picker = [[PHPickerViewController alloc] initWithConfiguration:configuration];
            picker.delegate = self;

            [self showPickerViewController:picker];

            return;
        }
    }

    UIImagePickerController *picker = [[UIImagePickerController alloc] init];
    [ImagePickerUtils setupPickerFromOptions:picker options:self.options target:target];
    picker.delegate = self;

    [self checkPermission:^(BOOL granted) {
        if (!granted) {
            self.callback(@[@{@"errorCode": errPermission}]);
            return;
        }
        [self showPickerViewController:picker];
    }];
}

- (void) showPickerViewController:(UIViewController *)picker
{
    dispatch_async(dispatch_get_main_queue(), ^{
        UIViewController *root = RCTPresentedViewController();
        [root presentViewController:picker animated:YES completion:nil];
    });
}

- (void)imagePickerController:(UIImagePickerController *)picker didFinishPickingMediaWithInfo:(NSDictionary<NSString *,id> *)info
{
    dispatch_block_t dismissCompletionBlock = ^{
        if ([info[UIImagePickerControllerMediaType] isEqualToString:(NSString *) kUTTypeImage]) {
            [self onImageObtained:info];
        }
        else {
            [self onVideoObtained:info];
        }
    };

    dispatch_async(dispatch_get_main_queue(), ^{
        [picker dismissViewControllerAnimated:YES completion:dismissCompletionBlock];
    });
}

- (void)imagePickerControllerDidCancel:(UIImagePickerController *)picker
{
    dispatch_async(dispatch_get_main_queue(), ^{
        [picker dismissViewControllerAnimated:YES completion:^{
            self.callback(@[@{@"didCancel": @YES}]);
        }];
    });
}

- (void)onImageObtained:(NSDictionary<NSString *,id> *)info
{
    NSURL *imageURL = [ImagePickerManager getNSURLFromInfo:info];
    UIImage *image = [ImagePickerManager getUIImageFromInfo:info];

    if ((target == camera) && [self.options[@"saveToPhotos"] boolValue]) {
        UIImageWriteToSavedPhotosAlbum(image, nil, nil, nil);
    }

    NSString *fileType = [ImagePickerUtils getFileType:[NSData dataWithContentsOfURL:imageURL]];

    if (![fileType isEqualToString:@"gif"]) {
        image = [ImagePickerUtils resizeImage:image
                                     maxWidth:[self.options[@"maxWidth"] floatValue]
                                    maxHeight:[self.options[@"maxHeight"] floatValue]];
    }

    NSData *data;
    if ([fileType isEqualToString:@"jpg"]) {
        data = UIImageJPEGRepresentation(image, [self.options[@"quality"] floatValue]);
    }
    else if ([fileType isEqualToString:@"png"]) {
        data = UIImagePNGRepresentation(image);
    }
    else {
        data = [NSData dataWithContentsOfURL:imageURL];
    }

    NSDictionary *response = [self makeResponseFromImage:image fileType:fileType data:data];
    self.callback(@[response]);
}

- (NSDictionary *)makeResponseFromImage:(UIImage *)image fileType:(NSString *)fileType data:(NSData *)data
{
    NSMutableDictionary *response = [[NSMutableDictionary alloc] init];
    response[@"type"] = [@"image/" stringByAppendingString:fileType];

    NSString *fileName = [self getImageFileName:fileType];
    NSString *path = [[NSTemporaryDirectory() stringByStandardizingPath] stringByAppendingPathComponent:fileName];
    [data writeToFile:path atomically:YES];

    if ([self.options[@"includeBase64"] boolValue]) {
        response[@"base64"] = [data base64EncodedStringWithOptions:0];
    }

    NSURL *fileURL = [NSURL fileURLWithPath:path];
    response[@"uri"] = [fileURL absoluteString];

    NSNumber *fileSizeValue = nil;
    NSError *fileSizeError = nil;
    [fileURL getResourceValue:&fileSizeValue forKey:NSURLFileSizeKey error:&fileSizeError];
    if (fileSizeValue){
        response[@"fileSize"] = fileSizeValue;
    }

    response[@"fileName"] = fileName;
    response[@"width"] = @(image.size.width);
    response[@"height"] = @(image.size.height);
    return [response copy];
}

- (void)onVideoObtained:(NSDictionary<NSString *,id> *)info
{
    NSString *fileName = [info[UIImagePickerControllerMediaURL] lastPathComponent];
    NSString *path = [[NSTemporaryDirectory() stringByStandardizingPath] stringByAppendingPathComponent:fileName];

    NSURL *videoURL = info[UIImagePickerControllerMediaURL];
    NSURL *videoDestinationURL = [NSURL fileURLWithPath:path];

    if ((target == camera) && [self.options[@"saveToPhotos"] boolValue]) {
        UISaveVideoAtPathToSavedPhotosAlbum(videoURL.path, nil, nil, nil);
    }

    [self moveVideoFromInputURL:videoURL toTargetURL:videoDestinationURL];
    NSDictionary *response = @{@"uri": videoDestinationURL.absoluteString};

    self.callback(@[response]);
}

- (void)moveVideoFromInputURL:(NSURL *)url toTargetURL:(NSURL *)targetURL
{
    if (![url.URLByResolvingSymlinksInPath.path isEqualToString:targetURL.URLByResolvingSymlinksInPath.path]) {
        NSFileManager *fileManager = [NSFileManager defaultManager];

        // Delete file if it already exists
        if ([fileManager fileExistsAtPath:targetURL.path]) {
            [fileManager removeItemAtURL:targetURL error:nil];
        }

        if (url) { // Protect against reported crash
          NSError *error = nil;

          // If we have write access to the source file, move it. Otherwise use copy.
          if ([fileManager isWritableFileAtPath:[url path]]) {
            [fileManager moveItemAtURL:url toURL:targetURL error:&error];
          } else {
            [fileManager copyItemAtURL:url toURL:targetURL error:&error];
          }

          if (error) {
              self.callback(@[@{@"errorCode": errOthers, @"errorMessage":  error.localizedFailureReason}]);
              return;
          }
        }
    }
}

#pragma mark - Helpers

- (void)checkCameraPermissions:(void(^)(BOOL granted))callback
{
    AVAuthorizationStatus status = [AVCaptureDevice authorizationStatusForMediaType:AVMediaTypeVideo];
    if (status == AVAuthorizationStatusAuthorized) {
        callback(YES);
        return;
    }
    else if (status == AVAuthorizationStatusNotDetermined){
        [AVCaptureDevice requestAccessForMediaType:AVMediaTypeVideo completionHandler:^(BOOL granted) {
            callback(granted);
            return;
        }];
    }
    else {
        callback(NO);
    }
}

- (void)checkPhotosPermissions:(void(^)(BOOL granted))callback
{
    PHAuthorizationStatus status = [PHPhotoLibrary authorizationStatus];
    if (status == PHAuthorizationStatusAuthorized) {
        callback(YES);
        return;
    } else if (status == PHAuthorizationStatusNotDetermined) {
        [PHPhotoLibrary requestAuthorization:^(PHAuthorizationStatus status) {
            if (status == PHAuthorizationStatusAuthorized) {
                callback(YES);
                return;
            }
            else {
                callback(NO);
                return;
            }
        }];
    }
    else {
        callback(NO);
    }
}

// Both camera and photo write permission is required to take picture/video and store it to public photos
- (void)checkCameraAndPhotoPermission:(void(^)(BOOL granted))callback
{
    [self checkCameraPermissions:^(BOOL cameraGranted) {
        if (!cameraGranted) {
            callback(NO);
            return;
        }

        [self checkPhotosPermissions:^(BOOL photoGranted) {
            if (!photoGranted) {
                callback(NO);
                return;
            }
            callback(YES);
        }];
    }];
}

- (void)checkPermission:(void(^)(BOOL granted)) callback
{
    void (^permissionBlock)(BOOL) = ^(BOOL permissionGranted) {
        if (!permissionGranted) {
            callback(NO);
            return;
        }
        callback(YES);
    };

    if (target == camera && [self.options[@"saveToPhotos"] boolValue]) {
        [self checkCameraAndPhotoPermission:permissionBlock];
    }
    else if (target == camera) {
        [self checkCameraPermissions:permissionBlock];
    }
    else {
        if (@available(iOS 11.0, *)) {
            callback(YES);
        }
        else {
            [self checkPhotosPermissions:permissionBlock];
        }
    }
}

- (NSString *)getImageFileName:(NSString *)fileType
{
    NSString *fileName = [[NSUUID UUID] UUIDString];
    fileName = [fileName stringByAppendingString:@"."];
    return [fileName stringByAppendingString:fileType];
}

+ (UIImage *)getUIImageFromInfo:(NSDictionary *)info
{
    UIImage *image = info[UIImagePickerControllerEditedImage];
    if (!image) {
        image = info[UIImagePickerControllerOriginalImage];
    }
    return image;
}

+ (NSURL *)getNSURLFromInfo:(NSDictionary *)info {
    if (@available(iOS 11.0, *)) {
        return info[UIImagePickerControllerImageURL];
    }
    else {
        return info[UIImagePickerControllerReferenceURL];
    }
}

@end

@implementation ImagePickerManager(PHPickerViewControllerDelegate)

- (void)picker:(PHPickerViewController *)picker didFinishPicking:(NSArray<PHPickerResult *> *)results API_AVAILABLE(ios(14))
{
    if (results.count == 0) {
        dispatch_async(dispatch_get_main_queue(), ^{
            [picker dismissViewControllerAnimated:YES completion:^{
                self.callback(@[@{@"didCancel": @YES}]);
            }];
        });
        return;
    }

    // Immediately dismiss the controller to prevent calling the completion handler twice
    [picker dismissViewControllerAnimated:YES completion:nil];

    for (PHPickerResult *result in results) {
        NSItemProvider *provider = result.itemProvider;

        if ([provider canLoadObjectOfClass:[UIImage class]]) {
            NSString *typeIdentifier = [provider.registeredTypeIdentifiers firstObject];
            [provider loadObjectOfClass:[UIImage class]
                      completionHandler:^(__kindof id<NSItemProviderReading> _Nullable object, NSError * _Nullable error) {
                UIImage *image = object;
                NSDictionary *response = [self makeResponseFromImage:image typeIdentifier:typeIdentifier];

                dispatch_async(dispatch_get_main_queue(), ^{
                    self.callback(@[response]);
                });
            }];
        }
        else if ([provider hasItemConformingToTypeIdentifier:@"public.movie"]) {
            [provider loadFileRepresentationForTypeIdentifier:@"public.movie"
                                            completionHandler:^(NSURL * _Nullable url, NSError * _Nullable error) {
                NSDictionary *response = [self makeResponseFromURL:url];

                dispatch_async(dispatch_get_main_queue(), ^{
                    self.callback(@[response]);
                });
            }];
        }
    }
}

- (NSDictionary *)makeResponseFromImage:(UIImage *)image typeIdentifier:(NSString *)typeIdentifier {
    NSString *fileType = [self fileTypeFromTypeIdentifier:typeIdentifier];

    NSData *data;
    if ([fileType isEqualToString:@"jpg"]) {
        data = UIImageJPEGRepresentation(image, [self.options[@"quality"] floatValue]);
    }
    else {
        data = UIImagePNGRepresentation(image);
    }

    if (![fileType isEqualToString:@"gif"]) {
        image = [ImagePickerUtils resizeImage:image
                                     maxWidth:[self.options[@"maxWidth"] floatValue]
                                    maxHeight:[self.options[@"maxHeight"] floatValue]];
    }

    return [self makeResponseFromImage:image fileType:fileType data:data];
}

- (NSString *)fileTypeFromTypeIdentifier:(NSString *)typeIdentifier
{
    if ([typeIdentifier containsString:@"jpg"] || [typeIdentifier containsString:@"jpeg"]) {
        return @"jpg";
    }
    else if ([typeIdentifier containsString:@"gif"]) {
        return @"gif";
    }
    return @"png";
}

- (NSDictionary *)makeResponseFromURL:(NSURL *)url {
    NSMutableDictionary *response = [[NSMutableDictionary alloc] init];

    NSString *path = [[NSTemporaryDirectory() stringByStandardizingPath] stringByAppendingPathComponent:url.lastPathComponent];
    NSURL *targetURL = [NSURL fileURLWithPath:path];

    [self moveVideoFromInputURL:url toTargetURL:targetURL];

    response[@"uri"] = targetURL.absoluteString;

    return [response copy];
}

@end
