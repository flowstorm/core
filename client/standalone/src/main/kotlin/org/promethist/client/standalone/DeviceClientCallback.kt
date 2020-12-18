package org.promethist.client.standalone

import javazoom.jl.player.Player
import org.promethist.client.BotClient
import org.promethist.client.BotClientCallback
import org.promethist.client.HttpRequest
import org.promethist.client.standalone.io.OutputAudioDevice
import org.promethist.client.standalone.ui.Screen
import org.promethist.client.util.HttpUtil
import org.promethist.core.model.LogEntry
import org.promethist.util.LoggerDelegate
import java.io.ByteArrayInputStream
import java.io.File
import java.io.PrintWriter

open class DeviceClientCallback(
        private val output: PrintWriter,
        private val distUrl: String? = null,
        private val doUpdate: Boolean = true,
        private val noCache: Boolean = false,
        private val noOutputAudio: Boolean = false,
        private val noOutputLogs: Boolean = false,
        private val outputPortName: String = "SPEAKER",
        private val exitOnError: Boolean = false,
        private val logs: Boolean = false
) : BotClientCallback {

    private var jarUpdater: JarUpdater? = null
    private var audioCancelled = false
    private val logger by LoggerDelegate()

    override fun onOpen(client: BotClient) {
        val sourcePath = this::class.java.protectionDomain.codeSource.location.toURI().path
        if (distUrl != null) {
            if (sourcePath.endsWith(".jar")) {
                logger.info("starting auto update for file $sourcePath from $distUrl")
                jarUpdater = JarUpdater(distUrl, File(sourcePath), doUpdate = doUpdate).apply {
                    Thread(this).start()
                }
            } else {
                logger.warn("auto update requested but source path $sourcePath is not JAR file")
            }
        }
    }

    override fun onReady(client: BotClient) {
        if (!noOutputLogs)
            output.println("{Ready}")
    }

    override fun onClose(client: BotClient) {
        if (!noOutputLogs)
            output.println("{Closed}")
    }

    override fun onError(client: BotClient, text: String) {
        output.println("{Error: $text}")
        if (exitOnError)
            System.exit(1)
    }

    override fun onFailure(client: BotClient, t: Throwable) {
        output.println("{Failure ${t.message}}")
        if (exitOnError)
            System.exit(2)
    }

    override fun onSessionId(client: BotClient, sessionId: String?) = output.println("{Session $sessionId}")

    override fun onRecognized(client: BotClient, text: String) {
        Screen.instance?.viewUserText(text)
        output.println("> $text")
    }

    override fun text(client: BotClient, text: String) {
        Screen.instance?.viewBotText(text)
        output.println("< ${text}")
    }

    override fun onLog(client: BotClient, logs: MutableList<LogEntry>) = logs.forEach {
        if (this.logs) {
            val time = "%.2f".format(it.relativeTime)
            output.println("+$time:${it.level}:[${it.text}]")
        }
    }

    override fun onBotStateChange(client: BotClient, newState: BotClient.State) {
        if (client.state != newState) {
            output.println("{${client.state} > $newState}")
            Screen.instance?.stateChange(client.state, newState)
        }
        jarUpdater?.allowed = (newState == BotClient.State.Sleeping)
    }

    override fun onWakeWord(client: BotClient) {
        output.println("{Wake word detected}")
    }

    override fun audio(client: BotClient, data: ByteArray) {
        if (!noOutputAudio) {
            audioCancelled = false
            val player = Player(ByteArrayInputStream(data))
            while (!player.isComplete && !audioCancelled) {
                while (client.state == BotClient.State.Paused) {
                    Thread.sleep(20)
                }
                player.play(5)
            }
            player.close()
        }
    }

    override fun audioCancel() {
       audioCancelled = true
    }

    override fun image(client: BotClient, url: String) {
        if (Screen.instance != null) {
            Screen.instance?.viewImage(ByteArrayInputStream(HttpUtil.httpRequest(url)))
        }
        output.println("{Image $url}")
    }

    override fun video(client: BotClient, url: String) {
        if (Screen.instance != null) {
            Screen.instance?.viewMedia(url)
        }
        output.println("{Video $url}")
    }

    override fun command(client: BotClient, command: String, code: String?) {
        output.println("{Command $command $code}")
        if (command.startsWith("volume")) {
            with (OutputAudioDevice) {
                val value = volume(outputPortName,
                    when {
                        code?.equals("up", true) == true -> VOLUME_UP
                        code?.equals("down", true) == true -> VOLUME_DOWN
                        else -> code?.toInt() ?: 7
                    }
                )
                output.println("{Volume $value}")
            }
        }
    }

    override fun httpRequest(client: BotClient, url: String, request: HttpRequest?): ByteArray? = HttpUtil.httpRequest(url, request, !noCache)
}