package com.promethistai.common

object ServiceUtil {

    val servicePorts = mapOf<String, Int>(
            "port" to 8080,
            "brainquist" to 8089,
            "brainquist-server" to 8089, //TODO remove - just for backward compatibility
            "illusionist" to 8090,
            "helena" to 8091,
            "editor" to 8092
    )

    enum class RunMode { local, docker, dist, detect }

    fun getEndpointUrl(serviceName: String, runMode: RunMode = RunMode.detect): String =
        when (runMode) {
            RunMode.local -> "http://localhost:${servicePorts[serviceName]}"
            RunMode.docker -> "http://${serviceName}:8080"
            RunMode.dist -> "https://${serviceName}" +
                    (if (AppConfig.instance["namespace"] != "default")
                        "." + AppConfig.instance["namespace"]
                    else
                        "") + ".promethist.ai"
            RunMode.detect ->
                getEndpointUrl(serviceName, RunMode.valueOf(
                        System.getenv("RUN_MODE") ?: AppConfig.instance.get("runmode", "local")))
        }

}