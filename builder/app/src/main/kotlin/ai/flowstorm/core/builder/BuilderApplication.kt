package ai.flowstorm.core.builder

import ai.flowstorm.common.AppConfig
import ai.flowstorm.common.JerseyApplication
import ai.flowstorm.common.ResourceBinder
import ai.flowstorm.common.ServiceUrlResolver
import ai.flowstorm.core.resources.FileResource

open class BuilderApplication : JerseyApplication() {

    val dataspace = AppConfig.instance.get("dsuffix", AppConfig.instance["namespace"])

    init {
        AppConfig.instance["name"] = "core-builder"

        register(object : ResourceBinder() {
            override fun configure() {

                bindAsContract(DialogueBuilder::class.java)

                val illusionistUrl = ServiceUrlResolver.getEndpointUrl("illusionist", namespace = dataspace)
                val illusionistBuilder = IllusionistModelBuilder(
                    illusionistUrl,
                    AppConfig.instance["illusionist.apiKey"],
                    AppConfig.instance.get("illusionist.approach", "logistic")
                )

                bind(illusionistBuilder).to(IntentModelBuilder::class.java)
                bind(illusionistBuilder).to(EntityModelBuilder::class.java)

                val coreUrl = ServiceUrlResolver.getEndpointUrl("core")

                // filestore
                bindTo(FileResource::class.java, "$coreUrl/file", AppConfig.instance["core.apiKey"])
            }
        })
    }
}