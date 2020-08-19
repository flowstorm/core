package com.promethist.client.standalone.cli

import com.beust.jcommander.Parameter
import com.promethist.common.AppConfig
import com.promethist.common.ServiceUrlResolver

open class ClientParams {

    @Parameter(names = ["-i", "--input"], order = 1, description = "Input file (e.g. stdin, none, /path/to/input.txt)")
    var input = "stdin"

    @Parameter(names = ["-o", "--output"], order = 2, description = "Output file (e.g. stdout, /path/to/output.txt)")
    var output = "stdout"

    @Parameter(names = ["-u", "--url"], order = 3, description = "Port URL")
    var url = ServiceUrlResolver.getEndpointUrl("port")

    @Parameter(names = ["-k", "--key"], order = 4, description = "Port contract key")
    var key = AppConfig.instance.get("port.key", "promethist")

    @Parameter(names = ["-x", "--secret"], order = 5, description = "Port contract secret (some contracts may require)")
    var secret: String? = null

    @Parameter(names = ["-l", "--language"], order = 6, description = "Preferred conversation language")
    var language = "en"

    @Parameter(names = ["-sd", "--speech-device"], order = 7, description = "Speech device (respeaker2)")
    var speechDeviceName = "none"

    @Parameter(names = ["-mc", "--mic-channel"], order = 8, description = "Microphone channel (count:selected)")
    var micChannel: String = "1:0"

    @Parameter(names = ["-spk", "--speaker"], order = 9, description = "Speaker name")
    var speakerName: String? = null
}