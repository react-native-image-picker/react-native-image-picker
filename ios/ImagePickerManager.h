#import "RCTBridgeModule.h"
#import <UIKit/UIKit.h>

typedef NS_ENUM(NSInteger, RNImagePickerTarget) {
  RNImagePickerTargetCamera = 1,
  RNImagePickerTargetLibrarySingleImage,
  RNImagePickerTargetDocument
};

@interface ImagePickerManager : NSObject <RCTBridgeModule, UINavigationControllerDelegate, UIActionSheetDelegate, UIImagePickerControllerDelegate>

@end
