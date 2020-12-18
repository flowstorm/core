package org.promethist.core.builder

import org.promethist.common.AppConfig
import org.promethist.common.JerseyApplication
import org.promethist.common.ResourceBinder

open class BuilderApplication : JerseyApplication() {

    init {
        AppConfig.instance["name"] = "core-builder"

        register(object : ResourceBinder() {
            override fun configure() {
                //TODO
            }
        })
    }
}