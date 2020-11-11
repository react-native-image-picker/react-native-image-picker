import {NativeModules} from 'react-native';

// Mock the ImagePickerManager native module to allow us to unit test the JavaScript code
NativeModules.ImagePickerManager = {
  launchCamera: jest.fn(),
  launchImageLibrary: jest.fn(),
};
