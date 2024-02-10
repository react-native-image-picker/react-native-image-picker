#import <React/RCTBridgeModule.h>
#import <UIKit/UIKit.h>
#import <React/RCTConvert.h>

typedef NS_ENUM(NSInteger, RNImagePickerTarget) {
  camera = 1,
  library
};

@implementation RCTConvert(PresentationStyle)

// see: https://developer.apple.com/documentation/uikit/uimodalpresentationstyle?language=objc
RCT_ENUM_CONVERTER(
    UIModalPresentationStyle,
    (@{
      @"currentContext": @(UIModalPresentationCurrentContext),
      @"fullScreen": @(UIModalPresentationFullScreen),
      @"pageSheet": @(UIModalPresentationPageSheet),
      @"formSheet": @(UIModalPresentationFormSheet),
      @"popover": @(UIModalPresentationPopover),
      @"overFullScreen": @(UIModalPresentationOverFullScreen),
      @"overCurrentContext": @(UIModalPresentationOverCurrentContext)
    }),
    UIModalPresentationCurrentContext,
    integerValue)

@end

#ifdef RCT_NEW_ARCH_ENABLED

#import "RNImagePickerSpec.h"
@interface ImagePickerManager : NSObject <NativeImagePickerSpec>
@end

#else

@interface ImagePickerManager : NSObject <RCTBridgeModule>
@end

#endif
