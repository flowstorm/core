package com.promethist.core.dialogue

import com.promethist.core.model.DialogueEvent
import java.util.*
import kotlin.reflect.full.memberProperties

abstract class BasicCzechDialogue() : BasicDialogue() {

    override fun addResponseItem(text: String?, image: String?, audio: String?, video: String?, repeatable: Boolean) =
            codeRun.context.turn.addResponseItem(text?.let { evaluateTextTemplate(evaluateMorphology(it)) }, image, audio, video, repeatable, voice)

    private fun evaluateMorphology(text: String): String {
        if (false)  {
            return text.replace("@", "")
        } else {
            return text.replace("l@", "la")
                    .replace("en@", "na")
                    .replace("sám@", "sama")
                    .replace("ý@", "á")
                    .replace("@", "a")
        }
    }



    var basicId = 1
    //Nodes
    val _goBack = GoBack(basicId++, repeat = true)
    val _basicVersionGlobalIntent = GlobalIntent(basicId++, "basicVolumeUpGlobalIntent", 0.99F, "verze")
    val _basicVersionResponse = Response(basicId++, { "#version, dialog $dialogueName" })

    val _basicVolumeUpGlobalIntent = GlobalIntent(basicId++, "basicVolumeUpGlobalIntent", 0.99F, "zvýšit hlasitost", "mluv hlasitěji")
    val _basicVolumeUpResponse = Response(basicId++, { "#volume_up zvyšuji hlasitist" })

    val _basicVolumeDownGlobalIntent = GlobalIntent(basicId++, "basicVolumeDownGlobalIntent", 0.99F, "snížit hlasitost", "mluv tišeji")
    val _basicVolumeDownResponse = Response(basicId++, { "#volume_down snižuji hlasitost" })

    val _basicLogApplicationErrorGlobalIntent = GlobalIntent(basicId++, "basicLogApplicationErrorGlobalIntent", 0.99F, "chyba aplikace", "problém aplikace")
    val _basicLogApplicationErrorResponse1 = Response(basicId++, {"O co jde?"})
    val _basicLogApplicationErrorResponse2 = Response(basicId++, {"Díky, pojďme zpátky."})
    val _basicLogApplicationErrorUserInputTransition = Transition(_basicLogApplicationErrorResponse2)
    val _basicLogApplicationErrorUserInput = UserInput(basicId++, arrayOf()) {
        val transition = Transition(_basicLogApplicationErrorResponse2)
        dialogueEvent = DialogueEvent(datetime = Date(), type = DialogueEvent.Type.UserError, user = user, sessionId = session.sessionId, applicationName = application.name, dialogueName = application.dialogueName, nodeId = turn.endFrame?.nodeId, text = input.transcript.text)
        transition
    }

    val _basicLogApplicationCommentGlobalIntent = GlobalIntent(basicId++, "basicLogApplicationCommentGlobalIntent", 0.99F, "komentář aplikace")
    val _basicLogApplicationCommentResponse1 = Response(basicId++, {"O co jde?"})
    val _basicLogApplicationCommentResponse2 = Response(basicId++, {"Díky, pojďme zpátky."})
    val _basicLogApplicationCommentUserInputTransition = Transition(_basicLogApplicationCommentResponse2)
    val _basicLogApplicationCommentUserInput = UserInput(basicId++, arrayOf()) {
        val transition = Transition(_basicLogApplicationCommentResponse2)
        dialogueEvent = DialogueEvent(datetime = Date(), type = DialogueEvent.Type.UserComment, user = user, sessionId = session.sessionId, applicationName = application.name, dialogueName = application.dialogueName, nodeId = turn.endFrame?.nodeId, text = input.transcript.text)
        transition
    }

    init {
        _basicVersionGlobalIntent.next = _basicVersionResponse
        _basicVersionResponse.next = _goBack

        _basicVolumeUpGlobalIntent.next = _basicVolumeUpResponse
        _basicVolumeUpResponse.next = _goBack

        _basicVolumeDownGlobalIntent.next = _basicVolumeDownResponse
        _basicVolumeDownResponse.next = _goBack

        _basicLogApplicationErrorGlobalIntent.next = _basicLogApplicationErrorResponse1
        _basicLogApplicationErrorResponse1.next = _basicLogApplicationErrorUserInput
        _basicLogApplicationErrorResponse2.next = _goBack

        _basicLogApplicationCommentGlobalIntent.next = _basicLogApplicationCommentResponse1
        _basicLogApplicationCommentResponse1.next = _basicLogApplicationCommentUserInput
        _basicLogApplicationCommentResponse2.next = _goBack
    }
}