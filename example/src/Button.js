import React from 'react';
import {StyleSheet, View, Button as RNButton} from 'react-native';

export function Button({title, onPress, color}) {
  return (
    <View style={styles.container}>
      <RNButton title={title} onPress={onPress} color={color} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    marginVertical: 8,
    marginHorizontal: 8,
  },
});
