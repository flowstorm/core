package org.promethist.client.standalone.cli

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import org.promethist.client.standalone.Application
import com.twilio.http.TwilioRestClient
import com.twilio.rest.api.v2010.account.Call
import com.twilio.type.PhoneNumber
import com.twilio.type.Twiml
import cz.alry.jcommander.CommandRunner

class CallCommand: CommandRunner<Application.Config, CallCommand.Config> {

    @Parameters(commandNames = ["call"], commandDescription = "Call")
    class Config {

        @Parameter(names = ["-e", "--environment"], order = 0, description = "Environment (develop, preview, production)")
        var environment: String? = null

        @Parameter(names = ["-u", "--url"], order = 1, description = "Custom port URL")
        var url: String? = null

        @Parameter(names = ["-a", "--account"], order = 2, required = true, description = "Twilio Account SID")
        lateinit var account: String

        @Parameter(names = ["-t", "--token"], order = 3, required = true, description = "Twilio Auth Token")
        lateinit var token: String

        @Parameter(names = ["-f", "--from"], order = 4, required = true, description = "Call from number")
        lateinit var from: String

        @Parameter(names = ["-o", "--to"], order = 5, required = true, description = "Call to number")
        lateinit var to: String

        @Parameter(names = ["-k", "--key"], order = 6, required = true, description = "Conversation key")
        lateinit var key: String

        @Parameter(names = ["-l", "--language"], order = 7, description = "Preferred conversation language")
        var language = "en"
    }

    override fun run(globalConfig: Application.Config, config: Config) {
        val portUrl = config.url ?: Application.getServiceUrl("core", config.environment ?: "production", "ws")
        val twiml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Response>
                      <Connect>
                        <Stream url="${portUrl}/call/">
                          <Parameter name="locale" value="${config.language}" />
                          <Parameter name="sender" value="${config.to}" />
                          <Parameter name="appKey" value="${config.key}" />
                        </Stream>  
                      </Connect>
                    </Response>
                """.trimIndent()
        val call = Call.creator(PhoneNumber(config.to), PhoneNumber(config.from), Twiml(twiml))
                .create(TwilioRestClient.Builder(config.account, config.token).build())

        println("$twiml\nSID: ${call.sid}")
    }
}