package org.promethist.client.standalone

import okhttp3.OkHttpClient
import okhttp3.Request
import org.promethist.util.LoggerDelegate
import java.io.File
import java.io.FileOutputStream
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class JarUpdater(distUrl: String, val jarFile: File, val sleepTime: Long = 15, val doUpdate: Boolean = true) : Runnable {

    val jarFilename = jarFile.path.substring(jarFile.path.lastIndexOf(File.separatorChar))
    val jarFileUrl = distUrl + jarFilename
    val headRequest = Request.Builder().url(jarFileUrl).head().build()
    val getRequest = Request.Builder().url(jarFileUrl).build()
    var allowed = true
    val client = OkHttpClient()
    private val logger by LoggerDelegate()

    override fun run() {
        while (true) {
            if (allowed) {
                try {
                    val response = client.newCall(headRequest).execute()
                    val lastModifiedHeader = response.header("Last-Modified")
                    if (lastModifiedHeader != null) {
                        val remoteLastModifiedDateTime =
                                ZonedDateTime.parse(lastModifiedHeader, DateTimeFormatter.RFC_1123_DATE_TIME)
                        val remoteLastModified = remoteLastModifiedDateTime.toInstant().toEpochMilli()
                        val localLastModified = jarFile.lastModified()
                        if (remoteLastModified > localLastModified) {
                            if (doUpdate) {
                                val jarDestFile = File("${jarFile.path}.update")
                                logger.info("auto update download from $jarFileUrl to $jarDestFile")
                                val response = client.newCall(getRequest).execute()
                                response.body?.byteStream()?.use { input ->
                                    FileOutputStream(jarDestFile).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                println("{AutoUpdate: $jarDestFile}")
                                System.exit(1)
                            } else {
                                println("{AutoUpdate: newer version of application available. Please download manualy or re-run with auto update enabled.}")
                                break
                            }
                        }
                    }
                    logger.debug("auto update check $jarFileUrl (lastModified = $lastModifiedHeader)")
                } catch (t: Throwable) {
                    logger.warn("auto update failed", t)
                }
            }
            Thread.sleep(1000 * sleepTime)
        }
    }
}