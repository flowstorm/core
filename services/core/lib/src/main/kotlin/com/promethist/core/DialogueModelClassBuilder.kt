package com.promethist.core

class DialogueModelClassBuilder(val name: String, parentClass: String = "Dialogue") {

    val source = StringBuilder()
    val className: String

    init {
        val names = mutableListOf(name.split("/"))
        className = "Model" + names.removeAt(names.size - 1)
        val version = "undefined"//AppConfig.instance.get("git.ref", "unknown")
        source
                .appendln("//--dialoguemodel;version:$version;name:$name")
                .appendln("package " + names.joinToString(".") { "`$it`" })
                .appendln("import com.promethist.core.*")
                .appendln("import com.promethist.core.model.*")
                .appendln("data class $className(")
                .appendln("\toverride val resourceLoader: ResourceLoader,")
                .appendln("\toverride val name: String)")
                .appendln(") : Dialogue(resourceLoader, name) {")
    }

    fun addIntent(nodeId: Int, nodeName: String, utterances: List<String>) {

    }

    fun addResponse(nodeId: Int, nodeName: String, texts: List<String>) {

    }

    fun addFunction(nodeId: Int, nodeName: String, transitions: Map<String, String>, functionSource: CharSequence) {
        source.appendln("\tval $nodeName = Function($nodeId) { context, logger ->")
        transitions.forEach { source.appendln("\t\tval ${it.key} = Transition(${it.value})") }
        source.appendln("//--function-start:$nodeName")
        source.appendln(functionSource)
        source.appendln("//--function-end:$nodeName\n")
    }

    fun addSubDialogue(nodeId: Int, nodeName: String, subDialogueName: String, functionSource: CharSequence = "create()") {
        source.appendln("\tval $nodeName = SubDialogue($nodeId, \"$subDialogueName\") {")
        source.appendln("//--function-start:$nodeName")
        source.appendln(functionSource)
        source.appendln("//--function-end:$nodeName\n")
    }

    fun finish(transitions: Map<String, String>, extensionSource: CharSequence? = null): StringBuilder {
        source.appendln("\tinit {")
        transitions.forEach { source.appendln("\t\t${it.key}.next = ${it.value}") }
        source.appendln("\t}")
        source.appendln('}')
        if (extensionSource != null)
            source.append(extensionSource)
        source.appendln("$className::class")
        return source
    }

}