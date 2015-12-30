Pod::Spec.new do |s|
  s.name         = "react-native-image-picker"
  s.version      = "0.9.0"
  s.summary      = "A React Native module that allows you to use the native UIImagePickerController UI to select a photo from the device library or directly from the camera"

  s.homepage     = "https://github.com/marcshilling/react-native-image-picker"

  s.license      = "MIT"
  s.platform     = :ios, "8.0"

  s.source       = { :git => "https://github.com/marcshilling/react-native-image-picker" }

  s.source_files  = "ios/*.{h,m}"

  s.dependency 'React'
end
