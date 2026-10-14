#import <UIKit/UIKit.h>
#import <XCTest/XCTest.h>
#import <ImageIO/ImageIO.h>

#import <React/RCTLog.h>
#import <React/RCTRootView.h>

#import <react-native-image-picker/ImagePickerUtils.h>

#define TIMEOUT_SECONDS 600
#define TEXT_TO_LOOK_FOR @"Welcome to React"

@interface exampleTests : XCTestCase

@end

@implementation exampleTests

- (UIImage *)makeTestImage
{
  UIGraphicsBeginImageContextWithOptions(CGSizeMake(1, 1), YES, 1);
  [[UIColor redColor] setFill];
  UIRectFill(CGRectMake(0, 0, 1, 1));
  UIImage *image = UIGraphicsGetImageFromCurrentImageContext();
  UIGraphicsEndImageContext();
  return image;
}

- (void)testDetectsHeicImageData
{
  UIImage *image = [self makeTestImage];

  NSMutableData *heicData = [NSMutableData data];
  CGImageDestinationRef destination = CGImageDestinationCreateWithData(
      (__bridge CFMutableDataRef)heicData, CFSTR("public.heic"), 1, NULL);
  if (destination == NULL) {
    XCTFail(@"HEIC encoding is unavailable");
    return;
  }

  CGImageDestinationAddImage(destination, image.CGImage, NULL);
  BOOL finalized = CGImageDestinationFinalize(destination);
  CFRelease(destination);

  XCTAssertTrue(finalized);
  XCTAssertEqualObjects([ImagePickerUtils getFileType:heicData], @"heic");
}

- (void)testPreservesExistingImageTypeDetection
{
  UIImage *image = [self makeTestImage];
  NSData *gifData = [[NSData alloc]
      initWithBase64EncodedString:@"R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw=="
      options:0];

  XCTAssertEqualObjects([ImagePickerUtils getFileType:UIImageJPEGRepresentation(image, 1)], @"jpg");
  XCTAssertEqualObjects([ImagePickerUtils getFileType:UIImagePNGRepresentation(image)], @"png");
  XCTAssertEqualObjects([ImagePickerUtils getFileType:gifData], @"gif");
}

- (BOOL)findSubviewInView:(UIView *)view matching:(BOOL (^)(UIView *view))test
{
  if (test(view)) {
    return YES;
  }
  for (UIView *subview in [view subviews]) {
    if ([self findSubviewInView:subview matching:test]) {
      return YES;
    }
  }
  return NO;
}

- (void)testRendersWelcomeScreen
{
  UIViewController *vc = [[[RCTSharedApplication() delegate] window] rootViewController];
  NSDate *date = [NSDate dateWithTimeIntervalSinceNow:TIMEOUT_SECONDS];
  BOOL foundElement = NO;

  __block NSString *redboxError = nil;
#ifdef DEBUG
  RCTSetLogFunction(
      ^(RCTLogLevel level, RCTLogSource source, NSString *fileName, NSNumber *lineNumber, NSString *message) {
        if (level >= RCTLogLevelError) {
          redboxError = message;
        }
      });
#endif

  while ([date timeIntervalSinceNow] > 0 && !foundElement && !redboxError) {
    [[NSRunLoop mainRunLoop] runMode:NSDefaultRunLoopMode beforeDate:[NSDate dateWithTimeIntervalSinceNow:0.1]];
    [[NSRunLoop mainRunLoop] runMode:NSRunLoopCommonModes beforeDate:[NSDate dateWithTimeIntervalSinceNow:0.1]];

    foundElement = [self findSubviewInView:vc.view
                                  matching:^BOOL(UIView *view) {
                                    if ([view.accessibilityLabel isEqualToString:TEXT_TO_LOOK_FOR]) {
                                      return YES;
                                    }
                                    return NO;
                                  }];
  }

#ifdef DEBUG
  RCTSetLogFunction(RCTDefaultLogFunction);
#endif

  XCTAssertNil(redboxError, @"RedBox error: %@", redboxError);
  XCTAssertTrue(foundElement, @"Couldn't find element with text '%@' in %d seconds", TEXT_TO_LOOK_FOR, TIMEOUT_SECONDS);
}

@end
