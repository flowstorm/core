package com.promethist.core

import com.promethist.core.model.FileObject
import java.io.InputStream
import java.io.OutputStream

interface FileStorage {

    fun readFile(path: String, output: OutputStream)
    fun getFile(path: String): FileObject
    fun writeFile(path: String, contentType: String, meta: List<String>, input: InputStream)
    fun deleteFile(path: String): Boolean
}