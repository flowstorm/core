const { merge } = require('webpack-merge');
const common = require('./webpack.common.js');
const globImporter = require('node-sass-glob-importer');
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const UglifyJsPlugin = require("uglifyjs-webpack-plugin");
const OptimizeCSSAssetsPlugin = require("optimize-css-assets-webpack-plugin");
const { CleanWebpackPlugin } = require('clean-webpack-plugin');
const path = require('path');

const miniCss = new MiniCssExtractPlugin({
	filename: '[name].css',
	chunkFilename: '[id].css',
});
const cleanWebpack = new CleanWebpackPlugin({
	output: {
		/**
		 * With zero configuration,
		 *   clean-webpack-plugin will remove files inside the directory below
		 */
		path: path.resolve(process.cwd(), 'dist'),
	},
});

module.exports = merge(common, {
	mode: "production",
    performance: {
        hints: false,
        maxEntrypointSize: 512000,
        maxAssetSize: 512000
    },
	optimization: {
        usedExports: true,
		minimizer: [
			new UglifyJsPlugin({
				cache: true,
				parallel: true,
				sourceMap: false,
			}),
			new OptimizeCSSAssetsPlugin({})
		]
	},
	module: {
		rules: [
			{
				test: /\.scss$/,
				exclude: /node_modules/,
				use: [
					{
						loader: MiniCssExtractPlugin.loader
					},
					{
						loader: 'css-loader',
						options: {
							sourceMap: false
						}
					},
					{
						loader: 'postcss-loader'
					},
					{
						loader: 'sass-loader',
						options: {
							implementation: require('sass'),
							sourceMap: false
						}
					}
				]
			}
		]
	},
	plugins: [
		miniCss,
		cleanWebpack
	]
});
