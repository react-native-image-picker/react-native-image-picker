import React from 'react';
import {
  AppRegistry
} from 'react-native';
import App from './App';

class Example extends React.Component {
  render() {
    return (
      <App />
    );
  }
}

AppRegistry.registerComponent('Example', () => Example);
