package com.promethist.core

import com.mongodb.ConnectionString
import com.mongodb.client.MongoDatabase
import com.promethist.common.*
import com.promethist.common.mongo.KMongoIdParamConverterProvider
import com.promethist.common.query.*
import com.promethist.core.context.ContextFactory
import com.promethist.core.context.ContextPersister
import com.promethist.core.runtime.*
import com.promethist.core.profile.MongoProfileRepository
import com.promethist.core.profile.ProfileRepository
import com.promethist.core.resources.*
import org.glassfish.hk2.api.InjectionResolver
import org.glassfish.hk2.api.PerLookup
import org.glassfish.hk2.api.TypeLiteral
import org.glassfish.jersey.process.internal.RequestScoped
import org.litote.kmongo.KMongo
import javax.inject.Singleton
import javax.ws.rs.ext.ParamConverterProvider

class Application : JerseyApplication() {

    init {
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

                // NLP pipeline (last binded component will be used first)

                // IR component
                val illusionistUrl = ServiceUrlResolver.getEndpointUrl("illusionist")
                val illusionist = Illusionist()
                illusionist.webTarget = RestClient.webTarget(illusionistUrl)
                        .path("/query")
                        .queryParam("key", AppConfig.instance["illusionist.apiKey"])
                bind(illusionist).to(Component::class.java).named("illusionist")

                // DM component (third - dialog user input decides whether to process the rest of pipeline or not)
                val dialogueFactory = DialogueFactory(FileResourceLoader(filestore, "dialogue",
                        AppConfig.instance.get("loader.noCache", "false") == "true",
                        AppConfig.instance.get("loader.useScript", "false") == "true"))
                bind(dialogueFactory).to(DialogueFactory::class.java)
                bind(DialogueManager::class.java).to(Component::class.java).named("dm")

                // NER component (second)
                val cassandraUrl = ServiceUrlResolver.getEndpointUrl("cassandra")
                val cassandra = Cassandra()
                cassandra.webTarget = RestClient.webTarget(cassandraUrl)
                        .path("/query")

                bind(cassandra).to(Component::class.java).named("cassandra")


                // tokenizer (first)
                bind(InternalTokenizer()).to(Component::class.java).named("tokenizer")

                bind(MongoProfileRepository::class.java).to(ProfileRepository::class.java)
                bindTo(ReportResource::class.java, ReportResourceImpl::class.java)
                bindTo(SessionResource::class.java, SessionResourceImpl::class.java)
                bindTo(ProfileResource::class.java, ProfileResourceImpl::class.java)
                bindTo(CommunityResource::class.java, CommunityResourceImpl::class.java)
                bindTo(DevicePairingResource::class.java, DevicePairingResourceImpl::class.java)
                bindTo(MongoDatabase::class.java,
                        KMongo.createClient(ConnectionString(AppConfig.instance["database.url"]))
                                .getDatabase(AppConfig.instance["name"] + "-" + AppConfig.instance["namespace"]))

                // dialogue manager helena (support of running V1 dialogue models)
                bindTo(BotService::class.java, ServiceUrlResolver.getEndpointUrl("helena") + "/dm")

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
}