#import "UniformTypeIdentifiers/UniformTypeIdentifiers.h"
#import "ImagePickerManager.h"
#import "ImagePickerUtils.h"
#import <React/RCTConvert.h>
#import <AVFoundation/AVFoundation.h>
#import <ImageIO/ImageIO.h>
#import <Photos/Photos.h>
#import <PhotosUI/PhotosUI.h>
#import <MobileCoreServices/MobileCoreServices.h>

@interface ImagePickerManager ()

@property (nonatomic, strong) RCTResponseSenderBlock callback;
@property (nonatomic, copy) NSDictionary *options;

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

BOOL photoSelected = NO;

RCT_EXPORT_MODULE(ImagePicker)

RCT_EXPORT_METHOD(launchCamera:(NSDictionary *)options callback:(RCTResponseSenderBlock)callback)
{
    target = camera;
    photoSelected = NO;
    dispatch_async(dispatch_get_main_queue(), ^{
        [self launchImagePicker:options callback:callback];
    });
}

RCT_EXPORT_METHOD(launchImageLibrary:(NSDictionary *)options callback:(RCTResponseSenderBlock)callback)
{
    target = library;
    photoSelected = NO;
    dispatch_async(dispatch_get_main_queue(), ^{
        [self launchImagePicker:options callback:callback];
    });
}

// We won't compile this code when we build for the old architecture.
#ifdef RCT_NEW_ARCH_ENABLED

- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
    return std::make_shared<facebook::react::NativeImagePickerSpecJSI>(params);
}
#endif

- (void)launchImagePicker:(NSDictionary *)options callback:(RCTResponseSenderBlock)callback
{
    self.callback = callback;

    if (target == camera && [ImagePickerUtils isSimulator]) {
        self.callback(@[@{@"errorCode": errCameraUnavailable}]);
        return;
    }

    self.options = options;

#if __has_include(<PhotosUI/PHPicker.h>)
    if (@available(iOS 14, *)) {
        if (target == library) {
            PHPickerConfiguration *configuration = [ImagePickerUtils makeConfigurationFromOptions:options target:target];
            PHPickerViewController *picker = [[PHPickerViewController alloc] initWithConfiguration:configuration];
            picker.delegate = self;
            picker.modalPresentationStyle = [RCTConvert UIModalPresentationStyle:options[@"presentationStyle"]];
            picker.presentationController.delegate = self;

            if([self.options[@"includeExtra"] boolValue]) {

                [self checkPhotosPermissions:^(BOOL granted) {
                    if (!granted) {
                        self.callback(@[@{@"errorCode": errPermission}]);
                        return;
                    }
                    [self showPickerViewController:picker];
                }];
            } else {
                [self showPickerViewController:picker];
            }

            return;
        }
    }
#endif
    UIImagePickerController *picker = [[UIImagePickerController alloc] init];
    [ImagePickerUtils setupPickerFromOptions:picker options:self.options target:target];
    picker.delegate = self;
    picker.presentationController.delegate = self;
    
    if([self.options[@"includeExtra"] boolValue]) {
        [self checkPhotosPermissions:^(BOOL granted) {
            if (!granted) {
                self.callback(@[@{@"errorCode": errPermission}]);
                return;
            }
            [self showPickerViewController:picker];
        }];
    } else {
      [self showPickerViewController:picker];
    }
}

- (void) showPickerViewController:(UIViewController *)picker
{
    dispatch_async(dispatch_get_main_queue(), ^{
        UIViewController *root = RCTPresentedViewController();
        [root presentViewController:picker animated:YES completion:nil];
    });
}

#pragma mark - Helpers

NSData* extractImageData(UIImage* image){
    CFMutableDataRef imageData = CFDataCreateMutable(NULL, 0);
    CGImageDestinationRef destination = CGImageDestinationCreateWithData(imageData, kUTTypeJPEG, 1, NULL);

    CFStringRef orientationKey[1];
    CFTypeRef   orientationValue[1];
    CGImagePropertyOrientation CGOrientation = CGImagePropertyOrientationForUIImageOrientation(image.imageOrientation);

    orientationKey[0] = kCGImagePropertyOrientation;
    orientationValue[0] = CFNumberCreate(NULL, kCFNumberIntType, &CGOrientation);

    CFDictionaryRef imageProps = CFDictionaryCreate( NULL, (const void **)orientationKey, (const void **)orientationValue, 1,
                    &kCFTypeDictionaryKeyCallBacks, &kCFTypeDictionaryValueCallBacks);

    CGImageDestinationAddImage(destination, image.CGImage, imageProps);

    CGImageDestinationFinalize(destination);

    CFRelease(destination);
    CFRelease(orientationValue[0]);
    CFRelease(imageProps);
    return (__bridge NSData *)imageData;
}



