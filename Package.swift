// swift-tools-version: 6.0

import PackageDescription

// Swift Package Manager manifest for apps that React Native 0.88+ builds with SwiftPM
// (`npx react-native spm`). React Native links this package through
// `<app>/ios/build/generated/autolinking/libs/RNImagePicker`, so the relative paths below point at
// the app's prebuilt React Native and codegen packages. CocoaPods builds use the podspec.

let package = Package(
  name: "RNImagePicker",
  platforms: [.iOS("15.1")],
  products: [
    .library(name: "RNImagePicker", targets: ["RNImagePicker"]),
  ],
  dependencies: [
    .package(name: "ReactNative", path: "../../../../xcframeworks"),
    .package(name: "React-GeneratedCode", path: "../../../ios"),
  ],
  targets: [
    .target(
      name: "RNImagePicker",
      dependencies: [
        .product(name: "ReactHeaders", package: "ReactNative"),
        .product(name: "ReactNativeHeaders", package: "ReactNative"),
        .product(name: "ReactNativeDependenciesHeaders", package: "ReactNative"),
        .product(name: "ReactAppHeaders", package: "React-GeneratedCode"),
      ],
      path: "ios",
      exclude: ["RNImagePicker.xcodeproj"],
      resources: [.copy("PrivacyInfo.xcprivacy")],
      publicHeadersPath: ".",
      cSettings: [.define("RCT_NEW_ARCH_ENABLED", to: "1")],
      cxxSettings: [
        .define("RCT_NEW_ARCH_ENABLED", to: "1"),
        // React Native's prebuilt C++ is compiled with the same NDEBUG state.
        .define("DEBUG", .when(configuration: .debug)),
        .define("NDEBUG", .when(configuration: .release)),
      ],
      linkerSettings: [
        .linkedFramework("UIKit"),
        .linkedFramework("Photos"),
        .linkedFramework("PhotosUI"),
        .linkedFramework("AVFoundation"),
        .linkedFramework("CoreMedia"),
        .linkedFramework("MobileCoreServices"),
        .linkedFramework("UniformTypeIdentifiers"),
      ]
    ),
  ],
  cxxLanguageStandard: .cxx20
)
