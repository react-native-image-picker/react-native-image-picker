/**
 * @format
 */

import {AppRegistry} from 'react-native';
import App from './src/App';
import {name as appName} from './app.json';
import notifee from '@notifee/react-native';

// To silent warn [notifee] no background event handler has been set. Set a handler via the "onBackgroundEvent" method.
// Or handle the notification being cliked in the system tray
notifee.onBackgroundEvent(async (detail) => {
    console.log('Background event received: ', detail);    
});

AppRegistry.registerComponent(appName, () => App);
