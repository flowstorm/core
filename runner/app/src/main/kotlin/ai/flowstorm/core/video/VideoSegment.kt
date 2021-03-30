package ai.flowstorm.core.video

import ai.flowstorm.common.model.TimeEntity
import com.fasterxml.jackson.annotation.JsonIgnore
import org.litote.kmongo.Id
import org.litote.kmongo.newId
import java.util.*

data class VideoSegment(
    override val _id: Id<VideoSegment> = newId(),
    val deviceId: String,
    val avatarId: String,
    val sequenceHash: String,
    val duration: Double,
    var index: Int = 0,
    var cacheHit: Boolean = true,
    val repeatable: Boolean = false,
    override var datetime: Date = Date()
) : TimeEntity<VideoSegment> {

    @get:JsonIgnore
    val sequencePath: String get() = "video/$avatarId/$sequenceHash"

    @get:JsonIgnore
    val path: String get() = "$sequencePath/${index.toString().padStart(3, '0')}.ts"
}
