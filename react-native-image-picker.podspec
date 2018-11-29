require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name         = package['name']
  s.version      = package['version']
  s.summary      = package['description']

  s.authors      = { 'Marc Shilling' => 'marcshilling@gmail.com' }
  s.homepage     = package['homepage']
  s.license      = package['license']
  s.platform     = :ios, "7.0"

  s.source       = { :git => "https://github.com/marcshilling/react-native-image-picker" }
  s.source_files  = "ios/*.{h,m}"

  s.dependency 'React'
end
