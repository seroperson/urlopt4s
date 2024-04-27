const path = require("path");
const webpack = require("webpack");
const TerserPlugin = require("terser-webpack-plugin");

module.exports = {
  entry: "./src/index.js",
  experiments: {
    outputModule: true,
  },
  output: {
    path: path.resolve(__dirname, "dist"),
    filename: "[name]-bundle.mjs",
    chunkFormat: "module",
    library: {
      type: "module",
    },
    // graaljs thing
    globalObject: "globalThis",
  },

  target: "node",
  optimization: {
    minimize: true,
    minimizer: [
      new TerserPlugin({
        terserOptions: {
          format: {
            comments: false,
          },
        },
        extractComments: false,
      }),
    ],
  },
  plugins: [
    // How to create polyfill:
    // https://gist.github.com/ef4/d2cf5672a93cf241fd47c020b9b3066a
    new webpack.ProvidePlugin({
      TextDecoder: ["text-encoding", "TextDecoder"],
      TextEncoder: ["text-encoding", "TextEncoder"],
    }),
    new webpack.optimize.AggressiveMergingPlugin(),
  ],
  mode: "production",
};
