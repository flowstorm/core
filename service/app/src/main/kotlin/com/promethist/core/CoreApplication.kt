package com.promethist.core

import com.commit451.mailgun.Contact
import com.commit451.mailgun.Mailgun
import com.mongodb.ConnectionString
import com.mongodb.client.MongoDatabase
import com.promethist.common.*
import com.promethist.common.ServerConfigProvider.ServerConfig
import com.promethist.common.mongo.KMongoIdParamConverterProvider
import com.promethist.common.query.*
import com.promethist.services.MessageSender
import com.promethist.common.services.MailgunSender
import com.promethist.core.context.ContextFactory
import com.promethist.core.context.ContextPersister
import com.promethist.core.storage.GoogleStorage
import com.promethist.core.profile.MongoProfileRepository
import com.promethist.core.profile.ProfileRepository
import com.promethist.core.resources.*
import com.promethist.core.runtime.*
import com.promethist.core.nlp.*
import com.promethist.core.provider.LocalFileStorage
import com.promethist.core.servlets.AlexaSkillServlet
import com.promethist.core.servlets.BotCallServlet
import com.promethist.core.servlets.BotClientServlet
import com.promethist.core.servlets.GoogleAppServlet
import com.promethist.core.storage.FileStorage
import com.promethist.core.tts.TtsAudioService
import org.glassfish.hk2.api.PerLookup
import org.glassfish.jersey.process.internal.RequestScoped
import org.litote.kmongo.KMongo
import java.io.File
import javax.inject.Singleton
import javax.ws.rs.ext.ParamConverterProvider

open class CoreApplication : JerseyApplication() {

    init {
        AppConfig.instance["name"] = "core"

        Monitoring.init()
        register(object : ResourceBinder() {
            override fun configure() {

                val runMode = ServiceUrlResolver.RunMode.valueOf(AppConfig.instance.get("runmode", "dist"))
                val dataspace = AppConfig.instance.get("dsuffix", AppConfig.instance["namespace"])
                AppConfig.instance.let {
                    println("Promethist ${it["git.ref"]} starting with namespace = ${it["namespace"]}, dataspace = $dataspace, runMode = $runMode")
                }

                val fileStorage = when (AppConfig.instance.get("storage.type", "Google")) {
                    "FileSystem" -> LocalFileStorage(File(AppConfig.instance["storage.base"]))
                    else -> GoogleStorage()
                }
                TtsAudioService.fileStorage = fileStorage //FIXME servlet part + tts service should be in DI

                /**
                 * NLP components
                 */
                bindTo(PipelineFactory::class.java)
                bindTo(ContextFactory::class.java)
                bindTo(ContextPersister::class.java)

                // IR component
                val illusionistUrl = ServiceUrlResolver.getEndpointUrl("illusionist", namespace = dataspace)
                val illusionist = Illusionist()
                illusionist.webTarget = RestClient.webTarget(illusionistUrl)
                        .path("/query")
                        .queryParam("key", AppConfig.instance["illusionist.apiKey"])
                bind(illusionist).to(Component::class.java).named("illusionist")

                bind(Action::class.java).to(Component::class.java).named("actionResolver")

                // DM component (third - dialog user input decides whether to process the rest of pipeline or not)
                val dialogueFactory = DialogueFactory(FileResourceLoader(fileStorage, "dialogue",
                        AppConfig.instance.get("loader.noCache", "false") == "true",
                        AppConfig.instance.get("loader.useScript", "false") == "true"))
                bind(dialogueFactory).to(DialogueFactory::class.java)
                bind(DialogueManager::class.java).to(Component::class.java).named("dm")

                // Duckling (time values)
                val ducklingUrl = ServiceUrlResolver.getEndpointUrl("duckling", namespace = dataspace)
                val duckling = Duckling()
                duckling.webTarget = RestClient.webTarget(ducklingUrl)
                        .path("/parse")

                bind(duckling).to(Component::class.java).named("duckling")

                // NER component (second)
                val cassandraUrl = ServiceUrlResolver.getEndpointUrl("cassandra", namespace = dataspace)
                val cassandra = Cassandra()
                cassandra.webTarget = RestClient.webTarget(cassandraUrl)
                        .path("/query")

                bind(cassandra).to(Component::class.java).named("cassandra")

                // tokenizer (first)
                bind(InternalTokenizer()).to(Component::class.java).named("tokenizer")
                /**
                 * Other components
                 */
                val mailgun = Mailgun.Builder(AppConfig.instance["mailgun.domain"], AppConfig.instance["mailgun.apikey"])
                        .baseUrl(AppConfig.instance["mailgun.baseUrl"])
                        .build()
                bindTo(MessageSender::class.java, MailgunSender(mailgun, Contact(
                        AppConfig.instance.get("sender.from.email", "bot@promethist.ai"),
                        AppConfig.instance.get("sender.from.name", "Promethist Bot")))
                )
                //TODO replace by object repository
                bindTo(MongoDatabase::class.java,
                        KMongo.createClient(ConnectionString(AppConfig.instance["database.url"]))
                                .getDatabase(AppConfig.instance["name"] + "-" + dataspace))
                bindTo(FileStorage::class.java, fileStorage)
                bind(MongoProfileRepository::class.java).to(ProfileRepository::class.java)

                bind(DialogueLog::class.java).to(DialogueLog::class.java).`in`(RequestScoped::class.java)

                // content distribution service (provided by admin)
                bindTo(ContentDistributionResource::class.java, ServiceUrlResolver.getEndpointUrl("admin"))

                bind(KMongoIdParamConverterProvider::class.java).to(ParamConverterProvider::class.java).`in`(Singleton::class.java)
                bindFactory(QueryValueFactory::class.java).to(Query::class.java).`in`(PerLookup::class.java)
            }
        })
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