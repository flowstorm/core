package com.promethist.core.builder

import com.promethist.core.Dialogue
import com.promethist.core.resources.FileResource
import com.promethist.core.runtime.FileResourceLoader
import com.promethist.core.runtime.Kotlin
import com.promethist.util.LoggerDelegate
import org.jetbrains.kotlin.daemon.common.toHexString
import java.io.ByteArrayInputStream
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

    fun createDialogue(source: DialogueSourceCodeBuilder): Dialogue {
        logger.info("creating dialogue model instance ${source.name}")
        val loader = FileResourceLoader(fileResource, "dialogue")

        //todo remove args?
        return Kotlin.newObject(Kotlin.loadClass(StringReader(source.code)), loader, source.name, *source.args.values.toTypedArray())
    }

    fun deploy(source: DialogueSourceCodeBuilder) {
        val path = "dialogue/${source.name}/model.kts"
        logger.info("writing dialogue model to file resource $path")
        val stream = ByteArrayInputStream(source.code.toByteArray())
        fileResource.writeFile(path, "text/kotlin", listOf("version:$version"), stream)
        logger.info("dialogue model ${source.name} built successfully")
    }

    fun buildIntentModels(name:String, dialogue: Dialogue) {
        logger.info("building intent models for dialogue model $name")
        val intentModels = mutableMapOf<String, String>()
        val language = Locale(dialogue.language)

        dialogue.globalIntents.apply/*ifNotEmpty*/ {


            val modelName = "$name"
            val modelId = md5(modelName)
            intentModels[modelName] = modelId
            intentModelBuilder.build(modelId, name, language, this)
        }

        dialogue.userInputs.forEach {
            val modelName = "${name}#${it.id}"
            val modelId = md5(modelName)
            intentModels[modelName] = modelId
            intentModelBuilder.build(modelId, modelName, language, it.intents.asList())
        }
        logger.info("builded intent models: $intentModels")
    }

    fun validate(dialogue: Dialogue) {
        logger.info("validating dialogue model $dialogue")
        dialogue.validate()
    }

    /**
     * Builds dialogue model with intent model and stores dialogue model class with included files using file resource.
     */
    fun build(source: DialogueSourceCodeBuilder) {
        val dialogue = createDialogue(source)

        validate(dialogue)
        buildIntentModels(source.name, dialogue)
        deploy(source)
    }

    companion object {
        private val md = MessageDigest.getInstance("MD5")

        fun md5(str: String): String = md.digest(str.toByteArray()).toHexString()
    }
}