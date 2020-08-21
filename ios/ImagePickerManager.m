#import "ImagePickerManager.h"
#import "ImagePickerUtils.h"
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
    dispatch_async(dispatch_get_main_queue(), ^{
        [self launchImagePicker:camera options:options callback:callback];
    });
}

RCT_EXPORT_METHOD(launchImageLibrary:(NSDictionary *)options callback:(RCTResponseSenderBlock)callback)
{
    dispatch_async(dispatch_get_main_queue(), ^{
        [self launchImagePicker:library options:options callback:callback];
    });
}

- (void)launchImagePicker:(RNImagePickerTarget)target options:(NSDictionary *)options callback:(RCTResponseSenderBlock)callback
{
    self.callback = callback;
    
    if (target == camera && [ImagePickerUtils isSimulator]) {
        self.callback(@[@{@"error": @"Camera not available on simulator"}]);
        return;
    }
    
    self.options = options;
    self.picker = [[UIImagePickerController alloc] init];
    
    [ImagePickerUtils setupPickerFromOptions:self.picker options:self.options target:target];
    self.picker.delegate = self;
    
    if (target == camera) {
        [self checkCameraPermissions:^(BOOL granted) {
            if (!granted) {
                self.callback(@[@{@"error": @"Camera permissions not granted"}]);
                return;
            }
            [self showPickerViewController];
        }];
    }
    else {
        if (@available(iOS 11.0, *)) {
            [self showPickerViewController];
        } else {
            [self checkPhotosPermissions:^(BOOL granted) {
                if (!granted) {
                    self.callback(@[@{@"error": @"Photo library permissions not granted"}]);
                    return;
                }
                [self showPickerViewController];
            }];
        }
    }
}

- (void) showPickerViewController {
    dispatch_async(dispatch_get_main_queue(), ^{
        UIViewController *root = RCTPresentedViewController();
        [root presentViewController:self.picker animated:YES completion:nil];
    });
}

- (void)imagePickerController:(UIImagePickerController *)picker didFinishPickingMediaWithInfo:(NSDictionary<NSString *,id> *)info
{
    dispatch_block_t dismissCompletionBlock = ^{
        
        if ([[info objectForKey:UIImagePickerControllerMediaType] isEqualToString: (NSString *)kUTTypeImage]) {
            [self onImageObtained:info];
        } else {
            [self onVideoObtained:info];
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

// Handle orientation
- (void)onImageObtained:(NSDictionary<NSString *,id> *)info {
    NSURL *imageURL = [info objectForKey:UIImagePickerControllerReferenceURL];
    self.response = [[NSMutableDictionary alloc] init];
    UIImage *image = [info objectForKey:UIImagePickerControllerOriginalImage];

    NSData *data;
    NSString *fileType = [ImagePickerUtils getFileType:[NSData dataWithContentsOfURL:imageURL]];
    
    if (![fileType isEqualToString:@"gif"]) {
        image = [ImagePickerUtils resizeImage:image maxWidth:[self.options[@"maxWidth"] floatValue] maxHeight:[self.options[@"maxHeight"] floatValue]];
    }
    
    if ([fileType isEqualToString:(NSString *)@"jpg"]) {
        data = UIImageJPEGRepresentation(image, [[self.options objectForKey:@"quality"] floatValue]);
    } else if ([fileType isEqualToString:(NSString *)@"png"]) {
        data = UIImagePNGRepresentation(image);
    } else {
        data = [NSData dataWithContentsOfURL:imageURL];
    }

    [self.response setObject:[@"image/" stringByAppendingString:fileType] forKey:@"type"];
    
    NSString *fileName = [self getImageFileName:fileType];
    NSString *path = [[NSTemporaryDirectory() stringByStandardizingPath] stringByAppendingPathComponent:fileName];
    [data writeToFile:path atomically:YES];

    if (![[self.options objectForKey:@"noData"] boolValue]) {
        NSString *dataString = [data base64EncodedStringWithOptions:0];
        [self.response setObject:dataString forKey:@"data"];
    }

    NSURL *fileURL = [NSURL fileURLWithPath:path];
    self.response[@"uri"] = [fileURL absoluteString];

    NSNumber *fileSizeValue = nil;
    NSError *fileSizeError = nil;
    [fileURL getResourceValue:&fileSizeValue forKey:NSURLFileSizeKey error:&fileSizeError];
    if (fileSizeValue){
        self.response[@"fileSize"] = fileSizeValue;
    }

    self.response[@"fileName"] = fileName;
    self.response[@"width"] = @(image.size.width);
    self.response[@"height"] = @(image.size.height);
    self.callback(@[self.response]);
    
}

- (void)onVideoObtained:(NSDictionary<NSString *,id> *)info {
    NSString *fileName = [info[UIImagePickerControllerMediaURL] lastPathComponent];
    NSString *path = [[NSTemporaryDirectory() stringByStandardizingPath] stringByAppendingPathComponent:fileName];

    self.response = [[NSMutableDictionary alloc] init];
    
    NSURL *videoRefURL = info[UIImagePickerControllerReferenceURL];
    NSURL *videoURL = info[UIImagePickerControllerMediaURL];
    NSURL *videoDestinationURL = [NSURL fileURLWithPath:path];

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
    self.callback(@[self.response]);
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

- (NSString*) getImageFileName:(NSString*)fileType {
    NSString *fileName = [[NSUUID UUID] UUIDString];
    fileName = [fileName stringByAppendingString:@"."];
    return [fileName stringByAppendingString:fileType];
}
@end

