package com.promethist.client.standalone.cli

import com.promethist.client.standalone.Application
import com.beust.jcommander.Parameters
import com.promethist.client.standalone.io.RespeakerMicArrayV2
import com.promethist.common.AppConfig
import cz.alry.jcommander.CommandRunner

class VersionCommand: CommandRunner<Application.Params, VersionCommand.Params> {

    @Parameters(commandNames = ["version"], commandDescription = "Show application version")
    class Params

    override fun run(globalParams: Application.Params, params: Params) {
        System.out.println("${AppConfig.instance["app.version"]} commit ${AppConfig.instance["git.commit"]}")
    }
}