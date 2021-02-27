package ai.flowstorm.core.runtime

import org.bson.types.ObjectId
import org.litote.kmongo.id.toId
import ai.flowstorm.common.messaging.StdOutSender
import ai.flowstorm.core.Component
import ai.flowstorm.core.Context
import ai.flowstorm.core.Input
import ai.flowstorm.core.Pipeline
import ai.flowstorm.core.dialogue.AbstractDialogue
import ai.flowstorm.core.model.*
import ai.flowstorm.core.provider.LocalFileStorage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*

object DialogueLoadTest {

    @JvmStatic
    fun main(args: Array<String>) {
        val logger: Logger = LoggerFactory.getLogger("dialogue-model-load-test")
        val loader = FileResourceLoader( "dialogue").apply { fileStorage = LocalFileStorage(File("test")) }
        val dialogueId = "dialogue1"
        val dialogue = loader.newObject<AbstractDialogue>("$dialogueId/model", "ble", 1, false)
        dialogue.loader = loader

        dialogue.validate()
        println(dialogue.describe())
        val user = User(username = "tester@flowstorm.ai", name = "Tester", surname = "Tester", nickname = "Tester")
        val space =
            SpaceImpl(name="Test Space")
        val context = Context(
            object : Pipeline {
                override val components = LinkedList<Component>()
                override fun process(context: Context): Context = components.pop().process(context)
            },
            Profile(user_id = user._id, space_id = space._id),
            Session(
                datetime = Date(),
                sessionId = "T-E-S-T",
                device = Device(deviceId = "test", description = ""),
                    user = user,
                        application = Application(name = "test", dialogue_id = ObjectId(dialogueId).toId())
                ),
                Turn(input = Input(transcript = Input.Transcript("some message"))),
                logger,
                dialogue.locale,
                SimpleCommunityStorage(),
                SimpleDialogueEventRepository(),
                StdOutSender()
        )

        val func = dialogue.functions.first()
        println("calling $func:")
        println(func.exec(context))

        dialogue.subDialogues.first().apply {
            val dialogueArgs = getConstructorArgs(context)
            val subDialogue = loader.newObjectWithArgs<AbstractDialogue>("${this.dialogueId}/model", dialogueArgs)
            println("subDialogue: $subDialogue")
            println(subDialogue.describe())
        }
    }
}