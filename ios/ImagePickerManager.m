#import "ImagePickerManager.h"
#import "RCTConvert.h"
#import <AssetsLibrary/AssetsLibrary.h>

@import MobileCoreServices;

@interface ImagePickerManager ()

@property (nonatomic, strong) UIAlertController *alertController;
@property (nonatomic, strong) UIImagePickerController *picker;
@property (nonatomic, strong) RCTResponseSenderBlock callback;
@property (nonatomic, strong) NSDictionary *defaultOptions;
@property (nonatomic, retain) NSMutableDictionary *options;
@property (nonatomic, strong) NSDictionary *customButtons;

@end

@implementation ImagePickerManager

RCT_EXPORT_MODULE();

- (instancetype)init
{
    if (self = [super init]) {
        self.defaultOptions = @{
            @"title": @"Select a Photo",
            @"cancelButtonTitle": @"Cancel",
            @"takePhotoButtonTitle": @"Take Photo…",
            @"chooseFromLibraryButtonTitle": @"Choose from Library…",
            @"quality" : @0.2, // 1.0 best to 0.0 worst
            @"allowsEditing" : @NO
        };
    }
    return self;
}

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
    self.options = [NSMutableDictionary dictionaryWithDictionary:self.defaultOptions]; // Set default options
    for (NSString *key in options.keyEnumerator) { // Replace default options
        [self.options setValue:options[key] forKey:key];
    }

    NSString *title = [self.options valueForKey:@"title"];
    if ([title isEqual:[NSNull null]] || title.length == 0) {
        title = nil; // A more visually appealing UIAlertControl is displayed with a nil title rather than title = @""
    }
    NSString *cancelTitle = [self.options valueForKey:@"cancelButtonTitle"];
    if ([cancelTitle isEqual:[NSNull null]] || cancelTitle.length == 0) {
        cancelTitle = self.defaultOptions[@"cancelButtonTitle"]; // Don't allow null or empty string cancel button title
    }
    NSString *takePhotoButtonTitle = [self.options valueForKey:@"takePhotoButtonTitle"];
    NSString *chooseFromLibraryButtonTitle = [self.options valueForKey:@"chooseFromLibraryButtonTitle"];

    if ([UIAlertController class] && [UIAlertAction class]) { // iOS 8+
        self.alertController = [UIAlertController alertControllerWithTitle:title message:nil preferredStyle:UIAlertControllerStyleActionSheet];

        UIAlertAction *cancelAction = [UIAlertAction actionWithTitle:cancelTitle style:UIAlertActionStyleCancel handler:^(UIAlertAction * action) {
            self.callback(@[@{@"didCancel": @YES}]); // Return callback for 'cancel' action (if is required)
        }];
        [self.alertController addAction:cancelAction];

        if (![takePhotoButtonTitle isEqual:[NSNull null]] && takePhotoButtonTitle.length > 0) {
            UIAlertAction *takePhotoAction = [UIAlertAction actionWithTitle:takePhotoButtonTitle style:UIAlertActionStyleDefault handler:^(UIAlertAction * action) {
                [self actionHandler:action];
            }];
            [self.alertController addAction:takePhotoAction];
        }
        if (![chooseFromLibraryButtonTitle isEqual:[NSNull null]] && chooseFromLibraryButtonTitle.length > 0) {
            UIAlertAction *chooseFromLibraryAction = [UIAlertAction actionWithTitle:chooseFromLibraryButtonTitle style:UIAlertActionStyleDefault handler:^(UIAlertAction * action) {
                [self actionHandler:action];
            }];
            [self.alertController addAction:chooseFromLibraryAction];
        }

        // Add custom buttons to action sheet
        if ([self.options objectForKey:@"customButtons"] && [[self.options objectForKey:@"customButtons"] isKindOfClass:[NSDictionary class]]) {
            self.customButtons = [self.options objectForKey:@"customButtons"];
            for (NSString *key in self.customButtons) {
                UIAlertAction *customAction = [UIAlertAction actionWithTitle:key style:UIAlertActionStyleDefault handler:^(UIAlertAction * action) {
                    [self actionHandler:action];
                }];
                [self.alertController addAction:customAction];
            }
        }

        dispatch_async(dispatch_get_main_queue(), ^{
            UIViewController *root = [[[[UIApplication sharedApplication] delegate] window] rootViewController];
            while (root.presentedViewController != nil) {
                root = root.presentedViewController;
            }

            /* On iPad, UIAlertController presents a popover view rather than an action sheet like on iPhone. We must provide the location
            of the location to show the popover in this case. For simplicity, we'll just display it on the bottom center of the screen
            to mimic an action sheet */
            self.alertController.popoverPresentationController.sourceView = root.view;
            self.alertController.popoverPresentationController.sourceRect = CGRectMake(root.view.bounds.size.width / 2.0, root.view.bounds.size.height, 1.0, 1.0);

            if (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad) {
                self.alertController.popoverPresentationController.permittedArrowDirections = 0;
                for (id subview in self.alertController.view.subviews) {
                    if ([subview isMemberOfClass:[UIView class]]) {
                        ((UIView *)subview).backgroundColor = [UIColor whiteColor];
                    }
                }
            }

            [root presentViewController:self.alertController animated:YES completion:nil];
        });
    }
    else { // iOS 7 support
        UIActionSheet *popup = [[UIActionSheet alloc] initWithTitle:title delegate:self cancelButtonTitle:cancelTitle destructiveButtonTitle:nil otherButtonTitles:takePhotoButtonTitle, chooseFromLibraryButtonTitle, nil];

        if ([self.options objectForKey:@"customButtons"] && [[self.options objectForKey:@"customButtons"] isKindOfClass:[NSDictionary class]]) {
            self.customButtons = [self.options objectForKey:@"customButtons"];
            for (NSString *key in self.customButtons) {
                [popup addButtonWithTitle:key];
            }
        }

        popup.tag = 1;
        dispatch_async(dispatch_get_main_queue(), ^{
            UIViewController *root = [[[[UIApplication sharedApplication] delegate] window] rootViewController];
            while (root.presentedViewController != nil) {
                root = root.presentedViewController;
            }
            [popup showInView:root.view];
        });
    }
}

