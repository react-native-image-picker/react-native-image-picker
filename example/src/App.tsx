import * as React from 'react';
import {
  Image,
  Platform,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  View,
  TextInput,
  Text,
} from 'react-native';
import {DemoButton, DemoResponse, DemoTitle} from './components';

import * as ImagePicker from 'react-native-image-picker';

/* toggle includeExtra */
const includeExtra = true;

export default function App() {
  const [response, setResponse] = React.useState<any>(null);
  const [assets, setAssets] = React.useState<ImagePicker.Asset[]>([]);
  const [cameraPackage, setCameraPackage] = React.useState('');

  const preselectedAssetIds = assets.flatMap(({assetId}) =>
    assetId ? [assetId] : [],
  );

  // Items kept from an earlier pick are listed in selectedAssetIds only, so their data comes from that pick.
  const onResponse = React.useCallback(
    (next: ImagePicker.ImagePickerResponse) => {
      setResponse(next);

      const picked = next.assets;
      const selected = next.selectedAssetIds;

      if (!picked) return;

      setAssets(previous =>
        selected
          ? selected.flatMap(
              id =>
                [...previous, ...picked].find(({assetId}) => assetId === id) ??
                [],
            )
          : picked,
      );
    },
    [],
  );

  const onButtonPress = React.useCallback(
    (
      type: 'capture' | 'library',
      options: ImagePicker.ImageLibraryOptions | ImagePicker.CameraOptions,
    ) => {
      const finalOptions = {
        ...options,
        ...(Platform.OS === 'android' &&
          cameraPackage && {
            androidCameraPackage: cameraPackage,
          }),
      };

      if (type === 'capture') {
        ImagePicker.launchCamera(finalOptions, onResponse);
      } else {
        ImagePicker.launchImageLibrary(finalOptions, onResponse);
      }
    },
    [cameraPackage, onResponse],
  );

  return (
    <SafeAreaView style={styles.container}>
      <DemoTitle>🌄 React Native Image Picker</DemoTitle>
      <ScrollView>
        <View style={styles.buttonContainer}>
          {actions.map(({title, type, options}) => (
            <DemoButton
              key={title}
              onPress={() => onButtonPress(type, options)}>
              {title}
            </DemoButton>
          ))}
          <DemoButton
            onPress={() =>
              onButtonPress('library', {
                selectionLimit: 0,
                mediaType: 'mixed',
                includeExtra,
                preselectedAssetIds,
              })
            }>
            {'Select again\n(keep selection)'}
          </DemoButton>
        </View>

        {Platform.OS === 'android' && (
          <View style={styles.inputContainer}>
            <Text style={styles.label}>Android Camera Package Name</Text>
            <TextInput
              style={styles.input}
              placeholder="e.g. net.sourceforge.opencamera"
              value={cameraPackage}
              onChangeText={setCameraPackage}
            />
          </View>
        )}

        <DemoResponse>{response}</DemoResponse>

        {assets.map(({uri}) => (
          <View key={uri} style={styles.imageContainer}>
            <Image
              resizeMode="cover"
              resizeMethod="scale"
              style={styles.image}
              source={{uri}}
            />
          </View>
        ))}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: 'aliceblue',
  },
  inputContainer: {
    padding: 16,
  },
  label: {
    fontSize: 14,
    fontWeight: '500',
    marginBottom: 6,
  },
  input: {
    height: 40,
    borderColor: '#ccc',
    borderWidth: 1,
    borderRadius: 6,
    paddingHorizontal: 10,
    backgroundColor: '#fff',
  },
  buttonContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginVertical: 8,
  },
  imageContainer: {
    marginVertical: 24,
    alignItems: 'center',
  },
  image: {
    width: 200,
    height: 200,
  },
});

interface Action {
  title: string;
  type: 'capture' | 'library';
  options: ImagePicker.CameraOptions | ImagePicker.ImageLibraryOptions;
}

const actions: Action[] = [
  {
    title: 'Take Image',
    type: 'capture',
    options: {
      saveToPhotos: true,
      mediaType: 'photo',
      includeBase64: false,
      includeExtra,
    },
  },
  {
    title: 'Select Image',
    type: 'library',
    options: {
      selectionLimit: 0,
      mediaType: 'photo',
      includeBase64: false,
      includeExtra,
    },
  },
  {
    title: 'Take Video',
    type: 'capture',
    options: {
      saveToPhotos: true,
      formatAsMp4: true,
      mediaType: 'video',
      includeExtra,
    },
  },
  {
    title: 'Select Video',
    type: 'library',
    options: {
      selectionLimit: 0,
      mediaType: 'video',
      formatAsMp4: true,
      includeExtra,
    },
  },
  {
    title: 'Select Image or Video\n(mixed)',
    type: 'library',
    options: {
      selectionLimit: 0,
      mediaType: 'mixed',
      includeExtra,
    },
  },
];

if (Platform.OS === 'ios') {
  actions.push({
    title: 'Take Image or Video\n(mixed)',
    type: 'capture',
    options: {
      saveToPhotos: true,
      mediaType: 'mixed',
      includeExtra,
      presentationStyle: 'fullScreen',
    },
  });
}
