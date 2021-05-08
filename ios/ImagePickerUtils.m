#import "ImagePickerUtils.h"
#import <CoreServices/CoreServices.h>
#import <PhotosUI/PhotosUI.h>

@implementation ImagePickerUtils

+ (void) setupPickerFromOptions:(UIImagePickerController *)picker options:(NSDictionary *)options target:(RNImagePickerTarget)target
{
    if (target == camera) {
        picker.sourceType = UIImagePickerControllerSourceTypeCamera;

        if ([options[@"cameraType"] isEqualToString:@"front"]) {
            picker.cameraDevice = UIImagePickerControllerCameraDeviceFront;
        } else {
            picker.cameraDevice = UIImagePickerControllerCameraDeviceRear;
        }
    }
    else {
        picker.sourceType = UIImagePickerControllerSourceTypePhotoLibrary;
    }

    if ([options[@"mediaType"] isEqualToString:@"video"]) {

        if ([options[@"videoQuality"] isEqualToString:@"high"]) {
            picker.videoQuality = UIImagePickerControllerQualityTypeHigh;
        }
        else if ([options[@"videoQuality"] isEqualToString:@"low"]) {
            picker.videoQuality = UIImagePickerControllerQualityTypeLow;
        }
        else {
            picker.videoQuality = UIImagePickerControllerQualityTypeMedium;
        }
        
        if (options[@"durationLimit"] > 0) {
            picker.videoMaximumDuration = [options[@"durationLimit"] doubleValue];
        }

        picker.mediaTypes = @[(NSString *)kUTTypeMovie];
    }
    else {
        picker.mediaTypes = @[(NSString *)kUTTypeImage];
    }
    
    picker.modalPresentationStyle = UIModalPresentationCurrentContext;
}

+ (PHPickerConfiguration *)makeConfigurationFromOptions:(NSDictionary *)options API_AVAILABLE(ios(14))
{
    PHPickerConfiguration *configuration = [[PHPickerConfiguration alloc] init];
    configuration.preferredAssetRepresentationMode = PHPickerConfigurationAssetRepresentationModeCurrent;

    if ([options[@"mediaType"] isEqualToString:@"video"]) {
        configuration.filter = [PHPickerFilter videosFilter];
    }
    else {
        configuration.filter = [PHPickerFilter imagesFilter];
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

@end
