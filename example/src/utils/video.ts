import {Platform} from 'react-native';
import type {Asset} from 'react-native-image-picker';

export function isWebVideo(asset: Asset) {
  return Platform.OS === 'web' && asset.uri?.startsWith('data:video/');
}
