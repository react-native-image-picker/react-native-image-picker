import {NativeModules} from 'react-native';
import {ImagePickerResponse, CameraOptions, Callback} from './src/types';

// Mock the ImagePickerManager native module to allow us to unit test the JavaScript code
NativeModules.ImagePickerManager = {
  launchCamera: jest.fn(),
  launchImageLibrary: jest.fn(),
};
