package com.promethist.core.runtime

import com.promethist.core.*
import com.promethist.core.dialogue.AbstractDialogue
import com.promethist.core.model.*
import com.promethist.core.provider.LocalFileStorage
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
        val loader = FileResourceLoader(LocalFileStorage(File("test")), "dialogue")
        val dialogueId = "dialogue1"
        val dialogue = loader.newObject<AbstractDialogue>("$dialogueId/model", "ble", 1, false)
        dialogue.loader = loader

        dialogue.validate()
        println(dialogue.describe())
        val user = User(username = "tester@promethist.ai", name = "Tester", surname = "Tester", nickname = "Tester")
        val context = Context(
                object : Pipeline {
                    override val components = LinkedList<Component>()
                    override fun process(context: Context): Context = components.pop().process(context)
                },
                Profile(user_id = user._id),
                Session(
                        datetime = Date(),
                        sessionId = "T-E-S-T",
                        user = user,
                        application = Application(name = "test", dialogue_id = ObjectId(dialogueId).toId(), voice = Voice.Grace)
                ),
                Turn(Input(transcript = Input.Transcript("some message"))),
                logger,
                dialogue.locale,
                SimpleCommunityResource()
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