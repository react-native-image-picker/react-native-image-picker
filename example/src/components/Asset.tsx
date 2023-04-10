import * as React from 'react';
import {Image, View, StyleSheet} from 'react-native';
import type {Asset} from 'react-native-image-picker';
import {isWebVideo} from '../utils/video';

interface Props {
  asset: Asset;
}

export function Asset({asset}: Props) {
  if (isWebVideo(asset)) {
    return (
      <View style={styles.assetContainer}>
        <video src={asset.uri} style={webVideoStyle} controls />
      </View>
    );
  }

  return (
    <View style={styles.assetContainer}>
      <Image
        resizeMode="cover"
        resizeMethod="scale"
        style={styles.asset}
        source={{uri: asset.uri}}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  assetContainer: {
    marginVertical: 24,
    alignItems: 'center',
  },
  asset: {
    width: 200,
    height: 200,
  },
});

const webVideoStyle = {
  maxHeight: 200,
};
