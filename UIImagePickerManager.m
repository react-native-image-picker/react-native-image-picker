#import "UIImagePickerManager.h"
#import "RCTConvert.h"

/*
 From 0.0 (worst quality image) to 1.0 (best)
 Play around with this to suit your app's needs. I found that a 1.0 compressed image takes
 a very long time to load into a React Native <Image>
 */
#define COMPRESSION_QUALITY 0.2

@interface UIImagePickerManager ()

@property (nonatomic, strong) UIActionSheet *sheet;
@property (nonatomic, strong) UIImagePickerController *picker;
@property (nonatomic, strong) RCTResponseSenderBlock callback;
@property (nonatomic, strong) NSDictionary *defaultOptions;
@property (nonatomic, retain) NSMutableDictionary *options;

@end

@implementation UIImagePickerManager

RCT_EXPORT_MODULE();

- (instancetype)init {
    
    if (self = [super init]) {
        
        self.defaultOptions = @{
                                @"title": @"Select a Photo",
                                @"cancelButtonTitle": @"Cancel",
                                @"takePhotoButtonTitle": @"Take Photo...",
                                @"chooseFromLibraryButtonTitle": @"Choose from Library...",
                                @"returnBase64Image" : @NO, // Only return base64 encoded version of the image
                                @"returnIsVertical" : @NO // If returning base64 image, return the orientation too
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
    self.sheet = [[UIActionSheet alloc] initWithTitle:[self.options valueForKey:@"title"] delegate:self cancelButtonTitle:[self.options valueForKey:@"cancelButtonTitle"] destructiveButtonTitle:nil otherButtonTitles:[self.options valueForKey:@"takePhotoButtonTitle"], [self.options valueForKey:@"chooseFromLibraryButtonTitle"], nil];
    
    dispatch_async(dispatch_get_main_queue(), ^{
        UIViewController *root = [[[[UIApplication sharedApplication] delegate] window] rootViewController];
        [self.sheet showInView:root.view];
    });
}

- (void)actionSheet:(UIActionSheet *)actionSheet clickedButtonAtIndex:(NSInteger)buttonIndex
{
    if (buttonIndex == 2) { // Cancel
        self.callback(@[@"cancel"]); // Return callback for 'cancel' action (if is required)
        return;
    }
    
    self.picker = [[UIImagePickerController alloc] init];
    self.picker.modalPresentationStyle = UIModalPresentationCurrentContext;
    self.picker.delegate = self;
    
    if (buttonIndex == 0) { // Take photo
        // Will crash if we try to use camera on the simulator
#if TARGET_IPHONE_SIMULATOR
        NSLog(@"Camera not available on simulator");
        return;
#else
        self.picker.sourceType = UIImagePickerControllerSourceTypeCamera;
#endif
    }
    else if (buttonIndex == 1) { // Choose from library
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
                
                // This will be the URL
                NSString* path = [documentsDirectory stringByAppendingPathComponent:ImageName];
                
                NSData *data = UIImageJPEGRepresentation(image, COMPRESSION_QUALITY);
                
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
        NSData *imageData = UIImageJPEGRepresentation(image, COMPRESSION_QUALITY);
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
@end
