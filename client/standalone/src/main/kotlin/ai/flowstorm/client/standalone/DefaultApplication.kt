package ai.flowstorm.client.standalone

import ai.flowstorm.client.standalone.cli.ClientCommand

object DefaultApplication {
    @JvmStatic
    fun main(args: Array<String>) {
        ClientCommand().run(Application.Config(), ClientCommand.Config().apply {
            screen = "window"
            environment = "preview"
            key = "flowstorm://zayda/movie-game/1"
        })
    }
}