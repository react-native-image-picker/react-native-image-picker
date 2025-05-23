#import "ImagePickerUtils.h"
#import <CoreServices/CoreServices.h>
#import <PhotosUI/PhotosUI.h>
#import <AVFoundation/AVFoundation.h>

@implementation ImagePickerUtils

+ (void) setupPickerFromOptions:(UIImagePickerController *)picker options:(NSDictionary *)options target:(RNImagePickerTarget)target
{
    if ([options[@"mediaType"] isEqualToString:@"video"] || [options[@"mediaType"] isEqualToString:@"mixed"]) {

        if ([[options objectForKey:@"videoQuality"] isEqualToString:@"high"]) {
            picker.videoQuality = UIImagePickerControllerQualityTypeHigh;
        }
        else if ([[options objectForKey:@"videoQuality"] isEqualToString:@"low"]) {
            picker.videoQuality = UIImagePickerControllerQualityTypeLow;
        }
        else {
            picker.videoQuality = UIImagePickerControllerQualityTypeMedium;
        }
    }
    
    if (target == camera) {
        picker.sourceType = UIImagePickerControllerSourceTypeCamera;

        if ([options[@"durationLimit"] doubleValue] > 0) {
            picker.videoMaximumDuration = [options[@"durationLimit"] doubleValue];
        }

        if ([options[@"cameraType"] isEqualToString:@"front"]) {
            picker.cameraDevice = UIImagePickerControllerCameraDeviceFront;
        } else {
            picker.cameraDevice = UIImagePickerControllerCameraDeviceRear;
        }
    } else {
        picker.sourceType = UIImagePickerControllerSourceTypePhotoLibrary;
    }

    if ([options[@"mediaType"] isEqualToString:@"video"]) {
        picker.mediaTypes = @[(NSString *)kUTTypeMovie];
    } else if ([options[@"mediaType"] isEqualToString:@"photo"]) {
        picker.mediaTypes = @[(NSString *)kUTTypeImage];
    } else if ([options[@"mediaType"] isEqualToString:@"mixed"]) {
        picker.mediaTypes = @[(NSString *)kUTTypeImage, (NSString *)kUTTypeMovie];
    }

    picker.modalPresentationStyle = [RCTConvert UIModalPresentationStyle:options[@"presentationStyle"]];
}

+ (PHPickerConfiguration *)makeConfigurationFromOptions:(NSDictionary *)options target:(RNImagePickerTarget)target API_AVAILABLE(ios(14))
{
    PHPickerConfiguration *configuration;
    
    if(options[@"includeExtra"]) {
        PHPhotoLibrary *photoLibrary = [PHPhotoLibrary sharedPhotoLibrary];
        configuration = [[PHPickerConfiguration alloc] initWithPhotoLibrary:photoLibrary];
    } else {
        configuration = [[PHPickerConfiguration alloc] init];
    }

    if ([[options objectForKey:@"assetRepresentationMode"] isEqualToString:@"current"]) {
        configuration.preferredAssetRepresentationMode = PHPickerConfigurationAssetRepresentationModeCurrent;
    }
    else if ([[options objectForKey:@"assetRepresentationMode"] isEqualToString:@"compatible"]) {
        configuration.preferredAssetRepresentationMode = PHPickerConfigurationAssetRepresentationModeCompatible;
    }
    else {
       configuration.preferredAssetRepresentationMode = PHPickerConfigurationAssetRepresentationModeAutomatic;
    }
    
    configuration.selectionLimit = [options[@"selectionLimit"] integerValue];

    if ([options[@"mediaType"] isEqualToString:@"video"]) {
        configuration.filter = [PHPickerFilter videosFilter];
    } else if ([options[@"mediaType"] isEqualToString:@"photo"]) {
        configuration.filter = [PHPickerFilter imagesFilter];
    } else if ((target == library) && ([options[@"mediaType"] isEqualToString:@"mixed"])) {
        configuration.filter = [PHPickerFilter anyFilterMatchingSubfilters: @[PHPickerFilter.imagesFilter, PHPickerFilter.videosFilter]];
    }
    return configuration;
}


