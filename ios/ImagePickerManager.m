#import "ImagePickerManager.h"
#import <React/RCTConvert.h>
#import <AssetsLibrary/AssetsLibrary.h>
#import <AVFoundation/AVFoundation.h>
#import <Photos/Photos.h>
#import <React/RCTUtils.h>

@import MobileCoreServices;

@interface ImagePickerManager ()

@property (nonatomic, strong) UIImagePickerController *picker;
@property (nonatomic, strong) RCTResponseSenderBlock callback;
@property (nonatomic, strong) NSDictionary *defaultOptions;
@property (nonatomic, retain) NSDictionary *options;
@property (nonatomic, retain) NSMutableDictionary *response;
@property (nonatomic, strong) NSArray *customButtons;

@end

@implementation ImagePickerManager

RCT_EXPORT_MODULE();

RCT_EXPORT_METHOD(launchCamera:(NSDictionary *)options callback:(RCTResponseSenderBlock)callback)
{
    self.callback = callback;
    [self launchImagePicker:RNImagePickerTargetCamera options:options];
}

RCT_EXPORT_METHOD(launchImageLibrary:(NSDictionary *)options callback:(RCTResponseSenderBlock)callback)
{
    self.callback = callback;
    [self launchImagePicker:RNImagePickerTargetLibrarySingleImage options:options];
}

RCT_EXPORT_METHOD(showImagePicker:(NSDictionary *)options callback:(RCTResponseSenderBlock)callback)
{
    self.callback = callback; // Save the callback so we can use it from the delegate methods
    self.options = options;

    dispatch_async(dispatch_get_main_queue(), ^{

        NSString *title = [self.options valueForKey:@"title"];
        if ([title isEqual:[NSNull null]] || title.length == 0) {
            title = nil; // A more visually appealing UIAlertControl is displayed with a nil title rather than title = @""
        }
        NSString *cancelTitle = [self.options valueForKey:@"cancelButtonTitle"];
        NSString *takePhotoButtonTitle = [self.options valueForKey:@"takePhotoButtonTitle"];
        NSString *chooseFromLibraryButtonTitle = [self.options valueForKey:@"chooseFromLibraryButtonTitle"];        

        UIAlertController *alertController = [UIAlertController alertControllerWithTitle:title message:nil preferredStyle:UIAlertControllerStyleActionSheet];
        alertController.view.tintColor = [RCTConvert UIColor:options[@"tintColor"]];

        UIAlertAction *cancelAction = [UIAlertAction actionWithTitle:cancelTitle style:UIAlertActionStyleCancel handler:^(UIAlertAction * action) {
            self.callback(@[@{@"didCancel": @YES}]); // Return callback for 'cancel' action (if is required)
        }];
        [alertController addAction:cancelAction];

        if (![takePhotoButtonTitle isEqual:[NSNull null]] && takePhotoButtonTitle.length > 0) {
            UIAlertAction *takePhotoAction = [UIAlertAction actionWithTitle:takePhotoButtonTitle style:UIAlertActionStyleDefault handler:^(UIAlertAction * action) {
                [self actionHandler:action];
            }];
            [alertController addAction:takePhotoAction];
        }
        if (![chooseFromLibraryButtonTitle isEqual:[NSNull null]] && chooseFromLibraryButtonTitle.length > 0) {
            UIAlertAction *chooseFromLibraryAction = [UIAlertAction actionWithTitle:chooseFromLibraryButtonTitle style:UIAlertActionStyleDefault handler:^(UIAlertAction * action) {
                [self actionHandler:action];
            }];
            [alertController addAction:chooseFromLibraryAction];
        }

        // Add custom buttons to action sheet
        if ([self.options objectForKey:@"customButtons"] && [[self.options objectForKey:@"customButtons"] isKindOfClass:[NSArray class]]) {
            self.customButtons = [self.options objectForKey:@"customButtons"];
            for (NSString *button in self.customButtons) {
                NSString *title = [button valueForKey:@"title"];
                UIAlertAction *customAction = [UIAlertAction actionWithTitle:title style:UIAlertActionStyleDefault handler:^(UIAlertAction * action) {
                    [self actionHandler:action];
                }];
                [alertController addAction:customAction];
            }
        }

        UIViewController *root = RCTPresentedViewController();

        /* On iPad, UIAlertController presents a popover view rather than an action sheet like on iPhone. We must provide the location
        of the location to show the popover in this case. For simplicity, we'll just display it on the bottom center of the screen
        to mimic an action sheet */
        alertController.popoverPresentationController.sourceView = root.view;
        alertController.popoverPresentationController.sourceRect = CGRectMake(root.view.bounds.size.width / 2.0, root.view.bounds.size.height, 1.0, 1.0);

        if (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad) {
            alertController.popoverPresentationController.permittedArrowDirections = 0;
            for (id subview in alertController.view.subviews) {
                if ([subview isMemberOfClass:[UIView class]]) {
                    ((UIView *)subview).backgroundColor = [UIColor whiteColor];
                }
            }
        }

        [root presentViewController:alertController animated:YES completion:nil];
    });
}

