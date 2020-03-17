package com.promethist.core.runtime

import java.io.File
import java.io.FileInputStream

class LocalFileLoader(private val base: File) : AbstractLoader() {
    override fun getInputStream(name: String) = FileInputStream(File(base, name))
    override fun getLastModified(name: String): Long = File(base, name).lastModified()
    override fun toString(): String = "${javaClass.simpleName}(base=$base)"
}