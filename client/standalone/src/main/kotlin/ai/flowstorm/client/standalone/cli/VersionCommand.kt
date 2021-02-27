package ai.flowstorm.client.standalone.cli

import com.beust.jcommander.Parameters
import cz.alry.jcommander.CommandRunner
import ai.flowstorm.client.standalone.Application
import ai.flowstorm.common.AppConfig

class VersionCommand: CommandRunner<Application.Config, VersionCommand.Config> {

    @Parameters(commandNames = ["version"], commandDescription = "Show application version")
    class Config

    override fun run(globalConfig: Application.Config, config: Config) {
        println("version ${AppConfig.version} commit ${AppConfig.instance["git.commit"]}")
    }
}