-(NSMutableDictionary *)mapImageToAsset:(UIImage *)image data:(NSData *)data phAsset:(PHAsset * _Nullable)phAsset suggestedName:(NSString * _Nullable)suggestedName {
    NSString *fileType = [ImagePickerUtils getFileType:data];
    if (target == camera) {
        if ([self.options[@"saveToPhotos"] boolValue]) {
            UIImageWriteToSavedPhotosAlbum(image, nil, nil, nil);
        }
        data = extractImageData(image);
    }

    UIImage* newImage = image;
    if (![fileType isEqualToString:@"gif"]) {
        newImage = [ImagePickerUtils resizeImage:image
                                     maxWidth:[self.options[@"maxWidth"] floatValue]
                                    maxHeight:[self.options[@"maxHeight"] floatValue]];
    }

    NSMutableDictionary *asset = [[NSMutableDictionary alloc] init];
    NSDictionary *exifData = getExifDataFromImage(data);
    if (exifData) {
        asset[@"exif"] = exifData;
    } else {
        asset[@"exif"] = @{};
    }

    float quality = [self.options[@"quality"] floatValue];
    if (![image isEqual:newImage] || (quality >= 0 && quality < 1)) {
        if ([fileType isEqualToString:@"jpg"]) {
            data = UIImageJPEGRepresentation(newImage, quality);
        } else if ([fileType isEqualToString:@"png"]) {
            data = UIImagePNGRepresentation(newImage);
        }
    }

    asset[@"type"] = [@"image/" stringByAppendingString:fileType];

    NSString *fileName = [self getImageFileNameFrom:phAsset ForType:fileType suggestedName:suggestedName];
    NSString *path = [[NSTemporaryDirectory() stringByStandardizingPath] stringByAppendingPathComponent:fileName];
    [data writeToFile:path atomically:YES];

    if ([self.options[@"includeBase64"] boolValue]) {
        asset[@"base64"] = [data base64EncodedStringWithOptions:0];
    }

    NSURL *fileURL = [NSURL fileURLWithPath:path];
    asset[@"uri"] = [fileURL absoluteString];

    NSNumber *fileSizeValue = nil;
    NSError *fileSizeError = nil;
    [fileURL getResourceValue:&fileSizeValue forKey:NSURLFileSizeKey error:&fileSizeError];
    if (fileSizeValue){
        asset[@"fileSize"] = fileSizeValue;
    }

    asset[@"fileName"] = fileName;
    asset[@"width"] = @(newImage.size.width);
    asset[@"height"] = @(newImage.size.height);

    if(phAsset){
        asset[@"timestamp"] = [self getDateTimeInUTC:phAsset.creationDate];
        asset[@"id"] = phAsset.localIdentifier;
    } else {
        NSString *timestamp = [self timestampFromExifDictionary:exifData];
        if (timestamp) {
            asset[@"timestamp"] = timestamp;
        } else {
            NSURL *writtenFileURL = [NSURL fileURLWithPath:path];
            NSDate *modDate = nil;
            [writtenFileURL getResourceValue:&modDate forKey:NSURLContentModificationDateKey error:nil];
            if (modDate) {
                asset[@"timestamp"] = [self getDateTimeInUTC:modDate];
            }
        }
    }

    return asset;
}

- (NSString *)timestampFromExifDictionary:(NSDictionary *)properties {
    if (!properties || properties.count == 0) return nil;
    NSString *dateString = nil;
    NSDictionary *exifDict = properties[(__bridge NSString *)kCGImagePropertyExifDictionary];
    if (exifDict) {
        dateString = exifDict[(__bridge NSString *)kCGImagePropertyExifDateTimeOriginal];
        if (!dateString.length) dateString = exifDict[(__bridge NSString *)kCGImagePropertyExifDateTimeDigitized];
    }
    if (!dateString.length) {
        NSDictionary *tiffDict = properties[(__bridge NSString *)kCGImagePropertyTIFFDictionary];
        if (tiffDict) dateString = tiffDict[(__bridge NSString *)kCGImagePropertyTIFFDateTime];
    }
    if (!dateString.length) return nil;
    NSDateFormatter *parser = [[NSDateFormatter alloc] init];
    [parser setDateFormat:@"yyyy:MM:dd HH:mm:ss"];
    NSDate *date = [parser dateFromString:dateString];
    return date ? [self getDateTimeInUTC:date] : nil;
}

