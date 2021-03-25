export interface Settings {
    animationSpeed?: number;
    backgroundAdvancedAnimationParticlesCount?: number;
    backgroundColor?: string;
    backgroundImageBlur?: number;
    backgroundSimpleAnimation?: boolean;
    detectOrientation?: boolean;
    fullScreen?: boolean;
    guiMode: GUIMode,
    imageAverageColorOpacity?: number;
    userMessageBackgroundColor?: string;
    userMessageTextColor?: string;
    botMessageBackgroundColor?: string;
    botMessageTextColor?: string;
    widgetSize?: {
        height: string;
        width: string;
    };
}

export interface Background {
    color?: string;
    url?: {
        blur: number;
        path: string;
    };
}

export enum ScreenTypeEnum {
    PLAYER = 'player',
    LIST = 'list',
    SETTINGS = 'settings',
}

export enum StateTypeEnum {
    CLOSED = 'closed',
    FAILED = 'failed',
    LISTENING = 'listening',
    OPEN = 'open',
    PAUSED = 'paused',
    RESPONDING = 'responding',
    PROCESSING = 'processing',
    SLEEPING = 'sleeping',
}

export enum OrientationEnum {
    LANDSCAPE = 'landscape',
    PORTRAIT = 'portrait',
}

export enum GUIMode {
    CHAT = 'chat',
    KIOSK = 'kiosk',
}

export enum MessageType {
    BOT = 'bot',
    USER = 'user',
}
