import merge from 'ramda/es/merge';
import isNil from 'ramda/es/isNil';
import is from 'ramda/es/is';
import isEmpty from 'ramda/es/isEmpty';
import defaultTo from 'ramda/es/defaultTo';
import times from 'ramda/es/times';
import clamp from 'ramda/es/clamp';
import FastAverageColor from 'fast-average-color';

import '../assets/main.scss';

import {
    baseStructureTemplate,
    chatMessageStructureTemplate,
    kioskMessageStructureTemplate,
} from './templates';
import {
    Background,
    GUIMode,
    MessageType,
    OrientationEnum,
    ScreenTypeEnum,
    Settings,
    StateTypeEnum,
} from './model/bot-ui.model';
import {
    getContentAsHtml,
    debounce,
    injectCss,
} from './utils';

const defaults: Settings = {
    animationSpeed: 500,
    backgroundAdvancedAnimationParticlesCount: 20,
    backgroundColor: '#927263',
    backgroundImageBlur: 0,
    backgroundSimpleAnimation: true,
    detectOrientation: true,
    fullScreen: true,
    guiMode: GUIMode.KIOSK,
    imageAverageColorOpacity: 0.5,
    widgetSize: {
        height: '500px',
        width: '800px',
    },
    userMessageBackgroundColor: 'rgba(255, 255, 255, .3)',
    userMessageTextColor: '#ffffff',
    botMessageBackgroundColor: 'rgba(0, 0, 0, .4)',
    botMessageTextColor: '#ffffff',
};

const fullScreenWidgetWidth = '100vw';
const fullScreenWidgetHeight = '100vh';
const minAnimationParticles = 0;
const maxAnimationParticles = 20;
const chatVolumeMuteStorageKey = 'chatVolumeMuted';
const chatMicrophoneStorageKey = 'chatMicrophoneEnabled';

export class BotUI  {
    private static element: HTMLElement;
    private static settings: Settings;
    private static orientation: OrientationEnum;

    private static imageKioskElement: HTMLElement;
    private static userTextKioskElement: HTMLElement;
    private static botTextKioskElement: HTMLElement
    private static messagesElement: HTMLElement
    private static userPcmElement: HTMLElement;
    private static botPcmElement: HTMLElement;
    private static backgroundElement: HTMLElement;
    private static chatInputElement: HTMLElement;
    private static chatInputMuteElement: HTMLElement;
    private static chatInputMicrophoneElement: HTMLElement;
    private static chatInputBargeElement: HTMLElement;

    private static isChatMuted: boolean = isNil(sessionStorage.getItem(chatVolumeMuteStorageKey));
    private static isMicrophoneEnabled: boolean = !isNil(sessionStorage.getItem(chatMicrophoneStorageKey));

