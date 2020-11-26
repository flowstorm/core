package com.promethist.core.resources

import com.promethist.core.FileStorage
import java.io.InputStream
import javax.inject.Inject
import javax.ws.rs.Path
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response
import javax.ws.rs.core.StreamingOutput

@Path("/file")
class FileResourceImpl: FileResource {

    @Inject
    lateinit var fileStorage: FileStorage

    override fun readFile(path: String): Response {
        val file = getFile(path)
        return Response.ok(
                StreamingOutput { output ->
                    try {
                        fileStorage.readFile(path, output)
                    } catch (e: Exception) {
                        throw WebApplicationException("File streaming failed", e)
                    }
                }, file.contentType)
                .header("Content-Disposition", "inline" + "; filename=\"${file.name}\"")
                .header("Content-Length", file.size)
                .build()
    }

    override fun writeFile(path: String, contentType: String, meta: List<String>, input: InputStream) =
            fileStorage.writeFile(path, contentType, meta, input)

    override fun deleteFile(path: String) = fileStorage.deleteFile(path)

    override fun getFile(path: String) = fileStorage.getFile(path)

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