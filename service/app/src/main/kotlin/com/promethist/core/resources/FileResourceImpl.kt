package com.promethist.core.resources

import com.promethist.core.model.FileObject
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.ws.rs.Path
import javax.ws.rs.core.Response

@Path("/file")
class FileResourceImpl: FileResource {

    @Inject
    lateinit var fileResource: FileResource

    override fun readFile(path: String): Response =
            fileResource.readFile(path)

    override fun readFile(path: String, output: OutputStream) = fileResource.readFile(path, output)

    override fun writeFile(path: String, contentType: String, meta: List<String>, input: InputStream) =
            fileResource.writeFile(path, contentType, meta, input)

    override fun deleteFile(path: String) =
            fileResource.deleteFile(path)

    override fun getFile(path: String): FileObject =
            fileResource.getFile(path)

    override fun provider() =
            fileResource.provider()

}