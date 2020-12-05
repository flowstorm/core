package org.promethist.client.standalone

import org.promethist.client.standalone.cli.ClientCommand
import org.promethist.client.standalone.cli.ToolCommand
import org.promethist.client.standalone.cli.VersionCommand
import org.promethist.client.standalone.cli.CallCommand
import org.promethist.common.AppConfig
import org.promethist.common.ServiceUrlResolver
import com.beust.jcommander.Parameter
import cz.alry.jcommander.CommandController
import io.sentry.Sentry

object Application {

    class Config {

        @Parameter(names = ["-l", "--logLevel"], description = "Set logging severity")
        var logLevel: String? = "WARN"
    }

    fun getServiceUrl(serviceName: String, environment: String, protocol: String = "http"): String {
        val env = if (listOf("production", "default").contains(environment)) "default" else environment
        return when (env) {
            "local" -> ServiceUrlResolver.getEndpointUrl(serviceName, ServiceUrlResolver.RunMode.local)
            else -> ServiceUrlResolver.getEndpointUrl(serviceName, ServiceUrlResolver.RunMode.dist, namespace = env, protocol = ServiceUrlResolver.Protocol.valueOf(protocol))
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        Sentry.init(AppConfig.instance["sentry.url"] + "?stacktrace.app.packages=org.promethist&release=" + AppConfig.instance["app.version"].let {
            if (it.startsWith('$')) "unknown" else it
        })
        val controller = CommandController(Config())
        controller.addCommand(VersionCommand.Config(), VersionCommand())
        controller.addCommand(ClientCommand.Config(), ClientCommand())
        controller.addCommand(CallCommand.Config(), CallCommand())
        controller.addCommand(ToolCommand.Config(), ToolCommand())
        controller.run(args)
    }
}