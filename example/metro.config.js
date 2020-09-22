/**
 * Metro configuration for React Native
 * https://github.com/facebook/react-native
 *
 * @format
 */

var path = require('path');

module.exports = {
  watchFolders: [
    path.resolve(__dirname, '../src'),
  ],
  resolver: {
  /**
   * Add "global" dependencies for our RN project here so that our local components can resolve their
   * dependencies correctly
   */
    extraNodeModules: new Proxy({}, {
      get: (target, name) => path.join(process.cwd(), `node_modules/${name}`)
    })
  },
  transformer: {
    getTransformOptions: async () => ({
      transform: {
        experimentalImportSupport: false,
        inlineRequires: false,
      },
    }),
  },
};
