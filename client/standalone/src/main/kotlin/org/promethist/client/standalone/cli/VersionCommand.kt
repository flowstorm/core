package org.promethist.client.standalone.cli

import org.promethist.client.standalone.Application
import com.beust.jcommander.Parameters
import org.promethist.common.AppConfig
import cz.alry.jcommander.CommandRunner

class VersionCommand: CommandRunner<Application.Config, VersionCommand.Config> {

    @Parameters(commandNames = ["version"], commandDescription = "Show application version")
    class Config

    override fun run(globalConfig: Application.Config, config: Config) {
        println("version ${AppConfig.version} commit ${AppConfig.instance["git.commit"]}")
    }
}