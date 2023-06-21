import {TurboModuleRegistry} from 'react-native';
import type {TurboModule} from 'react-native/Libraries/TurboModule/RCTExport';

export interface Spec extends TurboModule {
  launchCamera(options: Object, callback: () => void): void;
  launchImageLibrary(options: Object, callback: () => void): void;
  updatePickerAccess(callback: () => void): void;
}

export default TurboModuleRegistry.get<Spec>('ImagePicker');
