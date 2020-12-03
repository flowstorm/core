package com.promethist.core.context

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.promethist.common.AppConfig
import com.promethist.common.ObjectUtil
import com.promethist.core.Context
import com.promethist.core.model.Entity
import com.promethist.core.monitoring.Monitor
import com.promethist.core.profile.ProfileRepository
import com.promethist.core.resources.SessionResource
import com.promethist.core.runtime.DialogueLog
import com.promethist.core.type.Memorable
import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.impl.client.BasicCredentialsProvider
import org.elasticsearch.action.update.UpdateRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.common.xcontent.XContentType
import org.litote.kmongo.Id
import java.util.*
import javax.inject.Inject

class ContextPersister {

    @Inject
    lateinit var profileRepository: ProfileRepository

    @Inject
    lateinit var sessionResource: SessionResource

    @Inject
    lateinit var dialogueLog: DialogueLog

    @Inject
    lateinit var monitor: Monitor

    fun persist(context: Context) {
        context.turn.log.addAll(dialogueLog.log)
        context.turn.duration = System.currentTimeMillis() - context.turn.datetime.time
        context.session.turns.add(context.turn)
        context.session.datetime = Date()
        context.communities.values.forEach {
            context.communityStorage.update(it)
        }


        try {
            saveToElastic(context.session)
            saveToElastic(context.userProfile)
        } catch (e: Throwable) {
            monitor.capture(e, context.session)
        }
        sessionResource.update(context.session)
        profileRepository.save(context.userProfile)
    }

    private fun saveToElastic(entity: Entity<*>) = elasticClient?.let { client ->
        val index = entity::class.simpleName!!.toLowerCase()
        val id = entity._id.toString()
        UpdateRequest(index, id).apply {
            doc(jsonWriter.writeValueAsBytes(entity), XContentType.JSON)
            docAsUpsert(true)
            val res = client.update(this, RequestOptions.DEFAULT)
            println(res.result)
        }
    }

    interface EntityMixin {
        @JsonIgnore fun get_id(): Id<*>
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    interface MemorableMixin

    companion object {

        private val jsonWriter = ObjectUtil.createMapper()
                .addMixIn(Entity::class.java, EntityMixin::class.java)
                .addMixIn(Memorable::class.java, MemorableMixin::class.java)

        val elasticClient by lazy {
            with (AppConfig.instance) {
                getOrNull("es.host")?.let { host ->
                    RestHighLevelClient(
                        RestClient.builder(
                            HttpHost(host, (getOrNull("es.port") ?: "9243").toInt(), getOrNull("es.scheme") ?: "https")
                        ).apply {
                            getOrNull("es.user")?.let { user ->
                                setHttpClientConfigCallback {
                                    it.setDefaultCredentialsProvider(BasicCredentialsProvider().apply {
                                        setCredentials(AuthScope.ANY,
                                            UsernamePasswordCredentials(user, get("es.password", "")))
                                    })
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}