+ (BOOL) isSimulator
{
    #if TARGET_OS_SIMULATOR
        return YES;
    #endif
    return NO;
}

+ (NSString*) getFileType:(NSData *)imageData
{
    const uint8_t firstByteJpg = 0xFF;
    const uint8_t firstBytePng = 0x89;
    const uint8_t firstByteGif = 0x47;
    
    uint8_t firstByte;
    [imageData getBytes:&firstByte length:1];
    switch (firstByte) {
      case firstByteJpg:
        return @"jpg";
      case firstBytePng:
        return @"png";
      case firstByteGif:
        return @"gif";
      default:
        return @"jpg";
    }
}

+ (NSString *) getFileTypeFromUrl:(NSURL *)url {
    CFStringRef fileExtension = (__bridge CFStringRef)[url pathExtension];
    CFStringRef UTI = UTTypeCreatePreferredIdentifierForTag(kUTTagClassFilenameExtension, fileExtension, NULL);
    CFStringRef MIMEType = UTTypeCopyPreferredTagWithClass(UTI, kUTTagClassMIMEType);
    CFRelease(UTI);
    return (__bridge_transfer NSString *)MIMEType;
}

+ (NSNumber *) getFileSizeFromUrl:(NSURL *)url {
    NSError *attributesError;
    NSDictionary *fileAttributes = [[NSFileManager defaultManager] attributesOfItemAtPath:[url path] error:&attributesError];
    NSNumber *fileSizeNumber = [fileAttributes objectForKey:NSFileSize];
    long fileSize = [fileSizeNumber longLongValue];

    if (attributesError) {
        return nil;
    }

    return [NSNumber numberWithLong:fileSize];
}

+ (CGSize)getVideoDimensionsFromUrl:(NSURL *)url {
    AVURLAsset *asset = [AVURLAsset URLAssetWithURL:url options:nil];
    NSArray *tracks = [asset tracksWithMediaType:AVMediaTypeVideo];
    
    if ([tracks count] > 0) {
        AVAssetTrack *track = [tracks objectAtIndex:0];
        return track.naturalSize;
    }
    
    return CGSizeMake(0, 0);
}

+ (UIImage*)resizeImage:(UIImage*)image maxWidth:(float)maxWidth maxHeight:(float)maxHeight
{
    if ((maxWidth == 0) || (maxHeight == 0)) {
        return image;
    }
    
    if (image.size.width <= maxWidth && image.size.height <= maxHeight) {
        return image;
    }

    CGSize newSize = CGSizeMake(image.size.width, image.size.height);
    if (maxWidth < newSize.width) {
        newSize = CGSizeMake(maxWidth, (maxWidth / newSize.width) * newSize.height);
    }
    if (maxHeight < newSize.height) {
        newSize = CGSizeMake((maxHeight / newSize.height) * newSize.width, maxHeight);
    }
    
    newSize.width = (int)newSize.width;
    newSize.height = (int)newSize.height;

    UIGraphicsBeginImageContext(newSize);
    [image drawInRect:CGRectMake(0, 0, newSize.width, newSize.height)];
    UIImage *newImage = UIGraphicsGetImageFromCurrentImageContext();
    if (newImage == nil) {
        NSLog(@"could not scale image");
    }
    UIGraphicsEndImageContext();

    return newImage;
}

