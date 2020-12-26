package org.promethist.core.builder

import org.promethist.core.model.DialogueSourceCode
import org.promethist.core.model.TtsConfig
import org.promethist.core.model.Voice
import org.promethist.core.type.PropertyMap
import org.promethist.util.Digest
import org.promethist.util.LoggerDelegate
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime

class DialogueSourceCodeBuilder(val dialogueId: String, val name: String, val version: Int) {

    var properties: PropertyMap = mapOf()
    var initCode: CharSequence = ""
    var parameters: PropertyMap = mapOf()
    var extensionCode: CharSequence = ""
    var parentClass: String? = null
    private val buildId = "id" + Digest.md5()
    private val className: String
    private val source = StringBuilder()
    private val logger by LoggerDelegate()
    private val names: MutableList<String>

    init {
        if (!name.matches(Regex("([\\w\\-]+)/([\\w\\-]+)")))
            error("dialogue name $name does not conform to naming convention (product-name/dialogue-name)")
        names = name.split("/").toMutableList()
        className = "Model"// + names.removeAt(names.size - 1)
    }

    fun build(): DialogueSourceCode {
        if (parentClass == null) {
            val voice = if (properties.containsKey("voice")) properties["voice"] as Voice else Voice.Grace
            val config = TtsConfig.forVoice(voice)
            parentClass = when (config.locale.language) {
                "en" -> "BasicEnglishDialogue"
                "cs" -> "BasicCzechDialogue"
                else -> "BasicDialogue"
            }
        }

        logger.info("Building source code for dialogue $name class $className : $parentClass")

        source.clear()
        writeHeader()
        writeClassSignature()
        writeInit()

        goBacks.forEach { write(it) }
        sleeps.forEach { write(it) }
        globalIntents.forEach { write(it) }
        intents.forEach { write(it) }
        actions.forEach { write(it) }
        globalActions.forEach { write(it) }
        userInputs.forEach { write(it) }
        speeches.forEach { write(it) }
        images.forEach { write(it) }
        sounds.forEach { write(it) }
        commands.forEach { write(it) }
        functions.forEach { write(it) }
        subDialogues.forEach { write(it) }

        writeTransitions()

        source.appendLine('}')
        if (extensionCode.isNotBlank()) {
            source.append(extensionCode)
        }
        logger.info("Built source code for dialogue $name")

        return DialogueSourceCode(dialogueId, name, className, version, buildId, parameters, source.toString())
    }

    private val intents = mutableListOf<DialogueSourceCode.Intent>()
    private val globalIntents = mutableListOf<DialogueSourceCode.GlobalIntent>()
    private val userInputs = mutableListOf<DialogueSourceCode.UserInput>()
    private val speeches = mutableListOf<DialogueSourceCode.Speech>()
    private val sounds = mutableListOf<DialogueSourceCode.Sound>()
    private val images = mutableListOf<DialogueSourceCode.Image>()
    private val commands = mutableListOf<DialogueSourceCode.Command>()
    private val functions = mutableListOf<DialogueSourceCode.Function>()
    private val subDialogues = mutableListOf<DialogueSourceCode.SubDialogue>()
    private val goBacks = mutableListOf<DialogueSourceCode.GoBack>()
    private val sleeps = mutableListOf<DialogueSourceCode.Sleep>()
    private val actions = mutableListOf<DialogueSourceCode.Action>()
    private val globalActions = mutableListOf<DialogueSourceCode.GlobalAction>()
    private val transitions = mutableMapOf<String, String>()

    fun addNode(node: DialogueSourceCode.UserInput) = userInputs.add(node)
    fun addNode(node: DialogueSourceCode.Intent) = intents.add(node)
    fun addNode(node: DialogueSourceCode.GlobalIntent) = globalIntents.add(node)
    fun addNode(node: DialogueSourceCode.Speech) = speeches.add(node)
    fun addNode(node: DialogueSourceCode.Image) = images.add(node)
    fun addNode(node: DialogueSourceCode.Sound) = sounds.add(node)
    fun addNode(node: DialogueSourceCode.Command) = commands.add(node)
    fun addNode(node: DialogueSourceCode.Function) = functions.add(node)
    fun addNode(node: DialogueSourceCode.SubDialogue) = subDialogues.add(node)
    fun addNode(node: DialogueSourceCode.GoBack) = goBacks.add(node)
    fun addNode(node: DialogueSourceCode.Sleep) = sleeps.add(node)
    fun addNode(node: DialogueSourceCode.Action) = actions.add(node)
    fun addNode(node: DialogueSourceCode.GlobalAction) = globalActions.add(node)
    fun addTransition(transition: Pair<String, String>) = transitions.put(transition.first, transition.second)

