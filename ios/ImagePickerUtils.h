#import "ImagePickerManager.h"

@interface ImagePickerUtils : NSObject

+ (BOOL)isSimulator;

+ (void)setupPickerFromOptions:(UIImagePickerController *)picker options:(NSDictionary *)options target:(RNImagePickerTarget)target;

+ (NSString*)getFileType:(NSData*)imageData;

+ (UIImage*)resizeImage:(UIImage*)image maxWidth:(float)maxWidth maxHeight:(float)maxHeight;
    
@end
