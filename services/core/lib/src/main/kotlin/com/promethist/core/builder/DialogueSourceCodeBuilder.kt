package com.promethist.core.builder

import com.promethist.core.type.PropertyMap
import com.promethist.util.LoggerDelegate
import org.jetbrains.kotlin.daemon.common.toHexString
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.*

class DialogueSourceCodeBuilder(val name: String, val buildId: String) {

    // builder configuration:
    var parameters: PropertyMap = mapOf()
    var config: PropertyMap = mapOf()
    var initCode: CharSequence = ""
    var extensionCode: CharSequence = ""
    var parentClass: String = "Dialogue"

    var code: String = ""
        get() {
            if (field.isEmpty()) error("Code has not been build yet.")
            return field
        }

    private val source = StringBuilder()
    private val className: String
    private val logger by LoggerDelegate()
    private val names: MutableList<String>

    init {
        if (!name.matches(Regex("([\\w\\-]+)/([\\w\\-]+)/(\\d+)")))
            error("dialogue name $name does not conform to naming convention (product-name/dialogue-name/dialogue-version)")
        names = name.split("/").toMutableList()
        className = "Model" + names.removeAt(names.size - 1)
    }

    fun build() {
        logger.info("building source code for $name")
        logger.info("class $className : $parentClass")

        source.clear()
        writeHeader()
        writeClassSignature()
        writeInit()

        globalIntents.forEach { write(it) }
        intents.forEach { write(it) }
        userInputs.forEach { write(it) }
        responses.forEach { write(it) }
        functions.forEach { write(it) }
        subDialogues.forEach { write(it) }

        writeTransitions()

        source.appendln('}')
        if (extensionCode.isNotBlank()) {
            source.append(extensionCode)
        }
        source.appendln("$className::class")
        logger.info("built source code for $name")

        code = source.toString()
    }

    private val intents = mutableListOf<Intent>()
    private val globalIntents = mutableListOf<GlobalIntent>()
    private val userInputs = mutableListOf<UserInput>()
    private val responses = mutableListOf<Response>()
    private val functions = mutableListOf<Function>()
    private val subDialogues = mutableListOf<SubDialogue>()
    private val transitions = mutableMapOf<String, String>()

    data class Intent(val nodeId: Int, val nodeName: String, val utterances: List<String>)
    data class GlobalIntent(val nodeId: Int, val nodeName: String, val utterances: List<String>)
    data class UserInput(val nodeId: Int, val nodeName: String, val intentNames: List<String>, val skipGlobalIntents: Boolean, val transitions: Map<String, String>, val code: CharSequence = "")
    data class Response(val nodeId: Int, val nodeName: String, val texts: List<String>, val type: String = "Response")
    data class Function(val nodeId: Int, val nodeName: String, val transitions: Map<String, String>, val code: CharSequence)
    data class SubDialogue(val nodeId: Int, val nodeName: String, val subDialogueName: String, val code: CharSequence = "")

    fun addNode(node: UserInput) = userInputs.add(node)
    fun addNode(node: Intent) = intents.add(node)
    fun addNode(node: GlobalIntent) = globalIntents.add(node)
    fun addNode(node: Response) = responses.add(node)
    fun addNode(node: Function) = functions.add(node)
    fun addNode(node: SubDialogue) = subDialogues.add(node)
    fun addTransition(transition: Pair<String, String>) = transitions.put(transition.first, transition.second)

    private fun writeHeader() {
        source
                .appendln("//--dialogue-model;name:$name;time:" + LocalDateTime.now())
                .append("package " + names.joinToString(".") { "`$it`" }).appendln(".`$buildId`")
                .appendln()
                .appendln("import com.promethist.core.*")
                .appendln("import com.promethist.core.type.*")
                .appendln("import com.promethist.core.model.*")
                .appendln("import com.promethist.core.dialogue.*")
                .appendln("import com.promethist.core.runtime.Loader")
                .appendln("import kotlin.random.Random")
                .appendln("import java.time.*")
                .appendln()
    }

