import WebSpeechToText from './WebSpeechToText';
import AudioStreamSpeechToText from './AudioStreamSpeechToText';

export default class CrossBrowserStt {
	constructor(features, webSocket, callback, sendAudioOpen, sendAudioClose) {
		this.features = features;
		this.webSocket = webSocket;
		this.callback = callback;
		this.sendAudioOpen = sendAudioOpen;
		this.sendAudioClose = sendAudioClose;
	}

	detectEngine = async () => {
		return this.features
			.detect('SpeechRecognition')
			.then(webApi => {
				if (webApi) {
					return Promise.resolve('web-speech-api');
				}

				return Promise.resolve('audio-stream-api');
			})
			.catch(() => Promise.resolve('audio-stream-api'));
	};

	getEngine = () => {
		return this.api;
	};

	selectEngine = api => {
		if (this.api === api) {
			return;
		}

		if (api === 'audio-stream-api') {
			this.stt = new AudioStreamSpeechToText(
				this.features,
				this.webSocket,
				this.sendAudioOpen,
				this.sendAudioClose
			);
		} else if (api === 'web-speech-api') {
			this.stt = new WebSpeechToText(this.features, this.callback);
		}
	};

	selectLanguage = language => {
		this.stt.language = language;
	};

	open = async () => {
		if (this.stt.open) {
			return this.stt.open();
		}
		return Promise.resolve();
	};

	start = async () => {
		if (this.stt.start) {
			return this.stt.start();
		}
		return Promise.resolve();
	};

	stop = async () => {
		if (this.stt.stop) {
			return this.stt.stop();
		}
		return Promise.resolve();
	};

	close = async () => {
		if (this.stt.close) {
			return this.stt.close();
		}
		return Promise.resolve();
	};

	event = async data => {
		if (this.stt.event) {
			return this.stt.event(data);
		}
		return Promise.resolve();
	};
}
