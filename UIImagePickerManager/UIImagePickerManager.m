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

- (instancetype)init {
    
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
            @"quality" : @0.2 // 1.0 best to 0.0 worst
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
        [root presentViewController:self.picker animated:YES completion:nil];
    });
    
}

- (void)imagePickerController:(UIImagePickerController *)picker didFinishPickingMediaWithInfo:(NSDictionary *)info
{
    dispatch_async(dispatch_get_main_queue(), ^{
        [picker dismissViewControllerAnimated:YES completion:nil];
    });
    
    /* Getting all bools together */
    BOOL BASE64 = [[self.options valueForKey:@"returnBase64Image"] boolValue];
    BOOL returnOrientation = [self.options[@"returnIsVertical"] boolValue];
    
    /* Picked Image */
    UIImage *image = [info objectForKey:UIImagePickerControllerOriginalImage];
    
    /* If not Base64 then return URL */
    if (!BASE64) {
        
        if (self.picker.sourceType == UIImagePickerControllerSourceTypeCamera) {
            if (image != nil)
            {
                /* creating a temp url to be passed */
                NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory,
                                                                     NSUserDomainMask, YES);
                NSString *ImageUUID = [[NSUUID UUID] UUIDString];
                NSString *ImageName = [ImageUUID stringByAppendingString:@".jpg"];
                NSString *documentsDirectory = [paths objectAtIndex:0];
                
                // Rotate the image for upload to web
                image = [self fixOrientation:image];
                
                // This will be the URL
                NSString* path = [documentsDirectory stringByAppendingPathComponent:ImageName];
                
                NSData *data = UIImageJPEGRepresentation(image, [[self.options valueForKey:@"quality"] floatValue]);
                
                /* Write to the disk */
                [data writeToFile:path atomically:YES];
                
                self.callback(@[@"uri", path]);
            }
        }
        else {
            // Get URL for the image fetched from the Photos
            NSString *imageURL = [((NSURL*)info[UIImagePickerControllerReferenceURL]) absoluteString];
            if (imageURL) { // Image chosen from library, send
                self.callback(@[@"uri", imageURL]);
            }
        }
    }
    else {
        UIImage *image = info[UIImagePickerControllerOriginalImage];
        NSData *imageData = UIImageJPEGRepresentation(image, [[self.options valueForKey:@"quality"] floatValue]);
        NSString *dataString = [imageData base64EncodedStringWithOptions:0];
        
        if (returnOrientation) { // Return image orientation if desired
            NSString *vertical = (image.size.width < image.size.height) ? @"true" : @"false";
            self.callback(@[@"data", dataString, vertical]);
        }
        else {
            self.callback(@[@"data", dataString]);
        }
    }
}

- (void)imagePickerControllerDidCancel:(UIImagePickerController *)picker
{
    dispatch_async(dispatch_get_main_queue(), ^{
        [picker dismissViewControllerAnimated:YES completion:nil];
    });
    
    self.callback(@[@"cancel"]);
}

- (UIImage *)fixOrientation:(UIImage *)srcImg {
    if (srcImg.imageOrientation == UIImageOrientationUp) return srcImg;
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
    
    CGContextRef ctx = CGBitmapContextCreate(NULL, srcImg.size.width, srcImg.size.height,
                                             CGImageGetBitsPerComponent(srcImg.CGImage), 0,
                                             CGImageGetColorSpace(srcImg.CGImage),
                                             CGImageGetBitmapInfo(srcImg.CGImage));
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

@end
