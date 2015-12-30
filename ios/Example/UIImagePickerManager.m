#import "UIImagePickerManager.h"
#import "RCTConvert.h"

@interface UIImagePickerManager ()

@property (nonatomic, strong) UIAlertController *alertController;
@property (nonatomic, strong) UIImagePickerController *picker;
@property (nonatomic, strong) RCTResponseSenderBlock callback;
@property (nonatomic, strong) NSDictionary *defaultOptions;
@property (nonatomic, retain) NSMutableDictionary *options;
@property (nonatomic, strong) NSDictionary *customButtons;

@end

@implementation UIImagePickerManager

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
    
    self.alertController = [UIAlertController alertControllerWithTitle:title message:nil preferredStyle:UIAlertControllerStyleActionSheet];
    
    NSString *cancelTitle = [self.options valueForKey:@"cancelButtonTitle"];
    if ([cancelTitle isEqual:[NSNull null]] || cancelTitle.length == 0) {
        cancelTitle = self.defaultOptions[@"cancelButtonTitle"]; // Don't allow null or empty string cancel button title
    }
    UIAlertAction *cancelAction = [UIAlertAction actionWithTitle:cancelTitle style:UIAlertActionStyleCancel handler:^(UIAlertAction * action) {
        self.callback(@[@YES, [NSNull null]]); // Return callback for 'cancel' action (if is required)
    }];
    [self.alertController addAction:cancelAction];


    NSString *takePhotoButtonTitle = [self.options valueForKey:@"takePhotoButtonTitle"];
    NSString *chooseFromLibraryButtonTitle = [self.options valueForKey:@"chooseFromLibraryButtonTitle"];
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
    if([self.options objectForKey:@"customButtons"] && [[self.options objectForKey:@"customButtons"] isKindOfClass:[NSDictionary class]]){
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
        
        /* On iPad, UIAlertController presents a popover view rather than an action sheet like on iPhone. We must provide the location
           of the location to show the popover in this case. For simplicity, we'll just display it on the bottom center of the screen
           to mimic an action sheet */
        self.alertController.popoverPresentationController.sourceView = root.view;
        self.alertController.popoverPresentationController.sourceRect = CGRectMake(root.view.bounds.size.width / 2.0, root.view.bounds.size.height, 1.0, 1.0);
        [root presentViewController:self.alertController animated:YES completion:nil];
    });
}

- (void)launchImagePicker:(RNImagePickerTarget)target options:(NSDictionary *)options
{
    self.options = [NSMutableDictionary dictionaryWithDictionary:self.defaultOptions]; // Set default options
    for (NSString *key in options.keyEnumerator) { // Replace default options
        [self.options setValue:options[key] forKey:key];
    }
    
    [self launchImagePicker:target];
}

- (void)actionHandler:(UIAlertAction *)action
{
    // If button title is one of the keys in the customButtons dictionary return the value as a callback
    NSString *customButtonStr = [self.customButtons objectForKey:action.title];
    if (customButtonStr) {
        self.callback(@[@NO, @{@"customButton": customButtonStr}]);
        return;
    }

    if ([action.title isEqualToString:[self.options valueForKey:@"takePhotoButtonTitle"]]) { // Take photo
        [self launchImagePicker:RNImagePickerTargetCamera];
    }
    else if ([action.title isEqualToString:[self.options valueForKey:@"chooseFromLibraryButtonTitle"]]) { // Choose from library
        [self launchImagePicker:RNImagePickerTargetLibrarySingleImage];
    }
}

- (void)launchImagePicker:(RNImagePickerTarget)target
{
    self.picker = [[UIImagePickerController alloc] init];

    switch(target) {
        case RNImagePickerTargetCamera:
#if TARGET_IPHONE_SIMULATOR
            NSLog(@"Camera not available on simulator");
            return;
#else
            self.picker.sourceType = UIImagePickerControllerSourceTypeCamera;
            break;
#endif
        case RNImagePickerTargetLibrarySingleImage:
            self.picker.sourceType = UIImagePickerControllerSourceTypePhotoLibrary;
            break;
        default:
            NSLog(@"Well done: This shouldn't happen. Invalid ImagePicker target. Aborting...");
            return;
    }

    if ([[self.options objectForKey:@"allowsEditing"] boolValue]) {
        self.picker.allowsEditing = true;
    }
    self.picker.modalPresentationStyle = UIModalPresentationCurrentContext;
    self.picker.delegate = self;

    UIViewController *root = [[[[UIApplication sharedApplication] delegate] window] rootViewController];
    dispatch_async(dispatch_get_main_queue(), ^{
        if (root.presentedViewController) {
            [root.presentedViewController presentViewController:self.picker animated:YES completion:nil];
        }
        else {
            [root presentViewController:self.picker animated:YES completion:nil];
        }
    });
}

