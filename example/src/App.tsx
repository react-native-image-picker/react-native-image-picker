import * as React from 'react';
import {
  Image,
  Platform,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  View,
} from 'react-native';
import {DemoButton, DemoResponse, DemoTitle} from './components';

import * as ImagePicker from 'react-native-image-picker';
import { PERMISSIONS, Permission, RESULTS, check, request } from 'react-native-permissions';
import notifee, { AndroidColor, AndroidImportance } from '@notifee/react-native';

/* toggle includeExtra */
const includeExtra = true;

export default function App() {
  const [response, setResponse] = React.useState<any>(null);

  const onButtonPress = React.useCallback(async (type , options) => {
    if (type === 'capture') {
      try {
        await registerForegroundNotification();
        const response = await ImagePicker.launchCamera(options);
        setResponse(response);
      } finally {
        await stopForegroundNotification();
      }
    } else if (type === 'library') {
      ImagePicker.launchImageLibrary(options, setResponse);
    }
    else if (type === 'permission') {
      requestCameraPermissionIfNeeded().then((result) => {
        setResponse(result ? 'Permission Granted' : 'Permission Denied');
      })
    }
  }, []);


  React.useEffect(() => {
    // Close the notification when the app is closed
    return () => {stopForegroundNotification();}
  })

  return (
    <SafeAreaView style={styles.container}>
      <DemoTitle>🌄 React Native Image Picker</DemoTitle>
      <ScrollView>
        <View style={styles.buttonContainer}>
          {actions.map(({title, type, options}) => {
            return (
              <DemoButton
                key={title}
                onPress={() => onButtonPress(type, options)}>
                {title}
              </DemoButton>
            );
          })}
        </View>
        <DemoResponse>{response}</DemoResponse>

        {response?.assets &&
          response?.assets.map(({uri}: {uri: string}) => (
            <View key={uri} style={styles.imageContainer}>
              <Image
                resizeMode="cover"
                resizeMethod="scale"
                style={styles.image}
                source={{uri: uri}}
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
  type: 'capture' | 'library' | 'permission';
  options?: ImagePicker.CameraOptions | ImagePicker.ImageLibraryOptions;
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
  {
    title: 'Request Permission',
    type: 'permission',
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


const requestCameraPermissionIfNeeded = async () => {
  var permission: Permission = PERMISSIONS.IOS.CAMERA;
  if (Platform.OS === 'android') {
    permission = PERMISSIONS.ANDROID.CAMERA;
  }
  const result = await check(permission)

  if(result === RESULTS.GRANTED)  return true;
  else if (result === RESULTS.DENIED) {
    const result = await request(permission)
    if(result === RESULTS.GRANTED)  return true;
  }
  return false;
}

const registerForegroundNotification = async () => {
  if (Platform.OS === 'android') {
    // Need permission since Android 13
    const result = await notifee.requestPermission();

    const channelId = await notifee.createChannel({
      id: 'camera',
      name: 'Camera',
      importance: AndroidImportance.LOW,
    });

    notifee.registerForegroundService((notification) => {
      return new Promise(() => {  });
    });

    const notificationId = await notifee.displayNotification({
      title: 'Camera is running',
      body: 'Required for the app to stay alive in background',
      android: {
        channelId,
        asForegroundService: true,
        color: AndroidColor.CYAN,
        colorized: true,
        autoCancel: false,
        ongoing: true,
      },
    });
  }
}
const stopForegroundNotification = async () => {
  if (Platform.OS === 'android') {
    await notifee.stopForegroundService();
  }
}