    constructor(element: string, settings: Settings = defaults) {
        BotUI.settings = merge(defaults, settings);
        const root = document.documentElement;
        root.style.setProperty('--animation-speed', `${BotUI.settings.animationSpeed}ms`);
        root.style.setProperty('--background-url-blur', `${BotUI.settings.backgroundImageBlur}px`);
        root.style.setProperty('--background-color', `${BotUI.settings.backgroundColor}`);
        root.style.setProperty('--message-color-bot', `${BotUI.settings.botMessageTextColor}`);
        root.style.setProperty('--message-color-user', `${BotUI.settings.userMessageTextColor}`);
        root.style.setProperty('--message-background-bot', `${BotUI.settings.botMessageBackgroundColor}`);
        root.style.setProperty('--message-background-user', `${BotUI.settings.userMessageBackgroundColor}`);
        BotUI.element = document.getElementById(element);
        BotUI.orientation = OrientationEnum.LANDSCAPE;
        BotUI.element.innerHTML = baseStructureTemplate;
        BotUI.element.setAttribute('data-gui-mode', BotUI.settings.guiMode);
        BotUI.imageKioskElement = BotUI.element.querySelector('[data-image]');
        BotUI.userPcmElement = BotUI.element.querySelector('[data-user-pcm]');
        BotUI.botPcmElement = BotUI.element.querySelector('[data-bot-pcm]');
        BotUI.messagesElement = BotUI.element.querySelector('[data-messages]');
        BotUI.chatInputElement = BotUI.element.querySelector('[data-chat-input] input');
        BotUI.chatInputMuteElement = BotUI.element.querySelector('[data-chat-input-mute]');
        BotUI.chatInputMicrophoneElement = BotUI.element.querySelector('[data-chat-input-microphone]');
        BotUI.chatInputBargeElement = BotUI.element.querySelector('[data-chat-input-barge]');
        if (BotUI.settings.guiMode === GUIMode.KIOSK) {
            BotUI.messagesElement.innerHTML = kioskMessageStructureTemplate;
            BotUI.userTextKioskElement = BotUI.element.querySelector('[data-user-message] span');
            BotUI.botTextKioskElement = BotUI.element.querySelector('[data-bot-message] span');
        } else {
            if (BotUI.isChatMuted) {
                BotUI.chatInputMuteElement.classList.remove('icon--light');
            } else {
                BotUI.chatInputMuteElement.classList.add('icon--light');
            }
            window.setTimeout(() => {
                BotUI.getChatMute(!BotUI.isChatMuted, this.chatMuteCallback);
            });
            if (BotUI.isMicrophoneEnabled) {
                BotUI.chatInputMicrophoneElement.classList.remove('icon--light');
            } else {
                BotUI.chatInputMicrophoneElement.classList.add('icon--light');
            }
            window.setTimeout(() => {
                BotUI.getChatMicrophone(BotUI.isMicrophoneEnabled, this.chatMicrophoneCallback);
            });
        }
        BotUI.backgroundElement = BotUI.element.querySelector('[data-background]');
        const { width, height } = BotUI.settings.widgetSize;
        BotUI.element.style.width = defaultTo(fullScreenWidgetWidth, !BotUI.settings.fullScreen && width ? width : null);
        BotUI.element.style.height = defaultTo(fullScreenWidgetHeight, !BotUI.settings.fullScreen && height ? height : null);
        BotUI.settings.backgroundAdvancedAnimationParticlesCount = clamp(
            minAnimationParticles,
            maxAnimationParticles,
            BotUI.settings.backgroundAdvancedAnimationParticlesCount,
        );
        BotUI.backgroundElement.innerHTML = '';
        if (!!BotUI.settings.backgroundSimpleAnimation) {
            // BotUI.backgroundElement.setAttribute('data-background-animation', '');
        }
        times(() => {
            BotUI.backgroundElement.appendChild(document.createElement('span'));
        }, BotUI.settings.backgroundAdvancedAnimationParticlesCount);

        window.addEventListener('resize', debounce((e) => {
            const rect: DOMRect = BotUI.element.getBoundingClientRect();
            const orientation: OrientationEnum = (rect.width > rect.height) ? OrientationEnum.LANDSCAPE : OrientationEnum.PORTRAIT;
            this.setOrientation(orientation);
            BotUI.element.setAttribute('data-orientation', orientation);
        }));

        window.addEventListener('load', (e) => {
            const rect: DOMRect = BotUI.element.getBoundingClientRect();
            const orientation: OrientationEnum = (rect.width > rect.height) ? OrientationEnum.LANDSCAPE : OrientationEnum.PORTRAIT;
            this.setOrientation(orientation);
        });

        BotUI.chatInputElement.onkeydown = (e) => {
            if (e.keyCode === 13) {
                BotUI.getInputValue((BotUI.chatInputElement as HTMLInputElement).value, this.chatInputCallback);
                (BotUI.chatInputElement as HTMLInputElement).value = '';
            }
        }

        BotUI.chatInputMuteElement.onclick = (e) => {
            BotUI.isChatMuted = isNil(sessionStorage.getItem(chatVolumeMuteStorageKey));
            if (BotUI.isChatMuted) {
                sessionStorage.setItem(chatVolumeMuteStorageKey, 'true');
                BotUI.chatInputMuteElement.classList.add('icon--light');
            } else {
                sessionStorage.removeItem(chatVolumeMuteStorageKey);
                BotUI.chatInputMuteElement.classList.remove('icon--light');
            }
            BotUI.getChatMute(BotUI.isChatMuted, this.chatMuteCallback);
        }

        BotUI.chatInputMicrophoneElement.onclick = (e) => {
            BotUI.isMicrophoneEnabled = isNil(sessionStorage.getItem(chatMicrophoneStorageKey));
            BotUI._setMicrophone();
            BotUI.getChatMicrophone(BotUI.isMicrophoneEnabled, this.chatMicrophoneCallback);
        }

        BotUI.chatInputBargeElement.onclick = (e) => {
            const inputString = (BotUI.chatInputElement as HTMLInputElement).value
            if (inputString !== ''){
                BotUI.getInputValue(inputString, this.chatInputCallback);
                (BotUI.chatInputElement as HTMLInputElement).value = '';
            } else {
                this.chatBargeCallback();
            }
        }
        injectCss();
    }

    public setScreen = (screenType: ScreenTypeEnum = ScreenTypeEnum.PLAYER) => {
    }

