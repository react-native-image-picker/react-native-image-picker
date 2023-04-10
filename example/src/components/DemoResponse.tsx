import * as React from 'react';
import {Text, StyleSheet, ViewStyle, TextStyle, ScrollView} from 'react-native';
import type {ImagePickerResponse} from 'react-native-image-picker';

interface Props {
  children: ImagePickerResponse | null;
}

export function DemoResponse({children}: Props) {
  if (children == null) {
    return null;
  }

  return (
    <ScrollView style={styles.container}>
      <Text style={styles.text}>{JSON.stringify(children, null, 2)}</Text>
    </ScrollView>
  );
}

interface Styles {
  container: ViewStyle;
  text: TextStyle;
}

const styles = StyleSheet.create<Styles>({
  container: {
    backgroundColor: '#dcecfb',
    marginVertical: 8,
    padding: 8,
    borderRadius: 8,
    maxHeight: 200,
  },
  text: {
    color: 'black',
  },
});
