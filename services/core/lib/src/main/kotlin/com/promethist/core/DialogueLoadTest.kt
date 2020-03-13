package com.promethist.core

import com.promethist.core.model.Context
import com.promethist.core.model.Dialogue
import java.io.File
import java.io.FileInputStream

object DialogueLoadTest {

    class FileResourceLoader(private val base: File) : AbstractResourceLoader() {
        override fun getFileStream(name: String) = FileInputStream(File(base, name))
        override fun toString(): String = "${javaClass.simpleName}(base=$base)"
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val resourceLoader = FileResourceLoader(File("./test"))
        // helena will cache models in application scope (instantation will set constructor args from session.application.properties)
        val dialogue = resourceLoader.newObject<Dialogue>("product/some-dialogue/1/model", "ble", 1, false)
        dialogue.validate()
        println(dialogue)
        //println("properties: ${dialogue.properties}")
        println("nodes:")
        dialogue.nodes.forEach { println(it) }
        val func = dialogue.functions.first()
        println("calling $func:")
        println(func.exec(Context(message = "xx")))
        println("sub-dialogue: ${dialogue.subDialogues.first().dialogue}")

        //println(ObjectUtil.defaultMapper.writeValueAsString(dialogue))
    }
}