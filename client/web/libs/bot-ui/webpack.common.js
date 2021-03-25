const HtmlWebPackPlugin = require("html-webpack-plugin");
const path = require('path');

const htmlPlugin = new HtmlWebPackPlugin({
	template: "./src/index.html",
	filename: "./index.html",
	// scriptLoading: 'defer'
	inject: 'head'
});

module.exports = {
	entry:  {
		app: './src/app/app.ts',
		vendor: [
			'@babel/polyfill'
		]
	},
	output: {
		filename: '[name].bundle.js?[hash]',
		path: path.resolve(__dirname, 'dist')
	},
	resolve: {
		extensions: [".ts", ".js", ".json"]
	},
	module: {
		rules: [
            {
                test: /\.(woff(2)?|ttf|eot|svg)(\?v=\d+\.\d+\.\d+)?$/,
                use: [
                    {
                        loader: 'file-loader',
                        options: {
                            name: '[name].[ext]',
                        },
                    }
                ]
            },
			{
				test: /\.ts?$/,
				exclude: /node_modules/,
				use: [
                    {
                        loader: "babel-loader"
                    },
                    {
						loader: "awesome-typescript-loader",
                        // options: {
						//     "useBabel": true,
                        //     "babelOptions": {
                        //         "babelrc": false, /* Important line */
                        //         "presets": [
                        //             ["@babel/preset-env", { "targets": "last 2 versions, ie 11", "modules": false }]
                        //         ],
                        //         "plugins": ["ramda"],
                        //     },
                        //     "babelCore": "@babel/core", // needed for Babel v7
                        // },
					},
                ]
			}
		]
	},
	plugins: [
		htmlPlugin
	]
};
