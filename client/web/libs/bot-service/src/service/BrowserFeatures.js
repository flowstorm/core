export default class BrowserFeatures {
	constructor() {
		this.state = {
			getUserMedia: undefined,
			AudioContext: undefined,
			SpeechRecognition: undefined,
			SpeechSynthesis: undefined,
		};
	}

	getUserMedia = async () => {
		if (this.state.getUserMedia) {
			return Promise.resolve(this.state.getUserMedia);
		}

		// Older browsers might not implement mediaDevices at all, so we set an empty object first
		if (navigator.mediaDevices === undefined) {
			navigator.mediaDevices = {};
		}

		// Some browsers partially implement mediaDevices. We can't just assign an object
		// with getUserMedia as it would overwrite existing properties.
		// Here, we will just add the getUserMedia property if it's missing.
		if (navigator.mediaDevices.getUserMedia === undefined) {
			// First get ahold of the legacy getUserMedia, if present
			const getUserMedia =
				navigator.webkitGetUserMedia ||
				navigator.mozGetUserMedia ||
				navigator.msGetUserMedia ||
				false;

			// Some browsers just don't implement it - return a rejected promise with an error
			// to keep a consistent interface
			if (!getUserMedia) {
				return Promise.reject(new Error('getUserMedia is not implemented in this browser'));
			}

			navigator.mediaDevices.getUserMedia = constraints => {
				// Otherwise, wrap the call to the old navigator.getUserMedia with a Promise
				return new Promise(function(resolve, reject) {
					getUserMedia.call(navigator, constraints, resolve, reject);
				});
			};
		}

		this.state.getUserMedia = navigator.mediaDevices.getUserMedia;
		return Promise.resolve(this.state.getUserMedia);
	};

	getAudioContext = async () => {
		if (this.state.AudioContext) {
			return Promise.resolve(this.state.AudioContext);
		}

		this.state.AudioContext =
			window.AudioContext || // Default
			window.webkitAudioContext || // Safari and old versions of Chrome
			false;

		if (this.state.AudioContext) {
			return Promise.resolve(this.state.AudioContext);
		}
		return Promise.reject('AudioContext is not implemented in this browser');
	};

	getSpeechRecognition = async () => {
		Promise.resolve(false);
	};

	getSpeechRecognitionOld = async () => {
		// Internet Explorer 6-11
		const isIE = /* @cc_on!@*/ false || !!document.documentMode;

		// Edge 20+
		const isEdge = !isIE && !!window.StyleMedia;
		// Chrome 1 - 79
		const isChrome = !!window.chrome && (!!window.chrome.webstore || !!window.chrome.runtime);

		// Edge (based on chromium) detection
		const isEdgeChromium = isChrome && navigator.userAgent.indexOf('Edg') !== -1;
		if (isEdgeChromium || isEdge) {
			return Promise.reject('Web Speech API.SpeechRecognition does not work in Edge');
		}

		const ua = navigator.userAgent;
		const mobile = /IEMobile|Windows Phone|Lumia/i.test(ua)
			? 'w'
			: /iPhone|iP[oa]d/.test(ua)
			? 'i'
			: /Android/.test(ua)
			? 'a'
			: /BlackBerry|PlayBook|BB10/.test(ua)
			? 'b'
			: /Mobile Safari/.test(ua)
			? 's'
			: /webOS|Mobile|Tablet|Opera Mini|\bCrMo\/|Opera Mobi/i.test(ua)
			? 1
			: 0;

		const isAndroid = /Android/.test(ua) ? 1 : 0;

		this.state.isAndroid = isAndroid;

		this.state.SpeechRecognition =
			window.SpeechRecognition ||
			window.speechRecognition ||
			window.webkitSpeechRecognition ||
			false;

		if (this.state.SpeechRecognition) {
			return mobile ? Promise.resolve(this.state.SpeechRecognition) : Promise.resolve(false);
		}
		return Promise.reject('Web Speech API.SpeechRecognition is not implemented in this browser');
	};

	getSpeechSynthesis = async () => {
		if (this.state.SpeechSynthesis) {
			return Promise.resolve(this.state.SpeechSynthesis);
		}

		this.state.SpeechSynthesis = window.speechSynthesis || window.webkitSpeechSynthesis || false;
		if (this.state.SpeechSynthesis) {
			return Promise.resolve(this.state.SpeechSynthesis);
		}
		return Promise.reject('Web Speech API.SpeechSynthesis is not implemented in this browser');
	};

	detect = async feature => {
		switch (feature) {
			case 'getUserMedia':
				return Promise.resolve(this.getUserMedia());

			case 'AudioContext':
				return Promise.resolve(this.getAudioContext());
			case 'SpeechRecognition':
				return Promise.resolve(this.getSpeechRecognition());

			case 'SpeechSynthesis':
				return Promise.resolve(this.getSpeechSynthesis());

			default:
		}
		return Promise.resolve(false);
	};
}
