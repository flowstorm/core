package org.promethist.client.standalone

import org.promethist.client.standalone.cli.ClientCommand

object DefaultApplication {
    @JvmStatic
    fun main(args: Array<String>) {
        ClientCommand().run(Application.Config(), ClientCommand.Config().apply {
            screen = "window"
            environment = "preview"
            key = "promethist://zayda/movie-game/1"
        })
    }
}