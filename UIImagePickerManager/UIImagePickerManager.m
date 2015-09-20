#import "UIImagePickerManager.h"
#import "RCTConvert.h"

@interface UIImagePickerManager ()

@property (nonatomic, strong) UIActionSheet *sheet;
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
            @"takePhotoButtonTitle": @"Take Photo...",
            @"takePhotoButtonHidden": @NO,
            @"chooseFromLibraryButtonTitle": @"Choose from Library...",
            @"chooseFromLibraryButtonHidden": @NO,
            @"returnBase64Image" : @NO, // Only return base64 encoded version of the image
            @"returnIsVertical" : @NO, // If returning base64 image, return the orientation too
            @"quality" : @0.2, // 1.0 best to 0.0 worst
            @"allowsEditing" : @NO
        };
    }
    return self;
}

RCT_EXPORT_METHOD(showImagePicker:(NSDictionary *)options callback:(RCTResponseSenderBlock)callback)
{
    self.callback = callback; // Save the callback so we can use it from the delegate methods
    self.options = [NSMutableDictionary dictionaryWithDictionary:self.defaultOptions]; // Set default options
    for (NSString *key in options.keyEnumerator) { // Replace default options
        [self.options setValue:options[key] forKey:key];
    }

    BOOL takePhotoHidden = [[self.options objectForKey:@"takePhotoButtonHidden"] boolValue];
    BOOL chooseFromLibraryHidden = [[self.options objectForKey:@"chooseFromLibraryButtonHidden"] boolValue];

    // If they are both set to be hidden, then this module has no purpose -  we'll assume it was accidental and show both anyway.
    if ((takePhotoHidden && chooseFromLibraryHidden) || (!takePhotoHidden && !chooseFromLibraryHidden)) {
        self.sheet = [[UIActionSheet alloc] initWithTitle:[self.options valueForKey:@"title"] delegate:self cancelButtonTitle:[self.options valueForKey:@"cancelButtonTitle"] destructiveButtonTitle:nil otherButtonTitles:[self.options valueForKey:@"takePhotoButtonTitle"], [self.options valueForKey:@"chooseFromLibraryButtonTitle"], nil];
    }
    else if (takePhotoHidden) {
        self.sheet = [[UIActionSheet alloc] initWithTitle:[self.options valueForKey:@"title"] delegate:self cancelButtonTitle:[self.options valueForKey:@"cancelButtonTitle"] destructiveButtonTitle:nil otherButtonTitles:[self.options valueForKey:@"chooseFromLibraryButtonTitle"], nil];
    }
    else if (chooseFromLibraryHidden) {
        self.sheet = [[UIActionSheet alloc] initWithTitle:[self.options valueForKey:@"title"] delegate:self cancelButtonTitle:[self.options valueForKey:@"cancelButtonTitle"] destructiveButtonTitle:nil otherButtonTitles:[self.options valueForKey:@"takePhotoButtonTitle"], nil];
    }

    // Add custom buttons to action sheet
    if([self.options objectForKey:@"customButtons"] && [[self.options objectForKey:@"customButtons"] isKindOfClass:[NSDictionary class]]){
        self.customButtons = [self.options objectForKey:@"customButtons"];
        for (NSString *key in self.customButtons) {
            [self.sheet addButtonWithTitle:key];
        }
    }

    dispatch_async(dispatch_get_main_queue(), ^{
        UIViewController *root = [[[[UIApplication sharedApplication] delegate] window] rootViewController];
        [self.sheet showInView:root.view];
    });

}

- (void)actionSheet:(UIActionSheet *)actionSheet clickedButtonAtIndex:(NSInteger)buttonIndex
{
    NSString *buttonTitle = [actionSheet buttonTitleAtIndex:buttonIndex];

    if ([buttonTitle isEqualToString:[self.options valueForKey:@"cancelButtonTitle"]]) {
        self.callback(@[@"cancel"]); // Return callback for 'cancel' action (if is required)
        return;
    }

    // if button title is one of the keys in the customButtons dictionary return the value as a callback
    if ([self.customButtons objectForKey:buttonTitle]) {
        self.callback(@[[self.customButtons objectForKey:buttonTitle]]);
        return;
    }

    self.picker = [[UIImagePickerController alloc] init];
    if ([[self.options objectForKey:@"allowsEditing"] boolValue]) {
      self.picker.allowsEditing = true;
    }
    self.picker.modalPresentationStyle = UIModalPresentationCurrentContext;
    self.picker.delegate = self;

    if ([buttonTitle isEqualToString:[self.options valueForKey:@"takePhotoButtonTitle"]]) { // Take photo
        // Will crash if we try to use camera on the simulator
        #if TARGET_IPHONE_SIMULATOR
            NSLog(@"Camera not available on simulator");
            return;
        #else
            self.picker.sourceType = UIImagePickerControllerSourceTypeCamera;
        #endif
    }
    else if ([buttonTitle isEqualToString:[self.options valueForKey:@"chooseFromLibraryButtonTitle"]]) { // Choose from library
        self.picker.sourceType = UIImagePickerControllerSourceTypePhotoLibrary;
    }

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
        [[NSFileManager defaultManager] createDirectoryAtPath:newPath
                                  withIntermediateDirectories:YES
                                                   attributes:nil
                                                        error:&error];
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

    NSData *data = UIImageJPEGRepresentation(image, [[self.options valueForKey:@"quality"] floatValue]);

    BOOL BASE64 = [[self.options valueForKey:@"returnBase64Image"] boolValue];
    BOOL returnOrientation = [self.options[@"returnIsVertical"] boolValue];

    NSMutableArray *response = [[NSMutableArray alloc] init];

    if (BASE64) {
        NSString *dataString = [data base64EncodedStringWithOptions:0];
        [response addObjectsFromArray : @[@"data", dataString]];
    }
    else {
        [data writeToFile:path atomically:YES];
        NSString *fileURL = [[NSURL fileURLWithPath:path] absoluteString];
        // if storage options skipBackup set to true then set flag to skip icloud backup
        if ([[storageOptions objectForKey:@"skipBackup"] boolValue]) {
            [self addSkipBackupAttributeToItemAtPath:path];
        }
        [response addObjectsFromArray : @[@"uri", fileURL]];
    }

    if (returnOrientation) { // Return image orientation if desired
        NSString *vertical = (image.size.width < image.size.height) ? @"true" : @"false";
        [response addObject : vertical];
    }

    self.callback(response);
}

- (UIImage*)downscaleImageIfNecessary:(UIImage*)image maxWidth:(float)maxWidth maxHeight:(float)maxHeight
{
    UIImage* newImage = image;

    //Nothing to do here?
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

- (void)imagePickerControllerDidCancel:(UIImagePickerController *)picker
{
    dispatch_async(dispatch_get_main_queue(), ^{
        [picker dismissViewControllerAnimated:YES completion:nil];
    });

    self.callback(@[@"cancel"]);
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
