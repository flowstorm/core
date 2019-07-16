package com.promethistai.datastore.server

import java.io.FileInputStream
import java.io.IOException
import java.io.Serializable
import java.util.*

class Config: Serializable, Cloneable {

    private val properties: Properties = Properties()

    init {
        try {
            println(System.getProperty("user.dir"))
            properties.load(FileInputStream("config.properties"))
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    operator fun get(key: String): String {
        return this.properties[key] as String
    }

    operator fun set(key: String, value: Object) {
        this.properties[key] = value
    }

    companion object {
        val instance = Config()
    }
}
