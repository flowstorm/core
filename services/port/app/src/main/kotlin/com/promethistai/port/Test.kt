package com.promethistai.port

import javax.activation.MimetypesFileTypeMap

object Test {

    @JvmStatic
    fun main(args: Array<String>) {
        val map = MimetypesFileTypeMap()
        println(map.getContentType(".aa.test.mp3"))
    }
}