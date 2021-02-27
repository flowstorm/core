package ai.flowstorm.client.standalone

import com.beust.jcommander.Parameter
import cz.alry.jcommander.CommandController
import io.sentry.Sentry
import ai.flowstorm.client.standalone.cli.CallCommand
import ai.flowstorm.client.standalone.cli.ClientCommand
import ai.flowstorm.client.standalone.cli.ToolCommand
import ai.flowstorm.client.standalone.cli.VersionCommand
import ai.flowstorm.common.AppConfig
import ai.flowstorm.common.ServiceUrlResolver

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
        Sentry.init(AppConfig.instance["sentry.url"] + "?stacktrace.app.packages=ai.flowstorm&release=" + AppConfig.instance["app.version"].let {
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