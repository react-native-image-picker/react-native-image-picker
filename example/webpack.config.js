const path = require('path');
const fs = require('fs');

const webpack = require('webpack');
const HtmlWebpackPlugin = require('html-webpack-plugin');

const appDirectory = path.resolve(__dirname);
const {presets} = require(`${appDirectory}/babel.config.js`);

var includePaths = [
  fs.realpathSync(__dirname + '/node_modules/react-native-image-picker/lib'),
];

const compileNodeModules = [
  // Add every react-native package that needs compiling
  // 'react-native-image-picker',
  'react-native',
].map(moduleName => path.resolve(appDirectory, `node_modules/${moduleName}`));

console.log(path.resolve(appDirectory, `node_modules/`));

const babelLoaderConfiguration = {
  test: /\.js$|tsx?$/,
  // Add every directory that needs to be compiled by Babel during the build.
  include: [
    path.resolve(__dirname, 'index.web.ts'),
    path.resolve(__dirname, 'src/App.tsx'),
    path.resolve(__dirname, 'src'),
    ...compileNodeModules,
  ],
  use: {
    loader: 'babel-loader',
    options: {
      cacheDirectory: true,
      presets,
      plugins: ['react-native-web'],
    },
  },
};

const imageLoaderConfiguration = {
  test: /\.(gif|jpe?g|png)$/,
  use: {
    loader: 'url-loader',
    options: {
      name: '[name].[ext]',
    },
  },
};

module.exports = {
  entry: {
    app: path.join(__dirname, 'index.web.ts'),
  },
  output: {
    filename: 'bundle.web.js',
    path: path.resolve(appDirectory, 'dist'),
  },
  resolve: {
    extensions: ['.web.tsx', '.web.ts', '.tsx', '.ts', '.web.js', '.js'],
    alias: {
      'react-native$': 'react-native-web',
      // 'react-native-image-picker$': '../',
    },
    symlinks: false,
  },
  module: {
    rules: [
      babelLoaderConfiguration,
      imageLoaderConfiguration,
      {
        include: includePaths,
        test: /\.ts|\.tsx$/,
        use: 'ts-loader',
      },
    ],
  },
  plugins: [
    new HtmlWebpackPlugin({
      template: path.join(__dirname, 'web/index.html'),
    }),
    new webpack.HotModuleReplacementPlugin(),
    new webpack.DefinePlugin({
      // See: https://github.com/necolas/react-native-web/issues/349
      __DEV__: JSON.stringify(true),
    }),
  ],
};
