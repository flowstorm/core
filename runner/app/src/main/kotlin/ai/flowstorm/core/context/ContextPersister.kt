package ai.flowstorm.core.context

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonTypeInfo
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
import ai.flowstorm.common.AppConfig
import ai.flowstorm.common.ObjectUtil
import ai.flowstorm.common.monitoring.Monitor
import ai.flowstorm.core.Context
import ai.flowstorm.common.model.Entity
import ai.flowstorm.core.model.Session
import ai.flowstorm.core.model.Turn
import ai.flowstorm.core.monitoring.capture
import ai.flowstorm.core.repository.ProfileRepository
import ai.flowstorm.core.resources.SessionResource
import ai.flowstorm.core.runtime.DialogueLog
import ai.flowstorm.core.type.Memorable
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

    fun persist(context: Context) = with (context) {
        turn.log.addAll(dialogueLog.log)
        turn.duration = System.currentTimeMillis() - turn.datetime.time
        session.datetime = Date()
        try {
            sessionResource.create(turn)
            sessionResource.update(session)
            profileRepository.update(userProfile, true)
            communities.values.forEach {
                communityRepository.update(it)
            }
            saveToElastic(turn)
            saveToElastic(session)
            saveToElastic(userProfile)
        } catch (e: Throwable) {
            monitor.capture(e, session)
            throw e
        }
    }

    private fun saveToElastic(entity: Entity<*>) = elasticClient?.let { client ->
        val index = entity::class.simpleName!!.toLowerCase()
        val id = entity._id.toString()
        UpdateRequest(index, id).apply {
            doc(jsonWriter.writeValueAsBytes(entity), XContentType.JSON)
            docAsUpsert(true)
            val res = client.update(this, RequestOptions.DEFAULT)
        }
    }

    interface EntityMixin {
        @JsonIgnore fun get_id(): Id<*>
    }

    interface SessionMixin {
        @JsonIgnore fun getTurns(): MutableList<Turn> = mutableListOf()
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    interface MemorableMixin

    companion object {

        private val jsonWriter = ObjectUtil.createMapper()
                .addMixIn(Entity::class.java, EntityMixin::class.java)
                .addMixIn(Session::class.java, SessionMixin::class.java)
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