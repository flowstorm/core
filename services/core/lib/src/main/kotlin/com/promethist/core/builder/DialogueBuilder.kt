package com.promethist.core.builder

import com.promethist.core.dialogue.Dialogue
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

        val buildId = md5(random.nextLong().toString())

        val source = DialogueSourceCodeBuilder(name, buildId)
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
            return Kotlin.newObjectWithArgs(Kotlin.loadClass(StringReader(source.code)), source.parameters)
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
            val irModels = mutableListOf<IrModel>()
            val language = Locale(dialogue.language)

            dialogue.globalIntents.apply/*ifNotEmpty*/ {
                val irModel = IrModel(buildId, name, null)
                irModels.add(irModel)
                intentModelBuilder.build(irModel, language, this)
            }

            dialogue.userInputs.forEach {
                val irModel = IrModel(buildId, name, it.id)
                irModels.add(irModel)
                intentModelBuilder.build(irModel, language, it.intents.asList())
            }
            logger.info("built intent models: $irModels")
        }
    }

    companion object {
        private val random = Random()
        private val md = MessageDigest.getInstance("MD5")

        fun md5(str: String): String = md.digest(str.toByteArray()).toHexString()
    }
}