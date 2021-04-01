import { v4 as uuidv4 } from 'uuid';
import play from './assets/play.png';
import * as Sentry from '@sentry/browser';
import { Integrations } from '@sentry/tracing';
import './assets/main.scss';
import './assets/fonts/Century-Gothic.ttf';

const environment = 'local';
const scrollSpeed = 120; // pixels per second
const scrollDelay = 3; // seconds before the scrolling starts

Sentry.init({
	dsn: 'https://da1caa885aee4032898d553d1129571b@o318069.ingest.sentry.io/5438705',
	integrations: [new Integrations.BrowserTracing()],
	tracesSampleRate: 1.0,
	environment,
});

const Bot = window.botService.default;
const audios = {
	in: new Audio('https://core.flowstorm.ai/assets/audio/pop1.mp3'),
	out: new Audio('https://core.flowstorm.ai/assets/audio/pop2.mp3'),
};

const environments = {
	local: '',
	preview: '-preview',
	default: '',
};
const keys = {
	local: '5f7db5f1e662e830b20dbe7c',
	preview: '5ea04b86a7a6757defff6b1d',
	default: '5f7db5f1e662e830b20dbe7c',
};
let bot = undefined;
let botBackground = undefined;
let botKey = keys[environment];

window.initBot = function(botUI) {

	window.addEventListener('message', event => {
			 if (event.data === 'BotStopEvent') {
			 		onEnd();
			 }
	 });
	function setState(newState) {
		if (newState.status === 'LISTENING') {
			botUI.setOutputAudio(1);
		} else {
			botUI.setInputAudio(1);
		}
	}
	function getStatusString(status) {
		return status;
	}
	function addMessage(type, text, image, background) {
		if (type === 'sent') {
			if (text.charAt(0) !== '#') {
				botUI.setUserText(text);
			}
		} else {
			BotUI.botTextKioskElement.style.transition = 'transform 0s linear 0s';
			BotUI.botTextKioskElement.style.transform = 'translateY(0px)';
			botUI.setBotText(text);
			window.setTimeout(() => {
				const windowHeight =
					BotUI.orientation === 'portrait' ? window.innerHeight / 2 : window.innerHeight;
				if (BotUI.botTextKioskElement.scrollHeight > windowHeight) {
					const backgroundElementYTranslate = windowHeight - BotUI.botTextKioskElement.scrollHeight;
					BotUI.botTextKioskElement.style.transition =
						'transform ' +
						-backgroundElementYTranslate / scrollSpeed +
						's linear ' +
						scrollDelay +
						's';
					BotUI.botTextKioskElement.style.transform =
						'translateY(' + backgroundElementYTranslate + 'px)';
				}
			}, BotUI.settings.animationSpeed + 5);
		}
		if (image !== undefined && image !== null) {
			botUI.setImage(image);
		}
		if (background !== undefined && background !== null && background !== botBackground) {
			botBackground = background;
			if (background.startsWith('#')) {
				botUI.setBackgroundColor(background);
			} else {
				botUI.setBackgroundImage(background);
			}
		}
	}

	function showLogs(logs) {
		logs.forEach(l => {
			console.log(l.text);
		});
	}

	function onError(error) {
		console.log(error);
		Sentry.captureException(error.message);
		alert(
			'An error occurred and was reported to the page administrator. Please refresh the page or try the bot again later.'
		);
		onEnd();
	}

	function onEnd() {
		botUI.setBotText();
		botUI.setUserText();
		botUI.setInputAudio(null);
		botUI.setImage(null);
		document.getElementById('play').style.display = 'block';
	}

	function getAttributes() {
		return {
			// TODO add version
			clientType: 'web',
			clientScreen: true,
		};
	}

	function getUUID() {
		return uuidv4();
	}

	function getVoice() {
		return undefined;
	}

	function focusOnNode() {}

	function play(sound) {
		if (sound === 'in' || sound === 'out') {
			audios[sound].play();
		}
	}

	// STOP IMPLEMENTING
	if (window.location.pathname.length === 25) {
		botKey = window.location.pathname.substring(1);
	}

	bot = Bot(
		`https://core${environments[environment]}.flowstorm.ai`,
		undefined, // sender
		true, // autostart
		false // called from Kotlin
	);
	bot.setStatus = newState => {
		setState(newState);
	};
	bot.getStatusString = status => {
		getStatusString(status);
	};
	bot.addMessage = (type, text, image, background) => {
		addMessage(type, text, image, background);
	};
	bot.addLogs = logs => {
		showLogs(logs);
	};
	bot.onError = error => {
		onError(error);
	};
	bot.onEnd = onEnd;
	bot.getAttributes = getAttributes;
	bot.getUUID = getUUID;
	bot.getVoice = getVoice;
	bot.focusOnNode = focusOnNode;
	bot.play = sound => {
		play(sound);
	};
	return bot;
};

window.startBot = function() {
	if (bot) {

		var startAction = window.location.hash ? window.location.hash : undefined;
		const urlParams = new URLSearchParams(window.location.search);
	  startAction = urlParams.get('text') === null ? startAction : `#${urlParams.get('text')}`;

		bot.init(botKey, 'en', true, true, startAction);
	} else {
		Sentry.captureMessage('Bot is undefined');
		alert('There was an unexpected error with the bot. Please try reloading the page.');
	}
};
