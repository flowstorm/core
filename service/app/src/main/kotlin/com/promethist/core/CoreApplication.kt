package com.promethist.core

import com.mongodb.ConnectionString
import com.mongodb.client.MongoDatabase
import com.promethist.common.*
import com.promethist.common.ServerConfigProvider.ServerConfig
import com.promethist.common.mongo.KMongoIdParamConverterProvider
import com.promethist.common.query.*
import com.promethist.common.services.DummySender
import com.promethist.core.context.ContextFactory
import com.promethist.core.context.ContextPersister
import com.promethist.core.monitoring.StdOutMonitor
import com.promethist.core.monitoring.Monitor
import com.promethist.core.nlp.*
import com.promethist.core.profile.MongoProfileRepository
import com.promethist.core.profile.ProfileRepository
import com.promethist.core.provider.LocalFileStorage
import com.promethist.core.resources.*
import com.promethist.core.runtime.*
import com.promethist.core.servlets.AlexaSkillServlet
import com.promethist.core.servlets.BotCallServlet
import com.promethist.core.servlets.BotClientServlet
import com.promethist.core.servlets.GoogleAppServlet
import com.promethist.core.storage.FileStorage
import com.promethist.core.storage.GoogleStorage
import com.promethist.core.tts.TtsAudioService
import com.promethist.services.MessageSender
import org.glassfish.hk2.api.Factory
import org.glassfish.hk2.api.PerLookup
import org.glassfish.hk2.utilities.binding.AbstractBinder
import org.glassfish.jersey.process.internal.RequestScoped
import org.litote.kmongo.KMongo
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import javax.ws.rs.client.WebTarget
import javax.ws.rs.ext.ParamConverterProvider

open class CoreApplication : JerseyApplication() {

    private val runMode = ServiceUrlResolver.RunMode.valueOf(AppConfig.instance.get("runmode", "dist"))
    val dataspace = AppConfig.instance.get("dsuffix", AppConfig.instance["namespace"])

    init {
        AppConfig.instance["name"] = "core"
        AppConfig.instance.let {
            println("App ${it["git.ref"]} starting with namespace = ${it["namespace"]}, dataspace = $dataspace, runMode = $runMode")
        }

        register(object : ResourceBinder() {
            override fun configure() {
                bind(StdOutMonitor::class.java).to(Monitor::class.java)
                bindFactory(FileStorageFactory::class.java).to(FileStorage::class.java).`in`(Singleton::class.java)
                bindFactory(FileResourceLoaderFactory::class.java).to(Loader::class.java).`in`(Singleton::class.java)

                //we only need to register resource implementations when they are required to be injected into another class
                bind(CommunityResourceImpl::class.java).to(CommunityResource::class.java)
                bind(SessionResourceImpl::class.java).to(SessionResource::class.java)
                bind(DevicePairingResourceImpl::class.java).to(DevicePairingResource::class.java)
                bind(DialogueEventResourceImpl::class.java).to(DialogueEventResource::class.java)

                bind(TtsAudioService::class.java).to(TtsAudioService::class.java).`in`(Singleton::class.java)

                /**
                 * NLP components
                 */
                bindTo(PipelineFactory::class.java)
                bindTo(ContextFactory::class.java)
                bindTo(ContextPersister::class.java)

                // IR component
                bind(Illusionist::class.java).to(Component::class.java).named("illusionist")
                bind(Action::class.java).to(Component::class.java).named("actionResolver")

                // DM component (third - dialog user input decides whether to process the rest of pipeline or not)
                bindTo(DialogueFactory::class.java)
                bind(DialogueManager::class.java).to(Component::class.java).named("dm")
                // Duckling (time values)
                bind(Duckling::class.java).to(Component::class.java).named("duckling")
                // NER component (second)
                bind(Cassandra::class.java).to(Component::class.java).named("cassandra")
                // tokenizer (first)
                bind(InternalTokenizer::class.java).to(Component::class.java).named("tokenizer")

                /**
                 * Other components
                 */
                bind(DummySender::class.java).to(MessageSender::class.java)

                //TODO replace by object repository
                bindTo(MongoDatabase::class.java,
                        KMongo.createClient(ConnectionString(AppConfig.instance["database.url"]))
                                .getDatabase(AppConfig.instance["name"] + "-" + dataspace))
                bind(MongoProfileRepository::class.java).to(ProfileRepository::class.java)

                bind(DialogueLog::class.java).to(DialogueLog::class.java).`in`(RequestScoped::class.java)

                bind(KMongoIdParamConverterProvider::class.java).to(ParamConverterProvider::class.java).`in`(Singleton::class.java)
                bindFactory(QueryValueFactory::class.java).to(Query::class.java).`in`(PerLookup::class.java)
            }
        })

        // register WebTargets
        register(object : AbstractBinder() {
            override fun configure() {
                //Intent recognition
                bind(RestClient.webTarget(ServiceUrlResolver.getEndpointUrl("illusionist", namespace = dataspace))
                        .path("/query")
                        .queryParam("key", AppConfig.instance["illusionist.apiKey"])
                ).to(WebTarget::class.java).named("illusionist")

                //Duckling
                bind(RestClient.webTarget(ServiceUrlResolver.getEndpointUrl("duckling", namespace = dataspace))
                        .path("/parse")
                ).to(WebTarget::class.java).named("duckling")

                //Casandra
                bind(RestClient.webTarget(ServiceUrlResolver.getEndpointUrl("cassandra", namespace = dataspace))
                        .path("/query")
                ).to(WebTarget::class.java).named("cassandra")

            }
        })
    }

    class FileResourceLoaderFactory : Factory<FileResourceLoader> {
        @Inject
        lateinit var fileStorage: FileStorage

        override fun provide(): FileResourceLoader = FileResourceLoader("dialogue",
                AppConfig.instance.get("loader.noCache", "false") == "true",
                AppConfig.instance.get("loader.useScript", "false") == "true").apply {
            fileStorage = this@FileResourceLoaderFactory.fileStorage
        }
        override fun dispose(p0: FileResourceLoader?) {}
    }

    class FileStorageFactory : Factory<FileStorage> {
        override fun provide(): FileStorage = when (AppConfig.instance.get("storage.type", "Google")) {
            "FileSystem" -> LocalFileStorage(File(AppConfig.instance["storage.base"]))
            else -> GoogleStorage()
        }
        override fun dispose(p0: FileStorage?) {}
    }

    override val serverConfig: ServerConfig
        get() = ServerConfig(this, super.serverConfig.servlets +
                mapOf(
                        BotClientServlet::class.java to "/socket/*",
                        GoogleAppServlet::class.java to "/google/*",
                        BotCallServlet::class.java to "/call/*",
                        AlexaSkillServlet::class.java to "/alexa/*",
                )
        )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = JettyServer.run(CoreApplication())
    }
}