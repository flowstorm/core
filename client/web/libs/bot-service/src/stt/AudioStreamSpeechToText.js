export default class AudioStreamSpeechToText {
	constructor(features, webSocket, sendAudioOpen, sendAudioClose) {
		this.webSocket = webSocket;
		this.features = features;
		this.sendAudioOpen = sendAudioOpen;
		this.sendAudioClose = sendAudioClose;
	}

	close = async () => {
		if (this.audioContext) {
			await this.audioContext.close();
			this.audioContext = null;
		}
		return Promise.resolve();
	};

	streamAudioData = e => {
		const floatSamples = e.inputBuffer.getChannelData(0);

		// pcm_s16le - ffmpeg -f s16le -i input.raw output.wav
		// https://trac.ffmpeg.org/wiki/audio%20types
		const intarr = Int16Array.from(
			floatSamples.map(n => {
				const v = n < 0 ? n * 32768 : n * 32767;
				return Math.max(-32768, Math.min(32768, v));
			})
		);

		this.webSocket.send(intarr);
	};

	event = data => {
		if (data.type === 'InputAudioStreamOpen') {
			const audioContext = this.audioContext;
			const stream = this.stream;

			this.scriptProcessor = null;

			const inputPoint = audioContext.createGain();
			if (stream === null) {
				return;
			}
			const microphone = audioContext.createMediaStreamSource(stream);
			const analyser = audioContext.createAnalyser();

			this.scriptProcessor = inputPoint.context.createScriptProcessor(2048, 2, 2);

			microphone.connect(inputPoint);
			inputPoint.connect(analyser);
			inputPoint.connect(this.scriptProcessor);
			this.scriptProcessor.connect(inputPoint.context.destination);

			this.scriptProcessor.addEventListener('audioprocess', this.streamAudioData);
		}
	};

	start = async () => {
		if (this.audioContext) {
			if (this.audioContext.state === 'suspended') {
				return this.audioContext.resume().then(() => {
					return this.init();
				});
			} else if (this.audioContext.state === 'running') {
				return this.init();
			}
			return Promise.reject(`Unhandled AudioContext state [${this.audioContext.state}]`);
		}

		return this.features.getAudioContext().then(AudioContext => {
			this.audioContext = new AudioContext();
			this.sampleRate = this.audioContext.sampleRate;
			return this.init();
		});
	};

	init = async () => {
		return this.features
			.getUserMedia()
			.then(() => {
				return navigator.mediaDevices.getUserMedia({ audio: true });
			})
			.then(stream => {
				this.stream = stream;
				this.sendAudioOpen();
				return Promise.resolve();
			});
	};

	stop = async () => {
		this.sendAudioClose();

		if (this.scriptProcessor) {
			// Stop listening the stream from the michrophone
			this.scriptProcessor.removeEventListener('audioprocess', this.streamAudioData);
			this.scriptProcessor = null;
		}

		if (this.stream) {
			const tracks = this.stream.getTracks();

			tracks.forEach(function(track) {
				track.stop();
			});
			this.stream = null;
		}

		return this.audioContext.suspend();
	};
}