    public setOrientation = (orientation: OrientationEnum = BotUI.orientation) => {
        const root = document.documentElement;
        const rect: DOMRect = BotUI.element.getBoundingClientRect();
        BotUI.element.setAttribute('data-orientation', orientation);
        root.style.setProperty('--window-width', BotUI.element.style.width);
        root.style.setProperty('--window-height', BotUI.element.style.height);
    }

    public setState = (stateType: StateTypeEnum) => {

    }

    public setBackgroundColor = (color: string) => BotUI.setBackground({
        color,
    });

    public setBackgroundImage = (url: string, blur = BotUI.settings.backgroundImageBlur) => BotUI.setBackground({
        url: {
            path: url,
            blur,
        },
    });

    public setUserText = (text: string = null) => {
        if (BotUI.settings.guiMode === GUIMode.KIOSK) {
            BotUI.userTextKioskElement.setAttribute('data-empty', '');
            window.setTimeout(() => {
                if (isNil(text) || isEmpty(text)) {
                    BotUI.userTextKioskElement.setAttribute('data-empty', '');
                } else {
                    BotUI.userTextKioskElement.innerText = text;
                    BotUI.userTextKioskElement.removeAttribute('data-empty');
                }
            }, BotUI.settings.animationSpeed);
        }
        if (BotUI.settings.guiMode === GUIMode.CHAT) {
            BotUI.setChatMessage(text, null, MessageType.USER);
        }
    }

    public setBotText = (text: string = null) => {
        if (BotUI.settings.guiMode === GUIMode.KIOSK) {
            BotUI.botTextKioskElement.setAttribute('data-empty', '');
            window.setTimeout(() => {
                if (isNil(text) || isEmpty(text)) {
                    BotUI.botTextKioskElement.setAttribute('data-empty', '');
                } else {
                    BotUI.botTextKioskElement.innerText = text;
                    BotUI.botTextKioskElement.removeAttribute('data-empty');
                }
            }, BotUI.settings.animationSpeed);
        }
        if (BotUI.settings.guiMode === GUIMode.CHAT) {
            BotUI.setChatMessage(text, null, MessageType.BOT);
        }
    }

    public setImage = (url: string = null) => {
        if (BotUI.settings.guiMode === GUIMode.KIOSK) {
            const cleanImageElement = (full = true) => {
                BotUI.imageKioskElement.innerHTML = '';
                if (full) {
                    BotUI.imageKioskElement.classList.add('bu-d-none');
                    BotUI.element.removeAttribute('data-width-image');
                }
            };
            if (!url) {
                cleanImageElement();
            } else {
                const image = new Image();
                const fac = new FastAverageColor();
                const root = document.documentElement;
                image.crossOrigin = 'anonymous';
                image.onload = (e) => {
                    cleanImageElement(false);
                    BotUI.imageKioskElement.classList.remove('bu-d-none');
                    BotUI.element.setAttribute('data-width-image', '');
                    BotUI.imageKioskElement.appendChild(image);

                    fac.getColorAsync(BotUI.imageKioskElement.querySelector('img'))
                        .then(function (color) {
                            root.style.setProperty('--image-background-color', `rgba(${color.value[0]}, ${color.value[1]}, ${color.value[2]}, ${BotUI.settings.imageAverageColorOpacity})`);
                        })
                        .catch(function (e) {
                            console.log(e);
                        });

                }
                image.onerror = () => {
                    cleanImageElement();
                }
                image.src = 'https://images1-focus-opensocial.googleusercontent.com/gadgets/proxy?container=focus&refresh=2592000&url=' + url;
            }
        }
        if (BotUI.settings.guiMode === GUIMode.CHAT) {
            BotUI.setChatMessage(null, url, MessageType.BOT);
        }
    }

    public setInputAudio = (samples: any = null) => {
        BotUI.botPcmElement.classList.add('bu-invisible');
        BotUI.botPcmElement.classList.remove('bu-visible');
        if (isNil(samples) || isEmpty(samples)) {
            BotUI.userPcmElement.classList.add('bu-invisible');
            BotUI.userPcmElement.classList.remove('bu-visible');
        } else {
            BotUI.userPcmElement.classList.add('bu-visible');
            BotUI.userPcmElement.classList.remove('bu-invisible');
        }
    }

    public setOutputAudio = (samples: any = null, sampleRate = 16000, stereo = false) => {
        BotUI.userPcmElement.classList.add('bu-invisible');
        BotUI.userPcmElement.classList.remove('bu-visible');
        if (isNil(samples) || isEmpty(samples)) {
            BotUI.botPcmElement.classList.add('bu-invisible');
            BotUI.botPcmElement.classList.remove('bu-visible');
        } else {
            BotUI.botPcmElement.classList.add('bu-visible');
            BotUI.botPcmElement.classList.remove('bu-invisible');
        }
    }

