package ai.flowstorm.core.resources

import ai.flowstorm.common.ServiceUrlResolver
import ai.flowstorm.core.video.VideoService
import ai.flowstorm.core.video.VideoStream
import com.mongodb.client.MongoDatabase
import io.lindstrom.m3u8.model.MediaPlaylist
import io.lindstrom.m3u8.model.MediaSegment
import io.lindstrom.m3u8.parser.MediaPlaylistParser
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.litote.kmongo.updateOneById
import org.litote.kmongo.upsert
import javax.inject.Inject
import javax.ws.rs.Path

@Path("/video")
class VideoResourceImpl : VideoResource {

    @Inject
    lateinit var videoService: VideoService

    @Inject
    lateinit var database: MongoDatabase

    private val videoStreams by lazy { database.getCollection<VideoStream>() }
    private val playlistParser = MediaPlaylistParser()

    override fun mediaPlaylist(deviceId: String): String {
        val segments = videoService.getCurrentSegments(deviceId)
        val stream = videoStreams.find(VideoStream::deviceId eq deviceId).toList().firstOrNull() ?: VideoStream(deviceId = deviceId)
        stream.sequenceCount++
        stream.segmentCount += segments.size
        stream.cacheHits += segments.count { it.cacheHit }

        videoStreams.updateOneById(stream._id, stream, upsert())

        val fileUrl = ServiceUrlResolver.getEndpointUrl("core") + "/file"
        val playlist = MediaPlaylist.builder()
            .version(3)
            .ongoing(true)
            .targetDuration(segments.size)
            .mediaSequence(stream.sequenceCount)
            .addAllMediaSegments(segments.map {
                MediaSegment.builder()
                    .duration(it.duration)
                    .uri("$fileUrl/${it.path}")
                    .build()
            }).build()

        return playlistParser.writePlaylistAsString(playlist)
    }
}