package com.promethist.client.standalone

import com.promethist.client.standalone.cli.ClientCommand
import com.promethist.client.standalone.cli.ToolCommand
import com.promethist.client.standalone.cli.VersionCommand
import com.beust.jcommander.Parameter
import com.promethist.client.standalone.cli.CallCommand
import com.promethist.common.ServiceUrlResolver
import cz.alry.jcommander.CommandController

object Application {

    class Config {

        @Parameter(names = ["-l", "--logLevel"], description = "Set logging severity")
        var logLevel: String? = "WARN"
    }

    fun getServiceUrl(serviceName: String, environment: String, protocol: String = "http"): String {
        val env = if (listOf("production", "default").contains(environment))
            ""
        else
            ".${environment}"
        return if (environment == "local")
            "${protocol}://localhost:" + ServiceUrlResolver.servicePorts[serviceName]
        else
            "${protocol}s://${serviceName}${env}.promethist.com"
    }

    @JvmStatic
    fun main(args: Array<String>) {
        //Sentry.init(AppConfig.instance["sentry.url"] + "?stacktrace.app.packages=com.promethistai,ai.promethist")

        val controller = CommandController(Config())
        controller.addCommand(VersionCommand.Config(), VersionCommand())
        controller.addCommand(ClientCommand.Config(), ClientCommand())
        controller.addCommand(CallCommand.Config(), CallCommand())
        controller.addCommand(ToolCommand.Config(), ToolCommand())
        controller.run(args)
    }
}