+ (PHAsset *)fetchAssetFromImageInfo:(NSDictionary<NSString *,id> *)info
{
    // In iOS 14+, the best practice is to use PHPicker with assetIdentifier
    // But if we're using UIImagePickerController and have a URL, we can attempt to find the asset
    NSURL *assetURL = info[UIImagePickerControllerImageURL];
    if (!assetURL) {
        return nil;
    }
    
    // Try to get the asset based on the file path since we can't use ALAssetURLs anymore
    NSString *localID = [self getLocalIdentifierFromImageURL:assetURL];
    if (localID) {
        PHFetchResult* fetchResult = [PHAsset fetchAssetsWithLocalIdentifiers:@[localID] options:nil];
        return fetchResult.firstObject;
    }
    
    return nil;
}

// Helper method to try to get local identifier from a file URL
+ (NSString *)getLocalIdentifierFromImageURL:(NSURL *)url
{
    if (!url) {
        return nil;
    }
    
    // Try to find matching assets in the photo library
    PHFetchOptions *fetchOptions = [[PHFetchOptions alloc] init];
    fetchOptions.predicate = [NSPredicate predicateWithFormat:@"mediaType == %d", PHAssetMediaTypeImage];
    
    __block NSString *localIdentifier = nil;
    
    // Get the file name and create date to help narrow down the search
    NSString *fileName = url.lastPathComponent;
    
    // Fetch all photo assets and look for a match
    PHFetchResult *result = [PHAsset fetchAssetsWithOptions:fetchOptions];
    [result enumerateObjectsUsingBlock:^(PHAsset *asset, NSUInteger idx, BOOL *stop) {
        // Request the asset resource to get the file name
        PHAssetResource *resource = [[PHAssetResource assetResourcesForAsset:asset] firstObject];
        if ([resource.originalFilename isEqualToString:fileName]) {
            localIdentifier = asset.localIdentifier;
            *stop = YES;
        }
    }];
    
    return localIdentifier;
}

+ (BOOL)isAssetInICloud:(PHAsset *)asset
{
    if (!asset) {
        return NO;
    }
    
    PHImageRequestOptions *options = [[PHImageRequestOptions alloc] init];
    options.networkAccessAllowed = NO;
    options.synchronous = YES;
    
    __block BOOL isInICloud = NO;
    
    [[PHImageManager defaultManager] requestImageDataAndOrientationForAsset:asset 
                                                                    options:options 
                                                              resultHandler:^(NSData *imageData, NSString *dataUTI, CGImagePropertyOrientation orientation, NSDictionary *info) {
        if (!imageData) {
            NSNumber *isInCloudKey = info[PHImageResultIsInCloudKey];
            if (isInCloudKey && [isInCloudKey boolValue]) {
                isInICloud = YES;
            }
        }
    }];
    
    return isInICloud;
}

+ (UIImageOrientation)UIImageOrientationFromCGImagePropertyOrientation:(CGImagePropertyOrientation)cgOrientation {
    switch (cgOrientation) {
        case kCGImagePropertyOrientationUp:
            return UIImageOrientationUp;
        case kCGImagePropertyOrientationDown:
            return UIImageOrientationDown;
        case kCGImagePropertyOrientationLeft:
            return UIImageOrientationLeft;
        case kCGImagePropertyOrientationRight:
            return UIImageOrientationRight;
        case kCGImagePropertyOrientationUpMirrored:
            return UIImageOrientationUpMirrored;
        case kCGImagePropertyOrientationDownMirrored:
            return UIImageOrientationDownMirrored;
        case kCGImagePropertyOrientationLeftMirrored:
            return UIImageOrientationLeftMirrored;
        case kCGImagePropertyOrientationRightMirrored:
            return UIImageOrientationRightMirrored;
        default:
            return UIImageOrientationUp;
    }
}

