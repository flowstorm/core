package com.promethist.core

import com.commit451.mailgun.Contact
import com.commit451.mailgun.Mailgun
import com.mongodb.ConnectionString
import com.mongodb.client.MongoDatabase
import com.promethist.common.*
import com.promethist.common.mongo.KMongoIdParamConverterProvider
import com.promethist.common.query.*
import com.promethist.services.MessageSender
import com.promethist.common.services.MailgunSender
import com.promethist.core.context.ContextFactory
import com.promethist.core.context.ContextPersister
import com.promethist.core.model.Session
import com.promethist.core.profile.MongoProfileRepository
import com.promethist.core.profile.ProfileRepository
import com.promethist.core.resources.*
import com.promethist.core.runtime.*
import io.sentry.Sentry
import io.sentry.SentryEvent
import org.glassfish.hk2.api.InjectionResolver
import org.glassfish.hk2.api.PerLookup
import org.glassfish.hk2.api.TypeLiteral
import org.glassfish.jersey.process.internal.RequestScoped
import org.litote.kmongo.KMongo
import javax.inject.Singleton
import javax.ws.rs.ext.ParamConverterProvider

class Application : JerseyApplication() {

    init {
        Sentry.init()
        register(object : ResourceBinder() {
            override fun configure() {

                val runMode = ServiceUrlResolver.RunMode.valueOf(AppConfig.instance.get("runmode", "dist"))
                AppConfig.instance.let {
                    println("Promethist ${it["git.ref"]} core starting (namespace = ${it["namespace"]}, runMode = $runMode)")
                }

                // filestore
                val filestoreUrl = ServiceUrlResolver.getEndpointUrl("filestore")
                val filestore = RestClient.instance(FileResource::class.java, filestoreUrl)
                bind(filestore).to(FileResource::class.java)

                bindTo(PipelineFactory::class.java)
                bindTo(ContextFactory::class.java)
                bindTo(ContextPersister::class.java)

                // NLP pipeline (last bound component will be used first)
                val namespace = AppConfig.instance.get("dsuffix", AppConfig.instance["namespace"])

                // IR component
                val illusionistUrl = ServiceUrlResolver.getEndpointUrl("illusionist", namespace)
                val illusionist = Illusionist()
                illusionist.webTarget = RestClient.webTarget(illusionistUrl)
                        .path("/query")
                        .queryParam("key", AppConfig.instance["illusionist.apiKey"])
                bind(illusionist).to(Component::class.java).named("illusionist")

                bind(Action::class.java).to(Component::class.java).named("actionResolver")

                // DM component (third - dialog user input decides whether to process the rest of pipeline or not)
                val dialogueFactory = DialogueFactory(FileResourceLoader(filestore, "dialogue",
                        AppConfig.instance.get("loader.noCache", "false") == "true",
                        AppConfig.instance.get("loader.useScript", "false") == "true"))
                bind(dialogueFactory).to(DialogueFactory::class.java)
                bind(DialogueManager::class.java).to(Component::class.java).named("dm")

                // Duckling (time values)
                val ducklingUrl = ServiceUrlResolver.getEndpointUrl("duckling", namespace)
                val duckling = Duckling()
                duckling.webTarget = RestClient.webTarget(ducklingUrl)
                        .path("/parse")

                bind(duckling).to(Component::class.java).named("duckling")

                // NER component (second)
                val cassandraUrl = ServiceUrlResolver.getEndpointUrl("cassandra", namespace)
                val cassandra = Cassandra()
                cassandra.webTarget = RestClient.webTarget(cassandraUrl)
                        .path("/query")

                bind(cassandra).to(Component::class.java).named("cassandra")


                println("illusionistUrl = $illusionistUrl")
                println("cassandraUrl = $cassandraUrl")
                println("ducklingUrl = $ducklingUrl")

                val mailgun = Mailgun.Builder(AppConfig.instance["mailgun.domain"], AppConfig.instance["mailgun.apikey"])
                        .baseUrl(AppConfig.instance["mailgun.baseUrl"])
                        .build()
                bindTo(MessageSender::class.java, MailgunSender(mailgun, Contact(AppConfig.instance["emailsender.from.email"], AppConfig.instance["emailsender.from.name"])))

                // tokenizer (first)
                bind(InternalTokenizer()).to(Component::class.java).named("tokenizer")

                bind(MongoProfileRepository::class.java).to(ProfileRepository::class.java)
                bindTo(ReportResource::class.java, ReportResourceImpl::class.java)
                bindTo(SessionResource::class.java, SessionResourceImpl::class.java)
                bindTo(DialogueEventResource::class.java, DialogueEventResourceImpl::class.java)
                bindTo(ProfileResource::class.java, ProfileResourceImpl::class.java)
                bindTo(CommunityResource::class.java, CommunityResourceImpl::class.java)
                bindTo(DevicePairingResource::class.java, DevicePairingResourceImpl::class.java)
                bindTo(MongoDatabase::class.java,
                        KMongo.createClient(ConnectionString(AppConfig.instance["database.url"]))
                                .getDatabase(AppConfig.instance["name"] + "-" + AppConfig.instance.get("dsuffix", AppConfig.instance["namespace"])))

                bindTo(CoreResourceImpl::class.java)

                bind(DialogueLog::class.java).to(DialogueLog::class.java).`in`(RequestScoped::class.java)

                // admin
                bindTo(ContentDistributionResource::class.java, ServiceUrlResolver.getEndpointUrl("admin"))

                bind(KMongoIdParamConverterProvider::class.java).to(ParamConverterProvider::class.java).`in`(Singleton::class.java)
                bindFactory(QueryValueFactory::class.java).to(Query::class.java).`in`(PerLookup::class.java)

                bind(QueryInjectionResolver::class.java)
                        .to(object: TypeLiteral<InjectionResolver<QueryParams>>() {})
                        .`in`(Singleton::class.java)
            }
        })
    }

    companion object {
        fun capture(e: Throwable, session: Session? = null) = with(SentryEvent()) {
            throwable = e
            if (session != null)
                setExtras(mapOf(
                        "sessionId" to session.sessionId,
                        "applicationName" to session.application.name,
                        "dialogue_id" to session.application.dialogue_id.toString(),
                        "user_id" to session.user._id.toString()
                ))
            Sentry.captureEvent(this)
        }
    }
}