// iOS 7 Handler
- (void)actionSheet:(UIActionSheet *)popup clickedButtonAtIndex:(NSInteger)buttonIndex
{
    if (popup.tag == 1) {
        if (buttonIndex == [popup cancelButtonIndex]) {
            self.callback(@[@{@"didCancel": @YES}]);
            return;
        }
        switch (buttonIndex) {
            case 0:
                [self launchImagePicker:RNImagePickerTargetCamera];
                break;
            case 1:
                [self launchImagePicker:RNImagePickerTargetLibrarySingleImage];
                break;
            default:
                self.callback(@[@{@"customButton": [self.customButtons allKeys][buttonIndex - 2]}]);
                break;
        }
    }
}

// iOS 8+ Handler
- (void)actionHandler:(UIAlertAction *)action
{
    // If button title is one of the keys in the customButtons dictionary return the value as a callback
    NSString *customButtonStr = [self.customButtons objectForKey:action.title];
    if (customButtonStr) {
        self.callback(@[@{@"customButton": customButtonStr}]);
        return;
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
    self.options = [NSMutableDictionary dictionaryWithDictionary:self.defaultOptions]; // Set default options
    for (NSString *key in options.keyEnumerator) { // Replace default options
        [self.options setValue:options[key] forKey:key];
    }
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

    if ([[self.options objectForKey:@"mediaType"] isEqualToString:@"video"]) {
        self.picker.mediaTypes = @[(NSString *)kUTTypeMovie];

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
        }

    }
    else {
        self.picker.mediaTypes = @[(NSString *)kUTTypeImage];
    }

    if ([[self.options objectForKey:@"allowsEditing"] boolValue]) {
        self.picker.allowsEditing = true;
    }
    self.picker.modalPresentationStyle = UIModalPresentationCurrentContext;
    self.picker.delegate = self;

    dispatch_async(dispatch_get_main_queue(), ^{
        UIViewController *root = [[[[UIApplication sharedApplication] delegate] window] rootViewController];
        while (root.presentedViewController != nil) {
          root = root.presentedViewController;
        }
        [root presentViewController:self.picker animated:YES completion:nil];
    });
}

