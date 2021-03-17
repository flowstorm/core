package ai.flowstorm.core.builder

import ai.flowstorm.common.*
import ai.flowstorm.core.resources.FileResource

open class BuilderApplication : JerseyApplication() {

    init {
        config["name"] = "core-builder"

        register(object : ResourceBinder() {
            override fun configure() {

                bindAsContract(DialogueBuilder::class.java)

                val illusionistUrl = ServiceUrlResolver.getEndpointUrl("illusionist", namespace = config.dsuffix)
                val illusionistBuilder = IllusionistModelBuilder(
                    illusionistUrl,
                    config["illusionist.apiKey"],
                    config.get("illusionist.approach", "logistic")
                )

                bind(illusionistBuilder).to(IntentModelBuilder::class.java)
                bind(illusionistBuilder).to(EntityModelBuilder::class.java)

                val coreUrl = ServiceUrlResolver.getEndpointUrl("core")

                // filestore
                bindTo(FileResource::class.java, "$coreUrl/file", config["core.apiKey"])
            }
        })
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = JettyServer.run(BuilderApplication())
    }
}