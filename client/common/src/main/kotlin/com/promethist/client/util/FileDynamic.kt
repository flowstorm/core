package com.promethist.client.util

import com.promethist.common.Reloadable
import com.promethist.core.type.Dynamic
import java.io.File
import java.io.FileInputStream
import java.util.Properties

class FileDynamic(val file: File, dynamic: Dynamic) : Dynamic(dynamic), Reloadable {

    constructor(file: File) : this(file, EMPTY)

    override fun reload() {
        val props = Properties()
        FileInputStream(file).use {
            props.load(it)
        }
        (props as Map<String, String>).forEach {
            if (Regex("true|false").matches(it.value))
                put(it.key, it.value.toBoolean())
            else if (Regex("\\d+").matches(it.value))
                put(it.key, it.value.toInt())
            else if (Regex("[\\d\\.]+").matches(it.value))
                put(it.key, it.value.toDouble())
            else
                put(it.key, it.value)
        }
    }
}