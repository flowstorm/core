package org.promethist.client.standalone.cli

import com.beust.jcommander.Parameters
import cz.alry.jcommander.CommandRunner
import org.promethist.client.standalone.Application
import org.promethist.common.AppConfig

class VersionCommand: CommandRunner<Application.Config, VersionCommand.Config> {

    @Parameters(commandNames = ["version"], commandDescription = "Show application version")
    class Config

    override fun run(globalConfig: Application.Config, config: Config) {
        println("version ${AppConfig.version} commit ${AppConfig.instance["git.commit"]}")
    }
}