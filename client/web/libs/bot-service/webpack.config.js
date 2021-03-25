const path = require('path');

module.exports = {
	entry: ['babel-polyfill', './src/index.js'],
	output: {
		filename: 'bot-service.js',
		path: path.resolve(__dirname, 'build'),
		library: 'botService',
		libraryTarget: 'window',
	},
	module: {
		rules: [
			{
				test: /\.js$/,
				exclude: /node_modules/,
				use: {
					loader: 'babel-loader',
					options: {
						presets: ['@babel/preset-env'],
						plugins: ['@babel/plugin-proposal-class-properties'],
					},
				},
			},
		],
	},
};
