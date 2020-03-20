package com.promethist.core

import com.mongodb.ConnectionString
import com.mongodb.client.MongoDatabase
import com.promethist.common.AppConfig
import com.promethist.common.JerseyApplication
import com.promethist.common.ResourceBinder
import com.promethist.common.RestClient
import com.promethist.common.query.Query
import com.promethist.common.query.QueryInjectionResolver
import com.promethist.common.query.QueryParams
import com.promethist.common.query.QueryValueFactory
import com.promethist.core.context.ContextFactory
import com.promethist.core.nlp.IllusionistComponent
import com.promethist.core.nlp.Component
import com.promethist.core.nlp.Pipeline
import com.promethist.core.profile.MongoProfileRepository
import com.promethist.core.profile.ProfileRepository
import com.promethist.core.resources.*
import com.promethist.core.runtime.DialogueManager
import com.promethist.core.runtime.LocalFileLoader
import org.glassfish.hk2.api.InjectionResolver
import org.glassfish.hk2.api.TypeLiteral
import org.glassfish.jersey.process.internal.RequestScoped
import org.litote.kmongo.KMongo
import java.io.File
import javax.inject.Singleton

class Application : JerseyApplication() {

    init {
        register(object : ResourceBinder() {
            override fun configure() {

                // NLP pipeline
                bindTo(Pipeline::class.java)
                bindTo(ContextFactory::class.java)

                //register pipeline adapters - order is important

                val illusionist = IllusionistComponent()
                illusionist.webTarget = RestClient.webTarget(ServiceUtil.getEndpointUrl("illusionist"))
                        .path("/query")
                        .queryParam("key", AppConfig.instance["illusionist.apiKey"])

                bind(illusionist).to(Component::class.java).named("illusionist")
                bind(MongoProfileRepository::class.java).to(ProfileRepository::class.java)

                val dm = DialogueManager(LocalFileLoader(File(AppConfig.instance.get("models.dir"))))
                bindTo(DialogueManager::class.java, dm)

                bindTo(SessionResource::class.java, SessionResourceImpl::class.java)
                bindTo(MongoDatabase::class.java,
                        KMongo.createClient(ConnectionString(AppConfig.instance["database.url"]))
                                .getDatabase(AppConfig.instance["database.name"]))

                // manager
                val dialogueManagerUrl =
                        AppConfig.instance.get("dialoguemanager.url",
                                ServiceUtil.getEndpointUrl("helena",
                                        ServiceUtil.RunMode.valueOf(AppConfig.instance.get("runmode", "dist")))) + "/dm"
                println("dialogueManagerUrl = $dialogueManagerUrl")
                bindTo(BotService::class.java, dialogueManagerUrl)

                bindTo(BotServiceResourceImpl::class.java)

                // admin
                val adminUrl =
                        AppConfig.instance.get("admin.url",
                                ServiceUtil.getEndpointUrl("admin",
                                        ServiceUtil.RunMode.valueOf(AppConfig.instance.get("runmode", "dist"))))
                bindTo(ContentDistributionResource::class.java, adminUrl)


                bindFactory(QueryValueFactory::class.java).to(Query::class.java).`in`(RequestScoped::class.java)

                bind(QueryInjectionResolver::class.java)
                        .to(object: TypeLiteral<InjectionResolver<QueryParams>>() {})
                        .`in`(Singleton::class.java)

            }
        })
    }
}