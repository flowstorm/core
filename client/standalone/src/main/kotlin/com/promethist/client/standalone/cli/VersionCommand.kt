package com.promethist.client.standalone.cli

import com.promethist.client.standalone.Application
import com.beust.jcommander.Parameters
import com.promethist.common.AppConfig
import cz.alry.jcommander.CommandRunner

class VersionCommand: CommandRunner<Application.Config, VersionCommand.Config> {

    @Parameters(commandNames = ["version"], commandDescription = "Show application version")
    class Config

    override fun run(globalConfig: Application.Config, config: Config) {
        println("version ${AppConfig.version} commit ${AppConfig.instance["git.commit"]}")
    }
}