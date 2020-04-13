package com.promethist.core.builder

import com.promethist.core.Dialogue
import com.promethist.core.resources.FileResource
import com.promethist.core.runtime.Kotlin
import com.promethist.util.LoggerDelegate
import org.jetbrains.kotlin.daemon.common.toHexString
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.StringReader
import java.security.MessageDigest
import java.util.*
import javax.inject.Inject

class DialogueBuilder() {

    @Inject
    lateinit var fileResource: FileResource

    @Inject
    lateinit var intentModelBuilder: IntentModelBuilder

    private val logger by LoggerDelegate()

    private val version = "undefined"

    fun create(name: String): Builder = Builder(name)

    interface Resource {
        val filename: String
        val stream: InputStream
    }

    inner class Builder(val name: String) {
        val source = DialogueSourceCodeBuilder(name)
        val resources: MutableList<Resource> = mutableListOf()
        val basePath = "dialogue/${name}/"

        fun addResource(resource: Resource) = resources.add(resource)

        /**
         * Builds dialogue model with intent model and stores dialogue model class with included files using file resource.
         */
        fun build() {
            logger.info("start building dialogue model $name")
            source.build()
            val dialogue = createDialogue()
            validate(dialogue)
            saveSourceCode()
            saveResources()
            buildIntentModels(dialogue)
            logger.info("finished building dialogue model $name")
        }

        fun validate(dialogue: Dialogue) {
            dialogue.validate()
        }

        fun createDialogue(): Dialogue {
            logger.info("creating dialogue model instance $name")
            //todo remove args?
            return Kotlin.newObject(Kotlin.loadClass(StringReader(source.code)), *source.parameters.values.toTypedArray())
        }

        fun saveSourceCode() {
            val path = basePath + "model.kts"
            logger.info("saving dialogue model $name to file resource $path")
            val stream = ByteArrayInputStream(source.code.toByteArray())
            fileResource.writeFile(path, "text/kotlin", listOf("version:$version"), stream)
        }

        fun saveResources() {
            resources.forEach {
                val path = "${basePath}resources/${it.filename}"
                logger.info("saving resource file ${it.filename}")
                fileResource.writeFile(path, "text/json", listOf(), it.stream)
            }
        }

        fun buildIntentModels(dialogue: Dialogue) {
            logger.info("building intent models for dialogue model $name")
            val intentModels = mutableMapOf<String, String>()
            val language = Locale(dialogue.language)

            dialogue.globalIntents.apply/*ifNotEmpty*/ {
                val modelName = name
                val modelId = md5(modelName)
                intentModels[modelName] = modelId
                intentModelBuilder.build(modelId, modelName, language, this)
            }

            dialogue.userInputs.forEach {
                val modelName = "${name}#${it.id}"
                val modelId = md5(modelName)
                intentModels[modelName] = modelId
                intentModelBuilder.build(modelId, modelName, language, it.intents.asList())
            }
            logger.info("built intent models: $intentModels")
        }
    }

    companion object {
        private val md = MessageDigest.getInstance("MD5")

        fun md5(str: String): String = md.digest(str.toByteArray()).toHexString()
    }
}