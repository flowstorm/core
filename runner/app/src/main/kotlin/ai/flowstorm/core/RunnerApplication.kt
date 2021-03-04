package ai.flowstorm.core

import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.databind.module.SimpleAbstractTypeResolver
import com.fasterxml.jackson.databind.module.SimpleModule
import com.mongodb.ConnectionString
import com.mongodb.client.MongoDatabase
import org.glassfish.hk2.api.Factory
import org.glassfish.hk2.api.PerLookup
import org.glassfish.hk2.utilities.binding.AbstractBinder
import org.glassfish.jersey.process.internal.RequestScoped
import org.litote.kmongo.KMongo
import ai.flowstorm.common.*
import ai.flowstorm.common.ServerConfigProvider.ServerConfig
import ai.flowstorm.common.messaging.MessageSender
import ai.flowstorm.common.messaging.StdOutSender
import ai.flowstorm.common.mongo.KMongoIdParamConverterProvider
import ai.flowstorm.common.monitoring.Monitor
import ai.flowstorm.common.query.*
import ai.flowstorm.core.context.ContextFactory
import ai.flowstorm.core.context.ContextPersister
import ai.flowstorm.core.model.Space
import ai.flowstorm.core.model.SpaceImpl
import ai.flowstorm.common.monitoring.StdOutMonitor
import ai.flowstorm.core.nlp.*
import ai.flowstorm.core.provider.LocalFileStorage
import ai.flowstorm.core.repository.EventRepository
import ai.flowstorm.core.repository.ProfileRepository
import ai.flowstorm.core.repository.SessionRepository
import ai.flowstorm.core.repository.TurnRepository
import ai.flowstorm.core.repository.mongo.MongoEventRepository
import ai.flowstorm.core.repository.mongo.MongoProfileRepository
import ai.flowstorm.core.repository.mongo.MongoSessionRepository
import ai.flowstorm.core.repository.mongo.MongoTurnRepository
import ai.flowstorm.core.resources.*
import ai.flowstorm.core.runtime.*
import ai.flowstorm.core.servlets.AmazonAlexaServlet
import ai.flowstorm.core.servlets.BotCallServlet
import ai.flowstorm.core.servlets.BotClientServlet
import ai.flowstorm.core.servlets.GoogleAssistantServlet
import ai.flowstorm.core.storage.FileStorage
import ai.flowstorm.core.storage.GoogleStorage
import ai.flowstorm.core.storage.AmazonS3Storage
import ai.flowstorm.core.tts.TtsAudioService
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import javax.ws.rs.client.WebTarget
import javax.ws.rs.ext.ParamConverterProvider

open class RunnerApplication : JerseyApplication() {

    init {
        AppConfig.instance["name"] = "core"
        logger.info("Creating application (dsuffix=$dsuffix)")
        System.setSecurityManager(DialogueSecurityManager(AppConfig.instance.get("security.raiseExceptions", "true") != "false"))

        register(object : ResourceBinder() {
            override fun configure() {
                bind(StdOutMonitor::class.java).to(Monitor::class.java).`in`(Singleton::class.java)
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
                bindAsContract(PipelineFactory::class.java)
                bindAsContract(ContextFactory::class.java)
                bindAsContract(ContextPersister::class.java)

                // IR component
                bind(Illusionist::class.java).to(Component::class.java).named("illusionist")
                bind(Action::class.java).to(Component::class.java).named("actionResolver")

                // DM component (third - dialog user input decides whether to process the rest of pipeline or not)
                bindAsContract(DialogueFactory::class.java)
                bind(DialogueManager::class.java).to(Component::class.java).named("dm")
                // Sentiment
                bind(Triton::class.java).to(Component::class.java).named("triton")
                // Duckling (time values)
                bind(Duckling::class.java).to(Component::class.java).named("duckling")
                // NER component (second)
                bind(Cassandra::class.java).to(Component::class.java).named("cassandra")
                // tokenizer (first)
                bind(InternalTokenizer::class.java).to(Component::class.java).named("tokenizer")

                /**
                 * Other components
                 */
                bind(StdOutSender::class.java).to(MessageSender::class.java).`in`(Singleton::class.java)

                //TODO replace by object repository
                bindTo(MongoDatabase::class.java,
                    KMongo.createClient(ConnectionString(AppConfig.instance["database.url"]))
                        .getDatabase(AppConfig.instance["name"] + "-" + dsuffix))
                bind(MongoProfileRepository::class.java).to(ProfileRepository::class.java)
                bind(MongoSessionRepository::class.java).to(SessionRepository::class.java)
                bind(MongoTurnRepository::class.java).to(TurnRepository::class.java)
                bind(MongoEventRepository::class.java).to(EventRepository::class.java)

                this.bindAsContract(DialogueLog::class.java).`in`(RequestScoped::class.java)

                bind(KMongoIdParamConverterProvider::class.java).to(ParamConverterProvider::class.java).`in`(Singleton::class.java)
                bindFactory(QueryValueFactory::class.java).to(Query::class.java).`in`(PerLookup::class.java)
            }
        })

        // register WebTargets
        register(object : AbstractBinder() {
            override fun configure() {
                //Intent recognition
                bind(RestClient.webTarget(ServiceUrlResolver.getEndpointUrl("illusionist"))
                        .path("/query")
                        .queryParam("key", AppConfig.instance["illusionist.apiKey"])
                ).to(WebTarget::class.java).named("illusionist")

                //Duckling
                bind(RestClient.webTarget(ServiceUrlResolver.getEndpointUrl("duckling"))
                        .path("/parse")
                ).to(WebTarget::class.java).named("duckling")

                //Casandra
                bind(RestClient.webTarget(ServiceUrlResolver.getEndpointUrl("cassandra"))
                        .path("/query")
                ).to(WebTarget::class.java).named("cassandra")

                //Triton
                bind(RestClient.webTarget(ServiceUrlResolver.getEndpointUrl("triton"))
                        .path("/v2/models")
                ).to(WebTarget::class.java).named("triton")
            }
        })

        registerModelImplementations()
    }

    private fun registerModelImplementations() {
        val module = SimpleModule("Models", Version.unknownVersion())
        val resolver = SimpleAbstractTypeResolver()
        resolver.addMapping(Space::class.java, SpaceImpl::class.java)
        module.setAbstractTypes(resolver)
        ObjectUtil.defaultMapper.registerModule(module)
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
            "AmazonS3" -> AmazonS3Storage()
            else -> GoogleStorage("filestore-$dsuffix")
        }
        override fun dispose(storage: FileStorage?) {}
    }

    override val serverConfig: ServerConfig
        get() = ServerConfig(this, super.serverConfig.servlets +
                mapOf(
                        BotClientServlet::class.java to "/socket/*",
                        GoogleAssistantServlet::class.java to "/google/*",
                        BotCallServlet::class.java to "/call/*",
                        AmazonAlexaServlet::class.java to "/alexa/*",
                )
        )

    companion object {

        val dsuffix = AppConfig.instance.get("dsuffix", AppConfig.instance["namespace"])

        @JvmStatic
        fun main(args: Array<String>) = JettyServer.run(RunnerApplication())
    }
}