NSDictionary *getExifDataFromImage(NSData *data) {
    CGImageSourceRef imageSource = CGImageSourceCreateWithData((CFDataRef)data, NULL);
    if (!imageSource) {
        return nil;
    }

    NSDictionary *exifDictionary = (NSDictionary *) CFBridgingRelease(CGImageSourceCopyPropertiesAtIndex(imageSource, 0, NULL));
    if (!exifDictionary) {
        CFRelease(imageSource);
        return nil;
    }

    CFRelease(imageSource);

    return exifDictionary;
}

CGImagePropertyOrientation CGImagePropertyOrientationForUIImageOrientation(UIImageOrientation uiOrientation) {
    //code from here: https://developer.apple.com/documentation/imageio/cgimagepropertyorientation?language=objc
    switch (uiOrientation) {
        case UIImageOrientationUp: return kCGImagePropertyOrientationUp;
        case UIImageOrientationDown: return kCGImagePropertyOrientationDown;
        case UIImageOrientationLeft: return kCGImagePropertyOrientationLeft;
        case UIImageOrientationRight: return kCGImagePropertyOrientationRight;
        case UIImageOrientationUpMirrored: return kCGImagePropertyOrientationUpMirrored;
        case UIImageOrientationDownMirrored: return kCGImagePropertyOrientationDownMirrored;
        case UIImageOrientationLeftMirrored: return kCGImagePropertyOrientationLeftMirrored;
        case UIImageOrientationRightMirrored: return kCGImagePropertyOrientationRightMirrored;
    }
}

