import {launchCamera, launchImageLibrary} from '../index';
import {NativeModules} from 'react-native';
import {Callback, CameraOptions} from '../types';

describe('react-native-image-picker', () => {
  it('mocks the image picker with didCancel being true', (done) => {
    NativeModules.ImagePickerManager.launchCamera.mockImplementation(
      (props: CameraOptions, callback: Callback) => {
        callback({
          didCancel: true,
        });
      },
    );

    launchCamera({mediaType: 'photo'}, ({didCancel}) => {
      expect(didCancel).toBeTruthy();
      done();
    });
  });
});
