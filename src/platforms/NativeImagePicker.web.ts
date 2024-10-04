/* eslint-disable @typescript-eslint/ban-types */
export interface Spec {
  launchCamera(options: Object, callback: () => void): void;
  launchImageLibrary(options: Object, callback: () => void): void;
}
