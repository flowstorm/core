package com.promethist.core.builder

import com.promethist.core.model.Dialogue
import com.promethist.core.resources.FileResource
import com.promethist.core.runtime.Kotlin
import com.promethist.core.runtime.Loader
import java.io.ByteArrayInputStream
import java.io.StringReader
import java.math.BigInteger
import java.security.MessageDigest
import java.time.LocalDateTime

class DialogueModelBuilder(val name: String, initCode: CharSequence = "", parentClass: String = "Dialogue") {

    val source = StringBuilder()
    val className: String
    val version = "undefined"//AppConfig.instance.get("git.ref", "unknown")
    private val md = MessageDigest.getInstance("MD5")

    private fun md5(str: String): String =
            BigInteger(1, md.digest(str.toByteArray())).toString(16).padStart(32, '0')

    init {
        val names = name.split("/").toMutableList()
        className = "Model" + names.removeAt(names.size - 1)
        source
                .appendln("//--dialogue-model;version:$version;name:$name;time:" + LocalDateTime.now())
                .appendln("package " + names.joinToString(".") { "`$it`" }).appendln()
                .appendln("import com.promethist.core.*")
                .appendln("import com.promethist.core.model.*").appendln()
                .append("data class $className(override val resourceLoader: ResourceLoader, override val name: String")

        source.appendln(") : $parentClass(resourceLoader, name) {")
        if (initCode.isNotBlank()) {
            source.appendln("//--code-start;type:init")
            source.appendln(initCode)
            source.appendln("//--code-end;type:init")
        }
    }

    fun addIntent(nodeId: Int, nodeName: String, utterances: List<String>) {
        source.append("\tval $nodeName = Intent($nodeId")
        utterances.forEach {
            source.append(", ").append('"').append(it.trim().replace("\"", "\\\"")).append('"')
        }
        source.appendln(')')
        //TODO add to intent model
    }

    fun addResponse(nodeId: Int, nodeName: String, texts: List<String>, type: String = "Response") {
        source.append("\tval $nodeName = $type($nodeId")
        texts.forEach {
            source.append(", { \"\"\"").append(it).append("\"\"\" }")
        }
        source.appendln(')')
    }

    fun addFunction(nodeId: Int, nodeName: String, transitions: Map<String, String>, code: CharSequence) {
        source.appendln("\tval $nodeName = Function($nodeId) {")
        transitions.forEach { source.appendln("\t\tval ${it.key} = Transition(${it.value})") }
        source.appendln("//--code-start;type:function;name:$nodeName")
        source.appendln(code)
        source.appendln("//--code-end;type:function;name:$nodeName").appendln("\t}")
    }

    fun addSubDialogue(nodeId: Int, nodeName: String, subDialogueName: String, code: CharSequence = "create()") {
        source.appendln("\tval $nodeName = SubDialogue($nodeId, \"$subDialogueName\") {")
        source.appendln("//--code-start;type:subDialogue;name:$nodeName")
        source.appendln(code)
        source.appendln("//--code-end;type:subDialogue;name:$nodeName").appendln("\t}")
    }

    fun finalize(transitions: Map<String, String>, extensionSource: CharSequence? = null) {
        source.appendln()
        source.appendln("\tinit {")
        transitions.forEach { source.appendln("\t\t${it.key}.next = ${it.value}") }
        source.appendln("\t}")
        source.appendln('}')
        if (extensionSource != null)
            source.append(extensionSource)
        source.appendln("$className::class")
    }

    /**
     * Builds dialogue model with intent model and stores dialogue model class with included files using file resource.
     *
     */
    fun build(intentModelBuilder: IntentModelBuilder, loader: Loader, fileResource: FileResource) {
        val dialogue = Kotlin.newObject(Kotlin.loadClass<Dialogue>(StringReader(source.toString())), loader, name)
        dialogue.validate()
        val intentModels = mutableMapOf<String, String>()
        val modelId = md5("${name}")
        intentModels["Global"] = modelId
        intentModelBuilder.build(modelId, dialogue.globalIntents)
        dialogue.userInputs.forEach {
            val modelId = md5("${name}${it.id}")
            intentModels["${it.javaClass.simpleName}:${it.id}"] = modelId
            intentModelBuilder.build(modelId, it.intents.asList())
        }
        source.appendln("//--intent-models:$intentModels")
        val stream = ByteArrayInputStream(source.toString().toByteArray())
        fileResource.writeFile("$name/model.kts", "text/kotlin", listOf("version:$version"), stream)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            var nodeId = 0
            val builder = DialogueModelBuilder("product/dialogue/1", "val i = 1").apply {
                addResponse(--nodeId, "response1", listOf("hello", "hi"))
                addIntent(--nodeId, "intent1", listOf("yes", "ok"))
                addIntent(--nodeId, "intent2", listOf("no", "nope"))
                addFunction(--nodeId, "function1", mapOf("trans1" to "response1"), "println(trans1)\ntrans1")
                addSubDialogue(--nodeId, "subDialogue1", "product/subdialogue/1")
                finalize(mapOf("start" to "response1", "function1" to "stop"))
            }
            println(builder.source)
        }
    }
}