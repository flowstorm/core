package com.promethist.core.runtime

import com.promethist.common.RestClient
import com.promethist.core.Component
import com.promethist.core.Context
import com.promethist.core.model.*
import com.promethist.core.Input
import com.promethist.core.Pipeline
import com.promethist.core.dialogue.Dialogue
import com.promethist.core.model.metrics.Metrics
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
        val profile: Profile = Profile(user_id = user._id, name = user.username),
        private val ir: Component = SimpleIntentRecognition()
) {
    class SimpleIntentRecognition : Component {
        override fun process(context: Context): Context = Dialogue.threadContext().let {
            val text = context.input.transcript.text
            val userInput = it.dialogue.node(it.context.session.dialogueStack.first().nodeId) as Dialogue.UserInput
            userInput.intents.forEach { intent ->
                intent.utterances.forEach { utterance ->
                    if (utterance.contains(text, true))
                        context.turn.input.classes.add(Input.Class(Input.Class.Type.Intent, intent.id.toString()))
                }
            }
            if (context.input.intents.isEmpty())
                error("no intent after $userInput matching text \"$text\"")
            return context
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

    private val loader: Loader = FileResourceLoader(fileResource, "dialogue")
    private val logger by LoggerDelegate()

    fun run(input: BufferedReader, output: PrintStream) {
        val dm = DialogueManager(loader)
        val app = Application(name = "test", dialogueName = name, ttsVoice = "Grace",
                properties = properties)
        val session = Session(sessionId = "T-E-S-T", user = user, application = app)
        val language = Locale.ENGLISH
        val turn = Turn(Input(language, Input.Transcript("")))
        val context = Context(SimplePipeline(LinkedList(listOf(dm, ir))), profile, session, turn, Metrics(listOf()), logger)
        while (true) {
            context.pipeline.process(context)
            output.println("> ${context.turn.responseItems}")
            if (context.sessionEnded)
                break
            val text = input.readLine()!!.trim()
            if (text == "exit")
                break
            turn.input = Input(language, Input.Transcript(text))
            turn.attributes.clear()
            turn.responseItems.clear()
            context.pipeline = SimplePipeline(LinkedList(listOf(dm, ir)))
        }
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
                runner.run(BufferedReader(InputStreamReader(System.`in`)), System.out)
            }
        }
    }
}