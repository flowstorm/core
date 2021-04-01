const { merge } = require('webpack-merge');
const common = require('./webpack.common.js');
const globImporter = require('node-sass-glob-importer');
const MiniCssExtractPlugin = require("mini-css-extract-plugin");

const miniCss = new MiniCssExtractPlugin({
	filename: '[name].css?[hash]',
	chunkFilename: '[id].css',
});

module.exports = merge(common, {
	mode: "development",
	devtool: "source-map",
	devServer: {
		contentBase: './dist'
	},
	module: {
		rules: [
			{
				test: /\.scss$/,
				exclude: /node_modules/,
				use: [
					{
//						loader: 'style-loader'
						loader: MiniCssExtractPlugin.loader
					},
					{
						loader: 'css-loader',
						options: {
							sourceMap: true
						}
					},
					{
						loader: 'postcss-loader'
					},
					{
						loader: 'sass-loader',
						options: {
							implementation: require('sass'),
							sourceMap: true
						}
					}
				]
			}
		]
	},
    plugins: [
        miniCss
    ]
});
