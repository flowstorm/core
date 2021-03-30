const presets = [
	[
		"@babel/env",
		{
            modules: false,
		    targets: {
				edge: "85",
				firefox: "84",
				chrome: "85",
				safari: "12.1",
			},
			useBuiltIns: "usage",
            corejs: "3.0.0",
		}
	]
];

const plugins = [["ramda"]];

module.exports = { presets, plugins };
