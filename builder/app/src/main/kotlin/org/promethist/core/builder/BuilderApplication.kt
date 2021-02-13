package org.promethist.core.builder

import org.promethist.common.AppConfig
import org.promethist.common.JerseyApplication
import org.promethist.common.ResourceBinder
import org.promethist.common.ServiceUrlResolver
import org.promethist.core.resources.FileResource

open class BuilderApplication : JerseyApplication() {

    val dataspace = AppConfig.instance.get("dsuffix", AppConfig.instance["namespace"])

    init {
        AppConfig.instance["name"] = "core-builder"

        register(object : ResourceBinder() {
            override fun configure() {

                bindAsContract(DialogueBuilder::class.java)

                val illusionistTrainingUrl = ServiceUrlResolver.getEndpointUrl("illusionist-training", namespace = dataspace)
                val illusionistBuilder = IllusionistModelBuilder(
                    illusionistTrainingUrl,
                    AppConfig.instance["illusionist.apiKey"],
                    AppConfig.instance.get("illusionist.approach", "logistic")
                )

                bind(illusionistBuilder).to(IntentModelBuilder::class.java)

                val coreUrl = ServiceUrlResolver.getEndpointUrl("core")

                // filestore
                bindTo(FileResource::class.java, "$coreUrl/file", AppConfig.instance["core.apiKey"])
            }
        })
    }
}