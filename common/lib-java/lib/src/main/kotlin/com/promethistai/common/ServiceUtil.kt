package com.promethistai.common

object ServiceUtil {

    val servicePorts = mapOf(
            "port" to 8080,
            "admin" to 8089,
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
                        "") + ".promethistai.com"
            RunMode.detect ->
                getEndpointUrl(serviceName, RunMode.valueOf(
                        System.getenv("RUN_MODE") ?: AppConfig.instance.get("runmode", "dist")))
        }

}