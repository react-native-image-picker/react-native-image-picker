#import "UIImagePickerManager.h"

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

@end

@implementation UIImagePickerManager

RCT_EXPORT_MODULE();

RCT_EXPORT_METHOD(showImagePicker:(NSString *)title callback:(RCTResponseSenderBlock)callback)
{
  self.callback = callback; // Save the callback so we can use it from the delegate methods
  
  self.sheet = [[UIActionSheet alloc] initWithTitle:title delegate:self cancelButtonTitle:@"Cancel" destructiveButtonTitle:nil otherButtonTitles:@"Take Photo...", @"Choose from Library...", nil];
  
  dispatch_async(dispatch_get_main_queue(), ^{
    UIViewController *root = [[[[UIApplication sharedApplication] delegate] window] rootViewController];
    [self.sheet showInView:root.view];
  });
}

- (void)actionSheet:(UIActionSheet *)actionSheet clickedButtonAtIndex:(NSInteger)buttonIndex
{
  if (buttonIndex == 2) { // Cancel
    return;
  }

  self.picker = [[UIImagePickerController alloc] init];
  self.picker.modalPresentationStyle = UIModalPresentationCurrentContext;
  self.picker.delegate = self;

  if (buttonIndex == 0) { // Take photo
    self.picker.sourceType = UIImagePickerControllerSourceTypeCamera;
    #if TARGET_IPHONE_SIMULATOR
      return; // Will crash if we try to use camera on the simulator, so just do nothing
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
  
  NSString *imageURL = [((NSURL*)info[UIImagePickerControllerReferenceURL]) absoluteString];
  if (imageURL) { // Image chosen from library, send
    self.callback(@[@NO, imageURL]);
  }
  else {
    UIImage *image = info[UIImagePickerControllerOriginalImage];
    NSData *imageData = UIImageJPEGRepresentation(image, COMPRESSION_QUALITY);
    NSString *dataString = [imageData base64EncodedStringWithOptions:0];
    self.callback(@[@YES, dataString]);
  }
}

@end
