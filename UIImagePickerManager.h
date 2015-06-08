#import "RCTBridgeModule.h"
#import <UIKit/UIKit.h>

@interface UIImagePickerManager : NSObject <RCTBridgeModule, UINavigationControllerDelegate, UIActionSheetDelegate, UIImagePickerControllerDelegate>

@end
