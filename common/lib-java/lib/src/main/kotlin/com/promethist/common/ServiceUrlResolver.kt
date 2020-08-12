package com.promethist.common

import com.promethist.util.LoggerDelegate

object ServiceUrlResolver {

    val logger by LoggerDelegate()

    val servicePorts = mapOf(
            "port" to 8080,
            "filestore" to 8082,
            "core" to 8088,
            "admin" to 8089,
            "illusionist" to 8090,
            "helena" to 8091,
            "editor" to 8092,
            "cassandra" to 8093,
            "cassandra-training" to 8094,
            "illusionist-training" to 8095,
            "duckling" to 8096
    )

    enum class RunMode { local, docker, dist, detect }

    fun getEndpointUrl(serviceName: String): String {
        val runMode = RunMode.valueOf(AppConfig.instance.get("$serviceName.runmode", AppConfig.instance.get("runmode", "dist")))
        return AppConfig.instance.get("$serviceName.url", getEndpointUrl(serviceName, runMode)).also {
            logger.debug("Resolved $serviceName = $it")
        }
    }

    fun getEndpointUrl(serviceName: String, runMode: RunMode = RunMode.detect): String =
            when (runMode) {
                RunMode.local -> "http://localhost:${servicePorts[serviceName]}"
                RunMode.docker -> "http://${serviceName}:8080"
                RunMode.dist -> "https://${serviceName}" +
                        (if (AppConfig.instance["namespace"] != "default")
                            "." + AppConfig.instance["namespace"]
                        else
                            "") + ".promethist.com"
                RunMode.detect ->
                    getEndpointUrl(serviceName, RunMode.valueOf(
                            System.getenv("RUN_MODE") ?: AppConfig.instance.get("runmode", "dist")))
            }
}