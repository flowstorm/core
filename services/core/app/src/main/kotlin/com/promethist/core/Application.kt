package com.promethist.core

import com.mongodb.ConnectionString
import com.mongodb.client.MongoDatabase
import com.promethist.common.AppConfig
import com.promethist.common.JerseyApplication
import com.promethist.common.ResourceBinder
import com.promethist.common.query.Query
import com.promethist.common.query.QueryInjectionResolver
import com.promethist.common.query.QueryParams
import com.promethist.common.query.QueryValueFactory
import com.promethist.core.context.ContextFactory
import com.promethist.core.nlp.intentRecognition.IllusionistNlpAdapter
import com.promethist.core.nlp.NlpAdapter
import com.promethist.core.nlp.NlpPipeline
import com.promethist.core.nlp.dialogueManager.DialogueManagerNlpAdapter
import com.promethist.core.profile.MongoProfileRepository
import com.promethist.core.profile.ProfileRepository
import com.promethist.core.resources.*
import com.promethist.core.runtime.DialogueManager
import org.glassfish.hk2.api.InjectionResolver
import org.glassfish.hk2.api.TypeLiteral
import org.glassfish.jersey.process.internal.RequestScoped
import org.litote.kmongo.KMongo
import javax.inject.Singleton

class Application : JerseyApplication() {

    init {
        register(object : ResourceBinder() {
            override fun configure() {

                // NLP pipeline
                bindTo(NlpPipeline::class.java)
                bindTo(ContextFactory::class.java)

                //register pipeline adapters - order is important
                bind(IllusionistNlpAdapter::class.java).to(NlpAdapter::class.java).named("illusionist")
                bind(DialogueManagerNlpAdapter::class.java).to(NlpAdapter::class.java).named("dm")
                bind(MongoProfileRepository::class.java).to(ProfileRepository::class.java)



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