-(NSMutableDictionary *)mapVideoToAsset:(NSURL *)url phAsset:(PHAsset * _Nullable)phAsset error:(NSError **)error {
    // Resolve original filename from PHAsset resources; temp URL name is a system-generated UUID
    NSString *fileName = nil;
    if (phAsset) {
        NSArray<PHAssetResource *> *resources = [PHAssetResource assetResourcesForAsset:phAsset];
        NSArray<NSNumber *> *preferredTypes = @[
            @(PHAssetResourceTypeVideo),
            @(PHAssetResourceTypeFullSizeVideo),
            @(PHAssetResourceTypePairedVideo)
        ];
        for (NSNumber *typeNum in preferredTypes) {
            PHAssetResourceType preferredType = (PHAssetResourceType)[typeNum integerValue];
            for (PHAssetResource *resource in resources) {
                if (resource.type == preferredType) {
                    fileName = resource.originalFilename;
                    break;
                }
            }
            if (fileName) break;
        }
    }
    if (!fileName) fileName = [url lastPathComponent];
    NSString *path = [[NSTemporaryDirectory() stringByStandardizingPath] stringByAppendingPathComponent:fileName];
    NSURL *videoDestinationURL = [NSURL fileURLWithPath:path];
    NSString *fileExtension = [fileName pathExtension];

    if ((target == camera) && [self.options[@"saveToPhotos"] boolValue]) {
        UISaveVideoAtPathToSavedPhotosAlbum(url.path, nil, nil, nil);
    }

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

          if (error && *error) {
              return nil;
          }
        }
    }

    NSMutableDictionary *response = [[NSMutableDictionary alloc] init];

    if([self.options[@"formatAsMp4"] boolValue] && ![fileExtension isEqualToString:@"mp4"]) {
        NSURL *parentURL = [videoDestinationURL URLByDeletingLastPathComponent];
        NSString *path = [[parentURL.path stringByAppendingString:@"/"] stringByAppendingString:[[NSUUID UUID] UUIDString]];
        path = [path stringByAppendingString:@".mp4"];
        NSURL *outputURL = [NSURL fileURLWithPath:path];

        [[NSFileManager defaultManager] removeItemAtURL:outputURL error:nil];
        AVURLAsset *asset = [AVURLAsset URLAssetWithURL:videoDestinationURL options:nil];
        AVAssetExportSession *exportSession = [[AVAssetExportSession alloc] initWithAsset:asset presetName:AVAssetExportPresetPassthrough];

        exportSession.outputURL = outputURL;
        exportSession.outputFileType = AVFileTypeMPEG4;
        exportSession.shouldOptimizeForNetworkUse = YES;

        dispatch_semaphore_t sem = dispatch_semaphore_create(0);

        [exportSession exportAsynchronouslyWithCompletionHandler:^(void) {
            if (exportSession.status == AVAssetExportSessionStatusCompleted) {
                CGSize dimentions = [ImagePickerUtils getVideoDimensionsFromUrl:outputURL];
                response[@"fileName"] = [outputURL lastPathComponent];
                response[@"duration"] = [NSNumber numberWithDouble:CMTimeGetSeconds([AVAsset assetWithURL:outputURL].duration)];
                response[@"uri"] = outputURL.absoluteString;
                response[@"type"] = [ImagePickerUtils getFileTypeFromUrl:outputURL];
                response[@"fileSize"] = [ImagePickerUtils getFileSizeFromUrl:outputURL];
                response[@"width"] = @(dimentions.width);
                response[@"height"] = @(dimentions.height);
                if (!phAsset) {
                    NSString *timestamp = [self timestampFromVideoURL:outputURL];
                    if (timestamp) response[@"timestamp"] = timestamp;
                }

                dispatch_semaphore_signal(sem);
            } else if (exportSession.status == AVAssetExportSessionStatusFailed || exportSession.status == AVAssetExportSessionStatusCancelled) {
                dispatch_semaphore_signal(sem);
            }
        }];


        dispatch_semaphore_wait(sem, DISPATCH_TIME_FOREVER);
    } else {
        CGSize dimentions = [ImagePickerUtils getVideoDimensionsFromUrl:videoDestinationURL];
        response[@"fileName"] = fileName;
        response[@"duration"] = [NSNumber numberWithDouble:CMTimeGetSeconds([AVAsset assetWithURL:videoDestinationURL].duration)];
        response[@"uri"] = videoDestinationURL.absoluteString;
        response[@"type"] = [ImagePickerUtils getFileTypeFromUrl:videoDestinationURL];
        response[@"fileSize"] = [ImagePickerUtils getFileSizeFromUrl:videoDestinationURL];
        response[@"width"] = @(dimentions.width);
        response[@"height"] = @(dimentions.height);

        if(phAsset){
            response[@"timestamp"] = [self getDateTimeInUTC:phAsset.creationDate];
            response[@"id"] = phAsset.localIdentifier;
        } else {
            NSString *timestamp = [self timestampFromVideoURL:videoDestinationURL];
            if (timestamp) {
                response[@"timestamp"] = timestamp;
            }
        }
    }

    return response;
}

- (NSString *)timestampFromVideoURL:(NSURL *)url {
    if (!url) return nil;
    AVURLAsset *asset = [AVURLAsset URLAssetWithURL:url options:nil];
    for (AVMetadataItem *item in asset.commonMetadata) {
        if ([item.commonKey isEqual:AVMetadataCommonKeyCreationDate]) {
            NSDate *date = item.dateValue;
            if (date) return [self getDateTimeInUTC:date];
        }
    }
    return nil;
}