    public chatInputCallback = (...value) => {}

    public chatMicrophoneCallback = (...value) => {}

    public chatMuteCallback = (...value) => {}

    public chatBargeCallback = (...value) => {}

    public setMicrophone = (enable: boolean = false) => {
        BotUI.isMicrophoneEnabled = enable;
        BotUI._setMicrophone();
        BotUI.getChatMicrophone(BotUI.isMicrophoneEnabled, this.chatMicrophoneCallback);
    }

    public setUserMessageBackgroundColor = (color: string) => {
        const root = document.documentElement;
        const backgroundColor = isNil(color) || !is(String, color) ? BotUI.settings.userMessageBackgroundColor : color;
        root.style.setProperty('--message-background-user', backgroundColor);
    }

    public setBotMessageBackgroundColor = (color: string) => {
        const root = document.documentElement;
        const backgroundColor = isNil(color) || !is(String, color) ? BotUI.settings.botMessageBackgroundColor : color;
        root.style.setProperty('--message-background-bot', backgroundColor);
    }

    public setUserMessageTextColor = (color: string) => {
        const root = document.documentElement;
        const textColor = isNil(color) || !is(String, color) ? BotUI.settings.userMessageTextColor : color;
        root.style.setProperty('--message-color-user', textColor);
    }

    public setBotMessageTextColor = (color: string) => {
        const root = document.documentElement;
        const textColor = isNil(color) || !is(String, color) ? BotUI.settings.botMessageTextColor : color;
        root.style.setProperty('--message-color-bot', textColor);
    }

    private static getInputValue = (value: string, callback: Function) => callback(value);

    private static getChatMicrophone = (value: boolean, callback: Function) => {
        callback(value);
        (BotUI.chatInputElement as HTMLInputElement).value = '';
    }

    private static _setMicrophone = () => {
        if (BotUI.isMicrophoneEnabled) {
            sessionStorage.setItem(chatMicrophoneStorageKey, 'true');
            BotUI.chatInputMicrophoneElement.classList.remove('icon--light');
        } else {
            sessionStorage.removeItem(chatMicrophoneStorageKey);
            BotUI.chatInputMicrophoneElement.classList.add('icon--light');
        }
    }

    private static getChatMute = (value: boolean, callback: Function) => callback(value);

    private static setBackground = (background: Background) => {
        const { color = BotUI.settings.backgroundColor, url: { path, blur = BotUI.settings.backgroundImageBlur } = {} } = background;
        BotUI.settings.backgroundColor = color;
        BotUI.settings.backgroundImageBlur = blur;
        const root = document.documentElement;
        if (color) {
            root.style.setProperty('--background-color', color);
            BotUI.backgroundElement.classList.remove('background--image');
        }
        if (path) {
            root.style.setProperty('--background-url', `url("${path}")`);
            BotUI.backgroundElement.classList.add('background--image');
            root.style.setProperty('--background-url-blur', `${blur}px`);
        }
    }

    private static setChatMessage = (text: string, imageUrl: string, type: MessageType) => {
        const messageElement = BotUI.messagesElement;
        const messageTemplate = getContentAsHtml(chatMessageStructureTemplate);
        const messageTemplateElement = messageTemplate.querySelector('div.chat-message');
        const messageTemplateTextElement = messageTemplateElement.querySelector(':scope span');

        const { dataset: { messageType } = {} } = messageElement && messageElement.lastChild && <HTMLElement>messageElement.lastChild;
        if (messageType && messageType === type) {
            (messageElement.lastChild as Element).classList.remove('chat-message-last')
        }

        messageTemplateTextElement.innerHTML = text;
        messageTemplateElement.setAttribute('data-message-type', type);
        messageTemplateElement.classList.add('chat-message-' + type);
        messageTemplateElement.classList.add('chat-message-last');
        if (imageUrl) {
            const image = new Image();
            image.onload = (e) => {
                messageTemplateElement.appendChild(image);
                messageElement.scrollTop = messageElement.scrollHeight;
            }
            image.onerror = () => {

            }
            image.src = imageUrl;
        }
        messageElement.appendChild(messageTemplate.children[0]);
        messageElement.scrollTop = messageElement.scrollHeight;
        // messageElement.scrollIntoView({behavior: "smooth", block: "end", inline: "nearest"});
    }
}

(window as any).BotUI = BotUI;
