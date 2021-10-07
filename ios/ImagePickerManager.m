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
@property (nonatomic, copy) NSArray *include;

@end

@interface ImagePickerManager (UIImagePickerControllerDelegate) <UINavigationControllerDelegate, UIImagePickerControllerDelegate>
@end

@interface ImagePickerManager (UIAdaptivePresentationControllerDelegate) <UIAdaptivePresentationControllerDelegate>
@end

#if __has_include(<PhotosUI/PHPicker.h>)
@interface ImagePickerManager (PHPickerViewControllerDelegate) <PHPickerViewControllerDelegate>
@end
#endif

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
    self.include = [self.options objectForKey:@"include"];

#if __has_include(<PhotosUI/PHPicker.h>)
    if (@available(iOS 14, *)) {
        if (target == library) {
            PHPickerConfiguration *configuration = [ImagePickerUtils makeConfigurationFromOptions:options target:target];
            PHPickerViewController *picker = [[PHPickerViewController alloc] initWithConfiguration:configuration];
            picker.delegate = self;
            picker.presentationController.delegate = self;

            [self showPickerViewController:picker];
            return;
        }
    }
#endif
    
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

#pragma mark - Helpers

-(NSMutableDictionary *)mapImageToAsset:(NSURL *)url error:(NSError **)error {
    NSMutableDictionary *asset = [[NSMutableDictionary alloc] init];
    
    NSData *data = [[NSData alloc] initWithContentsOfURL:url];
    UIImage *image = [UIImage imageWithData:data];
    if ((target == camera) && [self.options[@"saveToPhotos"] boolValue]) {
        UIImageWriteToSavedPhotosAlbum(image, nil, nil, nil);
        
        if ([self.include containsObject:@"uri"]) {
            asset[@"uri"] = [url absoluteString];
        }
    }
    
    NSString *fileType;
    if (
        (![self.options[@"originalUri"] boolValue]) ||
        ([self.include containsObject:@"type"]) ||
        ([self.include containsObject:@"fileName"])
        ) {
        fileType = [ImagePickerUtils getFileType:data];
    }
    
    NSString *fileName;
    if(
        ([self.include containsObject:@"type"]) ||
        ([self.include containsObject:@"fileName"])
        ) {
        fileName = [self getImageFileName:fileType];
    }

    if (target == library) {
        if (![self.options[@"originalUri"] boolValue]) {
            if (![fileType isEqualToString:@"gif"]) {
                image = [ImagePickerUtils resizeImage:image
                                             maxWidth:[self.options[@"maxWidth"] floatValue]
                                            maxHeight:[self.options[@"maxHeight"] floatValue]];
            }

            if ([fileType isEqualToString:@"jpg"]) {
                data = UIImageJPEGRepresentation(image, [self.options[@"quality"] floatValue]);
            } else if ([fileType isEqualToString:@"png"]) {
                data = UIImagePNGRepresentation(image);
            }
            
            NSString *path = [[NSTemporaryDirectory() stringByStandardizingPath] stringByAppendingPathComponent:fileName];
            [data writeToFile:path atomically:YES];
            
            if ([self.include containsObject:@"base64"]) {
                asset[@"base64"] = [data base64EncodedStringWithOptions:0];
            }
            
            if ([self.include containsObject:@"uri"]) {
                NSURL *fileURL = [NSURL fileURLWithPath:path];
                asset[@"uri"] = [fileURL absoluteString];
            }
        } else {
            if ([self.include containsObject:@"uri"]) {
                asset[@"uri"] = [url absoluteString];
            }
        }
    }
    
    if ([self.include containsObject:@"type"]) {
        asset[@"type"] = [@"image/" stringByAppendingString:fileType];
    }
    if ([self.include containsObject:@"fileSize"]) {
        NSNumber *fileSizeValue = nil;
        NSError *fileSizeError = nil;
        [url getResourceValue:&fileSizeValue forKey:NSURLFileSizeKey error:&fileSizeError];
        if (fileSizeValue){
            asset[@"fileSize"] = fileSizeValue;
        }
    }
    if ([self.include containsObject:@"fileName"]) {
        asset[@"fileName"] = fileName;
    }
    if ([self.include containsObject:@"width"]) {
        asset[@"width"] = @(image.size.width);
    }
    if ([self.include containsObject:@"height"]) {
        asset[@"height"] = @(image.size.height);
    }

    return asset;
}

