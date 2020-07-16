package com.promethist.core.dialogue

import com.promethist.common.AppConfig
import com.promethist.core.model.DialogueEvent
import java.util.*

abstract class BasicEnglishDialogue() : BasicDialogue() {

    var basicId = 1
    //Nodes
    val _goBack = GoBack(basicId++, repeat = true)
    val _basicVersionGlobalIntent = GlobalIntent(basicId++, "basicVersionGlobalIntent", 0.99F, "version")
    val _basicVersionResponse = Response(basicId++, { "Server version ${AppConfig.version}, environment ${AppConfig.instance.get("namespace", "unknown")}, dialogue $dialogueName, dialogue version $version" })

    val _basicVolumeUpGlobalIntent = GlobalIntent(basicId++, "basicVolumeUpGlobalIntent", 0.99F, "volume up", "louder")
    val _basicVolumeUpResponse = Response(basicId++, { "#volume_up setting volume up" })

    val _basicVolumeDownGlobalIntent = GlobalIntent(basicId++, "basicVolumeDownGlobalIntent", 0.99F, "volume down", "quieter")
    val _basicVolumeDownResponse = Response(basicId++, { "#volume_down setting volume down" })

    val _basicLogApplicationErrorGlobalIntent = GlobalIntent(basicId++, "basicLogApplicationErrorGlobalIntent", 0.99F,  "application error", "application problem")
    val _basicLogApplicationErrorResponse1 = Response(basicId++, {"What's the problem?"})
    val _basicLogApplicationErrorResponse2 = Response(basicId++, {"Thanks. Let's get back."})
    val _basicLogApplicationErrorUserInputTransition = Transition(_basicLogApplicationErrorResponse2)
    val _basicLogApplicationErrorUserInput = UserInput(basicId++, arrayOf()) {
        val transition = Transition(_basicLogApplicationErrorResponse2)
        dialogueEvent = DialogueEvent(datetime = Date(), type = DialogueEvent.Type.UserError, user = user, sessionId = session.sessionId, properties = context.session.properties, applicationName = application.name, dialogueName = application.dialogueName, nodeId = turn.endFrame?.nodeId, text = input.transcript.text)
        transition
    }

    val _basicLogApplicationCommentGlobalIntent = GlobalIntent(basicId++, "basicLogApplicationCommentGlobalIntent", 0.99F, "application command", "application comment")
    val _basicLogApplicationCommentResponse1 = Response(basicId++, {"What's the comment?"})
    val _basicLogApplicationCommentResponse2 = Response(basicId++, {"Thanks. Let's get back"})
    val _basicLogApplicationCommentUserInputTransition = Transition(_basicLogApplicationCommentResponse2)
    val _basicLogApplicationCommentUserInput = UserInput(basicId++, arrayOf()) {
        val transition = Transition(_basicLogApplicationCommentResponse2)
        dialogueEvent = DialogueEvent(datetime = Date(), type = DialogueEvent.Type.UserComment, user = user, sessionId = session.sessionId, properties = context.session.properties, applicationName = application.name, dialogueName = application.dialogueName, nodeId = turn.endFrame?.nodeId, text = input.transcript.text)
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