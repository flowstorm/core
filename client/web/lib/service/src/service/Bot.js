import ChannelService from './ChannelService';
import BrowserFeatures from './BrowserFeatures';

export default function Bot(
	url,
	deviceId,
	autoStart,
	fromKotlin = true,
	token = null
) {
	class BotInterface {}
	let service = null;
	let receivedRecords = [];
	let currentAudio = undefined;

	let outputAudio = true;
	let inputAudio = true;
	let startMessage = "#intro";
	let key = undefined;
	let language = 'en';

	const bot = fromKotlin ? this : new BotInterface();
	let sessionEnded = false;
	let audioOpen = false;
	let lastResponseEmpty = false;
	let sleepTimeLimit = 0;

	const audios = {
		listening: new Audio('https://repository.promethist.ai/audio/client/listening.mp3'),
		recognized: new Audio('https://repository.promethist.ai/audio/client/recognized.mp3'),
		waiting: new Audio('https://repository.promethist.ai/audio/client/waiting.mp3'),
		error: new Audio('https://repository.promethist.ai/audio/client/error.mp3'),
		sleep: new Audio('https://repository.promethist.ai/audio/client/sleep.mp3'),
	};
	const waitingSoundDelay = 0;

	let userToken = token;
	let senderId = deviceId;
	if (deviceId === undefined && typeof window.localStorage !== 'undefined') {
		if (localStorage.getItem('sender') === null) {
			senderId = [...Array(16)].map(() => Math.floor(Math.random() * 16).toString(16)).join('');
			localStorage.setItem('sender', senderId);
		} else {
			senderId = localStorage.getItem('sender');
		}
	}

	document.addEventListener('BotStopEvent', () => {
		onStopClick();
	});

	window.addEventListener('message', event => {
			 if (event.data === 'BotStopEvent') {
			 		onStopClick();
			 }
	 });

	document.addEventListener('BotPauseEvent', () => {
		bot.setStatus({ status: 'PAUSED' });
		if (currentAudio.src !== '' && outputAudio) currentAudio.pause();
	});

	document.addEventListener('BotResumeEvent', () => {
		bot.setStatus({ status: 'RESPONDING' });
		if (currentAudio.src !== '' && outputAudio) currentAudio.play();
		else if (!outputAudio) {
			skipPlayedMessages();
			bot.setStatus({ status: 'LISTENING' });
		}
	});

	document.addEventListener('OutputAudioEvent', e => {
		if (outputAudio) {
			if (e.detail.state === 'RESPONDING') {
				skipPlayedMessages();
				handleAudioInput(true);
				bot.setStatus({ status: 'LISTENING' });
			}
			outputAudio = false;
		} else {
			outputAudio = true;
		}
	});

	document.addEventListener('InputAudioEvent', e => {
		inputAudio = !inputAudio;
		if (e.detail.state === 'LISTENING') {
			if (inputAudio) {
				handleAudioInput(true);
			} else {
				closeAudioStream('InputAudioEvent');
			}
		}
	});

	document.addEventListener('RESPONDINGClickEvent', () => {
		skipPlayedMessages();
		bot.setStatus({ status: 'LISTENING' });
		handleAudioInput(true);
	});
	document.addEventListener('SLEEPINGClickEvent', () => {});

	bot.init = function(
			appKey,
			lang,
			defaultInputAudio,
			defaultOutputAudio,
			startingMessage = '#intro'
	) {

		outputAudio = defaultOutputAudio;
		inputAudio = defaultInputAudio;
		key = appKey;
		language = lang;
		startMessage = startingMessage;

		if (sleepTimeLimit < getCurrentMillis() || sleepTimeLimit == 0) {
			return initialize();
		} else {
			sleepTimeLimit = 0;
			bot.addMessage('sent', startMessage);
			service.sendText(startMessage);
			return this;
		}
	};

	function initialize() {
		sleepTimeLimit = 0;
		currentAudio = new Audio('https://repository.promethist.ai/audio/client/intro.mp3');
		const audioPromise = currentAudio.play();
		if (audioPromise !== undefined) {
			audioPromise
				.then(_ => {
					console.log('Audio OK');
				})
				.catch(error => {
					console.error(error);
				});
		}
		for (const key in audios) {
			audios[key].play().then(_ => {
				audios[key].pause();
				audios[key].currentTime = 0;
			});
		}

		console.log('Bot.init');

		addTextListener();

		const endpoint = '/socket/';
		service = new ChannelService(
			new BrowserFeatures(),
			url.replace('http', 'ws') + endpoint,
			onMessage,
			errorCallback,
			key,
			language,
			senderId,
			bot,
			userToken
		);

		service
			.open()
			.then(() => bot.setStatus({ isActive: true }))
			.catch(error => {
				errorCallback(error);
			});
		return this;
	}

	function errorCallback(err) {
		playSound('error');
		bot.onError(err);
		onStopClick();
		bot.onEnd();
	}

	function addTextListener() {
		document.addEventListener('TextInputEvent', handleTextEvent);
	}

	function handleTextEvent(e) {
		document.removeEventListener('TextInputEvent', handleTextEvent);
		handleOnTextInput(e.detail.text, e.detail.audioOn);
	}

	function isNotNil(param) {
		return param !== null && param;
	}

	function handleAudioEnded() {
		currentAudio.removeEventListener('ended', handleAudioEnded);
		addRecord();
	}

	function addRecord() {
		if (isNotNil(receivedRecords) && receivedRecords.length > 0) {
			const [head, ...tail] = receivedRecords;
			const { audio, image, text, background } = head;
			if (audio && outputAudio) {
				currentAudio.src = audio;
				currentAudio.addEventListener('ended', handleAudioEnded);
				currentAudio.play().catch(error => {
					errorCallback({ type: `Audio error in ${currentAudio.src}`, message: error });
				});
			}

			// const bulkMessages = filter(isNotNil, [text, image]);
			bot.addMessage('received', text, image, background);

			receivedRecords = tail;

			if (currentAudio.src === '' || !outputAudio || !audio) {
				addRecord();
			}
		} else {
			if (service.sessionId && sleepTimeLimit == 0) {
				handleAudioInput(true);
			} else {
				if (sessionEnded || sleepTimeLimit != 0) {
					playSound('sleep');
					bot.onEnd();
				}
				bot.setStatus({ isActive: true, status: 'SLEEPING' });
			}
			receivedRecords = undefined;
		}
	}

	function onMessage(param) {
		const paramResponse = param.response;
		const items = paramResponse === undefined ? [] : paramResponse.items;
		switch (param.type) {
			case 'Response':
				// TODO remove
				stopWaitSound();
				if (bot.focusOnNode) bot.focusOnNode(param.response.attributes.nodeId);
				bot.setStatus({ status: 'RESPONDING' });
				if (paramResponse.sleepTimeout > 0) {
					sleepTimeLimit = getCurrentMillis() + paramResponse.sleepTimeout * 1000;
					bot.setStatus({ status: 'SLEEPING' });
				}
				lastResponseEmpty = items.length === 0;
				const records = items.map(({ audio, image, text, ssml, background }) => ({
					audio: isNotNil(audio)
						? audio.startsWith('/')
							? `${url}${audio}`
							: audio
						: isNotNil(ssml)
						? ssml.includes('<audio')
							? ssml.split('"')[1]
							: null
						: null,
					image: isNotNil(image) ? (image.startsWith('/') ? `${url}${image}` : image) : null,
					text,
					background: isNotNil(background) ? (background.length === 0 ? null : background) : null,
				}));

				receivedRecords = records;
				addRecord(0);
				bot.addLogs(paramResponse.logs);
				break;
			case 'Recognized':
				// Difference between Firefox and Chrome
				const recognizedItems = param.message === undefined ? [param] : param.message.items;
				// const bulkMessages = transformIncomingMessages(recognizedItems);
				bot.addMessage('sent', recognizedItems[0].text, null);
				// startWaitSound();
				bot.setStatus({ status: 'PROCESSING' });
				closeAudioStream();
				break;
			case 'Ready':
				service.setSessionId(bot.getUUID());
				sessionEnded = false;
				audioOpen = false;
				if (autoStart) {
					bot.addMessage('sent', startMessage);
					service.sendText(startMessage);
				} else {
					bot.play('bot_ready');
				}
				break;
			case 'InputAudioStreamOpen':
				bot.setStatus({ inputDisabled: false, status: 'LISTENING' });
				break;
			case 'SessionStarted':
				const sessionId = param.sessionId;
				service.setSessionId(sessionId);
				bot.setStatus({ status: 'RESPONDING' });
				break;
			case 'Error':
				bot.onError({ type: 'Server:', message: param.text });
				playSound('error');
			case 'SessionEnded':
				sessionEnded = true;
				closeAudioStream('sessionEnd');
				document.removeEventListener('TextInputEvent', handleTextEvent);
				service.setSessionId(null);
				if (lastResponseEmpty || !outputAudio) {
					bot.onEnd();
					stopWaitSound();
					playSound('sleep');
				}
				bot.setStatus({ status: 'SLEEPING' });
				break;
			default:
				break;
		}
	}
	function closeAudioStream(origin = 'default') {
		if (origin !== 'sessionEnd') playSound('recognized');
		if (audioOpen) {
			service
				.getStt()
				.stop()
				.then(() => {
					audioOpen = false;
					if (inputAudio) bot.setStatus({ inputDisabled: false, audioOn: false });
				})
				.catch(() => {
					errorCallback({ type: 'Speech to text stop error', message: origin });
				});
		}
	}
	function handleAudioInput(start) {
		if (inputAudio) {
			if (start && !audioOpen && sleepTimeLimit < getCurrentMillis()) {
				playSound('listening');
				bot.setStatus({ inputDisabled: true, audioOn: start });
				service
					.getStt()
					.start()
					.then(() => {
						audioOpen = true;
					})
					.catch(e => {
						errorCallback({ type: 'Speech to text start error ', message: e });
					});
			} else {
				closeAudioStream('handleAudioInput');
			}
		} else {
			bot.setStatus({ status: 'LISTENING' });
		}
	}

	function handleOnTextInput(text, audioOn) {
		skipPlayedMessages();
		bot.addMessage('sent', text, null);
		bot.setStatus({ status: 'PROCESSING' });
		// startWaitSound();
		service.sendText(text).then(() => {
			addTextListener();
		});
		if (audioOn && inputAudio) {
			closeAudioStream('handleTextInput');
		}
	}

	function skipPlayedMessages() {
		if (currentAudio.src !== '') {
			currentAudio.pause();
			currentAudio.src = '';

			if (isNotNil(receivedRecords) && receivedRecords.length > 0) {
				receivedRecords.forEach(message => {
					bot.addMessage('received', message.text, message.image, message.background);
				});

				receivedRecords = undefined;
			}
		}
	}
	function onStopClick() {
		stopWaitSound();
		document.removeEventListener('TextInputEvent', handleTextEvent);
		if (currentAudio)
			skipPlayedMessages();
		if (service) {
			service.setSessionId(null);
			service.close();
		}
		bot.setStatus({ status: 'SLEEPING' });
	}

	function startWaitSound() {
		setTimeout(function() {
			audios.waiting.addEventListener('ended', restartWaitingAudio);
			audios.waiting.play();
		}, waitingSoundDelay);
	}

	function stopWaitSound() {
		audios.waiting.removeEventListener('ended', restartWaitingAudio);
		audios.waiting.pause();
		audios.waiting.currentTime = 0;
	}

	function restartWaitingAudio() {
		playSound('waiting');
	}

	function playSound(sound) {
		audios[sound].currentTime = 0;
		audios[sound].play();
	}

	function getCurrentMillis() {
		const date = new Date();
		return date.getTime();
	}

	return bot;
}
