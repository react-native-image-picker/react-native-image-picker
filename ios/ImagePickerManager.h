#import <React/RCTBridgeModule.h>
#import <UIKit/UIKit.h>

typedef NS_ENUM(NSInteger, RNImagePickerTarget) {
  camera = 1,
  library
};

@interface ImagePickerManager : NSObject <RCTBridgeModule>

@end