    private fun writeInit() {
        source.appendln("\toverride val name = \"$name\"")
        source.appendln("\toverride val buildId = \"$buildId\"")

        config.forEach {
            source.append("override val ${it.key}: ${it.value::class.simpleName} = ")
            if (it.value is String)
                source.append('"').append((it.value as String).trim().replace("\"", "\\\"")).append('"')
            else
                source.append(it.value.toString())
            source.appendln()
        }

        if (initCode.isNotBlank()) {
            source.appendln("//--code-start;type:init")
            source.appendln(initCode.replace(Regex("//\\#include ([^\\s]+)")) {
                val path = it.groupValues[1]
                (if (path.startsWith("http://") || path.startsWith("https://")) {
                    val conn = URL(path).openConnection() as HttpURLConnection
                    if (conn.responseCode != 200)
                        error("include from url $path returned code ${conn.responseCode}")
                    else
                        conn.inputStream
                } else {
                    FileInputStream(path)
                }).use { input ->
                    val buf = ByteArrayOutputStream()
                    input.copyTo(buf)
                    "//--include-start;url:$path\n" + String(buf.toByteArray(), StandardCharsets.UTF_8) + "\n//--include-end\n"
                }
            })
            source.appendln("//--code-end;type:init")
        }
    }

    private fun writeClassSignature() {
        source.append("class $className(")

        var comma = ""
        parameters.forEach {
            if (it.value !is Int && it.value !is Long && it.value !is Float && it.value !is Double && it.value !is String && it.value !is Boolean)
                error("arg ${it.key} is if on unsupported type ${it.value::class.simpleName} (only Int, Long, Float, Double, String, Boolean supported)")
            source.append("${comma}val ${it.key}: ${it.value::class.simpleName} = ")
            if (it.value is String)
                source.append('"').append((it.value as String).trim().replace("\"", "\\\"")).append('"')
            else
                source.append(it.value.toString())
            comma = ", "
        }
        source.appendln(") : $parentClass() {")
    }

    private fun write(intent: Intent) {
        val (nodeId, nodeName, utterances) = intent
        source.append("\tval $nodeName = Intent($nodeId, \"$nodeName\"")
        utterances.forEach {
            source.append(", ").append('"').append(it.trim().replace("\"", "\\\"")).append('"')
        }
        source.appendln(')')
    }

    private fun write(intent: GlobalIntent) {
        val (nodeId, nodeName, utterances) = intent
        source.append("\tval $nodeName = GlobalIntent($nodeId, \"$nodeName\"")
        utterances.forEach {
            source.append(", ").append('"').append(it.trim().replace("\"", "\\\"")).append('"')
        }
        source.appendln(')')
    }

    private fun write(userInput: UserInput) {
        val (nodeId, nodeName, intentNames, skipGlobalIntents, transitions, code) = userInput
        source.append("\tval $nodeName = UserInput($nodeId, $skipGlobalIntents, arrayOf(")
        for (i in intentNames.indices) {
            if (i > 0)
                source.append(", ")
            source.append(intentNames[i])
        }
        source.appendln(")) {")
        transitions.forEach { source.appendln("\t\tval ${it.key} = Transition(${it.value})") }
        source.appendln("//--code-start;type:userInput;name:$nodeName")
        if (code.isNotEmpty()) {
            source.appendln(code)
        } else {
            source.appendln("pass")
        }
        source.appendln("//--code-end;type:userInput;name:$nodeName").appendln("\t}")
    }

    private fun write(response: Response) {
        val (nodeId, nodeName, texts, type) = response
        source.append("\tval $nodeName = $type($nodeId")
        texts.forEach {
            source.append(", { \"\"\"").append(it).append("\"\"\" }")
        }
        source.appendln(')')
    }

    private fun write(function: Function) {
        val (nodeId, nodeName, transitions, code) = function
        source.appendln("\tval $nodeName = Function($nodeId) {")
        transitions.forEach { source.appendln("\t\tval ${it.key} = Transition(${it.value})") }
        source.appendln("//--code-start;type:function;name:$nodeName")
        source.appendln(code)
        source.appendln("//--code-end;type:function;name:$nodeName").appendln("\t}")
    }

    private fun write(subDialogue: SubDialogue) {
        val (nodeId, nodeName, subDialogueName, code) = subDialogue
        source.appendln("\tval $nodeName = SubDialogue($nodeId, \"$subDialogueName\") {")
        source.appendln("//--code-start;type:subDialogue;name:$nodeName")
        source.appendln(if (code.isNotEmpty()) code else "it.create()")
        source.appendln("//--code-end;type:subDialogue;name:$nodeName").appendln("\t}")
    }

    private fun writeTransitions() {
        source.appendln()
        source.appendln("\tinit {")
        transitions.forEach { source.appendln("\t\t${it.key}.next = ${it.value}") }
        source.appendln("\t}")
    }

    companion object {
        private val random = Random()
        private val md = MessageDigest.getInstance("MD5")

        fun md5(str: String): String = md.digest(str.toByteArray()).toHexString()
    }
}