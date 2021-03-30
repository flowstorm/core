package ai.flowstorm.core.video

import ai.flowstorm.core.Response
import ai.flowstorm.core.storage.FileStorage
import ai.flowstorm.util.LoggerDelegate
import com.mongodb.client.MongoDatabase
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import java.io.ByteArrayInputStream
import javax.inject.Inject

class VideoService {

    @Inject
    lateinit var fileStorage: FileStorage

    @Inject
    lateinit var database: MongoDatabase

    private val videoSegments by lazy { database.getCollection<VideoSegment>() }
    private val logger by LoggerDelegate()

    private fun deleteSegments(deviceId: String) = videoSegments.deleteMany(VideoSegment::deviceId eq deviceId)

    /**
     * @var response
     * @var clear delete any existing segments for specified device
     */
    fun createSequenceSegments(deviceId: String, response: Response, clear: Boolean = false, dryRun: Boolean = false) {
        val segment = VideoSegment(deviceId = deviceId, avatarId = response.avatarId ?: "_default", sequenceHash = response.hash(), duration = 1.0) // first segment in sequence
        try {
            val segments = if (dryRun) {
                logger.info("DRY sequence ${segment.sequencePath}")
                (0 until 9).map { index ->
                    segment.copy(index = index)
                }
            } else {
                fileStorage.getFile(segment.path) // try to get first segment video file
                logger.info("HIT sequence ${segment.sequencePath}")
                mutableListOf<VideoSegment>().apply {
                    while (true) {
                        add(segment.copy())
                        segment.index++
                        try {
                            fileStorage.getFile(segment.path)
                        } catch (e: FileStorage.NotFoundException) {
                            // no more segment video files in sequence
                            logger.debug("Sequence ${segment.sequencePath} has ${segment.index} segment(s)")
                            break
                        }
                    }
                }
            }
            if (clear)
                deleteSegments(deviceId)
            videoSegments.insertMany(segments) // insert segment into database for playlist generation via video resource

        } catch (e: FileStorage.NotFoundException) {
            // first segment video file not found - let's render the whole sequence
            logger.info("MISS sequence ${segment.sequencePath} - going to rendering")
            segment.cacheHit = false
            //TODO prepare rendering - matchmaking of Unreal app instance
            while (true) {
                val buf: ByteArrayInputStream = TODO("create sequence segment from streaming source")
                logger.debug("Writing segment ${segment.path}")
                fileStorage.writeFile(segment.path, "application/x-mpegurl", listOf("avatarId:${segment.avatarId}", "sequenceHash:${segment.sequenceHash}"), buf)
                if (clear && segment.index == 0)
                    deleteSegments(deviceId)
                videoSegments.insertOne(segment)
                segment.index++
            }
        }
    }

    private fun getTransientSegments(id: String, count: Int = 2) =
        (0 until count).map { index ->
            VideoSegment(deviceId = "undefined", avatarId = "_transient", sequenceHash = id.padStart(24, '0'), index = index, duration = 1.0)
        }

    fun getCurrentSegments(deviceId: String, clear: Boolean = true) = with(videoSegments) {
        find(VideoSegment::deviceId eq deviceId).limit(4).toList().onEach { segment ->
            if (clear && !segment.repeatable)
                deleteOne(VideoSegment::_id eq segment._id)
        }.ifEmpty {
            getTransientSegments("0") // default transient sequence
        }
    }
}