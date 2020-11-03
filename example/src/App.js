import React from 'react';
import {
  StyleSheet,
  Text,
  View,
  Image,
  ScrollView,
  Platform,
  SafeAreaView,
} from 'react-native';
import {
  request,
  requestMultiple,
  Permission,
  PermissionStatus,
  PERMISSIONS,
  checkMultiple,
} from 'react-native-permissions';
import {Button} from './Button';

import * as ImagePicker from '../../src';

const PERMISSIONS_TO_REQUEST = Platform.select({
  ios: [PERMISSIONS.IOS.CAMERA],
  default: [
    PERMISSIONS.ANDROID.CAMERA,
    PERMISSIONS.ANDROID.WRITE_EXTERNAL_STORAGE,
  ],
});

export default function App() {
  const [response, setResponse] = React.useState(null);
  const [permission, setPermission] = React.useState('unavailable');

  React.useEffect(() => {
    checkMultiple(PERMISSIONS_TO_REQUEST).then((status) => {
      setPermission(JSON.stringify(status, null, 2));
    });
  }, []);

  return (
    <SafeAreaView>
      <ScrollView>
        <Button
          title="Take image"
          onPress={() =>
            ImagePicker.launchCamera(
              {
                mediaType: 'photo',
                includeBase64: false,
                maxHeight: 200,
                maxWidth: 200,
              },
              (response) => {
                setResponse(response);
              },
            )
          }
        />

        <Button
          title="Select image"
          onPress={() =>
            ImagePicker.launchImageLibrary(
              {
                mediaType: 'photo',
                includeBase64: false,
                maxHeight: 200,
                maxWidth: 200,
              },
              (response) => {
                setResponse(response);
              },
            )
          }
        />

        <Button
          title="Take video"
          onPress={() =>
            ImagePicker.launchCamera({mediaType: 'video'}, (response) => {
              setResponse(response);
            })
          }
        />

        <Button
          title="Select video"
          onPress={() =>
            ImagePicker.launchImageLibrary({mediaType: 'video'}, (response) => {
              setResponse(response);
            })
          }
        />

        <Button
          title="Request Permission"
          color="red"
          onPress={() => {
            requestMultiple(PERMISSIONS_TO_REQUEST).then((status) => {
              setPermission(JSON.stringify(status, null, 2));
            });
          }}
        />

        <Text style={{}}>Permission: {permission}</Text>
        <Text>Res: {JSON.stringify(response)}</Text>

        {response && (
          <View style={styles.image}>
            <Image
              style={{width: 200, height: 200}}
              source={{uri: response.uri}}
            />
          </View>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    backgroundColor: '#F5FCFF',
  },
  button: {
    marginVertical: 24,
    marginHorizontal: 24,
  },
  image: {
    marginVertical: 24,
    alignItems: 'center',
  },
});
