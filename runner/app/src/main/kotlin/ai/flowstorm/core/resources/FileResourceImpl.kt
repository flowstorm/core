package ai.flowstorm.core.resources

import ai.flowstorm.common.security.Authorized
import ai.flowstorm.core.storage.FileStorage
import ai.flowstorm.util.LoggerDelegate
import java.io.InputStream
import javax.inject.Inject
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.StreamingOutput

@Path("/file")
@Produces(MediaType.APPLICATION_JSON)
class FileResourceImpl: FileResource {

    private val logger by LoggerDelegate()

    @Inject
    lateinit var fileStorage: FileStorage

    override fun readFile(path: String): Response =
        try {
            val file = fileStorage.getFile(path)
            Response.ok(
                StreamingOutput { output ->
                    try {
                        fileStorage.readFile(path, output)
                    } catch (e: Exception) {
                        throw WebApplicationException("File streaming failed", e)
                    }
                }, file.contentType
            )
            .header("Content-Disposition", "inline" + "; filename=\"${file.name}\"")
            .header("Content-Length", file.size)
        } catch (e: FileStorage.NotFoundException) {
            logger.warn("File not found: $path")
            Response.status(404)
        }.build()

    @Authorized
    override fun writeFile(path: String, contentType: String, meta: List<String>, input: InputStream) =
            fileStorage.writeFile(path, contentType, meta, input)

    @Authorized
    override fun deleteFile(path: String) = fileStorage.deleteFile(path)

    override fun getFile(path: String): Response =
        try {
            Response.ok(fileStorage.getFile(path))
        } catch (e: FileStorage.NotFoundException) {
            Response.status(404)
        }.build()

    override fun provider() = fileStorage::class.simpleName!!

    /*
    // This version of the readFile function might be useful in the future when we decide to fix Safari compatibility
    // The range parameter must come from Range request header value
    fun readFileSafari(path: String, range: String): Response {
        val file = getFile(path)
        val rangeBytes = range.split("=")[1]
//        val size = if (rangeBytes === "0-") file.size else 2
        return Response.ok(
                StreamingOutput { output ->
                    try {
                        readFile(path, output)
                    } catch (e: Exception) {
                        throw WebApplicationException("File streaming failed", e)
                    }
                }, file.contentType)
                .header("Content-Disposition", "inline" + "; filename=\"${file.name}\"")
                .header("Content-Length", file.size)
                .header("Accept-Ranges", "bytes")
                .header("Content-Range", "bytes ${rangeBytes}/${file.size}")
                .status(206)
                .build()
    }*/
}