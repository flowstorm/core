package com.promethist.client.util

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.*

object NativeUtil {

    private const val MIN_PREFIX_LENGTH = 3
    const val NATIVE_FOLDER_PATH_PREFIX = "nativeutil"

    private var temporaryDir: File? = null

    fun loadLibraryFromJar(path: String) {
        require(path.startsWith("/")) { "The path has to be absolute (start with '/')." }

        // Obtain filename from path
        val parts = path.split("/").toTypedArray()
        val filename = if (parts.size > 1) parts[parts.size - 1] else null

        // Check if the filename is okay
        require(!(filename == null || filename.length < MIN_PREFIX_LENGTH)) { "The filename has to be at least 3 characters long." }

        // Prepare temporary file
        if (temporaryDir == null) {
            temporaryDir = createTempDirectory(NATIVE_FOLDER_PATH_PREFIX)
            temporaryDir!!.deleteOnExit()
        }
        val temp = File(temporaryDir, filename)
        try {
            NativeUtil::class.java.getResourceAsStream(path).use { `is` -> Files.copy(`is`, temp.toPath(), StandardCopyOption.REPLACE_EXISTING) }
        } catch (e: IOException) {
            temp.delete()
            throw e
        } catch (e: NullPointerException) {
            temp.delete()
            throw FileNotFoundException("File $path was not found inside JAR.")
        }
        try {
            System.load(temp.getAbsolutePath())
        } finally {
            if (isPosixCompliant) {
                // Assume POSIX compliant file system, can be deleted after loading
                temp.delete()
            } else {
                // Assume non-POSIX, and don't delete until last file descriptor closed
                temp.deleteOnExit()
            }
        }
    }

    private val isPosixCompliant: Boolean
        private get() = try {
            FileSystems.getDefault()
                    .supportedFileAttributeViews()
                    .contains("posix")
        } catch (e: FileSystemNotFoundException) {
            false
        } catch (e: ProviderNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        }

    private fun createTempDirectory(prefix: String): File {
        val tempDir = System.getProperty("java.io.tmpdir")
        val generatedDir = File(tempDir, prefix + System.nanoTime())
        if (!generatedDir.mkdir()) throw IOException("Failed to create temp directory " + generatedDir.getName())
        return generatedDir
    }
}