package org.promethist.core

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
import org.promethist.common.*
import org.promethist.common.ServerConfigProvider.ServerConfig
import org.promethist.common.messaging.MessageSender
import org.promethist.common.messaging.StdOutSender
import org.promethist.common.mongo.KMongoIdParamConverterProvider
import org.promethist.common.query.*
import org.promethist.core.context.ContextFactory
import org.promethist.core.context.ContextPersister
import org.promethist.core.model.Space
import org.promethist.core.model.SpaceImpl
import org.promethist.core.monitoring.Monitor
import org.promethist.core.monitoring.StdOutMonitor
import org.promethist.core.nlp.*
import org.promethist.core.provider.LocalFileStorage
import org.promethist.core.repository.ProfileRepository
import org.promethist.core.repository.mongo.MongoProfileRepository
import org.promethist.core.resources.*
import org.promethist.core.runtime.*
import org.promethist.core.servlets.AlexaSkillServlet
import org.promethist.core.servlets.BotCallServlet
import org.promethist.core.servlets.BotClientServlet
import org.promethist.core.servlets.GoogleAppServlet
import org.promethist.core.storage.FileStorage
import org.promethist.core.storage.GoogleStorage
import org.promethist.core.storage.AmazonS3Storage
import org.promethist.core.tts.TtsAudioService
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import javax.ws.rs.client.WebTarget
import javax.ws.rs.ext.ParamConverterProvider

open class RunnerApplication : JerseyApplication() {

    val dataspace = AppConfig.instance.get("dsuffix", AppConfig.instance["namespace"])

    init {
        AppConfig.instance["name"] = "core"

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
                                .getDatabase(AppConfig.instance["name"] + "-" + dataspace))
                bind(MongoProfileRepository::class.java).to(ProfileRepository::class.java)

                this.bindAsContract(DialogueLog::class.java).`in`(RequestScoped::class.java)

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
            else -> GoogleStorage().apply {
                bucket = "filestore-" + AppConfig.instance.get("dsuffix", AppConfig.instance["namespace"])
            }
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
        fun main(args: Array<String>) = JettyServer.run(RunnerApplication())
    }
}