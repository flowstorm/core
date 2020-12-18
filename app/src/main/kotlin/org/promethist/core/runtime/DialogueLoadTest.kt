package org.promethist.core.runtime

import org.promethist.common.messaging.StdOutSender
import org.promethist.core.*
import org.promethist.core.dialogue.AbstractDialogue
import org.promethist.core.model.*
import org.promethist.core.provider.LocalFileStorage
import org.bson.types.ObjectId
import org.litote.kmongo.id.toId
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
        val user = User(username = "tester@promethist.ai", name = "Tester", surname = "Tester", nickname = "Tester")
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
                Turn(Input(transcript = Input.Transcript("some message"))),
                logger,
                dialogue.locale,
                SimpleCommunityStorage(),
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