package com.promethist.client.standalone

import com.promethist.client.standalone.cli.ClientCommand

object DefaultApplication {
    @JvmStatic
    fun main(args: Array<String>) {
        ClientCommand().run(Application.Params(), ClientCommand.Params().apply {
            screen = "window"
            environment = "preview"
            key = "promethist://zayda/movie-game/1"
        })
    }
}