+ (void)fetchImageFromICloudIfNeeded:(PHAsset *)asset 
                         targetSize:(CGSize)targetSize 
                        contentMode:(PHImageContentMode)contentMode 
                            options:(PHImageRequestOptions *)options 
                         completion:(void (^)(UIImage *image, NSDictionary *info, NSError *error))completion
{
    if (!asset) {
        NSError *error = [NSError errorWithDomain:@"ImagePickerUtils" 
                                             code:1001 
                                         userInfo:@{NSLocalizedDescriptionKey: @"PHAsset is nil"}];
        completion(nil, nil, error);
        return;
    }
    
    if (!options) {
        options = [[PHImageRequestOptions alloc] init];
    }
    
    options.networkAccessAllowed = YES;
    options.deliveryMode = PHImageRequestOptionsDeliveryModeHighQualityFormat;
    options.resizeMode = PHImageRequestOptionsResizeModeExact;
    
    [[PHImageManager defaultManager] requestImageForAsset:asset 
                                               targetSize:targetSize 
                                              contentMode:contentMode 
                                                  options:options 
                                            resultHandler:^(UIImage *result, NSDictionary *info) {
        BOOL downloadNeeded = [info[PHImageResultIsInCloudKey] boolValue];
        BOOL cancelled = [info[PHImageCancelledKey] boolValue];
        BOOL error = info[PHImageErrorKey] != nil;
        
        if (cancelled) {
            NSError *cancelError = [NSError errorWithDomain:@"ImagePickerUtils" 
                                                       code:1002 
                                                   userInfo:@{NSLocalizedDescriptionKey: @"iCloud download was cancelled"}];
            completion(nil, info, cancelError);
            return;
        }
        
        if (error) {
            completion(nil, info, info[PHImageErrorKey]);
            return;
        }
        
        if (result) {
            completion(result, info, nil);
        } else if (downloadNeeded) {
            NSError *downloadError = [NSError errorWithDomain:@"ImagePickerUtils" 
                                                         code:1003 
                                                     userInfo:@{NSLocalizedDescriptionKey: @"Failed to download image from iCloud"}];
            completion(nil, info, downloadError);
        } else {
            NSError *unknownError = [NSError errorWithDomain:@"ImagePickerUtils" 
                                                        code:1004 
                                                    userInfo:@{NSLocalizedDescriptionKey: @"Unknown error occurred while fetching image"}];
            completion(nil, info, unknownError);
        }
    }];
}

+ (void)fetchImageDataFromICloudIfNeeded:(PHAsset *)asset 
                              options:(PHImageRequestOptions *)options 
                           completion:(void (^)(NSData *imageData, NSString *dataUTI, UIImageOrientation orientation, NSDictionary *info, NSError *error))completion
{
    if (!asset) {
        NSError *error = [NSError errorWithDomain:@"ImagePickerUtils" 
                                             code:1001 
                                         userInfo:@{NSLocalizedDescriptionKey: @"PHAsset is nil"}];
        completion(nil, nil, UIImageOrientationUp, nil, error);
        return;
    }
    
    if (!options) {
        options = [[PHImageRequestOptions alloc] init];
    }
    
    options.networkAccessAllowed = YES;
    options.deliveryMode = PHImageRequestOptionsDeliveryModeHighQualityFormat;
    
    [[PHImageManager defaultManager] requestImageDataAndOrientationForAsset:asset 
                                                                    options:options 
                                                              resultHandler:^(NSData *imageData, NSString *dataUTI, CGImagePropertyOrientation cgOrientation, NSDictionary *info) {
        BOOL downloadNeeded = [info[PHImageResultIsInCloudKey] boolValue];
        BOOL cancelled = [info[PHImageCancelledKey] boolValue];
        BOOL error = info[PHImageErrorKey] != nil;
        
        UIImageOrientation uiOrientation = [self UIImageOrientationFromCGImagePropertyOrientation:cgOrientation];
        
        if (cancelled) {
            NSError *cancelError = [NSError errorWithDomain:@"ImagePickerUtils" 
                                                       code:1002 
                                                   userInfo:@{NSLocalizedDescriptionKey: @"iCloud download was cancelled"}];
            completion(nil, nil, UIImageOrientationUp, info, cancelError);
            return;
        }
        
        if (error) {
            completion(nil, nil, UIImageOrientationUp, info, info[PHImageErrorKey]);
            return;
        }
        
        if (imageData) {
            completion(imageData, dataUTI, uiOrientation, info, nil);
        } else if (downloadNeeded) {
            NSError *downloadError = [NSError errorWithDomain:@"ImagePickerUtils" 
                                                         code:1003 
                                                     userInfo:@{NSLocalizedDescriptionKey: @"Failed to download image data from iCloud"}];
            completion(nil, nil, UIImageOrientationUp, info, downloadError);
        } else {
            NSError *unknownError = [NSError errorWithDomain:@"ImagePickerUtils" 
                                                        code:1004 
                                                    userInfo:@{NSLocalizedDescriptionKey: @"Unknown error occurred while fetching image data"}];
            completion(nil, nil, UIImageOrientationUp, info, unknownError);
        }
    }];
}

