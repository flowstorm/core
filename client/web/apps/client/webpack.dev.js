const { merge } = require('webpack-merge');
const common = require('./webpack.common.js');

module.exports = merge(common, {
	mode: 'development',
	devtool: 'source-map',
	devServer: {
		contentBase: './dist',
		historyApiFallback: true,
	},
	module: {
  		rules: [
  			{
  				test: /\.scss$/,
  				exclude: /node_modules/,
  				use: [
  					{
  						loader: 'style-loader'
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
							loader: 'resolve-url-loader',
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
  	}
});
