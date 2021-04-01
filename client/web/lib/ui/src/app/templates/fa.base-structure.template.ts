export const baseStructureTemplate = `
    <div data-wrapper class="bot bot-wrapper">
        <div class="column column-image">
            <div data-image class="image">
            </div>
        </div>
        <div data-avatar class="column column-avatar">
        </div>
        <div data-messages class="column column-messages">
        </div>
        <div data-chat-input class="chat-input">
            <div class="chat-input-wrapper">
                <input type="text" id="chatWindowTextInput" placeholder="Type text..."/>
                <span data-chat-input-settings class="settings settings--hidden">
                    <span data-chat-input-microphone>
                        <span data-chat-input-mic class="fas fa-mic-muted"></span>
                    </span>
                    <span data-chat-input-mute class="fas fa-volume-mute"></span>
                </span>
                <span data-chat-input-controls class="controls--visible">
                    <span data-chat-input-play class="fas fa-play"></span>
                    <span data-chat-input-stop class="fas fa-stop"></span>
                </span>
                <span data-chat-input-barge class="fas fa-mic"></span>
                <span data-chat-input-menu class="fas fa-menu"></span>
            </div>
        </div>
        <div data-user-pcm class="pcm pcm-user bu-invisible"></div>
        <div data-bot-pcm class="pcm pcm-bot bu-invisible"></div>
        <div data-background class="background">
        </div>
    </div>
`;
