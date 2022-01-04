#import "ImagePickerManager.h"
#import <Photos/Photos.h>

@class PHPickerConfiguration;

@interface ImagePickerUtils : NSObject

+ (BOOL)isSimulator;

+ (void)setupPickerFromOptions:(UIImagePickerController *)picker options:(NSDictionary *)options target:(RNImagePickerTarget)target;

+ (PHPickerConfiguration *)makeConfigurationFromOptions:(NSDictionary *)options target:(RNImagePickerTarget)target API_AVAILABLE(ios(14));

+ (NSString*)getFileType:(NSData*)imageData;

+ (UIImage*)resizeImage:(UIImage*)image maxWidth:(float)maxWidth maxHeight:(float)maxHeight;

+ (CGSize)getVideoDimensionsFromUrl:(NSURL *)url;

+ (NSString *) getFileTypeFromUrl:(NSURL *)url;

+ (NSString *) getFileSizeFromUrl:(NSURL *)url;

+ (PHAsset *)fetchPHAssetOnIOS13:(NSDictionary<NSString *,id> *)info;
    
@end
