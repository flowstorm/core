package ai.flowstorm.core.runtime

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.elasticsearch.action.update.UpdateRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.common.xcontent.XContentType
import org.litote.kmongo.Id
import ai.flowstorm.common.ObjectUtil
import ai.flowstorm.common.monitoring.Monitor
import ai.flowstorm.core.Context
import ai.flowstorm.common.model.Entity
import ai.flowstorm.core.model.Session
import ai.flowstorm.core.model.Turn
import ai.flowstorm.core.monitoring.capture
import ai.flowstorm.core.repository.ProfileRepository
import ai.flowstorm.core.resources.SessionResource
import ai.flowstorm.core.type.Memorable
import org.jvnet.hk2.annotations.Optional
import java.util.*
import javax.inject.Inject

class ContextPersister {

    @Inject
    lateinit var profileRepository: ProfileRepository

    @Inject
    lateinit var sessionResource: SessionResource

    @Inject
    lateinit var log: ContextLog

    @Inject
    lateinit var monitor: Monitor

    @Inject
    @Optional
    lateinit var elasticClient: RestHighLevelClient

    fun persist(context: Context) = with(context) {
        turn.log.addAll(this@ContextPersister.log.log)
        turn.duration = System.currentTimeMillis() - turn.datetime.time
        session.datetime = Date()
        try {
            sessionResource.create(turn)
            sessionResource.update(session)
            profileRepository.update(userProfile, true)
            communities.values.forEach {
                communityRepository.update(it)
            }
        } catch (e: Throwable) {
            monitor.capture(e, session)
            throw e
        }
        try {
            saveToElastic(turn)
            saveToElastic(session)
            saveToElastic(userProfile)
        } catch (e: Throwable) {
            monitor.capture(e, session)
        }
    }

    private fun saveToElastic(entity: Entity<*>) {
        if (!::elasticClient.isInitialized) return

        val index = entity::class.simpleName!!.toLowerCase()
        val id = entity._id.toString()
        UpdateRequest(index, id).apply {
            doc(jsonWriter.writeValueAsBytes(entity), XContentType.JSON)
            docAsUpsert(true)
            val res = elasticClient.update(this, RequestOptions.DEFAULT)
        }
    }

    interface EntityMixin {
        @JsonIgnore
        fun get_id(): Id<*>
    }

    interface SessionMixin {
        @JsonIgnore
        fun getTurns(): MutableList<Turn> = mutableListOf()
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    interface MemorableMixin

    companion object {

        private val jsonWriter = ObjectUtil.createMapper()
                .addMixIn(Entity::class.java, EntityMixin::class.java)
                .addMixIn(Session::class.java, SessionMixin::class.java)
                .addMixIn(Memorable::class.java, MemorableMixin::class.java)
    }
}