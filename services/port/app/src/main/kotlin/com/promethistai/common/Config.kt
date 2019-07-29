package com.promethistai.common

import java.io.FileInputStream
import java.io.IOException
import java.io.Serializable
import java.util.*

class Config: Serializable, Cloneable {

    private val properties: Properties = Properties()

    init {
        try {
            properties.load(FileInputStream("config.properties"))
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    operator fun get(key: String): String? {
        return if (this.properties[key] == null) null else this.properties[key] as String
    }

    operator fun set(key: String, value: Any) {
        this.properties[key] = value
    }

    companion object {
        val instance = Config()
    }
}
