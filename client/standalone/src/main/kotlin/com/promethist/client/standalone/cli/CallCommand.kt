package com.promethist.client.standalone.cli

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.promethist.client.standalone.Application
import com.twilio.Twilio
import com.twilio.rest.api.v2010.account.Call
import com.twilio.type.PhoneNumber
import com.twilio.type.Twiml
import cz.alry.jcommander.CommandRunner

class CallCommand: CommandRunner<Application.Config, CallCommand.Config> {

    @Parameters(commandNames = ["call"], commandDescription = "Call")
    class Config {

        @Parameter(names = ["-e", "--environment"], order = 0, description = "Environment (develop, preview, production)")
        var environment: String? = null

        @Parameter(names = ["-u", "--username"], order = 1, description = "Twilio Account SID")
        lateinit var username: String

        @Parameter(names = ["-p", "--password"], order = 2, description = "Twilio Auth Token")
        lateinit var password: String

        @Parameter(names = ["-f", "--from"], order = 3, description = "Call from number")
        lateinit var from: String

        @Parameter(names = ["-t", "--to"], order = 4, description = "Call to number")
        lateinit var to: String

        @Parameter(names = ["-k", "--key"], order = 5, description = "Conversation key")
        lateinit var key: String

        @Parameter(names = ["-l", "--language"], order = 6, description = "Preferred conversation language")
        var language = "en"
    }

    override fun run(globalConfig: Application.Config, config: Config) {
        Twilio.init(config.username, config.password)
        val call = Call.creator(PhoneNumber(config.to), PhoneNumber(config.from),
                Twiml("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Response>
                      <Connect>
                        <Stream url="${Application.getServiceUrl("port", config.environment ?: "production", "ws")}/call/">
                          <Parameter name="locale" value="${config.language}" />
                          <Parameter name="sender" value="${config.to}" />
                          <Parameter name="appKey" value="${config.key}" />
                        </Stream>  
                      </Connect>
                    </Response>
                """.trimIndent()))
                .create()

        println(call.sid)
    }
}