    private fun writeHeader() {
        source
            .appendLine("//--dialogue-model;name:$name;time:" + LocalDateTime.now())
            .appendLine("package model.$buildId")
            .appendLine()
            .appendLine("import org.promethist.core.*")
            .appendLine("import org.promethist.core.type.*")
            .appendLine("import org.promethist.core.type.value.*")
            .appendLine("import org.promethist.core.model.*")
            .appendLine("import org.promethist.core.dialogue.*")
            .appendLine("import org.promethist.core.runtime.*")
            .appendLine()
    }

    private fun writeInit() {
        source.appendLine("\toverride val dialogueId = \"$dialogueId\"")
        source.appendLine("\toverride val buildId = \"$buildId\"")
        source.appendLine("\toverride val dialogueName = \"$name\"")
        source.appendLine("\toverride val version = $version")

        properties.forEach {
            val className = it.value::class.simpleName
            source.append("override val ${it.key}: $className = ")
            when (it.value) {
                is String -> {
                    source.append('"').append((it.value as String).trim().replace("\"", "\\\"")).append('"')
                }
                is Enum<*> -> with (it.value as Enum<*>) {
                    source.append(className).append('.').append(name)
                }
                else -> source.append(it.value.toString())
            }
            source.appendLine()
        }

        if (initCode.isNotBlank()) {
            source.appendLine("//--code-start;type:init")
            source.appendLine(initCode.replace(Regex("//\\#include ([^\\s]+)")) {
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
            source.appendLine("//--code-end;type:init")
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
        source.appendLine(") : $parentClass() {")
    }

    private fun write(goBack: DialogueSourceCode.GoBack) = with(goBack) {
        source.appendLine("\tval $nodeName = GoBack($nodeId, $repeat)")
    }

    private fun write(sleep: DialogueSourceCode.Sleep) = with(sleep) {
        source.appendLine("\tval $nodeName = Sleep($nodeId, $timeout)")
    }

    private fun write(intent: DialogueSourceCode.Intent) = with(intent) {
        source.append("\tval $nodeName = Intent($nodeId, \"$nodeName\", ${threshold}F, listOf(${entities.joinToString(", ") { "\"" + it + "\"" }})")
        utterances.forEach {
            source.append(", ").append('"').append(it.trim().replace("\"", "\\\"")).append('"')
        }
        source.appendLine(')')
    }

    private fun write(intent: DialogueSourceCode.GlobalIntent) = with(intent) {
        source.append("\tval $nodeName = GlobalIntent($nodeId, \"$nodeName\", ${threshold}F, listOf(${entities.joinToString(", ") { "\"" + it + "\"" }})")
        utterances.forEach {
            source.append(", ").append('"').append(it.trim().replace("\"", "\\\"")).append('"')
        }
        source.appendLine(')')
    }

    private fun write(userInput: DialogueSourceCode.UserInput) = with(userInput) {
        val intents  = intentNames.joinToString(", ")
        val actions = actionNames.joinToString(", ")
        val expectedPhrases = this.expectedPhrases.toString().let { s ->
            if (s.isBlank())
                ""
            else
                s.replace('\n', ',')
                    .split(',').joinToString(", ") { """ExpectedPhrase("${it.trim()}")""" }
        }
        source.append("\tval $nodeName = UserInput($nodeId, $skipGlobalIntents, "
                + (if (userInput.sttMode == null) "null" else "SttConfig.Mode.${userInput.sttMode}")
                + ", listOf($expectedPhrases), arrayOf($intents), arrayOf($actions) ) {")
        transitions.forEach { source.appendLine("\t\tval ${it.key} = Transition(${it.value})") }
        source.appendLine("//--code-start;type:userInput;name:$nodeName")
        if (code.isNotEmpty()) {
            source.appendLine(code)
        } else {
            source.appendLine("pass")
        }
        source.appendLine("//--code-end;type:userInput;name:$nodeName").appendLine("\t}")
    }

    fun enumerateExpressions(s: String): String {
        var i = 0
        var l = 0
        val b = StringBuilder()
        while (++i < s.length) {
            if (s[i - 1] == '#' && s[i] == '{') {
                b.append(s.substring(l, i - 1))
                l = i + 1
                var c = 1
                while (c > 0 && ++i < s.length) {
                    if (s[i - 1] != '\\') {
                        if (s[i] == '{')
                            c++
                        if (s[i] == '}')
                            c--
                    }
                }
                b.append("\${enumerate(").append(s.substring(l, i)).append(")}")
                l = ++i
            }
        }
        if (l < s.length)
            b.append(s.substring(l, s.length))
        return b.toString()
    }

    private fun write(speech: DialogueSourceCode.Speech) = with(speech) {
        source.append("\tval $nodeName = Response($nodeId, $repeatable, " +
                (if (background != null) "\"$background\"" else "null"))
        texts.forEach { text ->
            source.append(", { \"\"\"").append(enumerateExpressions(text)).append("\"\"\" }")
        }
        source.appendLine(')')
    }

    private fun write(image: DialogueSourceCode.Image) = with(image) {
        this@DialogueSourceCodeBuilder.source.appendLine("\tval $nodeName = Response($nodeId, image = \"${source}\")")
    }

    private fun write(sound: DialogueSourceCode.Sound) = with(sound) {
        this@DialogueSourceCodeBuilder.source.appendLine("\tval $nodeName = Response($nodeId, audio = \"${source}\", isRepeatable = ${sound.repeatable})")
    }

    private fun write(command: DialogueSourceCode.Command) = with(command) {
        this@DialogueSourceCodeBuilder.source.appendLine("\tval $nodeName = Command($nodeId, \"${this.command}\", \"\"\"${code}\"\"\")")
    }

    private fun write(function: DialogueSourceCode.Function) = with(function) {
        source.appendLine("\tval $nodeName = Function($nodeId) {")
        source.appendLine("\tval transitions = mutableListOf<Transition>()")
        val lastLine = code.toString().trim().substringAfterLast('\n')
        var lastLineTransition = (lastLine.indexOf("Transition(") >= 0)
        transitions.forEach {
            if (lastLine.indexOf(it.key) >= 0)
                lastLineTransition = true
            if (code.indexOf(it.key) >= 0)
                source.append("\t\tval ${it.key} = ")
            source.appendLine("Transition(${it.value}).apply { transitions.add(this) }")
        }
        source.appendLine("//--code-start;type:function;name:$nodeName")
        source.appendLine(code)
        source.appendLine("//--code-end;type:function;name:$nodeName")

        if (transitions.size == 1 && !lastLineTransition) {
            transitions.entries.first().let {
                source.appendLine("Transition(${it.value})")
            }
        }
        source.appendLine("\t}")
    }

    private fun write(subDialogue: DialogueSourceCode.SubDialogue) = with(subDialogue) {
        source.appendLine("\tval $nodeName = SubDialogue($nodeId, \"$subDialogueId\") {")
        source.appendLine("//--code-start;type:subDialogue;name:$nodeName")
        source.appendLine(if (code.isNotEmpty()) code else "it.create()")
        source.appendLine("//--code-end;type:subDialogue;name:$nodeName").appendLine("\t}")
    }

    private fun write(action: DialogueSourceCode.Action) = with(action) {
        source.appendLine("\tval $nodeName = Action($nodeId,\"$nodeName\", \"${this.action}\")")
    }

    private fun write(action: DialogueSourceCode.GlobalAction) = with(action) {
        source.appendLine("\tval $nodeName = GlobalAction($nodeId,\"$nodeName\", \"${this.action}\")")
    }

    private fun writeTransitions() {
        source.appendLine()
        source.appendLine("\tinit {")
        transitions.forEach { source.appendLine("\t\t${it.key}.next = ${it.value}") }
        source.appendLine("\t}")
    }
}