package org.promethist.client.standalone

import org.promethist.client.BotClient
import org.promethist.client.BotClientCallback
import org.promethist.client.BotContext
import org.promethist.client.HttpRequest
import org.promethist.client.common.OkHttp3BotClientSocket
import org.promethist.client.util.HttpUtil
import org.promethist.client.util.InetInterface
import org.promethist.common.TextConsole
import org.promethist.core.model.LogEntry
import org.promethist.core.type.Dynamic

class SampleClient(context: BotContext) : TextConsole() {

    inner class Callback : BotClientCallback {

        override fun onOpen(client: BotClient) = println("{Open}")
        override fun onReady(client: BotClient) = println("{Ready}")
        override fun onClose(client: BotClient) = println("{Closing}")
        override fun onError(client: BotClient, text: String) = println("{Error $text}")
        override fun onFailure(client: BotClient, t: Throwable) = t.printStackTrace()
        override fun audioCancel() = println("{Audio Cancel}")
        override fun command(client: BotClient, command: String, code: String?) = println("{Command $command $code}")
        override fun httpRequest(client: BotClient, url: String, request: HttpRequest?) = HttpUtil.httpRequest(url, request)

        override fun onSessionId(client: BotClient, sessionId: String?) = println("{Session ID $sessionId}")
        override fun onBotStateChange(client: BotClient, newState: BotClient.State) = println("{State change to $newState}")
        override fun onWakeWord(client: BotClient) = println("{Wake word}")

        override fun audio(client: BotClient, data: ByteArray) = println("{Audio ${data.size} bytes}")
        override fun video(client: BotClient, url: String) = println("{Video $url}")
        override fun image(client: BotClient, url: String) = println("{image $url}")
        override fun text(client: BotClient, text: String) = println("< $text")

        override fun onRecognized(client: BotClient, text: String) = println("> $text")
        override fun onLog(client: BotClient, logs: MutableList<LogEntry>) = logs.forEach { println(it) }
    }

    val client = BotClient(context, OkHttp3BotClientSocket(context.url), null, Callback())

    fun open() = client.open()

    override fun afterInput(text: String) = client.doText(text)

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val context = BotContext(
                "https://core.promethist.com",
                if (args.isNotEmpty()) args[0] else error("Missing app Key"), // application ID
                "sample_" + InetInterface.getActive()?.hardwareAddress?.replace(":", ""),
                Dynamic("clientType" to "sample:1.0.0-SNAPSHOT"), // CHANGE TO your-client-name:version
                autoStart = false
            )
            SampleClient(context).apply {
                open()
                run()
            }
        }
    }
}