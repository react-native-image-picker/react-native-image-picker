import * as React from 'react';
import {View, Text, StyleSheet, ViewStyle, TextStyle} from 'react-native';

interface Props {
  children: string;
}

export function DemoTitle({children}: Props) {
  return (
    <View style={styles.container}>
      <Text style={styles.text}>{children}</Text>
    </View>
  );
}

interface Styles {
  container: ViewStyle;
  text: TextStyle;
}

const styles = StyleSheet.create<Styles>({
  container: {
    width: '100%',
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 16,
    backgroundColor: 'steelblue',
  },
  text: {
    fontSize: 24,
    color: 'white',
  },
});
