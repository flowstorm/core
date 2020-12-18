package org.promethist.core.runtime

import org.bson.types.ObjectId
import org.litote.kmongo.id.toId
import org.litote.kmongo.newId
import org.promethist.common.TextConsole
import org.promethist.common.messaging.StdOutSender
import org.promethist.core.*
import org.promethist.core.dialogue.AbstractDialogue
import org.promethist.core.model.*
import org.promethist.core.provider.LocalFileStorage
import org.promethist.core.storage.FileStorage
import org.promethist.core.type.MutablePropertyMap
import org.promethist.util.DataConverter
import org.promethist.util.LoggerDelegate
import java.io.*
import java.util.*

class DialogueRunner(
        val fileStorage: FileStorage,
        val dialogueId: String,
        val properties: MutablePropertyMap = mutableMapOf(),
        val user: User = User(username = "tester@promethist.ai", name = "Tester", surname = "Tester", nickname = "Tester"),
        val profile: Profile = Profile(user_id = user._id, space_id = newId())
) : TextConsole() {

    private val ir: Component = SimpleIntentRecognition()

    inner class SimpleIntentRecognition : Component {

        lateinit var models: Map<Model, Map<Int, List<String>>>

        override fun process(context: Context): Context {
            if (!this::models.isInitialized) {
                initModels(dmf.get(dialogueId, "", properties))
            }

            val text = context.input.transcript.text

            // select requested models
            val requestedModels = models.filter { it.key.id in context.intentModels.map { it.id } }

            val matches = mutableListOf<Pair<String, Int>>()
            for (model in requestedModels) {
                matches.addAll(model.value.filter { it.value.filter { it.contains(text, true) }.isNotEmpty() }.keys.map { model.key.id to it })
            }

            val match = when (matches.size) {
                0 -> error("no intent model ${context.intentModels.map { it.name }} matching text \"$text\"")
                1 -> matches.first()
                else -> {
                    context.logger.warn("multiple intents $matches matched text \"$text\"")
                    matches.first()
                }
            }

            context.turn.input.classes.add(Input.Class(Input.Class.Type.Intent, match.first + "#" + match.second.toString()))

            return context
        }

        private fun initModels(dialogue: AbstractDialogue) {
            val map = mutableMapOf<Model, Map<Int, List<String>>>()

            map.put(org.promethist.core.builder.IntentModel(dialogue.buildId, dialogue.dialogueId, null), dialogue.globalIntents.map { it.id to it.utterances.toList() }.toMap())

            dialogue.userInputs.forEach {
                map.put(org.promethist.core.builder.IntentModel(dialogue.buildId, dialogue.dialogueId, it.id), it.intents.map { it.id to it.utterances.toList() }.toMap())
            }

            models = map.toMap()
        }
    }

    class SimplePipeline(override val components: LinkedList<Component>) : Pipeline {
        override fun process(context: Context): Context {
            var processedContext = context
            if (components.isNotEmpty())
                processedContext = components.pop().process(context)
            return processedContext
        }
    }

    var locale = Defaults.locale
    var zoneId = Defaults.zoneId

    private val loader: Loader = FileResourceLoader("dialogue", useScript = true).apply { fileStorage = this@DialogueRunner.fileStorage }
    private val logger by LoggerDelegate()

    private val dmf = DialogueFactory().apply { loader = this@DialogueRunner.loader }
    private val dm = DialogueManager().apply {
        dialogueFactory = dmf
    }
    private val app = Application(name = "test", dialogue_id = ObjectId(dialogueId).toId(), properties = properties)
    private val session = Session(sessionId = "T-E-S-T", user = user, application = app, device = Device(deviceId = "test", description = ""))
    private val turn = Turn(Input(locale, zoneId, Input.Transcript("")))
    private val context = Context(SimplePipeline(LinkedList(listOf(dm, ir))), profile, session, turn, logger, locale, SimpleCommunityStorage(), StdOutSender())

    override fun beforeInput() {
        context.pipeline.process(context)
        output.println("> ${context.turn.responseItems}")
        if (context.sessionEnded)
            stop = true
    }

    override fun afterInput(text: String) {
        turn.input = Input(locale, zoneId, Input.Transcript(text))
        turn.attributes.clear()
        turn.responseItems.clear()
        context.pipeline = SimplePipeline(LinkedList(listOf(dm, ir)))
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            if (args.size < 2) {
                println("""
                    Usage:
                        <storage> <path/to/dialogue> (prop1(:Type)=val1 (prop2(:Type)=val2 ...))
                    
                    Examples:
                        product/some-dialogue/1 someText=bla maxMath:Int=5 doMath:Boolean=true
                        https://filestore.develop.promethist.com product/some-subdialogue/1
                        
                    Notes:
                        When local storage is used current working directory must contain filestore folder with
                        subfolder dialogue containing dialogue model(s) to run.
                """.trimIndent())
            } else {
                val properties: MutablePropertyMap = mutableMapOf()
                for (i in 2 until args.size) {
                    val p1 = args[i].split("=")
                    val p2 = p1[0].split(":")
                    properties[p2[0]] = DataConverter.valueFromString(p2[0], p2.getOrElse(1) { "String" }, p1[1])
                }
                val runner = DialogueRunner(LocalFileStorage(File("filestore")), args[0], properties
                )
                println("running dialogue ${args[0]} with properties $properties from ${runner.fileStorage}")
                runner.run()
            }
        }
    }
}