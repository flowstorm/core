package com.promethist.core.builder

import com.promethist.common.RestClient
import com.promethist.core.nlp.Dialogue
import com.promethist.core.resources.FileResource
import com.promethist.core.runtime.FileResourceLoader
import com.promethist.core.runtime.Kotlin
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.StringReader
import java.math.BigInteger
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.*

class DialogueModelBuilder(val name: String, private val language: Locale, private val args: Map<String, Any>, initCode: CharSequence = "", parentClass: String = "Dialogue") {

    val source = StringBuilder()
    private val className: String
    private val version = "undefined"//AppConfig.instance.get("git.ref", "unknown")
    private val md = MessageDigest.getInstance("MD5")
    private var logger = LoggerFactory.getLogger(this::class.qualifiedName)

    private fun md5(str: String): String =
            BigInteger(1, md.digest(str.toByteArray())).toString(16).padStart(32, '0')

    init {
        logger.info("initializing builder of dialogue model $name : $parentClass")
        if (!name.matches(Regex("([\\w\\-]+)/([\\w\\-]+)/(\\d+)")))
            error("dialogue name $name does not conform to naming convention (product-name/dialogue-name/dialogue-version)")
        val names = name.split("/").toMutableList()
        className = "Model" + names.removeAt(names.size - 1)
        source
                .appendln("//--dialogue-model;version:$version;name:$name;time:" + LocalDateTime.now())
                .appendln("package " + names.joinToString(".") { "`$it`" }).appendln()
                .appendln("import com.promethist.core.runtime.Loader")
                .appendln("import com.promethist.core.nlp.*")
                .appendln("import com.promethist.core.model.*").appendln()
                .append("data class $className(override val loader: Loader, override val name: String")
        args.forEach {
            if (it.value !is Int && it.value !is String && it.value !is Boolean)
                error("arg ${it.key} is if on unsupported type ${it.value::class.simpleName} (only Int, String, Boolean supported)")
            source.append(", val ${it.key}: ${it.value::class.simpleName} = ")
            if (it.value is String)
                source.append('"').append((it.value as String).trim().replace("\"", "\\\"")).append('"')
            else
                source.append(it.value.toString())
        }
        source.appendln(") : $parentClass(loader, name) {")
        source.appendln("\tval language = \"$language\"")
        if (initCode.isNotBlank()) {
            source.appendln("//--code-start;type:init")
            source.appendln(initCode)
            source.appendln("//--code-end;type:init")
        }
    }

    fun addIntent(nodeId: Int, nodeName: String, utterances: List<String>) {
        source.append("\tval $nodeName = Intent($nodeId, \"$nodeName\"")
        utterances.forEach {
            source.append(", ").append('"').append(it.trim().replace("\"", "\\\"")).append('"')
        }
        source.appendln(')')
    }

    fun addUserInput(nodeId: Int, nodeName: String, intentNames: List<String>, skipGlobalIntents: Boolean) {
        source.append("\tval $nodeName = UserInput($nodeId, $skipGlobalIntents")
        intentNames.forEach {
            source.append(", $it")
        }
        source.appendln(')')
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

    fun addSubDialogue(nodeId: Int, nodeName: String, subDialogueName: String, code: CharSequence = "it.create()") {
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
    fun build(intentModelBuilder: IntentModelBuilder, fileResource: FileResource) {
        logger.info("building dialogue model $name")
        val loader = FileResourceLoader(fileResource, "dialogue")
        val dialogue = Kotlin.newObject(Kotlin.loadClass<Dialogue>(StringReader(source.toString())), loader, name, *args.values.toTypedArray())

        logger.info("validating dialogue model $name - $dialogue")
        dialogue.validate()

        logger.info("building intent models for dialogue model $name")
        val intentModels = mutableMapOf<String, String>()
        val modelId = md5(name)
        dialogue.globalIntents.ifNotEmpty {
            intentModels[name] = modelId
            intentModelBuilder.build(modelId, name, language, this)
        }
        dialogue.userInputs.forEach {
            val name = "${name}#${it.id}"
            val modelId = md5(name)
            intentModels[name] = modelId
            intentModelBuilder.build(modelId, name, language, it.intents.asList())
        }
        logger.info("builded intent models: $intentModels")
        source.appendln("//--intent-models:$intentModels")

        val path = "dialogue/$name/model.kts"
        logger.info("writing dialogue model to file resource $path")
        val stream = ByteArrayInputStream(source.toString().toByteArray())
        fileResource.writeFile(path, "text/kotlin", listOf("version:$version"), stream)

        logger.info("dialogue model $name built successfully")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            var nodeId = 0
            val builder = DialogueModelBuilder("product/dialogue/1", Locale("en"), mapOf(
                    "str" to "bla",
                    "num" to 123,
                    "chk" to true
            ),"val i = 1").apply {
                addResponse(--nodeId, "response1", listOf("hello", "hi"))
                addIntent(--nodeId, "intent1", listOf("no", "nope"))
                addIntent(--nodeId, "intent2", listOf("yes", "ok"))
                addFunction(--nodeId, "function1", mapOf("trans1" to "response1"), "println(trans1)\ntrans1")
                //addSubDialogue(--nodeId, "subDialogue1", "product/subdialogue/1")

                // user inputs always at the end (all intents must be defined before)
                addUserInput(--nodeId, "input1", listOf("intent1", "intent2"), false)
                finalize(mapOf(
                        "start" to "response1",
                        "response1" to "input1",
                        "intent1" to "function1",
                        "intent2" to "stop"
                        //"subDialogue1" to "response1",
                ))
            }
            println(builder.source)

            //val fileResource = LocalFileStorage(File("test"))
            val fileResource = RestClient.instance(FileResource::class.java, "https://filestore.develop.promethist.com")
            val intentModelBuilder = IllusionistModelBuilder("https://illusionist.develop.promethist.com", "AIzaSyDgHsjHyK4cS11nEUJuRGeVUEDITi6OtZA")
            builder.build(intentModelBuilder, fileResource)
        }
    }
}