- (void)imagePickerController:(UIImagePickerController *)picker didFinishPickingMediaWithInfo:(NSDictionary *)info
{
    dispatch_async(dispatch_get_main_queue(), ^{
        [picker dismissViewControllerAnimated:YES completion:nil];
    });

    /* Picked Image */
    UIImage *image;
    if ([[self.options objectForKey:@"allowsEditing"] boolValue]) {
      image = [info objectForKey:UIImagePickerControllerEditedImage];
    }
    else {
      image = [info objectForKey:UIImagePickerControllerOriginalImage];
    }

    /* creating a temp url to be passed */
    NSString *ImageUUID = [[NSUUID UUID] UUIDString];
    NSString *ImageName = [ImageUUID stringByAppendingString:@".jpg"];

    // This will be the default URL
    NSString* path = [[NSTemporaryDirectory()stringByStandardizingPath] stringByAppendingPathComponent:ImageName];
    
    NSDictionary *storageOptions;
    // if storage options are provided change path to the documents directory
    if([self.options objectForKey:@"storageOptions"] && [[self.options objectForKey:@"storageOptions"] isKindOfClass:[NSDictionary class]]){
        // retrieve documents path
        NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
        NSString *documentsDirectory = [paths objectAtIndex:0];
        // update path to save image to documents directory
        path = [documentsDirectory stringByAppendingPathComponent:ImageName];

        storageOptions = [self.options objectForKey:@"storageOptions"];
        // if extra path is provided try to create it
        if ([storageOptions objectForKey:@"path"]) {
            NSString *newPath = [documentsDirectory stringByAppendingPathComponent:[storageOptions objectForKey:@"path"]];
            NSError *error = nil;
            [[NSFileManager defaultManager] createDirectoryAtPath:newPath withIntermediateDirectories:YES attributes:nil error:&error];
            
            // if there was an error do not update path
            if (error != nil) {
                NSLog(@"error creating directory: %@", error);
            }
            else {
                path = [newPath stringByAppendingPathComponent:ImageName];
            }
        }
    }
    

    
    // Rotate the image for upload to web
    image = [self fixOrientation:image];

    //If needed, downscale image
    float maxWidth = image.size.width;
    float maxHeight = image.size.height;
    if ([self.options valueForKey:@"maxWidth"]) {
        maxWidth = [[self.options valueForKey:@"maxWidth"] floatValue];
    }
    if ([self.options valueForKey:@"maxHeight"]) {
        maxHeight = [[self.options valueForKey:@"maxHeight"] floatValue];
    }
    image = [self downscaleImageIfNecessary:image maxWidth:maxWidth maxHeight:maxHeight];

    // Create the response object
    NSMutableDictionary *response = [[NSMutableDictionary alloc] init];
    
    [response setObject:@(maxWidth) forKey:@"width"];
    [response setObject:@(maxHeight) forKey:@"height"];

    NSData *data = UIImageJPEGRepresentation(image, [[self.options valueForKey:@"quality"] floatValue]);
    // base64 encoded image string, unless the caller doesn't want it
    if (![[self.options objectForKey:@"noData"] boolValue]) {
        NSString *dataString = [data base64EncodedStringWithOptions:0];
        [response setObject:dataString forKey:@"data"];
    }

    // file uri
    [data writeToFile:path atomically:YES];
    NSString *fileURL = [[NSURL fileURLWithPath:path] absoluteString];
    if ([[storageOptions objectForKey:@"skipBackup"] boolValue]) {
        [self addSkipBackupAttributeToItemAtPath:path];
    }
    [response setObject:fileURL forKey:@"uri"];
    
    // image orientation
    BOOL vertical = (image.size.width < image.size.height) ? YES : NO;
    [response setObject:@(vertical) forKey:@"isVertical"];

    self.callback(@[@NO, response]);
}

- (void)imagePickerControllerDidCancel:(UIImagePickerController *)picker
{
    dispatch_async(dispatch_get_main_queue(), ^{
        [picker dismissViewControllerAnimated:YES completion:nil];
    });
    
    self.callback(@[@YES, [NSNull null]]);
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
