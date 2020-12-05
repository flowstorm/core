package org.promethist.client

import org.promethist.core.model.LogEntry

interface BotClientCallback {

    // event handlers

    fun onOpen(client: BotClient)

    fun onReady(client: BotClient)

    fun onClose(client: BotClient)

    fun onError(client: BotClient, text: String)

    fun onFailure(client: BotClient, t: Throwable)

    fun onRecognized(client: BotClient, text: String)

    fun onSessionId(client: BotClient, sessionId: String?)

    fun onLog(client: BotClient, logs: MutableList<LogEntry>)

    fun onBotStateChange(client: BotClient, newState: BotClient.State)

    fun onWakeWord(client: BotClient)

    // content operations

    fun text(client: BotClient, text: String)

    fun audio(client: BotClient, data: ByteArray)

    fun audioCancel()

    fun image(client: BotClient, url: String)

    fun video(client: BotClient, url: String)

    fun command(client: BotClient, command: String, code: String?)

    fun httpRequest(client: BotClient, url: String, request: HttpRequest? = null): ByteArray?
}