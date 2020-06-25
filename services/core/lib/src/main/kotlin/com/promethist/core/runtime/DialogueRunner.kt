package com.promethist.core.runtime

import com.promethist.common.TextConsole
import com.promethist.common.RestClient
import com.promethist.core.*
import com.promethist.core.model.*
import com.promethist.core.dialogue.Dialogue
import com.promethist.core.provider.LocalFileStorage
import com.promethist.core.resources.FileResource
import com.promethist.core.type.MutablePropertyMap
import com.promethist.util.DataConverter
import com.promethist.util.LoggerDelegate
import java.io.*
import java.util.*

class DialogueRunner(
        val fileResource: FileResource,
        val name: String,
        val properties: MutablePropertyMap = mutableMapOf(),
        val user: User = User(username = "tester@promethist.ai", name = "Tester", surname = "Tester", nickname = "Tester"),
        val profile: Profile = Profile(user_id = user._id),
        private val ir: Component = SimpleIntentRecognition()
) : TextConsole() {
    class SimpleIntentRecognition : Component {

        lateinit var models: Map<IrModel, Map<Int, List<String>>>

        override fun process(context: Context): Context = Dialogue.codeRun.let {
            if (!this::models.isInitialized) {
                initModels(it.node.dialogue)
            }

            val text = context.input.transcript.text

            // select requested models
            val requestedModels = models.filter { it.key.id in context.irModels.map { it.id } }

            val matches = mutableListOf<Pair<String, Int>>()
            for (model in requestedModels) {
                matches.addAll(model.value.filter { it.value.filter { it.contains(text, true) }.isNotEmpty() }.keys.map { model.key.id to it })
            }

            val match = when (matches.size) {
                0 -> error("no intent model ${context.irModels.map { it.name }} matching text \"$text\"")
                1 -> matches.first()
                else -> {
                    context.logger.warn("multiple intents $matches matched text \"$text\"")
                    matches.first()
                }
            }

            context.turn.input.classes.add(Input.Class(Input.Class.Type.Intent, match.first + "#" + match.second.toString()))

            return context
        }

        private fun initModels(dialogue: Dialogue) {
            val map = mutableMapOf<IrModel, Map<Int, List<String>>>()

            map.put(com.promethist.core.builder.IrModel(dialogue.buildId, dialogue.dialogueName, null),
                    dialogue.globalIntents.map { it.id to it.utterances.toList() }.toMap())

            dialogue.userInputs.forEach {
                map.put(com.promethist.core.builder.IrModel(dialogue.buildId, dialogue.dialogueName, it.id),
                        it.intents.map { it.id to it.utterances.toList() }.toMap())
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

    private val loader: Loader = FileResourceLoader(fileResource, "dialogue", useScript = true)
    private val logger by LoggerDelegate()

    private val dm = DialogueManager().apply {
        dialogueFactory = DialogueFactory(loader)
    }
    private val app = Application(name = "test", dialogueName = name, voice = Voice.Grace, properties = properties)
    private val session = Session(sessionId = "T-E-S-T", user = user, application = app)
    private val turn = Turn(Input(locale, zoneId, Input.Transcript("")))
    private val context = Context(SimplePipeline(LinkedList(listOf(dm, ir))), profile, session, turn, logger, locale, SimpleCommunityResource())

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
                        local product/some-dialogue/1 someText=bla maxMath:Int=5 doMath:Boolean=true
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
                val runner = DialogueRunner(when (args[0]) {
                    "local" -> LocalFileStorage(File("filestore"))
                    else -> RestClient.instance(FileResource::class.java, args[0])
                }, args[1], properties
                )
                println("running dialogue ${args[1]} with properties $properties from ${args[0]} [${runner.fileResource}]")
                runner.run()
            }
        }
    }
}