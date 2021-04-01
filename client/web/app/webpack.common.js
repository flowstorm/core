const HtmlWebPackPlugin = require('html-webpack-plugin');
const path = require('path');

const htmlPlugin = new HtmlWebPackPlugin({
	template: './public/index.html',
	filename: './index.html',
	// scriptLoading: 'defer'
	inject: 'head',
});

module.exports = {
	entry: {
		app: './src/main.js',
	},
	output: {
		filename: '[name].bundle.js?[hash]',
		path: path.resolve(__dirname, 'dist'),
	},
	resolve: {
		extensions: ['.ts', '.js', '.json'],
	},
	module: {
		rules: [
			{
      		test: /\.(png|jpe?g|gif|svg|webp|ico)$/i,
      		use: [
      			{
      				loader: 'file-loader',
      				options: {
      					// TODO insert [hash]
      					name: 'assets/[name].[ext]',
      					publicPath: '/',
      				},
      			},
      		],
			},
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
			{
				test: /\.(woff(2)?|ttf|eot|svg)(\?v=\d+\.\d+\.\d+)?$/,
				use: [
					{
						loader: 'file-loader',
						options: {
							name: '[name].[ext]',
							esModule: false
						},
					},
				],
			},
		],
	},
	plugins: [htmlPlugin],
};