- (void)imagePickerController:(UIImagePickerController *)picker didFinishPickingMediaWithInfo:(NSDictionary *)info
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
            else if ([[[self.options objectForKey:@"imageFileType"] stringValue] isEqualToString:@"png"]) {
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
        NSMutableDictionary *response = [[NSMutableDictionary alloc] init];

        if ([mediaType isEqualToString:(NSString *)kUTTypeImage]) { // PHOTOS
            UIImage *image;
            if ([[self.options objectForKey:@"allowsEditing"] boolValue]) {
                image = [info objectForKey:UIImagePickerControllerEditedImage];
            }
            else {
                image = [info objectForKey:UIImagePickerControllerOriginalImage];
            }

            // GIFs break when resized, so we handle them differently
            if (imageURL && [[imageURL absoluteString] rangeOfString:@"ext=GIF"].location != NSNotFound) {
                ALAssetsLibrary* assetsLibrary = [[ALAssetsLibrary alloc] init];
                [assetsLibrary assetForURL:imageURL resultBlock:^(ALAsset *asset) {
                    ALAssetRepresentation *rep = [asset defaultRepresentation];
                    Byte *buffer = (Byte*)malloc(rep.size);
                    NSUInteger buffered = [rep getBytes:buffer fromOffset:0.0 length:rep.size error:nil];
                    NSData *data = [NSData dataWithBytesNoCopy:buffer length:buffered freeWhenDone:YES];
                    [data writeToFile:path atomically:YES];

                    NSMutableDictionary *response = [[NSMutableDictionary alloc] init];
                    [response setObject:@(image.size.width) forKey:@"width"];
                    [response setObject:@(image.size.height) forKey:@"height"];

                    BOOL vertical = (image.size.width < image.size.height) ? YES : NO;
                    [response setObject:@(vertical) forKey:@"isVertical"];

                    if (![[self.options objectForKey:@"noData"] boolValue]) {
                        NSString *dataString = [data base64EncodedStringWithOptions:0];
                        [response setObject:dataString forKey:@"data"];
                    }

                    NSURL *fileURL = [NSURL fileURLWithPath:path];
                    [response setObject:[fileURL absoluteString] forKey:@"uri"];

                    NSNumber *fileSizeValue = nil;
                    NSError *fileSizeError = nil;
                    [fileURL getResourceValue:&fileSizeValue forKey:NSURLFileSizeKey error:&fileSizeError];
                    if (fileSizeValue){
                        [response setObject:fileSizeValue forKey:@"fileSize"];
                    }

                    self.callback(@[response]);
                } failureBlock:^(NSError *error) {
                    self.callback(@[@{@"error": error.localizedFailureReason}]);
                }];
                return;
            }

            image = [self fixOrientation:image];  // Rotate the image for upload to web

            // If needed, downscale image
            float maxWidth = image.size.width;
            float maxHeight = image.size.height;
            if ([self.options valueForKey:@"maxWidth"]) {
                maxWidth = [[self.options valueForKey:@"maxWidth"] floatValue];
            }
            if ([self.options valueForKey:@"maxHeight"]) {
                maxHeight = [[self.options valueForKey:@"maxHeight"] floatValue];
            }
            image = [self downscaleImageIfNecessary:image maxWidth:maxWidth maxHeight:maxHeight];

            NSData *data;
            if ([[[self.options objectForKey:@"imageFileType"] stringValue] isEqualToString:@"png"]) {
                data = UIImagePNGRepresentation(image);
            }
            else {
                data = UIImageJPEGRepresentation(image, [[self.options valueForKey:@"quality"] floatValue]);
            }
            [data writeToFile:path atomically:YES];

            if (![[self.options objectForKey:@"noData"] boolValue]) {
                NSString *dataString = [data base64EncodedStringWithOptions:0]; // base64 encoded image string
                [response setObject:dataString forKey:@"data"];
            }

            BOOL vertical = (image.size.width < image.size.height) ? YES : NO;
            [response setObject:@(vertical) forKey:@"isVertical"];
            NSURL *fileURL = [NSURL fileURLWithPath:path];
            NSString *filePath = [fileURL absoluteString];
            [response setObject:filePath forKey:@"uri"];

            // add ref to the original image
            NSString *origURL = [imageURL absoluteString];
            if (origURL) {
              [response setObject:origURL forKey:@"origURL"];
            }

            NSNumber *fileSizeValue = nil;
            NSError *fileSizeError = nil;
            [fileURL getResourceValue:&fileSizeValue forKey:NSURLFileSizeKey error:&fileSizeError];
            if (fileSizeValue){
                [response setObject:fileSizeValue forKey:@"fileSize"];
            }

            [response setObject:@(image.size.width) forKey:@"width"];
            [response setObject:@(image.size.height) forKey:@"height"];
        }
        else { // VIDEO
            NSURL *videoURL = info[UIImagePickerControllerMediaURL];
            NSURL *videoDestinationURL = [NSURL fileURLWithPath:path];

            NSFileManager *fileManager = [NSFileManager defaultManager];
            if ([fileName isEqualToString:@"capturedvideo.MOV"]) {
                if ([fileManager fileExistsAtPath:videoDestinationURL.path]) {
                    [fileManager removeItemAtURL:videoDestinationURL error:nil];
                }
            }
            NSError *error = nil;
            [fileManager moveItemAtURL:videoURL toURL:videoDestinationURL error:&error];
            if (error) {
                self.callback(@[@{@"error": error.localizedFailureReason}]);
                return;
            }

            [response setObject:videoDestinationURL.absoluteString forKey:@"uri"];
        }

        // If storage options are provided, check the skipBackup flag
        if ([self.options objectForKey:@"storageOptions"] && [[self.options objectForKey:@"storageOptions"] isKindOfClass:[NSDictionary class]]) {
          NSDictionary *storageOptions = [self.options objectForKey:@"storageOptions"];

          if ([[storageOptions objectForKey:@"skipBackup"] boolValue]) {
            [self addSkipBackupAttributeToItemAtPath:path]; // Don't back up the file to iCloud
          }
        }

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
        return @NO;
    }
}

@end
