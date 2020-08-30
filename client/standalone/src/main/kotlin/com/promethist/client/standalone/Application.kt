package com.promethist.client.standalone

import com.promethist.client.standalone.cli.ClientCommand
import com.promethist.client.standalone.cli.ToolCommand
import com.promethist.client.standalone.cli.VersionCommand
import com.beust.jcommander.Parameter
import cz.alry.jcommander.CommandController

object Application {

    class Config {

        @Parameter(names = ["-l", "--log"], description = "Set logging severity")
        var logLevel: String? = "WARN"
    }

    @JvmStatic
    fun main(args: Array<String>) {
        //Sentry.init(AppConfig.instance["sentry.url"] + "?stacktrace.app.packages=com.promethistai,ai.promethist")
        val controller = CommandController(Config())
        controller.addCommand(VersionCommand.Config(), VersionCommand())
        controller.addCommand(ClientCommand.Config(), ClientCommand())
        controller.addCommand(ToolCommand.Config(), ToolCommand())
        controller.run(args)
    }
}