#import "ImagePickerManager.h"

@class PHPickerConfiguration;

@interface ImagePickerUtils : NSObject

+ (BOOL)isSimulator;

+ (void)setupPickerFromOptions:(UIImagePickerController *)picker options:(NSDictionary *)options target:(RNImagePickerTarget)target;

+ (PHPickerConfiguration *)makeConfigurationFromOptions:(NSDictionary *)options target:(RNImagePickerTarget)target API_AVAILABLE(ios(14));

+ (NSString *)getFileType:(NSData *)imageData isCamera:(Boolean)isCamera;

+ (UIImage *)resizeImage:(UIImage *)image maxWidth:(float)maxWidth maxHeight:(float)maxHeight;

@end
