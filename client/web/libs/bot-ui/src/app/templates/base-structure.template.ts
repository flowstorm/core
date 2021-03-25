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
                <input type="text" id="chatWindowTextInput"/>
                <span data-chat-input-barge class="icon icon--speaking-head"></span>
                <span data-chat-input-microphone class="icon icon--mic"></span>
                <span data-chat-input-mute class="icon icon--volume"></span>
            </div>
        </div>
        <div data-user-pcm class="pcm pcm-user bu-invisible"></div>
        <div data-bot-pcm class="pcm pcm-bot bu-invisible"></div>
        <div data-background class="background">
        </div>
    </div>
`;