-(NSMutableDictionary *)mapVideoToAsset:(NSURL *)url error:(NSError **)error {
    NSMutableDictionary *asset = [[NSMutableDictionary alloc] init];
    
    NSString *fileName = [url lastPathComponent];
    NSString *path = [[NSTemporaryDirectory() stringByStandardizingPath] stringByAppendingPathComponent:fileName];
    NSURL *videoDestinationURL = [NSURL fileURLWithPath:path];

    if ((target == camera) && [self.options[@"saveToPhotos"] boolValue]) {
        UISaveVideoAtPathToSavedPhotosAlbum(url.path, nil, nil, nil);
    }
    
    if(![self.options[@"originalUri"] boolValue]) {
        if (![url.URLByResolvingSymlinksInPath.path isEqualToString:videoDestinationURL.URLByResolvingSymlinksInPath.path]) {
            NSFileManager *fileManager = [NSFileManager defaultManager];
            
            // Delete file if it already exists
            if ([fileManager fileExistsAtPath:videoDestinationURL.path]) {
                [fileManager removeItemAtURL:videoDestinationURL error:nil];
            }

            if (url) { // Protect against reported crash

                // If we have write access to the source file, move it. Otherwise use copy.
                if ([fileManager isWritableFileAtPath:[url path]]) {
                    [fileManager moveItemAtURL:url toURL:videoDestinationURL error:error];
                } else {
                    [fileManager copyItemAtURL:url toURL:videoDestinationURL error:error];
                }

                if (error) {
                    return nil;
                }
            }
        }
        
        if ([self.include containsObject:@"uri"]) {
            asset[@"uri"] = videoDestinationURL.absoluteString;
        }
    } else {
        if ([self.include containsObject:@"uri"]) {
            asset[@"uri"] = [url absoluteString];
        }
    }
    
    if ([self.include containsObject:@"duration"]) {
        asset[@"duration"] = @(roundf(CMTimeGetSeconds([AVAsset assetWithURL:videoDestinationURL].duration)));
    }
    
    if ([self.include containsObject:@"type"]) {
        asset[@"type"] = [ImagePickerUtils getFileTypeFromUrl:videoDestinationURL];
    }
    
    return asset;
}

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

@implementation ImagePickerManager (UIImagePickerControllerDelegate)

- (void)imagePickerController:(UIImagePickerController *)picker didFinishPickingMediaWithInfo:(NSDictionary<NSString *,id> *)info
{
    dispatch_block_t dismissCompletionBlock = ^{
        NSMutableArray<NSDictionary *> *assets = [[NSMutableArray alloc] initWithCapacity:1];

        if ([info[UIImagePickerControllerMediaType] isEqualToString:(NSString *) kUTTypeImage]) {
            [assets addObject:[self mapImageToAsset:[ImagePickerManager getNSURLFromInfo:info] error:nil]];
        } else {
            NSError *error;
            NSDictionary *asset = [self mapVideoToAsset:info[UIImagePickerControllerMediaURL] error:&error];
            if (asset == nil) {
                self.callback(@[@{@"errorCode": errOthers, @"errorMessage":  error.localizedFailureReason}]);
                return;
            }
            [assets addObject:asset];
        }

        NSMutableDictionary *response = [[NSMutableDictionary alloc] init];
        response[@"assets"] = assets;
        self.callback(@[response]);
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

@end

@implementation ImagePickerManager (presentationControllerDidDismiss)

- (void)presentationControllerDidDismiss:(UIPresentationController *)presentationController
{
    self.callback(@[@{@"didCancel": @YES}]);
}

@end

#if __has_include(<PhotosUI/PHPicker.h>)
@implementation ImagePickerManager (PHPickerViewControllerDelegate)

- (void)picker:(PHPickerViewController *)picker didFinishPicking:(NSArray<PHPickerResult *> *)results API_AVAILABLE(ios(14))
{
    [picker dismissViewControllerAnimated:YES completion:nil];

    if (results.count == 0) {
        dispatch_async(dispatch_get_main_queue(), ^{
            self.callback(@[@{@"didCancel": @YES}]);
        });
        return;
    }

    dispatch_group_t completionGroup = dispatch_group_create();
    NSMutableArray<NSDictionary *> *assets = [[NSMutableArray alloc] initWithCapacity:results.count];

    for (PHPickerResult *result in results) {
        NSItemProvider *provider = result.itemProvider;
        dispatch_group_enter(completionGroup);

        if ([provider hasItemConformingToTypeIdentifier:(NSString *)kUTTypeImage]) {
            [provider loadFileRepresentationForTypeIdentifier:(NSString *)kUTTypeImage completionHandler:^(NSURL * _Nullable url, NSError * _Nullable error) {
                [assets addObject:[self mapImageToAsset:url error:nil]];
                dispatch_group_leave(completionGroup);
            }];
        }

        if ([provider hasItemConformingToTypeIdentifier:(NSString *)kUTTypeMovie]) {
            [provider loadFileRepresentationForTypeIdentifier:(NSString *)kUTTypeMovie completionHandler:^(NSURL * _Nullable url, NSError * _Nullable error) {
                [assets addObject:[self mapVideoToAsset:url error:nil]];
                dispatch_group_leave(completionGroup);
            }];
        }
    }

    dispatch_group_notify(completionGroup, dispatch_get_main_queue(), ^{
        //  mapVideoToAsset can fail and return nil.
        for (NSDictionary *asset in assets) {
            if (nil == asset) {
                self.callback(@[@{@"errorCode": errOthers}]);
                return;
            }
        }

        NSMutableDictionary *response = [[NSMutableDictionary alloc] init];
        [response setObject:assets forKey:@"assets"];

        self.callback(@[response]);
    });
}

@end
#endif