- (void)actionHandler:(UIAlertAction *)action
{
    // If button title is one of the keys in the customButtons dictionary return the value as a callback
    NSPredicate *predicate = [NSPredicate predicateWithFormat:@"title==%@", action.title];
    NSArray *results = [self.customButtons filteredArrayUsingPredicate:predicate];
    if (results.count > 0) {
        NSString *customButtonStr = [[results objectAtIndex:0] objectForKey:@"name"];
        if (customButtonStr) {
            self.callback(@[@{@"customButton": customButtonStr}]);
            return;
        }
    }

    if ([action.title isEqualToString:[self.options valueForKey:@"takePhotoButtonTitle"]]) { // Take photo
        [self launchImagePicker:RNImagePickerTargetCamera];
    }
    else if ([action.title isEqualToString:[self.options valueForKey:@"chooseFromLibraryButtonTitle"]]) { // Choose from library
        [self launchImagePicker:RNImagePickerTargetLibrarySingleImage];
    }
}

- (void)launchImagePicker:(RNImagePickerTarget)target options:(NSDictionary *)options
{
    self.options = options;
    [self launchImagePicker:target];
}

- (void)launchImagePicker:(RNImagePickerTarget)target
{
    self.picker = [[UIImagePickerController alloc] init];

    if (target == RNImagePickerTargetCamera) {
#if TARGET_IPHONE_SIMULATOR
        self.callback(@[@{@"error": @"Camera not available on simulator"}]);
        return;
#else
        self.picker.sourceType = UIImagePickerControllerSourceTypeCamera;
        if ([[self.options objectForKey:@"cameraType"] isEqualToString:@"front"]) {
            self.picker.cameraDevice = UIImagePickerControllerCameraDeviceFront;
        }
        else { // "back"
            self.picker.cameraDevice = UIImagePickerControllerCameraDeviceRear;
        }
#endif
    }
    else { // RNImagePickerTargetLibrarySingleImage
        self.picker.sourceType = UIImagePickerControllerSourceTypePhotoLibrary;
    }

    if ([[self.options objectForKey:@"mediaType"] isEqualToString:@"video"]
        || [[self.options objectForKey:@"mediaType"] isEqualToString:@"mixed"]) {

        if ([[self.options objectForKey:@"videoQuality"] isEqualToString:@"high"]) {
            self.picker.videoQuality = UIImagePickerControllerQualityTypeHigh;
        }
        else if ([[self.options objectForKey:@"videoQuality"] isEqualToString:@"low"]) {
            self.picker.videoQuality = UIImagePickerControllerQualityTypeLow;
        }
        else {
            self.picker.videoQuality = UIImagePickerControllerQualityTypeMedium;
        }

        id durationLimit = [self.options objectForKey:@"durationLimit"];
        if (durationLimit) {
            self.picker.videoMaximumDuration = [durationLimit doubleValue];
            self.picker.allowsEditing = NO;
        }
    }
    if ([[self.options objectForKey:@"mediaType"] isEqualToString:@"video"]) {
        self.picker.mediaTypes = @[(NSString *)kUTTypeMovie];
    } else if ([[self.options objectForKey:@"mediaType"] isEqualToString:@"mixed"]) {
        self.picker.mediaTypes = @[(NSString *)kUTTypeMovie, (NSString *)kUTTypeImage];
    } else {
        self.picker.mediaTypes = @[(NSString *)kUTTypeImage];
    }

    if ([[self.options objectForKey:@"allowsEditing"] boolValue]) {
        self.picker.allowsEditing = true;
    }
    self.picker.modalPresentationStyle = UIModalPresentationCurrentContext;
    self.picker.delegate = self;

    // Check permissions
    void (^showPickerViewController)() = ^void() {
        dispatch_async(dispatch_get_main_queue(), ^{
            UIViewController *root = RCTPresentedViewController();
            [root presentViewController:self.picker animated:YES completion:nil];
        });
    };

    if (target == RNImagePickerTargetCamera) {
        [self checkCameraPermissions:^(BOOL granted) {
            if (!granted) {
                self.callback(@[@{@"error": @"Camera permissions not granted"}]);
                return;
            }

            showPickerViewController();
        }];
    }
    else { // RNImagePickerTargetLibrarySingleImage
      if (@available(iOS 11.0, *)) {
        showPickerViewController();
      } else {
        [self checkPhotosPermissions:^(BOOL granted) {
          if (!granted) {
            self.callback(@[@{@"error": @"Photo library permissions not granted"}]);
            return;
          }

          showPickerViewController();
        }];
      }
    }
}