- (NSString *) getDateTimeInUTC:(NSDate *)date {
    NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
    [formatter setDateFormat:@"yyyy-MM-dd'T'HH:mm:ss.SSSZ"];
    return [formatter stringFromDate:date];
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

- (NSString *)getImageFileNameFrom:(PHAsset * _Nullable)phAsset ForType:(NSString *)fileType suggestedName:(NSString * _Nullable)suggestedName
{
    if (phAsset) {
        NSArray<PHAssetResource *> *resources = [PHAssetResource assetResourcesForAsset:phAsset];
        PHAssetResource *photoResource = nil;
        for (PHAssetResource *resource in resources) {
            if (resource.type == PHAssetResourceTypePhoto) {
                photoResource = resource;
                break;
            }
        }
        if (!photoResource) photoResource = resources.firstObject;
        if (photoResource) {
            NSString *base = [photoResource.originalFilename stringByDeletingPathExtension];
            return [base stringByAppendingPathExtension:fileType];
        }
    }
    // Fallback: use suggestedName from NSItemProvider when PHAsset is unavailable (limited access)
    if (suggestedName && suggestedName.length > 0) {
        return [suggestedName stringByAppendingPathExtension:fileType];
    }
    return [[[NSUUID UUID] UUIDString] stringByAppendingPathExtension:fileType];
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
        PHAsset *asset = nil;

        if (photoSelected == YES) {
           return;
        }
        photoSelected = YES;

        // If include extra, we fetch the PHAsset, this required library permissions
        if([self.options[@"includeExtra"] boolValue]) {
          asset = [ImagePickerUtils fetchPHAssetOnIOS13:info];
        }

        if ([info[UIImagePickerControllerMediaType] isEqualToString:(NSString *) kUTTypeImage]) {
            UIImage *image = [ImagePickerManager getUIImageFromInfo:info];

            [assets addObject:[self mapImageToAsset:image data:[NSData dataWithContentsOfURL:[ImagePickerManager getNSURLFromInfo:info]] phAsset:asset suggestedName:nil]];
        } else {
            NSError *error;
            NSDictionary *videoAsset = [self mapVideoToAsset:info[UIImagePickerControllerMediaURL] phAsset:asset error:&error];

            if (videoAsset == nil) {
                NSString *errorMessage = error.localizedFailureReason;
                if (errorMessage == nil) errorMessage = @"Video asset not found";
                self.callback(@[@{@"errorCode": errOthers, @"errorMessage": errorMessage}]);
                return;
            }
            [assets addObject:videoAsset];
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

    if (photoSelected == YES) {
        return;
    }
    photoSelected = YES;

    if (results.count == 0) {
        dispatch_async(dispatch_get_main_queue(), ^{
            self.callback(@[@{@"didCancel": @YES}]);
        });
        return;
    }

    dispatch_group_t completionGroup = dispatch_group_create();
    NSMutableArray<NSDictionary *> *assets = [[NSMutableArray alloc] initWithCapacity:results.count];
    for (int i = 0; i < results.count; i++) {
        [assets addObject:(NSDictionary *)[NSNull null]];
    }

    [results enumerateObjectsUsingBlock:^(PHPickerResult *result, NSUInteger index, BOOL *stop) {
        PHAsset *asset = nil;
        NSItemProvider *provider = result.itemProvider;
        // suggestedName is the original filename (without extension) from NSItemProvider.
        // Used as fallback when PHAsset fetch returns nil (e.g. Limited photo library access).
        NSString *suggestedName = provider.suggestedName;

        // If include extra, we fetch the PHAsset, this required library permissions
        if([self.options[@"includeExtra"] boolValue] && result.assetIdentifier != nil) {
            PHFetchResult* fetchResult = [PHAsset fetchAssetsWithLocalIdentifiers:@[result.assetIdentifier] options:nil];
            asset = fetchResult.firstObject;
        }

        dispatch_group_enter(completionGroup);

        if ([provider hasItemConformingToTypeIdentifier:(NSString *)kUTTypeImage]) {
            NSString *identifier = provider.registeredTypeIdentifiers.firstObject;
            // Matches both com.apple.live-photo-bundle and com.apple.private.live-photo-bundle
            if ([identifier containsString:@"live-photo-bundle"]) {
                // Handle live photos
                identifier = UTTypeImage.identifier;
            }

            [provider loadFileRepresentationForTypeIdentifier:identifier completionHandler:^(NSURL * _Nullable url, NSError * _Nullable error) {
                NSData *data = [[NSData alloc] initWithContentsOfURL:url];
                UIImage *image = [[UIImage alloc] initWithData:data];

                assets[index] = [self mapImageToAsset:image data:data phAsset:asset suggestedName:suggestedName];
                dispatch_group_leave(completionGroup);
            }];
        } else if ([provider hasItemConformingToTypeIdentifier:(NSString *)kUTTypeMovie]) {
            [provider loadFileRepresentationForTypeIdentifier:(NSString *)kUTTypeMovie completionHandler:^(NSURL * _Nullable url, NSError * _Nullable error) {
                NSDictionary *mappedAsset = [self mapVideoToAsset:url phAsset:asset error:nil];
                if (nil != mappedAsset) {
                    assets[index] = mappedAsset;
                }
                dispatch_group_leave(completionGroup);
            }];
        } else {
            // The provider didn't have an item matching photo or video (fails on M1 Mac Simulator)
            dispatch_group_leave(completionGroup);
        }
    }];

    dispatch_group_notify(completionGroup, dispatch_get_main_queue(), ^{
        //  mapVideoToAsset can fail and return nil, leaving asset NSNull.
        for (NSDictionary *asset in assets) {
            if ([asset isEqual:[NSNull null]]) {
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