+ (NSData *)getImageDataHandlingICloud:(NSURL *)url phAsset:(PHAsset *)asset {
    if (!url && !asset) {
        return nil;
    }
    
    if (url) {
        NSData *localData = [NSData dataWithContentsOfURL:url];
        if (localData && localData.length > 0) {
            return localData;
        }
    }
    
    if (!asset) {
        return nil;
    }
    
    if (![self isAssetInICloud:asset]) {
        if (url) {
            return [NSData dataWithContentsOfURL:url];
        }
        return nil;
    }
    
    __block NSData *resultData = nil;
    __block BOOL completed = NO;
    
    dispatch_semaphore_t semaphore = dispatch_semaphore_create(0);
    
    [self fetchImageDataFromICloudIfNeeded:asset 
                                   options:nil 
                                completion:^(NSData *imageData, NSString *dataUTI, UIImageOrientation orientation, NSDictionary *info, NSError *error) {
        if (imageData && !error) {
            resultData = imageData;
        }
        completed = YES;
        dispatch_semaphore_signal(semaphore);
    }];
    
    dispatch_time_t timeout = dispatch_time(DISPATCH_TIME_NOW, 30 * NSEC_PER_SEC);
    dispatch_semaphore_wait(semaphore, timeout);
    
    return resultData;
}

+ (void)getImageDataHandlingICloudAsync:(NSURL *)url 
                                phAsset:(PHAsset *)asset 
                             completion:(void (^)(NSData *imageData, NSError *error))completion {
    if (!completion) {
        return;
    }
    
    if (!url && !asset) {
        NSError *error = [NSError errorWithDomain:@"ImagePickerUtils" 
                                             code:1005 
                                         userInfo:@{NSLocalizedDescriptionKey: @"Both URL and PHAsset are nil"}];
        completion(nil, error);
        return;
    }
    
    if (url) {
        NSData *localData = [NSData dataWithContentsOfURL:url];
        if (localData && localData.length > 0) {
            completion(localData, nil);
            return;
        }
    }
    
    if (!asset) {
        NSError *error = [NSError errorWithDomain:@"ImagePickerUtils" 
                                             code:1006 
                                         userInfo:@{NSLocalizedDescriptionKey: @"Asset is nil and URL data is empty"}];
        completion(nil, error);
        return;
    }
    
    if (![self isAssetInICloud:asset]) {
        if (url) {
            NSData *data = [NSData dataWithContentsOfURL:url];
            completion(data, nil);
            return;
        } else {
            NSError *error = [NSError errorWithDomain:@"ImagePickerUtils" 
                                                 code:1007 
                                             userInfo:@{NSLocalizedDescriptionKey: @"Asset is not in iCloud but URL is nil"}];
            completion(nil, error);
            return;
        }
    }
    
    [self fetchImageDataFromICloudIfNeeded:asset 
                                   options:nil 
                                completion:^(NSData *imageData, NSString *dataUTI, UIImageOrientation orientation, NSDictionary *info, NSError *error) {
        completion(imageData, error);
    }];
}

@end