- (NSString * _Nullable)originalFilenameForAsset:(PHAsset * _Nullable)asset assetType:(PHAssetResourceType)type {
    if (!asset) { return nil; }

    PHAssetResource *originalResource;
    // Get the underlying resources for the PHAsset (PhotoKit)
    NSArray<PHAssetResource *> *pickedAssetResources = [PHAssetResource assetResourcesForAsset:asset];

    // Find the original resource (underlying image) for the asset, which has the desired filename
    for (PHAssetResource *resource in pickedAssetResources) {
        if (resource.type == type) {
            originalResource = resource;
        }
    }

    return originalResource.originalFilename;
}

- (void)imagePickerController:(UIImagePickerController *)picker didFinishPickingMediaWithInfo:(NSDictionary<NSString *,id> *)info
{
    dispatch_block_t dismissCompletionBlock = ^{

        NSURL *imageURL = [info valueForKey:UIImagePickerControllerReferenceURL];
        NSString *mediaType = [info objectForKey:UIImagePickerControllerMediaType];

        NSString *fileName;
        if ([mediaType isEqualToString:(NSString *)kUTTypeImage]) {
            NSString *tempFileName = [[NSUUID UUID] UUIDString];
            if (imageURL && [[imageURL absoluteString] rangeOfString:@"ext=GIF"].location != NSNotFound) {
                fileName = [tempFileName stringByAppendingString:@".gif"];
            }
            else if ([[self.options objectForKey:@"imageFileType"] isEqualToString:@"png"]) {
                fileName = [tempFileName stringByAppendingString:@".png"];
            }
            else {
                fileName = [tempFileName stringByAppendingString:@".jpg"];
            }
        }
        else {
            NSURL *videoURL = info[UIImagePickerControllerMediaURL];
            fileName = videoURL.lastPathComponent;
        }

        // We default to path to the temporary directory
        NSString *path = [[NSTemporaryDirectory()stringByStandardizingPath] stringByAppendingPathComponent:fileName];

        // If storage options are provided, we use the documents directory which is persisted
        if ([self.options objectForKey:@"storageOptions"] && [[self.options objectForKey:@"storageOptions"] isKindOfClass:[NSDictionary class]]) {
            NSDictionary *storageOptions = [self.options objectForKey:@"storageOptions"];

            NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
            NSString *documentsDirectory = [paths objectAtIndex:0];
            path = [documentsDirectory stringByAppendingPathComponent:fileName];

            // Creates documents subdirectory, if provided
            if ([storageOptions objectForKey:@"path"]) {
                NSString *newPath = [documentsDirectory stringByAppendingPathComponent:[storageOptions objectForKey:@"path"]];
                NSError *error;
                [[NSFileManager defaultManager] createDirectoryAtPath:newPath withIntermediateDirectories:YES attributes:nil error:&error];
                if (error) {
                    NSLog(@"Error creating documents subdirectory: %@", error);
                    self.callback(@[@{@"error": error.localizedFailureReason}]);
                    return;
                }
                else {
                    path = [newPath stringByAppendingPathComponent:fileName];
                }
            }
        }

        // Create the response object
        self.response = [[NSMutableDictionary alloc] init];

        if ([mediaType isEqualToString:(NSString *)kUTTypeImage]) { // PHOTOS
            UIImage *originalImage;
            if ([[self.options objectForKey:@"allowsEditing"] boolValue]) {
                originalImage = [info objectForKey:UIImagePickerControllerEditedImage];
            }
            else {
                originalImage = [info objectForKey:UIImagePickerControllerOriginalImage];
            }

            if (imageURL) {
                PHAsset *pickedAsset;
                if (@available(iOS 11.0, *)) {
                  pickedAsset = [info objectForKey: UIImagePickerControllerPHAsset];
                } else {
                  pickedAsset = [PHAsset fetchAssetsWithALAssetURLs:@[imageURL] options:nil].lastObject;
                }
                
                NSString *originalFilename = [self originalFilenameForAsset:pickedAsset assetType:PHAssetResourceTypePhoto];
                self.response[@"fileName"] = originalFilename ?: [NSNull null];
                if (pickedAsset.location) {
                    self.response[@"latitude"] = @(pickedAsset.location.coordinate.latitude);
                    self.response[@"longitude"] = @(pickedAsset.location.coordinate.longitude);
                }
                if (pickedAsset.creationDate) {
                    self.response[@"timestamp"] = [[ImagePickerManager ISO8601DateFormatter] stringFromDate:pickedAsset.creationDate];
                }
            }

            // GIFs break when resized, so we handle them differently
            if (imageURL && [[imageURL absoluteString] rangeOfString:@"ext=GIF"].location != NSNotFound) {
                ALAssetsLibrary* assetsLibrary = [[ALAssetsLibrary alloc] init];
                [assetsLibrary assetForURL:imageURL resultBlock:^(ALAsset *asset) {
                    ALAssetRepresentation *rep = [asset defaultRepresentation];
                    const NSUInteger repSize = (NSUInteger)[rep size];
                    Byte *buffer = (Byte*)malloc(repSize);
                    NSUInteger buffered = [rep getBytes:buffer fromOffset:0.0 length:repSize error:nil];
                    NSData *data = [NSData dataWithBytesNoCopy:buffer length:buffered freeWhenDone:YES];
                    [data writeToFile:path atomically:YES];

                    NSMutableDictionary *gifResponse = [[NSMutableDictionary alloc] init];
                    [gifResponse setObject:@(originalImage.size.width) forKey:@"width"];
                    [gifResponse setObject:@(originalImage.size.height) forKey:@"height"];

                    BOOL vertical = (originalImage.size.width < originalImage.size.height) ? YES : NO;
                    [gifResponse setObject:@(vertical) forKey:@"isVertical"];

                    if (![[self.options objectForKey:@"noData"] boolValue]) {
                        NSString *dataString = [data base64EncodedStringWithOptions:0];
                        [gifResponse setObject:dataString forKey:@"data"];
                    }

                    NSURL *fileURL = [NSURL fileURLWithPath:path];
                    [gifResponse setObject:[fileURL absoluteString] forKey:@"uri"];

                    NSNumber *fileSizeValue = nil;
                    NSError *fileSizeError = nil;
                    [fileURL getResourceValue:&fileSizeValue forKey:NSURLFileSizeKey error:&fileSizeError];
                    if (fileSizeValue){
                        [gifResponse setObject:fileSizeValue forKey:@"fileSize"];
                    }

                    self.callback(@[gifResponse]);
                } failureBlock:^(NSError *error) {
                    self.callback(@[@{@"error": error.localizedFailureReason}]);
                }];
                return;
            }

            UIImage *editedImage = [self fixOrientation:originalImage];  // Rotate the image for upload to web

            // If needed, downscale image
            float maxWidth = editedImage.size.width;
            float maxHeight = editedImage.size.height;
            if ([self.options valueForKey:@"maxWidth"]) {
                maxWidth = [[self.options valueForKey:@"maxWidth"] floatValue];
            }
            if ([self.options valueForKey:@"maxHeight"]) {
                maxHeight = [[self.options valueForKey:@"maxHeight"] floatValue];
            }
            editedImage = [self downscaleImageIfNecessary:editedImage maxWidth:maxWidth maxHeight:maxHeight];

            NSData *data;
            NSString *mimeType;
            if ([[self.options objectForKey:@"imageFileType"] isEqualToString:@"png"]) {
                data = UIImagePNGRepresentation(editedImage);
                mimeType = (__bridge_transfer NSString *)(UTTypeCopyPreferredTagWithClass(kUTTypePNG, kUTTagClassMIMEType));
            }
            else {
                data = UIImageJPEGRepresentation(editedImage, [[self.options valueForKey:@"quality"] floatValue]);
                mimeType = (__bridge_transfer NSString *)(UTTypeCopyPreferredTagWithClass(kUTTypeJPEG, kUTTagClassMIMEType));
            }
            [self.response setObject:mimeType forKey:@"type"];
            [data writeToFile:path atomically:YES];

            if (![[self.options objectForKey:@"noData"] boolValue]) {
                NSString *dataString = [data base64EncodedStringWithOptions:0]; // base64 encoded image string
                [self.response setObject:dataString forKey:@"data"];
            }

            BOOL vertical = (editedImage.size.width < editedImage.size.height) ? YES : NO;
            [self.response setObject:@(vertical) forKey:@"isVertical"];
            NSURL *fileURL = [NSURL fileURLWithPath:path];
            NSString *filePath = [fileURL absoluteString];
            [self.response setObject:filePath forKey:@"uri"];

            // add ref to the original image
            NSString *origURL = [imageURL absoluteString];
            if (origURL) {
              [self.response setObject:origURL forKey:@"origURL"];
            }

            NSNumber *fileSizeValue = nil;
            NSError *fileSizeError = nil;
            [fileURL getResourceValue:&fileSizeValue forKey:NSURLFileSizeKey error:&fileSizeError];
            if (fileSizeValue){
                [self.response setObject:fileSizeValue forKey:@"fileSize"];
            }

            [self.response setObject:@(editedImage.size.width) forKey:@"width"];
            [self.response setObject:@(editedImage.size.height) forKey:@"height"];

            NSDictionary *storageOptions = [self.options objectForKey:@"storageOptions"];
            if (storageOptions && [[storageOptions objectForKey:@"cameraRoll"] boolValue] == YES && self.picker.sourceType == UIImagePickerControllerSourceTypeCamera) {
                ALAssetsLibrary *library = [[ALAssetsLibrary alloc] init];
                if ([[storageOptions objectForKey:@"waitUntilSaved"] boolValue]) {
                    [library writeImageToSavedPhotosAlbum:originalImage.CGImage metadata:[info valueForKey:UIImagePickerControllerMediaMetadata] completionBlock:^(NSURL *assetURL, NSError *error) {
                        if (error) {
                            NSLog(@"Error while saving picture into photo album");
                        } else {
                            // when the image has been saved in the photo album
                            if (assetURL) {
                                PHAsset *capturedAsset = [PHAsset fetchAssetsWithALAssetURLs:@[assetURL] options:nil].lastObject;
                                NSString *originalFilename = [self originalFilenameForAsset:capturedAsset assetType:PHAssetResourceTypePhoto];
                                self.response[@"fileName"] = originalFilename ?: [NSNull null];
                                // This implementation will never have a location for the captured image, it needs to be added manually with CoreLocation code here.
                                if (capturedAsset.creationDate) {
                                    self.response[@"timestamp"] = [[ImagePickerManager ISO8601DateFormatter] stringFromDate:capturedAsset.creationDate];
                                }
                            }
                            self.callback(@[self.response]);
                        }
                    }];
                } else {
                    [library writeImageToSavedPhotosAlbum:originalImage.CGImage metadata:[info valueForKey:UIImagePickerControllerMediaMetadata] completionBlock:nil];
                }
            }
        }
        else { // VIDEO
            NSURL *videoRefURL = info[UIImagePickerControllerReferenceURL];
            NSURL *videoURL = info[UIImagePickerControllerMediaURL];
            NSURL *videoDestinationURL = [NSURL fileURLWithPath:path];

            if (videoRefURL) {
                PHAsset *pickedAsset = [PHAsset fetchAssetsWithALAssetURLs:@[videoRefURL] options:nil].lastObject;
                NSString *originalFilename = [self originalFilenameForAsset:pickedAsset assetType:PHAssetResourceTypeVideo];
                self.response[@"fileName"] = originalFilename ?: [NSNull null];
                if (pickedAsset.location) {
                    self.response[@"latitude"] = @(pickedAsset.location.coordinate.latitude);
                    self.response[@"longitude"] = @(pickedAsset.location.coordinate.longitude);
                }
                if (pickedAsset.creationDate) {
                    self.response[@"timestamp"] = [[ImagePickerManager ISO8601DateFormatter] stringFromDate:pickedAsset.creationDate];
                }
            }

            if ([videoURL.URLByResolvingSymlinksInPath.path isEqualToString:videoDestinationURL.URLByResolvingSymlinksInPath.path] == NO) {
                NSFileManager *fileManager = [NSFileManager defaultManager];

                // Delete file if it already exists
                if ([fileManager fileExistsAtPath:videoDestinationURL.path]) {
                    [fileManager removeItemAtURL:videoDestinationURL error:nil];
                }

                if (videoURL) { // Protect against reported crash
                  NSError *error = nil;

                  // If we have write access to the source file, move it. Otherwise use copy. 
                  if ([fileManager isWritableFileAtPath:[videoURL path]]) {
                    [fileManager moveItemAtURL:videoURL toURL:videoDestinationURL error:&error];
                  } else {
                    [fileManager copyItemAtURL:videoURL toURL:videoDestinationURL error:&error];
                  }
    
                  if (error) {
                      self.callback(@[@{@"error": error.localizedFailureReason}]);
                      return;
                  }
                }
            }

            [self.response setObject:videoDestinationURL.absoluteString forKey:@"uri"];
            if (videoRefURL.absoluteString) {
                [self.response setObject:videoRefURL.absoluteString forKey:@"origURL"];
            }

            NSDictionary *storageOptions = [self.options objectForKey:@"storageOptions"];
            if (storageOptions && [[storageOptions objectForKey:@"cameraRoll"] boolValue] == YES && self.picker.sourceType == UIImagePickerControllerSourceTypeCamera) {
                ALAssetsLibrary *library = [[ALAssetsLibrary alloc] init];
                [library writeVideoAtPathToSavedPhotosAlbum:videoDestinationURL completionBlock:^(NSURL *assetURL, NSError *error) {
                    if (error) {
                        self.callback(@[@{@"error": error.localizedFailureReason}]);
                        return;
                    } else {
                        NSLog(@"Save video succeed.");
                        if ([[storageOptions objectForKey:@"waitUntilSaved"] boolValue]) {
                            if (assetURL) {
                                PHAsset *capturedAsset = [PHAsset fetchAssetsWithALAssetURLs:@[assetURL] options:nil].lastObject;
                                NSString *originalFilename = [self originalFilenameForAsset:capturedAsset assetType:PHAssetResourceTypeVideo];
                                self.response[@"fileName"] = originalFilename ?: [NSNull null];
                                // This implementation will never have a location for the captured image, it needs to be added manually with CoreLocation code here.
                                if (capturedAsset.creationDate) {
                                    self.response[@"timestamp"] = [[ImagePickerManager ISO8601DateFormatter] stringFromDate:capturedAsset.creationDate];
                                }
                            }

                            self.callback(@[self.response]);
                        }
                    }
                }];
            }
        }

        // If storage options are provided, check the skipBackup flag
        if ([self.options objectForKey:@"storageOptions"] && [[self.options objectForKey:@"storageOptions"] isKindOfClass:[NSDictionary class]]) {
            NSDictionary *storageOptions = [self.options objectForKey:@"storageOptions"];

            if ([[storageOptions objectForKey:@"skipBackup"] boolValue]) {
                [self addSkipBackupAttributeToItemAtPath:path]; // Don't back up the file to iCloud
            }

            if ([[storageOptions objectForKey:@"waitUntilSaved"] boolValue] == NO ||
                [[storageOptions objectForKey:@"cameraRoll"] boolValue] == NO ||
                self.picker.sourceType != UIImagePickerControllerSourceTypeCamera)
            {
                self.callback(@[self.response]);
            }
        }
        else {
            self.callback(@[self.response]);
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

#pragma mark - Helpers

- (void)checkCameraPermissions:(void(^)(BOOL granted))callback
{
    AVAuthorizationStatus status = [AVCaptureDevice authorizationStatusForMediaType:AVMediaTypeVideo];
    if (status == AVAuthorizationStatusAuthorized) {
        callback(YES);
        return;
    } else if (status == AVAuthorizationStatusNotDetermined){
        [AVCaptureDevice requestAccessForMediaType:AVMediaTypeVideo completionHandler:^(BOOL granted) {
            callback(granted);
            return;
        }];
    } else {
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

- (UIImage*)downscaleImageIfNecessary:(UIImage*)image maxWidth:(float)maxWidth maxHeight:(float)maxHeight
{
    UIImage* newImage = image;

    // Nothing to do here
    if (image.size.width <= maxWidth && image.size.height <= maxHeight) {
        return newImage;
    }

    CGSize scaledSize = CGSizeMake(image.size.width, image.size.height);
    if (maxWidth < scaledSize.width) {
        scaledSize = CGSizeMake(maxWidth, (maxWidth / scaledSize.width) * scaledSize.height);
    }
    if (maxHeight < scaledSize.height) {
        scaledSize = CGSizeMake((maxHeight / scaledSize.height) * scaledSize.width, maxHeight);
    }

    // If the pixels are floats, it causes a white line in iOS8 and probably other versions too
    scaledSize.width = (int)scaledSize.width;
    scaledSize.height = (int)scaledSize.height;

    UIGraphicsBeginImageContext(scaledSize); // this will resize
    [image drawInRect:CGRectMake(0, 0, scaledSize.width, scaledSize.height)];
    newImage = UIGraphicsGetImageFromCurrentImageContext();
    if (newImage == nil) {
        NSLog(@"could not scale image");
    }
    UIGraphicsEndImageContext();

    return newImage;
}

- (UIImage *)fixOrientation:(UIImage *)srcImg {
    if (srcImg.imageOrientation == UIImageOrientationUp) {
        return srcImg;
    }

    CGAffineTransform transform = CGAffineTransformIdentity;
    switch (srcImg.imageOrientation) {
        case UIImageOrientationDown:
        case UIImageOrientationDownMirrored:
            transform = CGAffineTransformTranslate(transform, srcImg.size.width, srcImg.size.height);
            transform = CGAffineTransformRotate(transform, M_PI);
            break;

        case UIImageOrientationLeft:
        case UIImageOrientationLeftMirrored:
            transform = CGAffineTransformTranslate(transform, srcImg.size.width, 0);
            transform = CGAffineTransformRotate(transform, M_PI_2);
            break;

        case UIImageOrientationRight:
        case UIImageOrientationRightMirrored:
            transform = CGAffineTransformTranslate(transform, 0, srcImg.size.height);
            transform = CGAffineTransformRotate(transform, -M_PI_2);
            break;
        case UIImageOrientationUp:
        case UIImageOrientationUpMirrored:
            break;
    }

    switch (srcImg.imageOrientation) {
        case UIImageOrientationUpMirrored:
        case UIImageOrientationDownMirrored:
            transform = CGAffineTransformTranslate(transform, srcImg.size.width, 0);
            transform = CGAffineTransformScale(transform, -1, 1);
            break;

        case UIImageOrientationLeftMirrored:
        case UIImageOrientationRightMirrored:
            transform = CGAffineTransformTranslate(transform, srcImg.size.height, 0);
            transform = CGAffineTransformScale(transform, -1, 1);
            break;
        case UIImageOrientationUp:
        case UIImageOrientationDown:
        case UIImageOrientationLeft:
        case UIImageOrientationRight:
            break;
    }

    CGContextRef ctx = CGBitmapContextCreate(NULL, srcImg.size.width, srcImg.size.height, CGImageGetBitsPerComponent(srcImg.CGImage), 0, CGImageGetColorSpace(srcImg.CGImage), CGImageGetBitmapInfo(srcImg.CGImage));
    CGContextConcatCTM(ctx, transform);
    switch (srcImg.imageOrientation) {
        case UIImageOrientationLeft:
        case UIImageOrientationLeftMirrored:
        case UIImageOrientationRight:
        case UIImageOrientationRightMirrored:
            CGContextDrawImage(ctx, CGRectMake(0,0,srcImg.size.height,srcImg.size.width), srcImg.CGImage);
            break;

        default:
            CGContextDrawImage(ctx, CGRectMake(0,0,srcImg.size.width,srcImg.size.height), srcImg.CGImage);
            break;
    }

    CGImageRef cgimg = CGBitmapContextCreateImage(ctx);
    UIImage *img = [UIImage imageWithCGImage:cgimg];
    CGContextRelease(ctx);
    CGImageRelease(cgimg);
    return img;
}

- (BOOL)addSkipBackupAttributeToItemAtPath:(NSString *) filePathString
{
    NSURL* URL= [NSURL fileURLWithPath: filePathString];
    if ([[NSFileManager defaultManager] fileExistsAtPath: [URL path]]) {
        NSError *error = nil;
        BOOL success = [URL setResourceValue: [NSNumber numberWithBool: YES]
                                      forKey: NSURLIsExcludedFromBackupKey error: &error];

        if(!success){
            NSLog(@"Error excluding %@ from backup %@", [URL lastPathComponent], error);
        }
        return success;
    }
    else {
        NSLog(@"Error setting skip backup attribute: file not found");
        return NO;
    }
}

#pragma mark - Class Methods

+ (NSDateFormatter * _Nonnull)ISO8601DateFormatter {
    static NSDateFormatter *ISO8601DateFormatter;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        ISO8601DateFormatter = [[NSDateFormatter alloc] init];
        NSLocale *enUSPOSIXLocale = [NSLocale localeWithLocaleIdentifier:@"en_US_POSIX"];
        ISO8601DateFormatter.locale = enUSPOSIXLocale;
        ISO8601DateFormatter.timeZone = [NSTimeZone timeZoneWithAbbreviation:@"GMT"];
        ISO8601DateFormatter.dateFormat = @"yyyy-MM-dd'T'HH:mm:ssZZZZZ";
    });
    return ISO8601DateFormatter;
}

@end
