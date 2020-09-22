import React from 'react';
import {
  StyleSheet,
  Text,
  View,
  Button
} from 'react-native';
import ImagePicker from '../src';

export default class App extends React.Component {

  constructor(props) {
    super(props);

    this.state = {
      response: null
    }
  }

  render() {
    return (
      <View style={styles.container}>
        <Button
          title='Take image'
          onPress={() => ImagePicker.launchCamera({mediaType: 'photo', noData: true, maxHeight: 200, maxWidth: 200}, (response) => {
            this.setState({response})
          })}
        />

        <Button
          title='Select image'
          onPress={() => ImagePicker.launchImageLibrary({mediaType: 'photo', noData: true, maxHeight: 200, maxWidth: 200}, (response) => {
            this.setState({response})
          })}
        />

        <Button
          title='Take video'
          onPress={() => ImagePicker.launchCamera({mediaType: 'video'}, (response) => {
            this.setState({response})
          })}
        />

        <Button
          title='Select video'
          onPress={() => ImagePicker.launchImageLibrary({mediaType: 'video'}, (response) => {
            this.setState({response})
          })}
        />

        <Text>{JSON.stringify(this.state.response)}</Text>
      </View>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'space-around',
    alignItems: 'center',
    backgroundColor: '#F5FCFF',
  },
});