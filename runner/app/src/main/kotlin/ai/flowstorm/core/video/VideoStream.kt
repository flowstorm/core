package ai.flowstorm.core.video

import ai.flowstorm.common.model.TimeEntity
import org.litote.kmongo.Id
import org.litote.kmongo.newId
import java.util.*

data class VideoStream(
    override val _id: Id<VideoStream> = newId(),
    val deviceId: String,
    override var datetime: Date = Date(),
    var sequenceCount: Long = 0,
    var segmentCount: Int = 0,
    var cacheHits: Int = 0
) : TimeEntity<VideoStream>